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

package com.arm.wlauto.uiauto.msword;

import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.Map;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;

// Import the uiautomator libraries
import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiScrollable;
import com.android.uiautomator.core.UiSelector;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;

import com.arm.wlauto.uiauto.UxPerfUiAutomation;

import static com.arm.wlauto.uiauto.BaseUiAutomation.FindByCriteria.BY_ID;
import static com.arm.wlauto.uiauto.BaseUiAutomation.FindByCriteria.BY_TEXT;
import static com.arm.wlauto.uiauto.BaseUiAutomation.FindByCriteria.BY_DESC;

public class UiAutomation extends UxPerfUiAutomation {

    public enum DeviceType { PHONE, TABLET, UNKNOWN };

    public static final int WAIT_TIMEOUT_5SEC = 5000;
    public static final int WAIT_TIMEOUT_1SEC = 1000;
    public static final int SCROLL_WAIT_TIME_MS = 100;
    public static final int SCROLL_SWIPE_COUNT = 10;
    public static final int SCROLL_SWIPE_STEPS = 50;
    public static final String CLASS_BUTTON = "android.widget.Button";
    public static final String CLASS_EDIT_TEXT = "android.widget.EditText";
    public static final String CLASS_SCROLL_VIEW = "android.widget.ScrollView";
    public static final String CLASS_TEXT_VIEW = "android.widget.TextView";
    public static final String CLASS_TOGGLE_BUTTON = "android.widget.ToggleButton";

    protected LinkedHashMap<String, Timer> results = new LinkedHashMap<String, Timer>();
    protected Timer timer = new Timer();
    protected SurfaceLogger logger;
    protected Bundle parameters;
    protected DeviceType deviceType;
    protected boolean dumpsysEnabled;
    protected String outputDir;
    protected String packageName;
    protected String packageID;
    protected String testFile;

    public void runUiAutomation() throws Exception {
        parameters = getParams();
        dumpsysEnabled = Boolean.parseBoolean(parameters.getString("dumpsys_enabled"));
        packageName = parameters.getString("package");
        outputDir = parameters.getString("output_dir");
        packageID = packageName + ":id/";
        testFile = parameters.getString("test_file", "");
        setScreenOrientation(ScreenOrientation.NATURAL);

        skipWelcomeScreen();
        checkDeviceType();
        // Run the main tests
        testCreateDocument("UX-Perf-Word");
        if (Boolean.parseBoolean(parameters.getString("use_test_file"))) {
            testExistingDocument(testFile);
        }
        deleteDocuments();

        unsetScreenOrientation();
        writeResultsToFile(results, parameters.getString("output_file"));
    }

    public void skipWelcomeScreen() throws Exception {
        startLogger("welcome_screen_progress");
        waitForProgress(WAIT_TIMEOUT_5SEC * 6); // initial setup time (upto 30 sec)
        stopLogger("welcome_screen_progress");
        startLogger("welcome_screen_skip");
        clickUiObject(BY_TEXT, "Skip", true); // skip welcome screen
        stopLogger("welcome_screen_skip");
    }

    public void checkDeviceType() throws Exception {
        UiSelector phonePanel = new UiSelector().resourceId(packageID + "docsui_landingview_phone_header_panel");
        UiSelector tabletPanel = new UiSelector().resourceId(packageID + "docsui_landingview_leftpane");
        if (new UiObject(phonePanel).exists()) {
            deviceType = DeviceType.PHONE;
        } else if (new UiObject(tabletPanel).exists()) {
            deviceType = DeviceType.TABLET;
        } else {
            deviceType = DeviceType.UNKNOWN; // will default to phone actions
        }
    }

    public boolean isTablet() {
        return deviceType == DeviceType.TABLET;
    }

