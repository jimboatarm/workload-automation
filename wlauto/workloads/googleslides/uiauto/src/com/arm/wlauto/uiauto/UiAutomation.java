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

package com.arm.wlauto.uiauto.googleslides;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import android.os.Bundle;
import android.os.SystemClock;

// Import the uiautomator libraries
import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiScrollable;
import com.android.uiautomator.core.UiSelector;

import com.arm.wlauto.uiauto.UxPerfUiAutomation;

import static com.arm.wlauto.uiauto.BaseUiAutomation.FindByCriteria.BY_ID;
import static com.arm.wlauto.uiauto.BaseUiAutomation.FindByCriteria.BY_TEXT;
import static com.arm.wlauto.uiauto.BaseUiAutomation.FindByCriteria.BY_DESC;

public class UiAutomation extends UxPerfUiAutomation {

    public static final String PACKAGE = "com.google.android.apps.docs.editors.slides";
    public static final String PACKAGE_ID = PACKAGE + ":id/";

    public static final String CLASS_TEXT_VIEW = "android.widget.TextView";
    public static final String CLASS_IMAGE_VIEW = "android.widget.ImageView";
    public static final String CLASS_BUTTON = "android.widget.Button";
    public static final String CLASS_IMAGE_BUTTON = "android.widget.ImageButton";
    public static final String CLASS_TABLE_ROW = "android.widget.TableRow";

    public static final int DIALOG_WAIT_TIME_MS = 3000;
    public static final int SLIDE_WAIT_TIME_MS = 200;
    public static final int CLICK_REPEAT_INTERVAL_MS = 50;
    public static final int DEFAULT_SWIPE_STEPS = 10;

    public static final String NEW_DOC_FILENAME = "UX Perf Slides";

    public static final String SLIDE_TEXT_CONTENT =
        "class Workload(Extension):\n\tname = None\n\tdef init_resources(self, context):\n\t\tpass\n"
        + "\tdef validate(self):\n\t\tpass\n\tdef initialize(self, context):\n\t\tpass\n"
        + "\tdef setup(self, context):\n\t\tpass\n\tdef setup(self, context):\n\t\tpass\n"
        + "\tdef run(self, context):\n\t\tpass\n\tdef update_result(self, context):\n\t\tpass\n"
        + "\tdef teardown(self, context):\n\t\tpass\n\tdef finalize(self, context):\n\t\tpass\n";

    protected Map<String, Timer> results = new LinkedHashMap<String, Timer>();
    protected Timer timer = new Timer();
    protected SurfaceLogger logger;

    protected Bundle parameters;
    protected String outputDir;
    protected String localFile;
    protected int slideCount;
    protected boolean useLocalFile;

    public void parseParams(Bundle parameters) throws Exception {
        outputDir = parameters.getString("output_dir");
        localFile = parameters.getString("test_file");
        useLocalFile = localFile != null;
        if (useLocalFile) {
            slideCount = Integer.parseInt(parameters.getString("slide_count"));
        }
    }

    public void runUiAutomation() throws Exception {
        parameters = getParams();
        parseParams(parameters);
        setScreenOrientation(ScreenOrientation.NATURAL);
        skipWelcomeScreen();
        openAndCloseDrawer();
        enablePowerpointCompat();
        testEditNewSlidesDocument(NEW_DOC_FILENAME);
        if (useLocalFile) {
            testSlideshowFromStorage(localFile);
        }
        unsetScreenOrientation();
        writeResultsToFile(results, parameters.getString("results_file"));
    }

    protected void skipWelcomeScreen() throws Exception {
        startLogger("skip_welcome");
        clickUiObject(BY_TEXT, "Skip", true);
        stopLogger("skip_welcome");
        sleep(1);
        dismissWorkOfflineBanner(); // if it appears on the homescreen
    }

    protected void openAndCloseDrawer() throws Exception {
        startLogger("open_drawer");
        clickUiObject(BY_DESC, "drawer");
        getUiDevice().pressBack();
        stopLogger("open_drawer");
        sleep(1);
    }

    protected void enablePowerpointCompat() throws Exception {
        startLogger("enable_ppt_compat");
        clickUiObject(BY_DESC, "drawer");
        clickUiObject(BY_TEXT, "Settings", true);
        clickUiObject(BY_TEXT, "Create PowerPoint");
        getUiDevice().pressBack();
        stopLogger("enable_ppt_compat");
        sleep(1);
    }

