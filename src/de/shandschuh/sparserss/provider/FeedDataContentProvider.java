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
	private static final String FOLDER = Environment.getExternalStorageDirectory()+"/sparserss/";
	
	private static final String DATABASE_NAME = "sparserss.db";
	
	private static final int DATABASE_VERSION = 9;
	
	private static final int URI_FEEDS = 1;
	
	private static final int URI_FEED = 2;
	
	private static final int URI_ENTRIES = 3;
	
	private static final int URI_ENTRY= 4;
	
	private static final int URI_ALLENTRIES = 5;
	
	private static final int URI_ALLENTRIES_ENTRY = 6;
	
	private static final int URI_FAVORITES = 7;
	
	private static final int URI_FAVORITES_ENTRY = 8;
	
	protected static final String TABLE_FEEDS = "feeds";
	
	private static final String TABLE_ENTRIES = "entries";
	
	private static final String ALTER_TABLE = "ALTER TABLE ";
	
	private static final String ADD = " ADD ";
	
	private static final String EQUALS_ONE = "=1";

	public static final String IMAGEFOLDER = Environment.getExternalStorageDirectory()+"/sparserss/images/"; // faster than FOLDER+"images/"
	
	private static final String BACKUPOPML = Environment.getExternalStorageDirectory()+"/sparserss/backup.opml";
	
	private static UriMatcher URI_MATCHER;
	
	private static final String[] PROJECTION_PRIORITY = new String[] {FeedData.FeedColumns.PRIORITY};
	
	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "feeds", URI_FEEDS);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "feeds/#", URI_FEED);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "feeds/#/entries", URI_ENTRIES);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "feeds/#/entries/#", URI_ENTRY);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "entries", URI_ALLENTRIES);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "entries/#", URI_ALLENTRIES_ENTRY);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "favorites", URI_FAVORITES);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "favorites/#", URI_FAVORITES_ENTRY);
	}
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		public DatabaseHelper(Context context, String name, int version) {
			super(context, name, null, version);
			context.sendBroadcast(new Intent(Strings.ACTION_UPDATEWIDGET));
		}

		@Override
		public void onCreate(SQLiteDatabase database) {
			database.beginTransaction();
			database.execSQL(createTable(TABLE_FEEDS, FeedData.FeedColumns.COLUMNS, FeedData.FeedColumns.TYPES));
			database.execSQL(createTable(TABLE_ENTRIES, FeedData.EntryColumns.COLUMNS, FeedData.EntryColumns.TYPES));
			database.setTransactionSuccessful();
			database.endTransaction();
			
			File backupFile = new File(BACKUPOPML);
			
			if (backupFile.exists()) {
				/** Perform an automated import of the backup */
				OPML.importFromFile(backupFile, database);
			}
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

		@Override
		public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
			if (oldVersion < 2) {
				database.execSQL(new StringBuilder(ALTER_TABLE).append(TABLE_FEEDS).append(ADD).append(FeedData.FeedColumns.PRIORITY).append(' ').append(FeedData.TYPE_INT).toString());
			}
			if (oldVersion < 3) {
				database.execSQL(new StringBuilder(ALTER_TABLE).append(TABLE_ENTRIES).append(ADD).append(FeedData.EntryColumns.FAVORITE).append(' ').append(FeedData.TYPE_BOOLEAN).toString());
			}
			if (oldVersion < 4) {
				database.execSQL(new StringBuilder(ALTER_TABLE).append(TABLE_FEEDS).append(ADD).append(FeedData.FeedColumns.FETCHMODE).append(' ').append(FeedData.TYPE_INT).toString());
			}
			if (oldVersion < 5) {
				database.execSQL(new StringBuilder(ALTER_TABLE).append(TABLE_FEEDS).append(ADD).append(FeedData.FeedColumns.REALLASTUPDATE).append(' ').append(FeedData.TYPE_DATETIME).toString());
			} 
			if (oldVersion < 6) {
				Cursor cursor = database.query(TABLE_FEEDS, new String[] {FeedData.FeedColumns._ID}, null, null, null, null, FeedData.FeedColumns._ID);
				
				int count = 0;
				
				while (cursor.moveToNext()) {
					database.execSQL(new StringBuilder("UPDATE ").append(TABLE_FEEDS).append(" SET ").append(FeedData.FeedColumns.PRIORITY).append('=').append(count++).append(" WHERE _ID=").append(cursor.getLong(0)).toString());
				}
				cursor.close();
			} 
			if (oldVersion < 7) {
				database.execSQL(new StringBuilder(ALTER_TABLE).append(TABLE_FEEDS).append(ADD).append(FeedData.FeedColumns.WIFIONLY).append(' ').append(FeedData.TYPE_BOOLEAN).toString());
			}
			// we simply leave the "encoded" column untouched
			if (oldVersion < 9) {
				database.execSQL(new StringBuilder(ALTER_TABLE).append(TABLE_ENTRIES).append(ADD).append(FeedData.EntryColumns.ENCLOSURE).append(' ').append(FeedData.TYPE_TEXT).toString());
			} 
		}

		@Override
		public synchronized SQLiteDatabase getWritableDatabase() {
			File oldDatabaseFile = new File(Environment.getExternalStorageDirectory()+"/sparserss/sparserss.db");
			
			if (oldDatabaseFile.exists()) { // get rid of the old structure
				SQLiteDatabase newDatabase = super.getWritableDatabase();
				
				try {
					SQLiteDatabase oldDatabase  = SQLiteDatabase.openDatabase(Environment.getExternalStorageDirectory()+"/sparserss/sparserss.db", null, SQLiteDatabase.OPEN_READWRITE + SQLiteDatabase.CREATE_IF_NECESSARY);
					
					Cursor cursor = oldDatabase.query(TABLE_ENTRIES, null, null, null, null, null, null);
					
					newDatabase.beginTransaction();
					
					String[] columnNames = cursor.getColumnNames();
					
					int i = columnNames.length;
					
					int[] columnIndices = new int[i];
					
					for (int n = 0; n < i; n++) {
						columnIndices[n] = cursor.getColumnIndex(columnNames[n]);
					}
					
					
					
					while (cursor.moveToNext()) {
						ContentValues values = new ContentValues();
						
						for (int n = 0; n < i; n++) {
							if (!cursor.isNull(columnIndices[n])) {
								values.put(columnNames[n], cursor.getString(columnIndices[n]));
							}
						}
						
						newDatabase.insert(TABLE_ENTRIES, null, values);
					}
					cursor.close();
					cursor = oldDatabase.query(TABLE_FEEDS, null, null, null, null, null, FeedData.FeedColumns._ID);
					columnNames = cursor.getColumnNames();
					i = columnNames.length;
					columnIndices = new int[i];
					
					for (int n = 0; n < i; n++) {
						columnIndices[n] = cursor.getColumnIndex(columnNames[n]);
					}
					
					int count = 0;
					
					while (cursor.moveToNext()) {
						ContentValues values = new ContentValues();
						
						for (int n = 0; n < i; n++) {
							if (!cursor.isNull(columnIndices[n])) {
								if (FeedData.FeedColumns.ICON.equals(columnNames[n])) {
									values.put(FeedData.FeedColumns.ICON, cursor.getBlob(columnIndices[n]));
								} else {
									values.put(columnNames[n], cursor.getString(columnIndices[n]));
								}
							} 
						}
						values.put(FeedData.FeedColumns.PRIORITY, count++);
						newDatabase.insert(TABLE_FEEDS, null, values);
					}
					cursor.close();
					oldDatabase.close();
					oldDatabaseFile.delete();
					newDatabase.setTransactionSuccessful();
					newDatabase.endTransaction();
					OPML.exportToFile(BACKUPOPML, newDatabase);
				} catch (Exception e) {
					
				}
				return newDatabase;
			} else {
				return super.getWritableDatabase();
			}
		}
	}
	
	private SQLiteDatabase database;
	
	private String[] MAXPRIORITY = new String[] {"MAX("+FeedData.FeedColumns.PRIORITY+")"};

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
				
				/** Update the priorities */
				Cursor priorityCursor = database.query(TABLE_FEEDS, PROJECTION_PRIORITY, FeedData.FeedColumns._ID+"="+feedId, null, null, null, null);
				
				if (priorityCursor.moveToNext()) {
					database.execSQL("UPDATE "+TABLE_FEEDS+" SET "+FeedData.FeedColumns.PRIORITY+" = "+FeedData.FeedColumns.PRIORITY+"-1 WHERE "+FeedData.FeedColumns.PRIORITY+" > "+priorityCursor.getInt(0));
					priorityCursor.close();
				} else {
					priorityCursor.close();
				}
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
			case URI_ALLENTRIES : {
				table = TABLE_ENTRIES;
				break;
			}
			case URI_FAVORITES_ENTRY : 
			case URI_ALLENTRIES_ENTRY : {
				table = TABLE_ENTRIES;
				where.append(FeedData.EntryColumns._ID).append('=').append(uri.getPathSegments().get(1));
				break;
			}
			case URI_FAVORITES : {
				table = TABLE_ENTRIES;
				where.append(FeedData.EntryColumns.FAVORITE).append(EQUALS_ONE);
				break;
			}
		}
		
		if (!TextUtils.isEmpty(selection)) {
			if (where.length() > 0) {
				where.append(Strings.DB_AND);
			}
			where.append(selection);
		}
		
		int count = database.delete(table, where.toString(), selectionArgs);
		
		if (table == TABLE_FEEDS) { // == is ok here
			OPML.exportToFile(BACKUPOPML, database);
		}
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
			case URI_FAVORITES : 
			case URI_ALLENTRIES :
			case URI_ENTRIES : return "vnd.android.cursor.dir/vnd.feeddata.entry";
			case URI_FAVORITES_ENTRY : 
			case URI_ALLENTRIES_ENTRY : 
			case URI_ENTRY : return "vnd.android.cursor.item/vnd.feeddata.entry";
			default : throw new IllegalArgumentException("Unknown URI: "+uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		long newId = -1;
		
		int option = URI_MATCHER.match(uri);
		
		switch (option) {
			case URI_FEEDS : {
				Cursor cursor = database.query(TABLE_FEEDS, MAXPRIORITY, null, null, null, null, null, null);
				
				if (cursor.moveToNext()) {
					values.put(FeedData.FeedColumns.PRIORITY, cursor.getInt(0)+1);
				} else {
					values.put(FeedData.FeedColumns.PRIORITY, 1);
				}
				cursor.close();
				newId = database.insert(TABLE_FEEDS, null, values);
				OPML.exportToFile(BACKUPOPML, database);
				break;
			}
			case URI_ENTRIES : {
				values.put(FeedData.EntryColumns.FEED_ID, uri.getPathSegments().get(1));
				newId = database.insert(TABLE_ENTRIES, null, values);
				break;
			}
			case URI_ALLENTRIES : {
				newId = database.insert(TABLE_ENTRIES, null, values);
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
		try {
			File folder = new File(FOLDER);
			
			folder.mkdir(); // maybe we use the boolean return value later
		} catch (Exception e) {
			
		}
		database = new DatabaseHelper(getContext(), DATABASE_NAME, DATABASE_VERSION).getWritableDatabase();
		return database != null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		
		int option = URI_MATCHER.match(uri);
		
		if ((option == URI_FEED || option == URI_FEEDS) && sortOrder == null) {
			sortOrder = FeedData.FEED_DEFAULTSORTORDER;
		}
		
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
				queryBuilder.setTables("entries join (select name, icon, _id as feed_id from feeds) as F on (entries.feedid = F.feed_id)");
				break;
			}
			case URI_FAVORITES_ENTRY : 
			case URI_ALLENTRIES_ENTRY : {
				queryBuilder.setTables(TABLE_ENTRIES);
				queryBuilder.appendWhere(new StringBuilder(FeedData.EntryColumns._ID).append('=').append(uri.getPathSegments().get(1)));
				break;
			}
			case URI_FAVORITES : {
				queryBuilder.setTables("entries join (select name, icon, _id as feed_id from feeds) as F on (entries.feedid = F.feed_id)");
				queryBuilder.appendWhere(new StringBuilder(FeedData.EntryColumns.FAVORITE).append(EQUALS_ONE));
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
				
				long feedId = Long.parseLong(uri.getPathSegments().get(1));
				
				where.append(FeedData.FeedColumns._ID).append('=').append(feedId);
				if (values != null && values.containsKey(FeedData.FeedColumns.PRIORITY)) {
					int newPriority = values.getAsInteger(FeedData.FeedColumns.PRIORITY);
					
					Cursor priorityCursor = database.query(TABLE_FEEDS, PROJECTION_PRIORITY, FeedData.FeedColumns._ID+"="+feedId, null, null, null, null);
				
					if (priorityCursor.moveToNext()) {
						int oldPriority = priorityCursor.getInt(0);
						
						priorityCursor.close();
						if (newPriority > oldPriority) {
							database.execSQL("UPDATE "+TABLE_FEEDS+" SET "+FeedData.FeedColumns.PRIORITY+" = "+FeedData.FeedColumns.PRIORITY+"-1 WHERE "+FeedData.FeedColumns.PRIORITY+" BETWEEN "+(oldPriority+1)+" AND "+newPriority);
						} else if (newPriority < oldPriority) {
							database.execSQL("UPDATE "+TABLE_FEEDS+" SET "+FeedData.FeedColumns.PRIORITY+" = "+FeedData.FeedColumns.PRIORITY+"+1 WHERE "+FeedData.FeedColumns.PRIORITY+" BETWEEN "+newPriority+" AND "+(oldPriority-1));
						}
					} else {
						priorityCursor.close();
					}
				}
				break;
			}
			case URI_FEEDS : {
				table = TABLE_FEEDS;
				// maybe this should be disabled
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
			case URI_FAVORITES_ENTRY : 
			case URI_ALLENTRIES_ENTRY : {
				table = TABLE_ENTRIES;
				where.append(FeedData.EntryColumns._ID).append('=').append(uri.getPathSegments().get(1));
				break;
			}
			case URI_FAVORITES : {
				table = TABLE_ENTRIES;
				where.append(FeedData.EntryColumns.FAVORITE).append(EQUALS_ONE);				
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
		
		if (table == TABLE_FEEDS && (values.containsKey(FeedData.FeedColumns.NAME) || values.containsKey(FeedData.FeedColumns.URL) || values.containsKey(FeedData.FeedColumns.PRIORITY))) { // == is ok here
			OPML.exportToFile(BACKUPOPML, database);
		}
		if (count > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return count;
	}

}