    public void testCreateDocument(String documentName) throws Exception {
        newDocument("Newsletter");
        // Dismiss tooltip if it appears
        startLogger("new_doc_close_tooltip");
        UiObject tooltip = new UiObject(new UiSelector().textContains("Got it").className(CLASS_BUTTON));
        if (tooltip.waitForExists(WAIT_TIMEOUT_5SEC)) {
            tooltip.click();
        }
        stopLogger("new_doc_close_tooltip");

        // Rename document
        String titleId = isTablet() ? "DocTitle" : "DocTitlePortrait";
        startLogger("new_doc_rename");
        clickUiObject(BY_ID, packageID + titleId);
        clickUiObject(BY_ID, packageID + "OfcActionButton1"); // the 'X' button to clear
        UiObject nameField = getUiObjectByResourceId(packageID + "OfcEditText");
        nameField.setText(documentName);
        getUiDevice().pressEnter();
        stopLogger("new_doc_rename");
        UiObject dismissWarning = new UiObject(new UiSelector().className(CLASS_BUTTON).textContains("Dismiss"));
        if (dismissWarning.exists()) {
            dismissWarning.click();
        }

        // show command palette
        findText("Newsletter Title");
        UiSelector menuItems = new UiSelector().resourceId(packageID + "TabWidgetContent");
        if (isTablet()) {
            new UiObject(menuItems.textContains("Home")).click();
        } else {
            clickUiObject(BY_ID, packageID + "paletteToggleButton");
            clickUiObject(BY_ID, packageID + "ActiveTabButton");
            clickUiObject(BY_TEXT, "Home", CLASS_BUTTON);
        }

        // Text formatting
        clickUiObject(BY_DESC, "Bold", CLASS_TOGGLE_BUTTON);
        clickUiObject(BY_DESC, "Undo", CLASS_BUTTON);
        startLogger("format_font_style");
        clickUiObject(BY_DESC, "Bold", CLASS_TOGGLE_BUTTON);
        clickUiObject(BY_DESC, "Italic", CLASS_TOGGLE_BUTTON);
        clickUiObject(BY_DESC, "Underline", CLASS_TOGGLE_BUTTON);
        stopLogger("format_font_style");

        // Font size
        startLogger("format_font_size_menu");
        UiObject fontSizeBox = new UiObject(
            new UiSelector().resourceId(packageID + "fsComboBoxButton").instance(1));
        if (!fontSizeBox.exists()) {
            // we are on a tablet with portrait orientation
            new UiObject(new UiSelector().className(CLASS_TOGGLE_BUTTON).description("Font")).click();
            fontSizeBox = new UiObject(
                new UiSelector().resourceId(packageID + "fsComboBoxCalloutHorizontalButton").instance(1));
        }
        fontSizeBox.click();
        UiScrollable list = new UiScrollable(new UiSelector().resourceId(
            packageID + "galleryListControl").childSelector(new UiSelector().className(CLASS_SCROLL_VIEW)));
        list.scrollIntoView(new UiSelector().textContains("36"));
        stopLogger("format_font_size_menu");
        startLogger("format_font_size_action");
        clickUiObject(BY_TEXT, "36", CLASS_BUTTON, true);
        stopLogger("format_font_size_action");

        // Colours
        startLogger("format_font_colour");
        new UiObject(new UiSelector().descriptionMatches("Font Colou?r").className(CLASS_TOGGLE_BUTTON)).click();
        clickUiObject(BY_DESC, "Orange", true);
        stopLogger("format_font_colour");
        startLogger("format_font_highlight");
        clickUiObject(BY_DESC, "Highlight", CLASS_TOGGLE_BUTTON);
        clickUiObject(BY_DESC, "Yellow", true);
        stopLogger("format_font_highlight");

        // Dismiss format menu on phones
        if (!isTablet()) {
            getUiDevice().pressBack();
            getUiDevice().waitForIdle();
            getUiDevice().pressBack();
        }

        // Close file
        startLogger("new_doc_app_menu");
        if (isTablet()) {
            new UiObject(menuItems.textContains("File")).click();
        } else {
            clickUiObject(BY_ID, packageID + "Hamburger");
        }
        stopLogger("new_doc_app_menu");
        startLogger("new_doc_close_file");
        clickUiObject(BY_TEXT, "Close", CLASS_BUTTON, true);
        stopLogger("new_doc_close_file");
    }

