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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import android.content.Context;
import android.database.Cursor;
import de.shandschuh.sparserss.provider.FeedData;

public class OPML {
	private static final String START = "<?xml version=\"1.0\" encoding=\"utf-8\"?><opml version=\"1.1\"><head><title>Sparse RSS export</title><dateCreated>";
	
	private static final String AFTERDATE = "</dateCreated><ownerName>Sparse RSS</ownerName></head><body><outline text=\"Sparse RSS feeds\">";
	
	private static final String OUTLINE_TITLE = "<outline title=\"";
	
	private static final String OUTLINE_XMLURL = "\" type=\"rss\" xmlUrl=\"";
	
	private static final String OUTLINE_CLOSING = "\" />";
	
	private static final String CLOSING = "</outline></body></opml>\n";
	
	public static void importFromFile(String filename, Context context) {
		
	}
	
	public static void exportToFile(String filename, Context context) throws IOException {
		StringBuilder builder = new StringBuilder(START);
		
		builder.append(System.currentTimeMillis());
		builder.append(AFTERDATE);
		
		Cursor cursor = context.getContentResolver().query(FeedData.FeedColumns.CONTENT_URI, new String[] {FeedData.FeedColumns._ID, FeedData.FeedColumns.NAME, FeedData.FeedColumns.URL}, null, null, null);
		
		while(cursor.moveToNext()) {
			builder.append(OUTLINE_TITLE);
			builder.append(cursor.getString(1));
			builder.append(OUTLINE_XMLURL);
			builder.append(cursor.getString(2));
			builder.append(OUTLINE_CLOSING);
		}
		cursor.close();
		builder.append(CLOSING);
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

		writer.write(builder.toString());
		writer.close();
	}
}