    protected void testSlideshowFromStorage(String docName) throws Exception {
        // Sometimes docs deleted in __init__.py falsely appear on the app's home
        // For robustness, it's nice to remove these placeholders
        // However, the test should not crash because of it, so a silent catch is used
        UiObject docView = new UiObject(new UiSelector().textContains(docName));
        if (docView.waitForExists(1000)) {
            try {
                deleteDocument(docName);
            } catch (Exception e) {
                // do nothing
            }
        }

        // Open document
        startLogger("open_file_picker");
        clickUiObject(BY_DESC, "Open presentation");
        clickUiObject(BY_TEXT, "Device storage", true);
        stopLogger("open_file_picker");

        // Scroll through document list if necessary
        UiScrollable list = new UiScrollable(new UiSelector().className("android.widget.ListView"));
        list.scrollIntoView(new UiSelector().textContains(docName));
        startLogger("open_document");
        clickUiObject(BY_TEXT, docName);
        clickUiObject(BY_TEXT, "Open", CLASS_BUTTON, true);
        stopLogger("open_document");
        sleep(5);

        // Begin Slide show test
        // Note: A short wait-time is introduced before transition to the next slide to simulate
        // a real user's behaviour. Otherwise the test swipes through the slides too quickly.
        // These waits are not measured in the per-slide timings, and introduce a systematic
        // error in the overall slideshow timings.
        int centerY = getUiDevice().getDisplayHeight() / 2;
        int centerX = getUiDevice().getDisplayWidth() / 2;
        int slideIndex = 0;
        String testTag;
        SurfaceLogger slideLogger;

        // scroll forward in edit mode
        startLogger("slides_forward");
        while (++slideIndex < slideCount) {
            testTag = "slides_next_" + slideIndex;
            slideLogger = new SurfaceLogger(testTag, parameters);
            slideLogger.start();
            uiDeviceSwipeHorizontal(centerX + centerX/2, centerX - centerX/2,
                                    centerY, DEFAULT_SWIPE_STEPS);
            slideLogger.stop();
            results.put(testTag, slideLogger.result());
            SystemClock.sleep(SLIDE_WAIT_TIME_MS);
        }
        stopLogger("slides_forward");
        sleep(1);

        // scroll backward in edit mode
        startLogger("slides_reverse");
        while (--slideIndex > 0) {
            testTag = "slides_previous_" + slideIndex;
            slideLogger = new SurfaceLogger(testTag, parameters);
            slideLogger.start();
            uiDeviceSwipeHorizontal(centerX - centerX/2, centerX + centerX/2,
                                    centerY, DEFAULT_SWIPE_STEPS);
            slideLogger.stop();
            results.put(testTag, slideLogger.result());
            SystemClock.sleep(SLIDE_WAIT_TIME_MS);
        }
        stopLogger("slides_reverse");
        sleep(1);

        // scroll forward in slideshow mode
        startLogger("slideshow_open");
        clickUiObject(BY_DESC, "Start slideshow", true);
        stopLogger("slideshow_open");

        startLogger("slideshow_play");
        while (++slideIndex < slideCount) {
            testTag = "slideshow_next_" + slideIndex;
            slideLogger = new SurfaceLogger(testTag, parameters);
            slideLogger.start();
            uiDeviceSwipeHorizontal(centerX + centerX/2, centerX - centerX/2,
                                    centerY, DEFAULT_SWIPE_STEPS);
            slideLogger.stop();
            results.put(testTag, slideLogger.result());
            SystemClock.sleep(SLIDE_WAIT_TIME_MS);
        }
        stopLogger("slideshow_play");
        sleep(1);

        getUiDevice().pressBack();
        getUiDevice().pressBack();
    }

