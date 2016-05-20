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
import os.path as path
import re
from wlauto import AndroidUiAutoBenchmark, Parameter
from wlauto.utils.types import list_of_strings


def log_method(workload, name):
    workload.logger.info('===== {}() ======'.format(name))

class GoogleSlides(AndroidUiAutoBenchmark):

    name = 'googleslides'
    package = 'com.google.android.apps.docs.editors.slides'
    description = 'Creates a Google Slides presentation with some commonly used features'
    activity = ''

    # Views for FPS instrumentation
    view = [
        package + '/com.qo.android.quickpoint.Quickpoint',
        package + '/com.google.android.apps.docs.app.DocListActivity',
        package + '/com.google.android.apps.docs.welcome.warmwelcome.TrackingWelcomeActivity',
        package + '/com.google.android.apps.docs.app.NewMainProxyActivity',
    ]

    parameters = [
        Parameter('dumpsys_enabled', kind=bool, default=True,
                  description='''
                  If ``True``, dumpsys captures will be carried out during the test run.
                  The output is piped to log files which are then pulled from the phone.
                  '''),
        Parameter('local_file', kind=str,
                  description='''
                  If specified, the workload will push the PowerPoint file to be used for
                  testing on the device. Otherwise, a file will be created inside the app.
                  '''),
        Parameter('slide_count', kind=int, default=5,
                  description='''
                  Number of slides in aforementioned local file. Determines number of
                  swipe actions when playing slide show.
                  '''),
    ]

    instrumentation_log = '{}_instrumentation.log'.format(name)

    def __init__(self, device, **kwargs):
        super(GoogleSlides, self).__init__(device, **kwargs)
        self.run_timeout = 300
        self.output_file = path.join(self.device.working_directory, self.instrumentation_log)
        self.local_dir = self.dependencies_directory
        # Android downloads folder
        self.device_dir = path.join(self.device.working_directory, '..', 'Download')
        self.wa_test_file = 'wa_test_' + self.local_file

    def validate(self):
        log_method(self, 'validate')
        super(GoogleSlides, self).validate()
        self.uiauto_params['dumpsys_enabled'] = self.dumpsys_enabled
        self.uiauto_params['output_dir'] = self.device.working_directory
        self.uiauto_params['results_file'] = self.output_file
        if self.local_file:
            self.uiauto_params['local_file'] = self.wa_test_file
            self.uiauto_params['slide_count'] = self.slide_count

    def initialize(self, context):
        log_method(self, 'initialize')
        super(GoogleSlides, self).initialize(context)
        if self.local_file:
            # push local PPT file
            for entry in os.listdir(self.local_dir):
                if entry is self.local_file:
                    self.device.push_file(path.join(self.local_dir, self.local_file),
                                          path.join(self.device_dir, self.wa_test_file),
                                          timeout=60)

    def setup(self, context):
        log_method(self, 'setup')
        super(GoogleSlides, self).setup(context)

    def run(self, context):
        log_method(self, 'run')
        super(GoogleSlides, self).run(context)

    def update_result(self, context):
        log_method(self, 'update_result')
        super(GoogleSlides, self).update_result(context)
        self.get_metrics(context)

    def teardown(self, context):
        log_method(self, 'teardown')
        super(GoogleSlides, self).teardown(context)
        self.pull_logs(context)

    def finalize(self, context):
        log_method(self, 'finalize')
        super(GoogleSlides, self).finalize(context)
        if self.local_file:
            # delete pushed PPT file
            for entry in self.device.listdir(self.device_dir):
                if entry is self.wa_test_file:
                    self.device.delete_file(path.join(self.device_dir, entry))

    def wa_filename(self, filename):
        return self.file_prefix + filename

    def get_metrics(self, context):
        self.device.pull_file(self.output_file, context.output_directory)
        metrics_file = path.join(context.output_directory, self.instrumentation_log)
        with open(metrics_file, 'r') as wfh:
            regex = re.compile(r'(?P<key>\w+)\s+(?P<value1>\d+)\s+(?P<value2>\d+)\s+(?P<value3>\d+)')
            for line in wfh:
                match = regex.search(line)
                if match:
                    context.result.add_metric(match.group('key') + "_start",
                                              match.group('value1'), units='ms')
                    context.result.add_metric(match.group('key') + "_finish",
                                              match.group('value2'), units='ms')
                    context.result.add_metric(match.group('key') + "_duration",
                                              match.group('value3'), units='ms')

    def pull_logs(self, context):
        wd = self.device.working_directory
        for entry in self.device.listdir(wd):
            if entry.startswith(self.name) and entry.endswith('.log'):
                self.device.pull_file(path.join(wd, entry), context.output_directory)
                self.device.delete_file(path.join(wd, entry))
