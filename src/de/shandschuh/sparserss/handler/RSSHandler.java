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

package de.shandschuh.sparserss.handler;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;
import de.shandschuh.sparserss.Strings;
import de.shandschuh.sparserss.provider.FeedData;

public class RSSHandler extends DefaultHandler {
	public static final String AMP_SG = "&amp;";
	
	public static final String AMP = "&";
	
	private static final String TAG_RSS = "rss";
	
	private static final String TAG_RDF = "rdf";
	
	private static final String TAG_FEED = "feed";
	
	private static final String TAG_ENTRY = "entry";
	
	private static final String TAG_ITEM = "item";
	
	private static final String TAG_UPDATED = "updated";
	
	private static final String TAG_TITLE = "title";
	
	private static final String TAG_LINK = "link";
	
	private static final String TAG_DESCRIPTION = "description";
	
	private static final String TAG_SUMMARY = "summary";
	
	private static final String TAG_PUBDATE = "pubDate";
	
	private static final String TAG_DATE = "date";
	
	private static final String ATTRIBUTE_HREF = "href";
	
	private static final String MEST = "MEST";
	
	private static final String PLUS200 = "+0200";
	
	private static long KEEP_TIME = 172800000; // 2 days
	
	private static final DateFormat UPDATE_DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	
	private static final DateFormat UPDATE_DATEFORMAT_SLOPPY = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	
	private static final DateFormat PUBDATE_DATEFORMAT = new SimpleDateFormat("EEE', 'd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z", Locale.US);

	private Context context;
	
	private Date lastUpdateDate;
	
	private String id;

	private boolean titleTagEntered;
	
	private boolean updatedTagEntered;
	
	private boolean linkTagEntered;
	
	private boolean descriptionTagEntered;
	
	private boolean pubDateTagEntered;
	
	private boolean dateTagEntered;
	
	private StringBuilder title;
	
	private StringBuilder dateStringBuilder;
	
	private Date entryDate;
	
	private StringBuilder entryLink;
	
	private StringBuilder description;
	
	private Uri feedEntiresUri;
	
	private int newCount;
	
	private boolean feedRefreshed;
	
	private String feedTitle;
	
	private boolean done;
	
	public RSSHandler(Context context) {
		KEEP_TIME = PreferenceManager.getDefaultSharedPreferences(context).getInt(Strings.SETTINGS_KEEPTIME, 2)*86400000;
		this.context = context;
	}
	
