package com.lanius.kenoma.pob.Classes;

import android.content.res.Resources;
import android.util.Log;

import com.lanius.kenoma.pob.R;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;

public class StatVar
{
    public String db_alias;
    public String human_readable;
    public String description;
    public boolean isHeartRate;
    public String unit;

    public static ArrayList<StatVar> load(Resources res)
    {
        ArrayList<StatVar> arr = new ArrayList<>();
        arr.clear();

        try
        {
            XmlPullParser xpp = res.getXml(R.xml.statvars);

            while (xpp.getEventType() != XmlPullParser.END_DOCUMENT)
            {
                if (xpp.getEventType() == XmlPullParser.START_TAG)
                {
                    String node = xpp.getName();

                    if (node != null && node.equals("item"))
                    {
                        StatVar sv = new StatVar();
                        int size = xpp.getAttributeCount();
                        for (int i = 0; i < size; i++)
                        {
                            String attrName = xpp.getAttributeName(i);
                            String attrValue = xpp.getAttributeValue(i);
                            if (attrName != null)
                                switch (attrName)
                                {
                                    case "db_alias":
                                        sv.db_alias = attrValue;
                                        break;
                                    case "human_readable":
                                        sv.human_readable = attrValue;
                                        break;
                                    case "description":
                                        sv.description = attrValue;
                                        break;
                                    case "unit":
                                        sv.unit = attrValue;
                                        break;
                                    case "isHeartRate":
                                        sv.isHeartRate = Boolean.parseBoolean(attrValue);
                                        break;
                                }
                        }
                        arr.add(sv);
                    }
                }
                xpp.next();
            }
        } catch (Throwable t)
        {

            Log.d("LoadXml", "LoadExList: Request failed: " + t.toString());
        }
        return arr;
    }

    @Override
    public String toString()
    {
        return human_readable;
    }
}
