#    Copyright 2014-2018 ARM Limited
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

from wa import ApkUiautoWorkload, Parameter
from wa.framework.exception import ValidationError, WorkloadError
from wa.utils.types import list_of_strs
from wa.utils.misc import unique


class Jetstream(ApkUiautoWorkload):

    name = 'jetstream'
    package_names = ['com.android.chrome']
    regex = re.compile(r'Jetstream Score ([\d.]+)')
    tests = ['benchmark-3d-cube-SP', 'benchmark-3d-raytrace-SP', 'benchmark-acorn-wtb', 'benchmark-ai-astar', 'benchmark-Air', 'benchmark-async-fs', 'benchmark-Babylon', 'benchmark-babylon-wtb',
             'benchmark-base64-SP', 'benchmark-Basic', 'benchmark-bomb-workers', 'benchmark-Box2D', 'benchmark-cdjs', 'benchmark-chai-wtb', 'benchmark-coffeescript-wtb', 'benchmark-crypto',
             'benchmark-crypto-aes-SP', 'benchmark-crypto-md5-SP', 'benchmark-crypto-sha1-SP', 'benchmark-date-format-tofte-SP', 'benchmark-date-format-xparb-SP', 'benchmark-delta-blue',
             'benchmark-earley-boyer', 'benchmark-espree-wtb', 'benchmark-first-inspector-code-load', 'benchmark-FlightPlanner', 'benchmark-float-mm.c', 'benchmark-gaussian-blur', 
             'benchmark-gbemu', 'benchmark-gcc-loops-wasm', 'benchmark-hash-map', 'benchmark-HashSet-wasm', 'benchmark-jshint-wtb', 'benchmark-json-parse-inspector', 
             'benchmark-json-stringify-inspector', 'benchmark-lebab-wtb', 'benchmark-mandreel', 'benchmark-ML', 'benchmark-multi-inspector-code-load', 'benchmark-n-body-SP',
             'benchmark-navier-stokes', 'benchmark-octane-code-load', 'benchmark-octane-zlib', 'benchmark-OfflineAssembler', 'benchmark-pdfjs', 'benchmark-prepack-wtb',
             'benchmark-quicksort-wasm', 'benchmark-raytrace', 'benchmark-regex-dna-SP', 'benchmark-regexp', 'benchmark-richards', 'benchmark-richards-wasm', 'benchmark-segmentation',
             'benchmark-splay', 'benchmark-stanford-crypto-aes', 'benchmark-stanford-crypto-pbkdf2', 'benchmark-stanford-crypto-sha256', 'benchmark-string-unpack-code-SP',
             'benchmark-tagcloud-SP', 'benchmark-tsf-wasm', 'benchmark-typescript', 'benchmark-uglify-js-wtb', 'benchmark-UniPoker', 'benchmark-WSL']
    description = '''
    A workload to execute the speedometer web based benchmark

    Test description:
    1. Open chrome
    2. Navigate to the jetstream website - https://browserbench.org/JetStream/
    3. Execute the benchmark

    known working chrome version 80.0.3987.149
    '''
    requires_network = True

    parameters = [
        Parameter('tests', allowed_values=tests, kind=list, default=tests,
                  description='''
                  The sub test names for the jetstream benchmark
                  ''')
    ]

    def __init__(self, target, **kwargs):
        super(Jetstream, self).__init__(target, **kwargs)
        self.gui.timeout = 1500
    
    def init_resources(self, context):
        super(Jetstream, self).init_resources(context)
        self.gui.uiauto_params['tests'] = self.tests

    '''
    def update_output(self, context):
        super(Jetstream, self).update_output(context)
        result = None
        logcat_file = context.get_artifact_path('logcat')
        with open(logcat_file, errors='replace') as fh:
            for line in fh:
                match = self.regex.search(line)
                if match:
                    result = float(match.group(1))

        if result is not None:
            context.add_metric('Jetstream Score', result, 'Runs per minute', lower_is_better=False)
        else:
            raise WorkloadError("The Jetstream workload has failed. No score was obtainable.")
    '''
