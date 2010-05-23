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
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import de.shandschuh.sparserss.provider.FeedData;

public class EntriesListAdapter extends ResourceCursorAdapter {
	private int titleColumnPosition;
	
	private int dateColumn;
	
	private int readDateColumn;
	
	private static final String SQLREAD = "length(readdate) ASC, ";
	
	
	public EntriesListAdapter(Activity context, Uri uri) {
		super(context, R.layout.listitem, context.managedQuery(uri, null, null, null, new StringBuilder(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Strings.SETTINGS_PRIORITIZE, false) ? SQLREAD : Strings.EMPTY).append(FeedData.EntryColumns.DATE).append(Strings.DB_DESC).toString()));
		titleColumnPosition = getCursor().getColumnIndex(FeedData.EntryColumns.TITLE);
		dateColumn = getCursor().getColumnIndex(FeedData.EntryColumns.DATE);
		readDateColumn = getCursor().getColumnIndex(FeedData.EntryColumns.READDATE);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		TextView textView = (TextView) view.findViewById(android.R.id.text1);
		
		textView.setText(cursor.getString(titleColumnPosition));
		
		TextView dateTextView = ((TextView) view.findViewById(android.R.id.text2));
		
		if (cursor.isNull(readDateColumn)) {
			textView.setTypeface(Typeface.DEFAULT_BOLD);
			textView.setEnabled(true);
			dateTextView.setEnabled(true);
		} else {
			textView.setTypeface(Typeface.DEFAULT);
			textView.setEnabled(false);
			dateTextView.setEnabled(false);
		}
		dateTextView.setText(DateFormat.getDateTimeInstance().format(new Date(cursor.getLong(dateColumn))));
	}
}
