/*
 * TI's FM
 *
 * Copyright 2001-2011 Texas Instruments, Inc. - http://www.ti.com/
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
/*******************************************************************************\
 *
 *   FILE NAME:      FmRxHelp.java
 *
 *   BRIEF:          This file shows the version of the application and relevant
 *                   information.
 *
 *   DESCRIPTION:    General
 *
 *
 *
 *   AUTHOR:
 *
 \*******************************************************************************/
package com.ti.fmapp;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class FmRxHelp extends Activity {
    public static final String TAG = "FmTxHelp";

    /**
     * Called when the activity is first created.
     */

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fmrxhelp);

        //present the current app version
        TextView tv1 = (TextView) findViewById(R.id.txtversion);
        PackageManager manager = this.getPackageManager();
        PackageInfo info;
        String result = "";
        try {
            info = manager.getPackageInfo(this.getPackageName(), 0);
            result = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to get app version!");
        }
        tv1.setText(result);
    }

}
