package com.lanius.kenoma.pob.Classes;

import android.util.Log;

import com.lanius.kenoma.pob.hr_monitor.HxMData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExerciseLog
{
    public int exerciseID = 0;
    public String exercisName = "";
    public Date Started = new Date();

    public Date Finished = new Date();
    public double Weight = -1;
    // Exercise tags
    public String Tags = "";
    public List<Integer> RR = new ArrayList<Integer>();
    public List<Integer> FilteredRR = new ArrayList<Integer>();
    public int[] phaseDurations = new int[0];
    public int[] controlValue = new int[0];

    public int[] phaseTypes = new int[0];
    private HxMData last_data;
    private double charge = 0;
    private double aRR = 0;
    private double alpha = 0.8;

    public static int[] convertIntegers(List<Integer> integers)
    {
        int[] ret = new int[integers.size()];
        for (int i = 0; i < ret.length; i++)
            ret[i] = integers.get(i);

        return ret;
    }

    public static int abs(int i)
    {
        if (i < 0)
            return -i;
        else
            return i;
    }

    public void Update(HxMData data)
    {
        if (last_data != null)
        {
            charge = 0.99 * charge + 0.01 * data.BatteryCharge;
            int diff = HxMData.ToUnsignedInt((byte) ((byte) data.HeartBeatNum - (byte) last_data.HeartBeatNum));
            for (int i = 0; i < diff; i++)
            {
                int _RR = data.RR(diff - i);
                if (_RR != 0)
                    RR.add(_RR);

                if (RR.size() > 3)
                {
                    int pos = RR.size() - 3;
                    // /catch artifact RR-beat
                    if (abs(RR.get(pos) + RR.get(pos + 2) - RR.get(pos + 1)) < 100)
                    {
                        int delta = RR.get(pos) - RR.get(pos + 2);
                        int e_1 = RR.get(pos + 1) / 2;
                        int e_2 = e_1;
                        e_1 -= delta / 4;
                        e_2 += delta / 4;
                        if (abs(e_2 - RR.get(pos)) < 200)
                        {
                            FilteredRR.add(e_1);
                            FilteredRR.add(e_2);
                            aRR = alpha * aRR + (1.0 - alpha) * e_1;
                            aRR = alpha * aRR + (1.0 - alpha) * e_2;
                        }
                        Log.d("RR", "Update(): twice RR interval detected: [ " + RR.get(pos) + ", " + RR.get(pos + 1) + ", " + RR.get(pos + 2) + "]");
                    } else
                    {
                        if (abs(RR.get(pos) - RR.get(pos + 1)) < 250)
                            FilteredRR.add(RR.get(pos + 1));
                        aRR = alpha * aRR + (1.0 - alpha) * RR.get(pos + 1);
                    }
                }
            }
        }
        if (last_data == null && data != null)
        {
            charge = data.BatteryCharge;
            aRR = 60000 / data.HeartRate;
        }

        last_data = data;
    }

    public void Restart()
    {
        RR.clear();
        FilteredRR.clear();
        last_data = null;
    }

    public double getARR()
    {
        return aRR;
    }

    public int getBCharge()
    {
        if (last_data != null)
            return (int) charge;
        else
            return 0;
    }

    public int getCurrentHR()
    {
        if (last_data != null)
            return last_data.HeartRate;
        else
            return 0;
    }

    public int getCurrentRR()
    {
        if (RR.size() != 0)
            return RR.get(RR.size() - 1);
        else
            return 0;
    }

    public String Serialize()
    {
        JSONObject json = new JSONObject();
        JSONArray RR = new JSONArray();
        JSONArray phaseDurations = new JSONArray();
        JSONArray phaseTypes = new JSONArray();
        JSONArray controlValue = new JSONArray();

        JSONArray Tags = new JSONArray();

        for (int i = 0; i < this.RR.size(); i++)
            RR.put(this.RR.get(i));
        for (int i = 0; i < this.phaseDurations.length; i++)
            phaseDurations.put(this.phaseDurations[i]);
        for (int i = 0; i < this.phaseTypes.length; i++)
            phaseTypes.put(this.phaseTypes[i]);
        if (this.controlValue != null)
            for (int i = 0; i < this.controlValue.length; i++)
                controlValue.put(this.controlValue[i]);

        if (this.Tags != null)
            for (String tag : this.Tags.split(";"))
                if (tag.contains("Weight"))
                {
                    String w = tag.split(":")[1];
                    Weight = Double.parseDouble(w.replace(",", "."));
                } else
                    Tags.put(tag);

        try
        {
            json.put("id", this.exerciseID);

            json.put("exercise_name", this.exercisName);
            json.put("Weight", Weight);
            json.put("RR", RR);
            json.put("phaseDurations", phaseDurations);
            json.put("phaseTypes", phaseTypes);
            json.put("controlValue", controlValue);

            json.put("Tags", Tags);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ENGLISH);
            json.put("Started", sdf.format(this.Started));
            json.put("Finished", sdf.format(this.Finished));
        } catch (JSONException e)
        {
            Log.d("ExerciseLog", "Serialize: " + e.getMessage());
        }

        return json.toString();
    }

    public void Deserialize(String content)
    {
        try
        {
            JSONObject reader = new JSONObject(content);
            exerciseID = reader.getInt("id");
            exercisName = reader.optString("exercise_name");
            Weight = reader.getDouble("Weight");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ENGLISH);
            Started = sdf.parse(reader.getString("Started"));
            Finished = sdf.parse(reader.getString("Finished"));

            JSONArray arr;
            if (reader.has("RR"))
            {
                arr = reader.getJSONArray("RR");
                RR.clear();
                for (int i = 0; i < arr.length(); i++)
                    RR.add(Integer.parseInt(arr.get(i).toString()));
            }

            if (reader.has("phaseDurations"))
            {
                arr = reader.getJSONArray("phaseDurations");
                phaseDurations = new int[arr.length()];
                for (int i = 0; i < arr.length(); i++)
                    phaseDurations[i] = Integer.parseInt(arr.get(i).toString());
            }

            if (reader.has("phaseTypes"))
            {
                arr = reader.getJSONArray("phaseTypes");
                phaseTypes = new int[arr.length()];
                for (int i = 0; i < arr.length(); i++)
                    phaseTypes[i] = Integer.parseInt(arr.get(i).toString());
            }

            if (reader.has("controlValue"))
            {
                arr = reader.getJSONArray("controlValue");
                controlValue = new int[arr.length()];
                for (int i = 0; i < arr.length(); i++)
                    controlValue[i] = Integer.parseInt(arr.get(i).toString());
            }

            arr = reader.getJSONArray("Tags");
            Tags = "";
            for (int i = 0; i < arr.length(); i++)
                if (arr.get(i).toString().length() != 0)
                    Tags = arr.get(i).toString() + ";";


        } catch (Exception e)
        {

        }
    }

    public long getsumRR()
    {
        long retval = 0;
        for (int i = 0; i < RR.size(); i++)
            retval += RR.get(i);

        return retval;
    }

}
