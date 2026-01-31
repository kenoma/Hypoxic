package com.lanius.kenoma.pob;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.lanius.kenoma.pob.Classes.ExerciseLog;
import com.lanius.kenoma.pob.Classes.HRV;
import com.lanius.kenoma.pob.Classes.SwipeDetector;
import com.lanius.kenoma.pob.Classes.exFileInfo;
import com.lanius.kenoma.pob.sql.StoredStatsDbHelper;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ViewDatabaseActivity extends ActionBarActivity
{

    SwipeDetector swipeDetector = null;
    ListView listView;
    private RecordsAdapter dataAdapter;
    private boolean isButtonVisible = false;
    private ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        StoredStatsDbHelper dbHelper = new StoredStatsDbHelper(getApplicationContext());
        if (!dbHelper.isAnyRecords())
        {
            progress = ProgressDialog.show(this, getString(R.string.progress_recompute_title),
                    getString(R.string.progress_recompute_text), true);

            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    StoredStatsDbHelper dbHelper = new StoredStatsDbHelper(getApplicationContext());
                    recomputeStats(dbHelper);
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            if (progress != null && progress.isShowing())
                                progress.dismiss();
                        }
                    });
                }
            }).start();

        }


        setContentView(R.layout.activity_view_database);
        displayListView();

