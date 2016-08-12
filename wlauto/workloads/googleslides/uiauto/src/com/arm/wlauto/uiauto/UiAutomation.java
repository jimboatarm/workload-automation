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

import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;

// Import the uiautomator libraries
import com.android.uiautomator.core.Configurator;
import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiScrollable;
import com.android.uiautomator.core.UiSelector;

import com.arm.wlauto.uiauto.UxPerfUiAutomation;

import static com.arm.wlauto.uiauto.BaseUiAutomation.FindByCriteria.BY_ID;
import static com.arm.wlauto.uiauto.BaseUiAutomation.FindByCriteria.BY_TEXT;
import static com.arm.wlauto.uiauto.BaseUiAutomation.FindByCriteria.BY_DESC;

public class UiAutomation extends UxPerfUiAutomation {

    public enum DeviceType { PHONE, TABLET, UNKNOWN };

    public static final String PACKAGE = "com.google.android.apps.docs.editors.slides";
    public static final String PACKAGE_ID = PACKAGE + ":id/";

    public static final String CLASS_TEXT_VIEW = "android.widget.TextView";
    public static final String CLASS_IMAGE_VIEW = "android.widget.ImageView";
    public static final String CLASS_BUTTON = "android.widget.Button";
    public static final String CLASS_IMAGE_BUTTON = "android.widget.ImageButton";
    public static final String CLASS_TABLE_ROW = "android.widget.TableRow";

    public static final int WAIT_TIMEOUT_30SEC = 30000;
    public static final int WAIT_TIMEOUT_5SEC = 5000;
    public static final int WAIT_TIMEOUT_1SEC = 1000;
    public static final int SLIDE_WAIT_TIME_MS = 200;
    public static final int DEFAULT_SWIPE_STEPS = 10;

    public static final String NEW_DOC_FILENAME = "WORKLOAD AUTOMATION";

    protected Map<String, Timer> results = new LinkedHashMap<String, Timer>();
    protected Timer timer = new Timer();
    protected SurfaceLogger logger;

    protected DeviceType deviceType;
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
        changeAckTimeout(100);
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

    public void checkDeviceType() throws Exception {
        UiSelector slidePicker = new UiSelector().resourceId(PACKAGE_ID + "slide_picker");
        UiSelector currentSlide = new UiSelector().resourceId(PACKAGE_ID + "current_slide_panel");
        UiSelector filmstrip = new UiSelector().resourceId(PACKAGE_ID + "filmstrip_layout");
        if (new UiObject(slidePicker).exists() && new UiObject(currentSlide).exists()) {
            deviceType = DeviceType.TABLET;
        } else if (new UiObject(filmstrip).exists()) {
            deviceType = DeviceType.PHONE;
        } else {
            deviceType = DeviceType.UNKNOWN; // will default to phone actions
        }
    }

