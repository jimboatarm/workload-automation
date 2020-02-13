from wa import Parameter, Workload, File
from wa.utils.types import list_or_string
from dateutil.parser import parse
import datetime
import math
import os
import time

SPEC_TARGET_PATH_BASE = '/data/local/tmp/spec2k6'

TARGET_BIN_DIRECTORY = '/data/local/tmp/bin'

TARGET_OUTPUT_DIRECTORY = '/sdcard/devlib-target'

OUTPUT_FOLDER = 'spec_output' 

BASE_MACHINE_REFERENCE_TIMES = {'400.perlbench' : 9770, '401.bzip2' : 9650,
                                '403.gcc' : 8050, '429.mcf' : 9120, '445.gobmk' : 10490,
                                '456.hmmer' : 9330, '458.sjeng' : 12100,
                                '462.libquantum' : 20720, '464.h264ref' : 22130,
                                '471.omnetpp' : 6250, '473.astar' : 7020,
                                '483.xalancbmk' : 6900, '410.bwaves' : 13590,
                                '416.gamess' : 19580, '433.milc' : 9180, '434.zeusmp' : 9100,
                                '435.gromacs' : 7140, '436.cactusADM' : 11950,
                                '437.leslie3d' : 9400, '444.namd' : 8020, '447.dealII' : 11440,
                                '450.soplex' : 8340, '453.povray' : 5320, '454.calculix' : 8250,
                                '459.GemsFDTD' : 10610, '465.tonto' : 9840, '470.lbm' : 13740,
                                '481.wrf' : 11170, '482.sphinx3' : 19490}

SPEC_INT_TESTS = ['400.perlbench',
                  '401.bzip2',
                  '403.gcc',
                  '429.mcf',
                  '445.gobmk',
                  '456.hmmer',
                  '458.sjeng',
                  '462.libquantum',
                  '464.h264ref',
                  '471.omnetpp',
                  '473.astar',
                  '483.xalancbmk']

SPEC_FP_TESTS = ['410.bwaves',
                 '416.gamess',
                 '433.milc',
                 '434.zeusmp',
                 '435.gromacs',
                 '436.cactusADM',
                 '437.leslie3d',
                 '444.namd',
                 '447.dealII',
                 '450.soplex',
                 '453.povray',
                 '454.calculix',
                 '459.GemsFDTD',
                 '465.tonto',
                 '470.lbm',
                 '481.wrf',
                 '482.sphinx3']

ALL_TESTS = SPEC_INT_TESTS + SPEC_FP_TESTS

ALLOWED_TEST_NAMES = ['all', 'spec_int', 'spec_fp'] + ALL_TESTS

class Spec2006(Workload):

    name = 'spec2006'
    description = '''
    Workload to install and run spec2006. 
    
    SPEC CPU 2006 is a benchmarking test suite. Information can be found at https://www.spec.org/cpu2006/
    
    The suite contains two sets of benchmarks: SPECint and SPECfp. SPECint and Specfp can be run by entering spec_int and spec_fp under test_names in agenda.
    
    A ratio is calculated for each test in the 2006 suite based on time taken to complete test against a base machine refernace time.
    
    A goup score is calculated by taking the geometric mean of the subtest ratio scores for spec_int and spec_fp. The workload will only calculate the group
    scores if there are 5 or more succesful subtest runs for the group.

    Tests can be run in speed mode or throughput mode. Speed runs tests accross all available cpu's, throughput runs a seperate instance on each pnline cpu at the same time.

    It is recommended to use spec2006 version 1.2
    '''

    parameters = [
        Parameter('build_name', kind=str, mandatory=False,
                  description='Build name of spec2k6 binary. Builds can be found at '),
        Parameter('test_names', kind=list_or_string, mandatory=True, allowed_values=ALLOWED_TEST_NAMES,
        		  description='Use this parameter to define which tests to run from the cpu2006 suite'),
        Parameter('run_type', kind=str, default='speed', allowed_values=['speed', 'throughput'],
        	       description='run_type to set the tests running in speed or throughput(rate) mode')
    ]

    def __init__(self, target, **kwargs):
        super(Spec2006, self).__init__(target, **kwargs)
        if isinstance(self.test_names, str):
            self.test_names = [self.test_names]

        if 'all' in self.test_names:
        	self.test_names = ALL_TESTS
        elif 'spec_int' in self.test_names:
        	self.test_names = SPEC_INT_TESTS
        elif 'spec_fp' in self.test_names:
        	self.test_names = SPEC_FP_TESTS

    def init_resources(self, resolver):
        super(Spec2006, self).init_resources(resolver)

    def initialize(self, context):
        super(Spec2006, self).initialize(context)
        
        if self.run_type == 'speed':
            self.spec_runner = SpecRunnerSpeed(self.test_names, self.logger)
        else:
            # Convert to type string to ensure correct bitmask gets set in invoke_background()
            online_cpus = [str(cpu) for cpu in self.target.list_online_cpus()]
            self.spec_runner = SpecRunnerThroughput(self.test_names, self.logger, online_cpus)

    def setup(self, context):
        super(Spec2006, self).setup(context)
        self.spec_runner.setup(self.build_name, self.target, context, self)

    def run(self, context):
        super(Spec2006, self).run(context)
        self.spec_runner.run(self.target)

    def extract_results(self, context):
        super(Spec2006, self).extract_results(context)
        self.spec_runner.extract_results(self.target, context)
   
    def update_output(self, context):
        super(Spec2006, self).update_output(context)
        self.spec_runner.update_output(context)

    def teardown(self, context):
        super(Spec2006, self).teardown(context)
        self.spec_runner.finish(self.target)

