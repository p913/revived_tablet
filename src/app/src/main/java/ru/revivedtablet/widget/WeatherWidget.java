package ru.revivedtablet.widget;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ru.revivedtablet.ImageUtils;
import ru.revivedtablet.R;
import ru.revivedtablet.RevivedTabletApp;
import ru.revivedtablet.TabletCanvasUtils;
import ru.revivedtablet.weather.WeatherDatabase;
import ru.revivedtablet.weather.WeatherProvider;
import ru.revivedtablet.weather.WeatherProvider.WeatherCursorWrapper;
import ru.revivedtablet.weather.WeatherRemoteSource;

import android.animation.ObjectAnimator;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;

public class WeatherWidget implements Widget {
	
	//Интервал на периодическое обновление погоды
	private static final int UPDATE_FROM_SERVER_INTERVAL = 1000 * 60 * 60 * 4;
	
	//Пауза на обновление с сервера после просыпания - чтобы успел подняться wifi   
	private static final int PAUSE_ONRESUME_INTERVAL = 1000 * 5;
	
	//Сколько секунд показывать каждый день прогноза для дней, начиная со 2   
	private static final int SCROLL_DAY_INTERVAL = 1000 * 5;

	//Прогноз на не менее MAX_DAYS_COUNT дней - 2 показывавем всегда и остальное по дням
	private static int MAX_DAYS_COUNT = 9;
	
	//Высота виджета
	private static int WIDGET_HEIGHT = 80;

	//Пороги силы ветра (м/с) - умеренный (оранжевая стрелка) и сильный (красная стрелка)
	private static int WIND_THRESHOLD_NORMAL = 5;
	private static int WIND_THRESHOLD_STRONG = 10;

	private WeatherProvider weatherProvider;
	
	private Handler h = new Handler(Looper.getMainLooper());
	
	private Rect rect = new Rect(0, 0, 1, WIDGET_HEIGHT);
	
	private Bitmap bmpDayScroll1;
	private Bitmap bmpDayScroll2;
	
	private List<Bitmap> listDayBitmaps = new ArrayList<Bitmap>();
	
	private Paint forecastPaint;
	private Paint dayOfWeekPaint;
	private Paint statusTextPaint;
	private Paint blackAlphaPaint;
	
	private SimpleDateFormat forecastFormat = new SimpleDateFormat("c',' d", Locale.getDefault());
	private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("d MMMM H:mm", Locale.getDefault());
	
	private InvalidateBroker invalidater;
	
	private String cityId;
	private String cityName;
	private Date timestamp = new Date();
	
	private int currentDayIndex = 2; 
	
	private int daysScrollDy;
	private int daysScrollTop;
	private int daysScrollPercent;
	
	private ObjectAnimator dayScrollAnimation;
	
	private Runnable runUpdateForecast = new Runnable() {
		@Override
		public void run() {
			loadWeatherFromServer();
			h.postDelayed(this, UPDATE_FROM_SERVER_INTERVAL);
		}
	};
	
	private Runnable runDaysScroll = new Runnable() {
		@Override
		public void run() {
			dayScrollAnimation.start();
			
			h.postDelayed(this, SCROLL_DAY_INTERVAL);
		}
	};	

	public WeatherWidget(WeatherRemoteSource source) {

		weatherProvider = new WeatherProvider(source);

		statusTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		statusTextPaint.setColor(Color.WHITE);
		statusTextPaint.setTextAlign(Align.RIGHT);
		statusTextPaint.setTextSize(ImageUtils.URGE_TEXT_SIZE_SMALL);

		dayOfWeekPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		dayOfWeekPaint.setColor(Color.WHITE);
		dayOfWeekPaint.setTextSize(ImageUtils.URGE_TEXT_SIZE_NORMAL);
		dayOfWeekPaint.setTextAlign(Align.CENTER);

		forecastPaint = new Paint(dayOfWeekPaint);
		forecastPaint.setTextScaleX(ImageUtils.URGE_TEXT_SCALEX);
		forecastPaint.setTextAlign(Align.CENTER);

		blackAlphaPaint = new Paint();
		blackAlphaPaint.setStyle(Style.FILL);
		blackAlphaPaint.setColor(ImageUtils.URGE_FILL_COLOR);

		bmpDayScroll1 = BitmapFactory.decodeResource(RevivedTabletApp.getContext().getResources(), R.drawable.day_scroll_1);
		bmpDayScroll1.setDensity(Bitmap.DENSITY_NONE);
		bmpDayScroll2 = BitmapFactory.decodeResource(RevivedTabletApp.getContext().getResources(), R.drawable.day_scroll_2);
		bmpDayScroll2.setDensity(Bitmap.DENSITY_NONE);

		daysScrollDy = (rect.height() - bmpDayScroll1.getHeight() * (MAX_DAYS_COUNT - 2)) / (MAX_DAYS_COUNT - 1);
		daysScrollTop = (rect.height() - (bmpDayScroll1.getHeight() * (MAX_DAYS_COUNT - 2) + daysScrollDy * (MAX_DAYS_COUNT - 2))) / 2;

		dayScrollAnimation = ObjectAnimator.ofInt(this, "DaysScrollPercent", 0, 100);
		dayScrollAnimation.setDuration(500);
	}
	
