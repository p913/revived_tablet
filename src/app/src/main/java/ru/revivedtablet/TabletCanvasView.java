package ru.revivedtablet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import ru.revivedtablet.config.Configuration;
import ru.revivedtablet.morpher.ImageMorpher;
import ru.revivedtablet.morpher.MorphersFactory;
import ru.revivedtablet.widget.PlacedInLineWidget;
import ru.revivedtablet.widget.Widget;
import ru.revivedtablet.widget.Widget.InvalidateBroker;

import java.util.List;

/**
 * @author Peter
 *
 */
public class TabletCanvasView extends View implements Configuration.ConfigurationChangeListener {
	//Пауза между отдельными кадрами (шагами) при смене картинки 
	private static final int DELAY_STEP = 66; //15 кадров в секунду
	//Пауза между циклами морфинга или скроллинга  
	private static final int DELAY_BETWEEN_CYCLES = 15000;
	
	//Сколько раз скроллить картинку до смены ее на новую
	private static final int MAX_SCROLL_COUNT = 5;

	private static final int PADDING_SIZE = 5;
	
	private Bitmap bmpBackground;
	private Bitmap bmpForeground;
	
	private Rect rectBackground;
	private Rect rectForeground;
	private Rect rectDisplay;

	private int deltaX = 0;
	private int deltaY = 0;
	
	//
	private ImageProducer imageProducer;
	
	private ImageMorpher morpher;
	
	private Handler h = new Handler();
	private MorphingImpl runMorph = new MorphingImpl();
	private ScrollingImpl runScroll = new ScrollingImpl();
	
	private boolean paused = false;	
	private int step;

	private InvalidateBroker invalidateBroker = new InvalidateBroker() {
		private Runnable r = new Runnable() {
			@Override
			public void run() {
				TabletCanvasView.this.invalidate();
			}
		};

		@Override
		public void invalidate() {
			if (h.getLooper().getThread() == Thread.currentThread())
				TabletCanvasView.this.invalidate();
			else
				h.post(r);
		}
	};

	public TabletCanvasView(Context context) {
		super(context);
		
		rectDisplay = new Rect(0, 0, TabletCanvasUtils.getDisplayWidth(), TabletCanvasUtils.getDisplayHeight());
		rectBackground = new Rect(rectDisplay);
		rectForeground = new Rect(rectDisplay);
		
		imageProducer = new ImageProducer();
		
		prepareForegroundImage();
	}
	
	private void prepareForegroundImage() {
		//Верхнее изображение сделать фоном
		bmpBackground = bmpForeground;
		rectBackground.set(rectForeground);
		//Новое основное изображение
		bmpForeground = imageProducer.getNewRandomPreparedImage();
		if (bmpForeground != null) {
			if (bmpForeground.getHeight() > TabletCanvasUtils.getDisplayHeight()) {
				int top = (bmpForeground.getHeight() - TabletCanvasUtils.getDisplayHeight()) / 2;
				rectForeground.set(0, top, TabletCanvasUtils.getDisplayWidth(), top + TabletCanvasUtils.getDisplayHeight());
				deltaX = 0;
				deltaY = (deltaY==0)?1:deltaY;
			}
			else if (bmpForeground.getWidth() > TabletCanvasUtils.getDisplayWidth())  {
				int left = (bmpForeground.getWidth() - TabletCanvasUtils.getDisplayWidth()) / 2;
				rectForeground.set(left, 0, left + TabletCanvasUtils.getDisplayWidth(), TabletCanvasUtils.getDisplayHeight());
				deltaX = (deltaX==0)?1:deltaX;
				deltaY = 0;
			}
			else {
				rectForeground.set(0, 0, TabletCanvasUtils.getDisplayWidth(), TabletCanvasUtils.getDisplayHeight());
				deltaX = 0;
				deltaY = 0;
			}
				
		}
		
	} 
	
	public void pause() {
		paused = true;

		Configuration.getInstance().pause();

		for (Widget w: Configuration.getInstance().getWidgets())
			w.pause();

		h.removeCallbacks(runScroll);
		h.removeCallbacks(runMorph);
	}
	
	public void resume() {
		for (Widget w: Configuration.getInstance().getWidgets()) {
			w.resume();
		}

		Configuration.getInstance().resume();

		paused = false;
		
		h.postDelayed(runScroll, DELAY_STEP);
		
	}

