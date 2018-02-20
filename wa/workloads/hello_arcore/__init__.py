#    Copyright 2018 ARM Limited
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

import subprocess
import os
import csv

from wa import Parameter, ApkWorkload


class HelloArcore(ApkWorkload):

    name = 'hello_arcore'
    base_apk_name = 'com.google.ar.core.examples.java.helloar'
    description = (
        'This workload runs custom variations of the ARCore sample app Hello_AR. '
        'These apps should have been compiled with specific settings for ARCore. '
        'This workload selects the correct APK based on the provided parameters from '
        'the name of the APK which should comply to the format'
        '\n\t`{}_XYZ`\nwhere `XYZ` are the capitalized initials of the parameters. '
        'The workload launches the app, waits for a provided duration before closing the app. '
        'After running the app, the workload extracts from the logcat numeric values sitting '
        'at the end of lines containing the keyword `UXPERF` and preceded by either `FrameTime` '
        'or `PointCloudTime` and, depending on the `export_csv_times` parameter, outputs these '
        'in csv files respectively named `frametimes.csv` and `cloudtimes.csv`. Finally it adds '
        'the averages of these signals and their corresponding frequencies to the iteration metrics.'
    ).format(base_apk_name)

    arcore_modes = {
        'update_mode': {
            'values': ['blocking', 'latest_camera_image'],
            'description':
                'When `blocking`, `update()` (see ARCore API: `Session`) waits until a '
                'new camera image is available (typically 30 FPS) while when using the '
                '`latest_camera_image`, `update()` returns immediately without blocking '
                'and if no new camera image was available, returns the most recent '
                '`Frame` object.'
        },
        'plane_finding_mode': {
            'values': ['horizontal', 'disabled'],
            'description': 'Enables detection of (only) `horizontal` planes.'
        },
        'light_estimation_mode': {
            'values': ['ambient_intensity', 'disabled'],
            'description': 'Generates a single-value ambient lighting intensity estimation.'
        },
    }

    parameters = [Parameter(name=n, kind=str, mandatory=True,
                            allowed_values=v['values'], default=v['values'][0],
                            description=v['description'])
                  for n, v in arcore_modes.iteritems()]
    parameters.extend([
        Parameter(name='export_csv_times', kind=bool, default=False,
                  description=(
                      'If `True`, the raw frame times and cloud point times are exported '
                      'to respective CSV files for each iteration (default: `False`).')),
        Parameter(name='duration', kind=int, default=10,
                  description='Duration (in seconds) of the Hello AR app run (default: 10).'),
    ])

    def __init__(self, target, **kwargs):
        super(HelloArcore, self).__init__(target, **kwargs)
        used_params = [getattr(self, p) for p in self.arcore_modes.iterkeys()]
        self.package_names = [
            self.base_apk_name + '_' + ''.join([p[0].upper() for p in used_params])
        ]
        self.loading_time = self.duration  # Relies on ApkWorkload.loading_time
        # The wait itself happens in ApkWorkload.setup()

    # Assumes ns data:
    def _add_timeline_metrics(self, context, name, timeline):
        if timeline:
            avg = sum(timeline) / len(timeline)
            frq = 1e9 / avg
            context.add_metric(name + ' (Time)', avg, 'ns')
            context.add_metric(name + ' (Freq)', frq, 'Hz')

    def update_output(self, context):
        super(HelloArcore, self).update_output(context)
        with open(os.path.join(context.output_directory, 'logcat.log')) as f:
            log = f.read()
        frametimes = []
        cloudtimes = []
        right_iteration = False
        for line in log.splitlines():
            right_iteration = right_iteration or 'UpdateMode' in line
            if right_iteration and 'UXPERF' in line:
                if 'FrameTime' in line:
                    frametimes.append(long(line.split()[-1]))
                elif 'PointCloudTime' in line:
                    cloudtimes.append(long(line.split()[-1]))

        if self.export_csv_times:
            with open(os.path.join(context.output_directory, 'frametimes.csv'), 'w') as f:
                csv.writer(f).writerow(frametimes)
            with open(os.path.join(context.output_directory, 'cloudtimes.csv'), 'w') as f:
                csv.writer(f).writerow(cloudtimes)
        self._add_timeline_metrics(context, 'Frame', frametimes)
        self._add_timeline_metrics(context, 'Point Cloud', cloudtimes)