class SpecRunner():
    
    def __init__(self, tests, logger):
        self.tests = tests
        self.logger = logger
        self.name = 'SpecRunner'
        self.incomplete_tests = []
        self.spec_int_scores = []
        self.spec_fp_scores = []


    def setup(self, build_name, target, context, resource_owner):
        resource = File(resource_owner, "script/run_spec_2006.sh")
        host_executable = context.get_resource(resource)
        self.run_spec_script = target.install(host_executable)
        
        # Clean output directory on target
        if OUTPUT_FOLDER in target.list_directory(TARGET_OUTPUT_DIRECTORY):
            target.execute('cd {} && rm -r {}'.format(TARGET_OUTPUT_DIRECTORY, OUTPUT_FOLDER))
        target.execute('cd {} && mkdir {}'.format(TARGET_OUTPUT_DIRECTORY, OUTPUT_FOLDER))

        resource = File(resource_owner, build_name)
        spec2k6_binaries = context.get_resource(resource)
        
        #Remove base directory if it already exists
        if 'spec2k6' in target.list_directory('data/local/tmp'):
            command = 'rm -rf {}'.format(SPEC_TARGET_PATH_BASE)
            target.execute(command, as_root=target.is_rooted)

        #Install spec2k6 on device
        target.execute('cd /data/local/tmp && mkdir spec2k6') 
        for test in self.tests:
            test_run_folder = os.path.join(spec2k6_binaries, 'spec2006','benchspec', 'CPU2006' , test)
            if not os.path.exists(test_run_folder):
                test_run_folder = os.path.join(spec2k6_binaries, 'benchspec', 'CPU2006' , test)
            if not os.path.exists(test_run_folder):
                test_run_folder = os.path.join(spec2k6_binaries, test)
            if not os.path.exists(test_run_folder):
            	continue
            self.logger.info('Copying {} to device'.format(test))
            test_run_folder_target_dir = os.path.join(SPEC_TARGET_PATH_BASE, test)
            target.push(test_run_folder, test_run_folder_target_dir, timeout=300)

    def run(self, target):
        pass

    def extract_results(self, target, context):
        output_folder = os.path.join(TARGET_OUTPUT_DIRECTORY, 'spec_output')
        host_output_folder = os.path.join(context.output_directory, 'spec_output')
        target.pull(output_folder, host_output_folder, timeout=60)
        for test_name in self.tests:
            if test_name in self.incomplete_tests:
                continue
            for file in os.listdir(os.path.join(host_output_folder, test_name)):
                if 'ref' in file and 'err' in file:
                    if os.stat(os.path.join(host_output_folder, test_name, file)).st_size != 0:
                        self.logger.warning('errors were found during run in {}'.format(file))
                        if test_name not in self.incomplete_tests:
                            self.incomplete_tests.append(test_name)

    def finish(self, target):
        target.execute('cd {} && rm -r {}'.format(TARGET_OUTPUT_DIRECTORY, OUTPUT_FOLDER))
        
        if len(self.incomplete_tests) > 0:
            self.logger.warning('The following tests did not run correctly:{}'.format(self.incomplete_tests))

    def _does_run_folder_exist(self, target, test_name):
        test_dir = os.path.join(SPEC_TARGET_PATH_BASE, test_name)
        folders_in_test_dir = target.list_directory(test_dir)
        if 'run' not in folders_in_test_dir:
            return False
        return True

    def _does_test_folder_exist(self, target, test_name):
        folders_in_test_dir = target.list_directory(SPEC_TARGET_PATH_BASE)
        if test_name not in folders_in_test_dir:
            return False
        return True

    def _calculate_group_benchmark_scores(self, context, spec_int_scores, spec_fp_scores):
        # Calculate and add benchmark group scores if number of complete tests >= 5 
        if len(spec_int_scores) >= 5:
            group_score = self._calculate_group_benchmark_score(spec_int_scores)
            context.add_metric('spec_int_score', group_score, 'ratio_score')
        if len(spec_fp_scores) >=5:
            group_score = self._calculate_group_benchmark_score(spec_fp_scores)
            context.add_metric('spec_fp_score', group_score, 'ratio_score')

    @staticmethod
    def _calculate_group_benchmark_score(benchmark_ratios):
        # Calculate geometric mean
        return math.exp(math.fsum(math.log(benchmark_ratio) for benchmark_ratio in benchmark_ratios) / len(benchmark_ratios))

