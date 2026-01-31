package com.lanius.kenoma.pob.Classes;

import android.content.SharedPreferences;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.TreeMap;

public class Exercise
{
    public int id = 0;
    public String mode = "";
    public String name = "";
    public String shortDescription = "";
    public String detailedDescription = "";

    public int[] pattern = new int[0];
    public int Cycles = 50;
    public boolean enableTuning = true;///Разрешить пользователю задать собственную схему дыхания
    public TreeMap<Integer, int[]> events = new TreeMap<>();
    public TreeMap<Integer, Integer> targetRR = new TreeMap<>();

    public static String EncodeArray(int[] arr)
    {
        int max = 0;
        int min = Integer.MAX_VALUE;
        String res = "";
        for (int i = 0; i < arr.length; i++)
        {
            if (max < arr[i])
                max = arr[i];
            if (arr[i] != 0 && min > arr[i])
                min = arr[i];
        }

        if (max == min)
            for (int i = 0; i < arr.length; i++)
                if (arr[i] != 0)
                    res += 'a';
                else
                    res += 'x';
        else
            for (int i = 0; i < arr.length; i++)
                if (arr[i] != 0)
                {
                    if (arr[i] == max)
                        res += 'a';
                    else if (arr[i] == min)
                        res += 'c';
                    else
                        res += 'b';
                } else
                    res += 'x';

        return res;
    }

    static public ArrayList<Exercise> LoadExList(SharedPreferences settings, XmlPullParser xpp)
    {
        ArrayList<Exercise> exercises = new ArrayList<>();
        try
        {
            //SharedPreferences settings = getSharedPreferences(Constants.PROG_PREF + "_EX", 0);
            Exercise ex = null;

            while (xpp.getEventType() != XmlPullParser.END_DOCUMENT)
            {
                if (xpp.getEventType() == XmlPullParser.START_TAG)
                {
                    String node = xpp.getName();

                    if (node != null && node.equals("Exercise"))
                    {
                        Log.d("LoadXml", "LoadExList: Node: " + node);
                        ex = new Exercise();
                        int size = xpp.getAttributeCount();
                        for (int i = 0; i < size; i++)
                        {
                            String attrName = xpp.getAttributeName(i);
                            String attrValue = xpp.getAttributeValue(i);
                            if (attrName != null)
                                switch (attrName)
                                {
                                    case "id":
                                        ex.id = Integer.parseInt(attrValue);
                                        break;
                                    case "name":
                                        ex.name = attrValue;
                                        break;
                                    case "mode":
                                        ex.mode = attrValue;
                                        break;
                                    case "short":
                                        ex.shortDescription = attrValue;
                                        break;
                                    case "detailed":
                                        ex.detailedDescription = attrValue;
                                        break;
                                    case "enable_tuning":
                                        ex.enableTuning = Boolean.parseBoolean(attrValue);
                                        break;

                                    case "cycles":
                                        Integer tmp2 = settings.getInt("c_" + ex.id, -1);
                                        if (tmp2 != -1)
                                            attrValue = tmp2.toString();

                                        ex.Cycles = Integer.parseInt(attrValue);

                                        break;
                                    case "pattern":
                                        String[] arr2 = attrValue.split(",");
                                        ex.pattern = new int[arr2.length];
                                        for (int j = 0; j < arr2.length; j++)
                                            switch (arr2[j])
                                            {
                                                case "i":
                                                    ex.pattern[j] = Constants.EXERCISE_PHASE_INHALE;
                                                    break;
                                                case "e":
                                                    ex.pattern[j] = Constants.EXERCISE_PHASE_EXHALE;
                                                    break;
                                                case "p":
                                                    ex.pattern[j] = Constants.EXERCISE_PHASE_PAUSE;
                                                    break;
                                                case "h":
                                                    ex.pattern[j] = Constants.EXERCISE_PHASE_HOLD;
                                                    break;
                                            }
                                        break;
                                }
                        }

                        exercises.add(ex);

                    }

                    if (node != null && node.equals("event") && ex != null)
                    {
                        Log.d("LoadXml", "LoadExList: Event: " + node);
                        int cycleKey = 0;
                        int[] eventArr = null;
                        int size = xpp.getAttributeCount();
                        for (int i = 0; i < size; i++)
                        {
                            String attrName = xpp.getAttributeName(i);
                            String attrValue = xpp.getAttributeValue(i);
                            if (attrName != null)
                            {
                                switch (attrName)
                                {
                                    case "cycle":
                                        cycleKey = Integer.parseInt(attrValue);
                                        break;

                                    case "duration":
                                        if (cycleKey == 0 && ex.enableTuning)
                                        {
                                            String tmp = settings.getString("d_" + ex.id, "");
                                            if (!tmp.equals(""))
                                                attrValue = tmp;
                                        }

                                        String[] arr1 = attrValue.replace("[", "").replace("]", "").split(",");
                                        eventArr = new int[arr1.length];
                                        for (int j = 0; j < arr1.length; j++)
                                            eventArr[j] = Integer.parseInt(arr1[j]);
                                        break;
                                }
                            }
                        }
                        if (cycleKey == 0)
                            ex.events.put(-1, eventArr);
                        ex.events.put(cycleKey, eventArr);
                    }

                    if (node != null && node.equals("control_rr") && ex != null)
                    {
                        Log.d("LoadXml", "LoadExList: control_rr: " + node);
                        int cycleKey = 0;
                        int eventArr = 0;
                        int size = xpp.getAttributeCount();
                        for (int i = 0; i < size; i++)
                        {
                            String attrName = xpp.getAttributeName(i);
                            String attrValue = xpp.getAttributeValue(i);
                            if (attrName != null)
                            {
                                switch (attrName)
                                {
                                    case "cycle":
                                        cycleKey = Integer.parseInt(attrValue);
                                        break;

                                    case "RR":
                                        eventArr = Integer.parseInt(attrValue);
                                        break;
                                }
                            }
                        }

                        ex.targetRR.put(cycleKey, eventArr);
                    }
                }

                xpp.next();
            }


        } catch (Throwable t)
        {

            Log.d("LoadXml", "LoadExList: Request failed: " + t.toString());
        }
        return exercises;
    }


