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

import java.io.File;
import java.io.FilenameFilter;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import de.shandschuh.sparserss.provider.FeedData;
import de.shandschuh.sparserss.provider.OPML;
import de.shandschuh.sparserss.service.RefreshService;

public class RSSOverview extends ListActivity {	
	private static final int DIALOG_ADDFEED_ID = 1;
	
	private static final int DIALOG_ERROR_FEEDURLEXISTS = 2;
	
	private static final int DIALOG_ERROR_FEEDIMPORT = 3;
	
	private static final int DIALOG_ERROR_FEEDEXPORT = 4;
	
	private static final int DIALOG_ERROR_INVALIDIMPORTFILE = 5;
	
	private static final int DIALOG_ERROR_EXTERNALSTORAGENOTAVAILABLE = 6;
	
	private static final int DIALOG_ABOUT = 7;
	
	private static final int MENU_ADDFEED_ID = 1;
	
	private static final int MENU_REFRESH_ID = 2;
	
	private static final int CONTEXTMENU_EDIT_ID = 3;
	
	private static final int CONTEXTMENU_REFRESH_ID = 4;
	
	private static final int CONTEXTMENU_DELETE_ID = 5;
	
	private static final int CONTEXTMENU_MARKASREAD_ID = 6;
	
	private static final int CONTEXTMENU_MARKASUNREAD_ID = 7;
	
	private static final int CONTEXTMENU_DELETEREAD_ID = 8;
	
	private static final int CONTEXTMENU_DELETEALLENTRIES_ID = 9;
	
	private static final int MENU_SETTINGS_ID = 10;
	
	private static final int MENU_ALLREAD = 11;
	
	private static final int MENU_ABOUT_ID = 12;
	
	private static final int MENU_IMPORT_ID = 13;
	
	private static final int MENU_EXPORT_ID = 14;
	
	private static final int MENU_ENABLEFEEDSORT_ID = 15;
	
	private static final int MENU_DELETEREAD_ID = 16;
	
	private static final int MENU_DELETEALLENTRIES_ID = 17;
	
	private static final int MENU_DISABLEFEEDSORT_ID = 18;
	
	private static final int ACTIVITY_APPLICATIONPREFERENCES_ID = 1;
	
	private static final Uri CANGELOG_URI = Uri.parse("http://code.google.com/p/sparserss/wiki/Changelog");
	
	
	static NotificationManager notificationManager; // package scope
	
	boolean feedSort;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	if (MainTabActivity.isLightTheme(this)) {
    		setTheme(android.R.style.Theme_Light);
    	}
    	
        super.onCreate(savedInstanceState);

