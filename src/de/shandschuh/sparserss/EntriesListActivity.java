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

package de.shandschuh.sparserss;

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import de.shandschuh.sparserss.provider.FeedData;

public class EntriesListActivity extends ListActivity {
	private Uri uri;
	
	private EntriesListAdapter entriesListAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		uri = getIntent().getData();
		entriesListAdapter = new EntriesListAdapter(this, uri);
        setListAdapter(entriesListAdapter);
        if (getIntent().hasExtra(FeedData.FeedColumns.NAME)) {
        	setTitle(getIntent().getStringExtra(FeedData.FeedColumns.NAME));
        }
        RSSOverview.notificationManager.cancel(0);
	}

	@Override
	protected void onListItemClick(ListView listView, View view, int position, long id) {
		startActivity(new Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(uri, id)));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, Menu.NONE, R.string.contextmenu_markasread).setIcon(android.R.drawable.ic_menu_revert);
		menu.add(1, 1, Menu.NONE, R.string.contextmenu_hideread).setCheckable(true).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.setGroupVisible(0, entriesListAdapter.getCount() > 0);
		return super.onPrepareOptionsMenu(menu);
	}

	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getItemId() == 0) {
			new Thread() { // the update process takes some time
				public void run() {
					getContentResolver().update(uri, RSSOverview.getReadContentValues(), null, null);
				}
			}.start();
		} else {
			if (item.isChecked()) {
				item.setChecked(false).setTitle(R.string.contextmenu_hideread).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
				entriesListAdapter.showRead(true);
			} else {
				item.setChecked(true).setTitle(R.string.contextmenu_showread).setIcon(android.R.drawable.ic_menu_view);
				entriesListAdapter.showRead(false);
			}
		}
		return true;
	}
	
}
