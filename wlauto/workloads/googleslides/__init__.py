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
import shutil
from wlauto import AndroidUiAutoBenchmark, Parameter
from wlauto.utils.types import list_of_strings


__version__ = '0.1.0'

class GoogleSlides(AndroidUiAutoBenchmark):

    name = 'googleslides'
    description = '''
    A workload to perform standard productivity tasks with Google Slides. The workload carries
    out various tasks, such as creating a new presentation, adding text, images, and shapes,
    as well as basic editing and playing a slideshow.

    Under normal circumstances, this workload should be able to run without a network connection.
    The workload is split into two main scenarios, each starting out with the common steps 1-3.

    --- load ---
    Copying a PowerPoint presentation on to the device to test slide navigation

    Test description:
    1. Starts the app in offline access mode, and skips the welcome screen.
    2. Opens the left-hand app drawer menu and closes it again to measure menu transitions.
    3. Goes to the app settings page and enables PowerPoint compatibility mode. This allows
       .PPT files to be created inside Google Slides.
    4. A navigation test is performed while the file is in editing mode, swiping forward to
       the next slide until the end.
    5. Thereafter, the same action is done in the reverse direction back to the first slide.
    6. Finally, one more forward navigation pass through is done while in presentation mode.

    --- create ---
    Creating a new file in the application and performs basic editing on it.

    Test description:
    1. Starts the app in offline access mode, and skips the welcome screen.
    2. Opens the left-hand app drawer menu and closes it again to measure menu transitions.
    3. Goes to the app settings page and enables PowerPoint compatibility mode. This allows
       .PPT files to be created inside Google Slides.
    4. Creates a new PowerPoint presentation in the app (PPT compatibility mode) with a title
       slide and saves it to device storage.
    5. Inserts another slide with a title and text content. The font size of the text is then
       reduced to fit the slide.
    6. Inserts a slide with a title and image content. The image is picked from the gallery.
    7. Inserts the last slide and adds a shape which is dragged and resized to fit the view.
       Some text is also entered in this final slide.
    8. Finally, the app is navigated to the documents list, closing the file in the process.
       The file is then deleted from the documents list and removed from the device.
    '''

    package = 'com.google.android.apps.docs.editors.slides'
    activity = ''

    # Views for FPS instrumentation
    view = [
        package + '/com.google.android.apps.docs.quickoffice.filepicker.FilePickerActivity',
        package + '/com.google.android.apps.docs.editors.shared.filepicker.FilePickerActivity',
        package + '/com.google.android.apps.docs.quickoffice.filepicker.LocalSaveAsActivity',
        package + '/com.qo.android.quickpoint.Quickpoint',
        package + '/com.google.android.apps.docs.app.DocsPreferencesActivity',
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
        # Use Android downloads folder as it is the default folder opened by Google Slides'
        # file picker, and not WA's working directory. Helps test reliability by avoiding
        # having to navigate around the filesystem to locate pushed file.
        self.device_dir = path.join(self.device.working_directory, '..', 'Download')
        self.wa_test_file = 'wa_test_{}'.format(self.local_file) if self.local_file else None

    def validate(self):
        super(GoogleSlides, self).validate()
        self.uiauto_params['dumpsys_enabled'] = self.dumpsys_enabled
        self.uiauto_params['output_dir'] = self.device.working_directory
        self.uiauto_params['results_file'] = self.output_file
        if self.local_file:
            self.uiauto_params['local_file'] = self.wa_test_file
            self.uiauto_params['slide_count'] = self.slide_count

    def initialize(self, context):
        super(GoogleSlides, self).initialize(context)
        self.logger.info('local_dir={}, local_file={}'.format(self.local_dir, self.local_file))
        self.logger.info('device_dir={}, wa_test_file={}'.format(self.device_dir, self.wa_test_file))
        if self.local_file:
            # Copy to dependencies folder if necessary
            if not path.isfile(path.join(self.local_dir, self.local_file)):
                filepath = path.join(path.dirname(__file__), self.local_file)
                shutil.copy(filepath, self.local_dir)
            # push local PPT file
            for entry in os.listdir(self.local_dir):
                if entry == self.local_file:
                    self.device.push_file(path.join(self.local_dir, self.local_file),
                                          path.join(self.device_dir, self.wa_test_file),
                                          timeout=60)

    def update_result(self, context):
        super(GoogleSlides, self).update_result(context)
        self.get_metrics(context)

    def teardown(self, context):
        super(GoogleSlides, self).teardown(context)
        self.pull_logs(context)

    def finalize(self, context):
        super(GoogleSlides, self).finalize(context)
        if self.local_file:
            # delete pushed PPT file
            for entry in self.device.listdir(self.device_dir):
                if entry == self.wa_test_file:
                    self.device.delete_file(path.join(self.device_dir, entry))

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
