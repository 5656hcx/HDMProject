package com.comp3050.hearthealthmonitor.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.comp3050.hearthealthmonitor.database.DBHelper;
import com.comp3050.hearthealthmonitor.database.MiBandContract;
import com.comp3050.hearthealthmonitor.utility.C_Database;

/** An intent service to perform following core tasks：
 *  1. check if there is new data from smart bands
 *  2. fetch the new data and store it to private database
 **/

public class DatabaseSyncService extends IntentService {

    public DatabaseSyncService() {
        super("DatabaseSyncService");
    }

    @Override
        protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            DBHelper dbHelper = new DBHelper(this);
            Cursor import_cursor = getContentResolver().query(
                    MiBandContract.URI, null, null, null,
                    MiBandContract.TIMESTAMP); // gadgetbridge's cursor
            if (import_cursor != null) {
                if (import_cursor.moveToFirst()) {
                    try {
                        SQLiteDatabase export_db = dbHelper.getWritableDatabase();
                        Cursor export_cursor = export_db.query(
                                DBHelper.TABLE_NAME_DATA, null, null, null, null, null,
                                C_Database.TIMESTAMP + " DESC"); // application's cursor
                        ContentValues contentValues = new ContentValues();
                        if (export_cursor.moveToFirst()) {
                            int startpoint = export_cursor.getInt(export_cursor.getColumnIndex(C_Database.TIMESTAMP));
                            do {
                                if (import_cursor.getInt(import_cursor.getColumnIndex(MiBandContract.TIMESTAMP)) > startpoint)
                                    break;
                            } while (import_cursor.moveToNext());
                        }
                        if (!import_cursor.isAfterLast()) {
                            do {
                                contentValues.put(C_Database.TIMESTAMP, import_cursor.getInt(import_cursor.getColumnIndex(MiBandContract.TIMESTAMP)));
                                contentValues.put(C_Database.DEVICE_ID, import_cursor.getInt(import_cursor.getColumnIndex(MiBandContract.DEVICE_ID)));
                                contentValues.put(C_Database.USER_ID, import_cursor.getInt(import_cursor.getColumnIndex(MiBandContract.USER_ID)));
                                contentValues.put(C_Database.STEPS, import_cursor.getInt(import_cursor.getColumnIndex(MiBandContract.STEPS)));
                                contentValues.put(C_Database.HEART_RATE, import_cursor.getInt(import_cursor.getColumnIndex(MiBandContract.HEART_RATE)));
                                contentValues.put(C_Database.SYSTOLIC_BLOOD_PRESSURE, 120 + (int)(Math.random()*81-10));  // NOT FROM MI_BAND
                                contentValues.put(C_Database.DIASTOLIC_BLOOD_PRESSURE, 80 + (int)(Math.random()*21-10)); // NOT FROM MI_BAND
                                export_db.insert(DBHelper.TABLE_NAME_DATA, null, contentValues);
                            } while (import_cursor.moveToNext());
                        }
                        export_cursor.close();
                        export_db.close();
                    } catch (SQLiteException ignored) {}
                }
                import_cursor.close();
                startService(new Intent(this, CheckDataService.class));
            }
        }
    }

}
