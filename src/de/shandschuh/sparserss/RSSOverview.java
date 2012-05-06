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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import de.shandschuh.sparserss.provider.FeedData;
import de.shandschuh.sparserss.provider.OPML;
import de.shandschuh.sparserss.service.RefreshService;

public class RSSOverview extends ListActivity {
	private static final int DIALOG_ERROR_FEEDIMPORT = 3;
	
	private static final int DIALOG_ERROR_FEEDEXPORT = 4;
	
	private static final int DIALOG_ERROR_INVALIDIMPORTFILE = 5;
	
	private static final int DIALOG_ERROR_EXTERNALSTORAGENOTAVAILABLE = 6;
	
	private static final int DIALOG_ABOUT = 7;
	
	private static final int CONTEXTMENU_EDIT_ID = 3;
	
	private static final int CONTEXTMENU_REFRESH_ID = 4;
	
	private static final int CONTEXTMENU_DELETE_ID = 5;
	
	private static final int CONTEXTMENU_MARKASREAD_ID = 6;
	
	private static final int CONTEXTMENU_MARKASUNREAD_ID = 7;
	
	private static final int CONTEXTMENU_DELETEREAD_ID = 8;
	
	private static final int CONTEXTMENU_DELETEALLENTRIES_ID = 9;
	
	private static final int CONTEXTMENU_RESETUPDATEDATE_ID = 10;
	
	private static final int ACTIVITY_APPLICATIONPREFERENCES_ID = 1;
	
	private static final Uri CANGELOG_URI = Uri.parse("http://code.google.com/p/sparserss/wiki/Changelog");
	
	static NotificationManager notificationManager; // package scope
	
	boolean feedSort;
	
	private RSSOverviewListAdapter listAdapter;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	if (MainTabActivity.isLightTheme(this)) {
    		setTheme(R.style.Theme_Light);
    	}
        super.onCreate(savedInstanceState);

