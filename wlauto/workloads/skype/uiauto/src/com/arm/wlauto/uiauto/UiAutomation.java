package com.arm.wlauto.uiauto.skype;

import java.io.File;
import java.util.TreeMap;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;

// Import the uiautomator libraries
import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiScrollable;
import com.android.uiautomator.core.UiSelector;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;

import com.arm.wlauto.uiauto.UxPerfUiAutomation;

public class UiAutomation extends UxPerfUiAutomation {

    public static final String TAG = "skype";
    public static final String PACKAGE = "com.skype.raider";
    public static final String PACKAGE_ID = "com.skype.raider:id/";
    public static final String TEXT_VIEW = "android.widget.TextView";

    private Map<String, Timer> results = new TreeMap<String, Timer>();
    private boolean dumpsysEnabled;
    private String outputDir;

    public void runUiAutomation() throws Exception {
        // Override superclass value
        this.waitTimeout = 10000;

        // Get Params
        Bundle parameters = getParams();
        String loginName = parameters.getString("my_id");
        String loginPass = parameters.getString("my_pwd");
        String contactSkypeId = parameters.getString("skypeid");
        String contactName = parameters.getString("name").replace("_", " ");
        int callDuration = Integer.parseInt(parameters.getString("duration"));
        String callType = parameters.getString("action");
        String resultsFile = parameters.getString("results_file");
        outputDir = parameters.getString("output_dir", "/sdcard/wa-working");
        dumpsysEnabled = Boolean.parseBoolean(parameters.getString("dumpsys_enabled"));

        // Run tests
        handleLoginScreen(loginName, loginPass);
        confirmAccess();
        selectContact(contactName, contactSkypeId);
        if ("video".equalsIgnoreCase(callType)) {
            videoCallTest(callDuration);
        } else if ("voice".equalsIgnoreCase(callType)) {
            voiceCallTest(callDuration);
        }

        // Save results
        writeResultsToFile(results, resultsFile);
    }

    private void handleLoginScreen(String username, String password) throws Exception {
        String useridResoureId = PACKAGE_ID + "sign_in_userid";
        String nextButtonResourceId = PACKAGE_ID + "sign_in_next_btn";
        UiObject useridField = new UiObject(new UiSelector().resourceId(useridResoureId));
        UiObject nextButton = new UiObject(new UiSelector().resourceId(nextButtonResourceId));
        useridField.setText(username);
        nextButton.clickAndWaitForNewWindow();

        String passwordResoureId = PACKAGE_ID + "signin_password";
        String signinButtonResourceId = PACKAGE_ID + "sign_in_btn";
        UiObject passwordField = new UiObject(new UiSelector().resourceId(passwordResoureId));
        UiObject signinButton = new UiObject(new UiSelector().resourceId(signinButtonResourceId));
        passwordField.setText(password);
        signinButton.clickAndWaitForNewWindow();
    }

    private void confirmAccess() throws Exception {
        // First time run requires confirmation to allow access to local files
        UiObject allowButton = new UiObject(new UiSelector().textContains("Allow")
                                                            .className("android.widget.Button"));
        // Some devices request multiple permisson rights so clear them all here
        while (allowButton.waitForExists(timeout)) {
            allowButton.clickAndWaitForNewWindow(timeout);
        }
    }

    private void selectContact(String name, String id) throws Exception {
        Timer timer = new Timer();
        timer.start();
        UiObject peopleTab;
        // Open the 'People' tab aka contacts view
        // On phones, it is represented by an image with description
        // On tablets, the full text is shown without a description
        try {
            peopleTab = getUiObjectByDescription("People", TEXT_VIEW);
        } catch (UiObjectNotFoundException e) {
            peopleTab = getUiObjectByText("People", TEXT_VIEW);
        }
        peopleTab.click();

        // On first startup, the app may take a while to load the display name,
        // so try twice before declaring failure
        UiObject contactCard;
        try {
            contactCard = getUiObjectByText(name, TEXT_VIEW);
        } catch (UiObjectNotFoundException e) {
            contactCard = getUiObjectByText(name, TEXT_VIEW);
        }
        contactCard.clickAndWaitForNewWindow();
        timer.end();
        results.put("select_contact", timer);
    }

    private void voiceCallTest(int duration) throws Exception {
        String testTag = "voice_call";
        Timer timer = new Timer();
        timer.start();
        makeCall(duration, false, testTag);
        timer.end();
        results.put(testTag, timer);
    }

    private void videoCallTest(int duration) throws Exception {
        String testTag = "video_call";
        Timer timer = new Timer();
        timer.start();
        makeCall(duration, true, testTag);
        timer.end();
        results.put(testTag, timer);
    }

    private void makeCall(int duration, boolean video, String testTag) throws Exception {
        String viewName = "com.skype.android.app.calling.CallActivity";
        String dumpsysTag = TAG + "_" + testTag;
        if (video && dumpsysEnabled) {
            initDumpsysSurfaceFlinger(PACKAGE, viewName);
            initDumpsysGfxInfo(PACKAGE);
        }

        String description = video ? "Video call" : "Call options";
        UiObject callButton = new UiObject(new UiSelector().descriptionContains(description));
        callButton.click();
        sleep(duration);

        if (video && dumpsysEnabled) {
            exitDumpsysSurfaceFlinger(PACKAGE, viewName, new File(outputDir, dumpsysTag + "_surfFlinger.log"));
            exitDumpsysGfxInfo(PACKAGE, new File(outputDir, dumpsysTag + "_gfxInfo.log"));
        }
    }

}
