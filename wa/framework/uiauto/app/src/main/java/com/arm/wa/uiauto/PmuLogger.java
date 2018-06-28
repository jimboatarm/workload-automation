/*    Copyright 2014-2016 ARM Limited
        ://eu-gerrit-1.euhpc.arm.com/#/dashboard/selfhttps://eu-gerrit-1.euhpc.arm.com/#/dashboard/selfhttps://eu-gerrit-1.euhpc.arm.com/#/dashboard/selfhttps://eu-gerrit-1.euhpc.arm.com/#/dashboard/selfhttps://eu-gerrit-1.euhpc.arm.com/#/dashboard/selfill_process(perf_pid, click_p);
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arm.wa.uiauto;

import android.os.Bundle;
import android.util.Log;
import java.io.*;

    /**
     * Basic marker API for workloads to capture PMU counters between two
     * start and end markers within a Region of interest in a workload.
     * Markers are output to logcat with debug
     * priority and counters are logged into a text file which can be parsed by PMU parser
     * notebook in quark.
     *
     * The marker output n logcat consists of a logcat tag 'UX_PERF_PMU' and a message. The
     * message consists of a name for ROI being captured counters for and a timestamp. The timestamp
     * is separated by a single space from the name of the action.
     *
     * Typical usage:
     *
     * PmuLogger logger = PmuLogger("pmuTag", parameters);
     * logger.start();
     * // actions to be recorded
     * logger.stop();
     */
    public class PmuLogger {

        private Process uiP;
        private String functionName;
        private String pkgName;
        private String counters;
        private String capture_type;
        private String iteration;
        private String resDir;
        private String resFile;
        private String resFileReport;
        private String pmuTag;
        private boolean enabled;
        private Process click_p;
        private String perf_pid;

        public PmuLogger(String pmuTag, Bundle parameters, boolean enabled) {
            this.pmuTag = pmuTag;
            this.functionName = pmuTag.split("_")[0];
            Log.d("UX_PERF_PMU", parameters.getString("pmu_res_dir") + " start " + System.nanoTime());
            this.resDir = parameters.getString("pmu_res_dir") + "/" + functionName + "/" + parameters.getString("pmu_cluster") + "/" + pmuTag;
            this.enabled = enabled;
            this.counters = parameters.getString("pmu_counter");
            this.capture_type = parameters.getString("pmu_capture_type");
            this.pkgName = parameters.getString("package_name");
            if (capture_type.equals("record")) {
       		this.resFile = "perf_record_" + counters + "_" + parameters.getString("pmu_iteration") + ".data";
       		this.resFileReport = "perf_record_" + counters + "_" + parameters.getString("pmu_iteration") + ".txt";
    	    } else {
       	        this.resFile = "perf_stat_" + counters + parameters.getString("pmu_iteration") + ".txt";
    	   }
           resFile = resDir + "/" + resFile;
           resFileReport = resDir + "/" + resFileReport;
        }

        public void start() throws Exception {
            if (enabled) {
                Log.d("UX_PERF_PMU", pmuTag + " start " + System.nanoTime());
                create_perf_dir();
                kill_all_perf();
                click_p = perf_begin_capture();
                perf_pid = get_perf_pid();
            }
        }

        public void stop() throws Exception {
            if (enabled) {
                Log.d("UX_PERF_PMU", pmuTag + " end " + System.nanoTime());
                kill_process(perf_pid, click_p);
            }
            if (capture_type.equals("record")) {
            	parse_perf_data();
            }
        }


        public String get_package_pid(String pkg) throws Exception{
            Process pkg_pid;
            String ps_command = String.format("ps | grep %s", pkg);
            pkg_pid = Runtime.getRuntime().exec(new String[] { "su", "-c", ps_command});
            pkg_pid.waitFor();
            BufferedReader stdInput = new BufferedReader(new
                             InputStreamReader(pkg_pid.getInputStream()));


            String output = null;
            String s = null;
            // read the output from the command
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
                output += s;
            }

	    String perf_file = String.format("/sdcard/ps_perf.txt");
            dump_log(output, perf_file);

            String[] array = output.split(" +");
            return array[1];
        }

        public void create_perf_dir() throws Exception {
            Process p_perf_dir;
            String create_command = String.format("mkdir -p %s", resDir);
            p_perf_dir = Runtime.getRuntime().exec(new String[] { "su", "-c", create_command});
            p_perf_dir.waitFor();
            p_perf_dir.destroy();
        }

	public void parse_perf_data() throws Exception{
            Process perf_report;
            String perf_command = String.format("/data/local/tmp/perf report -i %s --sort comm,dso >> %s 2>&1", resFile, resFileReport);
            perf_report = Runtime.getRuntime().exec(new String[] { "su", "-c", perf_command});
            perf_report.waitFor();
            perf_report.destroy();

        }

        public Process perf_begin_capture() throws Exception{
            String app_pid = get_package_pid(pkgName);
            String app_file = String.format("/sdcard/ps_app.txt");
            dump_log(app_pid, app_file);
            Process perf_p;
            String perf_command;
            if (capture_type.equals("record")) {
            	perf_command = String.format("/data/local/tmp/perf record -e %s -p %s -o %s", counters, app_pid, resFile);
            } else {
            	perf_command = String.format("/data/local/tmp/perf stat -e %s -p %s >> %s 2>&1", counters, app_pid, resFile);
            }
            perf_p = Runtime.getRuntime().exec(new String[] { "su", "-c", perf_command});
            return perf_p;
        }

        public String get_perf_pid() throws Exception{
            String perf_p_id = "/data/local/tmp/perf";
            String perf_p_pid = get_package_pid(perf_p_id);
            String perf_file = String.format("/sdcard/ps_perf.txt");
            dump_log(perf_p_pid, perf_file);
            return perf_p_pid;
        }

        public void kill_process(String pid, Process perf_p)  throws Exception{
            Process perf_p_kill;
            String kill_command = String.format("kill -SIGINT %s", pid);
            perf_p_kill = Runtime.getRuntime().exec(new String[] { "su", "-c", kill_command});
            perf_p_kill.waitFor();
            perf_p.waitFor();
            perf_p_kill.destroy();
            perf_p.destroy();
        }

	public void kill_all_perf()  throws Exception{
            Process perf_kill;
            String kill_command = String.format("killall -9 /data/local/tmp/perf");
            perf_kill = Runtime.getRuntime().exec(new String[] { "su", "-c", kill_command});
            perf_kill.waitFor();
            perf_kill.destroy();
        }


        public void dump_log(String data, String file)  throws Exception{
            Process log;
            String log_command = String.format("echo %s >> %s", data, file);
            log = Runtime.getRuntime().exec(new String[] { "su", "-c", log_command});
            log.waitFor();
            log.destroy();
            }
        }
