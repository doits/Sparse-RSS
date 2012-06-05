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

package de.shandschuh.sparserss;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import de.shandschuh.sparserss.provider.FeedData;
import de.shandschuh.sparserss.service.FetcherService;

public class MainTabActivity extends TabActivity {
	private static final int DIALOG_LICENSEAGREEMENT = 0;
	
	private boolean tabsAdded;
	
	private static final String TAG_NORMAL = "normal";
	
	private static final String TAG_ALL = "all";
	
	private static final String TAG_FAVORITE = "favorite";
	
	public static MainTabActivity INSTANCE;
	
	public static final boolean POSTGINGERBREAD = !Build.VERSION.RELEASE.startsWith("1") &&
		!Build.VERSION.RELEASE.startsWith("2"); // this way around is future save
	
	
	private static Boolean LIGHTTHEME;
	
	public static boolean isLightTheme(Context context) {
		if (LIGHTTHEME == null) {
			LIGHTTHEME = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Strings.SETTINGS_LIGHTTHEME, false);
		}
		return LIGHTTHEME;
	}
	
	private Menu menu;
	
	private BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			setProgressBarIndeterminateVisibility(true);
		}
	};
	
	private boolean hasContent;
	
	public void onCreate(Bundle savedInstanceState) {
		if (isLightTheme(this)) {
	    	setTheme(R.style.Theme_Light);
	    }
	    super.onCreate(savedInstanceState);
	    
        //We need to display progress information
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
	    setContentView(R.layout.tabs);
	    INSTANCE = this;
	    hasContent = false;
        if (getPreferences(MODE_PRIVATE).getBoolean(Strings.PREFERENCE_LICENSEACCEPTED, false)) {
        	setContent();
        } else {
        	/* Workaround for android issue 4499 on 1.5 devices */
        	getTabHost().addTab(getTabHost().newTabSpec(Strings.EMPTY).setIndicator(Strings.EMPTY).setContent(new Intent(this, EmptyActivity.class)));
        	
        	showDialog(DIALOG_LICENSEAGREEMENT);
        }
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		setProgressBarIndeterminateVisibility(isCurrentlyRefreshing());
		registerReceiver(refreshReceiver, new IntentFilter("de.shandschuh.sparserss.REFRESH"));
	}
	
	@Override
	protected void onPause()
	{
		unregisterReceiver(refreshReceiver);
		super.onPause();
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setTitle(R.string.dialog_licenseagreement);
		builder.setNegativeButton(R.string.button_decline, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
				finish();
			}
		});
		builder.setPositiveButton(R.string.button_accept, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				
				Editor editor = getPreferences(MODE_PRIVATE).edit();
				
				editor.putBoolean(Strings.PREFERENCE_LICENSEACCEPTED, true);
				editor.commit();
				
				/* Part of workaround for android issue 4499 on 1.5 devices */
				getTabHost().clearAllTabs();
				
				/* we only want to invoke actions if the license is accepted */
				setContent();
			}
		});
		setupLicenseText(builder);
		builder.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					dialog.cancel();
					finish();
				}
				return true;
			}
		});
		return builder.create();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		this.menu = menu;

		Activity activity = getCurrentActivity();
		
		if (hasContent && activity != null) {
			return activity.onCreateOptionsMenu(menu);
		} else {
			menu.add(Strings.EMPTY); // to let the menu be available
			return true;
		}
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Activity activity = getCurrentActivity();
		
		if (hasContent && activity != null) {
			return activity.onMenuItemSelected(featureId, item);
		} else {
			return super.onMenuItemSelected(featureId, item);
		}
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Activity activity = getCurrentActivity();
		
		if (hasContent && activity != null) {
			return activity.onPrepareOptionsMenu(menu);
		} else {
			return super.onPrepareOptionsMenu(menu);
		}
	}
	
	private void setContent() {
	    TabHost tabHost = getTabHost();
	    
	    tabHost.addTab(tabHost.newTabSpec(TAG_NORMAL).setIndicator(getString(R.string.overview)).setContent(new Intent().setClass(this, RSSOverview.class)));
	    hasContent = true;
	    if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Strings.SETTINGS_SHOWTABS, false)) {
	    	tabHost.addTab(tabHost.newTabSpec(TAG_ALL).setIndicator(getString(R.string.all)).setContent(new Intent(Intent.ACTION_VIEW, FeedData.EntryColumns.CONTENT_URI).putExtra(EntriesListActivity.EXTRA_SHOWFEEDINFO, true)));
	    	
		    tabHost.addTab(tabHost.newTabSpec(TAG_FAVORITE).setIndicator(getString(R.string.favorites), getResources().getDrawable(android.R.drawable.star_big_on)).setContent(new Intent(Intent.ACTION_VIEW, FeedData.EntryColumns.FAVORITES_CONTENT_URI).putExtra(EntriesListActivity.EXTRA_SHOWFEEDINFO, true).putExtra(EntriesListActivity.EXTRA_AUTORELOAD, true)));
		    tabsAdded = true;
		    getTabWidget().setVisibility(View.VISIBLE);
	    }
	    if (POSTGINGERBREAD) {
		    /* Change the menu also on ICS when tab is changed */
		    tabHost.setOnTabChangedListener(new OnTabChangeListener() {
				public void onTabChanged(String tabId) {
					if (menu != null) {
						menu.clear();
						onCreateOptionsMenu(menu);
					}
				}
		    });
		    if (menu != null) {
				menu.clear();
				onCreateOptionsMenu(menu);
			}
	    }
	}

	public void setTabWidgetVisible(boolean visible) {
		if (visible) {
			if (!tabsAdded) {
				TabHost tabHost = getTabHost();
				
				tabHost.addTab(tabHost.newTabSpec(TAG_ALL).setIndicator(getString(R.string.all)).setContent(new Intent(Intent.ACTION_VIEW, FeedData.EntryColumns.CONTENT_URI).putExtra(EntriesListActivity.EXTRA_SHOWFEEDINFO, true)));
			    tabHost.addTab(tabHost.newTabSpec(TAG_FAVORITE).setIndicator(getString(R.string.favorites), getResources().getDrawable(android.R.drawable.star_big_on)).setContent(new Intent(Intent.ACTION_VIEW, FeedData.EntryColumns.FAVORITES_CONTENT_URI).putExtra(EntriesListActivity.EXTRA_SHOWFEEDINFO, true)));
				tabsAdded = true;
			}
			getTabWidget().setVisibility(View.VISIBLE);
		} else {
			getTabWidget().setVisibility(View.GONE);
		}
		
	}
	
	void setupLicenseText(AlertDialog.Builder builder) {
		View view = getLayoutInflater().inflate(R.layout.license, null);
		
		final TextView textView = (TextView) view.findViewById(R.id.license_text);

		textView.setTextColor(textView.getTextColors().getDefaultColor()); // disables color change on selection
		textView.setText(new StringBuilder(getString(R.string.license_intro)).append(Strings.THREENEWLINES).append(getString(R.string.license)));
		
		final TextView contributorsTextView = (TextView) view.findViewById(R.id.contributors_togglebutton);
		
		contributorsTextView.setOnClickListener(new OnClickListener() {
			boolean showingLicense = true;
			
			@Override
			public void onClick(View view) {
				if (showingLicense) {
					textView.setText(R.string.contributors_list);
					contributorsTextView.setText(R.string.license_word);
				} else {
					textView.setText(new StringBuilder(getString(R.string.license_intro)).append(Strings.THREENEWLINES).append(getString(R.string.license)));
					contributorsTextView.setText(R.string.contributors);
				}
				showingLicense = !showingLicense;
			}
			
		});
		builder.setView(view);
	}
	
	private boolean isCurrentlyRefreshing()
	{
		ActivityManager manager = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service: manager.getRunningServices(Integer.MAX_VALUE)) {
			if (FetcherService.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

}
