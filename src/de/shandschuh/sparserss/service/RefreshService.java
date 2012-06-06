/**
 * Sparse rss
 * 
 * Copyright (c) 2010 Stefan Handschuh
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

package de.shandschuh.sparserss.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import de.shandschuh.sparserss.Strings;

public class RefreshService extends Service {   
	private static final String SIXTYMINUTES = "3600000";
	
    private OnSharedPreferenceChangeListener listener = new OnSharedPreferenceChangeListener() {
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if (Strings.SETTINGS_REFRESHINTERVAL.equals(key)) {
				restartTimer();
			}
		}
    };

    private Intent refreshBroadcastIntent;
    
	private AlarmManager alarmManager;
	
	private PendingIntent timerIntent;
	
	
	
	private SharedPreferences preferences = null;
	
	@Override
	public IBinder onBind(Intent intent) {
		onRebind(intent);
		return null;
	}
	
	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return true;  // we want to use rebind
	}

	@Override
	public void onCreate() {
		super.onCreate();
		try {
			preferences = PreferenceManager.getDefaultSharedPreferences(createPackageContext(Strings.PACKAGE, 0));
		} catch (NameNotFoundException e) {
			preferences = PreferenceManager.getDefaultSharedPreferences(this);
		}
		
		refreshBroadcastIntent = new Intent(Strings.ACTION_REFRESHFEEDS);
		alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		preferences.registerOnSharedPreferenceChangeListener(listener);
		restartTimer();
	}

	private void restartTimer() {
		if (timerIntent == null) {
			timerIntent = PendingIntent.getBroadcast(this, 0, refreshBroadcastIntent, 0);
		} else {
			alarmManager.cancel(timerIntent);
		}
		
		int time = 3600000;
		
		try {
			time = Math.max(60000, Integer.parseInt(preferences.getString(Strings.SETTINGS_REFRESHINTERVAL, SIXTYMINUTES)));
		} catch (Exception exception) {

		}
		alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 10000, time, timerIntent);
	}

	@Override
	public void onDestroy() {
		if (timerIntent != null) {
			alarmManager.cancel(timerIntent);
		}
		preferences.unregisterOnSharedPreferenceChangeListener(listener);
		super.onDestroy();
	}
}