	public void setDaysScrollPercent(int value) {
		if (daysScrollPercent != value) {
			daysScrollPercent = value;
			
			if (daysScrollPercent == 100) {
				daysScrollPercent = 0;
				if (++currentDayIndex >= Math.min(listDayBitmaps.size(), MAX_DAYS_COUNT))
					currentDayIndex = 2;
			}
			
			if (invalidater != null)
				invalidater.invalidate();
		}
	}
	
	public String getCityId() {
		return cityId;
	}

	public String getForecastStatus() {
		if (cityName == null || timestamp == null)
			return "Нет данных о текущем прогнозе";
		else 
			return String.format("Текущий прогноз в г.%s (код %s) на %s", cityName, cityId, dateTimeFormat.format(timestamp));
	}

	@Override
	public void draw(Canvas canvas) {
		
		int x = rect.left;
		if (listDayBitmaps.size() >= 2) {
			//Рисуем первые 2 дня прогноза всегда одинаково
			canvas.drawBitmap(listDayBitmaps.get(0), x, 0, null);
			x += listDayBitmaps.get(0).getWidth() + ImageUtils.URGE_PADDING_BIG;
			canvas.drawBitmap(listDayBitmaps.get(1), x, 0, null);
			x += listDayBitmaps.get(1).getWidth() + ImageUtils.URGE_PADDING_BIG;
		}
		
		//Последний день - один из оставшихся дней прогноза
		if (listDayBitmaps.size() > currentDayIndex) {
			int scrollWidth = ImageUtils.URGE_PADDING_SMALL + bmpDayScroll1.getWidth() + ImageUtils.URGE_PADDING_SMALL;
			canvas.drawRect(x, 0, x + scrollWidth, listDayBitmaps.get(currentDayIndex).getHeight(), blackAlphaPaint);
			for (int i = 2; i < Math.min(listDayBitmaps.size(), MAX_DAYS_COUNT); i++)
				canvas.drawBitmap((i == currentDayIndex)?bmpDayScroll2:bmpDayScroll1, 
						x + ImageUtils.URGE_PADDING_SMALL, 
						daysScrollTop + (i - 2) * (bmpDayScroll1.getHeight() + daysScrollDy), null);
			x += scrollWidth;
			if (daysScrollPercent > 0) {
				int nextDayIndex = (currentDayIndex + 1 >= listDayBitmaps.size())?2:currentDayIndex + 1;
				int top = listDayBitmaps.get(currentDayIndex).getHeight() * daysScrollPercent / 100;
				canvas.drawBitmap(listDayBitmaps.get(nextDayIndex), x, top - listDayBitmaps.get(nextDayIndex).getHeight(), null);
				Bitmap tmp = Bitmap.createBitmap(listDayBitmaps.get(currentDayIndex), 0, 0, 
						listDayBitmaps.get(currentDayIndex).getWidth(), listDayBitmaps.get(currentDayIndex).getHeight() - top);
				canvas.drawBitmap(tmp, x, top, null);
				
			}
			else
				canvas.drawBitmap(listDayBitmaps.get(currentDayIndex), x, 0, null);
		}
		
	}

	@Override
	public void setInvalidateBroker(InvalidateBroker broker) {
		invalidater = broker;
	}

	@Override
	public void pause() {
		h.removeCallbacks(runDaysScroll);
		h.removeCallbacks(runUpdateForecast);
		dayScrollAnimation.cancel();
		daysScrollPercent = 0;
	}

	@Override
	public void resume() {
		updateWeatherFromLocalDb();
		
		h.postDelayed(runUpdateForecast, PAUSE_ONRESUME_INTERVAL);
		h.postDelayed(runDaysScroll, SCROLL_DAY_INTERVAL);
	}

	protected void updateWeatherFromLocalDb() {
		WeatherCursorWrapper cur = WeatherProvider.getForecastLocally();
		if (cur != null)
			try {
				renderForecast(cur.getCursor(), cur.getTimestamp(), cur.getCityName());
			} 
			finally {
				cur.close();
			}
	}

	protected void loadWeatherFromServer() {
		weatherProvider.updateForecastFromServer(new WeatherProvider.WeatherUpdatedListener() {
			@Override
			public void updated() {
				updateWeatherFromLocalDb();
			}
		});
	}

