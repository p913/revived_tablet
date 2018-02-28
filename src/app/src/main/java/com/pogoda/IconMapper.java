package com.pogoda;
import java.util.HashMap;
import java.util.Map;

import com.pogoda.xmlbinding.Hour;

public class IconMapper {
	private static final Map<Integer, IconKeeper> icons;
	
	static {
		icons = new HashMap<Integer, IconKeeper>();
		icons.put(1, new IconKeeper("skc_n", "skc_d", 0));
		icons.put(2, new IconKeeper("bkn_n", "bkn_d", 1));
		icons.put(3, new IconKeeper("bkn_n", "bkn_d", 1));
		icons.put(4, new IconKeeper("ovc", "ovc", 1));
		icons.put(5, new IconKeeper("bkn_mra_n", "bkn_mra_d", 2));
		icons.put(6, new IconKeeper("bkn_ra_n", "bkn_ra_d", 2));
		icons.put(7, new IconKeeper("ovc_ra", "ovc_ra", 2));
		icons.put(8, new IconKeeper("bkn_pra_n", "bkn_pra_d", 3));
		icons.put(9, new IconKeeper("bkn_pra_n", "bkn_pra_d", 3));
		icons.put(10, new IconKeeper("ovc_pra", "ovc_pra", 3));
		icons.put(11, new IconKeeper("ovc_ts_ra", "ovc_ts_ra", 3));
		icons.put(12, new IconKeeper("ovc_ts_ra", "ovc_ts_ra", 3));
		icons.put(13, new IconKeeper("ovc_ts_ra", "ovc_ts_ra", 3));
		icons.put(14, new IconKeeper("ovc_ts_ra", "ovc_ts_ra", 2));
		icons.put(15, new IconKeeper("ovc_ts_ra", "ovc_ts_ra", 2));
		icons.put(16, new IconKeeper("ovc_ts_ra", "ovc_ts_ra", 2));
		icons.put(17, new IconKeeper("bkn_msn_n", "bkn_msn_d", 2));
		icons.put(18, new IconKeeper("bkn_sn_n", "bkn_sn_d", 2));
		icons.put(19, new IconKeeper("ovc_psn", "ovc_psn", 2));
		icons.put(20, new IconKeeper("bkn_rs_n", "bkn_rs_d", 2));
		icons.put(21, new IconKeeper("bkn_rs_n", "bkn_rs_d", 2));
		icons.put(22, new IconKeeper("ovc_rs", "ovc_rs", 2));
		
	}
	
	public static String get(Hour[] hours) {
		int hour = 0, priority = 0;
		boolean isNight = false;
		String icon = "";
		
		//На основании усредненного часа суток выбираем дневные или ночные иконки  
		for (Hour h: hours)
			if (h != null)
				hour += h.hour;
		hour /= hours.length;
		isNight = (hour <= 5 || hour >= 18);
		
		//Иконки с осадками имеют бОльший приоритет
		
		for (Hour h: hours)
			if (h != null) {
				IconKeeper i = icons.get(h.symbol.icon);
				if (i != null) {
					if (i.getPriority() >= priority) {
						priority = i.getPriority();
						icon = isNight?i.getNightIcon():i.getDayIcon();
					}
				}
			}
		
		return icon;
	}
	
	private static class IconKeeper {
		private String dayIcon;
		private String nightIcon;
		private int priority;
		
		public IconKeeper(String nightIcon, String dayIcon, int priority) {
			this.nightIcon = nightIcon;
			this.dayIcon = dayIcon;
			this.priority = priority;
		}

		public String getDayIcon() {
			return dayIcon;
		}

		public String getNightIcon() {
			return nightIcon;
		}

		public int getPriority() {
			return priority;
		}
		
		
		
	}
	
}

