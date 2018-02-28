package ru.revivedtablet.morpher;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

public class HorizontalLinesMorpher extends AbstractImageMorpher {
	
	protected final static int STEP_COUNT = 20;
	
	protected Bitmap current = null;

	@Override
	public int getStepCount() {
		return STEP_COUNT;
	}

	@Override
	public void drawStepSafe(Bitmap bmpBackground, Bitmap bmpForeground, Rect rectBackground, Rect rectForeground, Canvas canvas, int step) {
		if (step == 1)
			current = getNewBitmap(bmpBackground, rectBackground);
		
		int[] buf = new int[current.getWidth()];
		int y;
		for (int i = 0; i <= current.getHeight() / (STEP_COUNT + 1); i++) {
			y = (i * (STEP_COUNT + 1)) + step - 1;
			if (y < current.getHeight()) {
				bmpForeground.getPixels(buf, 0, rectForeground.width(), rectForeground.left, rectForeground.top + y, rectForeground.width(), 1);
				current.setPixels(buf, 0, current.getWidth(), 0, y, current.getWidth(), 1);
			}
		}
		
		if (current != null) 
			canvas.drawBitmap(current, 0, 0, null);
	}

	@Override
	public void release() {
		current = null;		
	}
	


}
