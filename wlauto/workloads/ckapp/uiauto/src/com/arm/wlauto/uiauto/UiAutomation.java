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

package com.arm.wlauto.uiauto.ckapp;

import android.os.Bundle;
import android.os.SystemClock;

// Import the uiautomator libraries
import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiScrollable;
import com.android.uiautomator.core.UiSelector;

import com.arm.wlauto.uiauto.UxPerfUiAutomation;

public class UiAutomation extends UxPerfUiAutomation {

    public Bundle parameters;

    public void runUiAutomation() throws Exception {
        parameters = getParams();
        int timeout = Integer.parseInt(parameters.getString("timeout"));

        setScreenOrientation(ScreenOrientation.NATURAL);

        recognize();
        if (!popup(timeout)) {
            throw new UiObjectNotFoundException("Timed out. Could not find popup!");
        }

        unsetScreenOrientation();
    }

    private void recognize() throws Exception {
        UiObject button =
            new UiObject(new UiSelector().textContains("Recognize")
                                         .className("android.widget.Button"));
        // Wait up to 10 seconds
        if (!button.waitForExists(10000)) {
            throw new UiObjectNotFoundException("Could not find button: Recognize");
        }
        button.click();
    }

    private boolean popup(int timeout) throws Exception {
        UiObject result =
            new UiObject(new UiSelector().textContains("Is that correct result:")
                                         .className("android.widget.TextView"));
        // Wait 5 seconds then repeat n times to make a total timeout period of `timeout` minutes
        int maxtime = timeout*(60/5);
        int counter = 0;
        while (!result.waitForExists(5000) && (counter < maxtime)) {
            counter++;
        }
        return (counter < maxtime);
    }
}