    public String getName()
    {
        return name;
    }

    private void hypoxicScheme()
    {
        int[] _hold = events.get(-1);
        int hold = 10000;
        if (_hold != null)
            hold = _hold[3];
        if (id < 100)
        {
            events.put(0, new int[]{3000, 0, 3000, 0});
            events.put(3, new int[]{3000, 3000, 3000, 3000});
            events.put(5, new int[]{5000, hold / 2, 500, hold / 2 - 500});
            for (int i = 0; i < 4; i++)
            {
                events.put(6 + i * 13, new int[]{1000, 0, 1000, 0});
                events.put(6 + i * 13 + 1, new int[]{3000, 0, 3000, 0});
                events.put(6 + i * 13 + 10, new int[]{3000, 3000, 3000, 3000});
                if (i % 2 == 0)
                    events.put(6 + i * 13 + 12, new int[]{3000, hold, 1000, 0});
                else
                    events.put(6 + i * 13 + 12, new int[]{3000, 3000, 3000, hold});
            }
            events.put(58, new int[]{3000, 0, 3000, 0});
            Cycles = 59;
        } else if (id == 100)//vishama vritti
        {
            int part = hold / 4;

            events.put(0, new int[]{part, 0, part, 0});
            events.put(3, new int[]{part, 0, 2 * part, 0});
            events.put(5, new int[]{part, part, 2 * part, 0});
            events.put(7, new int[]{part, 2 * part, 2 * part, 0});
            events.put(9, new int[]{part, 3 * part, 2 * part, 0});
            events.put(11, new int[]{part, 4 * part, 2 * part, 0});
            events.put(13, new int[]{part, 4 * part, 2 * part, part});
            Cycles = 20;
        } else if (id == 101)//Sama-vritti
        {
            int part = hold / 10;

            events.put(0, new int[]{10 * part, 0 * part, 10 * part, 0 * part});
            events.put(2, new int[]{8 * part, 2 * part, 8 * part, 2 * part});
            events.put(4, new int[]{7 * part, 3 * part, 7 * part, 3 * part});
            events.put(6, new int[]{5 * part, 5 * part, 5 * part, 5 * part});
            Cycles = 15;
        } else if (id == 102)//Antara kumbhaka
        {
            int part = hold / 4;
            events.put(0, new int[]{part, 0, part, 0});
            events.put(2, new int[]{part, 0, 2 * part, 0});
            events.put(4, new int[]{part, part, 2 * part, 0});
            events.put(6, new int[]{part, 2 * part, 2 * part, 0});
            events.put(8, new int[]{part, 3 * part, 2 * part, 0});
            events.put(10, new int[]{part, 4 * part, 2 * part, 0});
            Cycles = 20;
        } else if (id == 103)//Bahir kumbhaka
        {
            int part = hold / 4;
            events.put(0, new int[]{part, 0, part, 0});
            events.put(2, new int[]{2 * part, 0, 2 * part, part});
            events.put(4, new int[]{3 * part, 0, 3 * part, 2 * part});
            events.put(6, new int[]{4 * part, 0, 4 * part, 3 * part});
            events.put(8, new int[]{4 * part, 0, 4 * part, 4 * part});
            Cycles = 20;
        }
    }

