package ru.revivedtablet;


import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

/*
 * http://yandex.st/weather/1.2.1/i/icons/30x30/bkn_-ra_n.png
 * https://yandex.st/weather/1.1.78/i/icons/48x48/bkn_-ra_n.png
 **/
public class TabletCanvasActivity extends Activity implements View.OnSystemUiVisibilityChangeListener  {
	
	public static final String INTENT_KEY_CONFIG_BUNDLE = "config";
	
	//В этот виджет мы выводим конечное изображение с наложенными часами и погодой
	private TabletCanvasView viewPhotoClockWeather;
	
	private Handler h = new Handler();
	
	private WebServerImpl webServer = new WebServerImpl();
	
	private Runnable runHideNavBar = new Runnable () {
		@Override
		public void run() {
			hideNavBar();
		}		
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		viewPhotoClockWeather = new TabletCanvasView(this);
		setContentView(viewPhotoClockWeather);

		try {
			webServer.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		webServer.stop();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		viewPhotoClockWeather.pause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		viewPhotoClockWeather.resume();
		
		hideNavBar();
		
	}
	
	private void hideNavBar() {
		View decorView = getWindow().getDecorView();
		int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
				View.SYSTEM_UI_FLAG_FULLSCREEN | 
				View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
				View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
		decorView.setSystemUiVisibility(uiOptions);
		decorView.setOnSystemUiVisibilityChangeListener(this);
	}

	@Override
	public void onSystemUiVisibilityChange(int visFlags) {
		if ((visFlags & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) 
			h.postDelayed(runHideNavBar, 3000);
	}
	
}
