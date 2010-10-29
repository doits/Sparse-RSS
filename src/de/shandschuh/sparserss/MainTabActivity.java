package de.shandschuh.sparserss;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;
import de.shandschuh.sparserss.provider.FeedData;

public class MainTabActivity extends TabActivity {
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.tabs);

	    TabHost tabHost = getTabHost();  

	    tabHost.addTab(tabHost.newTabSpec("normal").setIndicator(getString(R.string.overview)).setContent(new Intent().setClass(this, RSSOverview.class)));
	    
	    tabHost.addTab(tabHost.newTabSpec("all").setIndicator(getString(R.string.all)).setContent(new Intent(Intent.ACTION_VIEW, FeedData.EntryColumns.CONTENT_URI)));
	    tabHost.addTab(tabHost.newTabSpec("fav").setIndicator(getString(R.string.favorites), getResources().getDrawable(android.R.drawable.star_big_on)).setContent(new Intent(Intent.ACTION_VIEW, FeedData.EntryColumns.FAVORITES_CONTENT_URI)));
	    
	}

}
