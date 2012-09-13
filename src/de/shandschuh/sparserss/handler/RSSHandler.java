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

package de.shandschuh.sparserss.handler;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.Html;
import de.shandschuh.sparserss.Strings;
import de.shandschuh.sparserss.provider.FeedData;
import de.shandschuh.sparserss.provider.FeedDataContentProvider;
import de.shandschuh.sparserss.service.FetcherService;

public class RSSHandler extends DefaultHandler {
	private static final String ANDRHOMBUS = "&#";
	
	private static final String TAG_RSS = "rss";
	
	private static final String TAG_RDF = "rdf";
	
	private static final String TAG_FEED = "feed";
	
	private static final String TAG_ENTRY = "entry";
	
	private static final String TAG_ITEM = "item";
	
	private static final String TAG_UPDATED = "updated";
	
	private static final String TAG_TITLE = "title";
	
	private static final String TAG_LINK = "link";
	
	private static final String TAG_DESCRIPTION = "description";
	
	private static final String TAG_MEDIA_DESCRIPTION = "media:description";
	
	private static final String TAG_CONTENT = "content";
	
	private static final String TAG_MEDIA_CONTENT = "media:content";
	
	private static final String TAG_ENCODEDCONTENT = "encoded";
	
	private static final String TAG_SUMMARY = "summary";
	
	private static final String TAG_PUBDATE = "pubDate";
	
	private static final String TAG_DATE = "date";
	
	private static final String TAG_LASTBUILDDATE = "lastBuildDate";
	
	private static final String TAG_ENCLOSURE = "enclosure";
	
	private static final String TAG_GUID = "guid";
	
	private static final String ATTRIBUTE_URL = "url";
	
	private static final String ATTRIBUTE_HREF = "href";
	
	private static final String ATTRIBUTE_TYPE = "type";
	
	private static final String ATTRIBUTE_LENGTH = "length";
	
	private static final String[] TIMEZONES = {"MEST", "EST", "PST"};
	
	private static final String[] TIMEZONES_REPLACE = {"+0200", "-0500", "-0800"};

	private static final int TIMEZONES_COUNT = 3;

	
	private static long KEEP_TIME = 345600000l; // 4 days
	
	
	private static final DateFormat[] PUBDATE_DATEFORMATS = {
		new SimpleDateFormat("EEE', 'd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z", Locale.US),
		new SimpleDateFormat("d' 'MMM' 'yyyy' 'HH:mm:ss' 'Z", Locale.US),
		new SimpleDateFormat("EEE', 'd' 'MMM' 'yyyy' 'HH:mm:ss' 'z", Locale.US),
		
	};

	private static final int PUBDATEFORMAT_COUNT = 3;
	
	private static final DateFormat[] UPDATE_DATEFORMATS = {
		new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"),
		new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSz", Locale.US),
		
	};
	
	private static final int DATEFORMAT_COUNT = 2;
	
	private static final String Z = "Z";
	
	private static final String GMT = "GMT";
	
	private static final StringBuilder DB_FAVORITE  = new StringBuilder(" AND (").append(Strings.DB_EXCUDEFAVORITE).append(')');

	private static Pattern imgPattern = Pattern.compile("<img src=\\s*['\"]([^'\"]+)['\"][^>]*>"); // middle () is group 1; s* is important for non-whitespaces; ' also usable
	
	private Context context;
	
	private Date lastUpdateDate;
	
	String id;

	private boolean titleTagEntered;
	
	private boolean updatedTagEntered;
	
	private boolean linkTagEntered;
	
	private boolean descriptionTagEntered;
	
	private boolean pubDateTagEntered;
	
	private boolean dateTagEntered;
	
	private boolean lastUpdateDateTagEntered;
	
	private boolean guidTagEntered;
	
	private StringBuilder title;
	
	private StringBuilder dateStringBuilder;
	
	private Date entryDate;
	
	private StringBuilder entryLink;
	
	private StringBuilder description;
	
	private StringBuilder enclosure;
	
	private Uri feedEntiresUri;
	
	private int newCount;
	
	private boolean feedRefreshed;
	
	private String feedTitle;
	
	private String feedBaseUrl;
	
	private boolean done;
	
	private Date keepDateBorder;
	
	private InputStream inputStream;
	
	private Reader reader;

	private boolean fetchImages;
	
	private boolean cancelled;
	
	private Date lastBuildDate;
	
	private long realLastUpdate;
	
	private long now;
	
	private StringBuilder guid;
	
	private boolean efficientFeedParsing;
	
	public RSSHandler(Context context) {
		KEEP_TIME = Long.parseLong(PreferenceManager.getDefaultSharedPreferences(context).getString(Strings.SETTINGS_KEEPTIME, "4"))*86400000l;
		this.context = context;
		this.efficientFeedParsing = true;
	}
	
