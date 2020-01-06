package ru.revivedtablet.weather;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TerraMeteoSource implements WeatherRemoteSource {

    private static Pattern frPattern;

    private static int MAPPING_NIGHT = 100;
    private static int MAPPING_DAY = 200;
    private static Map<String, Integer> overcastMap;
    private static Map<String, Integer> precipitationMap;
    private static Map<Integer, String> iconsMap;

    static {
        overcastMap = new HashMap<>();
        overcastMap.put("ясно", 0);
        overcastMap.put("небольшая обл.", 1);
        overcastMap.put("облачно", 2);
        overcastMap.put("переменная обл.", 2);
        overcastMap.put("облачно с прояснениями", 2);

        precipitationMap = new HashMap<>();
        precipitationMap.put("без осадков", 0);
        precipitationMap.put("преимущественно без осадков", 0);
        precipitationMap.put("слабый дождь", 10);
        precipitationMap.put("дождь", 20);
        precipitationMap.put("сильный дождь", 30);
        precipitationMap.put("слабый снег", 40);
        precipitationMap.put("снег", 50);
        precipitationMap.put("сильный снег", 60);
        precipitationMap.put("небольшой снег , переходящий в дождь", 70);
        precipitationMap.put("снег , переходящий в дождь", 70);

        iconsMap = new HashMap<>();
        iconsMap.put(100, "skc_n");
        iconsMap.put(101, "bkn_n");
        iconsMap.put(102, "ovc");
        iconsMap.put(110, "bkn_mra_n");
        iconsMap.put(111, "bkn_mra_n");
        iconsMap.put(112, "ovc_mra");
        iconsMap.put(120, "bkn_ra_n");
        iconsMap.put(121, "bkn_ra_n");
        iconsMap.put(122, "ovc_ra");
        iconsMap.put(130, "bkn_pra_n");
        iconsMap.put(131, "bkn_pra_n");
        iconsMap.put(132, "ovc_pra");
        iconsMap.put(140, "bkn_msn_n");
        iconsMap.put(141, "bkn_msn_n");
        iconsMap.put(142, "ovc_msn");
        iconsMap.put(150, "bkn_sn_n");
        iconsMap.put(151, "bkn_sn_n");
        iconsMap.put(152, "ovc_sn");
        iconsMap.put(160, "bkn_psn_n");
        iconsMap.put(161, "bkn_psn_n");
        iconsMap.put(162, "ovc_psn");
        iconsMap.put(170, "bkn_rs_n");
        iconsMap.put(171, "bkn_rs_n");
        iconsMap.put(172, "ovc_rs");

        iconsMap.put(200, "skc_d");
        iconsMap.put(201, "bkn_d");
        iconsMap.put(202, "ovc");
        iconsMap.put(210, "bkn_mra_d");
        iconsMap.put(211, "bkn_mra_d");
        iconsMap.put(212, "ovc_mra");
        iconsMap.put(220, "bkn_ra_d");
        iconsMap.put(221, "bkn_ra_d");
        iconsMap.put(222, "ovc_ra");
        iconsMap.put(230, "bkn_pra_d");
        iconsMap.put(231, "bkn_pra_d");
        iconsMap.put(232, "ovc_pra");
        iconsMap.put(240, "bkn_msn_d");
        iconsMap.put(241, "bkn_msn_d");
        iconsMap.put(242, "ovc_msn");
        iconsMap.put(250, "bkn_sn_d");
        iconsMap.put(251, "bkn_sn_d");
        iconsMap.put(252, "ovc_sn");
        iconsMap.put(260, "bkn_psn_d");
        iconsMap.put(261, "bkn_psn_d");
        iconsMap.put(262, "ovc_psn");
        iconsMap.put(270, "bkn_rs_d");
        iconsMap.put(271, "bkn_rs_d");
        iconsMap.put(272, "ovc_rs");

        frPattern = Pattern.compile("(штиль|([СЮЗВ]{1,2}),?[СЮЗВ]{0,2} (\\d+) м/с),[^,]+,[^,]+, ([^,]+), (в 1-й|во 2й)?( половине)?(.+)");
    }

    private String url;

    public TerraMeteoSource(String url) {
        this.url = url;
    }

    private Date divtextToDate(Element el) {
        if (el != null) {
            String[] dp = el.text().split("(\\.| )");
            if (dp.length >= 3) {
                Calendar cl = Calendar.getInstance();
                try {
                    cl.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dp[0]));
                    cl.set(Calendar.MONTH, Integer.parseInt(dp[1]) - 1);
                    cl.set(Calendar.YEAR, Integer.parseInt(dp[2]));
                    return cl.getTime();
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public boolean updateFromRemote(SQLiteDatabase db) {
        try {
            long minInsertedId = savePeriodPage(db, 0, true);
            savePeriodPage(db, 1, false);
            savePeriodPage(db, 2, false);
            savePeriodPage(db, 3, false);
            savePeriodPage(db, 4, false);

            if (minInsertedId > 0)
                db.delete(WeatherDatabase.TABLE_FORECASTS, WeatherDatabase._ID + " < ?",
                    new String[] {String.valueOf(minInsertedId)});

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private long savePeriodPage(SQLiteDatabase db, int pagePeriodIndex, boolean saveCity) throws Exception {
        Document doc = Jsoup.parse(new URL(url + "&period=" + String.valueOf(pagePeriodIndex)), 5000);
        //Город
        if (saveCity) {
            Elements h1 = doc.getElementsByTag("h1");
            h1.first();
            ContentValues ts = new ContentValues();
            ts.put(WeatherDatabase.FLD_TIMESTAMP, WeatherDatabase.getDbDateTimeFormat().format(new Date()));
            ts.put(WeatherDatabase.FLD_CITY_NAME, h1.text());
            db.insertWithOnConflict(WeatherDatabase.TABLE_TIMESTAMP, null, ts, SQLiteDatabase.CONFLICT_REPLACE);
        }
        //Прогноз по дням
        int dayPeriodId;
        long minInsertedId = 0;
        Elements frcstList = doc.getElementsByClass("frcst");
        Iterator<Element> frcstIter = frcstList.iterator();
        while (frcstIter.hasNext()) {
            dayPeriodId = 1;
            Element day = frcstIter.next();
            Date date = divtextToDate(day.select(".date, .dateholl").first());
            if (date != null) {
                Elements rows = day.select(".frcst_row > .txt");
                Iterator<Element> rowsIter = rows.iterator();
                while (rowsIter.hasNext()) {
                    Element row = rowsIter.next();
                    Element time = row.previousElementSibling();
                    Element temp = row.getElementsByClass("bld_lrg").first();
                    if (temp != null) {
                        String[] tempRange = temp.text().replace("+", "").split("\\.+");
                        if (tempRange.length == 2) {
                            int tempMin = Integer.parseInt(tempRange[0]);
                            int tempMax = Integer.parseInt(tempRange[1]);

                            ContentValues cv = new ContentValues();

                            cv.put(WeatherDatabase.FLD_DATE, WeatherDatabase.getDbDateFormat().format(date));
                            cv.put(WeatherDatabase.FLD_PERIOD_ID, dayPeriodId++);

                            cv.put(WeatherDatabase.FLD_TEMPERATURE_FROM, tempMin);
                            cv.put(WeatherDatabase.FLD_TEMPERATURE_TO, tempMax);

                            String windDir = "s";
                            int windSpeed = 0;
                            String icon = "";
                            boolean isNight = (time != null && time.text().equals("ночь")) ;
                            temp = row.getElementsByClass("blu").first();
                            if (temp != null) {
                                Matcher m = frPattern.matcher(temp.text());
                                if (m.matches()) {
                                    if (m.group(1).equals("штиль")) {
                                        windDir = "";
                                        windSpeed = 0;
                                    } else {
                                        windDir = m.group(2).replace('С', 'n').replace('Ю', 's').replace('З', 'w').replace('В', 'e');
                                        windSpeed = Integer.parseInt(m.group(3));
                                    }

                                    Integer ovc = overcastMap.get(m.group(4));
                                    Integer pr = precipitationMap.get(m.group(7));
                                    int key = (ovc==null?0:ovc) + (pr==null?0:pr) + (isNight?MAPPING_NIGHT:MAPPING_DAY);
                                    if (iconsMap.containsKey(key))
                                        icon = iconsMap.get(key);
                                }
                            }

                            cv.put(WeatherDatabase.FLD_IMAGE, icon);
                            cv.put(WeatherDatabase.FLD_WIND_SPEED, windSpeed);
                            cv.put(WeatherDatabase.FLD_WIND_DIRECTION, windDir);

                            long ins = db.insert(WeatherDatabase.TABLE_FORECASTS, null, cv);
                            if (minInsertedId == 0)
                                minInsertedId = ins;

                        }
                    }

                }
            }
        }

        return minInsertedId;
    }
}
