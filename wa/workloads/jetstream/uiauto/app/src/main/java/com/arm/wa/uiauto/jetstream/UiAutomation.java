/*    Copyright 2014-2018 ARM Limited
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

package com.arm.wa.uiauto.jetstream;

import android.os.Bundle;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.UiScrollable;

import com.arm.wa.uiauto.BaseUiAutomation;
import android.util.Log;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class UiAutomation extends BaseUiAutomation {

    private int networkTimeoutSecs = 30;
    private long networkTimeout =  TimeUnit.SECONDS.toMillis(networkTimeoutSecs);
    public static String TAG = "UXPERF";
    public boolean textenabled = false;
    private String tests[];

    @Before
    public void initialize(){
        Bundle params = getParams();
        tests = params.getStringArray("tests");
        initialize_instrumentation();
    }

    @Test
    public void setup() throws Exception{
        setScreenOrientation(ScreenOrientation.NATURAL);
        dismissChromePopup();
        openJetstream();
    }

    @Test
    public void runWorkload() throws Exception {
        /*for (String i: tests) {
            UiObject i = 
            mDevice.findObject(new UiSelector().resourceId(i));
        }*/
        runBenchmark();
    }

    @Test
    public void teardown() throws Exception{
        clearTabs();
        unsetScreenOrientation();
    }

    public void runBenchmark() throws Exception {
        UiObject start =
            mDevice.findObject(new UiSelector().description("Start Test"));
            
        UiObject starttext =
            mDevice.findObject(new UiSelector().text("Start Test"));

        // Run Jetstream test
        if (start.waitForExists(10000)) {
            start.click();
        } else {
            starttext.click();
        }
        UiObject scores =
            mDevice.findObject(new UiSelector().resourceId("result-summary"));
        scores.waitForExists(2100000);
        getTestScore();
        //getScores(scores);
    }

    public void openJetstream() throws Exception {
        UiObject urlBar =
            mDevice.findObject(new UiSelector().resourceId("com.android.chrome:id/url_bar"));
         
        UiObject searchBox =  mDevice.findObject(new UiSelector().resourceId("com.android.chrome:id/search_box_text"));
        
        if (!urlBar.waitForExists(5000)) {
                searchBox.click();
        }

        String url = "http://browserbench.org/JetStream/";

        // Clicking search box turns it into url bar on some deivces
        if(urlBar.waitForExists(2000)) {
            urlBar.click();
            sleep(2);
            urlBar.setText(url);
        } else {
            searchBox.setText(url);
        }
        pressEnter();
    }

    /*public void getScores(UiObject scores) throws Exception {
        UiScrollable list = new UiScrollable(new UiSelector().scrollable(true));
        UiObject results =
            mDevice.findObject(new UiSelector().resourceId("results"));

        for (String test : tests) {
            Log.d(TAG, "TEST NAME: " + test);
            getTestScore(list, results, test);
        }
    }

    public void getTestScore(UiScrollable scrollable, UiObject resultsList, String test) throws Exception {
        for (int i=1; i < resultsList.getChildCount(); i++) {
            UiObject testname = resultsList.getChild(new UiSelector().resourceId(test));
            if (testname.exists() && testname.getText().equals(test)) {
                UiObject result = resultsList.getChild(new UiSelector().resourceId(test));
                Log.d(TAG, test + " score " + result.getText());
                return;
            }
        }
    }*/

    public void getTestScore() throws Exception {
        UiScrollable list = new UiScrollable(new UiSelector().scrollable(true));

        for (String test: tests) {
            UiObject result =
                mDevice.findObject(new UiSelector().resourceId(test));
            if (!result.exists()){
                //list.scrollIntoView(result);
                do {
                    list.scrollForward(2);
                }
                while (!result.exists());
            }
            Log.d(TAG, "TEST: " + test);
            Log.d(TAG, test + " score: " + result.getText());
        }
    }

    public void clearTabs() throws Exception {
        UiObject tabselector =
            mDevice.findObject(new UiSelector().resourceId("com.android.chrome:id/tab_switcher_button")
                .className("android.widget.ImageButton"));
        if (!tabselector.exists()){
            return;
        }
        tabselector.click();
        UiObject menu =
            mDevice.findObject(new UiSelector().resourceId("com.android.chrome:id/menu_button")
                .className("android.widget.ImageButton"));
        menu.click();
        UiObject closetabs =
            mDevice.findObject(new UiSelector().textContains("Close all tabs"));
        if (closetabs.exists()){
            closetabs.click();
        }
    }
}
