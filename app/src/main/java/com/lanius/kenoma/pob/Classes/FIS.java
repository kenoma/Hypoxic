package com.lanius.kenoma.pob.Classes;

import android.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FIS
{
    private fuzzyNode[] rules;
    private HashMap<String, double[]> base;

    public FIS(double[][] serializedRules, HashMap<String, double[]> base)
    {
        this.base = base;
        rules = new fuzzyNode[serializedRules.length];
        for (int i = 0; i < serializedRules.length; i++)
            rules[i] = new fuzzyNode(serializedRules[i]);
    }

    public double[] inference(double[] input, double[] defaultValue)
    {
        ArrayList<Pair<String, Double>> memb = fuzzificate(input);
        if (memb == null)
            return null;

        double[] retval = null;
        double norm = 0;

        for (Pair<String, Double> item : memb)
        {
            if (base.containsKey(item.first))
            {
                double[] val = base.get(item.first);
                norm += item.second;
                if (retval == null)
                    retval = new double[val.length];

                for (int i = 0; i < val.length; i++)
                    retval[i] += val[i] * item.second;
            }
        }

        if (norm == 0)
            return defaultValue;
        else
        {
            for (int i = 0; i < retval.length; i++)
                retval[i] /= norm;
            return retval;
        }
    }

    public void train(double[] input, double[] value)
    {
        ArrayList<Pair<String, Double>> memb = fuzzificate(input);
        if (memb == null)
            return;
        for (Pair<String, Double> item : memb)
        {
            if (base.containsKey(item.first))
            {
                double[] stored_value = base.get(item.first).clone();
                for (int i = 0; i < stored_value.length; i++)
                    stored_value[i] = item.second * value[i] + (1.0 - item.second) * stored_value[i];

                base.put(item.first, stored_value);
            } else
                base.put(item.first, value);
        }
    }

    public ArrayList<Pair<String, Double>> fuzzificate(double[] input)
    {
        if (input.length != rules.length)
            return null;

        ArrayList<Pair<String, Double>> membs = new ArrayList<Pair<String, Double>>();
        recursiveWalk(membs, 0, 1.0, "", input);
        return membs;
    }

    private void recursiveWalk(ArrayList<Pair<String, Double>> membs, int level, double p, String path, double[] inp)
    {
        if (level == rules.length)
        {
            if (p != 0)
                membs.add(new Pair<String, Double>(path, p));
            return;
        }

        double[] m = rules[level].inference(inp[level]);
        for (int i = 0; i < m.length; i++)
            if (m[i] > 0)
                recursiveWalk(membs, level + 1, p * m[i], path + "." + i, inp);
    }
}

