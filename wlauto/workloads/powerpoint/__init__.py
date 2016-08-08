#    Copyright 2014-2016 ARM Limited
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import os
import re

from wlauto import AndroidUiAutoBenchmark, Parameter, File

__version__ = '0.1.1'


class Powerpoint(AndroidUiAutoBenchmark):

    name = 'powerpoint'
    package = 'com.microsoft.office.powerpoint'
    activity = 'com.microsoft.office.apphost.LaunchActivity'
    view = [package + '/com.microsoft.office.apphost.LaunchActivity',
            package + '/.PPTActivity']
    description = """
    A workload to perform standard productivity tasks with Microsoft PowerPoint.
    This workload is split into two tests:

    --- create ---
    Prepares a basic presentation consisting of a simple title slide
    and a single image slide. The presentation is then presented in a slide show.

    Test description:
    1. Open Microsoft PowerPoint application
    2. Dismisses sign step and uses the app without an account.
    3. Creates a new presentation
    4. Specifies storage location when saving presentation
    5. Selects a slide template style
    6. Edits title text of first slide
    7. Selects a blank layout and creates a new slide
    8. Adds an image to the blank slide from the local storage
    9. Starts a slide show and presents the slides

    --- load ---
    Loads a presentation from file and sets a transition effect for the slides.
    The presentation is then presented in a slide show. The .pptx file to use
    should be placed in the dependencies directory.

    Test description:
    1. Open Microsoft PowerPoint application
    2. Dismisses sign step and uses the app without an account.
    3. Loads a presentation from file
    4. Selects a transition effect type
    5. Starts a slide show and presents the slides

    NOTE: This test is turned off by default. To run this test it must first be
    enabled in an agenda file by setting 'use_test_file' parameter to True.

    This workload requires a network connection (ideally, wifi) to run.
    """

    parameters = [
        Parameter('dumpsys_enabled', kind=bool, default=True,
                  description="""
                  If ``True``, dumpsys captures will be carried out during the
                  test run.  The output is piped to log files which are then
                  pulled from the phone.
                  """),
        Parameter('slide_template', kind=str, mandatory=False, default='Blank_presentation',
                  description="""
                  The slide template name to use when creating a new presentation.
                  If the device is offline, ``Blank presentation`` will be used.
                  All other templates require networking.
                  Note: spaces must be replaced with underscores in the slide template.
                  """),
        Parameter('title_name', kind=str, mandatory=False, default='Test_Title',
                  description="""
                  The title to use when creating a new presentation.
                  Note: spaces must be replaced with underscores in the title name.
                  """),
        Parameter('test_image', kind=str, mandatory=False, default='uxperf_1600x1200.jpg',
                  description="""
                  Image to be embedded in the ``test_file`` document.
                  """),
        Parameter('use_test_file', kind=bool, default=False,
                  description="""
                  If ``True``, pushes a preconfigured test file to the device
                  used for measuring performance metrics.
                  """),
        Parameter('test_file', kind=str, mandatory=False, default='uxperf_test_doc.pptx',
                  description="""
                  Filename to push to the device for testing
                  Note: spaces must be replaced with underscores in the test_file name.
                  """),
        Parameter('transition_effect', kind=str, mandatory=False, default='None',
                  description="""
                  The transition animation to use when moving between slides.
                  Note: Accepts single words only.
                  """),
        Parameter('number_of_slides', kind=int, mandatory=False, default=3,
                  constraint=lambda x: x > 0 and x <= 100, description="""
                  The number of slides to view when performing a slide show.
                  Note: Must be a number larger than 0 and smaller than one hundred.
                  """),
    ]

    instrumentation_log = name + '_instrumentation.log'

    def __init__(self, device, **kwargs):
        super(Powerpoint, self).__init__(device, **kwargs)
        self.output_file = self.path_on_device(self.instrumentation_log)

    def validate(self):
        super(Powerpoint, self).validate()
        self.uiauto_params['package'] = self.package
        self.uiauto_params['output_dir'] = self.device.working_directory
        self.uiauto_params['output_file'] = self.output_file
        self.uiauto_params['dumpsys_enabled'] = self.dumpsys_enabled
        self.uiauto_params['title_name'] = self.title_name
        self.uiauto_params['use_test_file'] = self.use_test_file
        self.uiauto_params['test_file'] = self.test_file
        self.uiauto_params['transition_effect'] = self.transition_effect
        self.uiauto_params['number_of_slides'] = self.number_of_slides
        # Networking is required for templates. Force use of blank template when no networking is enabled.
        if self.device.is_network_connected():
            self.uiauto_params['slide_template'] = self.slide_template
        else:
            self.uiauto_params['slide_template'] = 'Blank_presentation'

    def setup(self, context):
        super(Powerpoint, self).setup(context)

        # push test files to device
        fpath = context.resolver.get(File(self, self.test_image))
        fname = os.path.basename(fpath)
        self.device.push_file(fpath, self.path_on_device(fname), timeout=300)

        if self.use_test_file:
            fpath = context.resolver.get(File(self, self.test_file))
            fname = os.path.basename(fpath)  # Ensures correct behaviour in case params are absolute paths
            self.device.push_file(fpath, self.path_on_device(fname), timeout=300)

        # Force a re-index of the mediaserver cache to pick up new files
        self.device.execute('am broadcast -a android.intent.action.MEDIA_MOUNTED -d file:///sdcard')

    def update_result(self, context):
        super(Powerpoint, self).update_result(context)

        self.device.pull_file(self.output_file, context.output_directory)
        result_file = os.path.join(context.output_directory, self.instrumentation_log)

        with open(result_file, 'r') as wfh:
            pattern = r'(?P<key>\w+)\s+(?P<value1>\d+)\s+(?P<value2>\d+)\s+(?P<value3>\d+)'
            regex = re.compile(pattern)
            for line in wfh:
                match = regex.search(line)
                if match:
                    context.result.add_metric((match.group('key') + "_start"),
                                              match.group('value1'), units='ms')
                    context.result.add_metric((match.group('key') + "_finish"),
                                              match.group('value2'), units='ms')
                    context.result.add_metric((match.group('key') + "_duration"),
                                              match.group('value3'), units='ms')

    def teardown(self, context):
        super(Powerpoint, self).teardown(context)

        regex = re.compile(r'Presentation( \([0-9]+\))?\.pptx')

        for entry in self.device.listdir(self.device.working_directory):
            if entry.endswith(".log"):
                self.device.pull_file(self.path_on_device(entry), context.output_directory)
                self.device.delete_file(self.path_on_device(entry))

            # Clean up powerpoint file from 'create' test on each iteration
            if regex.search(entry):
                self.device.delete_file(self.path_on_device(entry))

        self.device.execute('am broadcast -a android.intent.action.MEDIA_MOUNTED -d file:///sdcard')

    def finalize(self, context):
        super(Powerpoint, self).finalize(context)

        for entry in self.device.listdir(self.device.working_directory):
            if entry.lower().endswith(('.jpg', '.jpeg', '.pptx')):
                self.device.delete_file(self.path_on_device(entry))

        # Force a re-index of the mediaserver cache to removed cached files
        self.device.execute('am broadcast -a android.intent.action.MEDIA_MOUNTED -d file:///sdcard')

    # Absolute path of the file inside WA working directory
    def path_on_device(self, name):
        return self.device.path.join(self.device.working_directory, name)