	@Override
	public void onDraw(Canvas canvas) {
		if (morpher != null)
			morpher.drawStep(bmpBackground, bmpForeground, rectBackground, rectForeground, canvas, step);
		else if (bmpForeground != null)
			canvas.drawBitmap(bmpForeground, rectForeground, rectDisplay, null);

		List<Widget> widgets = Configuration.getInstance().getWidgets();

		int bottom = TabletCanvasUtils.getDisplayHeight();
		for (int line = 0; line < 3; line++) {
			int left, widgetWidth, width = 0, height = 0;
			for (Widget w : widgets) {
				if (w instanceof PlacedInLineWidget && ((PlacedInLineWidget) w).getLine() == line) {
					width += ((PlacedInLineWidget) w).getWidth();
					height = Math.max(height, ((PlacedInLineWidget) w).getHeight());
				}
			}
			left = (TabletCanvasUtils.getDisplayWidth() - width) / 2;
			for (Widget w : widgets) {
				if (w instanceof PlacedInLineWidget && ((PlacedInLineWidget) w).getLine() == line) {
					widgetWidth = ((PlacedInLineWidget) w).getWidth();
					((PlacedInLineWidget) w).setDrawRect(new Rect(left, bottom - height, left + widgetWidth, bottom) );
					left += widgetWidth;
				}
			}

			bottom -= height + PADDING_SIZE;
		}

		for (Widget w: Configuration.getInstance().getWidgets())
			w.draw(canvas);
	}
	
	@Override 
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		
		for(Widget w: Configuration.getInstance().getWidgets()) {
			w.setInvalidateBroker(null);
		}

		Configuration.getInstance().removeChangeListener(this);
	}
	
	@Override
	protected void onAttachedToWindow() {
		Log.d("onAttachedToWindow", "onAttachedToWindow");
		super.onAttachedToWindow();
		
		for(Widget w: Configuration.getInstance().getWidgets()) {
			w.setInvalidateBroker(invalidateBroker);
		}

		Configuration.getInstance().addChangeListener(this);
	}

	@Override
	public void onChange(final List<Widget> old, final List<Widget> neww) {
		h.post(new Runnable() {
			@Override
			public void run() {
				for(Widget w: old) {
					w.pause();
					w.setInvalidateBroker(null);
				}
				for(Widget w: neww) {
					w.setInvalidateBroker(invalidateBroker);
					w.resume();
				}
				invalidate();
			}
		});
	}

	public class MorphingImpl implements Runnable {
		@Override
		public void run() {
			if (morpher == null) {
				morpher = MorphersFactory.getInstance().getRandomMorpher();
				step = 1;
				
				prepareForegroundImage();
			}
			else 
				step++;
				
			invalidate();

			if (step > morpher.getStepCount()) {
				morpher.release();
				morpher = null;
				h.postDelayed(runScroll, DELAY_BETWEEN_CYCLES);
			}
			else
				h.postDelayed(this, DELAY_STEP);
			
			
		}
		
	}
	
	public class ScrollingImpl implements Runnable {
		private int scrollCounter = 0;

		@Override
		public void run() {
			boolean breakScroll = false;
			if (bmpForeground != null) {
				if (deltaY != 0) {
					rectForeground.top += deltaY;
					rectForeground.bottom += deltaY;

					if (rectForeground.top <= 0 || rectForeground.bottom >= bmpForeground.getHeight()) {
						deltaY = - deltaY;
						breakScroll = true;
					}
				}
				else if (deltaX != 0) {
					rectForeground.left += deltaX;
					rectForeground.right += deltaX;
					
					if (rectForeground.left <= 0 || rectForeground.right >= bmpForeground.getWidth()) {
						deltaX = - deltaX;
						breakScroll = true;
					}
				}
				else
					breakScroll = true;
				
				invalidate();
				
				if (breakScroll) {
					if (++scrollCounter >= MAX_SCROLL_COUNT) {
						scrollCounter = 0;
						h.postDelayed(runMorph, DELAY_BETWEEN_CYCLES);
					}
					else
						h.postDelayed(this, DELAY_BETWEEN_CYCLES);
				}
				else 
					h.postDelayed(this, DELAY_STEP);
					
			}			
		}
		
	}  
	
	
}