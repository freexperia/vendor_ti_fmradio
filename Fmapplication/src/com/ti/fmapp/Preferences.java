/*
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
 *
 */
package com.ti.fmapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity {

    public static final String PREFS_NAME = "FMPREFS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }


    /**
     * Static Methods for easy accessing Preferences
     */


    /**
     * @param ctx App Context
     * @return If True use RDS value instead of preset name for notification display
     */
    public static boolean getNotificationsUseRDSinsteadPreset(Context ctx) {
        SharedPreferences settings = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return settings.getBoolean("rds_instead_preset", false);
    }

    public static void setNotificationsUseRDSinsteadPreset(Context ctx, boolean newValue) {
        SharedPreferences settings = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("rds_instead_preset", newValue);
        editor.commit();
    }


    /**
     * @param ctx App Context
     * @return Use notification bar controls
     */
    public static boolean getUseNotifications(Context ctx) {
        SharedPreferences settings = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return settings.getBoolean("use_notification_bar", true);
    }

    public static void setUseNotifications(Context ctx, boolean newValue) {
        SharedPreferences settings = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("use_notification_bar", newValue);
        editor.commit();
    }


    /**
     * @param ctx App Context
     * @return Print Debug Information to Logcat
     */
    public static boolean getPrintDebugInfo(Context ctx) {
        SharedPreferences settings = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return settings.getBoolean("print_debug_info", false);
    }

    public static void setPrintDebugInfo(Context ctx, boolean newValue) {
        SharedPreferences settings = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("print_debug_info", newValue);
        editor.commit();
    }

}
