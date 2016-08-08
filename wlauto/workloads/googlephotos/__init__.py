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
from wlauto.utils.types import list_of_strings

__version__ = '0.1.1'


class Googlephotos(AndroidUiAutoBenchmark):

    name = 'googlephotos'
    package = 'com.google.android.apps.photos'
    activity = 'com.google.android.apps.photos.home.HomeActivity'
    view = [package + '/com.google.android.apps.consumerphotoeditor.fragments.ConsumerPhotoEditorActivity',
            package + '/com.google.android.apps.photos.home.HomeActivity',
            package + '/com.google.android.apps.photos.localmedia.ui.LocalPhotosActivity',
            package + '/com.google.android.apps.photos.onboarding.AccountPickerActivity',
            package + '/com.google.android.apps.photos.onboarding.IntroActivity']
    description = """
    A workload to perform standard productivity tasks with Google Photos.  The workload carries out
    various tasks, such as browsing images, performing zooms, post-processing and saving a selected
    image to file.

    Test description:
    1. Four images are copied to the devices
    2. The application is started in offline access mode
    3. Gestures are performed to swipe between images and pinch zoom in and out of the selected
       image
    4. The Colour of a selected image is edited by selecting the colour menu, incrementing the
       colour, resetting the colour and decrementing the colour using the seek bar.
    5. A Crop test is performed on a selected image.  UiAutomator does not allow the selection of
       the crop markers so the image is tilted positively, reset and then negatively to get a
       similar cropping effect.
    6. A Rotate test is performed on a selected image, rotating anticlockwise 90 degrees, 180
       degrees and 270 degrees.
    """

    default_test_images = [
        'uxperf_1200x1600.png', 'uxperf_1600x1200.jpg',
        'uxperf_2448x3264.png', 'uxperf_3264x2448.jpg',
    ]

    parameters = [
        Parameter('dumpsys_enabled', kind=bool, default=True,
                  description="""
                  If ``True``, dumpsys captures will be carried out during the
                  test run.  The output is piped to log files which are then
                  pulled from the phone.
                  """),
        Parameter('test_images', kind=list_of_strings, default=default_test_images,
                  constraint=lambda x: len(x) >= 4,
                  description='A list of four image files to be pushed to the device.'),
    ]

    instrumentation_log = name + '_instrumentation.log'

    def __init__(self, device, **kwargs):
        super(Googlephotos, self).__init__(device, **kwargs)
        self.output_file = self.path_on_device(self.instrumentation_log)

    def validate(self):
        super(Googlephotos, self).validate()
        self.uiauto_params['package'] = self.package
        self.uiauto_params['output_dir'] = self.device.working_directory
        self.uiauto_params['output_file'] = self.output_file
        self.uiauto_params['dumpsys_enabled'] = self.dumpsys_enabled

    def initialize(self, context):
        super(Googlephotos, self).initialize(context)

        for ff in self.test_images:
            fpath = context.resolver.get(File(self, ff))
            fname = os.path.basename(fpath)  # Ensures correct behaviour in case params are absolute paths
            self.device.push_file(fpath, self.path_on_device(fname), timeout=300)

        # Force a re-index of the mediaserver cache to pick up new files
        self.device.execute('am broadcast -a android.intent.action.MEDIA_MOUNTED -d file:///sdcard')

    def update_result(self, context):
        super(Googlephotos, self).update_result(context)

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
        super(Googlephotos, self).teardown(context)

        regex = re.compile(r'^\w+~\d+\.jpe?g$')

        for entry in self.device.listdir(self.device.working_directory):
            if entry.endswith(".log"):
                self.device.pull_file(self.path_on_device(entry),
                                      context.output_directory)
                self.device.delete_file(self.path_on_device(entry))
            # Clean up edited files on each iteration
            if regex.search(entry):
                self.device.delete_file(self.path_on_device(entry))

        self.device.execute('am broadcast -a android.intent.action.MEDIA_MOUNTED -d file:///sdcard')

    def finalize(self, context):
        super(Googlephotos, self).finalize(context)

        for entry in self.device.listdir(self.device.working_directory):
            if entry.lower() in self.test_images:
                self.device.delete_file(self.path_on_device(entry))

        # Force a re-index of the mediaserver cache to removed cached files
        self.device.execute('am broadcast -a android.intent.action.MEDIA_MOUNTED -d file:///sdcard')

    # Absolute path of the file inside WA working directory
    def path_on_device(self, name):
        return self.device.path.join(self.device.working_directory, name)
