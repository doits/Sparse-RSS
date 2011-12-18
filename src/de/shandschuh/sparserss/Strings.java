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

import de.shandschuh.sparserss.provider.FeedData;

public final class Strings {
	public static final String PACKAGE = "de.shandschuh.sparserss";
	
	public static final String SETTINGS_REFRESHINTERVAL = "refresh.interval";
	
	public static final String SETTINGS_NOTIFICATIONSENABLED = "notifications.enabled";
	
	public static final String SETTINGS_REFRESHENABLED = "refresh.enabled";
	
	public static final String SETTINGS_REFRESHONPENENABLED = "refreshonopen.enabled";
	
	public static final String SETTINGS_NOTIFICATIONSRINGTONE = "notifications.ringtone";
	
	public static final String SETTINGS_NOTIFICATIONSVIBRATE = "notifications.vibrate";
	
	public static final String SETTINGS_PRIORITIZE = "contentpresentation.prioritize";
	
	public static final String SETTINGS_SHOWTABS = "tabs.show";
	
	public static final String SETTINGS_FETCHPICTURES = "pictures.fetch";
	
	public static final String SETTINGS_PROXYENABLED = "proxy.enabled";
	
	public static final String SETTINGS_PROXYPORT = "proxy.port";
	
	public static final String SETTINGS_PROXYHOST = "proxy.host";
	
	public static final String SETTINGS_PROXYWIFIONLY = "proxy.wifionly";
	
	public static final String SETTINGS_KEEPTIME = "keeptime";
	
	public static final String SETTINGS_BLACKTEXTONWHITE = "blacktextonwhite";
	
	public static final String SETTINGS_LIGHTTHEME = "lighttheme";
	
	public static final String SETTINGS_FONTSIZE = "fontsize";
	
	public static final String SETTINGS_STANDARDUSERAGENT = "standarduseragent";
	
	public static final String SETTINGS_DISABLEPICTURES = "pictures.disable";
	
	public static final String SETTINGS_HTTPHTTPSREDIRECTS = "httphttpsredirects";
	
	public static final String SETTINGS_OVERRIDEWIFIONLY = "overridewifionly";
	
	public static final String ACTION_REFRESHFEEDS = "de.shandschuh.sparserss.REFRESH";
	
	public static final String ACTION_UPDATEWIDGET = "de.shandschuh.sparserss.widget.UPDATE";
	
	public static final String ACTION_RESTART = "de.shandschuh.sparserss.RESTART";
	
	public static final String FEEDID = "feedid";
	
	public static final String DB_ISNULL = " IS NULL";
	
	public static final String DB_DESC = " DESC";
	
	public static final String DB_ARG = "=?";
	
	public static final String DB_AND = " AND ";
	
	public static final String DB_EXCUDEFAVORITE = new StringBuilder(FeedData.EntryColumns.FAVORITE).append(Strings.DB_ISNULL).append(" OR ").append(FeedData.EntryColumns.FAVORITE).append("=0").toString();
	
	public static final String EMPTY = "";

	public static final String HTTP = "http://";
	
	public static final String HTTPS = "https://";
	
	public static final String _HTTP = "http";
	
	public static final String _HTTPS = "https";

	public static final String PROTOCOL_SEPARATOR = "://";

	public static final String FILE_FAVICON = "/favicon.ico";
	
	public static final String SPACE = " ";
	
	public static final String TWOSPACE = "  ";
	
	public static final String HTML_TAG_REGEX = "<(.|\n)*?>";
	
	public static final String AND_CHAR = "&";
	
	public static final String AND_HTML = "&amp;";
	
	public static final String FILEURL = "file://";
	
	public static final String IMAGEFILE_IDSEPARATOR = "__";
	
	public static final String IMAGEID_REPLACEMENT = "##ID##";

	public static final String DEFAULTPROXYPORT = "8080";

	public static final String URL_SPACE = "%20";

	public static final String HTML_SPAN_REGEX = "<[/]?[ ]?span(.|\n)*?>";
	
	public static final String HTML_IMG_REGEX = "<[/]?[ ]?img(.|\n)*?>";

	public static final String ONE = "1";

	public static final Object THREENEWLINES = "\n\n\n";

	public static final String PREFERENCE_LICENSEACCEPTED = "license.accepted";
	
	public static final String HTML_LT = "&lt;";
	
	public static final String HTML_GT = "&gt;";
	
	public static final String LT = "<";
	
	public static final String GT = ">";

	protected static final String TRUE = "true";
	
	protected static final String FALSE = "false";
	
	public static final String READDATE_GREATERZERO = FeedData.EntryColumns.READDATE+">0";

}
