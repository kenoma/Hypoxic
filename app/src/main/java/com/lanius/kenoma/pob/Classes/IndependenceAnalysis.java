package com.lanius.kenoma.pob.Classes;

/**
 * Created by Kenoma on 19.08.2014.
 */
public class IndependenceAnalysis
{

    public static double getSynchronizationRate(int[] RR, int[] D, int[] t)
    {
        if(RR==null||RR.length<10)
            return 0.0;

        int[][] map = Mapping.regulation_ts(D, RR, t);

        int[][] hr = delay_vectors(map[0], 6, 1);
        int[][] br = delay_vectors(map[1], 6, 1);

        byte[][] rp_hr = get_rp(hr);
        byte[][] rp_br = get_rp(br);

        double s = S(hr, br, rp_hr, rp_br);
        return s;
    }

    static private double S(int[][] X, int[][] Y, byte[][] x_pack, byte[][] y_pack)
    {

        double RX = 0;
        double RXY = 0;

        for (int i = 0; i < X.length; i++)
            for (int j = 0; j < X.length; j++)
                if (x_pack[i][j] == 1 && i != j)
                {
                    RX++;
                    if (y_pack[i][j] == 1)
                        RXY++;
                }

        if (RX != 0)
            return RXY / RX;
        else
            return 0.0;
    }

    static private int[][] delay_vectors(int[] x, int delay, int tau)
    {
        int[][] retval = new int[x.length - tau * delay][];

        for (int t = 0; t < x.length - tau * delay; t += 1)
        {
            retval[t] = new int[delay];
            for (int i = 0; i < delay; i++)
                retval[t][i] = x[t + tau * i];
        }

        return retval;
    }

    static private byte[][] get_rp(int[][] X)
    {
        byte[][] retval = new byte[X.length][];

        int[][] new_line = new int[X.length][];
        double aver = 0.0;
        for (int i = 0; i < X.length; i++)
        {

            new_line[i] = new int[X.length];
            retval[i] = new byte[X.length];

            for (int j = 0; j < X.length; j++)
            {
                int d_x = 0;
                for (int z = 0; z < X[i].length; z++)
                {
                    int tmp = (X[i][z] - X[j][z]);
                    d_x += tmp * tmp;
                }
                new_line[i][j] = d_x;
                aver += d_x;
            }
        }
        aver /= X.length * X.length;

        for (int i = 1; i < X.length - 1; i++)
            for (int j = 1; j < X.length - 1; j++)
                if (new_line[i][j] < aver &&
                        (((new_line[i][j - 1] > new_line[i][j] && new_line[i][j] < new_line[i][j + 1])) ||
                                ((new_line[i - 1][j] > new_line[i][j] && new_line[i][j] < new_line[i + 1][j]))))
                {
                    //
                    retval[i][j] = 1;
                }

        return retval;
    }


}
