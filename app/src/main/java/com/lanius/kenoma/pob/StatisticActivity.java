package com.lanius.kenoma.pob;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.lanius.kenoma.pob.Classes.HRV;
import com.lanius.kenoma.pob.Classes.StatVar;
import com.lanius.kenoma.pob.Classes.StatVarAdapter;
import com.lanius.kenoma.pob.Classes.exFileInfo;
import com.lanius.kenoma.pob.sql.StoredStatsDbHelper;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.RangeCategorySeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class StatisticActivity extends ActionBarActivity
{
    List<View> pages = new ArrayList<View>();
    ViewPager viewPager;
    private SimplePagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = LayoutInflater.from(this);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ArrayList<exFileInfo> exers = exList();
        HashSet<String> distinct = new HashSet<>();
        for (exFileInfo fi : exers)
            distinct.add(fi.Name);

        for (String name : distinct)
            addExPage(exers, name, inflater);

        pagerAdapter = new SimplePagerAdapter(pages);
        viewPager = new ViewPager(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(0);

        if (pages.size() == 0)
        {
            finish();
            return;
        }

        setTitle(pages.get(viewPager.getCurrentItem()).getTag().toString());
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int i, float v, int i2)
            {

            }

            @Override
            public void onPageSelected(int i)
            {
                setTitle(pagerAdapter.pages.get(i).getTag().toString());
                drawChart();
            }

            @Override
            public void onPageScrollStateChanged(int i)
            {

            }
        });
        setContentView(viewPager);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_statistic, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.stat_screenshot:
                SendScreenShot();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void addExPage(ArrayList<exFileInfo> exList, String exName, LayoutInflater inflater)
    {
        View vi = inflater.inflate(R.layout.page_statistic, null);
        vi.setTag(exName);

        Spinner spinner = (Spinner) vi.findViewById(R.id.vars);
        //ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.stat_vars, android.R.layout.simple_spinner_item);

        ArrayList<StatVar> variables = StatVar.load(getResources());

        ArrayList<StatVar> checked = new ArrayList<>();
        StoredStatsDbHelper dbHelper = new StoredStatsDbHelper(getApplicationContext());
        for (StatVar var : variables)
            if (!var.db_alias.equals("length") && dbHelper.getData(exName, var.db_alias).size() > 1)
                checked.add(var);
        if (checked.size() == 0)
            return;


        StatVarAdapter iadapter = new StatVarAdapter(this, checked);
        iadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(iadapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                Log.d("StatSpinner", "onItemSelected");
                drawChart();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });

        spinner = (Spinner) vi.findViewById(R.id.mode);
        ArrayAdapter<CharSequence> sadapter = ArrayAdapter.createFromResource(this, R.array.stat_modes, android.R.layout.simple_spinner_item);
        sadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(sadapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                Log.d("StatSpinner", "onItemSelected");
                drawChart();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });

        pages.add(vi);

    }


    private void drawChart()
    {
        View currentChart = pagerAdapter.pages.get(viewPager.getCurrentItem());
        if (currentChart == null)
            return;

        String exName = currentChart.getTag().toString();
        String var = "";
        String human_readable = "";
        String units = "";
        int mode = 0;

        Spinner spinner = (Spinner) currentChart.findViewById(R.id.vars);
        if (spinner != null)
        {
            StatVar sel = (StatVar) spinner.getSelectedItem();
            var = sel.db_alias;
            human_readable = sel.human_readable;
            units = sel.unit;
        } else
            return;

        spinner = (Spinner) currentChart.findViewById(R.id.mode);
        if (spinner != null)
            mode = spinner.getSelectedItem().toString().equals("tag") ? 1 : 0;
        else
            return;

        StoredStatsDbHelper dbHelper = new StoredStatsDbHelper(getApplicationContext());
        TextView tv = (TextView) currentChart.findViewById(R.id.text_statistic);
        Pair<Double, Double> averstd = HRV.getAverStd(dbHelper.getData(exName, var));
        if (Double.isNaN(averstd.first) || Double.isNaN(averstd.second))
            tv.setText(getString(R.string.statistics_no_data));
        else
            tv.setText(String.format(getString(R.string.statistics_text_mask),
                    human_readable,
                    averstd.first,
                    averstd.second,
                    units
            ));

        LinearLayout chart = (LinearLayout) currentChart.findViewById(R.id.chart_top);
        chart.removeAllViews();

        if (mode == 0)
            drawRegularPlot(exName, var, chart, dbHelper);
        else if (mode == 1)
            drawRangePlot(exName, var, chart, dbHelper);
    }

    private void drawRangePlot(String exName, String var, LinearLayout chart, StoredStatsDbHelper dbHelper)
    {
        String[] tags = dbHelper.getTags(exName, false);
        XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
        XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
        RangeCategorySeries series = new RangeCategorySeries(exName);
        int bars = 1;
        double ymin = Double.MAX_VALUE;
        double ymax = 0.0;
        for (String tag : tags)
        {
            Pair<Double, Double> pair = dbHelper.getMinMax(exName, var, tag);
            if (pair.first != 0.0 && pair.second != 0.0)
            {
                String readableTag = "";
                String[] splts = tag.split(";");
                for (int i = 0; i < splts.length; i++)
                {
                    readableTag += splts[i];
                    if (i != splts.length - 1)
                        readableTag += "\n";
                }

                series.add(readableTag, pair.first, pair.second);
                ymin = Math.min(ymin, pair.first);
                ymax = Math.max(ymax, pair.second);
                mRenderer.addXTextLabel(bars++, tag.equals("") ? getString(R.string.statistics_no_tags) : readableTag);
            }
        }
        mDataset.addSeries(series.toXYSeries());
        XYSeriesRenderer barRenderer = new XYSeriesRenderer();
        barRenderer.setColor(Color.GREEN);
        barRenderer.setDisplayChartValues(true);
        barRenderer.setChartValuesTextSize(12);
        barRenderer.setChartValuesFormat(new DecimalFormat("#.##"));
        barRenderer.setChartValuesSpacing(3);
        barRenderer.setAnnotationsTextSize(getResources().getDimension(R.dimen.statistic_chart_tags_text_size));

        mRenderer.setBarSpacing(0.5);
        mRenderer.setXLabels(0);
        mRenderer.setYLabels(0);
        mRenderer.setXLabelsAlign(Paint.Align.CENTER);
        mRenderer.setYAxisMax(ymax + 0.2 * ymax);
        mRenderer.setYAxisMin(ymin - 0.2 * ymin);
        mRenderer.setXAxisMin(-0.5);
        mRenderer.setXAxisMax(tags.length + 0.5);

        mRenderer.setLabelsTextSize(getResources().getDimension(R.dimen.statistic_chart_tags_text_size));
        mRenderer.setAxisTitleTextSize(getResources().getDimension(R.dimen.statistic_chart_tags_text_size));
        mRenderer.setLegendTextSize(getResources().getDimension(R.dimen.statistic_chart_tags_text_size));
        mRenderer.addSeriesRenderer(barRenderer);

        mRenderer.setMarginsColor(Color.BLACK);
        mRenderer.setShowLegend(false);

        mRenderer.setYLabelsAngle(270.0f);
        mRenderer.setYLabelsPadding(10.0f);

        mRenderer.setPanEnabled(false, false);
        mRenderer.setZoomEnabled(false, false);
        mRenderer.setChartTitle("");

        mRenderer.setShowGrid(false);
        mRenderer.setShowTickMarks(false);
        mRenderer.setXLabelsColor(Color.DKGRAY);
        mRenderer.setYLabelsColor(0, Color.DKGRAY);

        GraphicalView mChart = ChartFactory.getRangeBarChartView(this, mDataset, mRenderer, BarChart.Type.DEFAULT);
        //GraphicalView mChart = ChartFactory.getTimeChartView(this, mDataset, mRenderer, "dd MMMM H:mm");

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
        File file = new File(extStorageDirectory, "stat_screenshot.png");
        if (file.exists())
        {
            file.delete();
            file = new File(extStorageDirectory, "stat_screenshot.png");
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

    private void drawRegularPlot(String exName, String var, LinearLayout chart, StoredStatsDbHelper dbHelper)
    {
        XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
        XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();

        String[] tags = dbHelper.getTags(exName, false);
        int[] allColors = getResources().getIntArray(R.array.rainbow);

        int ch = 0;
        for (String tag : tags)
        {
            ArrayList<Pair<Long, Double>> data = dbHelper.getStats(exName, var, tag);
            if (data.size() > 0)
            {
                XYSeries line = new XYSeries(tag.equals("") ? getString(R.string.statistics_no_tags) : tag);
                for (int i = 0; i < data.size(); i++)
                    line.add(data.get(i).first, data.get(i).second);

                mDataset.addSeries(line);

                XYSeriesRenderer lineRenderer = new XYSeriesRenderer();
                lineRenderer.setColor(allColors[ch++ % (allColors.length - 1)]);
                lineRenderer.setPointStyle(PointStyle.CIRCLE);
                lineRenderer.setPointStrokeWidth(3f);
                lineRenderer.setLineWidth(3f);

                mRenderer.addSeriesRenderer(lineRenderer);
            }
        }

        mRenderer.setMarginsColor(Color.BLACK);
        mRenderer.setShowLegend(true);
        mRenderer.setYLabelsAngle(270.0f);
        mRenderer.setYLabelsPadding(10.0f);

        mRenderer.setLabelsTextSize(getResources().getDimension(R.dimen.statistic_chart_tags_text_size));
        mRenderer.setAxisTitleTextSize(getResources().getDimension(R.dimen.statistic_chart_tags_text_size));
        mRenderer.setLegendTextSize(getResources().getDimension(R.dimen.statistic_chart_tags_text_size));
        mRenderer.setFitLegend(true);

        mRenderer.setPanEnabled(false, false);
        mRenderer.setZoomEnabled(false, false);
        mRenderer.setChartTitle("");

        //mRenderer.setMargins(new int[]{10, 0, 90, 0}); //top, left, bottom, right.
        //mRenderer.setXTitle(getString(R.string.chart_phases_indexes));
        //mRenderer.setYTitle(getString(R.string.chart_phase_rate));
        mRenderer.setShowGrid(true);
        mRenderer.setXLabelsColor(Color.DKGRAY);
        mRenderer.setYLabelsColor(0, Color.DKGRAY);
        GraphicalView mChart = ChartFactory.getTimeChartView(this, mDataset, mRenderer, "dd MMMM H:mm");

        chart.addView(mChart);
        mChart.repaint();
    }

    private ArrayList<exFileInfo> exList()
    {
        String path = this.getApplicationContext().getFilesDir().getAbsolutePath();
        Log.d("Files", "Path: " + path);
        File f = new File(path);
        File file[] = f.listFiles();
        ArrayList<exFileInfo> _exList = new ArrayList<exFileInfo>();
        if (file != null)
        {
            Log.d("Stat", "Size: " + file.length);
            for (int i = 0; i < file.length; i++)
            {
                Log.d("Stat", "FileName:" + file[i].getName());
            }

            for (File ex : file)
                if (ex.getName().contains(".ex"))
                {
                    exFileInfo d = new exFileInfo(ex.getName());
                    _exList.add(d);
                }
        }

        return _exList;
    }
}
