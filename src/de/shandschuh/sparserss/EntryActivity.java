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

import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import de.shandschuh.sparserss.provider.FeedData;

public class EntryActivity extends Activity {
	private static final String NEWLINE = "\n";
	
	private static final String BR = "<br/>";
	
	private static final String TEXT_HTML = "text/html";
	
	private static final String UTF8 = "utf-8";
	
	private static final String AND_DATE = " and date ";
	
	private static final String ASC = "date asc limit 1";
	
	private static final String DESC = "date desc limit 1";
	
	private int titlePosition;
	
	private int datePosition;
	
	private int abstractPosition;
	
	private int linkPosition;
	
	private int feedIdPosition;
	
	private int favoritePosition;
	
	private int readDatePosition;
	
	private Uri uri;
	
	private int feedId;
	
	boolean favorite;
	
	private boolean showRead;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.entry);
		
		uri = getIntent().getData();
		showRead = getIntent().getBooleanExtra(EntriesListActivity.EXTRA_SHOWREAD, true);
		
		Cursor entryCursor = getContentResolver().query(uri, null, null, null, null);
		
		titlePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.TITLE);
		datePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.DATE);
		abstractPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.ABSTRACT);
		linkPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.LINK);
		feedIdPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.FEED_ID);
		favoritePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.FAVORITE);
		readDatePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.READDATE);
		entryCursor.close();
		if (RSSOverview.notificationManager == null) {
			RSSOverview.notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		RSSOverview.notificationManager.cancel(0);
		uri = getIntent().getData();
		reload();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}

	private void reload() {
		ContentValues values = new ContentValues();
		
		values.put(FeedData.EntryColumns.READDATE, System.currentTimeMillis());
		
		Cursor entryCursor = getContentResolver().query(uri, null, null, null, null);
		
		if (entryCursor.moveToFirst()) {
			String abstractText = entryCursor.getString(abstractPosition);
			
			if (entryCursor.isNull(readDatePosition)) {
				getContentResolver().update(uri, values, new StringBuilder(FeedData.EntryColumns.READDATE).append(Strings.DB_ISNULL).toString(), null);
			}
			if (abstractText == null) {
				String link = entryCursor.getString(linkPosition);
				
				entryCursor.close();
				finish();
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
			} else {
				setTitle(entryCursor.getString(titlePosition));
				feedId = entryCursor.getInt(feedIdPosition);
				
				long date = entryCursor.getLong(datePosition);
				
				((TextView) findViewById(R.id.entry_date)).setText(DateFormat.getDateTimeInstance().format(new Date(date)));
				
				final ImageView imageView = (ImageView) findViewById(android.R.id.icon);
				
				favorite = entryCursor.getInt(favoritePosition) == 1;
				
				imageView.setImageResource(favorite ? android.R.drawable.star_on : android.R.drawable.star_off);
				imageView.setOnClickListener(new OnClickListener() {
					public void onClick(View view) {
						favorite = !favorite;
						imageView.setImageResource(favorite ? android.R.drawable.star_on : android.R.drawable.star_off);
						ContentValues values = new ContentValues();
						
						values.put(FeedData.EntryColumns.FAVORITE, favorite ? 1 : 0);
						getContentResolver().update(uri, values, null, null);
					
					}
				});
				// loadData does not recognize the encoding without correct html-header
				((WebView) findViewById(R.id.entry_abstract)).loadDataWithBaseURL(null, abstractText.indexOf('<') > -1 && abstractText.indexOf('>') > -1 ? abstractText : abstractText.replace(NEWLINE, BR), TEXT_HTML, UTF8, null);
				
				final String link = entryCursor.getString(linkPosition);
				
				((Button) findViewById(R.id.url_button)).setOnClickListener(new OnClickListener() {
					public void onClick(View view) {
						startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
					}
				});
				entryCursor.close();
				setupButton(R.id.prev_button, false, date);
				setupButton(R.id.next_button, true, date);
			}
		} else {
			entryCursor.close();
		}
		/*
		new Thread() {
			public void run() {
				sendBroadcast(new Intent(Strings.ACTION_UPDATEWIDGET)); // this is slow
			}
		}.start();
		*/
	}
	
	private void setupButton(int buttonId, boolean successor, long date) {
		StringBuilder queryString = new StringBuilder(FeedData.EntryColumns.FEED_ID).append('=').append(feedId).append(AND_DATE).append(successor ? '<' : '>').append(date);
		
		if (!showRead) {
			queryString.append(Strings.DB_AND).append(EntriesListAdapter.READDATEISNULL);
		}
		
		Cursor cursor = getContentResolver().query(FeedData.EntryColumns.CONTENT_URI, new String[] {FeedData.EntryColumns._ID}, queryString.toString() , null, successor ? DESC : ASC);
		
		Button button = (Button) findViewById(buttonId);
		
		if (cursor.moveToFirst()) {
			button.setEnabled(true);
			
			final String id = cursor.getString(0);
			button.setOnClickListener(new OnClickListener() {

				public void onClick(View arg0) {
					uri = FeedData.EntryColumns.ENTRY_CONTENT_URI(id);
					getIntent().setData(uri);
					reload();
				}
			});
		} else {
			button.setEnabled(false);
		}
		cursor.close();
	}
}
