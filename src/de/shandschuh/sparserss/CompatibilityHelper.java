/**
 * Sparse rss
 *
 * Copyright (c) 2012 Stefan Handschuh
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
 *
 * ==============================================================================
 * This helper class is needed for older Android versions that verify all
 * existing references on startup.
 */

package de.shandschuh.sparserss;

import android.app.Activity;
import android.graphics.drawable.Drawable;

public class CompatibilityHelper {
	private static final String METHOD_GETACTIONBAR = "getActionBar";
	
	private static final String METHOD_SETICON = "setIcon";
	
	public static void setActionBarDrawable(Activity activity, Drawable drawable) {
		try {
			Object actionBar = Activity.class.getMethod(METHOD_GETACTIONBAR).invoke(activity);
			
			actionBar.getClass().getMethod(METHOD_SETICON, Drawable.class).invoke(actionBar, drawable);
		} catch (Exception e) {
			
		}

	}
}
