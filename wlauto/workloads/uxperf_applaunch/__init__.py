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

from wlauto import Workload, AndroidBenchmark, UiAutomatorWorkload
from wlauto import Parameter
from wlauto import ExtensionLoader
from wlauto import File
from wlauto.exceptions import ConfigError
from wlauto.utils.android import ApkInfo


class UxperfApplaunch(Workload):
    name = 'uxperfapplaunch'
    description = '''
    Installs and runs a .apk file, waits wait_time_seconds, and tests if the app
    has started successfully.
    '''
    supported_platforms = ['android']

    parameters = [
        Parameter('workload_params', kind=dict, default=False,
                  description="""
                  If ``True``, UX_PERF action markers will be emitted to logcat during
                  the test run.
                  """),
        Parameter('workload_name', kind=str, description='Name of the application package to launch', mandatory=True),
        Parameter('applaunch_type', kind=str, default='warm',
                  description='Choose cold for cold start time or warm for warm start time'),
        Parameter('applaunch_iterations', kind=int, default=1,
                  description='Choose cold for cold start time or warm for warm start time'),
    ]

    def __init__(self, device, **kwargs):
        super(UxperfApplaunch, self).__init__(device, **kwargs)
        loader =  ExtensionLoader()
        self.workload = loader.get_workload(self.workload_name, device, **self.workload_params)

    def validate(self):
        self.workload.validate()
        self.workload.uiauto_params['package'] = self.workload.package
        if self.workload.activity:
            self.workload.uiauto_params['launch_activity'] = self.workload.activity
        else:
            self.workload.uiauto_params['launch_activity'] = "None"
        self.workload.uiauto_params['markers_enabled'] = self.workload.markers_enabled
        self.workload.uiauto_params['applaunch_type'] = self.applaunch_type

    def init_resources(self, context):
        self.workload.init_resources(context)

    def setup(self, context):
        AndroidBenchmark.setup(self.workload,context)
        if self.workload.launch_main == False:
            self.workload.launch_app()

    def stop_application(self, context):
        self.device.execute('am force-stop {}'.format(self.workload.package)) 

    def drop_pagecache(self):
        self.device.execute('sync') 
        self.device.execute('echo 1 > /proc/sys/vm/drop_caches', as_root=True) 
    
    def drop_inodecache(self):
        self.device.execute('sync') 
        self.device.execute('echo 2 > /proc/sys/vm/drop_caches', as_root=True) 
    
    def drop_inodepagecache(self):
        self.device.execute('sync') 
        self.device.execute('echo 3 > /proc/sys/vm/drop_caches',  as_root=True) 
    
    def clean_application(self,context):
        self.stop_application(context)
        if(self.applaunch_type == 'cold_inode'):
            self.drop_inodecache()
        elif(self.applaunch_type == 'cold_page'):
            self.drop_pagecache()
        elif(self.applaunch_type == 'cold_inodepage'):
            self.drop_inodepagecache()
        else:
            raise ConfigError("Applaunch type specified wrong")
            
    def run(self, context):
        self.workload.uiauto_method = "runApplaunchSetup"
        UiAutomatorWorkload.setup(self.workload, context)
        UiAutomatorWorkload.run(self.workload, context)
        #Run iterations of applaunch test
        for i in xrange(self.applaunch_iterations):
            if self.applaunch_type != 'warm':
                self.clean_application(context)
            sleep(10)
            self.workload.uiauto_method = "runApplaunchIteration"
            self.workload.uiauto_params['iteration_count'] = i
            UiAutomatorWorkload.setup(self.workload, context)
            AndroidBenchmark.clean_process(self.workload,context)
            UiAutomatorWorkload.run(self.workload, context)

        AndroidBenchmark.update_result(self.workload,context)

    def teardown(self,context):
        UiAutomatorWorkload.teardown(self.workload, context)
        AndroidBenchmark.teardown(self.workload, context)
        





    