    protected void testEditNewSlidesDocument(String docName) throws Exception {
        // create new file
        startLogger("create_document");
        clickUiObject(BY_DESC, "New presentation");
        clickUiObject(BY_TEXT, "New PowerPoint", true);
        stopLogger("create_document");

        // first slide
        enterTextInSlide("Title", "WORKLOAD AUTOMATION");
        enterTextInSlide("Subtitle", "Measuring perfomance of different productivity apps on Android OS");
        saveDocument(docName);

        insertSlide("Title and Content");
        enterTextInSlide("title", "Extensions - Workloads");
        enterTextInSlide("Text placeholder", SLIDE_TEXT_CONTENT);
        clickUiObject(BY_DESC, "Text placeholder");
        clickUiObject(BY_DESC, "Format");
        clickUiObject(BY_TEXT, "Droid Sans");
        clickUiObject(BY_TEXT, "Droid Sans Mono");
        clickUiObject(BY_ID, PACKAGE_ID + "palette_back_button");
        UiObject decreaseFont = getUiObjectByDescription("Decrease text");
        repeatClickUiObject(decreaseFont, 20, CLICK_REPEAT_INTERVAL_MS);
        getUiDevice().pressBack();

        // get image from gallery and insert
        insertSlide("Title Only");
        clickUiObject(BY_DESC, "Insert");
        clickUiObject(BY_TEXT, "Image", true);
        clickUiObject(BY_TEXT, "Recent");
        clickUiObject(BY_ID, "com.android.documentsui:id/date", true);

        // last slide
        insertSlide("Title Slide");
        // insert "?" shape
        startLogger("shape_insert");
        clickUiObject(BY_DESC, "Insert");
        clickUiObject(BY_TEXT, "Shape");
        clickUiObject(BY_TEXT, "Buttons");
        clickUiObject(BY_DESC, "actionButtonHelp");
        stopLogger("shape_insert");
        UiObject resizeHandle = new UiObject(new UiSelector().descriptionMatches(".*Bottom[- ]left resize.*"));
        UiObject subtitle = getUiObjectByDescription("subTitle");
        startLogger("shape_resize");
        resizeHandle.dragTo(subtitle, 40);
        stopLogger("shape_resize");
        startLogger("shape_drag");
        UiObject shape = getUiObjectByDescription("actionButtonHelp");
        shape.dragTo(subtitle, 40);
        stopLogger("shape_drag");
        getUiDevice().pressBack();
        enterTextInSlide("title", "THE END. QUESTIONS?");

        sleep(1);
        getUiDevice().pressBack();
        dismissWorkOfflineBanner(); // if it appears on the homescreen
        deleteDocument(docName);
    }

    public void insertSlide(String slideLayout) throws Exception {
        sleep(1); // a bit of time to see previous slide
        UiObject view = getUiObjectByDescription("Insert slide");
        view.clickAndWaitForNewWindow();
        view = getUiObjectByText(slideLayout);
        view.clickAndWaitForNewWindow();
    }

    public void enterTextInSlide(String viewName, String textToEnter) throws Exception {
        UiObject view = getUiObjectByDescription(viewName);
        view.click();
        view.setText(textToEnter);
        try {
            clickUiObject(BY_DESC, "Done");
        } catch (UiObjectNotFoundException e) {
            clickUiObject(BY_ID, "android:id/action_mode_close_button");
        }
        // On some devices, keyboard pops up when entering text, and takes a noticeable
        // amount of time (few milliseconds) to disappear after clicking Done.
        // In these cases, trying to find a view immediately after entering text leads
        // to an exception, so a short wait-time is added for stability.
        SystemClock.sleep(SLIDE_WAIT_TIME_MS);
    }

    public void saveDocument(String docName) throws Exception {
        startLogger("save_dialog_1");
        clickUiObject(BY_TEXT, "SAVE");
        clickUiObject(BY_TEXT, "Device");
        stopLogger("save_dialog_1");

        startLogger("save_dialog_2");
        UiObject filename = getUiObjectByResourceId(PACKAGE_ID + "file_name_edit_text");
        filename.clearTextField();
        filename.setText(docName);
        clickUiObject(BY_TEXT, "Save", CLASS_BUTTON);
        stopLogger("save_dialog_2");

        // Overwrite if prompted
        // Should not happen under normal circumstances. But ensures test doesn't stop
        // if a previous iteration failed prematurely and was unable to delete the file.
        // Note that this file isn't removed during workload teardown as deleting it is
        // part of the UiAutomator test case.
        UiObject overwriteView = new UiObject(new UiSelector().textContains("already exists"));
        if (overwriteView.waitForExists(DIALOG_WAIT_TIME_MS)) {
            clickUiObject(BY_TEXT, "Overwrite");
        }
        sleep(1);
    }

    public void deleteDocument(String docName) throws Exception {
        startLogger("delete_dialog_1");
        UiObject doc = getUiObjectByText(docName);
        doc.longClick();
        clickUiObject(BY_TEXT, "Remove");
        stopLogger("delete_dialog_1");

        startLogger("delete_dialog_2");
        UiObject deleteButton;
        try {
            deleteButton = getUiObjectByText("Remove", CLASS_BUTTON);
        } catch (UiObjectNotFoundException e) {
            deleteButton = getUiObjectByText("Ok", CLASS_BUTTON);
        }
        deleteButton.clickAndWaitForNewWindow();
        stopLogger("delete_dialog_2");
        sleep(1);
    }

    public void dismissWorkOfflineBanner() throws Exception {
        UiObject banner = new UiObject(new UiSelector().textContains("Work offline"));
        if (banner.waitForExists(1000)) {
            clickUiObject(BY_TEXT, "Got it", CLASS_BUTTON);
        }
    }

    protected void startLogger(String name) throws Exception {
        logger = new SurfaceLogger(name, parameters);
        logger.start();
    }

    protected void stopLogger(String name) throws Exception {
        logger.stop();
        results.put(name, logger.result());
    }

}
