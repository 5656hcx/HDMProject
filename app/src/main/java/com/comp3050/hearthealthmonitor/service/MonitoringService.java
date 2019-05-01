package com.comp3050.hearthealthmonitor.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

/** An background service to perform following core tasks：
 *  1. maintain a loop for real-time heart health monitoring
 *  2. run the core functional service every 1 minute
 **/

public class MonitoringService extends Service {

    private static final int MESSAGE_SYNC = 0x00;
    private static final long SYNC_DELAY = 60000;

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_SYNC) {
                    startService(new Intent(MonitoringService.this, DatabaseSyncService.class));
                    sendEmptyMessageDelayed(MESSAGE_SYNC, SYNC_DELAY);
            }
        }
    };

    @Override
    public void onCreate() {
        handler.sendEmptyMessage(MESSAGE_SYNC);
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}