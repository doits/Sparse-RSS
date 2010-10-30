package de.shandschuh.sparserss;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TabHost;
import de.shandschuh.sparserss.provider.FeedData;

public class MainTabActivity extends TabActivity {
	private boolean tabsAdded;
	
	private static final String TAG_NORMAL = "normal";
	
	private static final String TAG_ALL = "all";
	
	private static final String TAG_FAVORITE = "favorite";
	
	public static MainTabActivity INSTANCE;
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    INSTANCE = this;
	    setContentView(R.layout.tabs);

	    TabHost tabHost = getTabHost();  

	    tabHost.addTab(tabHost.newTabSpec(TAG_NORMAL).setIndicator(getString(R.string.overview)).setContent(new Intent().setClass(this, RSSOverview.class)));
	    
	    if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Strings.SETTINGS_SHOWTABS, false)) {
	    	tabHost.addTab(tabHost.newTabSpec(TAG_ALL).setIndicator(getString(R.string.all)).setContent(new Intent(Intent.ACTION_VIEW, FeedData.EntryColumns.CONTENT_URI)));
		    tabHost.addTab(tabHost.newTabSpec(TAG_FAVORITE).setIndicator(getString(R.string.favorites), getResources().getDrawable(android.R.drawable.star_big_on)).setContent(new Intent(Intent.ACTION_VIEW, FeedData.EntryColumns.FAVORITES_CONTENT_URI)));
		    tabsAdded = true;
		    getTabWidget().setVisibility(View.VISIBLE);
	    }
	}

	public void setTabWidgetVisible(boolean visible) {
		if (visible) {
			if (!tabsAdded) {
				TabHost tabHost = getTabHost();
				
				tabHost.addTab(tabHost.newTabSpec(TAG_ALL).setIndicator(getString(R.string.all)).setContent(new Intent(Intent.ACTION_VIEW, FeedData.EntryColumns.CONTENT_URI)));
			    tabHost.addTab(tabHost.newTabSpec(TAG_FAVORITE).setIndicator(getString(R.string.favorites), getResources().getDrawable(android.R.drawable.star_big_on)).setContent(new Intent(Intent.ACTION_VIEW, FeedData.EntryColumns.FAVORITES_CONTENT_URI)));
				tabsAdded = true;
			}
			getTabWidget().setVisibility(View.VISIBLE);
		} else {
			getTabWidget().setVisibility(View.GONE);
		}
		
	}

}
