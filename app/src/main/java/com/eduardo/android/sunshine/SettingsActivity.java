package com.eduardo.android.sunshine;

import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);
        bindPreferenceSummaryTovalue(findPreference(getString(R.string.pref_local_key)));
        bindPreferenceSummaryTovalue(findPreference(getString(R.string.pref_units_key)));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String value = newValue.toString();
        if(preference instanceof ListPreference){
            ListPreference listPreference = (ListPreference) preference;
            int indicePref = listPreference.findIndexOfValue(value);
            if(indicePref >= 0){
                preference.setSummary(listPreference.getEntries()[indicePref]);
            }
        } else {
            preference.setSummary(value);
        }

        return true;
    }

    private void bindPreferenceSummaryTovalue(Preference preference){
        preference.setOnPreferenceChangeListener(this);
        onPreferenceChange(preference, PreferenceManager
        .getDefaultSharedPreferences(preference.getContext())
        .getString(preference.getKey(), ""));
    }
}
