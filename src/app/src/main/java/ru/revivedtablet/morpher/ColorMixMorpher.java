package ru.revivedtablet.morpher;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;

public class ColorMixMorpher extends AbstractImageMorpher {
	
	private final static int STEP_COUNT = 20;
	
	private Bitmap copyForSetAlpha;

	@Override
	public int getStepCount() {
		return STEP_COUNT;
	}

	@Override
	public void drawStepSafe(Bitmap bmpBackground, Bitmap bmpForeground, Rect rectBackground, Rect rectForeground, Canvas canvas, int step) {
		try {
		if (step == 1) 
			copyForSetAlpha = getNewBitmap(bmpForeground, rectForeground);
			
		int[] buf = new int[copyForSetAlpha.getWidth()];
		int alpha = (((byte)((255 / (STEP_COUNT + 1)) * step)) << 24);
		for (int row = 0; row < copyForSetAlpha.getHeight(); row++) {
			bmpForeground.getPixels(buf, 0, copyForSetAlpha.getWidth(), rectForeground.left, rectForeground.top + row, copyForSetAlpha.getWidth(), 1);

			for (int i = 0; i < buf.length; i++)
				buf[i] = alpha + (buf[i] & 0xFFFFFF);

			copyForSetAlpha.setPixels(buf, 0, copyForSetAlpha.getWidth(), 0, row, copyForSetAlpha.getWidth(), 1);
		}

		canvas.drawBitmap(bmpBackground, rectBackground, new Rect(0, 0, rectBackground.width(), rectBackground.height()), null);
		copyForSetAlpha.setHasAlpha(true);
		canvas.drawBitmap(copyForSetAlpha, 0, 0, null);
		} 
		catch (IllegalArgumentException e) {
			e.printStackTrace();
			Log.e("bmpBackground.getHeight()", String.valueOf(bmpBackground.getHeight()));
			Log.e("bmpBackground.getWidth()", String.valueOf(bmpBackground.getWidth()));
			
			Log.e("rectBackground", "" + rectBackground);
			
			Log.e("bmpForeground.getHeight()", String.valueOf(bmpForeground.getHeight()));
			Log.e("bmpForeground.getWidth()", String.valueOf(bmpForeground.getWidth()));
			
			Log.e("rectForeground", "" + rectForeground);
			throw e;
			
		}
	}
	
	@Override
	public void release() {
		copyForSetAlpha = null;
	}
}