    public int[] getDurationScheme()
    {
        int[] tmp = events.get(0);
        int[] d = new int[Cycles * tmp.length + 4];
        if (mode.equals("hypoxic"))
            hypoxicScheme();

        d[0] = 2000;
        d[1] = 1000;
        d[2] = 800;
        d[3] = 800;

        for (int pi = 0; pi < Cycles; pi++)
        {
            if (events.containsKey(pi))
                tmp = events.get(pi);
            for (int ph = 0; ph < tmp.length; ph++)
            {
                d[pi * tmp.length + ph + 4] = tmp[ph];
//                float ratio = (count * 100.0f) / max;
//                ratio = ratio > 100.0f ? 1.0f : (ratio / 100.f);
//                if(mode.equals("skip"))
//                    tmp[ph] = events.get(next_key)[ph];
//                if(mode.equals("interpolate"))
//                    tmp[ph] = (int) ((1.0f - ratio) * tmp[ph] + ratio * events.get(next_key)[ph]);
//                d[pi * durations.length + ph + 4] = tmp[ph];

            }
        }

        return d;
    }

    public int[] getTypeScheme()
    {
        int[] t = new int[Cycles * pattern.length + 4];

        t[0] = Constants.EXERCISE_PHASE_PREPARATION_1;
        t[1] = Constants.EXERCISE_PHASE_PREPARATION_2;
        t[2] = Constants.EXERCISE_PHASE_PREPARATION_3;
        t[3] = Constants.EXERCISE_PHASE_PREPARATION_4;

        for (int i = 0; i < Cycles; i++)
            for (int ph = 0; ph < pattern.length; ph++)
                t[i * pattern.length + ph + 4] = pattern[ph];

        return t;
    }

    public int[] getControlScheme()
    {
        int[] control = new int[Cycles * pattern.length + 4];
        if (targetRR.size() != 0)
        {
            control[0] = 0;
            control[1] = 0;
            control[2] = 0;
            control[3] = 0;
            int tmp = 0;
            for (int pi = 0; pi < Cycles; pi++)
            {
                if (targetRR.containsKey(pi))
                    tmp = targetRR.get(pi);

                for (int ph = 0; ph < pattern.length; ph++)
                    control[pi * pattern.length + ph + 4] = tmp;
            }
        }
        return control;
    }

}
