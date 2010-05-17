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
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import de.shandschuh.sparserss.provider.FeedData;

public class EntryActivity extends Activity {
	private static final String NEWLINE = "\n";
	
	private static final String BR = "<br/>";
	
	private static final String TEXT_HTML = "text/html";
	
	private static final String UTF8 = "utf-8";
	
	private Cursor entryCursor;
	
	private int titlePosition;
	
	private int datePosition;
	
	private int abstractPosition;
	
	private int linkPosition;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.entry);
		
		Uri uri = getIntent().getData();
		
		ContentValues values = new ContentValues();
		
		values.put(FeedData.EntryColumns.READDATE, System.currentTimeMillis());
		
		getContentResolver().update(uri, values, new StringBuilder(FeedData.EntryColumns.READDATE).append(Strings.DB_ISNULL).toString(), null);
		entryCursor = managedQuery(uri, null, null, null, null);
		titlePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.TITLE);
		datePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.DATE);
		abstractPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.ABSTRACT);
		linkPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.LINK);
		reload();
	}
	
	private void reload() {
		if (entryCursor.moveToFirst()) {
			String abstractText = entryCursor.getString(abstractPosition);
			
			if (abstractText == null) {
				finish();
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(entryCursor.getString(linkPosition))));
			} else {
				setTitle(entryCursor.getString(titlePosition));
				((TextView) findViewById(R.id.entry_date)).setText(DateFormat.getDateTimeInstance().format(new Date(entryCursor.getLong(datePosition))));
				
				// loadData does not recognize the encoding without correct html-header
				((WebView) findViewById(R.id.entry_abstract)).loadDataWithBaseURL(null, abstractText.indexOf('<') > -1 && abstractText.indexOf('>') > -1 ? abstractText : abstractText.replace(NEWLINE, BR), TEXT_HTML, UTF8, null);
				((Button) findViewById(R.id.url_button)).setOnClickListener(new OnClickListener() {
					public void onClick(View view) {
						startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(entryCursor.getString(linkPosition))));
					}
				});
			}
			
		}
	}
}
