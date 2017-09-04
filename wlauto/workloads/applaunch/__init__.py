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

from wlauto import Workload, AndroidBenchmark, AndroidUxPerfWorkload, UiAutomatorWorkload
from wlauto import Parameter
from wlauto import ExtensionLoader
from wlauto import File
from wlauto import settings
from wlauto.exceptions import ConfigError
from wlauto.exceptions import ResourceError
from wlauto.utils.android import ApkInfo
from wlauto.utils.uxperf import UxPerfParser

import wlauto.common.android.resources


class Applaunch(AndroidUxPerfWorkload):

    name = 'applaunch'
    description = '''
    This workload launches and measures the launch time of applications for supporting workloads.

    Currently supported workloads are the ones that implement ``ApplaunchInterface``. For any
    workload to support this workload, it should implement the ``ApplaunchInterface``.
    The corresponding java file of the workload associated with the application being measured
    is executed during the run. The application that needs to be
    measured is passed as a parametre ``workload_name``. The parameters required for that workload
    have to be passed as a dictionary which is captured by the parametre ``workload_params``.
    This information can be obtained by inspecting the workload details of the specific workload.

    The workload allows to run multiple iterations of an application
    launch in two modes:

    1. Launch from background
    2. Launch from long-idle

    These modes are captured as a parameter applaunch_type.

    ``launch_from_background``
        Launches an application after the application is sent to background by
        pressing Home button.

    ``launch_from_long-idle``
        Launches an application after killing an application process and
        clearing all the caches.

    **Test Description:**

    -   During the initialization and setup, the application being launched is launched
        for the first time. The jar file of the workload of the application
        is moved to device at the location ``workdir`` which further implements the methods
        needed to measure the application launch time.

    -   Run phase calls the UiAutomator of the applaunch which runs in two subphases.
            A.  Applaunch Setup Run:
                    During this phase, welcome screens and dialogues during the first launch
                    of the instrumented application are cleared.
            B.  Applaunch Metric Run:
                    During this phase, the application is launched multiple times determined by
                    the iteration number specified by the parametre ``applaunch_iterations``.
                    Each of these iterations are instrumented to capture the launch time taken
                    and the values are recorded as UXPERF marker values in logfile.
    '''
    supported_platforms = ['android']

    parameters = [
        Parameter('workload_name', kind=str,
                  description='Name of the uxperf workload to launch',
                  default='gmail'),
        Parameter('workload_params', kind=dict, default={},
                  description="""
                  parameters of the uxperf workload whose application launch
                  time is measured
                  """),
        Parameter('applaunch_type', kind=str, default='launch_from_background',
                  allowed_values=['launch_from_background', 'launch_from_long-idle'],
                  description="""
                  Choose launch_from_long-idle for measuring launch time
                  from long-idle. These two types are described in the class
                  description.
                  """),
        Parameter('applaunch_iterations', kind=int, default=1,
                  description="""
                  Number of iterations of the application launch
                  """),
        Parameter('report_results', kind=bool, default=True,
                  description="""
                  Choose to report results of the application launch time.
                  """),
    ]

    def init_resources(self, context):
        super(Applaunch, self).init_resources(context)
        loader = ExtensionLoader(packages=settings.extension_packages, paths=settings.extension_paths)
        self.workload_params['markers_enabled'] = True
        self.workload = loader.get_workload(self.workload_name, self.device,
                                            **self.workload_params)
        self.init_workload_resources(context)
        self.package = self.workload.package

    def init_workload_resources(self, context):
        self.workload.uiauto_file = context.resolver.get(wlauto.common.android.resources.ApkFile(self.workload, uiauto=True))
        if not self.workload.uiauto_file:
            raise ResourceError('No UI automation Uiauto APK file found for workload {}.'.format(self.workload.name))
        self.workload.device_uiauto_file = self.device.path.join(self.device.working_directory, os.path.basename(self.workload.uiauto_file))
        if not self.workload.uiauto_package:
            self.workload.uiauto_package = os.path.splitext(os.path.basename(self.workload.uiauto_file))[0]

    def validate(self):
        super(Applaunch, self).validate()
        self.workload.validate()
        self.pass_parameters()

    def pass_parameters(self):
        self.uiauto_params['workload'] = self.workload.name
        self.uiauto_params['package_name'] = self.workload.package
        self.uiauto_params.update(self.workload.uiauto_params)
        if self.workload.activity:
            self.uiauto_params['launch_activity'] = self.workload.activity
        else:
            self.uiauto_params['launch_activity'] = "None"
        self.uiauto_params['applaunch_type'] = self.applaunch_type
        self.uiauto_params['applaunch_iterations'] = self.applaunch_iterations

    def setup(self, context):
        self.ux_setup()
        AndroidBenchmark.setup(self.workload, context)
        if not self.workload.launch_main:
            self.workload.launch_app()
        UiAutomatorWorkload.setup(self, context)
        self.workload.device.push_file(self.workload.uiauto_file, self.workload.device_uiauto_file)

    def run(self, context):
        UiAutomatorWorkload.run(self, context)

    def update_result(self, context):
        super(Applaunch, self).update_result(context)
        if self.report_results:
            parser = UxPerfParser(context, prefix='applaunch_')
            logfile = os.path.join(context.output_directory, 'logcat.log')
            parser.parse(logfile)
            parser.add_action_timings()

    def teardown(self, context):
        super(Applaunch, self).teardown(context)
        AndroidBenchmark.teardown(self.workload, context)
        UiAutomatorWorkload.teardown(self.workload, context)
        self.ux_teardown(context)

    #####################
    # CODE BELOW IS WIP #
    #####################
    # Due to running multiple times for sets of 6 pmu counters,
    # the workload can run considerably longer than normal.
    # Increase timeout of WA to not fail prematurely
    run_timeout = 60 * 60
    # One of the following: ['UTIL', 'PIDS', 'TIDS', 'ATRACE', 'GATOR']
    # If anything else/nothing, will perform no uxTracing methods
    # UTIL - Find the process and thread names by running 'top'
    # PIDS - Collect pmu counters by using 'perf', across a range of PIDs
    # TIDS - Collect pmu counters by using 'perf', across a range of TIDs
    # ATRACE - Collect systrace captures by using 'atrace'
    # GATOR - Perform a Streamline capture on device
    uxTracing = 'NONE'
    # On a slow system, e.g. 1 little core only, a simple applaunch may take as long as 10 seconds
    uxTimer = 10
    # A list of pmu events for different types of cores
    # little:A53, big:A73
    pmuCounters = {'little': ['r11', 'r8', 'r12', 'r10', 'r14',
                              'r1', 'r4', 'r3', 'r16', 'r17',
                              're1', 're0', 're4', 're5', 're6',
                              're7', 're8'],
                   'big': ['r11', 'r8', 'r12', 'r10', 'r14',
                           'r1', 'r4', 'r3', 'r16', 'r17',
                           'r2', 'r5', 'r15', 'r19', 'r40',
                           'r41', 'r50', 'r51', 'r56', 'r57',
                           'r58', 'rc0', 'rc1', 'rd3', 'rd8',
                           'rd9', 'rda']}
    # A list of the main PID names for each launchtype and app
    pidTargets = {'launch_from_long-idle':
                  {'adobereader': ['system_server', 'com.google.android.googlequicksearchbox:search', 'com.adobe.reader', 'com.android.systemui', 'com.android.vending'],
                   'googleplaybooks': ['system_server', 'com.google.android.googlequicksearchbox:search', 'com.google.android.apps.books'],
                   'googlephotos': ['system_server', 'com.google.android.googlequicksearchbox:search', 'com.google.android.apps.photos']}}
    # A list of the main TID names for each launchtype and app
    # Format: <tid name exactly 15 characters long - space padded if neccessary><pid name as above>
    tidTargets = {'launch_from_long-idle':
                  {'adobereader': ['HeapTaskDaemon com.adobe.reader', 'Jit thread poolcom.adobe.reader', 'RenderThread   com.adobe.reader', 'om.adobe.readercom.adobe.reader', 'pool-4-thread-1com.adobe.reader', 'pool-4-thread-2com.adobe.reader', 'RenderThread   com.android.systemui', 'ndroid.systemuicom.android.systemui', 'android.vendingcom.android.vending', 'HeapTaskDaemon com.google.android.googlequicksearchbox:search', 'HeapTaskDaemon system_server', 'android.bg     system_server'],
                   'googleplaybooks': ['AsyncTask #1   com.google.android.apps.books', 'BooksImageManagcom.google.android.apps.books', 'HeapTaskDaemon com.google.android.apps.books', 'Jit thread poolcom.google.android.apps.books', 'pool-1-thread-1com.google.android.apps.books', 'roid.apps.bookscom.google.android.apps.books', 'gle.android.gmscom.google.android.gms', 'HeapTaskDaemon com.google.android.googlequicksearchbox:search', 'HeapTaskDaemon system_server', 'android.displaysystem_server'],
                   'googlephotos': ['Jit thread poolcom.google.android.apps.photos', 'RenderThread   com.google.android.apps.photos', 'oid.apps.photoscom.google.android.apps.photos', 'HeapTaskDaemon com.google.android.googlequicksearchbox:search', 'HeapTaskDaemon system_server', 'android.bg     system_server', 'android.displaysystem_server']}}

    def ux_setup(self):
        # Setup device into test mode
        if 'set_test_mode' in dir(self.device):
            self.device.set_test_mode()
        # Clean and create output directory on device
        results = self.device.path.join(self.device.working_directory, 'uxtracing')
        self.device.delete_file(results)
        self.device.execute('mkdir -p ' + results)
        if self.uxTracing == 'PIDS':
            self.uiauto_params['pmu_targets'] = self.pidTargets[self.applaunch_type][self.workload_name]
        elif self.uxTracing == 'TIDS':
            self.uiauto_params['pmu_targets'] = self.tidTargets[self.applaunch_type][self.workload_name]
        self.uiauto_params['uxperf_timer'] = self.uxTimer
        self.uiauto_params['uxperf_tracing'] = self.uxTracing
        # PMU counters start multiplexing after 6 counters, which we dont want to happen
        # It has been observed for some processes, even 6 can fail
        # Thus split the list of counters into groups of 5
        pmuList = self.pmuCounters['big']
        pmuList = [','.join(x) for x in (pmuList[i:i + 5] for i in range(0, len(pmuList), 5))]
        self.uiauto_params['pmu_counters'] = pmuList

    def ux_teardown(self, context):
        # Setup device back to normal
        if 'set_normal_mode' in dir(self.device):
            self.device.set_normal_mode()
        # Pull results
        results = self.device.path.join(self.device.working_directory, 'uxtracing')
        self.device.pull_file(results, context.output_directory)
        # Special extra stuff for systrace files
        resultdir = os.path.join(context.output_directory, 'uxtracing')
        for f in os.listdir(resultdir):
            if ('atrace' in f) and f.endswith('.txt'):
                spath = os.path.join(os.environ['ANDROID_HOME'], 'platform-tools', 'systrace', 'systrace.py')
                sfile = os.path.join(resultdir, f)
                shtml = os.path.splitext(sfile)[0] + '.html'
                os.system('{} --from-file {} -o {}'.format(spath, sfile, shtml))
    #####################
