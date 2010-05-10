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
import android.view.View;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import de.shandschuh.sparserss.provider.FeedData;

public class EntriesListAdapter extends ResourceCursorAdapter {
	private int titleColumnPosition;
	
	private int dateColumn;
	
	private int readDateColumn;
	
	public EntriesListAdapter(Activity context, Uri uri) {
		super(context, android.R.layout.simple_list_item_2, context.managedQuery(uri, null, null, null, new StringBuilder(FeedData.EntryColumns.DATE).append(Strings.DB_DESC).toString()));
		titleColumnPosition = getCursor().getColumnIndex(FeedData.EntryColumns.TITLE);
		dateColumn = getCursor().getColumnIndex(FeedData.EntryColumns.DATE);
		readDateColumn = getCursor().getColumnIndex(FeedData.EntryColumns.READDATE);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		TextView textView = (TextView) view.findViewById(android.R.id.text1);
		
		textView.setText(cursor.getString(titleColumnPosition));
		if (cursor.isNull(readDateColumn)) {
			textView.setTypeface(Typeface.DEFAULT_BOLD);
		} else {
			textView.setTypeface(Typeface.DEFAULT);
		}
		((TextView) view.findViewById(android.R.id.text2)).setText(DateFormat.getDateTimeInstance().format(new Date(cursor.getLong(dateColumn))));
	}
}