class SpecRunnerSpeed(SpecRunner):

    def __init__(self, tests, logger):
        super().__init__(tests, logger)
    
    def run(self, target):
        super().run(target)
        for test_name in self.tests:
            self.logger.info('*****RUNNING******: ' + test_name)
            if not self._does_test_folder_exist(target, test_name) or not self._does_run_folder_exist(target, test_name):
                self.logger.warning('Test folder does not exist for {}......skipping'.format(test_name))
                self.incomplete_tests.append(test_name)
                continue
            target.execute('cd {} && mkdir {}'.format(os.path.join(TARGET_OUTPUT_DIRECTORY, OUTPUT_FOLDER), test_name))
            test_target_output_dir = os.path.join(TARGET_OUTPUT_DIRECTORY, OUTPUT_FOLDER, test_name)
            timing_output_file_path = os.path.join(test_target_output_dir, 'timing.txt')
            command = 'sh {} {} {} 2>&1 | tee {}'.format(self.run_spec_script, test_name, test_target_output_dir, timing_output_file_path)
            target.execute(command, as_root=target.is_rooted)

    def update_output(self, context):
        for test_name in self.tests:
            if test_name in self.incomplete_tests:
                continue
            timings_file = os.path.join(context.output_directory, OUTPUT_FOLDER, test_name, 'timing.txt')
            self._parse_timings_file(context, timings_file, test_name)
        self._calculate_group_benchmark_scores(context, self.spec_int_scores, self.spec_fp_scores)

    def _parse_timings_file(self, context, filepath, test_name):
        total_time = datetime.timedelta()
        with open(filepath) as fh:
            for line in fh:
                if not 'real' in line:
                    continue
                elapsed_time = parse(line.split('real')[0].strip()).time()
                (h, m, s) = elapsed_time.strftime('%H:%M:%S.%f').split(':')
                total_time += datetime.timedelta(hours=int(h), minutes=int(m), seconds=float(s))
        classifiers = {'run_time_seconds' : total_time.seconds, 'run_type' : 'speed'}

        if total_time.seconds == 0:
            self.incomplete_tests.append(test_name)
            return

        benchmark_ratio = self._calculate_test_benchmark_ratio(test_name, total_time)
        context.add_metric(test_name, benchmark_ratio, 'ratio_score', classifiers=classifiers)

        if test_name in SPEC_INT_TESTS:
            self.spec_int_scores.append(benchmark_ratio)
        else:
            self.spec_fp_scores.append(benchmark_ratio)

    @staticmethod
    def _calculate_test_benchmark_ratio(test_name, elapsed_time):
        # Calculate ratio based off reference machine times
        return round(BASE_MACHINE_REFERENCE_TIMES[test_name] / elapsed_time.seconds, 4)

