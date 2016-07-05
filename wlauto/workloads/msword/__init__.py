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
import shutil

from wlauto import AndroidUiAutoBenchmark, Parameter
from wlauto.exceptions import WorkloadError


__version__ = '0.1.0'

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
                  description='If ``True`` then use a provided test file instead of creating one'),
        Parameter('test_file', kind=str, description='Document to push to the device for testing'),
        Parameter('test_image', kind=str, description='Image to be embedded in the document under test'),
    ]

    instrumentation_log = '{}_instrumentation.log'.format(name)

    def __init__(self, device, **kwargs):
        super(MsWord, self).__init__(device, **kwargs)
        self.run_timeout = 300
        self.output_file = os.path.join(self.device.working_directory, self.instrumentation_log)

    def validate(self):
        super(MsWord, self).validate()
        self.uiauto_params['package'] = self.package
        self.uiauto_params['output_dir'] = self.device.working_directory
        self.uiauto_params['output_file'] = self.output_file
        self.uiauto_params['dumpsys_enabled'] = self.dumpsys_enabled
        if self.use_test_file:
            for param in ['test_file', 'test_image']:
                if not getattr(self, param, None):
                    raise WorkloadError('Parameter use_test_file is "True" but {} was not specified'.format(param))
            if self.test_file:
                self.uiauto_params['use_test_file'] = self.use_test_file
                self.uiauto_params['test_file'] = self.test_file
                self.uiauto_params['test_image'] = self.test_image

    def setup(self, context):
        super(MsWord, self).setup(context)
        # If necessary, copy dependencies to the WA dependencies folder
        current_dir = os.path.dirname(__file__)
        deps_dir = self.dependencies_directory
        if not os.path.isfile(os.path.join(deps_dir, self.test_file)):
            filepath = os.path.join(current_dir, self.test_file)
            shutil.copy(filepath, deps_dir)
        if not os.path.isfile(os.path.join(deps_dir, self.test_image)):
            filepath = os.path.join(current_dir, self.test_image)
            shutil.copy(filepath, deps_dir)

        # push file dependencies to device
        for entry in os.listdir(deps_dir):
            if entry == self.test_file or entry == self.test_image:
                self.device.push_file(os.path.join(deps_dir, entry),
                                      os.path.join(self.device.working_directory, entry),
                                      timeout=60)
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
        # delete pushed or created documents and media
        for entry in self.device.listdir(self.device.working_directory):
            if entry == self.test_file or entry == self.test_image or regex.search(entry):
                self.device.delete_file(os.path.join(self.device.working_directory, entry))
        # Force a re-index of the mediaserver cache to removed cached files
        self.device.execute('am broadcast -a android.intent.action.MEDIA_MOUNTED -d file:///sdcard')
        # pull logs
        for entry in self.device.listdir(self.device.working_directory):
            if entry.endswith('.log'):
                self.device.pull_file(os.path.join(self.device.working_directory, entry), context.output_directory)
                self.device.delete_file(os.path.join(self.device.working_directory, entry))