	private void renderForecast(Cursor cursor, Date timestamp, String cityName) {
		this.cityName = cityName;
		this.timestamp = timestamp;
		
		listDayBitmaps.clear();
		
		Bitmap tmp = Bitmap.createBitmap(48 * 6, WIDGET_HEIGHT, Config.ARGB_8888);
		tmp.setDensity(Bitmap.DENSITY_NONE);
		Canvas c = new Canvas(tmp);
		
		int x = 0, cellWidth, y, n = -1;
		String s1 = "", s2;
		
		if (cursor.moveToFirst())
			do {
				y = ImageUtils.URGE_PADDING_SMALL;//(int)dayOfWeekPaint.getTextSize() + ImageUtils.URGE_PADDING_SMALL;
				try {
					Date date = WeatherDatabase.getDbDateFormat().parse(cursor.getString(cursor.getColumnIndex(WeatherDatabase.FLD_DATE)));
					s2 = forecastFormat.format(date);
					if (!s1.equals(s2)) {
						s1 = new String(s2);

						if (n >= 0) { //Завершить работу с предыдущим днем
							listDayBitmaps.add(Bitmap.createBitmap(tmp, 0, 0, x + ImageUtils.URGE_PADDING_SMALL, tmp.getHeight()));
						}
						if (++n > MAX_DAYS_COUNT)
							break;
						
						tmp.eraseColor(0);
						c.drawRect(0, 0, tmp.getWidth(), tmp.getHeight(), blackAlphaPaint);
						//   c.drawText(s1, x, dayOfWeekPaint.getTextSize(), dayOfWeekPaint);
						x = (int)dayOfWeekPaint.getTextSize();
						c.save();
						c.rotate(-90.0F, 0, WIDGET_HEIGHT / 2);
						c.drawText(s1, 0, WIDGET_HEIGHT / 2 + (int)dayOfWeekPaint.getTextSize(), dayOfWeekPaint);
						c.restore();
					}

					x += ImageUtils.URGE_PADDING_SMALL;

					String temperature;
					temperature = cursor.getString(cursor.getColumnIndex(WeatherDatabase.FLD_TEMPERATURE_FROM));
					if (!cursor.isNull(cursor.getColumnIndex(WeatherDatabase.FLD_TEMPERATURE_TO)))
						temperature += ".." + cursor.getString(cursor.getColumnIndex(WeatherDatabase.FLD_TEMPERATURE_TO));
					Rect bounds = new Rect();
					forecastPaint.getTextBounds(temperature, 0, temperature.length(), bounds);
					cellWidth = bounds.width();

					String mDrawableName = cursor.getString(cursor.getColumnIndex(WeatherDatabase.FLD_IMAGE));
					if (mDrawableName != null) {
						int resIdCldn = RevivedTabletApp.getContext().getResources().getIdentifier(mDrawableName , "drawable", RevivedTabletApp.getContext().getPackageName());
						if (resIdCldn != 0) {
							Bitmap b = BitmapFactory.decodeResource(RevivedTabletApp.getContext().getResources(), resIdCldn);
							b.setDensity(Bitmap.DENSITY_NONE);
							//Позицию вычисляем с учетом ширины текста температуры
							cellWidth = Math.max(bounds.width(), b.getWidth());
							c.drawBitmap(b, x + (cellWidth - b.getWidth())/ 2 , y, null);
							y += b.getHeight();
						}
					}

					c.drawText(temperature, x + cellWidth / 2, tmp.getHeight() - ImageUtils.URGE_PADDING_SMALL, forecastPaint);
					
					x += cellWidth;
					
					String windDir = cursor.getString(cursor.getColumnIndex(WeatherDatabase.FLD_WIND_DIRECTION));
					int windSpeed = cursor.getInt(cursor.getColumnIndex(WeatherDatabase.FLD_WIND_SPEED));
					if (windSpeed >= WIND_THRESHOLD_NORMAL) {
						int resIdWind = RevivedTabletApp.getContext().getResources().getIdentifier("wind_" + (windSpeed >= WIND_THRESHOLD_STRONG?"s":"n") + "_" + windDir,
								"drawable", RevivedTabletApp.getContext().getPackageName());
						if (resIdWind != 0) {
							Bitmap bmpWind = BitmapFactory.decodeResource(RevivedTabletApp.getContext().getResources(), resIdWind);
							bmpWind.setDensity(Bitmap.DENSITY_NONE);
							c.drawBitmap(bmpWind, x - bmpWind.getWidth(), ImageUtils.URGE_PADDING_SMALL, null);
						}
					}
					
					
				}
				catch (ParseException e) {
					
				}

			} while (cursor.moveToNext());
		
		if (n >= 0 && n < MAX_DAYS_COUNT) 
			listDayBitmaps.add(Bitmap.createBitmap(tmp, 0, 0, x, tmp.getHeight()));
		
		tmp.recycle();
		
		//Высчитываем ширину и новое положение на экране
		if (listDayBitmaps.size() >= 3) {
			int totalWidth = listDayBitmaps.get(0).getWidth() + ImageUtils.URGE_PADDING_BIG + 
							listDayBitmaps.get(1).getWidth() + ImageUtils.URGE_PADDING_BIG +
							listDayBitmaps.get(2).getWidth() + ImageUtils.URGE_PADDING_SMALL + bmpDayScroll1.getWidth() + ImageUtils.URGE_PADDING;
			int left = (TabletCanvasUtils.getDisplayWidth() - totalWidth) / 2;
			rect.set(left, 0, left + totalWidth, listDayBitmaps.get(0).getHeight());
		}
		
		if (invalidater != null)
			invalidater.invalidate();
	}
	
	
}
