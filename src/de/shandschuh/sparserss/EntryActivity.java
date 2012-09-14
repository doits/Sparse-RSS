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

import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.animation.Animation;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
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
	
	private static final String DATE = "(date=";
	
	private static final String AND_ID = " and _id";
	
	private static final String ASC = "date asc, _id desc limit 1";
	
	private static final String DESC = "date desc, _id asc limit 1";
	
	private static final String CSS = "<head><style type=\"text/css\">body {max-width: 100%}\nimg {max-width: 100%; height: auto;}\npre {white-space: pre-wrap;}</style></head>";
	
	private static final String FONT_START = CSS+"<body link=\"#97ACE5\" text=\"#C0C0C0\">";
	
	private static final String FONT_FONTSIZE_START = CSS+"<body link=\"#97ACE5\" text=\"#C0C0C0\"><font size=\"+";
	
	private static final String FONTSIZE_START = "<font size=\"+";
	
	private static final String FONTSIZE_MIDDLE = "\">";
	
	private static final String FONTSIZE_END = "</font>";
	
	private static final String FONT_END = "</font></body>";
	
	private static final String BODY_START = "<body>";
	
	private static final String BODY_END = "</body>";
	
	private static final int BUTTON_ALPHA = 180;

	private static final String IMAGE_ENCLOSURE = "[@]image/";
	
	private static final String TEXTPLAIN = "text/plain";
	
	private int titlePosition;
	
	private int datePosition;
	
	private int abstractPosition;
	
	private int linkPosition;
	
	private int feedIdPosition;
	
	private int favoritePosition;
	
	private int readDatePosition;
	
	private int enclosurePosition;
	
	private String _id;
	
	private String _nextId;
	
	private String _previousId;
	
	private Uri uri;
	
	private Uri parentUri;
	
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
	
	private ImageButton playButton;
	
	int scrollX;
	
	int scrollY;
	
	private String link;
	
	private LayoutParams layoutParams;
	
	LinearLayout buttonPanel;
	
	private Handler handler;
	
	private SimpleTask buttonHideTask;
	
	private View content;
	
	private SharedPreferences preferences;
	
	private boolean localPictures;
	
	private TextView titleTextView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (MainTabActivity.isLightTheme(this)) {
			setTheme(R.style.Theme_Light);
		}
		
		super.onCreate(savedInstanceState);
		
		int titleId = -1;
		
		if (MainTabActivity.POSTGINGERBREAD) {
			canShowIcon = true;
			setContentView(R.layout.entry);
			try {
				/* This is a trick as com.android.internal.R.id.action_bar_title is not directly accessible */
				titleId = (Integer) Class.forName("com.android.internal.R$id").getField("action_bar_title").get(null);
			} catch (Exception exception) {
				
			}
		} else {
			canShowIcon = requestWindowFeature(Window.FEATURE_LEFT_ICON);
			setContentView(R.layout.entry);
			titleId = android.R.id.title;
		}
		
		try {
			titleTextView = (TextView) findViewById(titleId);
			titleTextView.setSingleLine(true);
			titleTextView.setHorizontallyScrolling(true);
			titleTextView.setMarqueeRepeatLimit(1);
			titleTextView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
			titleTextView.setFocusable(true);
			titleTextView.setFocusableInTouchMode(true);
		} catch (Exception e) {
			// just in case for non standard android, nullpointer etc
		}
		
		uri = getIntent().getData();
		parentUri = FeedData.EntryColumns.PARENT_URI(uri.getPath());
		showRead = getIntent().getBooleanExtra(EntriesListActivity.EXTRA_SHOWREAD, true);
		iconBytes = getIntent().getByteArrayExtra(FeedData.FeedColumns.ICON);
		feedId = 0;
		
		Cursor entryCursor = getContentResolver().query(uri, null, null, null, null);
		
		titlePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.TITLE);
		datePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.DATE);
		abstractPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.ABSTRACT);
		linkPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.LINK);
		feedIdPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.FEED_ID);
		favoritePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.FAVORITE);
		readDatePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.READDATE);
		enclosurePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.ENCLOSURE);
		
		entryCursor.close();
		if (RSSOverview.notificationManager == null) {
			RSSOverview.notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		}
		
		buttonPanel = (LinearLayout) findViewById(R.id.button_panel);
		nextButton = (ImageButton) findViewById(R.id.next_button);
		urlButton = (ImageButton) findViewById(R.id.url_button);
		urlButton.setAlpha(BUTTON_ALPHA+30);
		previousButton = (ImageButton) findViewById(R.id.prev_button);
		playButton = (ImageButton) findViewById(R.id.play_button);
		playButton.setAlpha(BUTTON_ALPHA);
		
		viewFlipper = (ViewFlipper) findViewById(R.id.content_flipper);
		
		
		
		layoutParams = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		
		webView = new WebView(this);
		
		viewFlipper.addView(webView, layoutParams);
		
		OnKeyListener onKeyEventListener = new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					if (keyCode == 92 || keyCode == 94) {
						scrollUp();
						return true;
					} else if (keyCode == 93 || keyCode == 95) {
						scrollDown();
						return true;
					}
				}
				return false;
			}
		};
		webView.setOnKeyListener(onKeyEventListener);
		
		content = findViewById(R.id.entry_content);
		
		webView0 = new WebView(this);
		webView0.setOnKeyListener(onKeyEventListener);
		
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		final boolean gestures = preferences.getBoolean(Strings.SETTINGS_GESTURESENABLED, true);
		
		final GestureDetector gestureDetector = new GestureDetector(this, new OnGestureListener() {
			public boolean onDown(MotionEvent e) {
				showButtons();
				return false;
			}
	
			public boolean onFling(MotionEvent e1, MotionEvent e2,
					float velocityX, float velocityY) {
				if (gestures) {
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
				}
				return false;
			}
	
			public void onLongPress(MotionEvent e) {
				showButtons();
			}
	
			public boolean onScroll(MotionEvent e1, MotionEvent e2,
					float distanceX, float distanceY) {
				showButtons();
				return false;
			}
	
			public void onShowPress(MotionEvent e) {
	
			}
	
			public boolean onSingleTapUp(MotionEvent e) {
				return false;
			}
		});
		
		OnTouchListener onTouchListener = new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}
		};
		
		webView.setOnTouchListener(onTouchListener);
		
		content.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				gestureDetector.onTouchEvent(event);
				return true; // different to the above one!
			}
		});
		
		webView0.setOnTouchListener(onTouchListener);
		
		scrollX = 0;
		scrollY = 0;
		
		handler = new Handler();
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);
		webView.restoreState(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (RSSOverview.notificationManager != null) {
			RSSOverview.notificationManager.cancel(0);
		}
		uri = getIntent().getData();
		parentUri = FeedData.EntryColumns.PARENT_URI(uri.getPath());
		if (MainTabActivity.POSTGINGERBREAD) {
			CompatibilityHelper.onResume(webView);
		}
		reload();
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}
	
	private void showButtons() {
		buttonPanel.setVisibility(View.VISIBLE);
		
		if (buttonHideTask != null) {
			buttonHideTask.cancel();
		}
		buttonHideTask = generateHideTimerTask();
		handler.postDelayed(buttonHideTask, 2000);
	}
	
	private SimpleTask generateHideTimerTask() {
		return new SimpleTask() {
			@Override
			public void runControlled() {
				if (webView.getBottom() > buttonPanel.getTop()) {
					buttonPanel.setVisibility(View.GONE);
				}
			}
		};
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
				if (titleTextView != null) {
					titleTextView.requestFocus(); // restart ellipsize
				}
				
				int _feedId = entryCursor.getInt(feedIdPosition);
				
				if (feedId != _feedId) {
					if (feedId != 0) {
						iconBytes = null; // triggers re-fetch of the icon
					}
					feedId = _feedId;
				}
				
				if (canShowIcon) {
					if (iconBytes == null || iconBytes.length == 0) {
						Cursor iconCursor = getContentResolver().query(FeedData.FeedColumns.CONTENT_URI(Integer.toString(feedId)), new String[] {FeedData.FeedColumns._ID, FeedData.FeedColumns.ICON}, null, null, null);
						
						if (iconCursor.moveToFirst()) {
							iconBytes = iconCursor.getBlob(1);
						}
						iconCursor.close();
					}
					
					if (iconBytes != null && iconBytes.length > 0) {
						int bitmapSizeInDip = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, getResources().getDisplayMetrics());
						Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
						if (bitmap != null) {
							if (bitmap.getHeight() != bitmapSizeInDip) {
								bitmap = Bitmap.createScaledBitmap(bitmap, bitmapSizeInDip, bitmapSizeInDip, false);
							}
							
							if (MainTabActivity.POSTGINGERBREAD) {
								CompatibilityHelper.setActionBarDrawable(this, new BitmapDrawable(bitmap));
							} else {
								setFeatureDrawable(Window.FEATURE_LEFT_ICON, new BitmapDrawable(bitmap));
							}
						}
					}
				}
				
				long timestamp = entryCursor.getLong(datePosition);
				
				Date date = new Date(timestamp);
				
				((TextView) findViewById(R.id.entry_date)).setText(new StringBuilder(DateFormat.getDateFormat(this).format(date)).append(' ').append(DateFormat.getTimeFormat(this).format(date)));
				
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
				localPictures = abstractText.indexOf(Strings.IMAGEID_REPLACEMENT) > -1;
				
				if (localPictures) {
					abstractText = abstractText.replace(Strings.IMAGEID_REPLACEMENT, _id+Strings.IMAGEFILE_IDSEPARATOR);
				}

				if (preferences.getBoolean(Strings.SETTINGS_DISABLEPICTURES, false)) {
					abstractText = abstractText.replaceAll(Strings.HTML_IMG_REGEX, Strings.EMPTY);
					webView.getSettings().setBlockNetworkImage(true);
				} else {
					if (webView.getSettings().getBlockNetworkImage()) {
						/*
						 * setBlockNetwortImage(false) calls postSync, which takes time,
						 * so we clean up the html first and change the value afterwards
						 */
						webView.loadData(Strings.EMPTY, TEXT_HTML, UTF8);
						webView.getSettings().setBlockNetworkImage(false);
					}
				}
				
				int fontsize = Integer.parseInt(preferences.getString(Strings.SETTINGS_FONTSIZE, Strings.ONE));
				
				/*
				if (abstractText.indexOf('<') > -1 && abstractText.indexOf('>') > -1) {
					abstractText = abstractText.replace(NEWLINE, BR);
				}
				*/
				
				if (MainTabActivity.isLightTheme(this) || preferences.getBoolean(Strings.SETTINGS_BLACKTEXTONWHITE, false)) {
					if (fontsize > 0) {
						webView.loadDataWithBaseURL(null, new StringBuilder(CSS).append(FONTSIZE_START).append(fontsize).append(FONTSIZE_MIDDLE).append(abstractText).append(FONTSIZE_END).toString(), TEXT_HTML, UTF8, null);
					} else {
						webView.loadDataWithBaseURL(null, new StringBuilder(CSS).append(BODY_START).append(abstractText).append(BODY_END).toString(), TEXT_HTML, UTF8, null);
					}
					webView.setBackgroundColor(Color.WHITE);
					content.setBackgroundColor(Color.WHITE);
				} else {
					if (fontsize > 0) {
						webView.loadDataWithBaseURL(null, new StringBuilder(FONT_FONTSIZE_START).append(fontsize).append(FONTSIZE_MIDDLE).append(abstractText).append(FONT_END).toString(), TEXT_HTML, UTF8, null);
					} else {
						webView.loadDataWithBaseURL(null, new StringBuilder(FONT_START).append(abstractText).append(BODY_END).toString(), TEXT_HTML, UTF8, null);
					}
					webView.setBackgroundColor(Color.BLACK);
					content.setBackgroundColor(Color.BLACK);
				}
				
				link = entryCursor.getString(linkPosition);
				
				if (link != null && link.length() > 0) {
					urlButton.setEnabled(true);
					urlButton.setAlpha(BUTTON_ALPHA+20);
					urlButton.setOnClickListener(new OnClickListener() {
						public void onClick(View view) {
							startActivityForResult(new Intent(Intent.ACTION_VIEW, Uri.parse(link)), 0);
						}
					});
				} else {
					urlButton.setEnabled(false);
					urlButton.setAlpha(80);
				}
				
				final String enclosure = entryCursor.getString(enclosurePosition);
				
				if (enclosure != null && enclosure.length() > 6 && enclosure.indexOf(IMAGE_ENCLOSURE) == -1) {
					playButton.setVisibility(View.VISIBLE);
					playButton.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							final int position1 = enclosure.indexOf(Strings.ENCLOSURE_SEPARATOR);
							
							final int position2 = enclosure.indexOf(Strings.ENCLOSURE_SEPARATOR, position1+3);
							
							final Uri uri = Uri.parse(enclosure.substring(0, position1));
							
							if (preferences.getBoolean(Strings.SETTINGS_ENCLOSUREWARNINGSENABLED, true)) {
								Builder builder = new AlertDialog.Builder(EntryActivity.this);
								
								builder.setTitle(R.string.question_areyousure);
								builder.setIcon(android.R.drawable.ic_dialog_alert);
								if (position2+4 > enclosure.length()) {
									builder.setMessage(getString(R.string.question_playenclosure, uri, position2+4 > enclosure.length() ? Strings.QUESTIONMARKS : enclosure.substring(position2+3)));
								} else {
									try {
										builder.setMessage(getString(R.string.question_playenclosure, uri, (Integer.parseInt(enclosure.substring(position2+3)) / 1024f)+getString(R.string.kb)));
									} catch (Exception e) {
										builder.setMessage(getString(R.string.question_playenclosure, uri, enclosure.substring(position2+3)));
									}
								}
								builder.setCancelable(true);
								builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										showEnclosure(uri, enclosure, position1, position2);
									}
								});
								builder.setNeutralButton(R.string.button_alwaysokforall, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										preferences.edit().putBoolean(Strings.SETTINGS_ENCLOSUREWARNINGSENABLED, false).commit();
										showEnclosure(uri, enclosure, position1, position2);
									}
								});
								builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
									}
								});
								builder.show();
							} else {
								showEnclosure(uri, enclosure, position1, position2);
							}
						}
					});
				} else {
					playButton.setVisibility(View.GONE);
				}
				entryCursor.close();
				setupButton(previousButton, false, timestamp);
				setupButton(nextButton, true, timestamp);
				webView.scrollTo(scrollX, scrollY); // resets the scrolling
				showButtons();
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
	
	private void showEnclosure(Uri uri, String enclosure, int position1, int position2) {
		try {
			startActivityForResult(new Intent(Intent.ACTION_VIEW).setDataAndType(uri, enclosure.substring(position1+3, position2)), 0);
		} catch (Exception e) {
			try {
				startActivityForResult(new Intent(Intent.ACTION_VIEW, uri), 0); // fallbackmode - let the browser handle this
			} catch (Throwable t) {
				Toast.makeText(EntryActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
			}
		}
	}

	private void setupButton(ImageButton button, final boolean successor, long date) {
		StringBuilder queryString = new StringBuilder(DATE).append(date).append(AND_ID).append(successor ? '>' : '<').append(_id).append(')').append(OR_DATE).append(successor ? '<' : '>').append(date);
		
		if (!showRead) {
			queryString.append(Strings.DB_AND).append(EntriesListAdapter.READDATEISNULL);
		}

		Cursor cursor = getContentResolver().query(parentUri, new String[] {FeedData.EntryColumns._ID}, queryString.toString() , null, successor ? DESC : ASC);
		
		if (cursor.moveToFirst()) {
			button.setEnabled(true);
			button.setAlpha(BUTTON_ALPHA);
			
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
			button.setAlpha(60);
		}
		cursor.close();
	}
	
	private void switchEntry(String id, boolean animate, Animation inAnimation, Animation outAnimation) {
		uri = parentUri.buildUpon().appendPath(id).build();
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
			viewFlipper.addView(webView, layoutParams);
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
		if (MainTabActivity.POSTGINGERBREAD) {
			CompatibilityHelper.onPause(webView);
		}
		scrollX = webView.getScrollX();
		scrollY = webView.getScrollY();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		webView.saveState(outState);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.entry, menu);
		return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_copytoclipboard: {
				if (link != null) {
					((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setText(link);
				}
				break;
			}
			case R.id.menu_delete: {
				getContentResolver().delete(uri, null, null);
				if (localPictures) {
					FeedData.deletePicturesOfEntry(_id);
				}
				
				if (nextButton.isEnabled()) {
					nextButton.performClick();
				} else {
					if (previousButton.isEnabled()) {
						previousButton.performClick();
					} else {
						finish();
					}
				}
				break;
			}
			case R.id.menu_share: {
				if (link != null) {
					startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, link).setType(TEXTPLAIN), getString(R.string.menu_share)));
				}
				break;
			}
		}
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			if (keyCode == 92 || keyCode == 94) {
				scrollUp();
				return true;
			} else if (keyCode == 93 || keyCode == 95) {
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
