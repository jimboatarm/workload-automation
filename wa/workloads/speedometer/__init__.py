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

from wa import UiautoWorkload, Parameter
from wa.framework.exception import ValidationError
from wa.utils.types import list_of_strs
from wa.utils.misc import unique


class speedometer(UiautoWorkload):

    name = 'speedometer'
    package_names = ['']
    description = '''
    A workload to execute the speedometer web based benchmark

    Test description:
    1. Open browser application
    2. Navigate to the speedometer website
    3. Execute the benchmark

    '''

    def __init__(self, target, **kwargs):
        super(speedometer, self).__init__(target, **kwargs)
        self.clean_assets = True
        self.gui.timeout = 1500

    def init_resources(self, context):
        super(speedometer, self).init_resources(context)

    def run(self, context):
        self.target.execute('am start -a android.intent.action.VIEW -d http://browserbench.org/Speedometer/')
        super(speedometer, self).run(context)

    regex = re.compile('Speedometer Score ([0-9]+\.[0-9]+)')

    def update_output(self, context):
        result = None
        super(speedometer, self).update_output(context)
        logcat_file = context.get_artifact_path('logcat')
        with open(logcat_file) as fh:
            for line in fh:
                match = self.regex.search(line)
                if match:
                    result = float(match.group(1))

        if result is not None:
            context.add_metric('Speedometer Score', result, 'Runs per minute', lower_is_better=False)

