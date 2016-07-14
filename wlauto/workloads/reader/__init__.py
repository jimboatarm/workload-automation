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
import logging
import re
import time

from wlauto import AndroidUiAutoBenchmark, Parameter
from wlauto.exceptions import DeviceError
from wlauto.exceptions import NotFoundError

__version__ = '0.1.0'


class Reader(AndroidUiAutoBenchmark):

    activity = 'com.adobe.reader.AdobeReader'
    name = 'reader'
    package = 'com.adobe.reader'
    view = [package + '/com.adobe.reader.help.AROnboardingHelpActivity',
            package + '/com.adobe.reader.viewer.ARSplitPaneActivity',
            package + '/com.adobe.reader.viewer.ARViewerActivity']
    description = """
    The Adobe Reader workflow carries out the following typical productivity tasks using
    Workload-Automation.

    Test description:

    1. Open a local file on the device.  The following steps are instrumented:
        1. Select the local files list menu
        2. Select the search button
        2. Search for a specific file from within the list
        3. Open the selected file
    2. Gestures test - measurements of fps, jank and other frame statistics, via dumpsys, are
       captured for the following swipe and pinch gestures:
        1. Swipe down across the central 50% of the screen in 200 x 5ms steps
        2. Swipe up across the central 50% of the screen in 200 x 5ms steps
        3. Swipe right from the edge of the screen in 50 x 5ms steps
        4. Swipe left from the edge of the screen  in 50 x 5ms steps
        5. Pinch out 50% in 100 x 5ms steps
        6. Pinch In 50% in 100 x 5ms steps
    3. Repeat the open file step 1.
    4. Search test - a test measuring the time taken to search a large 100+ page mixed content
       document for specific strings.
        1. Search document_name for first_search_string
        2. Search document_name for second_search_string
    """

    parameters = [
        Parameter('dumpsys_enabled', kind=bool, default=True,
                  description="""
                  If ``True``, dumpsys captures will be carried out during the
                  test run.  The output is piped to log files which are then
                  pulled from the phone.
                  """),
        Parameter('email', kind=str, default="email@gmail.com",
                  description="""
                  Email account used to register with Adobe online services.
                  """),
        Parameter('password', kind=str, default="password",
                  description="""
                  Password for Adobe online services.
                  """),
        Parameter('document_name', kind=str, default="uxperf_test_doc.pdf",
                  description="""
                  The document name to use for the Gesture and Search test.
                  Note: spaces must be replaced with underscores in the document name.
                  """),
        Parameter('first_search_string', kind=str, default="The_quick_brown_fox_jumps_over_the_lazy_dog",
                  description="""
                  The first test string to use for the word search test.
                  Note: Accepts single words only.
                  """),
        Parameter('second_search_string', kind=str, default="TEST_SEARCH_STRING",
                  description="""
                  The second test string to use for the word search test.
                  Note: This must be a single word - spaces will be replaced with underscores
                  """),
    ]

    instrumentation_log = ''.join([name, '_instrumentation.log'])

    def __init__(self, device, **kwargs):
        super(Reader, self).__init__(device, **kwargs)
        self.output_file = os.path.join(self.device.working_directory, self.instrumentation_log)
        self.reader_local_dir = self.device.path.join(self.device.external_storage_directory,
                                                      'Android/data/com.adobe.reader/files/')

    def validate(self):
        super(Reader, self).validate()
        self.uiauto_params['package'] = self.package
        self.uiauto_params['output_dir'] = self.device.working_directory
        self.uiauto_params['output_file'] = self.output_file
        self.uiauto_params['email'] = self.email
        self.uiauto_params['password'] = self.password
        self.uiauto_params['dumpsys_enabled'] = self.dumpsys_enabled
        self.uiauto_params['filename'] = self.document_name.replace(' ', '_')
        self.uiauto_params['first_search_string'] = self.first_search_string.replace(' ', '_')
        self.uiauto_params['second_search_string'] = self.second_search_string.replace(' ', '_')

    def initialize(self, context):
        super(Reader, self).initialize(context)

        if not self.device.is_network_connected():
            raise DeviceError('Network is not connected for device {}'.format(self.device.name))

    def setup(self, context):
        super(Reader, self).setup(context)

        # Check for workload dependencies before proceeding
        pdf_files = [entry for entry in os.listdir(self.dependencies_directory) if entry.endswith(".pdf")]

        if not len(pdf_files):
            raise NotFoundError("Cannot find {} file(s) in {}".format('pdf', self.dependencies_directory))
        else:
            for entry in pdf_files:
                self.device.push_file(os.path.join(self.dependencies_directory, entry),
                                      os.path.join(self.reader_local_dir, entry),
                                      timeout=300)

    def update_result(self, context):
        super(Reader, self).update_result(context)

        self.device.pull_file(self.output_file, context.output_directory)
        result_file = os.path.join(context.output_directory, self.instrumentation_log)

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
        super(Reader, self).teardown(context)

        for entry in self.device.listdir(self.device.working_directory):
            if entry.endswith(".log"):
                self.device.pull_file(os.path.join(self.device.working_directory, entry), context.output_directory)
                self.device.delete_file(os.path.join(self.device.working_directory, entry))

        for entry in self.device.listdir(self.reader_local_dir):
            if entry.endswith(".pdf"):
                self.device.delete_file(os.path.join(self.reader_local_dir, entry))
