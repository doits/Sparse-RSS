/**
 * Sparse rss
 * 
 * Copyright (c) 2010, 2011 Stefan Handschuh
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
import java.util.Vector;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
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
	private static final int STATE_NEUTRAL = 0;
	
	private static final int STATE_ALLREAD = 1;
	
	private static final int STATE_ALLUNREAD = 2;
	
	public static DateFormat DATEFORMAT = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
	
	private int titleColumnPosition;
	
	private int dateColumn;
	
	private int readDateColumn;
	
	private int favoriteColumn;
	
	private int idColumn;
	
	private int feedIconColumn;
	
	private int feedNameColumn;
	
	private int linkColumn;
	
	private static final String SQLREAD = "length(readdate) ASC, ";
	
	public static final String READDATEISNULL = "readdate is null";
	
	private boolean showRead;
	
	private Activity context;
	
	private Uri uri;
	
	private boolean showFeedInfo;
	
	private int forcedState;
	
	private Vector<Long> markedAsRead;
	
	private Vector<Long> markedAsUnread;
	
	private Vector<Long> favorited;
	
	private Vector<Long> unfavorited;
	
	public EntriesListAdapter(Activity context, Uri uri, boolean showFeedInfo, boolean autoreload) {
		super(context, R.layout.listitem, createManagedCursor(context, uri, true), autoreload);
		showRead = true;
		this.context = context;
		this.uri = uri;
		
		Cursor cursor = getCursor();
		
		titleColumnPosition = cursor.getColumnIndex(FeedData.EntryColumns.TITLE);
		dateColumn = cursor.getColumnIndex(FeedData.EntryColumns.DATE);
		readDateColumn = cursor.getColumnIndex(FeedData.EntryColumns.READDATE);
		favoriteColumn = cursor.getColumnIndex(FeedData.EntryColumns.FAVORITE);
		idColumn = cursor.getColumnIndex(FeedData.EntryColumns._ID);
		linkColumn = cursor.getColumnIndex(FeedData.EntryColumns.LINK);
		this.showFeedInfo = showFeedInfo;
		if (showFeedInfo) {
			feedIconColumn = cursor.getColumnIndex(FeedData.FeedColumns.ICON);
			feedNameColumn = cursor.getColumnIndex(FeedData.FeedColumns.NAME);
		}
		forcedState = STATE_NEUTRAL;
		markedAsRead = new Vector<Long>();
		markedAsUnread = new Vector<Long>();
		favorited = new Vector<Long>();
		unfavorited = new Vector<Long>();
	}

	@Override
	public void bindView(View view, final Context context, Cursor cursor) {
		TextView textView = (TextView) view.findViewById(android.R.id.text1);
		
		textView.setText(cursor.getString(titleColumnPosition));
		
		TextView dateTextView = (TextView) view.findViewById(android.R.id.text2);
		
		final ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
		
		final long id = cursor.getLong(idColumn);
		
		view.setTag(cursor.getString(linkColumn));
		 
		final boolean favorite = !unfavorited.contains(id) && (cursor.getInt(favoriteColumn) == 1 || favorited.contains(id));
		
		imageView.setImageResource(favorite ? android.R.drawable.star_on : android.R.drawable.star_off);
		imageView.setTag(favorite ? Strings.TRUE : Strings.FALSE);
		imageView.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				boolean newFavorite = !Strings.TRUE.equals(view.getTag());
				
				if (newFavorite) {
					view.setTag(Strings.TRUE);
					imageView.setImageResource(android.R.drawable.star_on);
					favorited.add(id);
					unfavorited.remove(id);
				} else {
					view.setTag(Strings.FALSE);
					imageView.setImageResource(android.R.drawable.star_off);
					unfavorited.add(id);
					favorited.remove(id);
				}
				
				ContentValues values = new ContentValues();
				
				values.put(FeedData.EntryColumns.FAVORITE, newFavorite ? 1 : 0);
				view.getContext().getContentResolver().update(uri, values, new StringBuilder(FeedData.EntryColumns._ID).append(Strings.DB_ARG).toString(), new String[] {Long.toString(id)});
				context.getContentResolver().notifyChange(FeedData.EntryColumns.FAVORITES_CONTENT_URI, null);
				
			}
		});
		
		if (showFeedInfo && feedIconColumn > -1 && feedNameColumn > -1) {
			byte[] iconBytes = cursor.getBlob(feedIconColumn);
			
			if (iconBytes != null && iconBytes.length > 0) {
				Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
				
				if (bitmap.getHeight() > 16) {
					bitmap = Bitmap.createScaledBitmap(bitmap, 16, 16, false);
				}
				textView.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(bitmap), null, null, null);
				
				dateTextView.setText(" "+DATEFORMAT.format(new Date(cursor.getLong(dateColumn)))+", "+cursor.getString(feedNameColumn)); // bad style
				dateTextView.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(bitmap), null, null,  null);
			} else {
				dateTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
				dateTextView.setText(DATEFORMAT.format(new Date(cursor.getLong(dateColumn)))+", "+cursor.getString(feedNameColumn));
			}
			
		} else {
			textView.setText(cursor.getString(titleColumnPosition));
			dateTextView.setText(DateFormat.getDateTimeInstance().format(new Date(cursor.getLong(dateColumn))));
		}
		
		if (forcedState == STATE_ALLUNREAD && !markedAsRead.contains(id) || (forcedState != STATE_ALLREAD && cursor.isNull(readDateColumn) && !markedAsRead.contains(id)) || markedAsUnread.contains(id)) {
			textView.setTypeface(Typeface.DEFAULT_BOLD);
			textView.setEnabled(true);
			dateTextView.setEnabled(true);
		} else {
			textView.setTypeface(Typeface.DEFAULT);
			textView.setEnabled(false);
			dateTextView.setEnabled(false);
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
	
	public void markAsRead() {
		forcedState = STATE_ALLREAD;
		markedAsRead.clear();
		markedAsUnread.clear();
		notifyDataSetInvalidated();
	}
	
	public void markAsUnread() {
		forcedState = STATE_ALLUNREAD;
		markedAsRead.clear();
		markedAsUnread.clear();
		notifyDataSetInvalidated();
	}
	
	public void neutralizeReadState() {
		forcedState = STATE_NEUTRAL;
	}

	public void markAsRead(long id) {
		markedAsRead.add(id);
		markedAsUnread.remove(id);
		notifyDataSetInvalidated();
	}

	public void markAsUnread(long id) {
		markedAsUnread.add(id);
		markedAsRead.remove(id);
		notifyDataSetInvalidated();
	}
	
}
