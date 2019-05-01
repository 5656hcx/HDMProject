package com.comp3050.hearthealthmonitor.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.comp3050.hearthealthmonitor.utility.C_Database;

/** Extended database open helper class **/

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "health_database";
    public static final String TABLE_NAME_DATA = "band_data";
    public static final String TABLE_NAME_MSG = "message_archive";

    private static final String SQL_DROP_DATA = "DROP TABLE IF EXISTS " + TABLE_NAME_DATA;
    private static final String SQL_DROP_MSG = "DROP TABLE IF EXISTS " + TABLE_NAME_MSG;

    private static final String SQL_CREATE_DATA = "CREATE TABLE " + TABLE_NAME_DATA +
            "(_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            C_Database.TIMESTAMP + " INTEGER, " +
            C_Database.DEVICE_ID + " INTEGER, " +
            C_Database.USER_ID + " INTEGER, " +
            C_Database.STEPS + " INTEGER, " +
            C_Database.HEART_RATE + " INTEGER, " +
            C_Database.SYSTOLIC_BLOOD_PRESSURE + " INTEGER, " +
            C_Database.DIASTOLIC_BLOOD_PRESSURE + " INTEGER);";

    private static final String SQL_CREATE_MSG = "CREATE TABLE " + TABLE_NAME_MSG+
            "(_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            C_Database.TIMESTAMP + " INTEGER, " +
            C_Database.IMPORTANCE + " INTEGER, " +
            C_Database.TITLE + " TEXT, " +
            C_Database.CONTENT + " TEXT, " +
            C_Database.SUMMARY + " TEXT);";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_DATA);
        db.execSQL(SQL_CREATE_MSG);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DROP_DATA);
        db.execSQL(SQL_DROP_MSG);
        onCreate(db);
    }
}
