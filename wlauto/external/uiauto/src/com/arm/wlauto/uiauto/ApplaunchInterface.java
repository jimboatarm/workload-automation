
/*    Copyright 2013-2016 ARM Limited
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

package com.arm.wlauto.uiauto;

import android.os.Bundle;
import android.util.Log;

// Import the uiautomator libraries
import com.android.uiautomator.core.UiObject;

public interface ApplaunchInterface {
	//Method that sets the userbeginObject per workload. This is workload specific and 
	//is expected to be overridden by the workload that inherits this class.
	public UiObject setUserBeginObject();
	//This method has the Uiautomation methods for clearing the initial run
	//dialogues of an application package. This is workload specific and 
	//is expected to be overridden by the workload that inherits this class.
	public void clearDialogues();
	public String getLaunchCommand();
}
