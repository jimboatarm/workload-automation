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

package com.arm.wlauto.uiauto.excel;

import android.os.Bundle;
import android.view.KeyEvent;

// Import the uiautomator libraries
import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiScrollable;
import com.android.uiautomator.core.UiSelector;

import com.arm.wlauto.uiauto.UxPerfUiAutomation;

import java.util.concurrent.TimeUnit;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;


public class UiAutomation extends UxPerfUiAutomation {

    public static String TAG = "uxperf_excel";

    public Bundle parameters;
    private int viewTimeoutSecs = 10;
    private long viewTimeout =  TimeUnit.SECONDS.toMillis(viewTimeoutSecs);
    private LinkedHashMap<String, Timer> timingResults = new LinkedHashMap<String, Timer>();

    public void runUiAutomation() throws Exception {
        parameters = getParams();

        setScreenOrientation(ScreenOrientation.NATURAL);
        confirmAccess();
        skipSignInView();

        // ----------------------------------------------------------------
        // Create simple workbook
        // ----------------------------------------------------------------
        newFile();
        newWorkbook();
        selectBlankWorkbook();
        dismissToolTip();
        createTable();
        nameWorkbook();
        pressBack();

        // ----------------------------------------------------------------
        // Load test workbook
        // ----------------------------------------------------------------
        if (Boolean.parseBoolean(parameters.getString("use_test_file"))) {
            openWorkbook("wa_test.xlsx");
            dismissToolTip();
            calculateCells();
            copyColumn();
            searchTable("4");
            gesturesTest();
        }

        pressBack();
        unsetScreenOrientation();
        writeResultsToFile(timingResults, parameters.getString("output_file"));
    }

    private void skipSignInView() throws Exception {
        UiObject skipSignIn = getUiObjectByText("Skip", "android.widget.TextView");
        skipSignIn.click();
    }

    private void newFile() throws Exception {
        UiObject newButton = new UiObject(new UiSelector().className("android.widget.Button").text("New"));

        // Check for the existence of the new button on non-tablet devices
        if (newButton.exists()) {
            newButton.click();
        }
    }

    private void openWorkbook(final String filename) throws Exception {

        String testTag = "open_workbook";
        SurfaceLogger logger = new SurfaceLogger(testTag, parameters);

        UiObject openButton = getUiObjectByText("Open", "android.widget.Button");
        openButton.click();

        navigateToTestFolder();

        UiObject fileEntry = getUiObjectByText(filename, "android.widget.TextView");

        logger.start();
        fileEntry.click();

        UiObject canvasContainer =
            new UiObject(new UiSelector().resourceId("com.microsoft.office.powerpoint:id/CanvasContainer"));

        canvasContainer.waitForExists(viewTimeout);

        logger.stop();
        timingResults.put(testTag, logger.result());
    }

    private void newWorkbook() throws Exception {
        UiObject docLocation = new UiObject(new UiSelector().text("This device > Documents"));

        if (!docLocation.waitForExists(viewTimeout)) {
            throw new UiObjectNotFoundException("Could not find \"document location view\".");
        }
        docLocation.click();

        UiObject selectLocation =
            getUiObjectByText("Select a different location...", "android.widget.TextView");
        selectLocation.click();

        navigateToTestFolder();

        UiObject selectButton = getUiObjectByText("Select", "android.widget.Button");
        selectButton.click();
    }

    private void navigateToTestFolder() throws Exception {
        UiObject deviceLocation = new UiObject(new UiSelector().text("This device"));
        deviceLocation.click();

        UiObject storageLocation =
            new UiObject(new UiSelector().resourceId(parameters.getString("package") + ":id/list_entry_title")
                                         .text("Storage"));
        storageLocation.click();

        UiScrollable scrollView =
            new UiScrollable(new UiSelector().resourceId("com.microsoft.office.excel:id/docsui_browse_folder_listview")
                                             .childSelector(new UiSelector().className("android.widget.ScrollView")));

        UiObject folderName =
            new UiObject(new UiSelector().className("android.widget.TextView").text("wa-working"));

        while (!folderName.exists()) {
            scrollView.scrollForward();
        }

        folderName.click();
    }

    private void selectBlankWorkbook() throws Exception {
        UiObject blankWorkBook = getUiObjectByText("Blank workbook", "android.widget.TextView");
        blankWorkBook.click();
    }

    private void dismissToolTip() throws Exception {
        UiObject gotItButton = getUiObjectByText("Got it!", "android.widget.Button");
        gotItButton.click();
    }

