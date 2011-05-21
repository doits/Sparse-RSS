/**
 * Sparse rss
 * 
 * Copyright (c) 2010, 2011 Stefan Handschuh
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

package de.shandschuh.sparserss;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import de.shandschuh.sparserss.service.RefreshService;

public class ApplicationPreferencesActivity extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.preferences);
		
		Preference enablePreference = (Preference) findPreference(Strings.SETTINGS_REFRESHENABLED);

		enablePreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (Boolean.TRUE.equals(newValue)) {
					new Thread() {
						public void run() {
							startService(new Intent(ApplicationPreferencesActivity.this, RefreshService.class));
						}
					}.start();
				} else {
					stopService(new Intent(ApplicationPreferencesActivity.this, RefreshService.class));
				}
				return true;
			}
		});
		
		Preference showTabsPreference = (Preference) findPreference(Strings.SETTINGS_SHOWTABS);
		
		showTabsPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (MainTabActivity.INSTANCE != null ) {
					MainTabActivity.INSTANCE.setTabWidgetVisible(Boolean.TRUE.equals(newValue));
					
				}
				return true;
			}
		});
		
		final CheckBoxPreference acceptInvalidSSLCertificatesPreference = (CheckBoxPreference) findPreference(Strings.SETTINGS_ACCEPTINVALIDSSL);
		
		acceptInvalidSSLCertificatesPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (Boolean.TRUE.equals(newValue)) {
					Builder builder = new AlertDialog.Builder(ApplicationPreferencesActivity.this);
					
					builder.setIcon(android.R.drawable.ic_dialog_alert);
					builder.setTitle(android.R.string.dialog_alert_title);
					builder.setMessage(R.string.hint_invalidsslcerts);
					builder.setPositiveButton(android.R.string.yes, new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							Editor editor = acceptInvalidSSLCertificatesPreference.getEditor();
							
							editor.putBoolean(Strings.SETTINGS_ACCEPTINVALIDSSL, true);
							editor.commit();
							acceptInvalidSSLCertificatesPreference.setChecked(true);
						}
					});
					builder.setNegativeButton(android.R.string.no, null);
					builder.show();
					
					return false;
				} else {
					return true;
				}
			}
		});
		
	}
}
