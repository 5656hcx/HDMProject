package com.comp3050.hearthealthmonitor.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;

import com.comp3050.hearthealthmonitor.R;
import com.comp3050.hearthealthmonitor.activity.MainActivity;
import com.comp3050.hearthealthmonitor.database.DBHelper;
import com.comp3050.hearthealthmonitor.entity.MyMessage;
import com.comp3050.hearthealthmonitor.utility.C_Database;

public class MonitoringService extends Service {

    private static final String CHANNEL_NAME = "monitoring";
    private static final String CHANNEL_ID = "monitoring_0";
    private static final int NOTIFICATION_ID = 0;
    private static final int MESSAGE_UPDATE = 0x0a;
    private static final int MESSAGE_PAUSE = 0x0b;
    private static final long SYNC_DELAY = 10000;
    private static final String QUERY_ORDER = C_Database.TIMESTAMP + " DESC";

    private DBHelper dbHelper;
    private SharedPreferences preferences;

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_UPDATE:
                    startService(new Intent(MonitoringService.this, DatabaseSyncService.class));
                    try {
                        SQLiteDatabase db = dbHelper.getReadableDatabase();
                        Cursor cursor = fetchLatestData(db);
                        if (cursor != null && cursor.moveToFirst()) {
                            checkHeartRate(cursor);
                            checkBloodPressure(cursor);
                            cursor.close();
                        }
                        db.close();
                    } catch (SQLiteException ignored) {}
                    sendEmptyMessageDelayed(MESSAGE_UPDATE, SYNC_DELAY);
                    break;
                case MESSAGE_PAUSE:
                    removeMessages(MESSAGE_UPDATE);
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        dbHelper = new DBHelper(this);
        preferences = getSharedPreferences("LastSyncTime", MODE_PRIVATE);
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler.sendEmptyMessage(MESSAGE_UPDATE);
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void checkHeartRate(Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int heartRate = cursor.getInt(cursor.getColumnIndex(C_Database.HEART_RATE));
            } while (cursor.moveToNext());
            cursor.moveToFirst();
        }
    }
    private void checkBloodPressure(Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int systolicBP = cursor.getInt(cursor.getColumnIndex(C_Database.SYSTOLIC_BLOOD_PRESSURE));
                int diastolicBP = cursor.getInt(cursor.getColumnIndex(C_Database.DIASTOLIC_BLOOD_PRESSURE));
            } while (cursor.moveToNext());
            cursor.moveToFirst();
        }
    }

    private Cursor fetchLatestData(SQLiteDatabase db) {

        Cursor cursor;
        long lastSyncTime = preferences.getLong("timestamp", -1);
        if (lastSyncTime == -1) {
            // first time syncing
            cursor = db.query(DBHelper.TABLE_NAME_DATA, null, null, null, null, null, QUERY_ORDER);
            if (!cursor.moveToFirst()) {
                // database is empty, no need to do any thing more
                cursor.close();
                cursor = null;
            } else {
                // database is not empty, return a cursor that contains all the data to be evaluated
                lastSyncTime = cursor.getLong(cursor.getColumnIndex(C_Database.TIMESTAMP));
                preferences.edit().putLong("timestamp", lastSyncTime).apply();
            }
        } else {
            // routine syncing
            lastSyncTime = preferences.getLong("timestamp", -1);
            String selection = C_Database.TIMESTAMP + " >?";
            String[] selectionArgs = new String[] { String.valueOf(lastSyncTime)};
            cursor = db.query(DBHelper.TABLE_NAME_DATA, null, selection, selectionArgs, null, null, QUERY_ORDER);
            if (cursor.moveToFirst()) {
                lastSyncTime = cursor.getLong(cursor.getColumnIndex(C_Database.TIMESTAMP));
                preferences.edit().putLong("timestamp", lastSyncTime).apply();
            } else {
                cursor.close();
                cursor = null;
            }
        }
        return cursor;
    }

    private void notifyAndArchive(MyMessage myMessage) {

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.favicon_notify)
                        .setContentTitle(myMessage.getTitle())
                        .setContentText(myMessage.getSummary())
                        .setContentIntent(PendingIntent.getActivity(this, 0,
                                new Intent(this, MainActivity.class),
                                PendingIntent.FLAG_UPDATE_CURRENT));
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1)
            notificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT));
        notificationManager.notify(NOTIFICATION_ID, builder.build());

        try {
            SQLiteDatabase database = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(C_Database.TIMESTAMP, myMessage.getTimestamp());
            values.put(C_Database.TITLE, myMessage.getTitle());
            values.put(C_Database.CONTENT, myMessage.getContent());
            values.put(C_Database.SUMMARY, myMessage.getSummary());
            values.put(C_Database.IMPORTANCE, myMessage.getType().getImportance());
            database.insert(DBHelper.TABLE_NAME_MSG, null, values);
            database.close();
        } catch (SQLiteException ignored) {}
    }
}