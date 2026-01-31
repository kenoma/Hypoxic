package com.lanius.kenoma.pob;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.lanius.kenoma.pob.Classes.SwipeDetector;
import com.lanius.kenoma.pob.Classes.Tag;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import kankan.wheel.widget.WheelView;
import kankan.wheel.widget.adapters.ArrayWheelAdapter;

public class TagSelector extends Activity
{
    public static final String TAGS_NAME = "Tags";
    TagsAdapter dataAdapter = null;
    SwipeDetector swipeDetector = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_tag_selector);
        displayListView();

        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    public void Resume(View view)
    {
        String tags = dataAdapter.GetSelectedTags();

        Intent resultIntent = new Intent();
        resultIntent.putExtra("tags", tags);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    private void displayListView()
    {
        SharedPreferences settings = getSharedPreferences(TAGS_NAME, 0);
        String[] tags = settings.getString("tags", getString(R.string.default_tags)).split(";");

        ArrayList<Tag> tagList = new ArrayList<Tag>();
        for (String tag : tags)
            tagList.add(new Tag(tag, false));

        tagList.add(new Tag("", false));
        swipeDetector = new SwipeDetector();
        AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3)
            {
                updateListItems();
                if (swipeDetector.swipeDetected())
                    if (swipeDetector.getAction() == SwipeDetector.Action.RL)
                    {
                        ImageButton button = (ImageButton) view.findViewById(R.id.deleteButton);
                        button.setVisibility(View.VISIBLE);
                    }
            }
        };
        dataAdapter = new TagsAdapter(this, R.layout.tag_info, tagList);
        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(dataAdapter);
        listView.setOnTouchListener(swipeDetector);
        listView.setOnItemClickListener(listener);
    }

    public void DeleteItem(View v)
    {
        Tag tag = (Tag) v.getTag();
        dataAdapter.remove(tag);
        SharedPreferences settings = getSharedPreferences(TAGS_NAME, 0);
        SharedPreferences.Editor edit = settings.edit();
        edit.putString("tags", dataAdapter.GetTags());
        edit.commit();
        updateListItems();
    }

    private void updateListItems()
    {
        ListView listView = (ListView) findViewById(R.id.list_view);
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
        dataAdapter.notifyDataSetChanged();
    }

    private class TagsAdapter extends ArrayAdapter<Tag>
    {
        private ArrayList<Tag> tagList;
        private TextView.OnEditorActionListener mEditorActionListener = new TextView.OnEditorActionListener()
        {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                // TODO Auto-generated method stub
                if (actionId == EditorInfo.IME_ACTION_DONE)
                {
                    String str = v.getText().toString();
                    Tag tag = (Tag) v.getTag();
                    if (tag != null && tag.name.length() == 0 && str != null && str.length() != 0)
                    {
                        tag.name = str;
                        SharedPreferences settings = getSharedPreferences(TAGS_NAME, 0);
                        SharedPreferences.Editor edit = settings.edit();
                        edit.putString("tags", GetTags());
                        edit.commit();
                        tagList.add(new Tag("", false));
                        notifyDataSetChanged();
                        return true;
                    }
                }

                return false;
            }
        };

        public TagsAdapter(Context context, int textViewResourceId,
                           ArrayList<Tag> tagList)
        {
            super(context, textViewResourceId, tagList);
            this.tagList = tagList;
        }

        public String GetTags()
        {
            String res = "";
            for (Tag tag : tagList)
                    res += tag.name + ";";
            return res;
        }

        public String GetSelectedTags()
        {
            String res = "";
            for (Tag tag : tagList)
                if (tag.selected)
                    res += tag.name + ";";
            return res;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            ViewHolder holder = null;
            Log.v("ConvertView", String.valueOf(position));

            if (convertView == null)
            {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = vi.inflate(R.layout.tag_info, null);

                holder = new ViewHolder();
                // holder.code = (TextView) convertView.findViewById(R.id.code);
                holder.name = (CheckBox) convertView.findViewById(R.id.checkBox);
                holder.delete = (ImageButton) convertView.findViewById(R.id.deleteButton);
                holder.input = (EditText) convertView.findViewById(R.id.textInput);

                convertView.setTag(holder);

                holder.name.setOnClickListener(new View.OnClickListener()
                {
                    public void onClick(View v)
                    {
                        updateListItems();
                        CheckBox cb = (CheckBox) v;
                        Tag tag = (Tag) cb.getTag();

                        tag.setSelected(cb.isChecked());
                    }
                });
            } else
            {
                holder = (ViewHolder) convertView.getTag();
            }

            Tag tag = tagList.get(position);
            // holder.code.setText(" (" + country.getCode() + ")");
            holder.name.setText(tag.getName());
            holder.name.setChecked(tag.isSelected());
            holder.name.setTag(tag);
            holder.delete.setTag(tag);
            holder.input.setTag(tag);
            holder.input.setOnEditorActionListener(mEditorActionListener);

            if (tag.name != null && tag.name == "")
            {
                holder.input.setVisibility(View.VISIBLE);
                holder.name.setVisibility(View.GONE);
            } else
            {
                holder.input.setVisibility(View.GONE);
                holder.name.setVisibility(View.VISIBLE);
            }

            return convertView;

        }

        private class ViewHolder
        {
            CheckBox name;
            ImageButton delete;
            EditText input;
        }


    }

}
