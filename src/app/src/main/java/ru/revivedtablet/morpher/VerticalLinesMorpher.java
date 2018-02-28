package ru.revivedtablet.morpher;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

public class VerticalLinesMorpher extends HorizontalLinesMorpher {

	@Override
	public void drawStepSafe(Bitmap bmpBackground, Bitmap bmpForeground, Rect rectBackground, Rect rectForeground, Canvas canvas, int step) {
		if (step == 1)
			current = getNewBitmap(bmpBackground, rectBackground);
		
		int x;
		for (int col = 0; col <= current.getWidth() / (STEP_COUNT + 1); col++) {
			for (int row = 0; row < current.getHeight(); row ++) {
				x = (col * (STEP_COUNT + 1)) + step - 1;
				if (x < current.getWidth())
					current.setPixel(x, row, bmpForeground.getPixel(rectForeground.left + x, rectForeground.top + row));
			}
		}
		
		if (current != null) 
			canvas.drawBitmap(current, 0, 0, null);
	}	

}
