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

from wlauto import AndroidUxPerfWorkload, Parameter


class Ckapp(AndroidUxPerfWorkload):

    name = 'ckapp'
    package = 'openscience.crowdsource.video.experiments'
    activity = '.MainActivity'
    description = '''
    '''

    parameters = [
        Parameter('timeout', kind=int, default=2,
                  description='''
                  Timeout value in minutes to keep waiting for the result popup to appear.
                  '''),
    ]

    # This workload relies on the internet so check that there is a working
    # internet connection
    requires_network = True

    def validate(self):
        super(Ckapp, self).validate()
        self.uiauto_params['timeout'] = self.timeout
