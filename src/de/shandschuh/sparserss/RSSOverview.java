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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import de.shandschuh.sparserss.provider.FeedData;
import de.shandschuh.sparserss.provider.FeedDataContentProvider;
import de.shandschuh.sparserss.service.RefreshService;

public class RSSOverview extends ListActivity {	
	private static final String HTTP = "http://";
	
	private static final String HTTPS = "https://";
	
	private static final int MENU_ADDFEED_ID = 1;
	
	private static final int MENU_REFRESH_ID = 2;
	
	private static final int DIALOG_ADDFEED_ID = 1;
	
	private static final int DIALOG_ERROR_FEEDURLEXISTS = 2;
	
	private static final int CONTEXTMENU_EDIT_ID = 3;
	
	private static final int CONTEXTMENU_REFRESH_ID = 4;
	
	private static final int CONTEXTMENU_DELETE_ID = 5;
	
	private static final int CONTEXTMENU_MARKASREAD_ID = 6;
	
	private static final int MENU_SETTINGS_ID = 7;
	
	private static final int MENU_ALLREAD = 8;
	
	private static final int MENU_ABOUT_ID = 9;
	
	private static final int ACTIVITY_APPLICATIONPREFERENCES_ID = 1;
	
	private boolean serviceConnected = false;
	
	static NotificationManager notificationManager; // package scope
	