    public boolean isTablet() {
        return deviceType == DeviceType.TABLET;
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
        waitForProgress(WAIT_TIMEOUT_30SEC);

        // Begin Slide show test
        // Note: Using coordinates slightly offset from the slide edges avoids accidentally
        // selecting any shapes or text boxes inside the slides while swiping, which may
        // cause the view to switch into edit mode and fail the test
        UiObject slideCanvas = new UiObject(new UiSelector().resourceId(PACKAGE_ID + "main_canvas"));
        Rect canvasBounds = slideCanvas.getVisibleBounds();
        int leftEdge = canvasBounds.left + 10;
        int rightEdge = canvasBounds.right - 10;
        int yCoordinate = canvasBounds.top + 5;
        int slideIndex = 0;
        String testTag;
        SurfaceLogger slideLogger;

        // scroll forward in edit mode
        startLogger("slides_forward");
        while (++slideIndex < slideCount) {
            testTag = "slides_next_" + slideIndex;
            slideLogger = new SurfaceLogger(testTag, parameters);
            slideLogger.start();
            uiDeviceSwipeHorizontal(rightEdge, leftEdge, yCoordinate, DEFAULT_SWIPE_STEPS);
            slideLogger.stop();
            results.put(testTag, slideLogger.result());
            waitForProgress(WAIT_TIMEOUT_5SEC);
        }
        stopLogger("slides_forward");
        sleep(1);

        // scroll backward in edit mode
        startLogger("slides_reverse");
        while (--slideIndex > 0) {
            testTag = "slides_previous_" + slideIndex;
            slideLogger = new SurfaceLogger(testTag, parameters);
            slideLogger.start();
            uiDeviceSwipeHorizontal(leftEdge, rightEdge, yCoordinate, DEFAULT_SWIPE_STEPS);
            slideLogger.stop();
            results.put(testTag, slideLogger.result());
            waitForProgress(WAIT_TIMEOUT_5SEC);
        }
        stopLogger("slides_reverse");
        sleep(1);

        // scroll forward in slideshow mode
        startLogger("slideshow_open");
        clickUiObject(BY_DESC, "Start slideshow", true);
        UiObject onDevice = new UiObject(new UiSelector().textContains("this device"));
        if (onDevice.waitForExists(WAIT_TIMEOUT_1SEC)) {
            onDevice.clickAndWaitForNewWindow();
            waitForProgress(WAIT_TIMEOUT_30SEC);
            UiObject presentation = new UiObject(new UiSelector().descriptionContains("Presentation Viewer"));
            presentation.waitForExists(WAIT_TIMEOUT_30SEC);
        }
        stopLogger("slideshow_open");

        startLogger("slideshow_play");
        while (++slideIndex < slideCount) {
            testTag = "slideshow_next_" + slideIndex;
            slideLogger = new SurfaceLogger(testTag, parameters);
            slideLogger.start();
            uiDeviceSwipeHorizontal(rightEdge, leftEdge, yCoordinate, DEFAULT_SWIPE_STEPS);
            slideLogger.stop();
            results.put(testTag, slideLogger.result());
            waitForProgress(WAIT_TIMEOUT_5SEC);
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

        // Check and set device type
        checkDeviceType();

        // first slide
        enterTextInSlide("Title", docName);
        enterTextInSlide("Subtitle", "Measuring perfomance of different productivity apps on Android OS");
        saveDocument(docName);

        // get image from gallery and insert
        insertSlide("Title only");
        clickUiObject(BY_DESC, "Insert");
        clickUiObject(BY_TEXT, "Image", true);
        clickUiObject(BY_TEXT, "From photos");
        // // Switch to grid view if necessary
        // UiObject viewGrid = new UiObject(new UiSelector().descriptionContains("Grid view"));
        // if (viewGrid.waitForExists(WAIT_TIMEOUT_1SEC)) {
        //     viewGrid.click();
        // }
        UiObject imagesFolder = new UiObject(new UiSelector().className(CLASS_TEXT_VIEW).textContains("Images"));
        if (!imagesFolder.waitForExists(WAIT_TIMEOUT_1SEC)) {
            clickUiObject(BY_DESC, "Show roots");
        }
        imagesFolder.click();
        // UiScrollable imagesList = new UiScrollable(new UiSelector().resourceId("com.android.documentsui:id/grid"));
        // imagesList.scrollIntoView(new UiSelector().textContains("wa-working").className(CLASS_TEXT_VIEW));
        clickUiObject(BY_TEXT, "wa-working", true);
        clickUiObject(BY_ID, "com.android.documentsui:id/date", true);
        sleep(1);

        // last slide - insert shape
        String shapeName = "Rounded rectangle";
        insertSlide("Title slide");
        startLogger("shape_insert");
        clickUiObject(BY_DESC, "Insert");
        clickUiObject(BY_TEXT, "Shape");
        clickUiObject(BY_DESC, shapeName);
        stopLogger("shape_insert");
        UiObject resizeHandle = new UiObject(new UiSelector().descriptionMatches(".*Bottom[- ]right resize.*"));
        Rect bounds = resizeHandle.getVisibleBounds();
        int newX = bounds.left - 40;
        int newY = bounds.bottom - 40;
        startLogger("shape_resize");
        resizeHandle.dragTo(newX, newY, 40);
        stopLogger("shape_resize");
        startLogger("shape_drag");
        UiSelector container = new UiSelector().resourceId(PACKAGE_ID + "main_canvas");
        UiSelector shapeSelector = container.childSelector(new UiSelector().descriptionContains(shapeName));
        new UiObject(shapeSelector).dragTo(newX, newY, 40);
        stopLogger("shape_drag");
        getUiDevice().pressBack();

        sleep(1);
        getUiDevice().pressBack();
        dismissWorkOfflineBanner(); // if it appears on the homescreen
        deleteDocument(docName);
    }

    public void insertSlide(String slideLayout) throws Exception {
        sleep(1); // a bit of time to see previous slide
        clickUiObject(BY_DESC, "Add slide", true);
        clickUiObject(BY_TEXT, slideLayout, true);
    }

    private long changeAckTimeout(long newTimeout) {
        Configurator config = Configurator.getInstance();
        long oldTimeout = config.getActionAcknowledgmentTimeout();
        config.setActionAcknowledgmentTimeout(newTimeout);
        return oldTimeout;
    }

    private void tapOpenArea() throws Exception {
        UiObject openArea = getUiObjectByResourceId(PACKAGE_ID + "punch_view_pager");
        Rect bounds = openArea.getVisibleBounds();
        tapDisplay(bounds.centerX(), bounds.top + 10); // 10px from top of view
    }

    public void enterTextInSlide(String viewName, String textToEnter) throws Exception {
        UiSelector container = new UiSelector().resourceId(PACKAGE_ID + "main_canvas");
        UiObject view = new UiObject(container.childSelector(new UiSelector().descriptionContains(viewName)));
        view.click();
        getUiDevice().pressEnter();
        view.setText(textToEnter);
        tapOpenArea();
        // On some devices, keyboard pops up when entering text, and takes a noticeable
        // amount of time (few milliseconds) to disappear after clicking Done.
        // In these cases, trying to find a view immediately after entering text leads
        // to an exception, so a short wait-time is added for stability.
        SystemClock.sleep(SLIDE_WAIT_TIME_MS);
    }

    public void saveDocument(String docName) throws Exception {
        UiObject saveActionButton = new UiObject(new UiSelector().resourceId(PACKAGE_ID + "action"));
        UiObject unsavedIndicator = new UiObject(new UiSelector().textContains("Not saved"));
        startLogger("save_dialog_1");
        if (saveActionButton.exists()) {
            saveActionButton.click();
        } else if (unsavedIndicator.exists()) {
            unsavedIndicator.click();
        }
        clickUiObject(BY_TEXT, "Device");
        stopLogger("save_dialog_1");

        startLogger("save_dialog_2");
        clickUiObject(BY_TEXT, "Save", CLASS_BUTTON);
        stopLogger("save_dialog_2");

        // Overwrite if prompted
        // Should not happen under normal circumstances. But ensures test doesn't stop
        // if a previous iteration failed prematurely and was unable to delete the file.
        // Note that this file isn't removed during workload teardown as deleting it is
        // part of the UiAutomator test case.
        UiObject overwriteView = new UiObject(new UiSelector().textContains("already exists"));
        if (overwriteView.waitForExists(WAIT_TIMEOUT_1SEC)) {
            clickUiObject(BY_TEXT, "Overwrite");
        }
        sleep(1);
    }

    public void deleteDocument(String docName) throws Exception {
        startLogger("delete_dialog_1");
        UiObject doc = getUiObjectByText(docName);
        UiObject moreActions = doc.getFromParent(new UiSelector().descriptionContains("More actions"));
        moreActions.click();
        clickUiObject(BY_TEXT, "Delete");
        stopLogger("delete_dialog_1");

        startLogger("delete_dialog_2");
        try {
            clickUiObject(BY_TEXT, "OK", CLASS_BUTTON, true);
        } catch (UiObjectNotFoundException e) {
            clickUiObject(BY_TEXT, "Remove", CLASS_BUTTON, true);
        }
        stopLogger("delete_dialog_2");
        sleep(1);
    }

    public void dismissWorkOfflineBanner() throws Exception {
        UiObject banner = new UiObject(new UiSelector().textContains("Work offline"));
        if (banner.waitForExists(WAIT_TIMEOUT_1SEC)) {
            clickUiObject(BY_TEXT, "Got it", CLASS_BUTTON);
        }
    }

    protected boolean waitForProgress(int timeout) throws Exception {
        UiObject progress = new UiObject(new UiSelector().className("android.widget.ProgressBar"));
        if (progress.exists()) {
            return progress.waitUntilGone(timeout);
        } else {
            return false;
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
