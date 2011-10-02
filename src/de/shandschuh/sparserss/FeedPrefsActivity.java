package de.shandschuh.sparserss;

import de.shandschuh.sparserss.provider.FeedData;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;

public class FeedPrefsActivity extends PreferenceActivity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.feedsettings);
		
		final String feedId = getIntent().getStringExtra(FeedData.FeedColumns._ID);
		
		OnPreferenceChangeListener listener = new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference pref, Object change) {
				ContentValues values = new ContentValues();
				
				if( pref.getKey().equals(Strings.FEED_SETTINGS_ALERT_RINGTONE)) {
					values.put(FeedData.FeedColumns.ALERT_RINGTONE, change.toString());
					getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(feedId), values, null, null);
					return true;
				} else if(pref.getKey().equals(Strings.FEED_SETTINGS_OTHER_ALERT_RINGTONE)) {
					int val = change.equals(Boolean.TRUE) ? 1 : 0;
					values.put(FeedData.FeedColumns.OTHER_ALERT_RINGTONE, val);
					getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(feedId), values, null, null);
					return true;
				} else if(pref.getKey().equals(Strings.FEED_SETTINGS_SKIP_ALERT)) {
					int val = change.equals(Boolean.TRUE) ? 1 : 0;
					values.put(FeedData.FeedColumns.SKIP_ALERT, val);
					getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(feedId), values, null, null);
					return true;
				}
				return false;
			}
		};
		
		CheckBoxPreference skipAlert = (CheckBoxPreference)findPreference(Strings.FEED_SETTINGS_SKIP_ALERT);
		CheckBoxPreference other_ringtone = (CheckBoxPreference)findPreference(Strings.FEED_SETTINGS_OTHER_ALERT_RINGTONE);
		Preference ringtone = findPreference(Strings.FEED_SETTINGS_ALERT_RINGTONE);
		
		skipAlert.setOnPreferenceChangeListener(listener);
		ringtone.setOnPreferenceChangeListener(listener);
		other_ringtone.setOnPreferenceChangeListener(listener);
		
		Cursor defaultValues = getContentResolver().query(FeedData.FeedColumns.CONTENT_URI(feedId), new String[] {
			FeedData.FeedColumns.OTHER_ALERT_RINGTONE,
			FeedData.FeedColumns.ALERT_RINGTONE,
			FeedData.FeedColumns.SKIP_ALERT
		}, null, null, null);
		
		defaultValues.moveToFirst();
		other_ringtone.setChecked(defaultValues.getInt(0) == 1);
		ringtone.setDefaultValue(defaultValues.getString(1)); // XXX does not work
		skipAlert.setChecked(defaultValues.getInt(2) == 1);
		defaultValues.close();
		
	}

}
