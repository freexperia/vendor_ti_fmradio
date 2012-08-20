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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.ti.fmapp.logic.PreSetRadio;
import com.ti.fmapp.utils.Utils;

import java.util.ArrayList;

public class PreSetsDB {

    // Database fields
    private SQLiteDatabase database;
    private PreSetsDBHelper dbHelper;
    private Context mCtx;
    private String[] allColumns = {PreSetsDBHelper.COLUMN_UID, PreSetsDBHelper.COLUMN_NAME,
            PreSetsDBHelper.COLUMN_FREQUENCY};


    public PreSetsDB(Context ctx) {
        mCtx = ctx;
    }

    public PreSetsDB open() throws SQLException {
        dbHelper = new PreSetsDBHelper(mCtx);
        database = dbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        try {
            dbHelper.close();
        } catch (Exception e) {
            Utils.debugFunc("Could not close Radio PreSets DB", Log.ERROR);
            e.printStackTrace();
        }
    }

    public long createPreSetItem(String name, String frequency) {
        ContentValues values = new ContentValues();
        values.put(PreSetsDBHelper.COLUMN_NAME, name);
        values.put(PreSetsDBHelper.COLUMN_FREQUENCY, frequency);
        return database.insert(PreSetsDBHelper.TABLE_PRESETS, null, values);
    }

    public void deletePreSetItem(PreSetRadio preSetRadio) {
        long id = preSetRadio.getUid();
        database.delete(PreSetsDBHelper.TABLE_PRESETS, PreSetsDBHelper.COLUMN_UID + " = " + id, null);
    }

    public ArrayList<PreSetRadio> getAllPreSetRadios() {
        ArrayList<PreSetRadio> result = new ArrayList<PreSetRadio>(6);
        Cursor cursor = database.query(PreSetsDBHelper.TABLE_PRESETS,
                allColumns, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            PreSetRadio preSetRadio = cursorToPreSetRadio(cursor);
            result.add(preSetRadio);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return result;
    }

    public PreSetRadio getPreSetRadioByUID(long uid) {
        Cursor cursor = database.query(PreSetsDBHelper.TABLE_PRESETS,
                allColumns, PreSetsDBHelper.COLUMN_UID + "=" + uid, null, null, null, null);
        cursor.moveToFirst();
        PreSetRadio alarmItem = cursorToPreSetRadio(cursor);
        // Make sure to close the cursor
        cursor.close();
        return alarmItem;
    }

    public void updateRadioPreSet(long uid, String name, String frequency) {
        Utils.debugFunc("Will update preset state.", Log.VERBOSE);
        String updatePreset = "UPDATE preset_stations SET name = '" + name + "' WHERE _uid=" + uid + ";";
        database.execSQL(updatePreset);
        updatePreset = "UPDATE preset_stations SET frequency = '" + frequency + "' WHERE _uid=" + uid + ";";
        database.execSQL(updatePreset);
    }

    private PreSetRadio cursorToPreSetRadio(Cursor cursor) {
        PreSetRadio preSetRadio = new PreSetRadio();
        preSetRadio.setUid(cursor.getInt(0));
        preSetRadio.setStationName(cursor.getString(1));
        preSetRadio.setStationFrequency(cursor.getString(2));
        return preSetRadio;
    }

}
