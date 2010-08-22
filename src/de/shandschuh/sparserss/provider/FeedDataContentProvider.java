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

package de.shandschuh.sparserss.provider;

import java.io.File;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import de.shandschuh.sparserss.Strings;

public class FeedDataContentProvider extends ContentProvider {
	public static boolean USE_SDCARD;
	
	private static final String FOLDER = Environment.getExternalStorageDirectory()+"/sparserss/";
	
	private static final String DATABASE_NAME = "sparserss.db";
	
	private static final int DATABASE_VERSION = 3;
	
	private static final int URI_FEEDS = 1;
	
	private static final int URI_FEED = 2;
	
	private static final int URI_ENTRIES = 3;
	
	private static final int URI_ENTRY= 4;
	
	private static final int URI_ALLENTRIES = 5;
	
	private static final int URI_ALLENTRIES_ENTRY = 6;
	
	private static final String TABLE_FEEDS = "feeds";
	
	private static final String TABLE_ENTRIES = "entries";
	
	private static final String ALTER_TABLE = "ALTER TABLE ";
	
	private static final String ADD = " ADD ";
	
	private static UriMatcher URI_MATCHER;
	
	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "feeds", URI_FEEDS);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "feeds/#", URI_FEED);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "feeds/#/entries", URI_ENTRIES);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "feeds/#/entries/#", URI_ENTRY);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "entries", URI_ALLENTRIES);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "entries/#", URI_ALLENTRIES_ENTRY);
	}
	
	private static class DatabaseHelper {
		private SQLiteDatabase database;
		
		public DatabaseHelper(Context context, String name, int version) {
			File file = new File(FOLDER);
			
			if ((file.exists() && file.isDirectory() || file.mkdir()) && file.canWrite()) {
				try {
					database = SQLiteDatabase.openDatabase(FOLDER+name, null, SQLiteDatabase.OPEN_READWRITE + SQLiteDatabase.CREATE_IF_NECESSARY);
					
					if (database.getVersion() == 0) {
						onCreate(database);
					} else {
						onUpgrade(database, database.getVersion(), DATABASE_VERSION);
					}
					database.setVersion(DATABASE_VERSION);
					USE_SDCARD = true;
				} catch (SQLException sqlException) {
					database = new SQLiteOpenHelper(context, name, null, version) {
						@Override
						public void onCreate(SQLiteDatabase db) {
							DatabaseHelper.this.onCreate(db);
						}

						@Override
						public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
							DatabaseHelper.this.onUpgrade(db, oldVersion, newVersion);
						}
					}.getWritableDatabase();
					USE_SDCARD = false;
				}
			} else {
				database = new SQLiteOpenHelper(context, name, null, version) {
					@Override
					public void onCreate(SQLiteDatabase db) {
						DatabaseHelper.this.onCreate(db);
					}

					@Override
					public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
						DatabaseHelper.this.onUpgrade(db, oldVersion, newVersion);
					}
				}.getWritableDatabase();
				USE_SDCARD = false;
			}
			context.sendBroadcast(new Intent(Strings.ACTION_UPDATEWIDGET));
		}

		public void onCreate(SQLiteDatabase database) {
			database.execSQL(createTable(TABLE_FEEDS, FeedData.FeedColumns.COLUMNS, FeedData.FeedColumns.TYPES));
			database.execSQL(createTable(TABLE_ENTRIES, FeedData.EntryColumns.COLUMNS, FeedData.EntryColumns.TYPES));
		}
		
		private String createTable(String tableName, String[] columns, String[] types) {
			if (tableName == null || columns == null || types == null || types.length != columns.length || types.length == 0) {
				throw new IllegalArgumentException("Invalid parameters for creating table "+tableName);
			} else {
				StringBuilder stringBuilder = new StringBuilder("CREATE TABLE ");
				
				stringBuilder.append(tableName);
				stringBuilder.append(" (");
				for (int n = 0, i = columns.length; n < i; n++) {
					if (n > 0) {
						stringBuilder.append(", ");
					}
					stringBuilder.append(columns[n]).append(' ').append(types[n]);
				}
				return stringBuilder.append(");").toString();
			}
		}

		public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
			if (oldVersion < 2) {
				database.execSQL(new StringBuilder(ALTER_TABLE).append(TABLE_FEEDS).append(ADD).append(FeedData.FeedColumns.PRIORITY).append(' ').append(FeedData.TYPE_INT).toString());
			}
			if (oldVersion < 3) {
				database.execSQL(new StringBuilder(ALTER_TABLE).append(TABLE_ENTRIES).append(ADD).append(FeedData.EntryColumns.FAVORITE).append(' ').append(FeedData.TYPE_BOOLEAN).toString());
			}
		}

		public SQLiteDatabase getWritableDatabase() {
			return database;
		}
	}
	
	private SQLiteDatabase database;

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int option = URI_MATCHER.match(uri);
		
		String table = null;
		
		StringBuilder where = new StringBuilder();
		
		switch(option) {
			case URI_FEED : {
				table = TABLE_FEEDS;
			
				final String feedId = uri.getPathSegments().get(1);
				
				new Thread() {
					public void run() {
						delete(FeedData.EntryColumns.CONTENT_URI(feedId), null, null);
					}
				}.start();
				
				where.append(FeedData.FeedColumns._ID).append('=').append(feedId);
				break;
			}
			case URI_FEEDS : {
				table = TABLE_FEEDS;
				delete(FeedData.FeedColumns.CONTENT_URI("0"), null, null);
				break;
			}
			case URI_ENTRY : {
				table = TABLE_ENTRIES;
				where.append(FeedData.EntryColumns._ID).append('=').append(uri.getPathSegments().get(3));
				break;
			}
			case URI_ENTRIES : {
				table = TABLE_ENTRIES;
				where.append(FeedData.EntryColumns.FEED_ID).append('=').append(uri.getPathSegments().get(1));
				break;
			}
			case URI_ALLENTRIES : {
				table = TABLE_ENTRIES;
				break;
			}
			case URI_ALLENTRIES_ENTRY : {
				table = TABLE_ENTRIES;
				where.append(FeedData.EntryColumns._ID).append('=').append(uri.getPathSegments().get(1));
				break;
			}
		}
		
		if (!TextUtils.isEmpty(selection)) {
			where.append(Strings.DB_AND).append(selection);
		}
		
		int count = database.delete(table, where.toString(), selectionArgs);
		
		if (count > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return count;
	}

	@Override
	public String getType(Uri uri) {
		int option = URI_MATCHER.match(uri);
		
		switch(option) {
			case URI_FEEDS : return "vnd.android.cursor.dir/vnd.feeddata.feed";
			case URI_FEED : return "vnd.android.cursor.item/vnd.feeddata.feed";
			case URI_ALLENTRIES :
			case URI_ENTRIES : return "vnd.android.cursor.dir/vnd.feeddata.entry";
			case URI_ALLENTRIES_ENTRY : 
			case URI_ENTRY : return "vnd.android.cursor.item/vnd.feeddata.entry";
			default : throw new IllegalArgumentException("Unknown URI: "+uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		long newId = 0;
		
		int option = URI_MATCHER.match(uri);
		
		switch (option) {
			case URI_FEEDS : {
				database.insert(TABLE_FEEDS, null, values);
				break;
			}
			case URI_ENTRIES : {
				values.put(FeedData.EntryColumns.FEED_ID, uri.getPathSegments().get(1));
				database.insert(TABLE_ENTRIES, null, values);
				break;
			}
			case URI_ALLENTRIES : {
				database.insert(TABLE_ENTRIES, null, values);
				break;
			}
			default : throw new IllegalArgumentException("Illegal insert");
		}
		if (newId > -1) {
			getContext().getContentResolver().notifyChange(uri, null);
			return ContentUris.withAppendedId(uri, newId);
		} else {
			throw new SQLException("Could not insert row into "+uri);
		}
	}

	@Override
	public boolean onCreate() {
		database = new DatabaseHelper(getContext(), DATABASE_NAME, DATABASE_VERSION).getWritableDatabase();
		
		return database != null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		
		int option = URI_MATCHER.match(uri);
		
		
		switch(option) {
			case URI_FEED : {
				queryBuilder.setTables(TABLE_FEEDS);
				queryBuilder.appendWhere(new StringBuilder(FeedData.FeedColumns._ID).append('=').append(uri.getPathSegments().get(1)));
				break;
			}
			case URI_FEEDS : {
				queryBuilder.setTables(TABLE_FEEDS);
				break;
			}
			case URI_ENTRY : {
				queryBuilder.setTables(TABLE_ENTRIES);
				queryBuilder.appendWhere(new StringBuilder(FeedData.EntryColumns._ID).append('=').append(uri.getPathSegments().get(3)));
				break;
			}
			case URI_ENTRIES : {
				queryBuilder.setTables(TABLE_ENTRIES);
				queryBuilder.appendWhere(new StringBuilder(FeedData.EntryColumns.FEED_ID).append('=').append(uri.getPathSegments().get(1)));
				break;
			}
			case URI_ALLENTRIES : {
				queryBuilder.setTables(TABLE_ENTRIES);
				break;
			}
			case URI_ALLENTRIES_ENTRY : {
				queryBuilder.setTables(TABLE_ENTRIES);
				queryBuilder.appendWhere(new StringBuilder(FeedData.EntryColumns._ID).append('=').append(uri.getPathSegments().get(1)));
				break;
			}
		}
		Cursor cursor = queryBuilder.query(database, projection, selection, selectionArgs, null, null, sortOrder);

		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int option = URI_MATCHER.match(uri);
		
		String table = null;
		
		StringBuilder where = new StringBuilder();
		
		switch(option) {
			case URI_FEED : {
				table = TABLE_FEEDS;
				where.append(FeedData.FeedColumns._ID).append('=').append(uri.getPathSegments().get(1));
				break;
			}
			case URI_FEEDS : {
				table = TABLE_FEEDS;
				break;
			}
			case URI_ENTRY : {
				table = TABLE_ENTRIES;
				where.append(FeedData.EntryColumns._ID).append('=').append(uri.getPathSegments().get(3));
				break;
			}
			case URI_ENTRIES : {
				table = TABLE_ENTRIES;
				where.append(FeedData.EntryColumns.FEED_ID).append('=').append(uri.getPathSegments().get(1));
				break;
			}
			case URI_ALLENTRIES: {
				table = TABLE_ENTRIES;
				break;
			}
			case URI_ALLENTRIES_ENTRY : {
				table = TABLE_ENTRIES;
				where.append(FeedData.EntryColumns._ID).append('=').append(uri.getPathSegments().get(1));
				break;
			}
		}
		
		if (!TextUtils.isEmpty(selection)) {
			if (where.length() > 0) {
				where.append(Strings.DB_AND).append(selection);
			} else {
				where.append(selection);
			}
		}
		
		int count = database.update(table, values, where.toString(), selectionArgs);
		
		if (count > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return count;
	}

}
