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

    public static final String ANDROID_WIDGET = "android.widget.";
    public static final String CLASS_TEXT_VIEW = ANDROID_WIDGET + "TextView";
    public static final String CLASS_IMAGE_VIEW = ANDROID_WIDGET + "ImageView";
    public static final String CLASS_BUTTON = ANDROID_WIDGET + "Button";
    public static final String CLASS_IMAGE_BUTTON = ANDROID_WIDGET + "ImageButton";
    public static final String CLASS_TABLE_ROW = ANDROID_WIDGET + "TableRow";
    public static final String CLASS_PROGRESS_BAR = ANDROID_WIDGET + "ProgressBar";
    public static final String CLASS_LIST_VIEW = ANDROID_WIDGET + "ListView";

    public static final int WAIT_TIMEOUT_1SEC = 1000;
    public static final int SLIDE_WAIT_TIME_MS = 200;
    public static final int DEFAULT_SWIPE_STEPS = 10;

    public static final String NEW_DOC_FILENAME = "WORKLOAD AUTOMATION";

    protected Map<String, Timer> results = new LinkedHashMap<String, Timer>();
    protected Timer timer = new Timer();
    protected SurfaceLogger logger;
    protected String PACKAGE_ID;
    protected DeviceType deviceType;
    protected Bundle parameters;
    protected String outputDir;
    protected String localFile;
    protected int slideCount;
    protected boolean useLocalFile;


    public void runUiAutomation() throws Exception {
        // Setup
        parameters = getParams();
        parseParams(parameters);
        setScreenOrientation(ScreenOrientation.NATURAL);
        changeAckTimeout(100);
        // UI automation begins here
        skipWelcomeScreen();
        sleep(1);
        dismissWorkOfflineBanner();
        sleep(1);
        enablePowerpointCompat();
        sleep(1);
        testEditNewSlidesDocument(NEW_DOC_FILENAME);
        if (useLocalFile) {
            sleep(1);
            testSlideshowFromStorage(localFile);
        }
        // UI automation ends here
        unsetScreenOrientation();
        writeResultsToFile(results, parameters.getString("results_file"));
    }

    public void parseParams(Bundle parameters) throws Exception {
        outputDir = parameters.getString("output_dir");
        localFile = parameters.getString("test_file");
        useLocalFile = localFile != null;
        if (useLocalFile) {
            slideCount = Integer.parseInt(parameters.getString("slide_count"));
        }
        PACKAGE_ID = parameters.getString("package") + ":id/";
    }

    public void dismissWorkOfflineBanner() throws Exception {
        UiObject banner = new UiObject(new UiSelector().textContains("Work offline"));
        if (banner.waitForExists(WAIT_TIMEOUT_1SEC)) {
            clickUiObject(BY_TEXT, "Got it", CLASS_BUTTON);
        }
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

    public void insertSlide(String slideLayout) throws Exception {
        clickUiObject(BY_DESC, "Add slide", true);
        clickUiObject(BY_TEXT, slideLayout, true);
    }

    public void insertImage() throws Exception {
        clickUiObject(BY_DESC, "Insert");
        clickUiObject(BY_TEXT, "Image", true);
        clickUiObject(BY_TEXT, "From photos");

        UiObject imagesFolder = new UiObject(new UiSelector().className(CLASS_TEXT_VIEW).textContains("Images"));
        if (!imagesFolder.waitForExists(WAIT_TIMEOUT_1SEC)) {
            clickUiObject(BY_DESC, "Show roots");
        }
        imagesFolder.click();

        clickUiObject(BY_TEXT, "wa-working", true);
        clickUiObject(BY_ID, "com.android.documentsui:id/date", true);
    }

    public void insertShape(String shapeName) throws Exception {
        startLogger("shape_insert");
        clickUiObject(BY_DESC, "Insert");
        clickUiObject(BY_TEXT, "Shape");
        clickUiObject(BY_DESC, shapeName);
        stopLogger("shape_insert");
    }

    public void modifyShape(String shapeName) throws Exception {
        UiObject resizeHandle = new UiObject(new UiSelector().descriptionMatches(".*Bottom[- ]right resize.*"));
        Rect bounds = resizeHandle.getVisibleBounds();
        int newX = bounds.left - 40;
        int newY = bounds.bottom - 40;
        startLogger("shape_resize");
        resizeHandle.dragTo(newX, newY, 40);
        stopLogger("shape_resize");

        UiSelector container = new UiSelector().resourceId(PACKAGE_ID + "main_canvas");
        UiSelector shapeSelector = container.childSelector(new UiSelector().descriptionContains(shapeName));
        startLogger("shape_drag");
        new UiObject(shapeSelector).dragTo(newX, newY, 40);
        stopLogger("shape_drag");
    }

    public void openDocument(String docName) throws Exception {
        clickUiObject(BY_DESC, "Open presentation");
        clickUiObject(BY_TEXT, "Device storage", true);
        UiScrollable list = new UiScrollable(new UiSelector().className(CLASS_LIST_VIEW));
        list.scrollIntoView(new UiSelector().textContains(docName));
        startLogger("document_open");
        clickUiObject(BY_TEXT, docName);
        clickUiObject(BY_TEXT, "Open", CLASS_BUTTON, true);
        stopLogger("document_open");
    }

    public void newDocument() throws Exception {
        startLogger("document_new");
        clickUiObject(BY_DESC, "New presentation");
        clickUiObject(BY_TEXT, "New PowerPoint", true);
        stopLogger("document_new");
    }
    
    public void saveDocument(String docName) throws Exception {
        UiObject saveActionButton = new UiObject(new UiSelector().resourceId(PACKAGE_ID + "action"));
        UiObject unsavedIndicator = new UiObject(new UiSelector().textContains("Not saved"));
        startLogger("document_save");
        if (saveActionButton.exists()) {
            saveActionButton.click();
        } else if (unsavedIndicator.exists()) {
            unsavedIndicator.click();
        }
        clickUiObject(BY_TEXT, "Device");
        clickUiObject(BY_TEXT, "Save", CLASS_BUTTON);
        stopLogger("document_save");

        // Overwrite if prompted
        // Should not happen under normal circumstances. But ensures test doesn't stop
        // if a previous iteration failed prematurely and was unable to delete the file.
        // Note that this file isn't removed during workload teardown as deleting it is
        // part of the UiAutomator test case.
        UiObject overwriteView = new UiObject(new UiSelector().textContains("already exists"));
        if (overwriteView.waitForExists(WAIT_TIMEOUT_1SEC)) {
            clickUiObject(BY_TEXT, "Overwrite");
        }
    }

    public void deleteDocument(String docName) throws Exception {
        UiObject doc = getUiObjectByText(docName);
        UiObject moreActions = doc.getFromParent(new UiSelector().descriptionContains("More actions"));
        startLogger("document_delete");
        moreActions.click();
        clickUiObject(BY_TEXT, "Delete");
        try {
            clickUiObject(BY_TEXT, "OK", CLASS_BUTTON, true);
        } catch (UiObjectNotFoundException e) {
            clickUiObject(BY_TEXT, "Remove", CLASS_BUTTON, true);
        }
        stopLogger("document_delete");
    }


    protected void skipWelcomeScreen() throws Exception {
        clickUiObject(BY_TEXT, "Skip", true);
    }

    protected void enablePowerpointCompat() throws Exception {
        startLogger("enable_pptmode");
        clickUiObject(BY_DESC, "drawer");
        clickUiObject(BY_TEXT, "Settings", true);
        clickUiObject(BY_TEXT, "Create PowerPoint");
        getUiDevice().pressBack();
        stopLogger("enable_pptmode");
    }

    protected void testEditNewSlidesDocument(String docName) throws Exception {
        // Init
        newDocument();
        checkDeviceType();

        // Slide 1 - Text
        enterTextInSlide("Title", docName);
        enterTextInSlide("Subtitle", "Measuring perfomance of different productivity apps on Android OS");
        
        // Save
        saveDocument(docName);
        sleep(1);

        // Slide 2 - Image
        insertSlide("Title only");
        insertImage();
        sleep(1);

        // Slide 3 - Shape
        insertSlide("Title slide");
        String shapeName = "Rounded rectangle";
        insertShape(shapeName);
        modifyShape(shapeName);
        getUiDevice().pressBack();
        sleep(1);

        // Tidy up
        getUiDevice().pressBack();
        dismissWorkOfflineBanner(); // if it appears on the homescreen
        deleteDocument(docName);
    }

    protected void testSlideshowFromStorage(String docName) throws Exception {
        // Open document
        openDocument(docName);
        waitForProgress(WAIT_TIMEOUT_1SEC*30);

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
        boolean status;

        // scroll forward in edit mode
        while (++slideIndex < slideCount) {
            testTag = "slideshow_editforward" + slideIndex;
            slideLogger = new SurfaceLogger(testTag, parameters);
            slideLogger.start();
            uiDeviceSwipeHorizontal(rightEdge, leftEdge, yCoordinate, DEFAULT_SWIPE_STEPS);
            status = waitForProgress(WAIT_TIMEOUT_1SEC*5);
            slideLogger.stop();
            if (status) {
                results.put(testTag, slideLogger.result());
            }
        }
        sleep(1);

        // scroll backward in edit mode
        while (--slideIndex > 0) {
            testTag = "slideshow_editbackward" + slideIndex;
            slideLogger = new SurfaceLogger(testTag, parameters);
            slideLogger.start();
            uiDeviceSwipeHorizontal(leftEdge, rightEdge, yCoordinate, DEFAULT_SWIPE_STEPS);
            status = waitForProgress(WAIT_TIMEOUT_1SEC*5);
            slideLogger.stop();
            if (status) {
                results.put(testTag, slideLogger.result());
            }
        }
        sleep(1);

        // run slideshow
        startLogger("slideshow_run");
        clickUiObject(BY_DESC, "Start slideshow", true);
        UiObject onDevice = new UiObject(new UiSelector().textContains("this device"));
        if (onDevice.waitForExists(WAIT_TIMEOUT_1SEC)) {
            onDevice.clickAndWaitForNewWindow();
            waitForProgress(WAIT_TIMEOUT_1SEC*30);
            UiObject presentation = new UiObject(new UiSelector().descriptionContains("Presentation Viewer"));
            presentation.waitForExists(WAIT_TIMEOUT_1SEC*30);
        }
        stopLogger("slideshow_run");
        sleep(1);

        // scroll forward in slideshow mode
        while (++slideIndex < slideCount) {
            testTag = "slideshow_playforward" + slideIndex;
            slideLogger = new SurfaceLogger(testTag, parameters);
            slideLogger.start();
            uiDeviceSwipeHorizontal(rightEdge, leftEdge, yCoordinate, DEFAULT_SWIPE_STEPS);
            status = waitForProgress(WAIT_TIMEOUT_1SEC*5);
            slideLogger.stop();
            if (status) {
                results.put(testTag, slideLogger.result());
            }
        }
        sleep(1);

        // scroll backward in slideshow mode
        while (--slideIndex > 0) {
            testTag = "slideshow_playbackward" + slideIndex;
            slideLogger = new SurfaceLogger(testTag, parameters);
            slideLogger.start();
            uiDeviceSwipeHorizontal(leftEdge, rightEdge, yCoordinate, DEFAULT_SWIPE_STEPS);
            status = waitForProgress(WAIT_TIMEOUT_1SEC*5);
            slideLogger.stop();
            if (status) {
                results.put(testTag, slideLogger.result());
            }
        }
        sleep(1);

        getUiDevice().pressBack();
        getUiDevice().pressBack();
    }

    protected void startLogger(String name) throws Exception {
        logger = new SurfaceLogger(name, parameters);
        logger.start();
    }

    protected void stopLogger(String name) throws Exception {
        logger.stop();
        results.put(name, logger.result());
    }

    protected boolean waitForProgress(int timeout) throws Exception {
        UiObject progress = new UiObject(new UiSelector().className(CLASS_PROGRESS_BAR));
        if (progress.exists()) {
            return progress.waitUntilGone(timeout);
        } else {
            return false;
        }
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

}