    public void testExistingDocument(String documentName) throws Exception {
        openDocument(documentName);
        // Dismiss tooltip if it appears
        UiObject tooltip = new UiObject(new UiSelector().textContains("Got it").className(CLASS_BUTTON));
        startLogger("open_doc_close_tooltip");
        if (tooltip.waitForExists(WAIT_TIMEOUT_5SEC)) {
            tooltip.click();
        }
        stopLogger("open_doc_close_tooltip");

        // Scroll down the document
        for (int i = 0; i < SCROLL_SWIPE_COUNT; i++) {
            startLogger("scroll_down_" + i);
            getUiDevice().pressKeyCode(KeyEvent.KEYCODE_PAGE_DOWN);
            stopLogger("scroll_down_" + i);
        }

        // Insert shape
        UiSelector menuItems = new UiSelector().resourceId(packageID + "TabWidgetContent");
        if (isTablet()) {
            new UiObject(menuItems.textContains("Insert")).click();
        } else {
            clickUiObject(BY_ID, packageID + "paletteToggleButton");
            clickUiObject(BY_ID, packageID + "ActiveTabButton");
            clickUiObject(BY_TEXT, "Insert", CLASS_BUTTON);
        }
        startLogger("insert_shape_menu");
        clickUiObject(BY_TEXT, "Shapes", CLASS_TOGGLE_BUTTON);
        stopLogger("insert_shape_menu");
        UiScrollable list = new UiScrollable(new UiSelector().resourceId(
            packageID + "galleryListControl").childSelector(new UiSelector().className(CLASS_SCROLL_VIEW)));
        list.scrollIntoView(new UiSelector().descriptionContains("Rectangle"));
        startLogger("insert_shape_action");
        clickUiObject(BY_DESC, "Rectangle", true);
        stopLogger("insert_shape_action");

        // Edit shape
        startLogger("insert_shape_edit_fill");
        UiObject fillButton =
            new UiObject(new UiSelector().className(CLASS_TOGGLE_BUTTON).descriptionContains("Fill"));
        if (fillButton.exists()) {
            fillButton.click();
        } else {
            // we are on a tablet with portrait orientation
            clickUiObject(BY_TEXT, "Fill", CLASS_TOGGLE_BUTTON);
        }
        clickUiObject(BY_DESC, "Blue", true);
        stopLogger("insert_shape_edit_fill");

        // Insert image
        startLogger("insert_image_menu");
        if (isTablet()) {
            new UiObject(menuItems.textContains("Insert")).click();
        } else {
            clickUiObject(BY_ID, packageID + "ActiveTabButton");
            clickUiObject(BY_TEXT, "Insert", CLASS_BUTTON);
        }
        clickUiObject(BY_TEXT, "Pictures", CLASS_TOGGLE_BUTTON);
        clickUiObject(BY_TEXT, "Photos", CLASS_BUTTON, true);
        stopLogger("insert_image_menu");

        UiScrollable imagesList;
        if (isTablet()) {
            // Switch to grid view if necessary
            UiObject viewGrid = new UiObject(new UiSelector().descriptionContains("Grid view"));
            if (viewGrid.exists()) {
                viewGrid.click();
            }
            UiObject internalStorage = new UiObject(new UiSelector().textContains("Internal storage"));
            if (!internalStorage.waitForExists(WAIT_TIMEOUT_1SEC)) {
                clickUiObject(BY_DESC, "More options");
                clickUiObject(BY_TEXT, "Show SD card");
            }
            internalStorage.click();
            imagesList = new UiScrollable(new UiSelector().resourceId("com.android.documentsui:id/list"));
        } else {
            // phones
            UiObject imagesFolder = new UiObject(new UiSelector().className(CLASS_TEXT_VIEW).textContains("Images"));
            if (!imagesFolder.waitForExists(WAIT_TIMEOUT_1SEC)) {
                clickUiObject(BY_DESC, "Show roots");
            }
            imagesFolder.click();
            imagesList = new UiScrollable(new UiSelector().resourceId("com.android.documentsui:id/grid"));
        }
        imagesList.scrollIntoView(new UiSelector().textContains("wa-working").className(CLASS_TEXT_VIEW));
        clickUiObject(BY_TEXT, "wa-working", CLASS_TEXT_VIEW, true);

        // Select the first image
        startLogger("insert_image_action");
        if (isTablet()) {
            clickUiObject(BY_TEXT, ".jpg", true);
        } else {
            clickUiObject(BY_ID, "com.android.documentsui:id/date", true);
        }
        stopLogger("insert_image_action");

        // Edit image
        startLogger("insert_image_edit_style");
        clickUiObject(BY_TEXT, "Styles", CLASS_TOGGLE_BUTTON, true);
        UiSelector style =
            new UiSelector().resourceId(packageID + "GalleryNormalView").className(CLASS_BUTTON).instance(5);
        new UiObject(style).click();
        stopLogger("insert_image_edit_style");
        startLogger("insert_image_edit_wrap");
        clickUiObject(BY_TEXT, "Wrap Text");
        clickUiObject(BY_TEXT, "Square");
        stopLogger("insert_image_edit_wrap");
        // hide command pallete on phones
        if (!isTablet()) {
            clickUiObject(BY_ID, packageID + "CommandPaletteHandle");
        }

        // Scroll up the document
        for (int i = 0; i < SCROLL_SWIPE_COUNT; i++) {
            startLogger("scroll_up_" + i);
            getUiDevice().pressKeyCode(KeyEvent.KEYCODE_PAGE_UP);
            stopLogger("scroll_up_" + i);
        }
        getUiDevice().pressBack();
        waitForProgress(WAIT_TIMEOUT_5SEC); // Give the app a short while to settle down
    }