    	if (notificationManager == null) {
        	notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        setContentView(R.layout.main);
        setListAdapter(new RSSOverviewListAdapter(this));
        getListView().setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
				menu.setHeaderTitle(((TextView) ((AdapterView.AdapterContextMenuInfo) menuInfo).targetView.findViewById(android.R.id.text1)).getText());
				menu.add(0, CONTEXTMENU_REFRESH_ID, Menu.NONE, R.string.contextmenu_refresh);
				menu.add(0, CONTEXTMENU_MARKASREAD_ID, Menu.NONE, R.string.contextmenu_markasread);
				menu.add(0, CONTEXTMENU_MARKASUNREAD_ID, Menu.NONE, R.string.contextmenu_markasunread);
				menu.add(0, CONTEXTMENU_DELETEREAD_ID, Menu.NONE, R.string.contextmenu_deleteread);
				menu.add(0, CONTEXTMENU_DELETEALLENTRIES_ID, Menu.NONE, R.string.contextmenu_deleteallentries);
				menu.add(0, CONTEXTMENU_EDIT_ID, Menu.NONE, R.string.contextmenu_edit);
				menu.add(0, CONTEXTMENU_DELETE_ID, Menu.NONE, R.string.contextmenu_delete);
			}
        });
        getListView().setOnTouchListener(new OnTouchListener() {
        	private int dragedItem = -1;
        	
        	private ImageView dragedView;
        	
        	private WindowManager windowManager = RSSOverview.this.getWindowManager();
        	
        	private LayoutParams layoutParams;
        	
        	private int minY = 25; // is the header size --> needs to be changed
        	
        	private ListView listView = getListView();
        	
			public boolean onTouch(View v, MotionEvent event) {
				if (feedSort) {
					int action = event.getAction();
					
					switch (action) {
						case MotionEvent.ACTION_DOWN:
						case MotionEvent.ACTION_MOVE: {
							// this is the drag action
							if (dragedItem == -1) {
								dragedItem = listView.pointToPosition((int) event.getX(), (int) event.getY());
								if (dragedItem > -1) {
									dragedView = new ImageView(listView.getContext());
									
									View item = listView.getChildAt(dragedItem - listView.getFirstVisiblePosition());
									
									if (item != null) {
										item.setDrawingCacheEnabled(true);
										dragedView.setImageBitmap(Bitmap.createBitmap(item.getDrawingCache()));
										
										layoutParams = new LayoutParams();
										layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
										layoutParams.gravity = Gravity.TOP;
										layoutParams.y = (int) event.getY();
										windowManager.addView(dragedView, layoutParams);
									} else {
										dragedItem = -1;
									}
								}
							} else if (dragedView != null) {
								layoutParams.y = Math.max(minY, Math.max(0, Math.min((int) event.getY(), listView.getHeight()-minY)));
								windowManager.updateViewLayout(dragedView, layoutParams);
							}
							break;
						}
						case MotionEvent.ACTION_UP: 
						case MotionEvent.ACTION_CANCEL: {
							// this is the drop action
							if (dragedItem > -1) {
								windowManager.removeView(dragedView);

								int newPosition = listView.pointToPosition((int) event.getX(), (int) event.getY());
								
								if (newPosition == -1) {
									newPosition = listView.getCount()-1;
								}
								if (newPosition != dragedItem) {
									ContentValues values = new ContentValues();
									
									values.put(FeedData.FeedColumns.PRIORITY, newPosition);
									getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(listView.getItemIdAtPosition(dragedItem)), values, null, null);
								}
								dragedItem = -1;
							}
							break;
						}
					}
					return true;
				} else {
					return false;
				}
			}
        });
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Strings.SETTINGS_REFRESHENABLED, false)) {
        	startService(new Intent(this, RefreshService.class)); // starts the service independent to this activity
        } else {
        	stopService(new Intent(this, RefreshService.class));
        }
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Strings.SETTINGS_REFRESHONPENENABLED, false)) {
        	sendBroadcast(new Intent(Strings.ACTION_REFRESHFEEDS));
        }
    }
    
	@Override
	protected void onResume() {
		super.onResume();
		notificationManager.cancel(0);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_ADDFEED_ID, Menu.NONE, R.string.menu_addfeed).setIcon(android.R.drawable.ic_menu_add);
		menu.add(0, MENU_REFRESH_ID, Menu.NONE, R.string.menu_refresh).setIcon(android.R.drawable.ic_menu_rotate);
		menu.add(0, MENU_SETTINGS_ID, Menu.NONE, R.string.menu_settings).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, MENU_ALLREAD, Menu.NONE, R.string.menu_allread).setIcon(android.R.drawable.ic_menu_revert);
		menu.add(0, MENU_ABOUT_ID, Menu.NONE, R.string.menu_about).setIcon(android.R.drawable.ic_menu_info_details);
		
		// no icons will be shown from here
		menu.add(0, MENU_IMPORT_ID, Menu.NONE, R.string.menu_import);
		menu.add(0, MENU_EXPORT_ID, Menu.NONE, R.string.menu_export);
		menu.add(0, MENU_ENABLEFEEDSORT_ID, Menu.NONE, R.string.menu_enablefeedsort);
		menu.add(0, MENU_DELETEREAD_ID, Menu.NONE, R.string.contextmenu_deleteread);
		menu.add(0, MENU_DELETEALLENTRIES_ID, Menu.NONE, R.string.contextmenu_deleteallentries);
		
		menu.add(1, MENU_DISABLEFEEDSORT_ID, Menu.NONE, R.string.menu_disablefeedsort).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		
		return true;
	}
	
	

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.setGroupVisible(0, !feedSort);
		menu.setGroupVisible(1, feedSort);
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
				new Thread() {
					public void run() {
						sendBroadcast(new Intent(Strings.ACTION_REFRESHFEEDS).putExtra(Strings.SETTINGS_OVERRIDEWIFIONLY, PreferenceManager.getDefaultSharedPreferences(RSSOverview.this).getBoolean(Strings.SETTINGS_OVERRIDEWIFIONLY, false)));
					}
				}.start();
				break;
			}
			case CONTEXTMENU_EDIT_ID: {
				String id = Long.toString(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id);
				
				Cursor cursor = getContentResolver().query(FeedData.FeedColumns.CONTENT_URI(id), new String[] {FeedData.FeedColumns.NAME, FeedData.FeedColumns.URL, FeedData.FeedColumns.WIFIONLY}, null, null, null);
				
				cursor.moveToFirst();
				createURLDialog(cursor.getString(0), cursor.getString(1), id, cursor.getInt(2) == 1).show();
				cursor.close();
				break;
			}
			case CONTEXTMENU_REFRESH_ID: {
				final String id = Long.toString(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id);

				ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
				
				final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
				
				if (networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED) { // since we have acquired the networkInfo, we use it for basic checks
					final Intent intent = new Intent(Strings.ACTION_REFRESHFEEDS).putExtra(Strings.FEEDID, id);
					
					final Thread thread = new Thread() {
						public void run() {
							sendBroadcast(intent);
						}
					};
					
					if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || PreferenceManager.getDefaultSharedPreferences(RSSOverview.this).getBoolean(Strings.SETTINGS_OVERRIDEWIFIONLY, false)) {
						intent.putExtra(Strings.SETTINGS_OVERRIDEWIFIONLY, true);
						thread.start();
					} else {
						Cursor cursor = getContentResolver().query(FeedData.FeedColumns.CONTENT_URI(id), new String[] {FeedData.FeedColumns.WIFIONLY}, null, null, null);
						
						cursor.moveToFirst();
						
						if (cursor.isNull(0) || cursor.getInt(0) == 0) {
							thread.start();
						} else {
							Builder builder = new AlertDialog.Builder(this);
							
							builder.setIcon(android.R.drawable.ic_dialog_alert);
							builder.setTitle(R.string.dialog_hint);
							builder.setMessage(R.string.question_refreshwowifi);
							builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					            public void onClick(DialogInterface dialog, int which) {
					            	intent.putExtra(Strings.SETTINGS_OVERRIDEWIFIONLY, true);
					            	thread.start();
					            }
					        });
							builder.setNeutralButton(R.string.button_alwaysokforall, new DialogInterface.OnClickListener() {
					            public void onClick(DialogInterface dialog, int which) {
					            	PreferenceManager.getDefaultSharedPreferences(RSSOverview.this).edit().putBoolean(Strings.SETTINGS_OVERRIDEWIFIONLY, true).commit();
					            	intent.putExtra(Strings.SETTINGS_OVERRIDEWIFIONLY, true);
					            	thread.start();
					            }
					        });
							builder.setNegativeButton(android.R.string.no, null);
							builder.show();
						}
						cursor.close();
					}
					
				}
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
						String id = Long.toString(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id);
						
						if (getContentResolver().update(FeedData.EntryColumns.CONTENT_URI(id), getReadContentValues(), new StringBuilder(FeedData.EntryColumns.READDATE).append(Strings.DB_ISNULL).toString(), null) > 0) {
							getContentResolver().notifyChange(FeedData.FeedColumns.CONTENT_URI(id), null);
						}
					}
				}.start();
				break;
			}
			case CONTEXTMENU_MARKASUNREAD_ID: {
				new Thread() {
					public void run() {
						String id = Long.toString(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id);
						
						if (getContentResolver().update(FeedData.EntryColumns.CONTENT_URI(id), getUnreadContentValues(), null, null) > 0) {
							getContentResolver().notifyChange(FeedData.FeedColumns.CONTENT_URI(id), null);;
						}
					}
				}.start();
				break;
			}
			case CONTEXTMENU_DELETEREAD_ID: {
				new Thread() {
					public void run() {
						String id = Long.toString(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id);
						
						Uri uri = FeedData.EntryColumns.CONTENT_URI(id);
						
						String selection = Strings.READDATE_GREATERZERO+Strings.DB_AND+" ("+Strings.DB_EXCUDEFAVORITE+")";
						
						FeedData.deletePicturesOfFeed(RSSOverview.this, uri, selection);
						if (getContentResolver().delete(uri, selection, null) > 0) {
							getContentResolver().notifyChange(FeedData.FeedColumns.CONTENT_URI(id), null);
						}
					}
				}.start();
				break;
			}
			case CONTEXTMENU_DELETEALLENTRIES_ID: {
				showDeleteAllEntriesQuestion(this, FeedData.EntryColumns.CONTENT_URI(Long.toString(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id)));
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
				showDialog(DIALOG_ABOUT);
				break;
			}
			case MENU_IMPORT_ID: {
				if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ||Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
					final AlertDialog.Builder builder = new AlertDialog.Builder(this);
					
					builder.setTitle(R.string.select_file);
					
					try {
						final String[] fileNames = Environment.getExternalStorageDirectory().list(new FilenameFilter() {
							public boolean accept(File dir, String filename) {
								return new File(dir, filename).isFile();
							}
						});
						builder.setItems(fileNames, new DialogInterface.OnClickListener()  {
							public void onClick(DialogInterface dialog, int which) {
								try {
									OPML.importFromFile(new StringBuilder(Environment.getExternalStorageDirectory().toString()).append(File.separator).append(fileNames[which]).toString(), RSSOverview.this);
								} catch (Exception e) {
									showDialog(DIALOG_ERROR_FEEDIMPORT);
								}
							}
						});
						builder.show();
					} catch (Exception e) {
						showDialog(DIALOG_ERROR_FEEDIMPORT);
					}
				} else {
					showDialog(DIALOG_ERROR_EXTERNALSTORAGENOTAVAILABLE);
				}
				
				break;
			}
			case MENU_EXPORT_ID: {
				if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ||Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
					try {
						String filename = new StringBuilder(Environment.getExternalStorageDirectory().toString()).append("/sparse_rss_").append(System.currentTimeMillis()).append(".opml").toString();
						
						OPML.exportToFile(filename, this);
						Toast.makeText(this, String.format(getString(R.string.message_exportedto), filename), Toast.LENGTH_LONG).show();
					} catch (Exception e) {
						showDialog(DIALOG_ERROR_FEEDEXPORT);
					}
				} else {
					showDialog(DIALOG_ERROR_EXTERNALSTORAGENOTAVAILABLE);
				}
				break;
			}
			case MENU_ENABLEFEEDSORT_ID: {
				feedSort = true;
				break;
			}
			case MENU_DELETEREAD_ID: {
				FeedData.deletePicturesOfFeedAsync(this, FeedData.EntryColumns.CONTENT_URI, Strings.READDATE_GREATERZERO);
				getContentResolver().delete(FeedData.EntryColumns.CONTENT_URI, Strings.READDATE_GREATERZERO, null);
				((RSSOverviewListAdapter) getListAdapter()).notifyDataSetChanged();
				break;
			}
			case MENU_DELETEALLENTRIES_ID: {
				showDeleteAllEntriesQuestion(this, FeedData.EntryColumns.CONTENT_URI);
				break;
			}
			case MENU_DISABLEFEEDSORT_ID: {
				feedSort = false;
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
	
	public static final ContentValues getUnreadContentValues() {
		ContentValues values = new ContentValues();
		
		values.putNull(FeedData.EntryColumns.READDATE);
		return values;
	}

	@Override
	protected void onListItemClick(ListView listView, View view, int position, long id) {
		Intent intent = new Intent(Intent.ACTION_VIEW, FeedData.EntryColumns.CONTENT_URI(Long.toString(id)));
		
		intent.putExtra(FeedData.FeedColumns._ID, id);
		startActivity(intent);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;

		switch (id) {
			case DIALOG_ADDFEED_ID: {
				dialog = createURLDialog(null, null, null, false);
				break;
			}
			case DIALOG_ERROR_FEEDURLEXISTS: {
				dialog = createErrorDialog(R.string.error_feedurlexists);
				break;
			}
			case DIALOG_ERROR_FEEDIMPORT: {
				dialog = createErrorDialog(R.string.error_feedimport);
				break;
			}
			case DIALOG_ERROR_FEEDEXPORT: {
				dialog = createErrorDialog(R.string.error_feedexport);
				break;
			}
			case DIALOG_ERROR_INVALIDIMPORTFILE: {
				dialog = createErrorDialog(R.string.error_invalidimportfile);
				break;
			}
			case DIALOG_ERROR_EXTERNALSTORAGENOTAVAILABLE: {
				dialog = createErrorDialog(R.string.error_externalstoragenotavailable);
				break;
			}
			case DIALOG_ABOUT: {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				
				builder.setIcon(android.R.drawable.ic_dialog_info);		
				builder.setTitle(R.string.menu_about);
				MainTabActivity.INSTANCE.setupLicenseText(builder);			
				builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
				builder.setNeutralButton(R.string.changelog, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						startActivity(new Intent(Intent.ACTION_VIEW, CANGELOG_URI));
					}
				});
				return builder.create();
			}
			default: dialog = null;
		}
		return dialog;
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		if (id == DIALOG_ADDFEED_ID) {
			EditText editText = (EditText) dialog.findViewById(R.id.feed_url);
			
			editText.setText(Strings.EMPTY);
			((EditText) dialog.findViewById(R.id.feed_title)).setText(Strings.EMPTY);
		}
		super.onPrepareDialog(id, dialog);
	}

	private Dialog createURLDialog(String title, String url, final String id, boolean refreshOnlyWifi) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		View view = getLayoutInflater().inflate(R.layout.feedsettings, null);
		
		final EditText nameEditText = (EditText) view.findViewById(R.id.feed_title);
		
		final EditText urlEditText = (EditText) view.findViewById(R.id.feed_url);
		
		final CheckBox refreshOnlyWifiCheckBox = (CheckBox) view.findViewById(R.id.wifionlycheckbox);
		
		if (title != null) {
			builder.setTitle(title);
			nameEditText.setText(title);
		} else {
			builder.setTitle(R.string.editfeed_title);
		}
		if (url != null) { // indicates an edit
			urlEditText.setText(url);
			refreshOnlyWifiCheckBox.setChecked(refreshOnlyWifi);
			
			int urlLength = url.length();
			
			if (urlLength > 0) {
				urlEditText.setSelection(urlLength-1);
			}
			builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					String url = urlEditText.getText().toString();
					
					Cursor cursor = getContentResolver().query(FeedData.FeedColumns.CONTENT_URI, new String[] {FeedData.FeedColumns._ID}, new StringBuilder(FeedData.FeedColumns.URL).append(Strings.DB_ARG).toString(), new String[] {url}, null);
					
					if (cursor.moveToFirst() && !id.equals(cursor.getString(0))) {
						showDialog(DIALOG_ERROR_FEEDURLEXISTS);
					} else {
						ContentValues values = new ContentValues();
						
						if (!url.startsWith(Strings.HTTP) && !url.startsWith(Strings.HTTPS)) {
							url = Strings.HTTP+url;
						}
						values.put(FeedData.FeedColumns.URL, url);
						
						String name = nameEditText.getText().toString();
						
						values.put(FeedData.FeedColumns.NAME, name.trim().length() > 0 ? name : null);
						values.put(FeedData.FeedColumns.FETCHMODE, 0);
						values.put(FeedData.FeedColumns.WIFIONLY, refreshOnlyWifiCheckBox.isChecked() ? 1 : 0);
						values.put(FeedData.FeedColumns.ERROR, (String) null);
						getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id), values, null, null);	
					}
					cursor.close();
				}
			});
		} else {
			builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					String url = urlEditText.getText().toString();
					
					if (!url.startsWith(Strings.HTTP) && !url.startsWith(Strings.HTTPS)) {
						url = Strings.HTTP+url;
					}
					
					Cursor cursor = getContentResolver().query(FeedData.FeedColumns.CONTENT_URI, null, new StringBuilder(FeedData.FeedColumns.URL).append(Strings.DB_ARG).toString(), new String[] {url}, null);
					
					if (cursor.moveToFirst()) {
						showDialog(DIALOG_ERROR_FEEDURLEXISTS);
					} else {
						ContentValues values = new ContentValues();
						
						values.put(FeedData.FeedColumns.WIFIONLY, refreshOnlyWifiCheckBox.isChecked() ? 1 : 0);
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
	
	private Dialog createErrorDialog(int messageId) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		builder.setMessage(messageId);
		builder.setTitle(R.string.error);
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setPositiveButton(android.R.string.ok, null);
		return builder.create();
	}
	
	private static void showDeleteAllEntriesQuestion(final Context context, final Uri uri) {
		Builder builder = new AlertDialog.Builder(context);
		
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setTitle(R.string.contextmenu_deleteallentries);
		builder.setMessage(R.string.question_areyousure);
		builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            	new Thread() {
					public void run() {
						FeedData.deletePicturesOfFeed(context, uri, Strings.DB_EXCUDEFAVORITE);
						if (context.getContentResolver().delete(uri, Strings.DB_EXCUDEFAVORITE, null) > 0) {
							context.getContentResolver().notifyChange(FeedData.FeedColumns.CONTENT_URI, null);
						}
					}
				}.start();
            }
        });
		builder.setNegativeButton(android.R.string.no, null);
		builder.show();
	}
    
}
