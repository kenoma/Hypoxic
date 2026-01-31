package com.lanius.kenoma.pob.sql;

import android.content.Context;
import android.provider.BaseColumns;

import com.lanius.kenoma.pob.Classes.StatVar;

import java.util.ArrayList;

public class StoredStatsDB
{
    public static String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + StoredStat.TABLE_NAME;
    private static String TEXT_TYPE = " TEXT";
    private static String DATETIME_TYPE = " INTEGER";
    private static String DOUBLE_TYPE = " REAL";
    private static String INT_TYPE = " INTEGER";
    private static String COMMA_SEP = ",";
    public String SQL_CREATE_ENTRIES =
            "CREATE TABLE IF NOT EXISTS " + StoredStat.TABLE_NAME + " (" +
                    StoredStat._ID + " INTEGER PRIMARY KEY," +
                    StoredStat.COLUMN_NAME_FILENAME + TEXT_TYPE + COMMA_SEP +
                    StoredStat.COLUMN_NAME_EXERCISE_NAME + TEXT_TYPE + COMMA_SEP +
                    StoredStat.COLUMN_NAME_EXERCISE_STARTED + DATETIME_TYPE + COMMA_SEP +
                    StoredStat.COLUMN_NAME_EXERCISE_FINISHED + DATETIME_TYPE + COMMA_SEP +
                    StoredStat.COLUMN_NAME_TAGS + TEXT_TYPE + COMMA_SEP;


    public StoredStatsDB(Context context)
    {
        ArrayList<StatVar> vars = StatVar.load(context.getResources());
        for (StatVar var : vars)
            SQL_CREATE_ENTRIES += var.db_alias + DOUBLE_TYPE + COMMA_SEP;
        SQL_CREATE_ENTRIES = SQL_CREATE_ENTRIES.substring(0, SQL_CREATE_ENTRIES.length() - 1);
        SQL_CREATE_ENTRIES += " )";
    }

    /* Inner class that defines the table contents */
    public static abstract class StoredStat implements BaseColumns
    {
        public static final String TABLE_NAME = "stats";

        public static final String COLUMN_NAME_FILENAME = "filename";
        public static final String COLUMN_NAME_EXERCISE_NAME = "exName";
        public static final String COLUMN_NAME_EXERCISE_STARTED = "started";
        public static final String COLUMN_NAME_EXERCISE_FINISHED = "finished";

        public static final String COLUMN_NAME_TAGS = "tags";


    }
}