    public void openDocument(String documentName) throws Exception {
        clickUiObject(BY_TEXT, "Open", CLASS_BUTTON, true);
        gotoStorageFolder("wa-working");
        int listIndex = isTablet() ? 3 : 0;
        UiScrollable list = new UiScrollable(new UiSelector().className(CLASS_SCROLL_VIEW).instance(listIndex));
        startLogger("open_doc_action");
        list.scrollIntoView(new UiSelector().textContains(documentName));
        clickUiObject(BY_TEXT, documentName, true);
        stopLogger("open_doc_action");
    }

    protected void newDocument(String templateName) throws Exception {
        startLogger("new_doc_action");
        clickUiObject(BY_TEXT, "New", true);
        stopLogger("new_doc_action");

        // Save in WA working folder
        clickUiObject(BY_ID, packageID + "docsui_LocationListSplitButton");
        clickUiObject(BY_TEXT, "Select", CLASS_TEXT_VIEW, true);
        gotoStorageFolder("wa-working");
        clickUiObject(BY_ID, packageID + "docsui_backstage_select_button", true);
        UiObject setDefault = new UiObject(new UiSelector().className(CLASS_BUTTON).textContains("Set as default"));
        if (setDefault.exists()) {
            setDefault.click();
        }

        // Wait until templates have been downloaded (checks the first template)
        UiObject templates = new UiObject(new UiSelector().textContains("Take Notes"));
        templates.waitForExists(WAIT_TIMEOUT_5SEC);
        startLogger("new_doc_template_scroll");
        UiScrollable grid = new UiScrollable(new UiSelector().className("android.widget.GridView"));
        grid.scrollIntoView(new UiSelector().textContains(templateName));
        stopLogger("new_doc_template_scroll");
        startLogger("new_doc_template_select");
        clickUiObject(BY_TEXT, templateName, true);
        stopLogger("new_doc_template_select");
        startLogger("new_doc_progress");
        waitForProgress(WAIT_TIMEOUT_5SEC);
        stopLogger("new_doc_progress");
    }

    protected void deleteDocuments() throws Exception {
        UiObject moreOptions = new UiObject(new UiSelector()
                                            .resourceId(packageID + "list_entry_commands_launcher_button")
                                            .className(CLASS_TOGGLE_BUTTON));
        // Remove all docs from front page list
        int count = 0;
        while (moreOptions.exists()) {
            startLogger("delete_doc_recent_" + count);
            moreOptions.click();
            clickUiObject(BY_TEXT, "Remove from list");
            stopLogger("delete_doc_recent_" + count);
            count++;
        }
        // Remove created document from device
        clickUiObject(BY_TEXT, "Open", CLASS_BUTTON, true);
        gotoStorageFolder("wa-working");
        count = 0;
        while (moreOptions.exists()) {
            startLogger("delete_doc_device_" + count);
            moreOptions.click();
            clickUiObject(BY_TEXT, "Delete", true);
            clickUiObject(BY_TEXT, "Yes", CLASS_BUTTON);
            stopLogger("delete_doc_device_" + count);
            count++;
        }
    }

    protected void gotoStorageFolder(String folderName) throws Exception {
        clickUiObject(BY_TEXT, "This device");
        UiObject storage =
            new UiObject(new UiSelector().resourceId(packageID + "list_entry_title").textContains("Storage"));
        storage.click();
        int listIndex = isTablet() ? 3 : 0;
        UiScrollable list = new UiScrollable(new UiSelector().className(CLASS_SCROLL_VIEW).instance(listIndex));
        while (!list.exists() && listIndex > 0) {
            // support multi-column display of the file manager on tablets
            list = new UiScrollable(new UiSelector().className(CLASS_SCROLL_VIEW).instance(listIndex - 1));
        }
        list.scrollIntoView(new UiSelector().textContains(folderName));
        clickUiObject(BY_TEXT, folderName);
    }

    protected void findText(String textToFind) throws Exception {
        startLogger("find_text_button");
        clickUiObject(BY_DESC, "Find", CLASS_BUTTON);
        stopLogger("find_text_button");
        UiObject findField = clickUiObject(BY_ID, packageID + "OfcEditText", CLASS_EDIT_TEXT);
        findField.setText(textToFind);
        startLogger("find_text_action");
        clickUiObject(BY_ID, packageID + "OfcActionButton2", CLASS_BUTTON); // click search
        stopLogger("find_text_action");
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
