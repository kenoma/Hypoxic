package com.lanius.kenoma.pob.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Pair;

import com.lanius.kenoma.pob.Classes.ExerciseLog;
import com.lanius.kenoma.pob.Classes.StatVar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class StoredStatsDbHelper extends SQLiteOpenHelper
{
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 21;
    public static final String DATABASE_NAME = "StoredStats.db";
    private Context context;

    public StoredStatsDbHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    public void onCreate(SQLiteDatabase db)
    {
        StoredStatsDB stdb = new StoredStatsDB(context);
        db.execSQL(stdb.SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        db.execSQL(StoredStatsDB.SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void storeData(String fileName, ExerciseLog log, HashMap<String, Double> dic)
    {
        // Gets the data repository in write mode
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(StoredStatsDB.StoredStat.COLUMN_NAME_FILENAME, fileName);
        values.put(StoredStatsDB.StoredStat.COLUMN_NAME_EXERCISE_NAME, log.exercisName);
        values.put(StoredStatsDB.StoredStat.COLUMN_NAME_EXERCISE_STARTED, log.Started.getTime());
        values.put(StoredStatsDB.StoredStat.COLUMN_NAME_EXERCISE_FINISHED, log.Finished.getTime());
        values.put(StoredStatsDB.StoredStat.COLUMN_NAME_TAGS, log.Tags);

        for (String key : dic.keySet())
            values.put(key, dic.get(key));

        db.insert(
                StoredStatsDB.StoredStat.TABLE_NAME,
                null,
                values);
        db.close();
    }

    public boolean isAnyRecords()
    {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("select * from " + StoredStatsDB.StoredStat.TABLE_NAME + ";", null);
        boolean retval = false;
        if (c.getCount() > 0)
            retval = true;
        c.close();
        db.close();
        return retval;
    }

    public boolean deleteRecord(String name)
    {
        SQLiteDatabase db = getWritableDatabase();
        boolean retval = db.delete(StoredStatsDB.StoredStat.TABLE_NAME, StoredStatsDB.StoredStat.COLUMN_NAME_FILENAME + "='" + name + "'", null) > 0;
        db.close();
        return retval;
    }

    public HashMap<String, Double> getStatsByFileName(String fileName)
    {
        SQLiteDatabase db = getReadableDatabase();
        HashMap<String, Double> map = new HashMap<>();
        Cursor c = db.rawQuery(String.format("select * from %s where filename='%s'", StoredStatsDB.StoredStat.TABLE_NAME, fileName), null);

        if (c.moveToFirst())
        {
            do
            {
                ArrayList<StatVar> vars = StatVar.load(context.getResources());
                for (StatVar var : vars)
                {
                    int col_num = c.getColumnIndex(var.db_alias);
                    if (col_num != -1)
                    {
                        double value = c.getDouble(col_num);
                        if (value != Double.MIN_VALUE)
                            map.put(var.db_alias, value);
                    }
                }
            }
            while (c.moveToNext());
            c.close();
            db.close();
            return map;
        } else
        {
            c.close();
            db.close();
            return null;
        }

    }

    public double getAverage(String tags, String var, String exName)
    {
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.rawQuery(String.format("select avg(%s) as aver from %s where tags = '%s' and exName='%s' and %s!=%s",
                var,
                StoredStatsDB.StoredStat.TABLE_NAME,
                tags,
                exName,
                var,
                Double.MIN_VALUE), null);

        double retval = 0.0;
        if (c.moveToFirst())
            retval = c.getDouble(c.getColumnIndexOrThrow("aver"));

        c.close();
        db.close();
        return retval;
    }

    public String[] getTags(String exName, boolean split)
    {
        SQLiteDatabase db = getReadableDatabase();
        HashSet<String> retval = new HashSet<>();

        Cursor c = db.rawQuery(String.format("select tags from %s where exName='%s'", StoredStatsDB.StoredStat.TABLE_NAME, exName), null);
        if (c.moveToFirst())
        {
            do
            {
                String tmp = c.getString(c.getColumnIndexOrThrow(StoredStatsDB.StoredStat.COLUMN_NAME_TAGS));
                if (!split)
                    retval.add(tmp);
                else
                    for (String gut : tmp.split(";"))
                        retval.add(gut);

            }
            while (c.moveToNext());
        }
        c.close();
        db.close();
        return retval.toArray(new String[0]);
    }

    public ArrayList<Double> getData(String exName, String var)
    {
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.rawQuery(String.format(
                "select %s from %s where exName='%s' and %s!=%s",
                var,
                StoredStatsDB.StoredStat.TABLE_NAME,
                exName,
                var, Double.MIN_VALUE
        ), null);

        ArrayList<Double> list = new ArrayList<>();
        if (c.moveToFirst())
        {
            do
            {
                list.add(c.getDouble(c.getColumnIndexOrThrow(var)));
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    public Pair<Double, Double> getMinMax(String exName, String var, String tag)
    {
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.rawQuery(String.format(
                tag.equals("") ?
                        "select max(%s) as vMax, min(%s) as vMin from %s where exName='%s' and %s!=%s and tags = ''" :
                        "select max(%s) as vMax, min(%s) as vMin from %s where exName='%s' and %s!=%s and tags = '%s'",
                var,
                var,
                StoredStatsDB.StoredStat.TABLE_NAME,
                exName,
                var, Double.MIN_VALUE,
                tag
        ), null);

        Pair<Double, Double> pair = new Pair<>(0.0, 0.0);
        if (c.moveToFirst())
        {
            pair = new Pair<>(
                    c.getDouble(c.getColumnIndexOrThrow("vMin")),
                    c.getDouble(c.getColumnIndexOrThrow("vMax"))
            );
        }
        c.close();
        db.close();
        return pair;
    }

    public ArrayList<Pair<Long, Double>> getStats(String exName, String var, String includedTag)
    {
        SQLiteDatabase db = getReadableDatabase();

        ArrayList<Pair<Long, Double>> retval = new ArrayList<>();

        Cursor c = db.rawQuery(String.format(
                includedTag.equals("") ?
                        "select %s,%s from %s where exName='%s' and %s!=%s and tags = ''" :
                        "select %s,%s from %s where exName='%s' and %s!=%s and tags = '%s'",
                var,
                StoredStatsDB.StoredStat.COLUMN_NAME_EXERCISE_STARTED,
                StoredStatsDB.StoredStat.TABLE_NAME, exName,
                var, Double.MIN_VALUE,
                includedTag
        ), null);
        if (c.moveToFirst())
        {
            do
            {
                Pair<Long, Double> pair = new Pair<>(c.getLong(c.getColumnIndexOrThrow(StoredStatsDB.StoredStat.COLUMN_NAME_EXERCISE_STARTED)),
                        c.getDouble(c.getColumnIndexOrThrow(var)));
                retval.add(pair);
            }
            while (c.moveToNext());
        }
        c.close();
        db.close();
        return retval;
    }

}
