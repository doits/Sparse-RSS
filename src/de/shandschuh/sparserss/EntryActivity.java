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

import android.R.color;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.GestureDetector.OnGestureListener;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;
import de.shandschuh.sparserss.provider.FeedData;

public class EntryActivity extends Activity {
	/*
	private static final String NEWLINE = "\n";
	
	private static final String BR = "<br/>";
	*/
	
	private static final String TEXT_HTML = "text/html";
	
	private static final String UTF8 = "utf-8";
	
	private static final String OR_DATE = " or date ";
	
	private static final String AND_DATE = " and ((date=";
	
	private static final String AND_ID = " and _id";
	
	private static final String ASC = "date asc, _id desc limit 1";
	
	private static final String DESC = "date desc, _id asc limit 1";
	
	private static final String CSS = "<head><style type=\"text/css\">img {max-width: 100%;}</style></head>";
	
	private static final String FONT_START = CSS+"<body link=\"#97ACE5\" text=\"#D0D0D0\">";
	
	private static final String FONT_FONTSIZE_START = CSS+"<body link=\"#97ACE5\" text=\"#D0D0D0\"><font size=\"+";
	
	private static final String FONTSIZE_START = "<font size=\"+";
	
	private static final String FONTSIZE_MIDDLE = "\">";
	
	private static final String FONTSIZE_END = "</font>";
	
	private static final String FONT_END = "</font></body>";
	
	private static final String BODY_START = "<body>";
	
	private static final String BODY_END = "</body>";
	
	private static final int MENU_COPYURL_ID = 1;
	
	private static final int MENU_DELETE_ID = 2;
	
	private int titlePosition;
	
	private int datePosition;
	
	private int abstractPosition;
	
	private int linkPosition;
	
	private int feedIdPosition;
	
	private int favoritePosition;
	
	private int readDatePosition;
	
	private String _id;
	
	private String _nextId;
	
	private String _previousId;
	
	private Uri uri;
	
	private int feedId;
	
	boolean favorite;
	
	private boolean showRead;
	
	private boolean canShowIcon;
	
	private byte[] iconBytes;
	
	private WebView webView;
	
	private WebView webView0; // only needed for the animation
	
	private ViewFlipper viewFlipper;
	
	private ImageButton nextButton;
	
	private ImageButton urlButton;
	
	private ImageButton previousButton;
	
	int scrollX;
	
	int scrollY;
	
	private String link;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (MainTabActivity.isLightTheme(this)) {
			setTheme(android.R.style.Theme_Light);
		}
		
		super.onCreate(savedInstanceState);
		
		canShowIcon = requestWindowFeature(Window.FEATURE_LEFT_ICON);

		setContentView(R.layout.entry);

		try {
			TextView titleTextView = (TextView) findViewById(android.R.id.title);
			
			titleTextView.setSingleLine(true);
			titleTextView.setHorizontallyScrolling(true);
			titleTextView.setMarqueeRepeatLimit(-1);
			titleTextView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
			titleTextView.setFocusable(true);
			titleTextView.setFocusableInTouchMode(true);
		} catch (Exception e) {
			// just in case for non standard android, nullpointer etc
		}
		
		uri = getIntent().getData();
		showRead = getIntent().getBooleanExtra(EntriesListActivity.EXTRA_SHOWREAD, true);
		iconBytes = getIntent().getByteArrayExtra(FeedData.FeedColumns.ICON);
		
		Cursor entryCursor = getContentResolver().query(uri, null, null, null, null);
		
		titlePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.TITLE);
		datePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.DATE);
		abstractPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.ABSTRACT);
		linkPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.LINK);
		feedIdPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.FEED_ID);
		favoritePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.FAVORITE);
		readDatePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.READDATE);
		entryCursor.close();
		if (RSSOverview.notificationManager == null) {
			RSSOverview.notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		}
		
		viewFlipper = (ViewFlipper) findViewById(R.id.content_flipper);
		
		final GestureDetector gestureDetector = new GestureDetector(this, new OnGestureListener() {
			public boolean onDown(MotionEvent e) {
				return false;
			}

			public boolean onFling(MotionEvent e1, MotionEvent e2,
					float velocityX, float velocityY) {
				if (Math.abs(velocityY) < Math.abs(velocityX)) {
					if (velocityX > 800) {
						if (previousButton.isEnabled()) {
							previousEntry(true);
						}
					} else if (velocityX < -800) {
						if (nextButton.isEnabled()) {
							nextEntry(true);
						}
					}
				}
				return false;
			}

			public void onLongPress(MotionEvent e) {
			}

			public boolean onScroll(MotionEvent e1, MotionEvent e2,
					float distanceX, float distanceY) {
				return false;
			}

			public void onShowPress(MotionEvent e) {
				
			}

			public boolean onSingleTapUp(MotionEvent e) {
				return false;
			}
			
		});
		webView = new WebView(this);
		
		viewFlipper.addView(webView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		
		OnKeyListener onKeyEventListener = new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					if (keyCode == KeyEvent.KEYCODE_PAGE_UP || keyCode == 94) {
						scrollUp();
						return true;
					} else if (keyCode == KeyEvent.KEYCODE_PAGE_DOWN || keyCode == 95) {
						scrollDown();
						return true;
					}
				}
				return false;
			}
		};
		webView.setOnKeyListener(onKeyEventListener);
		
		OnTouchListener onTouchListener = new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}
		};
		webView.setOnTouchListener(onTouchListener);
		
		webView0 = new WebView(this);
		webView0.setOnKeyListener(onKeyEventListener);
		webView0.setOnTouchListener(onTouchListener);
		
		scrollX = 0;
		scrollY = 0;
		
		nextButton = (ImageButton) findViewById(R.id.next_button);
		urlButton = ((ImageButton) findViewById(R.id.url_button));
		urlButton.setAlpha(160);
		previousButton = (ImageButton) findViewById(R.id.prev_button);
	}

	@Override
	protected void onResume() {
		super.onResume();
		RSSOverview.notificationManager.cancel(0);
		uri = getIntent().getData();
		reload();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}

	private void reload() {
		if (_id != null && _id.equals(uri.getLastPathSegment())) {
			return;
		}
		
		_id = uri.getLastPathSegment();
		
		ContentValues values = new ContentValues();
		
		values.put(FeedData.EntryColumns.READDATE, System.currentTimeMillis());
		
		Cursor entryCursor = getContentResolver().query(uri, null, null, null, null);
		
		if (entryCursor.moveToFirst()) {
			String abstractText = entryCursor.getString(abstractPosition);
			
			if (entryCursor.isNull(readDatePosition)) {
				getContentResolver().update(uri, values, new StringBuilder(FeedData.EntryColumns.READDATE).append(Strings.DB_ISNULL).toString(), null);
			}
			if (abstractText == null) {
				String link = entryCursor.getString(linkPosition);
				
				entryCursor.close();
				finish();
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
			} else {
				setTitle(entryCursor.getString(titlePosition));
				feedId = entryCursor.getInt(feedIdPosition);
				
				if (canShowIcon) {
					if (iconBytes != null && iconBytes.length > 0) {
						setFeatureDrawable(Window.FEATURE_LEFT_ICON, new BitmapDrawable(BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length)));
					} else {
						Cursor iconCursor = getContentResolver().query(FeedData.FeedColumns.CONTENT_URI(Integer.toString(feedId)), new String[] {FeedData.FeedColumns._ID, FeedData.FeedColumns.ICON}, null, null, null);
						
						if (iconCursor.moveToFirst()) {
							iconBytes = iconCursor.getBlob(1);
							
							if (iconBytes != null && iconBytes.length > 0) {
								setFeatureDrawable(Window.FEATURE_LEFT_ICON, new BitmapDrawable(BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length)));
							}
						}
						iconCursor.close();
					}
				}
				
				long date = entryCursor.getLong(datePosition);
				
				((TextView) findViewById(R.id.entry_date)).setText(DateFormat.getDateTimeInstance().format(new Date(date)));
				
				final ImageView imageView = (ImageView) findViewById(android.R.id.icon);
				
				favorite = entryCursor.getInt(favoritePosition) == 1;
				
				imageView.setImageResource(favorite ? android.R.drawable.star_on : android.R.drawable.star_off);
				imageView.setOnClickListener(new OnClickListener() {
					public void onClick(View view) {
						favorite = !favorite;
						imageView.setImageResource(favorite ? android.R.drawable.star_on : android.R.drawable.star_off);
						ContentValues values = new ContentValues();
						
						values.put(FeedData.EntryColumns.FAVORITE, favorite ? 1 : 0);
						getContentResolver().update(uri, values, null, null);
					
					}
				});
				// loadData does not recognize the encoding without correct html-header
				abstractText = abstractText.replace(Strings.IMAGEID_REPLACEMENT, uri.getLastPathSegment()+Strings.IMAGEFILE_IDSEPARATOR);

				SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
				
				if (preferences.getBoolean(Strings.SETTINGS_DISABLEPICTURES, false)) {
					abstractText = abstractText.replaceAll(Strings.HTML_IMG_REGEX, Strings.EMPTY);
				}
				
				int fontsize = Integer.parseInt(preferences.getString(Strings.SETTINGS_FONTSIZE, Strings.ONE));
				
				/*
				if (abstractText.indexOf('<') > -1 && abstractText.indexOf('>') > -1) {
					abstractText = abstractText.replace(NEWLINE, BR);
				}
				*/
				
				if (MainTabActivity.isLightTheme(this) || preferences.getBoolean(Strings.SETTINGS_BLACKTEXTONWHITE, false)) {
					if (fontsize > 0) {
						webView.loadDataWithBaseURL(null, new StringBuilder(FONTSIZE_START).append(fontsize).append(FONTSIZE_MIDDLE).append(abstractText).append(FONTSIZE_END).toString(), TEXT_HTML, UTF8, null);
					} else {
						webView.loadDataWithBaseURL(null, new StringBuilder(CSS).append(BODY_START).append(abstractText).append(BODY_END).toString(), TEXT_HTML, UTF8, null);
					}
				} else {
					if (fontsize > 0) {
						webView.loadDataWithBaseURL(null, new StringBuilder(FONT_FONTSIZE_START).append(fontsize).append(FONTSIZE_MIDDLE).append(abstractText).append(FONT_END).toString(), TEXT_HTML, UTF8, null);
					} else {
						webView.loadDataWithBaseURL(null, new StringBuilder(FONT_START).append(abstractText).append(BODY_END).toString(), TEXT_HTML, UTF8, null);
					}
					webView.setBackgroundColor(color.black);
				}
				
				link = entryCursor.getString(linkPosition);
				
				if (link != null && link.length() > 0) {
					urlButton.setEnabled(true);
					urlButton.setOnClickListener(new OnClickListener() {
						public void onClick(View view) {
							startActivityForResult(new Intent(Intent.ACTION_VIEW, Uri.parse(link)), 0);
						}
					});
				} else {
					urlButton.setEnabled(false);
				}
				
				entryCursor.close();
				setupButton(previousButton, false, date);
				setupButton(nextButton, true, date);
				webView.scrollTo(scrollX, scrollY); // resets the scrolling
			}
		} else {
			entryCursor.close();
		}
		/*
		new Thread() {
			public void run() {
				sendBroadcast(new Intent(Strings.ACTION_UPDATEWIDGET)); // this is slow
			}
		}.start();
		*/
	}

	private void setupButton(ImageButton button, final boolean successor, long date) {
		StringBuilder queryString = new StringBuilder(FeedData.EntryColumns.FEED_ID).append('=').append(feedId).append(AND_DATE).append(date).append(AND_ID).append(successor ? '>' : '<').append(_id).append(')').append(OR_DATE).append(successor ? '<' : '>').append(date).append(')');
		
		if (!showRead) {
			queryString.append(Strings.DB_AND).append(EntriesListAdapter.READDATEISNULL);
		}

		Cursor cursor = getContentResolver().query(FeedData.EntryColumns.CONTENT_URI, new String[] {FeedData.EntryColumns._ID}, queryString.toString() , null, successor ? DESC : ASC);
		
		if (cursor.moveToFirst()) {
			button.setEnabled(true);
			button.setAlpha(90);
			
			final String id = cursor.getString(0);
			
			if (successor) {
				_nextId = id;
			} else {
				_previousId = id;
			}
			button.setOnClickListener(new OnClickListener() {
				public void onClick(View view) {
					if (successor) {
						nextEntry(false);
					} else {
						previousEntry(false);
					}
				}
			});
		} else {
			button.setEnabled(false);
			button.setAlpha(30);
		}
		cursor.close();
	}
	
	private void switchEntry(String id, boolean animate, Animation inAnimation, Animation outAnimation) {
		uri = FeedData.EntryColumns.ENTRY_CONTENT_URI(id);
		getIntent().setData(uri);
		scrollX = 0;
		scrollY = 0;
		
		if (animate) {
			WebView dummy = webView; // switch reference
			
			webView = webView0;
			webView0 = dummy;
		}
		
		reload();
		
		if (animate) {
			viewFlipper.setInAnimation(inAnimation);
			viewFlipper.setOutAnimation(outAnimation);
			viewFlipper.addView(webView, 1);
			viewFlipper.showNext();
			viewFlipper.removeViewAt(0);
		}
	}
	
	private void nextEntry(boolean animate) {
		switchEntry(_nextId, animate, Animations.SLIDE_IN_RIGHT, Animations.SLIDE_OUT_LEFT);
	}
	
	private void previousEntry(boolean animate) {
		switchEntry(_previousId, animate, Animations.SLIDE_IN_LEFT, Animations.SLIDE_OUT_RIGHT);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		scrollX = webView.getScrollX();
		scrollY = webView.getScrollY();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_COPYURL_ID, Menu.NONE, R.string.contextmenu_copyurl).setIcon(android.R.drawable.ic_menu_share);
		menu.add(0, MENU_DELETE_ID, Menu.NONE, R.string.contextmenu_delete).setIcon(android.R.drawable.ic_menu_delete);
		return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
			case MENU_COPYURL_ID: {
				if (link != null) {
					((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setText(link);
				}
				break;
			}
			case MENU_DELETE_ID: {
				getContentResolver().delete(uri, null, null);
				
				Button button = (Button) findViewById(R.id.next_button);
				
				if (button.isEnabled()) {
					button.performClick();
				} else {
					button = (Button) findViewById(R.id.prev_button);
					if (button.isEnabled()) {
						button.performClick();
					} else {
						finish();
					}
				}
				break;
			}
		}
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			if (keyCode == KeyEvent.KEYCODE_PAGE_UP || keyCode == 94) {
				scrollUp();
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_PAGE_DOWN || keyCode == 95) {
				scrollDown();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	
	private void scrollUp() {
		if (webView != null) {
			webView.pageUp(false);
		}
	}
	
	private void scrollDown() {
		if (webView != null) {
			webView.pageDown(false);
		}
	}

	/**
	 * Works around android issue 6191
	 */
	@Override
	public void unregisterReceiver(BroadcastReceiver receiver) {
		try {
			super.unregisterReceiver(receiver);
		} catch (Exception e) {
			// do nothing
		}
	}

}
