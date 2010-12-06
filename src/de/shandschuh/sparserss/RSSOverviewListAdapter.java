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
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import de.shandschuh.sparserss.provider.FeedData;

public class RSSOverviewListAdapter extends ResourceCursorAdapter {
	private static final String COUNT = "COUNT(*) - COUNT(readdate)";
	
	private static final String COLON = ": ";
	
	private static final String COMMA = ", ";
	
	private int nameColumnPosition;
	
	private int lastUpdateColumn;
	
	private int idPosition;
	
	private int linkPosition;
	
	private int errorPosition;
	
	private int iconPosition;
	
	public RSSOverviewListAdapter(Activity context) {
		super(context, R.layout.listitem, context.managedQuery(FeedData.FeedColumns.CONTENT_URI, null, null, null, null));
		nameColumnPosition = getCursor().getColumnIndex(FeedData.FeedColumns.NAME);
		lastUpdateColumn = getCursor().getColumnIndex(FeedData.FeedColumns.LASTUPDATE);
		idPosition = getCursor().getColumnIndex(FeedData.FeedColumns._ID);
		linkPosition = getCursor().getColumnIndex(FeedData.FeedColumns.URL);
		errorPosition = getCursor().getColumnIndex(FeedData.FeedColumns.ERROR);
		iconPosition = getCursor().getColumnIndex(FeedData.FeedColumns.ICON);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		TextView textView = ((TextView) view.findViewById(android.R.id.text1));
		
		Cursor countCursor = context.getContentResolver().query(FeedData.EntryColumns.CONTENT_URI(cursor.getString(idPosition)), new String[] {COUNT}, null, null, null);
		
		countCursor.moveToFirst();
		
		int unreadCount = countCursor.getInt(0);
		
		countCursor.close();
		
		long date = cursor.getLong(lastUpdateColumn);
		
		TextView updateTextView = ((TextView) view.findViewById(android.R.id.text2));;
		
		if (cursor.isNull(errorPosition)) {
			updateTextView.setText(new StringBuilder(context.getString(R.string.update)).append(COLON).append(date == 0 ? context.getString(R.string.never) : new StringBuilder(DateFormat.getDateTimeInstance().format(new Date(date))).append(COMMA).append(unreadCount).append(' ').append(context.getString(R.string.unread))));
		} else {
			updateTextView.setText(new StringBuilder(context.getString(R.string.error)).append(COLON).append(cursor.getString(errorPosition)));
		}
		if (unreadCount > 0) {
			textView.setTypeface(Typeface.DEFAULT_BOLD);
			textView.setEnabled(true);
			updateTextView.setEnabled(true);
		} else {
			textView.setTypeface(Typeface.DEFAULT);
			textView.setEnabled(false);
			updateTextView.setEnabled(false);
		}
		
		byte[] iconBytes = cursor.getBlob(iconPosition);
		
		if (iconBytes != null && iconBytes.length > 0) {
			textView.setText(" " + (cursor.isNull(nameColumnPosition) ? cursor.getString(linkPosition) : cursor.getString(nameColumnPosition)));
			textView.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length)), null, null, null);
			view.setTag(iconBytes);
		} else {
			textView.setText(cursor.isNull(nameColumnPosition) ? cursor.getString(linkPosition) : cursor.getString(nameColumnPosition));
		}
	}
}
