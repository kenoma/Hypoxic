package com.lanius.kenoma.pob.Classes;

public class fuzzyNode
{
    private double[] terms;

    public fuzzyNode(double[] terms)
    {
        this.terms = terms;
    }

    public static double fzFuncTriangle(double x, double a, double b, double c)
    {
        if (x >= a && x <= b)
            return 1.0 - (b - x) / (b - a);
        if (x >= b && x <= c)
            return 1.0 - (x - b) / (c - b);
        return 0.0;
    }

    public static double fzFuncOpenLeft(double x, double a, double b)
    {
        if (Double.isInfinite(x))
            return 0.0;
        if (x <= a)
            return 1.0;
        if (x >= a && x <= b)
            return 1.0 - (x - a) / (b - a);
        return 0.0;
    }

    public static double fzFuncOpenRight(double x, double a, double b)
    {
        if (Double.isInfinite(x))
            return 0.0;
        if (x >= b)
            return 1.0;
        if (x >= a && x <= b)
            return 1.0 - (b - x) / (b - a);
        return 0.0;
    }

    public double[] inference(double x)
    {
        if (terms.length < 2)
            return null;

        double[] retval = new double[terms.length];

        retval[0] = fzFuncOpenLeft(x, terms[0], terms[1]);
        retval[terms.length - 1] = fzFuncOpenRight(x, terms[terms.length - 2], terms[terms.length - 1]);
        for (int i = 1; i < terms.length - 1; i++)
            retval[i] = fzFuncTriangle(x, terms[i - 1], terms[i], terms[i + 1]);

        return retval;
    }
}
