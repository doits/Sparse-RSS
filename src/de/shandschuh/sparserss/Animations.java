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
 */

package de.shandschuh.sparserss;

import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

public class Animations {
	/** Slide in from right */
	public static final TranslateAnimation SLIDE_IN_RIGHT = generateAnimation(1, 0);
	
	/** Slide in from left */
	public static final TranslateAnimation SLIDE_IN_LEFT = generateAnimation(-1, 0);
	
	/** Slide out to right */
	public static final TranslateAnimation SLIDE_OUT_RIGHT = generateAnimation(0, 1);
	
	/** Slide out to left */
	public static final TranslateAnimation SLIDE_OUT_LEFT = generateAnimation(0, -1);

	/** Duration of one animation */
	private static final long DURATION = 180;
	
	private static TranslateAnimation generateAnimation(float fromX, float toX) {
		TranslateAnimation transformAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, fromX, Animation.RELATIVE_TO_SELF, toX, 0, 0, 0, 0);
	
		transformAnimation.setDuration(DURATION);
		return transformAnimation;
	}
	
}
