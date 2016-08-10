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


class Gmail(AndroidUiAutoBenchmark):

    name = 'gmail'
    package = 'com.google.android.gm'
    activity = ''
    view = [package + '/com.google.android.gm.ConversationListActivityGmail',
            package + '/com.google.android.gm.ComposeActivityGmail']
    description = """
    A workload to perform standard productivity tasks within Gmail.  The workload carries out
    various tasks, such as creating new emails and sending them, whilst also producing metrics for
    action completion times.

    Test description:

    1. Open Gmail application
    2. Click to create New mail
    3. Enter recipient details in the To: field
    4. Enter text in the Subject edit box
    5. Enter text in the Compose edit box
    6. Attach five images from the local Images folder to the email
    7. Click the Send mail button
    """

    default_test_images = [
        'uxperf_1200x1600.png', 'uxperf_1600x1200.jpg',
        'uxperf_2448x3264.png', 'uxperf_3264x2448.jpg',
    ]

    parameters = [
        Parameter('recipient', kind=str, mandatory=True,
                  description=""""
                  The email address of the recipient.  Setting a void address
                  will stop any mesage failures clogging up your device inbox
                  """),
        Parameter('dumpsys_enabled', kind=bool, default=True,
                  description="""
                  If ``True``, dumpsys captures will be carried out during the
                  test run.  The output is piped to log files which are then
                  pulled from the phone.
                  """),
        Parameter('test_images', kind=list_of_strings, default=default_test_images,
                  constraint=lambda x: len(x) >= 4,
                  description="""
                  A list of four image files to be pushed to the device.
                  These will be attached to an email as part of the test.
                  """),
    ]

    instrumentation_log = name + '_instrumentation.log'

    def __init__(self, device, **kwargs):
        super(Gmail, self).__init__(device, **kwargs)
        self.uiauto_params['recipient'] = self.recipient
        self.output_file = self.path_on_device(self.instrumentation_log)

    def validate(self):
        super(Gmail, self).validate()
        self.uiauto_params['package'] = self.package
        self.uiauto_params['output_dir'] = self.device.working_directory
        self.uiauto_params['output_file'] = self.output_file
        self.uiauto_params['dumpsys_enabled'] = self.dumpsys_enabled
        self.uiauto_params['number_of_images'] = len(self.test_images)

    def initialize(self, context):
        super(Gmail, self).initialize(context)

        # This workload relies on the internet so check that there is a working internet connection
        if not self.device.is_network_connected():
            raise DeviceError('Network is not connected for device {}'.format(self.device.name))

        # Check for workload dependencies before proceeding
        for ff in self.test_images:
            fpath = context.resolver.get(File(self, ff))
            fname = os.path.basename(fpath)  # Ensures correct behaviour in case params are absolute paths
            self.device.push_file(fpath, self.path_on_device(fname), timeout=300)

        # Force a re-index of the mediaserver cache to pick up new files
        self.device.execute('am broadcast -a android.intent.action.MEDIA_MOUNTED -d file:///sdcard')

    def update_result(self, context):
        super(Gmail, self).update_result(context)

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
        super(Gmail, self).teardown(context)

        for entry in self.device.listdir(self.device.working_directory):
            if entry.endswith(".log"):
                self.device.pull_file(self.path_on_device(entry), context.output_directory)
                self.device.delete_file(self.path_on_device(entry))

    def finalize(self, context):
        super(Gmail, self).finalize(context)

        for entry in self.device.listdir(self.device.working_directory):
            if entry.lower().endswith(('.jpg', '.jpeg')):
                self.device.delete_file(self.path_on_device(entry))

        # Force a re-index of the mediaserver cache to pick up new files
        self.device.execute('am broadcast -a android.intent.action.MEDIA_MOUNTED -d file:///sdcard')

    # Absolute path of the file inside WA working directory
    def path_on_device(self, name):
        return self.device.path.join(self.device.working_directory, name)
