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

package de.shandschuh.sparserss.widget;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import de.shandschuh.sparserss.R;

public class ColorPickerDialogPreference extends DialogPreference {
	private SeekBar redSeekBar;
	
	private SeekBar greenSeekBar;
	
	private SeekBar blueSeekBar;
	
	private SeekBar transparencySeekBar;
	
	int color;
	
	public ColorPickerDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		color = SparseRSSAppWidgetProvider.STANDARD_BACKGROUND;
	}
	
	@Override
	protected View onCreateDialogView() {
		final View view = super.onCreateDialogView();
		
		view.setBackgroundColor(color);
		
		redSeekBar = (SeekBar) view.findViewById(R.id.seekbar_red);
		greenSeekBar = (SeekBar) view.findViewById(R.id.seekbar_green);
		blueSeekBar = (SeekBar) view.findViewById(R.id.seekbar_blue);
		transparencySeekBar = (SeekBar) view.findViewById(R.id.seekbar_transparency);
		
		int _color = color;
		
		transparencySeekBar.setProgress(((_color / 0x01000000)*100)/255);
		_color %= 0x01000000;
		redSeekBar.setProgress(((_color / 0x00010000)*100)/255);
		_color %= 0x00010000;
		greenSeekBar.setProgress(((_color / 0x00000100)*100)/255);
		_color %= 0x00000100;
		blueSeekBar.setProgress((_color*100)/255);
		
		OnSeekBarChangeListener onSeekBarChangeListener = new OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				int red = (redSeekBar.getProgress()*255) / 100;
				
				int green = (greenSeekBar.getProgress()*255) / 100;
				
				int blue = (blueSeekBar.getProgress()*255) / 100;
				
				int transparency = (transparencySeekBar.getProgress()*255) / 100;
				
				color = transparency*0x01000000 + red*0x00010000 + green*0x00000100 + blue;
				view.setBackgroundColor(color);
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				
			}
		};
		
		redSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
		greenSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
		blueSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
		transparencySeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
		return view;
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			persistInt(color);
		}
		super.onDialogClosed(positiveResult);
	}

}
