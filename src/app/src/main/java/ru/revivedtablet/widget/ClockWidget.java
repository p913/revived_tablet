package ru.revivedtablet.widget;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import ru.revivedtablet.TabletCanvasUtils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;

public class ClockWidget implements Widget, Runnable {
	
	private static final int TIME_UPDATE_INTERVAL = 10000;
	//Число тиков {@link TIME_UPDATE_INTERVAL}, после которых надо поменять положение текста текущего времени 
	private static final int TIME_POS_MAX_TICKS = 100;
	
	
	private Rect rect = new Rect(0, 0, 32, 32);
	private InvalidateBroker invalidater;
	
	private Paint timePaint;
	private Paint datePaint;
	
	private String timeText;
	private int timeTextSize;
	private int timeTextBottom;
	private int timeTextLeft;
	private int timeTextMaxWidth;
	private int timeNewPosCounter = 0;
	private SimpleDateFormat timeFormat = new SimpleDateFormat("H:mm", Locale.getDefault());
	private SimpleDateFormat dayOfWeekFormat = new SimpleDateFormat("cccc", Locale.getDefault());
	private SimpleDateFormat dayOfMonthFormat = new SimpleDateFormat("d MMMM", Locale.getDefault());
	
	private Calendar cal = Calendar.getInstance();
	
	Handler h = new Handler(Looper.getMainLooper());

	public ClockWidget() {
		timePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		timePaint.setColor(Color.WHITE);
		timePaint.setTextSize(10);
		timePaint.setShadowLayer(2, 2, 2, Color.BLACK);
		timePaint.setTextScaleX(0.7F);

		datePaint = new Paint(timePaint);
		
		calculateBoundRect();
	}
	
	@Override
	public void draw(Canvas canvas) {
		timeText = timeFormat.format(cal.getTime());
		canvas.drawText(timeText, timeTextLeft, timeTextBottom, timePaint);
		Rect bounds = new Rect();
		timePaint.getTextBounds(timeText + "1", 0, timeText.length() + 1, bounds);
		canvas.drawText(dayOfWeekFormat.format(cal.getTime()), timeTextLeft + bounds.width(), timeTextBottom, datePaint);
		canvas.drawText(dayOfMonthFormat.format(cal.getTime()), timeTextLeft + bounds.width(), timeTextBottom - datePaint.getTextSize(), datePaint);
	}

	@Override
	public void setInvalidateBroker(InvalidateBroker broker) {
		invalidater = broker;
	}

	private void calculateBoundRect() {
		timeTextSize = TabletCanvasUtils.getDisplayHeight() / 3;
		
		timePaint.setTextSize(timeTextSize);
		datePaint.setTextSize((float)(timeTextSize * 0.3));
		
		//Ширина времени и дня недели
		Rect textMaxRect = new Rect();
		int m = 0;
		timePaint.getTextBounds("88:88 ", 0, 6, textMaxRect);
		timeTextMaxWidth = textMaxRect.width();
		
		String[] months = new DateFormatSymbols().getMonths();
	    for (String month : months) {
	    	datePaint.getTextBounds("28 " + month, 0, 3 + month.length(), textMaxRect);
	    	if (textMaxRect.width() > m)
	    		m = textMaxRect.width();
	    }
		
	    String[] weekdays = new DateFormatSymbols().getWeekdays();
	    for (String day : weekdays) {
	    	datePaint.getTextBounds(day, 0, day.length(), textMaxRect);
	    	if (textMaxRect.width() > m)
	    		m = textMaxRect.width();
	    }
	    
	    timeTextMaxWidth += m;
				
		rect = new Rect(TabletCanvasUtils.getDisplayWidth() / 10, // 10% ширина экрана отступаем слева
				TabletCanvasUtils.getDisplayHeight() / 4, // 25% высоты экрана сверху
				TabletCanvasUtils.getDisplayWidth() * 9 / 10, // 10% ширина экрана справа
				TabletCanvasUtils.getDisplayHeight() * 3 / 4 // 25% высоты экрана снизу
				);
		//Log.d("timeCornerBounds", rect.toString());
		
		newTimeCornerPoint();
		
	}
	
	private void newTimeCornerPoint() {
		timeTextLeft = rect.left + (int)(Math.random() * (rect.width() - timeTextMaxWidth));
		timeTextBottom = rect.bottom - (int)(Math.random() * (rect.height() - timeTextSize));
		//Log.d("timeTextLeft", String.valueOf(timeTextLeft));
		//Log.d("timeTextBottom", String.valueOf(timeTextBottom));
	}

	@Override
	public void run() {
		updateTime();
		h.postDelayed(this, TIME_UPDATE_INTERVAL);
		
	}
	
	public void updateTime() {
		boolean needInvalidate = false;
		
		if (timeNewPosCounter++ > TIME_POS_MAX_TICKS) {
			newTimeCornerPoint();
			timeNewPosCounter = 0;
			needInvalidate = true;
		}

		cal.setTimeInMillis(System.currentTimeMillis());
		String curTimeText = timeFormat.format(cal.getTime());
		if (!curTimeText.equals(timeText)) {
			needInvalidate = true;
		}
		
		if (needInvalidate && invalidater != null) {
			invalidater.invalidate();
		}
	}

	@Override
	public void pause() {
		h.removeCallbacks(this);		
	}

	@Override
	public void resume() {
		updateTime();
		h.postDelayed(this, TIME_UPDATE_INTERVAL);		
	}


}
