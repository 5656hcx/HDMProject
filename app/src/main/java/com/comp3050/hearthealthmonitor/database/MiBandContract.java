package com.comp3050.hearthealthmonitor.database;

import android.net.Uri;

public final class MiBandContract {
    public static final String AUTHORITY = "com.getmiband.android.provider";
    public static final Uri URI = Uri.parse("content://" + AUTHORITY + "/miBand");
    public static final String TIMESTAMP = "TIMESTAMP";
    public static final String DEVICE_ID = "DEVICE_ID";
    public static final String USER_ID = "USER_ID";
    public static final String RAW_INTENSITY = "RAW_INTENSITY";
    public static final String RAW_KIND = "RAW_KIND";
    public static final String STEPS = "STEPS";
    public static final String HEART_RATE = "HEART_RATE";
}