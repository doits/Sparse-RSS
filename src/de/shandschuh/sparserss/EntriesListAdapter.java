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
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import de.shandschuh.sparserss.provider.FeedData;

public class EntriesListAdapter extends ResourceCursorAdapter {
	public static DateFormat DATEFORMAT = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
	
	private int titleColumnPosition;
	
	private int dateColumn;
	
	private int readDateColumn;
	
	private int favoriteColumn;
	
	private int idColumn;
	
	private int feedIconColumn;
	
	private int feedNameColumn;
	
	private static final String SQLREAD = "length(readdate) ASC, ";
	
	public static final String READDATEISNULL = "readdate is null";
	
	private boolean showRead;
	
	private Activity context;
	
	private Uri uri;
	
	private boolean showFeedInfo;
	
	public EntriesListAdapter(Activity context, Uri uri, boolean showFeedInfo) {
		super(context, R.layout.listitem, createManagedCursor(context, uri, true));
		showRead = true;
		this.context = context;
		this.uri = uri;
		titleColumnPosition = getCursor().getColumnIndex(FeedData.EntryColumns.TITLE);
		dateColumn = getCursor().getColumnIndex(FeedData.EntryColumns.DATE);
		readDateColumn = getCursor().getColumnIndex(FeedData.EntryColumns.READDATE);
		favoriteColumn = getCursor().getColumnIndex(FeedData.EntryColumns.FAVORITE);
		idColumn = getCursor().getColumnIndex(FeedData.EntryColumns._ID);
		this.showFeedInfo = showFeedInfo;
		if (showFeedInfo) {
			feedIconColumn = getCursor().getColumnIndex(FeedData.FeedColumns.ICON);
			feedNameColumn = getCursor().getColumnIndex(FeedData.FeedColumns.NAME);
		}
	}

	@Override
	public void bindView(View view, final Context context, Cursor cursor) {
		TextView textView = (TextView) view.findViewById(android.R.id.text1);
		
		textView.setText(cursor.getString(titleColumnPosition));
		
		TextView dateTextView = (TextView) view.findViewById(android.R.id.text2);
		
		ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
		 
		final boolean favorite = cursor.getInt(favoriteColumn) == 1;
		
		final String id = cursor.getString(idColumn);
		
		imageView.setImageResource(favorite ? android.R.drawable.star_on : android.R.drawable.star_off);
		imageView.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				ContentValues values = new ContentValues();
				
				values.put(FeedData.EntryColumns.FAVORITE, favorite ? 0 : 1);
				view.getContext().getContentResolver().update(uri, values, new StringBuilder(FeedData.EntryColumns._ID).append(Strings.DB_ARG).toString(), new String[] {id});
				context.getContentResolver().notifyChange(FeedData.EntryColumns.FAVORITES_CONTENT_URI, null);
			}
		});
		if (cursor.isNull(readDateColumn)) {
			textView.setTypeface(Typeface.DEFAULT_BOLD);
			textView.setEnabled(true);
			dateTextView.setEnabled(true);
		} else {
			textView.setTypeface(Typeface.DEFAULT);
			textView.setEnabled(false);
			dateTextView.setEnabled(false);
		}
		if (showFeedInfo) {
			byte[] iconBytes = cursor.getBlob(feedIconColumn);
			
			if (iconBytes != null && iconBytes.length > 0) {
				dateTextView.setText(" "+DATEFORMAT.format(new Date(cursor.getLong(dateColumn)))+", "+cursor.getString(feedNameColumn)); // bad style
				dateTextView.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length)), null, null,  null);
			} else {
				dateTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
				dateTextView.setText(DATEFORMAT.format(new Date(cursor.getLong(dateColumn)))+", "+cursor.getString(feedNameColumn));
			}
			
		} else {
			textView.setText(cursor.getString(titleColumnPosition));
			dateTextView.setText(DateFormat.getDateTimeInstance().format(new Date(cursor.getLong(dateColumn))));
		}
	}

	public void showRead(boolean showRead) {
		if (showRead != this.showRead) {
			changeCursor(createManagedCursor(context, uri, showRead));
			this.showRead = showRead;
		}
	}
	
	public boolean isShowRead() {
		return showRead;
	}
	
	private static Cursor createManagedCursor(Activity context, Uri uri, boolean showRead) {
		return context.managedQuery(uri, null, showRead ? null : READDATEISNULL, null, new StringBuilder(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Strings.SETTINGS_PRIORITIZE, false) ? SQLREAD : Strings.EMPTY).append(FeedData.EntryColumns.DATE).append(Strings.DB_DESC).toString());
	}
}
