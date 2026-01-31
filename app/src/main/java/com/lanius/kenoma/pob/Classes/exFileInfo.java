package com.lanius.kenoma.pob.Classes;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class exFileInfo
{
    public String fName = "";
    public String Name = "";
    public boolean isHRPresents = false;
    public Date Started = new Date();
    public Date Finished = new Date();

    public exFileInfo(String fname)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.ENGLISH);
        String[] parts = fname.split("_");
        if (parts.length == 4)
        {
            Name = parts[0];
            isHRPresents = Integer.parseInt(parts[1]) != 0;
            try
            {
                Started = sdf.parse(parts[2]);
                Finished = sdf.parse(parts[3]);
            } catch (ParseException e)
            {

            }
        }
        fName = fname;
    }

}