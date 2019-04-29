package com.comp3050.hearthealthmonitor.utility;

public class C_Parameter {

    private C_Parameter() {}

    public static final int HR_UPPER_BOUND = 220;
    public static final int HR_LOWER_BOUND = 20;
    public static final int HR_TOO_FAST = 100;
    public static final int HR_TOO_SLOW = 60;

    public static final int BP_UPPER_BOUND = 300;
    public static final int BP_LOWER_BOUND = 0;
    public static final int SBP_HYPERTENSIVE_CRISIS = 180;
    public static final int DBP_HYPERTENSIVE_CRISIS = 120;
    public static final int SBP_HYPERTENSIVE_STAGE2 = 140;
    public static final int DBP_HYPERTENSIVE_STAGE2 = 90;
    public static final int SBP_HYPERTENSIVE_STAGE1 = 130;
    public static final int DBP_HYPERTENSIVE_STAGE1 = 80;
}
