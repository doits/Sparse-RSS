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

package de.shandschuh.sparserss.handler;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

import de.shandschuh.sparserss.provider.FeedDataContentProvider;

public class PictureFilenameFilter implements FilenameFilter {
	private static final String REGEX = "__[^\\.]*\\.[A-Za-z]*";
	
	private Pattern pattern;
	
	public PictureFilenameFilter(String entryId) {
		setEntryId(entryId);
	}

	public PictureFilenameFilter() {

	}

	public void setEntryId(String entryId) {
		pattern = Pattern.compile(entryId+REGEX);
	}

	public boolean accept(File dir, String filename) {
		if (dir != null && dir.equals(FeedDataContentProvider.IMAGEFOLDER_FILE)) { // this should be always true but lets check it anyway
			return pattern.matcher(filename).find();
		} else {
			return false;
		}
	}

}
