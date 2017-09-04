/*    Copyright 2014-2016 ARM Limited
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

package com.arm.wlauto.uiauto.applaunch;

import android.os.Bundle;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiObject;
import android.util.Log;

import com.arm.wlauto.uiauto.ApplaunchInterface;
import com.arm.wlauto.uiauto.UxPerfUiAutomation;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import dalvik.system.DexClassLoader;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.ExtendedSSLSession;

import android.support.test.InstrumentationRegistry;
import android.support.test.rule.logging.AtraceLogger;


@RunWith(AndroidJUnit4.class)
public class UiAutomation extends UxPerfUiAutomation {

    /**
     * Uiobject that marks the end of launch of an application, which is workload
     * specific and added in the workload Java file by a method called getLaunchEndObject().
     */
    public UiObject launchEndObject;
    /** Timeout to wait for application launch to finish. */
    private Integer launch_timeout = 10;
    public String applaunchType;
    public int applaunchIterations;
    public String activityName;
    public ApplaunchInterface launch_workload;

    /* CODE BELOW IS WIP */
    /* [INPUT] Define what type of tracing to do:
     * UTIL - Find the process and thread names by running 'top'
     * PIDS - Collect pmu counters by using 'perf', across a range of PIDs
     * TIDS - Collect pmu counters by using 'perf', across a range of TIDs
     * ATRACE - Collect systrace captures by using 'atrace'
     * GATOR - Perform a Streamline capture on device
     */
    private String uxTracing;
    // [INPUT] Define how long to run commands for, if time based
    private int uxTimer;

    private Process uxProcess;
    private AtraceLogger uxAtrace;    
    private String[] pmuCounters;
    private String[] pmuTargets;
    
    private Process execRoot(String command) throws Exception{
        Log.d("Executing as root", command);
        return Runtime.getRuntime().exec(new String[] { "su", "-c", command});
    }

    private String execRootOutput(String command) throws Exception{
        Process proc = execRoot(command);
        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        int read;
        char[] buffer = new char[4096];
        StringBuffer output = new StringBuffer();
        while ((read = reader.read(buffer)) > 0) {
            output.append(buffer, 0, read);
        }
        reader.close();
        proc.waitFor();
        proc.destroy();
        return output.toString().trim();
    }
    
    private void uxTracingSetup() throws Exception{
        uxTracing = parameters.getString("uxperf_tracing").toUpperCase();
        uxTimer = parameters.getInt("uxperf_timer");
        switch (uxTracing) {
            case "UTIL":
                /* In order to get the list of hot PIDS, we need to use top... */
                sleep(20);
                killBackground();
                String topCmd = String.format("top -H -s cpu -d 1 -n %d > %s/uxtracing/top_baseline.txt 2>&1",
                                               uxTimer, parameters.getString("workdir"));
                Process topProc = execRoot(topCmd);
                topProc.waitFor();
                topProc.destroy();
                break;
            case "PIDS":
            case "TIDS":
                pmuCounters = parameters.getStringArray("pmu_counters");
                pmuTargets = parameters.getStringArray("pmu_targets");
                // Increase the number of iterations due to how many times we have to run pmu captures
                applaunchIterations *= pmuCounters.length * pmuTargets.length;
                // Run a baseline perf capture before the app is launched
                sleep(20);
                killBackground();
                for (String pmuCounter : pmuCounters) {
                    String perfCmd = String.format("/data/local/tmp/wa-bin/perf stat -a -e %s sleep %d >> %s/uxtracing/pmu_baseline.txt 2>&1",
                                                   pmuCounter, uxTimer, parameters.getString("workdir"));
                    Process perfProc = execRoot(perfCmd);
                    perfProc.waitFor();
                    perfProc.destroy();
                }
                break;
            case "ATRACE":
                uxAtrace = AtraceLogger.getAtraceLoggerInstance(InstrumentationRegistry.getInstrumentation());
                break;
            case "GATOR":
                // For local captures, you need a session.xml
                String session = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><session call_stack_unwinding=\"yes\" parse_debug_info=\"yes\" version=\"1\" high_resolution=\"no\" buffer_mode=\"streaming\" sample_rate=\"normal\" duration=\"0\" live_rate=\"100\"><energy_capture version=\"1\" type=\"none\"><channel id=\"0\" resistance=\"20\" power=\"yes\"/></energy_capture></session>";
                File xml = new File("/data/local/tmp/wa-bin", "session.xml");
                if (!xml.exists()) {
                    // Write out the new session data
                    FileOutputStream fos = new FileOutputStream(xml, false);
                    fos.write(session.getBytes());
                    fos.flush();
                    fos.close();
                }
                // Make sure gator isnt running already
                Process cmd = execRoot("pkill -l SIGKILL gatord");
                cmd.waitFor();
                cmd.destroy();
                break;
            default:
                // Do Nothing
                break;
        }
    }
    private void uxTracingStart(AppLaunch applaunch, Integer iteration) throws Exception{
        String command;
        switch (uxTracing) {
            case "UTIL":
                command = String.format("top -H -s cpu -d 1 -n %d > %s/uxtracing/top_all_%d.txt 2>&1",
                                        uxTimer, parameters.getString("workdir"), iteration);
                uxProcess = execRoot(command);
                sleep(1);
                applaunch.startLaunch();
                break;
            case "PIDS":
            case "TIDS":
                applaunch.startLaunch();
                sleep(1);
                if ((pmuCounters.length > 0) || (pmuTargets.length > 0)) {
                    Integer actual_iteration = iteration / (pmuCounters.length * pmuTargets.length);
                    String pmuCounter = pmuCounters[iteration % pmuCounters.length]; // Cycle around the list of pmuCounters
                    String pmuTarget = pmuTargets[(iteration / pmuCounters.length) % pmuTargets.length]; // Cycle around the list of pmuTargets
                    if (uxTracing.equals("PIDS")) {
                        command = String.format("/data/local/tmp/wa-bin/perf stat -a -e %s -p `pidof -s %s` sleep %d >> %s/uxtracing/pmu_%s_%d.txt 2>&1",
                                                pmuCounter, pmuTarget, uxTimer, parameters.getString("workdir"), pmuTarget.replace("/", ""), actual_iteration);
                    }
                    else {
                        String thread = pmuTarget.substring(0, 15).trim();
                        String process = pmuTarget.substring(15);
                        String tid_cmd = String.format("ps -t | grep %s | grep `pidof -s %s`", thread, process);
                        String output = execRootOutput(tid_cmd);
                        if (output.isEmpty()) {
                            Log.d("Unable to find tid", String.format("PID %s : TID %s", process, thread));
                            return;
                        }
                        output = output.split("\\s+")[1];
                        command = String.format("/data/local/tmp/wa-bin/perf stat -a -e %s -t %s sleep %d >> %s/uxtracing/pmu_%s_%s_%d.txt 2>&1",
                                                pmuCounter, output, uxTimer, parameters.getString("workdir"), process.replace("/", ""), thread.replace("/", ""), actual_iteration);
                    }
                    uxProcess = execRoot(command);
                }
                else {
                    uxTracing = "NONE";
                }
                break;
            case "ATRACE":
                uxAtrace.atraceStart(new HashSet<String>(Arrays.asList("sched", "am", "wm", "gfx", "view", "dalvik", "input")), 15360, uxTimer,
                                     new File(String.format("%s/uxtracing", parameters.getString("workdir"))), iteration.toString());
                sleep(1);
                applaunch.startLaunch();
                break;
            case "GATOR":
                command = String.format("/data/local/tmp/wa-bin/gatord -d -s /data/local/tmp/wa-bin/session.xml -o %1$s/uxtracing/%2$d.apc > %1$s/uxtracing/gator_log_%2$d.txt 2>&1",
                                        parameters.getString("workdir"), iteration);
                uxProcess = execRoot(command);
                sleep(uxTimer);
                applaunch.startLaunch();
                break;
            default:
                // Do Nothing
                break;
        }
    }
    private void uxTracingStop(AppLaunch applaunch) throws Exception{
        applaunch.endLaunch();
        sleep(1);
        switch (uxTracing) {
            case "ATRACE":
                uxAtrace.atraceStop();
                break;
            case "UTIL":
            case "PIDS":
            case "TIDS":
                uxProcess.waitFor();
                uxProcess.destroy();
                break;
            case "GATOR":
                Process cmd = execRoot("pkill -l SIGINT gatord");
                cmd.waitFor();
                cmd.destroy();
                uxProcess.waitFor();
                uxProcess.destroy();
                break;
            default:
                // Do Nothing
                break;
        }
    }

    /** Uiautomator function called by the applaunch workload. */