    	if (notificationManager == null) {
        	notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        setContentView(R.layout.main);
        listAdapter = new RSSOverviewListAdapter(this);
        setListAdapter(listAdapter);
        getListView().setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
				menu.setHeaderTitle(((TextView) ((AdapterView.AdapterContextMenuInfo) menuInfo).targetView.findViewById(android.R.id.text1)).getText());
				menu.add(0, CONTEXTMENU_REFRESH_ID, Menu.NONE, R.string.contextmenu_refresh);
				menu.add(0, CONTEXTMENU_MARKASREAD_ID, Menu.NONE, R.string.contextmenu_markasread);
				menu.add(0, CONTEXTMENU_MARKASUNREAD_ID, Menu.NONE, R.string.contextmenu_markasunread);
				menu.add(0, CONTEXTMENU_DELETEREAD_ID, Menu.NONE, R.string.contextmenu_deleteread);
				menu.add(0, CONTEXTMENU_DELETEALLENTRIES_ID, Menu.NONE, R.string.contextmenu_deleteallentries);
				menu.add(0, CONTEXTMENU_EDIT_ID, Menu.NONE, R.string.contextmenu_edit);
				menu.add(0, CONTEXTMENU_RESETUPDATEDATE_ID, Menu.NONE, R.string.contextmenu_resetupdatedate);
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
										View sortView = item.findViewById(R.id.sortitem);
										
										if (sortView.getLeft() <= event.getX()) {
											item.setDrawingCacheEnabled(true);
											dragedView.setImageBitmap(Bitmap.createBitmap(item.getDrawingCache()));
											
											layoutParams = new LayoutParams();
											layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
											layoutParams.gravity = Gravity.TOP;
											layoutParams.y = (int) event.getY();
											windowManager.addView(dragedView, layoutParams);
										} else {
											dragedItem = -1;
											return false; // do not comsume
										}
										
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
								return true;
							} else {
								return false;
							}
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
        	new Thread() {
				public void run() {
					sendBroadcast(new Intent(Strings.ACTION_REFRESHFEEDS));
				}
        	}.start();
        }
    }
    
	@Override
	protected void onResume() {
		super.onResume();
		if (RSSOverview.notificationManager != null) {
			notificationManager.cancel(0);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.feedoverview, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.setGroupVisible(R.id.menu_group_0, !feedSort);
		menu.setGroupVisible(R.id.menu_group_1, feedSort);
		return true;
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onMenuItemSelected(int featureId, final MenuItem item) {
		setFeedSortEnabled(false);
		switch (item.getItemId()) {
			case R.id.menu_addfeed: {
				startActivity(new Intent(Intent.ACTION_INSERT).setData(FeedData.FeedColumns.CONTENT_URI));
				break;
			}
			case R.id.menu_refresh: {
				new Thread() {
					public void run() {
						sendBroadcast(new Intent(Strings.ACTION_REFRESHFEEDS).putExtra(Strings.SETTINGS_OVERRIDEWIFIONLY, PreferenceManager.getDefaultSharedPreferences(RSSOverview.this).getBoolean(Strings.SETTINGS_OVERRIDEWIFIONLY, false)));
					}
				}.start();
				break;
			}
			case CONTEXTMENU_EDIT_ID: {
				startActivity(new Intent(Intent.ACTION_EDIT).setData(FeedData.FeedColumns.CONTENT_URI(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id)));
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
			case CONTEXTMENU_RESETUPDATEDATE_ID: {
				ContentValues values = new ContentValues();
				
				values.put(FeedData.FeedColumns.LASTUPDATE, 0);
				values.put(FeedData.FeedColumns.REALLASTUPDATE, 0);
				getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(Long.toString(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id)), values, null, null);
				break;
			}
				
			case R.id.menu_settings: {
				startActivityForResult(new Intent(this, ApplicationPreferencesActivity.class), ACTIVITY_APPLICATIONPREFERENCES_ID);
				break;
			}
			case R.id.menu_allread: {
				new Thread() {
					public void run() {
						if (getContentResolver().update(FeedData.EntryColumns.CONTENT_URI, getReadContentValues(), new StringBuilder(FeedData.EntryColumns.READDATE).append(Strings.DB_ISNULL).toString(), null) > 0) {
							getContentResolver().notifyChange(FeedData.FeedColumns.CONTENT_URI, null);
						}
					}
				}.start();
				break;
			}
			case R.id.menu_about: {
				showDialog(DIALOG_ABOUT);
				break;
			}
			case R.id.menu_import: {
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
			case R.id.menu_export: {
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
			case R.id.menu_enablefeedsort: {
				setFeedSortEnabled(true);
				break;
			}
			case R.id.menu_deleteread: {
				FeedData.deletePicturesOfFeedAsync(this, FeedData.EntryColumns.CONTENT_URI, Strings.READDATE_GREATERZERO);
				getContentResolver().delete(FeedData.EntryColumns.CONTENT_URI, Strings.READDATE_GREATERZERO, null);
				((RSSOverviewListAdapter) getListAdapter()).notifyDataSetChanged();
				break;
			}
			case R.id.menu_deleteallentries: {
				showDeleteAllEntriesQuestion(this, FeedData.EntryColumns.CONTENT_URI);
				break;
			}
			case R.id.menu_disablefeedsort: {
				// do nothing as the feed sort gets disabled anyway
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
		setFeedSortEnabled(false);
		
		Intent intent = new Intent(Intent.ACTION_VIEW, FeedData.EntryColumns.CONTENT_URI(Long.toString(id)));
		
		intent.putExtra(FeedData.FeedColumns._ID, id);
		startActivity(intent);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;

		switch (id) {
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
	
	private void setFeedSortEnabled(boolean enabled) {
		if (enabled != feedSort) {
			listAdapter.setFeedSortEnabled(enabled);
			feedSort = enabled;
		}
	}
    
}
