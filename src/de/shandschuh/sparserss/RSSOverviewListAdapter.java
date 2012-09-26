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

import java.text.DateFormat;
import java.util.Date;
import java.util.Vector;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.util.TypedValue;
import android.view.View;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import de.shandschuh.sparserss.provider.FeedData;

public class RSSOverviewListAdapter extends ResourceCursorAdapter {
	private static final String COUNT_UNREAD = "COUNT(*) - COUNT(readdate)";
	
	private static final String COUNT = "COUNT(*)";
	
	private String COLON;
	
	private int nameColumnPosition;
	
	private int lastUpdateColumn;
	
	private int idPosition;
	
	private int linkPosition;
	
	private int errorPosition;
	
	private int iconPosition;
	
	private Handler handler;
	
	private SimpleTask updateTask;
	
	private boolean feedSort;
	
	private Vector<View> sortViews;
	
	private DateFormat dateFormat;
	
	private DateFormat timeFormat;
	
	public RSSOverviewListAdapter(Activity activity) {
		super(activity, R.layout.feedlistitem, activity.managedQuery(FeedData.FeedColumns.CONTENT_URI, null, null, null, null));
		nameColumnPosition = getCursor().getColumnIndex(FeedData.FeedColumns.NAME);
		lastUpdateColumn = getCursor().getColumnIndex(FeedData.FeedColumns.LASTUPDATE);
		idPosition = getCursor().getColumnIndex(FeedData.FeedColumns._ID);
		linkPosition = getCursor().getColumnIndex(FeedData.FeedColumns.URL);
		errorPosition = getCursor().getColumnIndex(FeedData.FeedColumns.ERROR);
		iconPosition = getCursor().getColumnIndex(FeedData.FeedColumns.ICON);
		COLON = activity.getString(R.string.colon);
		handler = new Handler();
		updateTask = new SimpleTask() {
			@Override
			public void runControlled() {
				RSSOverviewListAdapter.super.onContentChanged();
				cancel(); // cancel the task such that it does not run more than once without explicit intention
			}
			
			@Override
			public void postRun() {
				if (getPostCount() > 1) { // enforce second run even if task is canceled
					handler.postDelayed(updateTask, 1500);
				}
			}
		};
		sortViews = new Vector<View>();
		dateFormat = android.text.format.DateFormat.getDateFormat(activity);
		timeFormat = android.text.format.DateFormat.getTimeFormat(activity);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		TextView textView = ((TextView) view.findViewById(android.R.id.text1));
		
		textView.setSingleLine();
		
		Cursor countCursor = context.getContentResolver().query(FeedData.EntryColumns.CONTENT_URI(cursor.getString(idPosition)), new String[] {COUNT_UNREAD, COUNT}, null, null, null);
		
		countCursor.moveToFirst();
		
		int unreadCount = countCursor.getInt(0);
		
		int count = countCursor.getInt(1);
		
		countCursor.close();
		
		long timestamp = cursor.getLong(lastUpdateColumn);
		
		TextView updateTextView = ((TextView) view.findViewById(android.R.id.text2));;
		
		if (cursor.isNull(errorPosition)) {
			Date date = new Date(timestamp);
			
			updateTextView.setText(new StringBuilder(context.getString(R.string.update)).append(COLON).append(timestamp == 0 ? context.getString(R.string.never) : new StringBuilder(dateFormat.format(date)).append(' ').append(timeFormat.format(date)).append(Strings.COMMASPACE).append(unreadCount).append('/').append(count).append(' ').append(context.getString(R.string.unread))));
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
			Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
			
			if (bitmap != null && bitmap.getHeight() > 0 && bitmap.getWidth() > 0) {
				int bitmapSizeInDip = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18f, context.getResources().getDisplayMetrics());
				
				if (bitmap.getHeight() != bitmapSizeInDip) {
					bitmap = Bitmap.createScaledBitmap(bitmap, bitmapSizeInDip, bitmapSizeInDip, false);
				}
				textView.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(bitmap), null, null, null);
				textView.setText(" " + (cursor.isNull(nameColumnPosition) ? cursor.getString(linkPosition) : cursor.getString(nameColumnPosition)));
			} else {
				textView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
				textView.setText(cursor.isNull(nameColumnPosition) ? cursor.getString(linkPosition) : cursor.getString(nameColumnPosition));
			}
		} else {
			view.setTag(null);
			textView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
			textView.setText(cursor.isNull(nameColumnPosition) ? cursor.getString(linkPosition) : cursor.getString(nameColumnPosition));
		}
		
		View sortView = view.findViewById(R.id.sortitem);
		
		if (!sortViews.contains(sortView)) { // as we are reusing views, this is fine
			sortViews.add(sortView);
		}
		sortView.setVisibility(feedSort ? View.VISIBLE : View.GONE);
	}

	@Override
	protected synchronized void onContentChanged() {
		/*
		 * we delay the second(!) content change by 1.5 second such that it gets called at most once per 1.5 seconds 
		 * to take stress away from the UI and avoid not needed updates
		 */
		if (!updateTask.isPosted()) {
			super.onContentChanged();
			updateTask.post(2); // we post 2 tasks
			handler.postDelayed(updateTask, 1500); // waits one second until the task gets unposted
			updateTask.cancel(); // put the canceled task in the queue to enable it again optionally
		} else {
			if (updateTask.getPostCount() < 2) {
				updateTask.post(); // enables the task and adds a new one
			} else {
				updateTask.enable();
			}
		}
	}

	public void setFeedSortEnabled(boolean enabled) {
		feedSort = enabled;
		
		/* we do not want to call notifyDataSetChanged as this requeries the cursor*/
		int visibility = feedSort ? View.VISIBLE : View.GONE;
		
		for (View sortView : sortViews) {
			sortView.setVisibility(visibility);
		}
	}
}
