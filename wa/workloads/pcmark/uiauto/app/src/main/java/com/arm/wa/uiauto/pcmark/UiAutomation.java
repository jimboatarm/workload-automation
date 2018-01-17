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

package com.arm.wa.uiauto.pcmark;

import android.os.Bundle;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;

import com.arm.wa.uiauto.UxPerfUiAutomation.GestureTestParams;
import com.arm.wa.uiauto.UxPerfUiAutomation.GestureType;
import com.arm.wa.uiauto.BaseUiAutomation;
import com.arm.wa.uiauto.ActionLogger;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import static com.arm.wa.uiauto.BaseUiAutomation.FindByCriteria.BY_DESC;
import static com.arm.wa.uiauto.BaseUiAutomation.FindByCriteria.BY_ID;
import static com.arm.wa.uiauto.BaseUiAutomation.FindByCriteria.BY_TEXT;

// Import the uiautomator libraries

@RunWith(AndroidJUnit4.class)
public class UiAutomation extends BaseUiAutomation {

    private int networkTimeoutSecs = 30;
    private long networkTimeout =  TimeUnit.SECONDS.toMillis(networkTimeoutSecs);

    @Before
    public void initialize(){
        initialize_instrumentation();
    }

    @Test
    public void setup() throws Exception{
        setScreenOrientation(ScreenOrientation.NATURAL);
    }

    @Test
    public void runWorkload() throws Exception {
        loadBenchmarks();
        installBenchmark();
        runBenchmark();
    }

    @Test
    public void teardown() throws Exception{
        unsetScreenOrientation();
    }

    //Swipe to benchmarks and back to initialise the app correctly
    private void loadBenchmarks() throws Exception {
        UiObject title = 
            mDevice.findObject(new UiSelector().resourceId("com.futuremark.pcmark.android.benchmark:id/actionBarMenuItemText")
                .className("android.widget.TextView"));
        if (title.exists()){
            title.click();
            UiObject benchPage = getUiObjectByText("BENCHMARKS");
            benchPage.click();
            title.click();
            UiObject pcmark = getUiObjectByText("PCMARK");
            pcmark.click();
        }
    }

    //Install the Work 2.0 Performance Benchmark
    private void installBenchmark() throws Exception {
        UiObject benchmark = 
            mDevice.findObject(new UiSelector().descriptionContains("INSTALL("));
        if (benchmark.exists()) {
            benchmark.click();
                UiObject install = 
                    mDevice.findObject(new UiSelector().description("INSTALL")
                        .className("android.view.View"));
                install.click();
                UiObject installed =
                    mDevice.findObject(new UiSelector().description("RUN")
                        .className("android.view.View"));
                installed.waitForExists(240000);
        }
    }
    
    //Execute the Work 2.0 Performance Benchmark - wait up to ten minutes for this to complete
    private void runBenchmark() throws Exception {
        UiObject run =
            mDevice.findObject(new UiSelector().resourceId("CONTROL_PCMA_WORK_V2_DEFAULT")
                                               .className("android.view.View")
                                               .childSelector(new UiSelector().index(1)
                                               .className("android.view.View")));
        run.click();
        UiObject score = 
            mDevice.findObject(new UiSelector().descriptionContains("Work 2.0 performance score")
                .className("android.view.View"));
        score.waitForExists(600000);
    }
}
