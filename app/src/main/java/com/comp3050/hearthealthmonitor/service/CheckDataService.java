package com.comp3050.hearthealthmonitor.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;

import com.comp3050.hearthealthmonitor.R;
import com.comp3050.hearthealthmonitor.activity.MessageActivity;
import com.comp3050.hearthealthmonitor.database.DBHelper;
import com.comp3050.hearthealthmonitor.entity.MyMessage;
import com.comp3050.hearthealthmonitor.utility.C_Database;
import com.comp3050.hearthealthmonitor.utility.C_Parameter;

/** An intent service to perform following core tasks：
 *  1. analyse heart rate every 1 minute
 *  2. analyse blood pressure every 1 hour
 *  3. generate a decision message and inform the client
 **/

public class CheckDataService extends IntentService {

    private static final String QUERY_ORDER = C_Database.TIMESTAMP + " DESC";
    private static final String CHANNEL_NAME = "monitoring";
    private static final String CHANNEL_ID = "com.comp3050.hearthealthmonitor.monitoring";
    private static final long BP_SYNC_PERIOD_MINUTES = 60;
    private SharedPreferences preferences;

    public CheckDataService() {
        super("CheckDataService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        preferences = getSharedPreferences("LastSyncTime", MODE_PRIVATE);
        if (intent != null) {
            DBHelper dbHelper = new DBHelper(this);
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor = fetchLatestData(db);
                if (cursor != null && cursor.moveToFirst()) {
                    checkHeartRate(cursor);
                    checkBloodPressure(cursor);
                    cursor.close();
                }
                db.close();
            } catch (SQLiteDatabaseLockedException ignored) {} catch (SQLiteException ignored) {}
        }
    }

