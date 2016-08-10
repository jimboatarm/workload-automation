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


class MsWord(AndroidUiAutoBenchmark):

    name = 'msword'
    package = 'com.microsoft.office.word'
    activity = 'com.microsoft.office.apphost.LaunchActivity'
    description = '''
    A workload to perform standard productivity tasks with Microsoft Word. The workload carries
    out various tasks, such as creating a new document, adding and editing text formatting,
    shapes and images.

    The workload is split into two main scenarios:

    --- create ---
    Creates a new file inside the application and performs editing tasks on it.

    Test description:
    1. Starts the app and skips the welcome screen. Tries to determine device type (phone/tablet)
       as UI elements for certain actions differ. If unable to do so, assumes it to be a phone.
    2. A new Microsoft Word file is created on-device from the Newsletter template and renamed.
    3. Performs a search of the document title. Once found, this is highlighted and font style
       changed (bold, italic, underline).
    4. More formatting of the highlighted text - font size, font colour and background colour
       are also changed.
    5. Finally the file is closed and app returns to the documents list.
    6. At the end of the test, the created file is removed from the recent documents list and
       deleted from the device.

    --- load ---
    Copies an existing Microsoft Word file from the host to the device and runs tests on it.

    Test description:
    1. Pushes an existing Word document and an image to the device.
    2. Starts the app and skips the welcome screen. Tries to determine device type (phone/tablet)
       as UI elements for certain actions differ. If unable to do so, assumes it to be a phone.
    3. Dismisses the help tooltip if it appears. Performs a simple navigation test by scrolling
       down the file (maximum 10 swipes).
    4. Inserts a shape into the document and changes it's fill colour.
    5. Inserts the pushed image into the document and changes the image frame style.
    6. Finally, scrolls back to the beginning of the file and closes the it.
    7. At the end of the test, the modified file (which is automatically saved) is removed from
       the recent documents list and deleted from the device.
    '''

    view = [
        package + '/com.microsoft.office.word.WordActivity',
        package + '/com.microsoft.office.apphost.LaunchActivity',
    ]

    parameters = [
        Parameter('dumpsys_enabled', kind=bool, default=True,
                  description='''
                  If ``True``, dumpsys captures will be carried out during the test run.
                  The output is piped to log files which are then pulled from the phone.
                  '''),
        Parameter('use_test_file', kind=bool, default=False,
                  description="""
                  If ``True``, pushes a preconfigured test file to the device
                  used for measuring performance metrics.
                  """),
        Parameter('test_file', kind=str, mandatory=False, default='uxperf_test_doc.docx',
                  description="""
                  Filename to push to the device for testing
                  Note: spaces must be replaced with underscores in the test_file name.
                  """),
        Parameter('test_image', kind=str, mandatory=False, default='uxperf_1600x1200.jpg',
                  description="""
                  Image to be embedded in the ``test_file`` document.
                  Only applicable if ``use_test_file`` is true.
                  """),
    ]

    instrumentation_log = name + '_instrumentation.log'

    def __init__(self, device, **kwargs):
        super(MsWord, self).__init__(device, **kwargs)
        self.run_timeout = 300
        self.output_file = self.path_on_device(self.instrumentation_log)

    def validate(self):
        super(MsWord, self).validate()
        self.uiauto_params['package'] = self.package
        self.uiauto_params['output_dir'] = self.device.working_directory
        self.uiauto_params['output_file'] = self.output_file
        self.uiauto_params['dumpsys_enabled'] = self.dumpsys_enabled
        self.uiauto_params['use_test_file'] = self.use_test_file
        if self.use_test_file:
            self.uiauto_params['test_file'] = self.test_file
            self.uiauto_params['test_image'] = self.test_image

    def setup(self, context):
        super(MsWord, self).setup(context)

        # push test files to device
        if self.use_test_file:
            for ff in (self.test_file, self.test_image):
                fpath = context.resolver.get(File(self, ff))
                fname = os.path.basename(ff)  # Ensures correct behaviour in case params are absolute paths
                self.device.push_file(fpath, self.path_on_device(fname), timeout=300)

            # Force a re-index of the mediaserver cache to pick up new files
            self.device.execute('am broadcast -a android.intent.action.MEDIA_MOUNTED -d file:///sdcard')

    def update_result(self, context):
        super(MsWord, self).update_result(context)
        if self.dumpsys_enabled:
            self.device.pull_file(self.output_file, context.output_directory)
            result_file = os.path.join(context.output_directory, self.instrumentation_log)
            # pull instrumentation data
            with open(result_file, 'r') as wfh:
                regex = re.compile(r'(?P<key>\w+)\s+(?P<value1>\d+)\s+(?P<value2>\d+)\s+(?P<value3>\d+)')
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
        super(MsWord, self).teardown(context)

        regex = re.compile(r'Document( \([0-9]+\))?\.docx')

        # pull logs
        for entry in self.device.listdir(self.device.working_directory):
            if entry.endswith('.log'):
                self.device.pull_file(self.path_on_device(entry), context.output_directory)
                self.device.delete_file(self.path_on_device(entry))

            # Clean up document file from 'create' test on each iteration
            if regex.search(entry):
                self.device.delete_file(self.path_on_device(entry))

        self.device.execute('am broadcast -a android.intent.action.MEDIA_MOUNTED -d file:///sdcard')

    def finalize(self, context):
        super(MsWord, self).finalize(context)

        for entry in self.device.listdir(self.device.working_directory):
            if entry.lower() in (self.test_file.lower(), self.test_image.lower()):
                self.device.delete_file(self.path_on_device(entry))

        # Force a re-index of the mediaserver cache to removed cached files
        self.device.execute('am broadcast -a android.intent.action.MEDIA_MOUNTED -d file:///sdcard')

    # Absolute path of the file inside WA working directory
    def path_on_device(self, name):
        return self.device.path.join(self.device.working_directory, name)
