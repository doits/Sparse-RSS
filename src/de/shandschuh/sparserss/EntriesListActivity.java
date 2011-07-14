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

import java.text.MessageFormat;

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import de.shandschuh.sparserss.provider.FeedData;

public class EntriesListActivity extends ListActivity {
	private static final int MENU_MARKASREAD_ID = 1;
	
	private static final int MENU_MARKASUNREAD_ID = 2;
	
	private static final int MENU_HIDEREAD_ID = 3;
	
	private static final int CONTEXTMENU_MARKASREAD_ID = 4;
	
	private static final int CONTEXTMENU_MARKASUNREAD_ID = 5;
	
	public static final String EXTRA_SHOWREAD = "show_read";
	
	public static final String EXTRA_SHOWFEEDINFO = "show_feedinfo";

	private Uri uri;
	
	private EntriesListAdapter entriesListAdapter;
	
	private byte[] iconBytes;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		iconBytes = getIntent().getByteArrayExtra(FeedData.FeedColumns.ICON);

		if (iconBytes != null && iconBytes.length > 0) { // we cannot insert the icon here because it would be overwritten, but we have to reserve the icon here
			if (!requestWindowFeature(Window.FEATURE_LEFT_ICON)) {
				iconBytes = null;
			}
		}
        
		setContentView(R.layout.entries);
		uri = getIntent().getData();
		entriesListAdapter = new EntriesListAdapter(this, uri, getIntent().getBooleanExtra(EXTRA_SHOWFEEDINFO, false));
        setListAdapter(entriesListAdapter);
        
        String title = getIntent().getStringExtra(FeedData.FeedColumns.NAME);
        
        if (title != null) {
        	setTitle(title);
        }
        if (iconBytes != null && iconBytes.length > 0) {
        	setFeatureDrawable(Window.FEATURE_LEFT_ICON, new BitmapDrawable(BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length)));
        }
        RSSOverview.notificationManager.cancel(0);
        
        getListView().setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
				menu.setHeaderTitle(((TextView) ((AdapterView.AdapterContextMenuInfo) menuInfo).targetView.findViewById(android.R.id.text1)).getText());
				menu.add(0, CONTEXTMENU_MARKASREAD_ID, Menu.NONE, R.string.contextmenu_markasread).setIcon(android.R.drawable.ic_menu_manage);
				menu.add(0, CONTEXTMENU_MARKASUNREAD_ID, Menu.NONE, R.string.contextmenu_markasunread).setIcon(android.R.drawable.ic_menu_manage);
			}
        });
	}

	@Override
	protected void onListItemClick(ListView listView, View view, int position, long id) {
		startActivity(new Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(uri, id)).putExtra(EXTRA_SHOWREAD, entriesListAdapter.isShowRead()).putExtra(FeedData.FeedColumns.ICON, iconBytes));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_MARKASREAD_ID, Menu.NONE, R.string.contextmenu_markasread).setIcon(android.R.drawable.ic_menu_revert);
		menu.add(0, MENU_MARKASUNREAD_ID, Menu.NONE, MessageFormat.format(getString(R.string.contextmenu_markasunread), entriesListAdapter.getCount())).setIcon(android.R.drawable.ic_menu_set_as);
		menu.add(0, MENU_HIDEREAD_ID, Menu.NONE, R.string.contextmenu_hideread).setCheckable(true).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.setGroupVisible(0, entriesListAdapter.getCount() > 0);
		return super.onPrepareOptionsMenu(menu);
	}

	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
			case MENU_MARKASREAD_ID: {
				new Thread() { // the update process takes some time
					public void run() {
						getContentResolver().update(uri, RSSOverview.getReadContentValues(), null, null);
					}
				}.start();
				break;
			}
			case MENU_MARKASUNREAD_ID: {
				new Thread() { // the update process takes some time
					public void run() {
						getContentResolver().update(uri, RSSOverview.getUnreadContentValues(), null, null);
					}
				}.start();
				break;
			}
			case MENU_HIDEREAD_ID: {
				if (item.isChecked()) {
					item.setChecked(false).setTitle(R.string.contextmenu_hideread).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
					entriesListAdapter.showRead(true);
				} else {
					item.setChecked(true).setTitle(R.string.contextmenu_showread).setIcon(android.R.drawable.ic_menu_view);
					entriesListAdapter.showRead(false);
				}
				break;
			}
			case CONTEXTMENU_MARKASREAD_ID: {
				getContentResolver().update(ContentUris.withAppendedId(uri, ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id), RSSOverview.getReadContentValues(), null, null);
				break;
			}
			case CONTEXTMENU_MARKASUNREAD_ID: {
				getContentResolver().update(ContentUris.withAppendedId(uri, ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id), RSSOverview.getUnreadContentValues(), null, null);
				break;
			}
			
		}
		return true;
	}
	
}