    private void checkHeartRate(Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int heartRate = cursor.getInt(cursor.getColumnIndex(C_Database.HEART_RATE));
                if (heartRate >= C_Parameter.HR_LOWER_BOUND && heartRate <= C_Parameter.HR_UPPER_BOUND) {
                    if (heartRate > C_Parameter.HR_TOO_FAST) {
                        archiveAndNotify(new MyMessage(MyMessage.MessageType.ADVISE,
                                getString(R.string.message_hr_racing_title),
                                getEffectAndSuggestionHeartRateFast(),
                                getString(R.string.message_hr_racing_detected_summary),
                                cursor.getLong(cursor.getColumnIndex(C_Database.TIMESTAMP)) * 1000));
                    } else if (heartRate < C_Parameter.HR_TOO_SLOW) {
                        archiveAndNotify(new MyMessage(MyMessage.MessageType.ADVISE,
                                getString(R.string.message_hr_slow_title),
                                getEffectAndSuggestionHeartRateSlow(),
                                getString(R.string.message_hr_slow_detected_summary),
                                cursor.getLong(cursor.getColumnIndex(C_Database.TIMESTAMP)) * 1000));
                    }
                }

            } while (cursor.moveToNext());
            cursor.moveToFirst();
        }
    }

    private void checkBloodPressure(Cursor cursor) {

        if (cursor != null && cursor.moveToLast()) {

            long breakTime;
            if ((breakTime = preferences.getLong("timestamp_bp", -1)) == -1) {
                breakTime = cursor.getLong(cursor.getColumnIndex(C_Database.TIMESTAMP));
                preferences.edit().putLong("timestamp_bp", breakTime).apply();
            }

            do {
                long recordTime = cursor.getLong(cursor.getColumnIndex(C_Database.TIMESTAMP));

                if ((recordTime - breakTime) < BP_SYNC_PERIOD_MINUTES * 60) {
                    continue;
                }

                breakTime = recordTime;
                preferences.edit().putLong("timestamp_bp", breakTime).apply();

                boolean additionalDetection;
                int systolicBP = cursor.getInt(cursor.getColumnIndex(C_Database.SYSTOLIC_BLOOD_PRESSURE));
                int diastolicBP = cursor.getInt(cursor.getColumnIndex(C_Database.DIASTOLIC_BLOOD_PRESSURE));
                if (systolicBP > C_Parameter.BP_UPPER_BOUND || diastolicBP < C_Parameter.BP_LOWER_BOUND) continue;
                else if (systolicBP >= C_Parameter.SBP_HYPERTENSION || diastolicBP >= C_Parameter.DBP_HYPERTENSION)
                {
                    additionalDetection = true;
                    archiveAndNotify(new MyMessage(MyMessage.MessageType.EMERGENCY,
                            getString(R.string.message_hbp_detected_title),
                            getString(R.string.message_bp_ignore),
                            getString(R.string.message_hbp_detected_summary),
                            recordTime * 1000));
                }
                else if (systolicBP >= C_Parameter.SBP_PREHYPERTENSION || diastolicBP >= C_Parameter.DBP_PREHYPERTENSION)
                {
                    additionalDetection = false;
                    archiveAndNotify(new MyMessage(MyMessage.MessageType.EMERGENCY,
                            getString(R.string.message_pre_hbp_detected_title),
                            getString(R.string.message_bp_ignore),
                            getString(R.string.message_pre_hbp_detected_summary),
                            cursor.getLong(cursor.getColumnIndex(C_Database.TIMESTAMP)) * 1000));
                }
                else continue;
                if (additionalDetection)
                {
                    archiveAndNotify(new MyMessage(MyMessage.MessageType.ADVISE,
                            getString(R.string.message_hbp_detected_title),
                            getEffectBloodPressure(),
                            getString(R.string.message_summary_effect_hbp),
                            cursor.getLong(cursor.getColumnIndex(C_Database.TIMESTAMP)) * 1000));
                }
                else
                {
                    archiveAndNotify(new MyMessage(MyMessage.MessageType.ADVISE,
                            getString(R.string.message_pre_hbp_detected_title),
                            getSuggestionBloodPressure(),
                            getString(R.string.message_summary_suggestion_hbp),
                            cursor.getLong(cursor.getColumnIndex(C_Database.TIMESTAMP)) * 1000));
                }
            } while (cursor.moveToPrevious());
            cursor.moveToFirst();
        }
    }

    private void archiveAndNotify(MyMessage message) {

        try {
            SQLiteDatabase database = new DBHelper(this).getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(C_Database.TIMESTAMP, message.getTimestamp());
            values.put(C_Database.TITLE, message.getTitle());
            values.put(C_Database.CONTENT, message.getContent());
            values.put(C_Database.SUMMARY, message.getSummary());
            values.put(C_Database.IMPORTANCE, message.getType().getImportance());
            final long rowId = database.insert(DBHelper.TABLE_NAME_MSG, null, values);
            database.close();

            Intent intent = new Intent(this, MessageActivity.class);
            intent.putExtra("data", rowId);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addNextIntentWithParentStack(intent);
            PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.favicon_notify)
                    .setContentTitle(message.getTitle())
                    .setContentText(message.getSummary())
                    .setContentIntent(pendingIntent)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message.getContent()))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build();
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                        CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT));
            }
            int notificationId = (int) System.currentTimeMillis();
            notificationManagerCompat.notify(notificationId, notification);
        } catch (SQLiteDatabaseLockedException ignored) {} catch (SQLiteException ignored) {}
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

    private String getSuggestionBloodPressure() {
        String[] suggestionList = new String[] {
                getString(R.string.message_bp_suggestion_1),
                getString(R.string.message_bp_suggestion_2),
                getString(R.string.message_bp_suggestion_3),
                getString(R.string.message_bp_suggestion_4),
                getString(R.string.message_bp_suggestion_5),
        };
        int which = (int) (Math.random() * (suggestionList.length));
        return suggestionList[which];
    }

    private String getEffectBloodPressure() {
        String[] effectList = new String[] {
                getString(R.string.message_bp_effect_1),
                getString(R.string.message_bp_effect_2),
                getString(R.string.message_bp_effect_3),
                getString(R.string.message_bp_effect_4),
                getString(R.string.message_bp_effect_5),
        };
        int which = (int) (Math.random() * (effectList.length));
        return effectList[which];
    }

    private String getEffectAndSuggestionHeartRateFast() {
        String[] effectList = new String[] {
                getString(R.string.message_racing_hr_effect_1),
                getString(R.string.message_racing_hr_effect_2),
                getString(R.string.message_racing_hr_effect_3),
        };
        String[] suggestionList = new String[] {
                getString(R.string.message_racing_hr_suggestion_1),
                getString(R.string.message_racing_hr_suggestion_2),
                getString(R.string.message_racing_hr_suggestion_3),
        };
        int which = (int) (Math.random() * (effectList.length));
        return effectList[which] + "\n" + suggestionList[which];
    }

    private String getEffectAndSuggestionHeartRateSlow() {
        return getString(R.string.message_slow_hr_effect_1) + '\n' + getString(R.string.message_slow_hr_suggestion_1);
    }
}
