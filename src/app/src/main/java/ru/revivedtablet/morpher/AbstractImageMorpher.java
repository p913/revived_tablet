package ru.revivedtablet.morpher;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

public abstract class AbstractImageMorpher implements ImageMorpher {

	@Override
	public void drawStep(Bitmap bmpBackground, Bitmap bmpForeground,
			Rect rectBackground, Rect rectForeground, Canvas canvas, int step) {
		if (step >= 1 && step <= getStepCount() &&
				rectBackground.height() == rectForeground.height() && 
				rectBackground.width() == rectForeground.width())
			drawStepSafe(bmpBackground, bmpForeground, rectBackground, rectForeground, canvas, step);
	}
		
	protected abstract void drawStepSafe(Bitmap bmpBackground, Bitmap bmpForeground,
			Rect rectBackground, Rect rectForeground, Canvas canvas, int step);

	
	public Bitmap getNewBitmap(Bitmap src, Rect rect) {
		Bitmap res = Bitmap.createBitmap(rect.width(), rect.height(), src.getConfig());
		res.setDensity(Bitmap.DENSITY_NONE);
		Canvas c = new Canvas(res);
		c.drawBitmap(src, rect, new Rect(0, 0, res.getWidth(), res.getHeight()), null);
		return res;
	} 
}
