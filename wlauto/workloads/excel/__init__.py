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

__version__ = '0.1.0'


class Excel(AndroidUiAutoBenchmark):

    name = 'excel'
    package = 'com.microsoft.office.excel'
    activity = 'com.microsoft.office.apphost.LaunchActivity'
    view = [package + '/com.microsoft.office.apphost.LaunchActivity',
            package + '/.excelMainActivity']
    description = """
    A workload to perform standard productivity tasks with Microsoft Excel.
    This workload is split into two tests:

    --- create ---
    Prepares a basic workbook consisting of a simple table using formulas to
    sum up two columns. The table rows and columns are then formatted.

    Test description:
    1. Open Microsoft Excel
    2. Dismisses sign in step and uses the app without an account
    3. Create a new spreadsheet (workbook)
    4. Specifies storage location when saving presentation
    5. Chooses a 'blank' template
    6. Inputs row header and formats
    7. Inputs remaining rows and performs a SUM operation on the data
    8. The workbook is renamed

    --- load ---
    Loads a pre-existing test file for performance testing. A root cell in the
    pre-existing test document is used to calculate the remaining cell values
    within the sheet. The root cell value is changed to exercise the device
    during cell recalculation. This test also copies columns rows, searches the
    spreadsheet for a common data value and performs a series of gesture tests.

    Test description:
    1. Open the wa_test.xlsx workbook
    2. Recalculates the sheets cell values based on three root values
    3. Copies a single column from first sheet to second
    4. A search of the current sheet is performed for a preselected value
    5. Gestures are performed to pinch zoom in and out of the workbook

    Note: This test is turned off by default. To run this test it must first be
    enabled in an agenda file by setting 'use_test_file' parameter to True.
    """

    parameters = [
        Parameter('dumpsys_enabled', kind=bool, default=True,
                  description="""
                  If ``True``, dumpsys captures will be carried out during the
                  test run.  The output is piped to log files which are then
                  pulled from the phone.
                  """),
        Parameter('use_test_file', kind=bool, default=False,
                  description="""
                  If ``True``, pushes a preconfigured test file to the device
                  used for measuring performance metrics.
                  """),
    ]

    instrumentation_log = ''.join([name, '_instrumentation.log'])

    def __init__(self, device, **kwargs):
        super(Excel, self).__init__(device, **kwargs)
        self.output_file = self.path_on_device(self.instrumentation_log)
        self.test_file = "uxperf_test_doc.xlsx"

    def validate(self):
        super(Excel, self).validate()
        self.uiauto_params['package'] = self.package
        self.uiauto_params['output_dir'] = self.device.working_directory
        self.uiauto_params['output_file'] = self.output_file
        self.uiauto_params['dumpsys_enabled'] = self.dumpsys_enabled
        self.uiauto_params['use_test_file'] = self.use_test_file
        self.uiauto_params['test_file'] = self.test_file

    def setup(self, context):
        super(Excel, self).setup(context)

        if self.use_test_file:
            fpath = context.resolver.get(File(self, self.test_file))
            self.device.push_file(fpath, self.path_on_device(self.test_file), timeout=300)

    def update_result(self, context):
        super(Excel, self).update_result(context)

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
        super(Excel, self).teardown(context)

        for entry in self.device.listdir(self.device.working_directory):
            if entry.endswith(".log"):
                self.device.pull_file(self.path_on_device(entry),
                                      context.output_directory)
                self.device.delete_file(self.path_on_device(entry))

            # Clean up Excel files on each iteration
            if entry.endswith(".xlsx"):
                self.device.delete_file(self.path_on_device(entry))

    # Absolute path of the file inside WA working directory
    def path_on_device(self, name):
        return self.device.path.join(self.device.working_directory, name)
