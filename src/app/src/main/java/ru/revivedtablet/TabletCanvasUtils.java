package ru.revivedtablet;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class TabletCanvasUtils {
	
	private static Integer displayWidth;
	private static Integer displayHeight;
	
	private TabletCanvasUtils() {

	}

	public static int getDisplayHeight() {
		if (displayHeight == null)
			caclDisplaySize();
		return displayHeight;
	}
	
	public static int getDisplayWidth() {
		if (displayWidth == null)
			caclDisplaySize();
		return displayWidth;
	}
	
	private static void caclDisplaySize() {
		Resources resources = RevivedTabletApp.getContext().getResources();
		DisplayMetrics dm = new DisplayMetrics();
		WindowManager wm = (WindowManager) RevivedTabletApp.getContext().getSystemService(Context.WINDOW_SERVICE);
		Display disp = wm.getDefaultDisplay();
		disp.getMetrics(dm);
		int dispWidth = Integer.valueOf(dm.widthPixels);
		int dispHeight = Integer.valueOf(dm.heightPixels + getNavBarHeight(resources));
		if (dispWidth < dispHeight) {
			int p = dispWidth;
			dispWidth = dispHeight;
			dispHeight = p;
		};
		
		displayWidth = Integer.valueOf(dispWidth);
		displayHeight = Integer.valueOf(dispHeight);

		
	}
	
	private static int getNavBarHeight (Resources resources)
    {
        int id = resources.getIdentifier("config_showNavigationBar", "bool", "android");
        if (id > 0 && resources.getBoolean(id)) {
    		int idh = resources.getIdentifier("navigation_bar_height", "dimen", "android");
    		if (idh > 0) 
    			return resources.getDimensionPixelSize(idh);
        }
        return 0;
    }	

}
