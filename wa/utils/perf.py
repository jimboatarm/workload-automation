#    Copyright 2013-2015 ARM Limited
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

class SimpleperfReportSample(object):

    def __init__(self, event_type, time, event_count, thread_id, thread_name, vaddr_in_file, file, symbol, callchain):
        self.event_type = event_type
        self.event_count = event_count
        self.time = time
        self.thread_id = thread_id
        self.thread_name = thread_name
        self.vaddr_in_file = vaddr_in_file
        self.file = file
        self.symbol = symbol
        self.callchain = callchain

    def __repr__(self):
        return 'event_type: {}, time: {}, event_count: {}, thread_id: {}, thread_name: {}, vaddr_in_file: {}, file: {}, symbol: {}, callchain: {}'.format(
            self.event_type, self.time, self.event_count,
            self.thread_id, self.thread_name, self.vaddr_in_file,
            self.file, self.symbol, self.callchain
        )

    __str__ = __repr__

class SimpleperfReportMetaInfo(object):

    def __init__(self, trace_offcpu, event_types, app_package_name):
        self.trace_offcpu = trace_offcpu
        self.event_types = event_types
        self.app_package_name = app_package_name

    def __repr__(self):
        return 'trace_offcpu: {}, event_types: {}, app_package_name: {}'. format(
            self.trace_offcpu, self.event_types, self.app_package_name)

    __str__ = __repr__


class SimpleperfReportSampleParser(object):

    def parse(self, filepath):
        samples = []
        with open(filepath) as fh:
            is_first_sample = True
            sample_string = ''
            for line in fh:
                if not line.split():
                    continue
                if line.split()[0] == 'lost_situation:':
                    samples.append(self._parse_sample(sample_string))
                    return samples
                if line.split()[0].strip() == 'sample:' and not is_first_sample:
                    samples.append(self._parse_sample(sample_string))
                    sample_string = ''
                if line.split()[0].strip() == 'sample:':
                    is_first_sample = False
                if is_first_sample == False:
                    sample_string = sample_string + line
        return samples


    def _parse_sample(self, sample_string):
        sample = {}
        callchain = []
        in_callchain = False
        for line in sample_string.splitlines():
            if line.split()[0].strip() == 'sample:':
                continue
            if line.split()[0].strip() == 'callchain:':
                in_callchain = True
                continue
            if in_callchain:
                callchain.append({line.split()[0].strip().strip(':') : ''.join(line.split()[1:]).strip()})
                continue
            else:
                sample[line.split()[0].strip().strip(':')] = ''.join(line.split()[1:]).strip()
        return SimpleperfReportSample(sample.get('event_type', None), 
                                          sample.get('time', None), 
                                          sample.get('event_count', None), 
                                          sample.get('thread_id', None), 
                                          sample.get('thread_name', None), 
                                          sample.get('vaddr_in_file', None), 
                                          sample.get('file', None), 
                                          sample.get('symbol', None),
                                          callchain)

    def _parse_callchain(self, callchain_string):
        pass

    def parse_meta_info(self, filepath):
        event_types = []
        meta_info = {}
        is_in_meta_info = False
        with open(filepath) as fh:
            for line in fh:
                if line.split()[0].strip() == 'meta_info:':
                    is_in_meta_info = True
                if not is_in_meta_info:
                    continue
                if line.split()[0].strip() == 'sample:':
                    print(event_types)
                    return SimpleperfReportMetaInfo(meta_info['trace_offcpu'],
                                                    event_types,
                                                    meta_info['app_package_name'])
                if line.split()[0].strip() == 'event_type:':
                    event_types.append(line.split()[1].strip())
                else:
                    meta_info[line.split()[0].strip().strip(':')] = ''.join(line.split()[1:]).strip()