@Test
public void runUiAutomation() throws Exception{
        initialize_instrumentation();
        parameters = getParams();

        // Get workload apk file parameters
        String workload = parameters.getString("workload");
        String workloadAPKPath = parameters.getString("workdir");
        String workloadName = String.format("com.arm.wlauto.uiauto.%1s.apk",workload);
        String workloadAPKFile = String.format("%1s/%2s",workloadAPKPath, workloadName);

        // Load the apk file
        File apkFile = new File(workloadAPKFile);
        File dexLocation = mContext.getDir("outdex", 0);
        if(!apkFile.exists()) {
            throw new Exception(String.format("APK file not found: %s ", workloadAPKFile));
        }
        DexClassLoader classloader = new DexClassLoader(apkFile.toURI().toURL().toString(),
        dexLocation.getAbsolutePath(), null, mContext.getClassLoader());

        Class uiautomation = null;
        Object uiautomation_interface = null;
        String workloadClass = String.format("com.arm.wlauto.uiauto.%1s.UiAutomation",workload);
        try {
            uiautomation = classloader.loadClass(workloadClass);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        Log.d("Class loaded:", uiautomation.getCanonicalName());
        uiautomation_interface = uiautomation.newInstance();

        // Create an Application Interface object from the workload
        launch_workload = ((ApplaunchInterface)uiautomation_interface);

        // Get parameters for application launch
        getPackageParameters();
        applaunchType = parameters.getString("applaunch_type");
        applaunchIterations = parameters.getInt("applaunch_iterations");
        activityName = parameters.getString("launch_activity");

        // Run the workload for application launch initialization
        runApplaunchSetup();
        uxTracingSetup();
        // Run the workload for application launch measurement
        for (int iteration = 0; iteration < applaunchIterations; iteration++) {
            Log.d("Applaunch iteration number: ", String.valueOf(applaunchIterations));
            sleep(20);//sleep for a while before next iteration
            killBackground();
            runApplaunchIteration(iteration);
            closeApplication();
        }
    }

    /**
     * Setup run for applaunch workload that clears the initial
     * run dialogues on launching an application package.
     */
    public void runApplaunchSetup() throws Exception{
        setScreenOrientation(ScreenOrientation.NATURAL);
        launch_workload.initialize_instrumentation();
        launch_workload.setWorkloadParameters(parameters);
        launch_workload.runApplicationInitialization();
        launchEndObject = launch_workload.getLaunchEndObject();
        unsetScreenOrientation();
        closeApplication();
    }

    /**
     * This method performs multiple iterations of application launch and
     * records the time taken for each iteration.
     */
    public void runApplaunchIteration(Integer iteration_count) throws Exception{
        String testTag = "applaunch" + iteration_count;
        String launchCommand = launch_workload.getLaunchCommand();
        AppLaunch applaunch = new AppLaunch(testTag, launchCommand);

        uxTracingStart(applaunch, iteration_count); //Launch the application and start timer
        uxTracingStop(applaunch); //marks the end of launch and stops timer
    }

    /*
     * AppLaunch class implements methods that facilitates launching applications
     * from the uiautomator. It has methods that are used for one complete iteration of application
     * launch instrumentation.
     * ActionLogger class is instantiated within the class for measuring applaunch time.
     * startLaunch(): Marks the beginning of the application launch, starts Timer
     * endLaunch(): Marks the end of application, ends Timer
     * launchMain(): Starts the application launch process and validates the finish of launch.
    */
    private class AppLaunch {

        private String testTag;
        private String launchCommand;
        private ActionLogger logger;
        Process launch_p;

        public AppLaunch(String testTag, String launchCommand) {
            this.testTag = testTag;
            this.launchCommand = launchCommand;
            this.logger = new ActionLogger(testTag, parameters);
        }

        // Called by launchMain() to check if app launch is successful
        public void launchValidate(Process launch_p) throws Exception {
            launch_p.waitFor();
            Integer exit_val = launch_p.exitValue();
            if (exit_val != 0) {
                throw new Exception("Application could not be launched");
            }
        }

        // Marks the end of application launch of the workload.
        public void endLaunch() throws Exception{
            waitObject(launchEndObject, launch_timeout);
            logger.stop();
            launch_p.destroy();
        }

        // Launches the application.
        public void launchMain() throws Exception{
            launch_p = Runtime.getRuntime().exec(launchCommand);
            launchValidate(launch_p);
        }

        // Beginning of application launch
        public void startLaunch() throws Exception{
            logger.start();
            launchMain();
        }
    }

    // Exits the application according to application launch type.
    public void closeApplication() throws Exception{
        if(applaunchType.equals("launch_from_background")) {
            pressHome();
        }
        else if(applaunchType.equals("launch_from_long-idle")) {
            killApplication();
            dropCaches();
        }
    }

    // Kills the application process
    public void killApplication() throws Exception{
        Process kill_p;
        String command = String.format("am force-stop %s", packageName);
        kill_p = Runtime.getRuntime().exec(new String[] { "su", "-c", command});
        kill_p.waitFor();
        kill_p.destroy();
    }

    // Kills the background processes
    public void killBackground() throws Exception{
        Process kill_p;
        kill_p = Runtime.getRuntime().exec("am kill-all");
        kill_p.waitFor();
        kill_p.destroy();
    }

    // Drop the caches
    public void dropCaches() throws Exception{
        Process sync;
        sync = Runtime.getRuntime().exec(new String[] { "su", "-c", "sync"});
        sync.waitFor();
        sync.destroy();

        Process drop_cache;
        String command = "echo 3 > /proc/sys/vm/drop_caches";
        drop_cache = Runtime.getRuntime().exec(new String[] { "su", "-c", command});
        drop_cache.waitFor();
        drop_cache.destroy();
    }
}