	private ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			serviceConnected = true;
		}

		public void onServiceDisconnected(ComponentName name) {
			serviceConnected = false;
		}
	};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (notificationManager == null) {
        	notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        setContentView(R.layout.main);
        setListAdapter(new RSSOverviewListAdapter(this));
        getListView().setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
				menu.setHeaderTitle(((TextView) ((AdapterView.AdapterContextMenuInfo) menuInfo).targetView.findViewById(android.R.id.text1)).getText());
				menu.add(0, CONTEXTMENU_EDIT_ID, Menu.NONE, R.string.contextmenu_edit);
				menu.add(0, CONTEXTMENU_REFRESH_ID, Menu.NONE, R.string.contextmenu_refresh);
				menu.add(0, CONTEXTMENU_DELETE_ID, Menu.NONE, R.string.contextmenu_delete);
				menu.add(0, CONTEXTMENU_MARKASREAD_ID, Menu.NONE, R.string.contextmenu_markasread);
			}
        });
        if (!serviceConnected && PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Strings.SETTINGS_REFRESHENABLED, false)) {
        	startService(new Intent(this, RefreshService.class)); // starts the service independent to this activity
        	bindService(new Intent(this, RefreshService.class), serviceConnection, BIND_AUTO_CREATE);
        	serviceConnected = true;
        } 
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Strings.SETTINGS_REFRESHONPENENABLED, false)) {
        	sendBroadcast(new Intent(Strings.ACTION_REFRESHFEEDS));
        }
        
        if (!FeedDataContentProvider.USE_SDCARD) {
        	Button button = (Button) findViewById(R.id.reload_button);
        	
        	button.setOnClickListener(new OnClickListener() {
    			public void onClick(View view) {
    				sendBroadcast(new Intent(Strings.ACTION_RESTART));
    			}
            });
        	button.setVisibility(View.VISIBLE);
        }
    }
    
	@Override
	protected void onResume() {
		super.onResume();
		notificationManager.cancel(0);
	}

	@Override
	protected void onDestroy() {
		if (serviceConnected) {
			unbindService(serviceConnection);
			serviceConnected = false;
		}
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_ADDFEED_ID, Menu.NONE, R.string.menu_addfeed).setIcon(android.R.drawable.ic_menu_add);
		menu.add(0, MENU_REFRESH_ID, Menu.NONE, R.string.menu_refresh).setIcon(android.R.drawable.ic_menu_rotate);
		menu.add(0, MENU_SETTINGS_ID, Menu.NONE, R.string.menu_settings).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, MENU_ALLREAD, Menu.NONE, R.string.menu_allread).setIcon(android.R.drawable.ic_menu_revert);
		menu.add(0, MENU_ABOUT_ID, Menu.NONE, R.string.menu_about).setIcon(android.R.drawable.ic_menu_info_details);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, final MenuItem item) {
		switch (item.getItemId()) {
			case MENU_ADDFEED_ID: {
				showDialog(DIALOG_ADDFEED_ID);
				break;
			}
			case MENU_REFRESH_ID: {
				sendBroadcast(new Intent(Strings.ACTION_REFRESHFEEDS));
				break;
			}
			case CONTEXTMENU_EDIT_ID: {
				String id = Long.toString(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id);
				
				Cursor cursor = getContentResolver().query(FeedData.FeedColumns.CONTENT_URI(id), new String[] {FeedData.FeedColumns.NAME, FeedData.FeedColumns.URL}, null, null, null);
				
				cursor.moveToFirst();
				createURLDialog(cursor.getString(0), cursor.getString(1), id).show();
				cursor.close();
				break;
			}
			case CONTEXTMENU_REFRESH_ID: {
				sendBroadcast(new Intent(Strings.ACTION_REFRESHFEEDS).putExtra(Strings.FEEDID, Long.toString(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id)));
				break;
			}
			case CONTEXTMENU_DELETE_ID: {
				String id = Long.toString(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id);
				
				Cursor cursor = getContentResolver().query(FeedData.FeedColumns.CONTENT_URI(id), new String[] {FeedData.FeedColumns.NAME}, null, null, null);
				
				cursor.moveToFirst();
				
				Builder builder = new AlertDialog.Builder(this);
				
				builder.setIcon(android.R.drawable.ic_dialog_alert);
				builder.setTitle(cursor.getString(0));
				builder.setMessage(R.string.question_deletefeed);
				builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int which) {
		            	new Thread() {
		            		public void run() {
		            			getContentResolver().delete(FeedData.FeedColumns.CONTENT_URI(Long.toString(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id)), null, null);
								sendBroadcast(new Intent(Strings.ACTION_UPDATEWIDGET));
		            		}
		            	}.start();
		            }

		        });
				builder.setNegativeButton(android.R.string.no, null);
				cursor.close();
				builder.show();
				break;
			}
			case CONTEXTMENU_MARKASREAD_ID: {
				new Thread() {
					public void run() {
						getContentResolver().update(FeedData.EntryColumns.CONTENT_URI(Long.toString(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id)), getReadContentValues(), new StringBuilder(FeedData.EntryColumns.READDATE).append(Strings.DB_ISNULL).toString(), null);
					}
				}.start();
				break;
			}
			case MENU_SETTINGS_ID: {
				startActivityForResult(new Intent(this, ApplicationPreferencesActivity.class), ACTIVITY_APPLICATIONPREFERENCES_ID);
				break;
			}
			case MENU_ALLREAD: {
				new Thread() {
					public void run() {
						if (getContentResolver().update(FeedData.EntryColumns.CONTENT_URI, getReadContentValues(), new StringBuilder(FeedData.EntryColumns.READDATE).append(Strings.DB_ISNULL).toString(), null) > 0) {
							getContentResolver().notifyChange(FeedData.FeedColumns.CONTENT_URI, null);
						}
					}
				}.start();
				break;
			}
			case MENU_ABOUT_ID: {
				startActivity(new Intent(this, AboutActivity.class));
				break;
			}
		}
		return true;
	}
	
	public static final ContentValues getReadContentValues() {
		ContentValues values = new ContentValues();
		
		values.put(FeedData.EntryColumns.READDATE, System.currentTimeMillis());
		return values;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ACTIVITY_APPLICATIONPREFERENCES_ID) {
			if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Strings.SETTINGS_REFRESHENABLED, false)) {
				if (!serviceConnected) {
					bindService(new Intent(this, RefreshService.class), serviceConnection, BIND_AUTO_CREATE);
					serviceConnected = true;
				}
			} else if (serviceConnected) {
				unbindService(serviceConnection);
				serviceConnected = false;
			}
		}
	}

	@Override
	protected void onListItemClick(ListView listView, View view, int position, long id) {
		Intent intent = new Intent(Intent.ACTION_VIEW, FeedData.EntryColumns.CONTENT_URI(Long.toString(id)));
		
		intent.putExtra(FeedData.FeedColumns.NAME, ((TextView) view.findViewById(android.R.id.text1)).getText());
		startActivity(intent);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;

		switch (id) {
			case DIALOG_ADDFEED_ID: {
				dialog = createURLDialog(null, null, null);
				break;
			}
			case DIALOG_ERROR_FEEDURLEXISTS: {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				
				builder.setMessage(R.string.error_feedurlexists);
				builder.setTitle(R.string.error);
				builder.setIcon(android.R.drawable.ic_dialog_alert);
				builder.setPositiveButton(android.R.string.ok, null);
				dialog = builder.create();
				break;
			}
			default: dialog = null;
		}
		return dialog;
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		if (id == DIALOG_ADDFEED_ID) {
			EditText editText = (EditText) dialog.findViewById(R.id.feed_url);
			
			editText.setText("");
			((EditText) dialog.findViewById(R.id.feed_title)).setText("");
		}
		super.onPrepareDialog(id, dialog);
	}

	private Dialog createURLDialog(String title, String url, final String id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		View view = getLayoutInflater().inflate(R.layout.feedsettings, null);
		
		final EditText nameEditText = (EditText) view.findViewById(R.id.feed_title);
		
		final EditText urlEditText = (EditText) view.findViewById(R.id.feed_url);
		
		if (title != null) {
			builder.setTitle(title);
			nameEditText.setText(title);
		} else {
			builder.setTitle(R.string.editfeed_title);
		}
		if (url != null) { // indicates an edit
			urlEditText.setText(url);
			urlEditText.setSelection(url.length()-1);
			builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					ContentValues values = new ContentValues();
						
					values.put(FeedData.FeedColumns.URL, urlEditText.getText().toString());
					
					String name = nameEditText.getText().toString();
					
					values.put(FeedData.FeedColumns.NAME, name.trim().length() > 0 ? name : null);
					getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id), values, null, null);
				}
			});
		} else {
			builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					String url = urlEditText.getText().toString();
					
					Cursor cursor = getContentResolver().query(FeedData.FeedColumns.CONTENT_URI, null, new StringBuilder(FeedData.FeedColumns.URL).append(Strings.DB_ARG).toString(), new String[] {url}, null);
					
					if (cursor.getCount() > 0) {
						showDialog(DIALOG_ERROR_FEEDURLEXISTS);
					} else {
						ContentValues values = new ContentValues();
						
						if (!url.startsWith(HTTP) && !url.startsWith(HTTPS)) {
							url = HTTP+url;
						}
						values.put(FeedData.FeedColumns.URL, url);
						values.put(FeedData.FeedColumns.ERROR, (String) null);
						
						String name = nameEditText.getText().toString();
						
						if (name.trim().length() > 0) {
							values.put(FeedData.FeedColumns.NAME, name);
						}
						getContentResolver().insert(FeedData.FeedColumns.CONTENT_URI, values);
					}
					cursor.close();
				}
			});
		}
		builder.setView(view); 
		
		
		builder.setNegativeButton(android.R.string.cancel, null);
		return builder.create();
	}
    
}
