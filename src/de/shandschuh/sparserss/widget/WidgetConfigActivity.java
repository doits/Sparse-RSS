/**
 * Sparse rss
 * 
 * Copyright (c) 2010-2012 Stefan Handschuh
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package de.shandschuh.sparserss.widget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.view.View;
import android.view.View.OnClickListener;
import de.shandschuh.sparserss.R;
import de.shandschuh.sparserss.provider.FeedData;

public class WidgetConfigActivity extends PreferenceActivity {
	private int widgetId;
	
	private static final String NAMECOLUMN = new StringBuilder("ifnull(").append(FeedData.FeedColumns.NAME).append(',').append(FeedData.FeedColumns.URL).append(") as title").toString();
	
	public static final String ZERO = "0";
	
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setResult(RESULT_CANCELED);
		
		Bundle extras = getIntent().getExtras();
		
		if (extras != null) {
			widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }
        addPreferencesFromResource(R.layout.widgetpreferences);
        setContentView(R.layout.widgetconfig);
        
        final ListPreference entryCountPreference = (ListPreference) getPreferenceScreen().getPreference(0);
        
        final PreferenceCategory feedsPreferenceCategory = (PreferenceCategory) getPreferenceScreen().getPreference(1);
        
		
		Cursor cursor = this.getContentResolver().query(FeedData.FeedColumns.CONTENT_URI, new String[] {FeedData.FeedColumns._ID, NAMECOLUMN}, null, null, null);
		
		if (cursor.moveToFirst()) {
			int[] ids = new int[cursor.getCount()+1];
			
			CheckBoxPreference checkboxPreference = new CheckBoxPreference(this);
			
			checkboxPreference.setTitle(R.string.all_feeds);
			feedsPreferenceCategory.addPreference(checkboxPreference);
			checkboxPreference.setKey(ZERO);
			checkboxPreference.setDisableDependentsState(true);
			ids[0] = 0;
			for (int n = 1; !cursor.isAfterLast(); cursor.moveToNext(), n++) {
				checkboxPreference = new CheckBoxPreference(this);
				checkboxPreference.setTitle(cursor.getString(1));
				ids[n] = cursor.getInt(0);
				checkboxPreference.setKey(Integer.toString(ids[n]));
				feedsPreferenceCategory.addPreference(checkboxPreference);
				checkboxPreference.setDependency(ZERO);
			}
			cursor.close();			
			
			findViewById(R.id.save_button).setOnClickListener(new OnClickListener() {
				public void onClick(View view) {
					SharedPreferences.Editor preferences = getSharedPreferences(SparseRSSAppWidgetProvider.class.getName(), 0).edit();
					
					boolean hideRead = false;//((CheckBoxPreference) getPreferenceManager().findPreference("widget.hideread")).isChecked();
					
					preferences.putBoolean(widgetId+".hideread", hideRead);
					
					StringBuilder builder = new StringBuilder();
					
					for (int n = 0, i = feedsPreferenceCategory.getPreferenceCount(); n < i; n++) {
						CheckBoxPreference preference = (CheckBoxPreference) feedsPreferenceCategory.getPreference(n);
						
						if (preference.isChecked()) {
							if (n == 0) {
								break;
							} else {
								if (builder.length() > 0) {
									builder.append(',');
								}
								builder.append(preference.getKey());
							}
						}
					}
					
					String feedIds = builder.toString();
					
					String entryCount = entryCountPreference.getValue();
					
					preferences.putString(widgetId+".feeds", feedIds);
					preferences.putString(widgetId+".entrycount", entryCount);
					preferences.commit();
					
					SparseRSSAppWidgetProvider.updateAppWidget(WidgetConfigActivity.this, widgetId, hideRead, entryCount, feedIds);
					setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId));
					finish();
				}
			});
		} else {
			// no feeds found --> use all feeds, no dialog needed
			cursor.close();
			setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId));
		}
	}
	
	
}
