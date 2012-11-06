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
package com.ti.fmapp.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.ti.fmapp.utils.Utils;


public class PreSetsDBHelper extends SQLiteOpenHelper {

    public static final String TABLE_PRESETS = "preset_stations";
    public static final String COLUMN_UID = "_uid";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_FREQUENCY = "frequency";

    private static final String DATABASE_NAME = "preset_stations.db";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_PRESETS + "( " + COLUMN_UID
            + " integer primary key autoincrement, " + COLUMN_NAME
            + " text not null," + COLUMN_FREQUENCY
            + " text not null"
            + ");";

    public PreSetsDBHelper(Context ctx) {
        super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Utils.debugFunc("[AlarmsDB] Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data", Log.VERBOSE, true);
        db.execSQL("DROP TABLE IF EXISTS" + TABLE_PRESETS);
        onCreate(db);
    }
}
