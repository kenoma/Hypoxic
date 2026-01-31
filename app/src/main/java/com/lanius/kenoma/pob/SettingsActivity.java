package com.lanius.kenoma.pob;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.lanius.kenoma.pob.Classes.Constants;
import com.lanius.kenoma.pob.Classes.DatePreference;
import com.lanius.kenoma.pob.Classes.SingleMediaScanner;
import com.lanius.kenoma.pob.Classes.SoundListPreference;
import com.lanius.kenoma.pob.dialogs.DirectoryChooserDialog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class SettingsActivity extends PreferenceActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        PreferenceManager.setDefaultValues(this, R.xml.settings,
                false);
        initSummary(getPreferenceScreen());

        Preference button = (Preference) findPreference("prefPathToAdditionalEx");
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            private String m_chosenDir = "";

            @Override
            public boolean onPreferenceClick(Preference arg0)
            {
                // Create DirectoryChooserDialog and register a callback
                DirectoryChooserDialog directoryChooserDialog =
                        new DirectoryChooserDialog(SettingsActivity.this,
                                new DirectoryChooserDialog.ChosenDirectoryListener()
                                {
                                    @Override
                                    public void onChosenDir(String chosenDir)
                                    {
                                        m_chosenDir = chosenDir;
                                        Preference pref = findPreference("prefPathToAdditionalEx");
                                        SharedPreferences.Editor ed = pref.getEditor();
                                        ed.putString("prefPathToAdditionalEx", m_chosenDir);
                                        ed.commit();
                                        pref.setSummary(m_chosenDir);
                                        InputStream is = getResources().openRawResource(R.raw.blank);
                                        String content = "";
                                        try
                                        {
                                            byte[] input = new byte[is.available()];
                                            while (is.read(input) != -1)
                                            {
                                            }
                                            content += new String(input);
                                        } catch (IOException e)
                                        {
                                            e.printStackTrace();
                                        }
                                        writeToFile(m_chosenDir, content);
                                        Toast.makeText(SettingsActivity.this, "Chosen directory: " +
                                                chosenDir, Toast.LENGTH_LONG).show();
                                    }
                                });

                // Load directory chooser dialog for initial 'm_chosenDir' directory.
                // The registered callback will be called upon final directory selection.
                directoryChooserDialog.chooseDirectory(m_chosenDir);
                return true;
            }
        });
    }

    private void writeToFile(String path, String _data)
    {
        try
        {
            File myFile = new File(path, "hypoxic_add.xml");
            if (!myFile.exists())
            {
                myFile.createNewFile();

                FileOutputStream fos;
                byte[] data = _data.getBytes();
                try
                {
                    fos = new FileOutputStream(myFile);
                    fos.write(data);
                    fos.flush();
                    fos.close();

                    SingleMediaScanner ms = new SingleMediaScanner(this, new File(path + "/hypoxic_add.xml"));
                } catch (FileNotFoundException e)
                {
                    e.printStackTrace();
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null)
        {
            Uri soundUri = data.getData();
           /* MediaPlayer mp = MediaPlayer.create(this, soundUri);
            boolean isFileEnoughShort = false;
            long duration = mp.getDuration();
            if (duration < 5000 && duration != -1)
                isFileEnoughShort = true;*/

            //if (isFileEnoughShort)
            //{
            String path = getFilePathFromContentUri(soundUri, getContentResolver());
            Log.d("settings", "Intent data" + data.getData().toString());
            String sett = "prefInhaleSound";
            if (requestCode == Constants.REQUEST_SOUND_FILE_PAUSE)
                sett = "prefPauseSound";
            else if (requestCode == Constants.REQUEST_SOUND_FILE_EXHALE)
                sett = "prefExhaleSound";
            else if (requestCode == Constants.REQUEST_SOUND_FILE_HOLD)
                sett = "prefHoldSound";

            Preference pref = findPreference(sett);
            SharedPreferences.Editor ed = pref.getEditor();
            ed.putString(sett, path);
            ed.commit();
            pref.setSummary(path);
            //} else
            //     Toast.makeText(this, getString(R.string.settings_only_short_audio_enabled), Toast.LENGTH_SHORT).show();
        }
    }

    private String getFilePathFromContentUri(Uri uri, ContentResolver contentResolver)
    {
        String filePath;
        String[] filePathColumn = {MediaStore.MediaColumns.DATA};

        Cursor cursor = contentResolver.query(uri, filePathColumn, null, null, null);
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        filePath = cursor.getString(columnIndex);
        cursor.close();
        return filePath;
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key)
    {
        updatePrefSummary(findPreference(key));
    }

    private void initSummary(Preference p)
    {
        if (p instanceof PreferenceGroup)
        {
            PreferenceGroup pGrp = (PreferenceGroup) p;
            for (int i = 0; i < pGrp.getPreferenceCount(); i++)
            {
                initSummary(pGrp.getPreference(i));
            }
        } else
        {
            updatePrefSummary(p);
        }
    }

    private void updatePrefSummary(Preference p)
    {
        if (p instanceof SoundListPreference)
        {
            p.setSummary(((SoundListPreference) p).getValue());
        } else if (p instanceof ListPreference)
        {
            ListPreference listPref = (ListPreference) p;
            p.setSummary(listPref.getEntry());
        }
        if (p instanceof EditTextPreference)
        {
            EditTextPreference editTextPref = (EditTextPreference) p;
            if (p.getTitle().toString().contains("assword"))
            {
                p.setSummary("******");
            } else
            {
                p.setSummary(editTextPref.getText());
            }
        }
        if (p instanceof MultiSelectListPreference)
        {
            EditTextPreference editTextPref = (EditTextPreference) p;
            p.setSummary(editTextPref.getText());
        }
        if (p instanceof DatePreference)
        {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            p.setSummary(pref.getString("prefUserBirthdate", ""));
        }
        if (p.getKey().equals("prefPathToAdditionalEx"))
        {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            p.setSummary(pref.getString("prefPathToAdditionalEx", ""));
        }
    }
}