	public void init(Date lastUpdateDate, String id, String title) {
		this.lastUpdateDate = lastUpdateDate;
		this.id = id;
		feedEntiresUri = FeedData.EntryColumns.CONTENT_URI(id);
		context.getContentResolver().delete(feedEntiresUri, new StringBuilder(FeedData.EntryColumns.DATE).append('<').append(System.currentTimeMillis()-KEEP_TIME).toString(), null);
		newCount = 0;
		feedRefreshed = false;
		feedTitle = title;
		done = false;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (TAG_UPDATED.equals(localName)) {
			updatedTagEntered = true;
			dateStringBuilder = new StringBuilder();
		} else if (TAG_ENTRY.equals(localName) || TAG_ITEM.equals(localName)) {
			if (!feedRefreshed) {
				ContentValues values = new ContentValues();
					
				if (feedTitle == null && title != null && title.length() > 0) {
					values.put(FeedData.FeedColumns.NAME, title.toString());
				}
				values.put(FeedData.FeedColumns.ERROR, (String) null);
				values.put(FeedData.FeedColumns.LASTUPDATE, entryDate != null ? entryDate.getTime() : System.currentTimeMillis() - 1000);
				context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id), values, null, null);
				
				feedRefreshed = true;
			}
		} else if (TAG_TITLE.equals(localName)) {
			if (title == null) {
				titleTagEntered = true;
				title = new StringBuilder();
			}
		} else if (TAG_LINK.equals(localName)) {
			entryLink = new StringBuilder();
			
			boolean foundLink = false;
			
			for (int n = 0, i = attributes.getLength(); n < i; n++) {
				if (ATTRIBUTE_HREF.equals(attributes.getLocalName(n))) {
					if (attributes.getValue(n) != null) {
						entryLink.append(attributes.getValue(n));
						foundLink = true;
						linkTagEntered = false;
					} else {
						linkTagEntered = true;
					}
					break;
				}
			}
			if (!foundLink) {
				linkTagEntered = true;
			}
			
		} else if (TAG_DESCRIPTION.equals(localName) || TAG_SUMMARY.equals(localName)) {
			descriptionTagEntered = true;
			description = new StringBuilder();
		} else if (TAG_PUBDATE.equals(localName)) {
			pubDateTagEntered = true;
			dateStringBuilder = new StringBuilder();
		} else if (TAG_DATE.equals(localName)) {
			dateTagEntered = true;
			dateStringBuilder = new StringBuilder();
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (titleTagEntered) {
			title.append(ch, start, length);
		} else if (updatedTagEntered) {
			dateStringBuilder.append(ch, start, length);
		} else if (linkTagEntered) {
			entryLink.append(ch, start, length);
		} else if (descriptionTagEntered) {
			description.append(ch, start, length);
		} else if (pubDateTagEntered) {
			dateStringBuilder.append(ch, start, length);
		} else if (dateTagEntered) {
			dateStringBuilder.append(ch, start, length);
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (TAG_TITLE.equals(localName)) {
			titleTagEntered = false;
		} else if (TAG_DESCRIPTION.equals(localName) || TAG_SUMMARY.equals(localName)) {
			descriptionTagEntered = false;
		} else if (TAG_LINK.equals(localName)) {
			linkTagEntered = false;
		} else if (TAG_UPDATED.equals(localName)) {
			try {
				entryDate = UPDATE_DATEFORMAT.parse(dateStringBuilder.toString());
			} catch (ParseException e) {
				try {
					entryDate = UPDATE_DATEFORMAT_SLOPPY.parse(dateStringBuilder.toString());
				} catch (ParseException e2) {
					
				}
			}
			updatedTagEntered = false;
		} else if (TAG_PUBDATE.equals(localName)) {
			try {
				entryDate = PUBDATE_DATEFORMAT.parse(dateStringBuilder.toString().replace(MEST, PLUS200)); // replace is needed because mest is no supported timezone
			} catch (ParseException e) {
				
			}
			pubDateTagEntered = false;
		} else if (TAG_DATE.equals(localName)) {
			try {
				entryDate = UPDATE_DATEFORMAT.parse(dateStringBuilder.toString());
			} catch (ParseException e) {
				try {
					entryDate = UPDATE_DATEFORMAT_SLOPPY.parse(dateStringBuilder.toString());
				} catch (ParseException e2) {
					
				}
			}
			dateTagEntered = false;
		} else if (TAG_ENTRY.equals(localName) || TAG_ITEM.equals(localName)) {
			if (entryDate != null && entryDate.after(lastUpdateDate)) {
				ContentValues values = new ContentValues();
				
				values.put(FeedData.EntryColumns.DATE, entryDate.getTime());
				values.put(FeedData.EntryColumns.TITLE, title.toString().replace(AMP_SG, AMP));
				if (description != null) {
					values.put(FeedData.EntryColumns.ABSTRACT, description.toString().trim()); // maybe better use regex, but this will do it for now
					description = null;
				}
				
				String entryLinkString = entryLink.toString();
				
				if (context.getContentResolver().update(feedEntiresUri, values, new StringBuilder(FeedData.EntryColumns.LINK).append(Strings.DB_ARG).toString(), new String[] {entryLinkString}) == 0) {
					values.put(FeedData.EntryColumns.LINK, entryLinkString);
					
					context.getContentResolver().insert(feedEntiresUri, values);
					newCount++;
				}
			}
			title = null;
		} else if (TAG_RSS.equals(localName) || TAG_RDF.equals(localName) || TAG_FEED.equals(localName)) {
			done = true;
		}
	}
	
	public int getNewCount() {
		return newCount;
	}
	
	public boolean isDone() {
		return done;
	}
	
}