    private void createTable() throws Exception {

        String testTag = "create_table";
        SurfaceLogger logger = new SurfaceLogger(testTag, parameters);
        logger.start();

        String[] columnNames = {"Item", "Net", "Gross"};
        String[] rowValues   = {"Potatoes", "0.40", "0.48", "Onions", "0.90", "1.08"};

        final int nColumns = columnNames.length;
        final int nRows = rowValues.length / nColumns;

        // Create the header
        for (String columnName : columnNames) {
            setCell(columnName);
            pressDPadRight();
        }

        formatHeader();
        resetColumnPosition(nColumns);

        // Create the rows
        for (int i = 0; i < nRows; ++i) {
            for (int j = 0; j < nColumns; ++j) {
                setCell(rowValues[(i * nColumns) + j]);
                pressDPadRight();
            }
            resetColumnPosition(nColumns);
        }

        // Calculate the column totals
        setCell("TOTAL");
        pressDPadRight();
        setCell("=SUM(B2, B3)");
        pressDPadRight();
        setCell("=SUM(C2, C3)");
        resetColumnPosition(nColumns);

        logger.stop();
        timingResults.put(testTag, logger.result());
    }

    private void formatHeader() throws Exception {
        String testTag = "format_header";
        SurfaceLogger logger = new SurfaceLogger(testTag, parameters);

        highlightRow();
        openCommandPalette();
        logger.start();

        UiObject boldButton = getUiObjectByDescription("Bold", "android.widget.ToggleButton");
        boldButton.click();

        // On phones, the borderButton has a text field
        UiObject borderButtonPhone = new UiObject(new UiSelector().text("Borders"));

        // On tablets, the borderButton has a description field
        UiObject borderButtonTablet = new UiObject(new UiSelector().description("Borders"));

        if (borderButtonPhone.exists()) {
            borderButtonPhone.click();
        } else {
            borderButtonTablet.click();
        }

        UiObject borderStyle = getUiObjectByDescription("Top and Thick Bottom Border", "android.widget.Button");
        borderStyle.click();

        closeCommandPalette();

        logger.stop();
        timingResults.put(testTag, logger.result());
    }

    private void clickColumnHeader() throws Exception {
        UiObject headerCell =
            new UiObject(new UiSelector().resourceId("com.microsoft.office.excel:id/mainCanvas")
                                         .childSelector(new UiSelector().index(0)
                                         .childSelector(new UiSelector().index(0)
                                         .childSelector(new UiSelector().index(2)
                                         .childSelector(new UiSelector().index(2)
                                         .childSelector(new UiSelector().index(0)
                                         .childSelector(new UiSelector().index(0))))))));
        headerCell.click();
    }

    private UiObject getCellBox() throws Exception {
        UiObject cellBox =
            new UiObject(new UiSelector().resourceId("com.microsoft.office.excel:id/mainCanvas")
                                         .childSelector(new UiSelector().index(0)
                                         .childSelector(new UiSelector().index(0)
                                         .childSelector(new UiSelector().index(1)
                                         .childSelector(new UiSelector().index(0)
                                         .childSelector(new UiSelector().index(2)
                                         .childSelector(new UiSelector().index(2))))))));

        if (!cellBox.exists()) {
            return new UiObject(new UiSelector().resourceId("com.microsoft.office.excel:id/mainCanvas")
                                         .childSelector(new UiSelector().index(0)
                                         .childSelector(new UiSelector().index(0)
                                         .childSelector(new UiSelector().index(0)
                                         .childSelector(new UiSelector().index(0)
                                         .childSelector(new UiSelector().index(2)
                                         .childSelector(new UiSelector().index(2))))))));
        }

        return cellBox;
    }

    private void copyColumn() throws Exception {
        String testTag = "copy_data_column";

        SurfaceLogger logger = new SurfaceLogger(testTag, parameters);
        logger.start();

        clickColumnHeader();
        UiObject copyButton = getUiObjectByDescription("Copy", "android.widget.Button");
        copyButton.click();

        UiObject sheet2 = getUiObjectByText("Sheet2", "android.widget.TextView");
        sheet2.click();

        getCellBox().click();
        UiObject pasteButton = getUiObjectByDescription("Paste", "android.widget.Button");
        pasteButton.click();

        logger.stop();
        timingResults.put(testTag, logger.result());

        getUiDevice().waitForIdle();
    }

