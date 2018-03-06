package ru.revivedtablet.widget;

import ru.revivedtablet.ImageUtils;
import ru.revivedtablet.R;
import ru.revivedtablet.RevivedTabletApp;
import ru.revivedtablet.TabletCanvasUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;

public class BatteryWidget implements Widget, Runnable {
	
	//Пауза между обновлением статуса батарее, если она заряжена полностью и подключена к зарядке  
	private static final int DELAY_UPDATE_BATTARY_STATUS_LONG = 1200000; //20 минут
	//Пауза между обновлением статуса батарее, меньше, чем DELAY_UPDATE_BATTARY_STATUS_LONG  
	private static final int DELAY_UPDATE_BATTARY_STATUS_SHORT = 300000; //5 минут
	
	private Handler h = new Handler(Looper.getMainLooper());
	
	private InvalidateBroker invalidater;
	
	private boolean isCharging;
	private int level;
	
	private Paint percentsTextPaint;
	
	private Rect rect;
	
	private BroadcastReceiver rcv;
	
	private Bitmap bmpBattery100;
	private Bitmap bmpBattery75;
	private Bitmap bmpBattery50;
	private Bitmap bmpBattery25;
	private Bitmap bmpBattery;
	
	public BatteryWidget() {
		rect = new Rect(0, 0, TabletCanvasUtils.getDisplayWidth(), ImageUtils.URGE_TEXT_SIZE_SMALL);
		
		bmpBattery100 = BitmapFactory.decodeResource(RevivedTabletApp.getContext().getResources(), R.drawable.battery100);
		bmpBattery100.setDensity(Bitmap.DENSITY_NONE);
		bmpBattery75 = BitmapFactory.decodeResource(RevivedTabletApp.getContext().getResources(), R.drawable.battery75);
		bmpBattery75.setDensity(Bitmap.DENSITY_NONE);
		bmpBattery50 = BitmapFactory.decodeResource(RevivedTabletApp.getContext().getResources(), R.drawable.battery50);
		bmpBattery50.setDensity(Bitmap.DENSITY_NONE);
		bmpBattery25 = BitmapFactory.decodeResource(RevivedTabletApp.getContext().getResources(), R.drawable.battery25);
		bmpBattery25.setDensity(Bitmap.DENSITY_NONE);
		bmpBattery = BitmapFactory.decodeResource(RevivedTabletApp.getContext().getResources(), R.drawable.battery);
		bmpBattery.setDensity(Bitmap.DENSITY_NONE);
		
		percentsTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		percentsTextPaint.setColor(Color.WHITE);
		percentsTextPaint.setTextSize(ImageUtils.URGE_TEXT_SIZE_SMALL);
	}

	@Override
	public void draw(Canvas canvas) {
		Rect bounds = new Rect();
		percentsTextPaint.getTextBounds("100%", 0, 4, bounds);
		int x = rect.right - bounds.width() - 5;
		canvas.drawText(String.valueOf(level) + "%", x, percentsTextPaint.getTextSize(), percentsTextPaint);
		Bitmap b = bmpBattery;
		if (level >= 95)
			b = bmpBattery100;
		else if (level >= 70)
			b = bmpBattery75;
		else if (level >= 45)
			b = bmpBattery50;
		else if (level >= 20)
			b = bmpBattery25;
		canvas.drawBitmap(b, x - b.getWidth() - 5, 
				(ImageUtils.URGE_TEXT_SIZE_SMALL - b.getHeight()) / 2, null);
		
		if (isCharging) {
			Bitmap ba = BitmapFactory.decodeResource(RevivedTabletApp.getContext().getResources(), R.drawable.acplugged);
			canvas.drawBitmap(ba, x - b.getWidth() - 5 - ba.getWidth(), 
									(ImageUtils.URGE_TEXT_SIZE_SMALL - ba.getHeight()) / 2, null);
		}
	}

	@Override
	public void setInvalidateBroker(InvalidateBroker broker) {
		invalidater = broker;
	}

	@Override
	public void pause() {
		h.removeCallbacks(this);
		
		unregisterReceiver();
	}

	@Override
	public void resume() {
		updateBattaryStatus();
		
		if (isCharging && level == 100) 
			h.postDelayed(this, DELAY_UPDATE_BATTARY_STATUS_LONG);
		else
			h.postDelayed(this, DELAY_UPDATE_BATTARY_STATUS_SHORT);
		
		registerReceiver();
	}

	private void updateBattaryStatus() {
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = RevivedTabletApp.getContext().registerReceiver(null, ifilter);

		boolean oldIsCharging = isCharging;
		int oldLevel = level;
		
		// Are we charging / charged?
		int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
		                     status == BatteryManager.BATTERY_STATUS_FULL;
		
		int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

		this.level = (int)(100 * level / (float)scale);
		
		if (invalidater != null && (oldIsCharging != isCharging || oldLevel != level))
				invalidater.invalidate();
	}

	@Override
	public void run() {
		updateBattaryStatus();
		
		if (isCharging && level == 100) 
			h.postDelayed(this, DELAY_UPDATE_BATTARY_STATUS_LONG);
		else
			h.postDelayed(this, DELAY_UPDATE_BATTARY_STATUS_SHORT);
		
	}
	
	private void registerReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.intent.action.ACTION_BATTERY_LOW");
		filter.addAction("android.intent.action.ACTION_BATTERY_OKAY");
		filter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
		filter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");		
		
		rcv = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				updateBattaryStatus();				
			}			
		};
		
		RevivedTabletApp.getContext().registerReceiver(rcv,  filter);
	}
	
	private void unregisterReceiver() {
		if (rcv != null) {
			RevivedTabletApp.getContext().unregisterReceiver(rcv);
			rcv = null;
		} 
	}	
    
}
