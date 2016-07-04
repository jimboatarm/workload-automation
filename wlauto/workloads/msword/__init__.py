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

import glob
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

    Under normal circumstances, this workload should be able to run without a network connection.

    Test description:
    1. The workload runs the following tests:
       - Copying a Microsoft Word file on to the device and adding an image to the file,
         as well as scrolling and navigation tests, and
       - Creating a new file inside the application and performing some editing tasks.
    2. The application is started in offline access mode
    3. For the first test, a simple navigation test is performed on the file, scrolling to the end,
       inserting a shape and an image into the file, modifying their colour and style, and finally
       scrolling back to the beginning of the file.
    4. During the second, a new Microsoft Word file is created on-device from a Newsletter template.
       Some editing is done on the file, including changing text formatting and the title content.
    5. At the end of the test, the automatically saved files are removed from the recent documents
       list and also deleted from the device storage.
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
        Parameter('test_file', kind=str,
                  description='Document to load to the device for testing'),
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
            if self.test_file:
                self.uiauto_params['use_test_file'] = self.use_test_file
                self.uiauto_params['test_file'] = self.test_file
            else:
                raise WorkloadError('Parameter use_test_file is "True" but test_file was not specified')

    def setup(self, context):
        super(MsWord, self).setup(context)
        # If necessary, copy dependencies to the WA dependencies folder
        current_dir = os.path.dirname(__file__)
        deps_dir = self.dependencies_directory
        if not os.path.isfile(os.path.join(deps_dir, self.test_file)):
            filepath = os.path.join(current_dir, self.test_file)
            shutil.copy(filepath, deps_dir)
        if not glob.glob(os.path.join(deps_dir, '*.jpg')):
            for entry in glob.glob(os.path.join(current_dir, '*.jpg')):
                shutil.copy(entry, deps_dir)

        # push existing document
        for entry in os.listdir(deps_dir):
            if entry == self.test_file or entry.endswith('.jpg'):
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
        # delete pushed or created documents
        for entry in self.device.listdir(self.device.working_directory):
            if entry == self.test_file or entry.endswith('.jpg') or regex.search(entry):
                self.device.delete_file(os.path.join(self.device.working_directory, entry))
        # Force a re-index of the mediaserver cache to removed cached files
        self.device.execute('am broadcast -a android.intent.action.MEDIA_MOUNTED -d file:///sdcard')
        # pull logs
        for entry in self.device.listdir(self.device.working_directory):
            if entry.endswith('.log'):
                self.device.pull_file(os.path.join(self.device.working_directory, entry), context.output_directory)
                self.device.delete_file(os.path.join(self.device.working_directory, entry))
