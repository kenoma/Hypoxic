package com.lanius.kenoma.pob.Classes;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kenoma on 26.08.2014.
 */
public final class Mapping
{

    final   static public int[][] mapRRtoBreathPhases(int[] p_duration, int[] RR)
    {

        int[][] res = new int[p_duration.length][];
        if (p_duration.length == 0)
            return res;

        double sumDur = p_duration[0];
        double sumRR = 0.0;
        int currentPhase = 0;

        ArrayList<Integer> tmp = new ArrayList<>();
        for (int aRR : RR)
        {
            sumRR += aRR;

            if (sumDur < sumRR)
            {
                int[] _tmp = new int[tmp.size()];
                for (int i = 0; i < tmp.size(); i++)
                    _tmp[i] = tmp.get(i);
                res[currentPhase] = _tmp;
                tmp.clear();
                while (sumDur < sumRR)
                {
                    currentPhase++;
                    if (currentPhase >= p_duration.length)
                        break;
                    sumDur += p_duration[currentPhase];
                }

            }
            tmp.add(aRR);
            if (currentPhase >= p_duration.length)
                break;
        }

        if (tmp.size() != 0 && currentPhase < p_duration.length)
        {
            int[] _tmp = new int[tmp.size()];
            for (int i = 0; i < tmp.size(); i++)
                _tmp[i] = tmp.get(i);
            res[currentPhase] = _tmp;
        }

        return res;
    }

    final  static public Integer[][] mapRRtoBreathCycles(int[] p_type, int[] p_duration, int[] RR, int[] pattern)
    {
        int p_index = 0;
        List<Integer[]> retval = new ArrayList<>();
        int[][] mRR = mapRRtoBreathPhases(p_duration, RR);
        ArrayList<Integer> tmp = new ArrayList<>();
        for (int i = 0; i < mRR.length; i++)
        {
            if (p_type[i] == pattern[p_index])
            {
                p_index++;
                if (mRR[i] != null)
                    for (int j = 0; j < mRR[i].length; j++)
                        tmp.add(mRR[i][j]);

                if (p_index == pattern.length)
                {
                    retval.add(tmp.toArray(new Integer[0]));
                    tmp.clear();
                    p_index = 0;
                }
            } else
            {
                p_index = 0;
                tmp.clear();
            }
        }
        Integer[][] _retval = new Integer[retval.size()][];
        for (int i = 0; i < retval.size(); i++)
            _retval[i] = retval.get(i);
        return _retval;
    }

    final  static public Integer[][] mapDurationstoBreathCycles(int[] p_type, int[] p_duration, int[] pattern)
    {
        int p_index = 0;
        List<Integer[]> retval = new ArrayList<>();
        ArrayList<Integer> tmp = new ArrayList<>();
        for (int i = 0; i < p_duration.length; i++)
        {
            if (p_type[i] == pattern[p_index])
            {
                p_index++;
                tmp.add(p_duration[i]);

                if (p_index == pattern.length)
                {
                    retval.add(tmp.toArray(new Integer[0]));
                    tmp.clear();
                    p_index = 0;
                }
            } else
            {
                p_index = 0;
                tmp.clear();
            }
        }
        Integer[][] _retval = new Integer[retval.size()][];
        for (int i = 0; i < retval.size(); i++)
            _retval[i] = retval.get(i);
        return _retval;
    }

    static public int[][] regulation_ts(int[] p_duration, int[] RR, int[] p_type)
    {
        ArrayList<ArrayList<Integer>> retval = new ArrayList<>();
        for (int i = 0; i < 4; i++)
            retval.add(new ArrayList<Integer>());

        int sumDur = p_duration[0];
        int lastDur = 0;
        int sumRR = 0;
        int currentPhase = 0;
        int plim = p_duration.length;
        for (int aRR : RR)
        {
            sumRR += aRR;

            if (sumDur < sumRR)
                while (sumDur < sumRR)
                {
                    currentPhase++;
                    if (currentPhase >= plim)
                        break;
                    lastDur = sumDur;
                    sumDur += p_duration[currentPhase];
                }

            if (currentPhase >= plim)
                break;

            if (p_type[currentPhase] == 5 || p_type[currentPhase] == 8)
            {
                retval.get(0).add(aRR);
                retval.get(1).add(-(sumRR - lastDur));
                retval.get(2).add(p_duration[currentPhase]);
                retval.get(3).add(p_type[currentPhase]);
            }

            if (p_type[currentPhase] == 6 || p_type[currentPhase] == 9)
            {
                retval.get(0).add(aRR);
                retval.get(1).add((sumRR - lastDur));
                retval.get(2).add(p_duration[currentPhase]);
                retval.get(3).add(p_type[currentPhase]);
            }
        }


        int[][] tmp = new int[4][];
        for (int i = 0; i < 4; i++)
        {
            ArrayList<Integer> arr = retval.get(i);
            int llim = arr.size();

            int[] line = new int[llim];
            for (int j = 0; j < llim; j++)
                line[j] = arr.get(j);
            tmp[i] = line;
        }
        return tmp;
    }


}