class SpecRunnerThroughput(SpecRunner):

    def __init__(self, tests, logger, online_cpus):
        super().__init__(tests, logger)
        self.online_cpus = online_cpus
        #TODO: Tune expected wait time for each test
        self.EXPECTED_WAIT_TIME = {'400.perlbench' : 502, '401.bzip2' : 360,
                                   '403.gcc' : 8050, '429.mcf' : 9120, '445.gobmk' : 10490,
                                   '456.hmmer' : 9330, '458.sjeng' : 12100,
                                   '462.libquantum' : 20720, '464.h264ref' : 22130,
                                   '471.omnetpp' : 6250, '473.astar' : 7020,
                                   '483.xalancbmk' : 6900, '410.bwaves' : 13590,
                                   '416.gamess' : 19580, '433.milc' : 9180, '434.zeusmp' : 9100,
                                   '435.gromacs' : 7140, '436.cactusADM' : 11950,
                                   '437.leslie3d' : 9400, '444.namd' : 8020, '447.dealII' : 11440,
                                   '450.soplex' : 8340, '453.povray' : 5320, '454.calculix' : 8250,
                                   '459.GemsFDTD' : 10610, '465.tonto' : 9840, '470.lbm' : 13740,
                                   '481.wrf' : 11170, '482.sphinx3' : 19490}

    def run(self, target):
        for test_name in self.tests:
            self.logger.info('*****RUNNING******: ' + test_name)
            if not self._does_test_folder_exist(target, test_name) or not self._does_run_folder_exist(target, test_name):
                self.logger.warning('Test folder does not exist for {}......skipping'.format(test_name))
                self.incomplete_tests.append(test_name)
                continue
            target.execute('cd {} && mkdir {}'.format(os.path.join(TARGET_OUTPUT_DIRECTORY, OUTPUT_FOLDER), test_name))
            test_target_output_dir = os.path.join(TARGET_OUTPUT_DIRECTORY, OUTPUT_FOLDER, test_name)
            for cpu in self.online_cpus:
                output_file_path = os.path.join(TARGET_OUTPUT_DIRECTORY, OUTPUT_FOLDER, test_name, 'cpu_{}_timing.txt'.format(cpu))
                command = 'sh {} {} {} {} 2>&1 | tee {}'.format(self.run_spec_script, test_name, test_target_output_dir, cpu, output_file_path)
                target.background_invoke(command, on_cpus=cpu)
            #time.sleep(2400) # TODO: Sleep for expected wait time once gathered for each test
            is_running = True
            while is_running:
                time.sleep(60)
                open_timing_files = target.execute('lsof | grep _timing.txt', will_succeed=False, check_exit_code=False)
                if len(open_timing_files.splitlines()) <= 0:
                    is_running = False

    def update_output(self, context):
        for test_name in self.tests:
            if test_name in self.incomplete_tests:
                continue
            longest_elapsed_time = datetime.timedelta()
            for cpu in self.online_cpus:
                total_time_elapsed_for_cpu = datetime.timedelta()
                host_outfile = os.path.join(context.output_directory, OUTPUT_FOLDER, test_name, 'cpu_{}_timing.txt'.format(cpu))
                with open(host_outfile) as fh:
                    for line in fh:
                        if not 'real' in line:
                            continue
                        elapsed_time = parse(line.split('real')[0].strip()).time()
                        (h, m, s) = elapsed_time.strftime('%H:%M:%S.%f').split(':')
                        total_time_elapsed_for_cpu += datetime.timedelta(hours=int(h), minutes=int(m), seconds=float(s))
                if total_time_elapsed_for_cpu > longest_elapsed_time:
                    longest_elapsed_time = total_time_elapsed_for_cpu

            if longest_elapsed_time.seconds == 0:
                self.incomplete_tests.append(test_name)
                return

            classifiers = {'run_time_seconds' : longest_elapsed_time.seconds, 'run_type' : 'throughput'}
            benchmark_ratio = self._calculate_test_benchmark_ratio(test_name, longest_elapsed_time, len(self.online_cpus))
            context.add_metric(test_name, benchmark_ratio, 'ratio_score', classifiers=classifiers)

            if test_name in SPEC_INT_TESTS:
                self.spec_int_scores.append(benchmark_ratio)
            else:
                self.spec_fp_scores.append(benchmark_ratio)
        self._calculate_group_benchmark_scores(context, self.spec_int_scores, self.spec_fp_scores)

    @staticmethod
    def _calculate_test_benchmark_ratio(test_name, elapsed_time, number_cpus):
        # Calculate ratio based off reference machine times
        return round(number_cpus * (BASE_MACHINE_REFERENCE_TIMES[test_name] / elapsed_time.seconds), 4)
        

