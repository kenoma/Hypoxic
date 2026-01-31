package com.lanius.kenoma.pob;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lanius.kenoma.pob.Classes.Constants;
import com.lanius.kenoma.pob.Classes.ExerciseLog;
import com.lanius.kenoma.pob.Classes.HRV;
import com.lanius.kenoma.pob.Classes.Mapping;
import com.lanius.kenoma.pob.Classes.StatVar;
import com.lanius.kenoma.pob.dialogs.InfoDialog;
import com.lanius.kenoma.pob.sql.StoredStatsDbHelper;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ResultActivity extends FragmentActivity
{
    private boolean isInit = false;
    //private View page_br_vs_hr;
    //private View page_breathing_phases;
    private View page_heart_rate;
    private View page_heart_variability;
    //private View page_breathing_synch;

    private SimplePagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = LayoutInflater.from(this);
        List<View> pages = new ArrayList<View>();

        page_heart_variability = inflater.inflate(R.layout.page_heart_rate_variability, null);
        pages.add(page_heart_variability);

        page_heart_rate = inflater.inflate(R.layout.page_chart, null);
        pages.add(page_heart_rate);

//        page_br_vs_hr = inflater.inflate(R.layout.page_chart, null);
//        pages.add(page_br_vs_hr);
//
//        page_breathing_phases = inflater.inflate(R.layout.page_chart, null);
//        pages.add(page_breathing_phases);
//
//        page_breathing_synch = inflater.inflate(R.layout.page_chart, null);
//        pages.add(page_breathing_synch);


        pagerAdapter = new SimplePagerAdapter(pages);
        ViewPager viewPager = new ViewPager(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(0);

        setContentView(viewPager);
    }

    // Initiating Menu XML file (menu.xml)
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_chart, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.db_screenshot:
                SendScreenShot();
                return true;
            case R.id.info_vars:
                getInfo();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (!isInit)
        {
            isInit = true;
            int[] RR = getIntent().getIntArrayExtra("RR");
            int[] Du = getIntent().getIntArrayExtra("d");
            int[] Tp = getIntent().getIntArrayExtra("t");
            int[] Cd = getIntent().getIntArrayExtra("control");
            String fname = getIntent().getStringExtra("filename");
            String tags = getIntent().getStringExtra("Tags");
            String exName = getIntent().getStringExtra("exName");
            HRV(RR, Cd, Du, Tp, fname, tags, exName);
            TextView view = (TextView) page_heart_variability.findViewById(R.id.Tags);
            view.setText(tags);


            if (RR != null && RR.length != 0)
            {
                drawHR(RR);
                //if(Cd.length>0)
                //    drawSynchrogramm(Cd);
                //else
                //{
                //pagerAdapter.pages.remove(page_breathing_synch);
                //pagerAdapter.notifyDataSetChanged();
                //}

            } else
            {
                //if (RR == null)
                //    pagerAdapter.pages.remove(page_br_vs_hr);
                pagerAdapter.pages.remove(page_heart_rate);
                //pagerAdapter.pages.remove(page_heart_variability);
                pagerAdapter.notifyDataSetChanged();
                //page_heart_rate.setVisibility(View.INVISIBLE);
            }

            //if (Du != null && Tp != null && Du.length == Tp.length)
            //{
            //drawBScheme(Du, Tp);
            //if (RR != null)
            //    drawBRvsHR(HRV.filterRR(RR), Du, Tp);
            //}


        }
    }

    private HashMap<Double, Double> getDistribution(int[][] split, int[] Tp, int[] Du, int type, double resolution)
    {
        HashMap<Double, Double> res = new HashMap<>();
        HashMap<Double, Double> count = new HashMap<>();
        for (int i = 0; i < split.length; i++)
            if (split[i] != null && Tp[i] == type)
            {
                Double db = Math.round(Du[i] / resolution) * resolution;
                for (int j = 0; j < split[i].length; j++)
                {
                    if (res.containsKey(db))
                    {
                        res.put(db, res.get(db) + 60000.0 / split[i][j]);
                        count.put(db, count.get(db) + 1);
                    } else
                    {
                        res.put(db, 60000.0 / split[i][j]);
                        count.put(db, 1.0);
                    }
                }
            }
        ArrayList<Double> list = new ArrayList<Double>(res.keySet());
        java.util.Collections.sort(list);
        HashMap<Double, Double> retval = new HashMap<>();
        for (Double db : list)
            retval.put(60000.0 / db, res.get(db) / count.get(db));

        return retval;
    }

    private void drawBRvsHR(int[] RR, int[] Du, int[] Tp)
    {
        int[][] split = Mapping.mapRRtoBreathPhases(Du, RR);

        XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
        XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();

        HashMap<Integer, XYSeries> mSeries = new HashMap<>();
        HashMap<Integer, Integer> mColors = new HashMap<>();

        Resources res = getResources();
        mColors.put(Constants.EXERCISE_PHASE_INHALE, res.getColor(R.color.inhale));
        mColors.put(Constants.EXERCISE_PHASE_PAUSE, res.getColor(R.color.pause));
        mColors.put(Constants.EXERCISE_PHASE_EXHALE, res.getColor(R.color.exhale));
        mColors.put(Constants.EXERCISE_PHASE_HOLD, res.getColor(R.color.hold));
        mSeries.put(Constants.EXERCISE_PHASE_INHALE, new XYSeries(getString(R.string.exercize_inhale)));
        mSeries.put(Constants.EXERCISE_PHASE_PAUSE, new XYSeries(getString(R.string.exercize_pause)));
        mSeries.put(Constants.EXERCISE_PHASE_EXHALE, new XYSeries(getString(R.string.exercize_exhale)));
        mSeries.put(Constants.EXERCISE_PHASE_HOLD, new XYSeries(getString(R.string.exercize_hold)));

        for (Integer phase : mSeries.keySet())
        {
            HashMap<Double, Double> map = getDistribution(split, Tp, Du, phase, 600.0);
            XYSeries ser = mSeries.get(phase);
            for (Double db : map.keySet())
                ser.add(db, map.get(db));
        }


        for (Integer ser : mSeries.keySet())
        {
            mDataset.addSeries(mSeries.get(ser));
            XYSeriesRenderer cRenderer = new XYSeriesRenderer();

            cRenderer.setPointStyle(PointStyle.CIRCLE);
            cRenderer.setFillPoints(true);
            cRenderer.setColor(mColors.get(ser));

            mRenderer.addSeriesRenderer(cRenderer);
        }

        mRenderer.setPointSize(4.0f);
        mRenderer.setPanEnabled(false);
        GraphicalView mChart = ChartFactory.getCubeLineChartView(this, mDataset, mRenderer, 0.3f);//getScatterChartView(this, mDataset, mRenderer);
        LinearLayout chart = null;// (LinearLayout) page_br_vs_hr.findViewById(R.id.chart);

        mRenderer.setChartTitle(getString(R.string.result_breath_heart_rate_chart));
        mRenderer.setXTitle(getString(R.string.result_breath_heart_rate_br));
        mRenderer.setYTitle(getString(R.string.result_breath_heart_rate_averhr));
        mRenderer.setPanEnabled(false, false);
        mRenderer.setZoomEnabled(false, false);
        mRenderer.setYLabelsAngle(270.0f);
        mRenderer.setYLabelsPadding(10.0f);
        mRenderer.setXLabelsColor(Color.DKGRAY);
        mRenderer.setYLabelsColor(0, Color.DKGRAY);

        mRenderer.setMarginsColor(Color.BLACK);

        chart.addView(mChart);
        mChart.repaint();
    }

    private void HRV(int[] RR, int[] cRR, int[] p_duration, int[] p_types, String filename, String tags, String exName)
    {

        StoredStatsDbHelper dbHelper = new StoredStatsDbHelper(this.getApplicationContext());
        HashMap<String, Double> dic = dbHelper.getStatsByFileName(filename);
        if (dic == null)
            dic = HRV.analyse(RR, cRR, p_duration, p_types);


        ArrayList<StatVar> vars = StatVar.load(getResources());
        String display_content = "<center><table border=\"0\" cellpadding=\"4\" cellspacing=\"4\" text-align=\"center\"  vertical-align=\"middle\">";

        display_content += getString(R.string.heart_rate_variability_var_header);
        for (StatVar var : vars)
            if (((var.isHeartRate && dic.containsKey("length") && dic.get("length") != 0.0) || !var.isHeartRate) && dic.containsKey(var.db_alias))
            {
                Double ind = dic.get(var.db_alias);
                if (ind != Double.MIN_VALUE)
                    display_content += String.format(getString(R.string.heart_rate_variability_var_mask),
                            var.human_readable,
                            format(ind),
                            var.unit,
                            getDevString(var.db_alias, tags, exName, dic.get(var.db_alias))
                    );
            }

        display_content += "</table></br>";
        if (dic.containsKey("length") && dic.get("length") == 0.0)
            display_content += getString(R.string.no_heart_rate_provided);
        display_content += "</center>";
        WebView view = (WebView) page_heart_variability.findViewById(R.id.stat_browser);
        WebSettings settings = view.getSettings();
        //Display display = getWindowManager().getDefaultDisplay();
        //Point size = new Point();
        //int width =Math.min( display.getWidth(),display.getHeight());

        //if(width>=600)
        //    settings.setTextSize(WebSettings.TextSize.LARGER);
        //else
        //    settings.setTextSize(WebSettings.TextSize.NORMAL);

        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        display_content = String.format(
                getString(R.string.variables_blank_page),
                display_content);

        view.loadDataWithBaseURL(null, display_content , "text/html", "utf-8", null);
        //view.setInitialScale(100);
        //view.setText(Html.fromHtml(display_content));

        /*String display_content = getString(R.string.no_heart_rate_provided);
        WebView view = (WebView) page_heart_variability.findViewById(R.id.stat_browser);
        view.loadDataWithBaseURL(null, display_content, "text/html", "utf-8", null);*/
        //view.setText(Html.fromHtml(display_content));

    }

    private String getDevString(String db_aliac, String tags, String exName, double current_value)
    {
        StoredStatsDbHelper dbHelper = new StoredStatsDbHelper(getApplicationContext());

        double aver = dbHelper.getAverage(tags, db_aliac, exName);

        aver = 100.0 * (1.0 - aver / current_value);
        if (aver > 0)
            return String.format("<font color=\"green\">+%.0f</font>", aver);
        else if (aver < 0)
            return String.format("<font color=\"red\">%.0f</font>", aver);

        return " 0";
    }

    private String format(double d)
    {
        if (d == (int) d)
            return String.format("%d", (int) d);
        else
            return String.format("%.2f", d);
    }


    private void drawHR(int[] RR)
    {
        if (RR == null || RR.length == 0)
            return;

        XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
        XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
        XYSeries mCurrentSeries = new XYSeries(getString(R.string.hr_chart_title));

        int count = 0;
        double alpha = 0.8;

        double aRR = HRV.average(RR);
        double sumRR = 0;
        for (int i = 1; i < RR.length - 1; i++)
        {
            if (ExerciseLog.abs(RR[i - 1] + RR[i + 1] - RR[i]) < 100 || (RR[i] - aRR) > 400)
            {
                int delta = RR[i - 1] - RR[i + 1];
                int e_1 = RR[i] / 2;
                int e_2 = e_1;
                e_1 -= delta / 4;
                e_2 += delta / 4;
                aRR = alpha * aRR + (1.0 - alpha) * e_1;
                sumRR += e_1;
                mCurrentSeries.add(++count, 60000.0 / e_1);

                aRR = alpha * aRR + (1.0 - alpha) * e_2;
                sumRR += e_2;
                mCurrentSeries.add(++count, 60000.0 / e_2);


            } else if (RR[i] < 1500 && RR[i] > 200)
            {
                aRR = alpha * aRR + (1.0 - alpha) * RR[i];
                sumRR += RR[i];
                mCurrentSeries.add(++count, 60000.0 / RR[i]);

            }
        }


        mDataset.addSeries(mCurrentSeries);
        XYSeriesRenderer mCurrentRenderer = new XYSeriesRenderer();
        mCurrentRenderer.setColor(Color.RED);
        mRenderer.addSeriesRenderer(mCurrentRenderer);

        mRenderer.setPanEnabled(false);
        GraphicalView mChart = ChartFactory.getCubeLineChartView(this, mDataset, mRenderer, 0.5f);
        LinearLayout chart = (LinearLayout) page_heart_rate.findViewById(R.id.chart);

        mRenderer.setChartTitle(getString(R.string.result_heart_rate_chart));
        mRenderer.setXTitle(getString(R.string.chart_heartbeat));
        mRenderer.setYTitle(getString(R.string.chart_heartrate));
        mRenderer.setPanEnabled(false, false);
        mRenderer.setZoomEnabled(false, false);
        mRenderer.setYLabelsAngle(270.0f);
        mRenderer.setYLabelsPadding(10.0f);
        mRenderer.setMarginsColor(Color.BLACK);
        mRenderer.setXLabelsColor(Color.DKGRAY);
        mRenderer.setYLabelsColor(0, Color.DKGRAY);
        chart.addView(mChart);
        mChart.repaint();
    }

    private void SendScreenShot()
    {
        View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        rootView.setDrawingCacheEnabled(true);
        Bitmap bitmap = rootView.getDrawingCache();

        Uri screenshotUri = Uri.fromFile(savebitmap(bitmap));
        final Intent emailIntent1 = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        emailIntent1.putExtra(Intent.EXTRA_STREAM, screenshotUri);
        emailIntent1.setType("image/png");
        startActivity(Intent.createChooser(emailIntent1, "Send email using"));
    }

    private File savebitmap(Bitmap bmp)
    {
        String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
        OutputStream outStream = null;
        File file = new File(extStorageDirectory, "result_screenshot.png");
        if (file.exists())
        {
            file.delete();
            file = new File(extStorageDirectory, "result_screenshot.png");
        }

        try
        {
            outStream = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
            outStream.close();
        } catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
        return file;
    }


    private void drawBScheme(int[] Du, int[] Tp)
    {
        XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
        XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
        XYSeries inhale = new XYSeries(getString(R.string.exercize_inhale));
        XYSeries pause = new XYSeries(getString(R.string.exercize_pause));
        XYSeries exhale = new XYSeries(getString(R.string.exercize_exhale));
        XYSeries hold = new XYSeries(getString(R.string.exercize_hold));


        for (int i = 0; i < Du.length; i++)
        {
            switch (Tp[i])
            {
                case Constants.EXERCISE_PHASE_INHALE:
                    inhale.add((double) i, Du[i] == 0 ? 0 : 60000.0 / Du[i]);
                    break;

                case Constants.EXERCISE_PHASE_PAUSE:
                    pause.add((double) i, Du[i] == 0 ? 0 : 60000.0 / Du[i]);
                    break;

                case Constants.EXERCISE_PHASE_EXHALE:
                    exhale.add((double) i, Du[i] == 0 ? 0 : 60000.0 / Du[i]);
                    break;

                case Constants.EXERCISE_PHASE_HOLD:

                    hold.add((double) i, Du[i] == 0 ? 0 : 60000.0 / Du[i]);
                    break;
            }
        }

        mDataset.addSeries(inhale);
        mDataset.addSeries(pause);
        mDataset.addSeries(exhale);
        mDataset.addSeries(hold);
        Resources res = getResources();
        XYSeriesRenderer inhaleRenderer = new XYSeriesRenderer();
        inhaleRenderer.setColor(res.getColor(R.color.inhale));
        inhaleRenderer.setPointStyle(PointStyle.CIRCLE);
        inhaleRenderer.setPointStrokeWidth(3f);
        inhaleRenderer.setLineWidth(3f);

        XYSeriesRenderer pauseRenderer = new XYSeriesRenderer();
        pauseRenderer.setColor(res.getColor(R.color.pause));
        pauseRenderer.setPointStrokeWidth(2f);
        pauseRenderer.setLineWidth(2f);
        XYSeriesRenderer exhaleRenderer = new XYSeriesRenderer();
        exhaleRenderer.setPointStyle(PointStyle.SQUARE);
        exhaleRenderer.setColor(res.getColor(R.color.exhale));
        exhaleRenderer.setPointStrokeWidth(3f);
        exhaleRenderer.setLineWidth(3f);
        XYSeriesRenderer holdRenderer = new XYSeriesRenderer();
        holdRenderer.setColor(res.getColor(R.color.hold));
        holdRenderer.setPointStrokeWidth(2f);
        holdRenderer.setLineWidth(2f);

        mRenderer.addSeriesRenderer(inhaleRenderer);
        mRenderer.addSeriesRenderer(pauseRenderer);
        mRenderer.addSeriesRenderer(exhaleRenderer);
        mRenderer.addSeriesRenderer(holdRenderer);
        mRenderer.setMarginsColor(Color.BLACK);

        mRenderer.setYLabelsAngle(270.0f);
        mRenderer.setYLabelsPadding(10.0f);

        mRenderer.setPanEnabled(false, false);
        mRenderer.setZoomEnabled(false, false);
        mRenderer.setChartTitle(getString(R.string.result_breath_rate));
        mRenderer.setXTitle(getString(R.string.chart_phases_indexes));
        mRenderer.setYTitle(getString(R.string.chart_phase_rate));
        mRenderer.setShowGrid(true);
        mRenderer.setXLabelsColor(Color.DKGRAY);
        mRenderer.setYLabelsColor(0, Color.DKGRAY);
        GraphicalView mChart = ChartFactory.getCubeLineChartView(this, mDataset, mRenderer, 0.5f);
        LinearLayout chart = null;// (LinearLayout) page_breathing_phases.findViewById(R.id.chart);

        chart.addView(mChart);
        mChart.repaint();
    }

    private void drawSynchrogramm(int[] arr)
    {
        XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
        XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
        XYSeries line = new XYSeries("SY");

        for (int i = 0; i < arr.length; i++)
            line.add(i, arr[i]);

        mDataset.addSeries(line);

        Resources res = getResources();
        XYSeriesRenderer inhaleRenderer = new XYSeriesRenderer();
        inhaleRenderer.setColor(Color.RED);
        inhaleRenderer.setPointStyle(PointStyle.POINT);
        inhaleRenderer.setPointStrokeWidth(0f);
        inhaleRenderer.setLineWidth(3f);

        mRenderer.addSeriesRenderer(inhaleRenderer);
        mRenderer.setMarginsColor(Color.BLACK);

        mRenderer.setYLabelsAngle(270.0f);
        mRenderer.setYLabelsPadding(10.0f);

        mRenderer.setPanEnabled(false, false);
        mRenderer.setZoomEnabled(false, false);
        mRenderer.setChartTitle(getString(R.string.result_breath_rate));
        mRenderer.setXTitle(getString(R.string.chart_phases_indexes));
        mRenderer.setYTitle(getString(R.string.chart_phase_rate));
        mRenderer.setShowGrid(true);
        mRenderer.setXLabelsColor(Color.DKGRAY);
        mRenderer.setYLabelsColor(0, Color.DKGRAY);
        GraphicalView mChart = ChartFactory.getLineChartView(this, mDataset, mRenderer);
        LinearLayout chart = null;// (LinearLayout) page_breathing_synch.findViewById(R.id.chart);

        chart.addView(mChart);
        mChart.repaint();

    }

    public void getInfo()
    {
        FragmentManager fm = getSupportFragmentManager();
        InfoDialog infoDialog = new InfoDialog();
        infoDialog.show(fm, "nonono");
    }
}
