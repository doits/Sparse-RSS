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

package de.shandschuh.sparserss.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class FeedData {
	public static final String CONTENT = "content://";
	
	public static final String AUTHORITY = "de.shandschuh.sparserss.provider.FeedData";
	
	private static final String TYPE_PRIMARY_KEY = "INTEGER PRIMARY KEY AUTOINCREMENT";
	
	private static final String TYPE_TEXT = "TEXT";
	
	public static final String TYPE_DATETIME = "DATETIME";
	
	public static final String TYPE_INT = "INT";

	public static final String TYPE_BOOLEAN = "INTEGER(1)";
	
	public static class FeedColumns implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse(new StringBuilder(CONTENT).append(AUTHORITY).append("/feeds").toString());
		
		public static final String URL = "url";
		
		public static final String NAME = "name";
		
		public static final String LASTUPDATE = "lastupdate";
		
		public static final String ICON = "icon";
		
		public static final String ERROR = "error";
		
		public static final String PRIORITY = "priority";
		
		public static final String FETCHMODE = "fetchmode";
		
		public static final String REALLASTUPDATE = "reallastupdate";
		
		public static final String[] COLUMNS = new String[] {_ID, URL, NAME, LASTUPDATE, ICON, ERROR, PRIORITY, FETCHMODE, REALLASTUPDATE};
		
		public static final String[] TYPES = new String[] {TYPE_PRIMARY_KEY, "TEXT UNIQUE", TYPE_TEXT, TYPE_DATETIME, "BLOB", TYPE_TEXT, TYPE_INT, TYPE_INT, TYPE_DATETIME};
		
		public static final Uri CONTENT_URI(String feedId) {
			return Uri.parse(new StringBuilder(CONTENT).append(AUTHORITY).append("/feeds/").append(feedId).toString());
		}
	}
	
	public static class EntryColumns implements BaseColumns {
		public static final String FEED_ID = "feedid";
		
		public static final String TITLE = "title";
		
		public static final String ABSTRACT = "abstract";
		
		public static final String DATE = "date";
		
		public static final String READDATE = "readdate";
		
		public static final String LINK = "link";
		
		public static final String FAVORITE = "favorite";
		
		public static final String[] COLUMNS = new String[] {_ID, FEED_ID, TITLE, ABSTRACT, DATE, READDATE, LINK, FAVORITE};
		
		public static final String[] TYPES = new String[] {TYPE_PRIMARY_KEY, "INTEGER(7)", TYPE_TEXT, TYPE_TEXT, TYPE_DATETIME, TYPE_DATETIME, TYPE_TEXT, TYPE_BOOLEAN};

		public static Uri CONTENT_URI = Uri.parse(new StringBuilder(CONTENT).append(AUTHORITY).append("/entries").toString());
		
		public static Uri FAVORITES_CONTENT_URI = Uri.parse(new StringBuilder(CONTENT).append(AUTHORITY).append("/favorites").toString());

		public static Uri CONTENT_URI(String feedId) {
			return Uri.parse(new StringBuilder(CONTENT).append(AUTHORITY).append("/feeds/").append(feedId).append("/entries").toString());
		}

		public static Uri ENTRY_CONTENT_URI(String entryId) {
			return Uri.parse(new StringBuilder(CONTENT).append(AUTHORITY).append("/entries/").append(entryId).toString());
		}
		
	}

}