//        int requestedOrientation = getResources().getInteger(R.integer.allowed_orientations);
//        int currentOrientation = getRequestedOrientation();
//        if (currentOrientation != requestedOrientation)
//        {
//            setRequestedOrientation(requestedOrientation);
//        }
    }

    @Override
    public void onPause()
    {
        super.onPause();

        if (progress != null && progress.isShowing())
            progress.dismiss();
        progress = null;
    }

    // Initiating Menu XML file (menu.xml)
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_view_database, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.db_send_email:
                SendEmail();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void recomputeStats(StoredStatsDbHelper dbHelper)
    {
        String path = this.getApplicationContext().getFilesDir().getAbsolutePath();
        Log.d("Files", "Path: " + path);
        File f = new File(path);
        File file[] = f.listFiles();
        if (file != null)
        {
            Log.d("Files", "Size: " + file.length);
            for (int i = 0; i < file.length; i++)
                if (file[i].getName().contains(".ex"))
                {
                    Log.d("Recompute", "FileName:" + file[i].getName());
                    try
                    {
                        InputStream inputStream = openFileInput(file[i].getName());
                        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        int j = inputStreamReader.read();
                        while (j != -1)
                        {
                            byteArrayOutputStream.write(j);
                            j = inputStreamReader.read();
                        }
                        inputStream.close();

                        ExerciseLog log = new ExerciseLog();
                        log.Deserialize(byteArrayOutputStream.toString());
                        //if (log.RR.size() != 0)
                        {
                            HashMap<String, Double> dic = HRV.analyse(HRV.filterRR(toIntArray(log.RR)), log.controlValue, log.phaseDurations, log.phaseTypes);
                            if (dic != null)
                                dbHelper.storeData(file[i].getName(), log, dic);
                        }
                    } catch (IOException fond)
                    {
                        Log.d("Recompute", fond.getMessage());
                    }
                    //Toast.makeText(ViewDatabaseActivity.this, String.format("%s/%s done...", i, file.length), Toast.LENGTH_SHORT).show();
                }
        }
    }

    private int[] toIntArray(List<Integer> integerList)
    {
        int[] intArray = new int[integerList.size()];
        for (int i = 0; i < integerList.size(); i++)
            intArray[i] = integerList.get(i);

        return intArray;
    }

    private void displayListView()
    {
        String path = this.getApplicationContext().getFilesDir().getAbsolutePath();
        Log.d("Files", "Path: " + path);
        File f = new File(path);
        File file[] = f.listFiles();
        if (file != null)
        {
            Log.d("Files", "Size: " + file.length);
            for (int i = 0; i < file.length; i++)
            {
                Log.d("Files", "FileName:" + file[i].getName());
            }

            ArrayList<exFileInfo> exList = new ArrayList<exFileInfo>();
            for (File ex : file)
                if (ex.getName().contains(".ex"))
                {
                    exFileInfo d = new exFileInfo(ex.getName());
                    exList.add(d);
                }

            swipeDetector = new SwipeDetector();

            AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3)
                {
                    updateListItems();
                    if (swipeDetector.swipeDetected())
                    {
                        if (swipeDetector.getAction() == SwipeDetector.Action.RL)
                        {
                            ImageButton button = (ImageButton) view.findViewById(R.id.deleteButton);
                            button.setVisibility(View.VISIBLE);
                            isButtonVisible = true;
                            //updateListItems();
                        } else
                        {
                            ImageButton button = (ImageButton) view.findViewById(R.id.deleteButton);
                            button.setVisibility(View.GONE);
                        }
                    } else
                    {
                        ImageButton button = (ImageButton) view.findViewById(R.id.show_chart);
                        button.performClick();
                    }
                }
            };

            dataAdapter = new RecordsAdapter(this, R.layout.db_record_info, exList);
            listView = (ListView) findViewById(R.id.list_view);
            listView.setAdapter(dataAdapter);
            listView.setOnTouchListener(swipeDetector);
            listView.setOnItemClickListener(listener);

            listView.setOnScrollListener(new AbsListView.OnScrollListener()
            {
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
                {
                    // TODO Auto-generated method stub
                }

                public void onScrollStateChanged(AbsListView view, int scrollState)
                {
                    updateListItems();
                }
            });
        }
    }

    public void DeleteItem(View v)
    {

        exFileInfo ex = (exFileInfo) v.getTag();
        if (ex != null)
        {
            String path = this.getApplicationContext().getFilesDir().getAbsolutePath();
            File file = new File(path + "//" + ex.fName);
            boolean delfile = file.delete();

            if (delfile)
            {
                StoredStatsDbHelper dbHelper = new StoredStatsDbHelper(getApplicationContext());
                dbHelper.deleteRecord(ex.fName);

            }

            dataAdapter.remove(ex);
            dataAdapter.exList.remove(ex);
            updateListItems();
        }
    }

    private void updateListItems()
    {
        if (isButtonVisible)
        {
            View v;
            ImageButton butt;

            for (int i = 0; i < listView.getCount(); i++)
            {
                v = listView.getChildAt(i);
                if (v != null)
                {
                    butt = (ImageButton) v.findViewById(R.id.deleteButton);
                    butt.setVisibility(View.GONE);
                }
            }
            isButtonVisible = false;
        }
        dataAdapter.notifyDataSetChanged();
    }


    private String getTextArchiveToSendItviaEmail()
    {
        StringBuilder sb = new StringBuilder();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        sb.append(String.format("{\"UserBirthdate\":\"%s\",\n", sharedPrefs.getString("prefUserBirthdate", "NA")));
        sb.append(String.format("\"UserHeight\":%s,\n", sharedPrefs.getString("prefUserHeight", "NA")));
        sb.append(String.format("\"UserSex\":\"%s\",\n", sharedPrefs.getString("prefUserSex", "NA")));

        sb.append("\"records\":[\n");
        String path = this.getApplicationContext().getFilesDir().getAbsolutePath();
        Log.d("Files", "Path: " + path);
        File f = new File(path);
        File file[] = f.listFiles();
        if (file != null)
        {
            Log.d("Files", "Size: " + file.length);
            for (int i = 0; i < file.length; i++)
            {
                try
                {
                    BufferedReader br = new BufferedReader(new FileReader(file[i]));
                    String line;

                    while ((line = br.readLine()) != null)
                    {
                        sb.append(line);
                        sb.append('\n');
                    }
                    br.close();
                } catch (IOException e)
                {
                    Log.d("Error", "Files:" + e.getMessage());
                }
                Log.d("Files", "FileName:" + file[i].getName());
                sb.append("\n,\n");
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    public void SendEmail()
    {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.database_email_title));
        i.putExtra(Intent.EXTRA_TEXT, getTextArchiveToSendItviaEmail());

        try
        {
            startActivity(Intent.createChooser(i, getString(R.string.database_send_email)));
        } catch (android.content.ActivityNotFoundException ex)
        {
            Toast.makeText(ViewDatabaseActivity.this, getString(R.string.database_fail_email), Toast.LENGTH_SHORT).show();
        }
    }


    private class RecordsAdapter extends ArrayAdapter<exFileInfo>
    {
        private ArrayList<exFileInfo> exList;

        public RecordsAdapter(Context context, int textViewResourceId,
                              ArrayList<exFileInfo> logList)
        {
            super(context, textViewResourceId, logList);
            this.exList = new ArrayList<exFileInfo>();
            this.exList.addAll(logList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            ViewHolder holder = null;
            Log.v("ConvertView", String.valueOf(position));

            if (convertView == null)
            {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = vi.inflate(R.layout.db_record_info, null);

                holder = new ViewHolder();

                holder.name = (TextView) convertView.findViewById(R.id.name);
                holder.start = (TextView) convertView.findViewById(R.id.start);
                holder.end = (TextView) convertView.findViewById(R.id.end);
                holder.button = (ImageButton) convertView.findViewById(R.id.show_chart);
                holder.delbutton = (ImageButton) convertView.findViewById(R.id.deleteButton);

                convertView.setTag(holder);

                holder.button.setOnClickListener(new View.OnClickListener()
                {
                    public void onClick(View v)
                    {
                        ImageButton cb = (ImageButton) v;
                        exFileInfo fdata = (exFileInfo) cb.getTag();
                        Toast.makeText(getApplicationContext(), fdata.Name, Toast.LENGTH_SHORT).show();
                        int i;
                        try
                        {
                            InputStream inputStream = openFileInput(fdata.fName);
                            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            i = inputStreamReader.read();
                            while (i != -1)
                            {
                                byteArrayOutputStream.write(i);
                                i = inputStreamReader.read();
                            }
                            inputStream.close();

                            ExerciseLog log = new ExerciseLog();
                            log.Deserialize(byteArrayOutputStream.toString());
                            Intent resultAct = new Intent(ViewDatabaseActivity.this, ResultActivity.class);
                            resultAct.putExtra("RR", ExerciseLog.convertIntegers(log.RR));
                            resultAct.putExtra("d", log.phaseDurations);
                            resultAct.putExtra("t", log.phaseTypes);
                            resultAct.putExtra("control", log.controlValue);
                            resultAct.putExtra("Tags", log.Tags);
                            resultAct.putExtra("filename", fdata.fName);
                            resultAct.putExtra("exName", log.exercisName);

                            startActivity(resultAct);
                        } catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                    }
                });
            } else
            {
                holder = (ViewHolder) convertView.getTag();
            }

            exFileInfo tag = exList.get(position);
            holder.name.setText(tag.Name);
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM");
            holder.start.setText(sdf.format(tag.Started));
            sdf = new SimpleDateFormat("HH:mm");
            holder.end.setText(sdf.format(tag.Started) + "-" + sdf.format(tag.Finished));
            holder.button.setTag(tag);
            holder.delbutton.setTag(tag);
            return convertView;

        }

        private class ViewHolder
        {
            TextView name;
            TextView start;
            TextView end;
            ImageButton button;
            ImageButton delbutton;
        }

    }
}
