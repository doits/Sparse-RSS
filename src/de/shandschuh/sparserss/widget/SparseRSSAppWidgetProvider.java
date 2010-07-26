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

package de.shandschuh.sparserss.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.RemoteViews;
import de.shandschuh.sparserss.R;
import de.shandschuh.sparserss.RSSOverview;
import de.shandschuh.sparserss.Strings;
import de.shandschuh.sparserss.provider.FeedData;

public class SparseRSSAppWidgetProvider extends AppWidgetProvider {
	private static final String LIMIT = " limit 7";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (Strings.ACTION_UPDATEWIDGET.equals(intent.getAction())) {
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
			
			onUpdate(context, appWidgetManager, appWidgetManager.getAppWidgetIds(new ComponentName(context, SparseRSSAppWidgetProvider.class)));
		} else {
			super.onReceive(context, intent);
		}
	}

	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Cursor cursor = context.getContentResolver().query(FeedData.EntryColumns.CONTENT_URI, new String[] {FeedData.EntryColumns.TITLE, FeedData.EntryColumns._ID}, null /* new StringBuilder(FeedData.EntryColumns.READDATE).append(" IS NULL").toString()*/, null, new StringBuilder(FeedData.EntryColumns.DATE).append(Strings.DB_DESC).append(LIMIT).toString());
        
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.homescreenwidget);

        views.setOnClickPendingIntent(R.id.feed_icon, PendingIntent.getActivity(context, 0, new Intent(context, RSSOverview.class), 0));
        
        int[] ids = {R.id.news_1, R.id.news_2, R.id.news_3, R.id.news_4, R.id.news_5, R.id.news_6, R.id.news_7};
        
        int n = 0;
        
        while (cursor.moveToNext() && n < ids.length) {
        	views.setTextViewText(ids[n], cursor.getString(0));
        	views.setOnClickPendingIntent(ids[n++], PendingIntent.getActivity(context, 0, new Intent(Intent.ACTION_VIEW, FeedData.EntryColumns.ENTRY_CONTENT_URI(cursor.getString(1))), PendingIntent.FLAG_CANCEL_CURRENT));
        }
        cursor.close();
        for (; n < ids.length; n++) {
        	views.setTextViewText(ids[n], Strings.EMPTY);
        }
        appWidgetManager.updateAppWidget(appWidgetIds, views);
    }

}