    private void calculateCells() throws Exception {
        String testTag = "calculate_cells";

        // log cumulative results
        SurfaceLogger logger = new SurfaceLogger(testTag, parameters);
        logger.start();

        String[] rootValues = {"2", "100", "3.14"};

        UiObject enterButton =
            new UiObject(new UiSelector().resourceId("com.microsoft.office.excel:id/enterButton"));

        for (String value : rootValues) {
            setCell(value);
            enterButton.click();
        }

        logger.stop();
        timingResults.put(testTag, logger.result());
    }

    private void gesturesTest() throws Exception {
        String testTag = "gestures";

        // Perform pinch tests on the current workbook
        LinkedHashMap<String, GestureTestParams> testParams = new LinkedHashMap<String, GestureTestParams>();
        testParams.put("pinch_out", new GestureTestParams(GestureType.PINCH, PinchType.OUT, 100, 50));
        testParams.put("pinch_in", new GestureTestParams(GestureType.PINCH, PinchType.IN, 100, 50));

        Iterator<Entry<String, GestureTestParams>> it = testParams.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, GestureTestParams> pair = it.next();
            GestureType type = pair.getValue().gestureType;
            Direction dir = pair.getValue().gestureDirection;
            PinchType pinch = pair.getValue().pinchType;
            int steps = pair.getValue().steps;
            int percent = pair.getValue().percent;

            UiObject view = new UiObject(new UiSelector().enabled(true));

            if (!view.waitForExists(viewTimeout)) {
                throw new UiObjectNotFoundException("Could not find \"table view\".");
            }

            String runName = String.format(testTag + "_" + pair.getKey());
            SurfaceLogger logger = new SurfaceLogger(runName, parameters);
            logger.start();

            switch (type) {
                case PINCH:
                    uiObjectVertPinch(view, pinch, steps, percent);
                    break;
                default:
                    break;
            }

            logger.stop();
            timingResults.put(runName, logger.result());
        }
    }

    private void searchTable(final String searchTerm) throws Exception {
        String testTag = "search_table";

        SurfaceLogger logger = new SurfaceLogger(testTag, parameters);
        logger.start();

        UiObject findButton = getUiObjectByDescription("Find", "android.widget.Button");
        findButton.click();

        UiObject editText = new UiObject(new UiSelector().className("android.widget.EditText"));
        editText.setText(searchTerm);
        pressEnter();

        logger.stop();
        timingResults.put(testTag, logger.result());
    }

    private void nameWorkbook() throws Exception {
        UiObject docTitleName;

        // Resource IDs differ between phones and tablet devices
        try {
            docTitleName = getUiObjectByResourceId("com.microsoft.office.excel:id/DocTitlePortrait",
                                                   "android.widget.TextView");
        } catch (UiObjectNotFoundException e) {
            docTitleName = getUiObjectByResourceId("com.microsoft.office.excel:id/DocTitle",
                                                   "android.widget.TextView");
        }

        docTitleName.setText("WA_Test_Book");
        pressEnter();
    }

    // Helper method for setting the currently selected cell's text content
    private void setCell(final String value) throws Exception {
        getCellBox().setText(value);
        getUiDevice().pressDPadCenter();
    }

    // Helper method for resetting the current cell position to the first column
    private void resetColumnPosition(final int nColumns) throws Exception {
        pressDPadDown();

        for (int i = 0; i < nColumns; ++i) {
            pressDPadLeft();
        }
    }

    // Helper method for highlighting the current cell's row and bringing up the format palette
    private void highlightRow() throws Exception {
        UiObject row = new UiObject(new UiSelector().resourceId("com.microsoft.office.excel:id/mainCanvas")
                                                    .childSelector(new UiSelector().index(0)
                                                    .childSelector(new UiSelector().index(0)
                                                    .childSelector(new UiSelector().index(0)
                                                    .childSelector(new UiSelector().index(2)
                                                    .childSelector(new UiSelector().index(0)))))));
        row.click();
    }

    // Helper method for opening the command palette on non-tablet devices
    private void openCommandPalette() throws Exception {
        UiObject paletteToggleButton =
            new UiObject(new UiSelector().resourceId("com.microsoft.office.excel:id/paletteToggleButton"));

        if (paletteToggleButton.exists()) {
            paletteToggleButton.click();
        }
    }

    // Helper method for closing the command palette on non-tablet devices
    private void closeCommandPalette() throws Exception {
        UiObject commandPaletteHandle =
            new UiObject(new UiSelector().resourceId("com.microsoft.office.excel:id/CommandPaletteHandle"));

        if (commandPaletteHandle.exists()) {
            commandPaletteHandle.click();
        }
    }
}