	public void init(Date lastUpdateDate, final String id, String title, String url) {
		final long keepDateBorderTime = KEEP_TIME > 0 ? System.currentTimeMillis()-KEEP_TIME : 0;
		
		keepDateBorder = new Date(keepDateBorderTime);
		this.lastUpdateDate = lastUpdateDate;
		this.id = id;
		feedEntiresUri = FeedData.EntryColumns.CONTENT_URI(id);
		
		final String query = new StringBuilder(FeedData.EntryColumns.DATE).append('<').append(keepDateBorderTime).append(DB_FAVORITE).toString();
		
		FeedData.deletePicturesOfFeed(context, feedEntiresUri, query);
		
		context.getContentResolver().delete(feedEntiresUri, query, null);
		newCount = 0;
		feedRefreshed = false;
		feedTitle = title;
		
		int index = url.indexOf('/', 8); // this also covers https://
		
		if (index > -1) {
			feedBaseUrl = url.substring(0, index);
		} else {
			feedBaseUrl = null;
		}
		this.title = null;
		this.dateStringBuilder = null;
		this.entryLink = null;
		this.description = null;
		this.enclosure = null;
		inputStream = null;
		reader = null;
		entryDate = null;
		lastBuildDate = null;
		realLastUpdate = lastUpdateDate.getTime();
		
		done = false;
		cancelled = false;
		
		titleTagEntered = false;
		updatedTagEntered = false;
		linkTagEntered = false;
		descriptionTagEntered = false;
		pubDateTagEntered = false;
		dateTagEntered = false;
		lastUpdateDateTagEntered = false;
		now = System.currentTimeMillis();
		guid = null;
		guidTagEntered = false;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (TAG_UPDATED.equals(localName)) {
			updatedTagEntered = true;
			dateStringBuilder = new StringBuilder();
		} else if (TAG_ENTRY.equals(localName) || TAG_ITEM.equals(localName)) {
			description = null;
			entryLink = null;
			if (!feedRefreshed) {
				ContentValues values = new ContentValues();
					
				if (feedTitle == null && title != null && title.length() > 0) {
					values.put(FeedData.FeedColumns.NAME, title.toString().trim());
				}
				values.put(FeedData.FeedColumns.ERROR, (String) null);
				values.put(FeedData.FeedColumns.LASTUPDATE, System.currentTimeMillis() - 1000);
				if (lastBuildDate != null) {
					realLastUpdate = Math.max(entryDate != null && entryDate.after(lastBuildDate) ? entryDate.getTime() : lastBuildDate.getTime(), realLastUpdate);
				} else {
					realLastUpdate = Math.max(entryDate != null ? entryDate.getTime() : System.currentTimeMillis() - 1000, realLastUpdate);
				}
				values.put(FeedData.FeedColumns.REALLASTUPDATE, realLastUpdate);
				context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id), values, null, null);
				title = null;
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
		} else if ((TAG_DESCRIPTION.equals(localName) && !TAG_MEDIA_DESCRIPTION.equals(qName)) || (TAG_CONTENT.equals(localName) && !TAG_MEDIA_CONTENT.equals(qName))) {
			descriptionTagEntered = true;
			description = new StringBuilder();
		} else if (TAG_SUMMARY.equals(localName)) {
			if (description == null) {
				descriptionTagEntered = true;
				description = new StringBuilder();
			}
		} else if (TAG_PUBDATE.equals(localName)) {
			pubDateTagEntered = true;
			dateStringBuilder = new StringBuilder();
		} else if (TAG_DATE.equals(localName)) {
			dateTagEntered = true;
			dateStringBuilder = new StringBuilder();
		} else if (TAG_LASTBUILDDATE.equals(localName)) {
			lastUpdateDateTagEntered = true;
			dateStringBuilder = new StringBuilder();
		} else if (TAG_ENCODEDCONTENT.equals(localName)) {
			descriptionTagEntered = true;
			description = new StringBuilder();
		} else if (TAG_ENCLOSURE.equals(localName)) {
			if (enclosure == null) { // fetch the first enclosure only
				enclosure = new StringBuilder(attributes.getValue(Strings.EMPTY, ATTRIBUTE_URL));
				
				enclosure.append(Strings.ENCLOSURE_SEPARATOR);
				
				String value = attributes.getValue(Strings.EMPTY, ATTRIBUTE_TYPE);
				
				if (value != null) {
					enclosure.append(value);
				}
				enclosure.append(Strings.ENCLOSURE_SEPARATOR);
				value = attributes.getValue(Strings.EMPTY, ATTRIBUTE_LENGTH);
				if (value != null) {
					enclosure.append(value);
				}
			}
		} else if (TAG_GUID.equals(localName)) {
			guidTagEntered = true;
			guid = new StringBuilder();
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
		} else if (lastUpdateDateTagEntered) {
			dateStringBuilder.append(ch, start, length);
		} else if (guidTagEntered) {
			guid.append(ch, start, length);
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (TAG_TITLE.equals(localName)) {
			titleTagEntered = false;
		} else if ((TAG_DESCRIPTION.equals(localName) && !TAG_MEDIA_DESCRIPTION.equals(qName)) || TAG_SUMMARY.equals(localName) || (TAG_CONTENT.equals(localName) && !TAG_MEDIA_CONTENT.equals(qName)) || TAG_ENCODEDCONTENT.equals(localName)) {
			descriptionTagEntered = false;
		} else if (TAG_LINK.equals(localName)) {
			linkTagEntered = false;
		} else if (TAG_UPDATED.equals(localName)) {
			entryDate = parseUpdateDate(dateStringBuilder.toString());
			updatedTagEntered = false;
		} else if (TAG_PUBDATE.equals(localName)) {
			entryDate = parsePubdateDate(dateStringBuilder.toString().replace(Strings.TWOSPACE, Strings.SPACE));
			pubDateTagEntered = false;
		} else if (TAG_LASTBUILDDATE.equals(localName)) {
			lastBuildDate = parsePubdateDate(dateStringBuilder.toString().replace(Strings.TWOSPACE, Strings.SPACE));
			lastUpdateDateTagEntered = false;
		} else if (TAG_DATE.equals(localName)) {
			entryDate = parseUpdateDate(dateStringBuilder.toString());
			dateTagEntered = false;
		} else if (TAG_ENTRY.equals(localName) || TAG_ITEM.equals(localName)) {
			if (title != null && (entryDate == null || ((entryDate.after(lastUpdateDate) || !efficientFeedParsing) && entryDate.after(keepDateBorder)))) {
				ContentValues values = new ContentValues();
				
				if (entryDate != null && entryDate.getTime() > realLastUpdate) {
					realLastUpdate = entryDate.getTime();
					
					values.put(FeedData.FeedColumns.REALLASTUPDATE, realLastUpdate);
					context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id), values, null, null);
					values.clear();
				}
				
				if (entryDate != null) {
					values.put(FeedData.EntryColumns.DATE, entryDate.getTime());
					values.putNull(FeedData.EntryColumns.READDATE);
				}
				values.put(FeedData.EntryColumns.TITLE, unescapeTitle(title.toString().trim()));
				
				Vector<String> images = null;
				
				if (description != null) {
					String descriptionString = description.toString().trim().replaceAll(Strings.HTML_SPAN_REGEX, Strings.EMPTY);
					
					if (descriptionString.length() > 0) {
						if (fetchImages) {
							images = new Vector<String>(4);
							 
							Matcher matcher = imgPattern.matcher(description);
							
							while (matcher.find()) {
								String match = matcher.group(1).replace(Strings.SPACE, Strings.URL_SPACE);
								
								images.add(match);
								descriptionString = descriptionString.replace(match, new StringBuilder(Strings.FILEURL).append(FeedDataContentProvider.IMAGEFOLDER).append(Strings.IMAGEID_REPLACEMENT).append(match.substring(match.lastIndexOf('/')+1)).toString());
							}
						}
						values.put(FeedData.EntryColumns.ABSTRACT, descriptionString); 
					}
				}
				
				String enclosureString = null;
				
				StringBuilder existanceStringBuilder = new StringBuilder(FeedData.EntryColumns.LINK).append(Strings.DB_ARG);
				
				if (enclosure != null && enclosure.length() > 0) {
					enclosureString = enclosure.toString();
					values.put(FeedData.EntryColumns.ENCLOSURE, enclosureString);
					existanceStringBuilder.append(Strings.DB_AND).append(FeedData.EntryColumns.ENCLOSURE).append(Strings.DB_ARG);
				}
				
				String guidString = null;
				
				if (guid != null && guid.length() > 0) {
					guidString = guid.toString();
					values.put(FeedData.EntryColumns.GUID, guidString);
					existanceStringBuilder.append(Strings.DB_AND).append(FeedData.EntryColumns.GUID).append(Strings.DB_ARG);
				}
				
				String entryLinkString = Strings.EMPTY; // don't set this to null as we need *some* value
				
				if (entryLink != null &&  entryLink.length() > 0) {
					entryLinkString = entryLink.toString().trim();
					if (feedBaseUrl != null && !entryLinkString.startsWith(Strings.HTTP) && !entryLinkString.startsWith(Strings.HTTPS)) {
						entryLinkString = feedBaseUrl + (entryLinkString.startsWith(Strings.SLASH) ? entryLinkString : Strings.SLASH + entryLinkString);
					}
				}
				
				String[] existanceValues = enclosureString != null ? (guidString != null ? new String[] {entryLinkString, enclosureString, guidString}: new String[] {entryLinkString, enclosureString}) : (guidString != null ? new String[] {entryLinkString, guidString} : new String[] {entryLinkString});
				
				boolean skip = false;
				
				if (!efficientFeedParsing) {
					if (context.getContentResolver().update(feedEntiresUri, values, existanceStringBuilder.toString()+" AND "+FeedData.EntryColumns.DATE+"<"+entryDate.getTime(), existanceValues) == 1) {
						newCount++;
						skip = true;
					} else {
						values.remove(FeedData.EntryColumns.READDATE);
						// continue with the standard procedure but don't reset the read-date
					}
				}
				
				if (!skip && ((entryLinkString.length() == 0 && guidString == null) || context.getContentResolver().update(feedEntiresUri, values, existanceStringBuilder.toString(), existanceValues) == 0)) {
					values.put(FeedData.EntryColumns.LINK, entryLinkString);
					if (entryDate == null) {
						values.put(FeedData.EntryColumns.DATE, now--);
					}
					
					String entryId = context.getContentResolver().insert(feedEntiresUri, values).getLastPathSegment();
					
					if (fetchImages) {
						FeedDataContentProvider.IMAGEFOLDER_FILE.mkdir(); // create images dir
						for (int n = 0, i = images != null ? images.size() : 0; n < i; n++) {
							try {
								String match = images.get(n);
								
								byte[] data = FetcherService.getBytes(new URL(images.get(n)).openStream());
								
								FileOutputStream fos = new FileOutputStream(new StringBuilder(FeedDataContentProvider.IMAGEFOLDER).append(entryId).append(Strings.IMAGEFILE_IDSEPARATOR).append(match.substring(match.lastIndexOf('/')+1)).toString());
								
								fos.write(data);
								fos.close();
							} catch (Exception e) {

							}
						}
					}
					
					newCount++;
				} else if (entryDate == null && efficientFeedParsing) {
					cancel();
				}
			} else if (efficientFeedParsing) {
				cancel();
			}
			description = null;
			title = null;
			enclosure = null;
			guid = null;
		} else if (TAG_RSS.equals(localName) || TAG_RDF.equals(localName) || TAG_FEED.equals(localName)) {
			done = true;
		} else if (TAG_GUID.equals(localName)) {
			guidTagEntered = false;
		}
	}
	
	public int getNewCount() {
		return newCount;
	}
	
	public boolean isDone() {
		return done;
	}
	
	public boolean isCancelled() {
		return cancelled;
	}
	
	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
		reader = null;
	}
	
	public void setReader(Reader reader) {
		this.reader = reader;
		inputStream = null;
	}
	
	private void cancel() {
		if (!cancelled) {
			cancelled = true;
			done = true;
			if (inputStream != null) {
				try {
					inputStream.close(); // stops all parsing
				} catch (IOException e) {
					
				}
			} else if (reader != null) {
				try {
					reader.close(); // stops all parsing
				} catch (IOException e) {
					
				}
			}
		}
	}

	public void setFetchImages(boolean fetchImages) {
		this.fetchImages = fetchImages;
	}
	
	private static Date parseUpdateDate(String string) {
		string = string.replace(Z, GMT);
		for (int n = 0; n < DATEFORMAT_COUNT; n++) {
			try {
				return UPDATE_DATEFORMATS[n].parse(string);
			} catch (ParseException e) { } // just do nothing
		}
		return null;
	}
	
	private static Date parsePubdateDate(String string) {
		for (int n = 0; n < TIMEZONES_COUNT; n++) {
			string = string.replace(TIMEZONES[n], TIMEZONES_REPLACE[n]);
		}
		for (int n = 0; n < PUBDATEFORMAT_COUNT; n++) {
			try {
				return PUBDATE_DATEFORMATS[n].parse(string);
			} catch (ParseException e) { } // just do nothing
		}
		return null;
	}
	
	private static String unescapeTitle(String title) {
		String result = title.replace(Strings.AMP_SG, Strings.AMP).replaceAll(Strings.HTML_TAG_REGEX, Strings.EMPTY).replace(Strings.HTML_LT, Strings.LT).replace(Strings.HTML_GT, Strings.GT).replace(Strings.HTML_QUOT, Strings.QUOT).replace(Strings.HTML_APOSTROPHE, Strings.APOSTROPHE);
		
		if (result.indexOf(ANDRHOMBUS) > -1) {
			return Html.fromHtml(result, null, null).toString();
		} else {
			return result;
		}
	}

	public void setEfficientFeedParsing(boolean efficientFeedParsing) {
		this.efficientFeedParsing = efficientFeedParsing;
	}
	
}
