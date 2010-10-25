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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Xml;
import de.shandschuh.sparserss.R;
import de.shandschuh.sparserss.RSSOverview;
import de.shandschuh.sparserss.Strings;
import de.shandschuh.sparserss.handler.RSSHandler;
import de.shandschuh.sparserss.provider.FeedData;

public class FetcherService extends Service {
	private static final int FETCHMODE_DIRECT = 1;
	
	private static final int FETCHMODE_REENCODE = 2;
	
	private static final String KEY_USERAGENT = "User-agent";
	
	private static final String VALUE_USERAGENT = "Mozilla/5.0";
	
	private static final String CHARSET = "charset=";
	
	private static final String COUNT = "COUNT(*)";
	
	private static final String CONTENT_TYPE_TEXT_HTML = "text/html";
	
	private static final String LINK_RSS = "<link rel=\"alternate\" ";
	
	private static final String LINK_RSS_SLOPPY = "<link rel=alternate "; 
	
	private static final String HREF = "href=\"";
	
	private static final String HTML_BODY = "<body";
	
	private static final String SLASH = "/";
	
	private static final String ENCODING = "encoding=\"";
	
	private NotificationManager notificationManager;
	
	static {
		HttpURLConnection.setFollowRedirects(true);
	}
	
	@Override
	public void onStart(final Intent intent, int startId) {
		super.onStart(intent, startId);

		ConnectivityManager connectivityManager =  (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		
		if (networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED) {
			new Thread() {
				public void run() {
					int newCount = FetcherService.refreshFeedsStatic(FetcherService.this, intent.getStringExtra(Strings.FEEDID));
					
					if (newCount > 0) {
						SharedPreferences preferences = null;

						try {
							preferences = PreferenceManager.getDefaultSharedPreferences(createPackageContext(Strings.PACKAGE, 0));
						} catch (NameNotFoundException e) {
							preferences = PreferenceManager.getDefaultSharedPreferences(FetcherService.this);
						}
						if (preferences.getBoolean(Strings.SETTINGS_NOTIFICATIONSENABLED, false)) {
							Cursor cursor = getContentResolver().query(FeedData.EntryColumns.CONTENT_URI, new String[] {COUNT}, new StringBuilder(FeedData.EntryColumns.READDATE).append(Strings.DB_ISNULL).toString(), null, null);
							
							cursor.moveToFirst();
							newCount = cursor.getInt(0);
							cursor.close();
							
							String text = new StringBuilder().append(newCount).append(' ').append(getString(R.string.newentries)).toString();
							
							Notification notification = new Notification(R.drawable.ic_statusbar_rss, text, System.currentTimeMillis());
							
							Intent notificationIntent = new Intent(FetcherService.this, RSSOverview.class);
							
							PendingIntent contentIntent = PendingIntent.getActivity(FetcherService.this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

							notification.flags |= Notification.FLAG_AUTO_CANCEL;
							if (preferences.getBoolean(Strings.SETTINGS_NOTIFICATIONSVIBRATE, false)) {
								notification.defaults |= Notification.DEFAULT_VIBRATE;
							}
							
							String ringtone = preferences.getString(Strings.SETTINGS_NOTIFICATIONSRINGTONE, null);
							
							if (ringtone != null && ringtone.length() > 0) {
								notification.sound = Uri.parse(ringtone);
							}
							notification.setLatestEventInfo(FetcherService.this, getString(R.string.rss_feeds), text, contentIntent);
							notificationManager.notify(0, notification);
						} else {
							notificationManager.cancel(0);
						}
					}
					
					stopSelf();
				}
			}.start();
		} else {
			stopSelf();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	private static synchronized int refreshFeedsStatic(Context context, String feedId) {
		Cursor cursor = context.getContentResolver().query(feedId == null ? FeedData.FeedColumns.CONTENT_URI : FeedData.FeedColumns.CONTENT_URI(feedId), null, null, null, null); // no managed query here
		
		int urlPosition = cursor.getColumnIndex(FeedData.FeedColumns.URL);
		
		int idPosition = cursor.getColumnIndex(FeedData.FeedColumns._ID);
		
		int lastUpdatePosition = cursor.getColumnIndex(FeedData.FeedColumns.LASTUPDATE);
		
		int titlePosition = cursor.getColumnIndex(FeedData.FeedColumns.NAME);
		
		int fetchmodePosition = cursor.getColumnIndex(FeedData.FeedColumns.FETCHMODE);
		
//		HttpURLConnection.setFollowRedirects(false);
		
		int result = 0;
				
		while(cursor.moveToNext()) {
			String id = cursor.getString(idPosition);
			
			RSSHandler handler = new RSSHandler(context);
			
			try {
				HttpURLConnection connection = setupConnection(cursor.getString(urlPosition));
				
				String contentType = connection.getContentType();
				
				int fetchMode = cursor.getInt(fetchmodePosition);
				
				handler.init(new Date(cursor.getLong(lastUpdatePosition)), id, cursor.getString(titlePosition));
				
				if (fetchMode == 0) {
					if (contentType != null && contentType.startsWith(CONTENT_TYPE_TEXT_HTML)) {
						BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
						
						String line = null;
						
						int pos = -1;
						
						while ((line = reader.readLine()) != null) {
							connection = null;
							if (line.indexOf(HTML_BODY) > -1) {
								break;
							} else {
								pos = line.indexOf(LINK_RSS);
								
								if (pos == -1) {
									pos = line.indexOf(LINK_RSS_SLOPPY);
								}
								if (pos > -1) {
									int posStart = line.indexOf(HREF, pos);

									if (posStart > -1) {
										String url = line.substring(posStart+6, line.indexOf('"', posStart+10)).replace(RSSHandler.AMP_SG, RSSHandler.AMP);
										
										ContentValues values = new ContentValues();
										
										if (url.startsWith(SLASH)) {
											url = cursor.getString(urlPosition)+url;
										} 
										values.put(FeedData.FeedColumns.URL, url);
										context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id), values, null, null);
										connection = setupConnection(url);
										contentType = connection.getContentType();
										break;
									}
								}
							}
						}
						if (connection == null) { // this indicates a badly configured feed
							connection = setupConnection(cursor.getString(urlPosition));
						}
					} else if (contentType != null) {
						
						int index = contentType.indexOf(CHARSET);
						
						if (index > -1) {
							int index2 = contentType.indexOf(';', index);
							
							
							try {
								Xml.findEncodingByName(index2 > -1 ?contentType.substring(index+8, index2) : contentType.substring(index+8));
								fetchMode = FETCHMODE_DIRECT;
							} catch (UnsupportedEncodingException usee) {
								fetchMode = FETCHMODE_REENCODE;
							}
						} else {
							fetchMode = FETCHMODE_REENCODE;
						}
						
					} else {
						BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
						
						char[] chars = new char[20];
						
						int length = bufferedReader.read(chars);
						
						String xmlDescription = new String(chars, 0, length);
						
						connection.disconnect();
						connection = setupConnection(connection.getURL());
						
						int start = xmlDescription != null ?  xmlDescription.indexOf(ENCODING) : -1;
						
						if (start > -1) {
							try {
								Xml.findEncodingByName(xmlDescription.substring(start+10, xmlDescription.indexOf('"', start+11)));
								fetchMode = FETCHMODE_DIRECT;
							} catch (UnsupportedEncodingException usee) {
								fetchMode = FETCHMODE_REENCODE;
							}
						} else {
							fetchMode = FETCHMODE_DIRECT; // absolutely no encoding information found
						}
					}
					
					ContentValues values = new ContentValues();
					
					values.put(FeedData.FeedColumns.FETCHMODE, fetchMode); 
					context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id), values, null, null);
				}
				
				switch (fetchMode) {
					case FETCHMODE_DIRECT: {
						if (contentType != null) {
							int index = contentType.indexOf(CHARSET);
							
							int index2 = contentType.indexOf(';', index);
							
							Xml.parse(connection.getInputStream(), Xml.findEncodingByName(index2 > -1 ?contentType.substring(index+8, index2) : contentType.substring(index+8)), handler);
						} else {
							Xml.parse(new InputStreamReader(connection.getInputStream()), handler);
						}
						break;
					}
					case FETCHMODE_REENCODE: {
						
						ByteArrayOutputStream ouputStream = new ByteArrayOutputStream();
						
						InputStream inputStream = connection.getInputStream();
						  byte[] byteChunk = new byte[4096]; 
						  int n;

						  while ( (n = inputStream.read(byteChunk)) > 0 ) {
							  ouputStream.write(byteChunk, 0, n);
						  }
						

						
						String xmlText = ouputStream.toString();
						
						int start = xmlText != null ?  xmlText.indexOf(ENCODING) : -1;
						
						if (start > -1) {
							Xml.parse(new StringReader(new String(ouputStream.toByteArray(), xmlText.substring(start+10, xmlText.indexOf('"', start+11)))), handler);
						} else {
							// use content type
							if (contentType != null) {
								
								int index = contentType.indexOf(CHARSET);
								
								if (index > -1) {
									int index2 = contentType.indexOf(';', index);
									
									try {
										Xml.parse(new StringReader(new String(ouputStream.toByteArray(), index2 > -1 ?contentType.substring(index+8, index2) : contentType.substring(index+8))), handler);
									} catch (Exception e) {
										
									}
								}
							}
						}
						break;
					}
				}
				result += handler.getNewCount();
				
			} catch (Throwable e) {
				if (!handler.isDone()) {
					ContentValues values = new ContentValues();
					
					values.put(FeedData.FeedColumns.FETCHMODE, 0); // resets the fetchmode to determine it again later
					values.put(FeedData.FeedColumns.ERROR, e.getMessage());
					context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id), values, null, null);
				}
			}
		}
		cursor.close();
		
		if (result > 0) {
			context.sendBroadcast(new Intent(Strings.ACTION_UPDATEWIDGET));
		}
		return result;
	}
	
	private static final HttpURLConnection setupConnection(String url) throws IOException {
		return setupConnection(new URL(url));
	}
	
	private static final HttpURLConnection setupConnection(URL url) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		
		connection.setDoInput(true);
		connection.setDoOutput(false);
		connection.setRequestProperty(KEY_USERAGENT, VALUE_USERAGENT); // some feeds need this to work properly
		connection.connect();
		return connection;
	}
}
