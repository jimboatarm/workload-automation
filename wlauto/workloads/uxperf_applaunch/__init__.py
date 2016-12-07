#    Copyright 2015 ARM Limited
#
# Licensed under the Apache License, Version 2.0 (the 'License');
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an 'AS IS' BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# pylint: disable=attribute-defined-outside-init
import os

from time import sleep

from wlauto.common.android.workload import AndroidApplaunchWorkload
from wlauto import Parameter
from wlauto import File
from wlauto.exceptions import ConfigError
from wlauto.utils.android import ApkInfo


class UxperfApplaunch(AndroidUiAutoBenchmark):
    name = 'uxperfapplaunch'
    description = '''
    Installs and runs a .apk file, waits wait_time_seconds, and tests if the app
    has started successfully.
    '''
    supported_platforms = ['android']

    parameters = [
        Parameter('workload_name', kind=str, description='Name of the application package to launch', mandatory=True),
        Parameter('applaunch_type', kind=str, default='warm',
                  description='Choose cold for cold start time or warm for warm start time'),
    ]

    def __init__(self, device, **kwargs):
        super(UxperfApplaunch, self).__init__(device, **kwargs)
        loader =  ExtensionLoader()
        self.workload = loader.get_workload(self.workload_name, device, **kwargs)

    def init_resources(self,context):
        AndroidUiAutoBenchmark.init_resources(self.workload, context)

    def validate(self, device, **kwargs):
        super(UxperfApplaunch, self).validate()
        self.uiauto_params['package'] = self.workload.package
        self.uiauto_params['markers_enabled'] = self.markers_enabled
        self.uiauto_params['applaunch_type'] = self.applaunch_type

    def setup(self, context):
        AndroidBenchmark.setup(self.workload,context)
    
    def applaunch_stop(self, context):
        if self.applaunch_type == 'cold':
            self.device.execute('am force-stop {}'.format(self.workload.package))
        self.device.execute('am kill-all')  # kill all *background* activities
        self.device.clear_logcat()

    def run(self, context):
        self.uiauto_method = "runClearDialogues"
        UiAutomatorWorkload.setup(self.workload, context)
        UiAutomatorWorkload.run(self.workload, context)
        self.applaunch_stop(self, context)
        self.uiauto_method = "runApplaunchIteration"
        #Run iterations of applaunch test
        for i in xrange(self.iterations):
            UiAutomatorWorkload.setup(self.workload, context)
            UiAutomatorWorkload.run(self.workload, context)
            self.applaunch_stop(self, context)








