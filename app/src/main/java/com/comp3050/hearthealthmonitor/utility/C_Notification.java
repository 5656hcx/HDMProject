package com.comp3050.hearthealthmonitor.utility;

public class C_Notification {

    private C_Notification() {}

    public static final int NumberOfSuggestion = 7;
    public static final String title = "WHAT'S WRONG WITH YOUR HEART";

    public static final String suggestion_0 = "HELLO MOM";
    public static final String suggestion_1 = "HELLO DAD";
    public static final String suggestion_2 = "HELLO YOU";
    public static final String suggestion_3 = "HELLO HER";
    public static final String suggestion_4 = "HELLO HIM";
    public static final String suggestion_5 = "HELLO ME";
    public static final String suggestion_6 = "HELLO WORLD";

    public static String randomText() {
        int selection = (int)(Math.random()*NumberOfSuggestion);
        switch (selection) {
            case 0:
                return suggestion_0;
            case 1:
                return suggestion_1;
            case 2:
                return suggestion_2;
            case 3:
                return suggestion_3;
            case 4:
                return suggestion_4;
            case 5:
                return suggestion_5;
            case 6:
                return suggestion_6;
        }
        return suggestion_6;
    }
}
