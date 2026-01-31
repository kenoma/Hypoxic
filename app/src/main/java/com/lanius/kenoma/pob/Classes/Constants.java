package com.lanius.kenoma.pob.Classes;

import java.util.UUID;

public class Constants
{
    public static final boolean D = true;

    public static final String PROG_PREF = "PREFS";

    public static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_TAGS_CHOOSEN = 2;
    public static final int REQUEST_EXERCISE = 3;
    public static final int REQUEST_RESULTS = 4;
    public static final int REQUEST_DESCRIPTION = 5;
    public static final int REQUEST_SETTINGS = 6;
    public static final UUID HXM_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    public static final int EXERCISE_PHASE_FINISH = -1;
    public static final int EXERCISE_PHASE_PREPARATION_1 = 1;
    public static final int EXERCISE_PHASE_PREPARATION_2 = 2;
    public static final int EXERCISE_PHASE_PREPARATION_3 = 3;
    public static final int EXERCISE_PHASE_PREPARATION_4 = 4;
    public static final int EXERCISE_PHASE_INHALE = 5;
    public static final int EXERCISE_PHASE_EXHALE = 6;
    public static final int EXERCISE_PHASE_PAUSE = 8;
    public static final int EXERCISE_PHASE_HOLD = 9;

    public static final String BROADCAST_HR = "broadcast-hr";

    public static final int REQUEST_SOUND_FILE_INHALE = 100;
    public static final int REQUEST_SOUND_FILE_PAUSE = 102;
    public static final int REQUEST_SOUND_FILE_EXHALE = 101;
    public static final int REQUEST_SOUND_FILE_HOLD = 103;
    public static int ALARM_ID = 1234;
}
