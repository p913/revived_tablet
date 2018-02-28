package ru.revivedtablet.weather;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.pogoda.IconMapper;
import com.pogoda.xmlbinding.Day;
import com.pogoda.xmlbinding.Hour;
import com.pogoda.xmlbinding.Report;
import com.pogoda.xmlbinding.ReportVars;
import com.pogoda.xmlbinding.Symbol;
import com.pogoda.xmlbinding.Var;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

public class PogodaComSource implements WeatherRemoteSource {
    private String location;

    private String affiliate;

    private String language;

    public PogodaComSource(String location, String affiliate) {
        this(location, affiliate, "en");
    }

    public PogodaComSource(String location, String affiliate, String language) {
        this.location = location;
        this.affiliate = affiliate;
        this.language = language;
    }

    @Override
    public boolean updateFromRemote(SQLiteDatabase db) {
        return updateForecastDetailed(db) && updateForecastShort(db, new Date());
    }

    protected boolean updateForecastDetailed(SQLiteDatabase db) {
        try {
            URL url = new URL(String.format("HttpLib://api.pogoda.com/index.php?api_lang=%s&localidad=%s&affiliate_id=%s&v=2",
                    language, location, affiliate));
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                InputStream is = new BufferedInputStream(urlConnection.getInputStream());

                long minInsertedId = 0;
                int details = 2;
                Serializer serializer = new Persister();
                Report report = serializer.read(Report.class, is);

                if (report.location != null && report.location.city != null) {
                    ContentValues ts = new ContentValues();
                    ts.put(WeatherDatabase.FLD_TIMESTAMP, WeatherDatabase.getDbDateTimeFormat().format(new Date()));
                    ts.put(WeatherDatabase.FLD_CITY_NAME, report.location.city);
                    if (0 == db.update(WeatherDatabase.TABLE_TIMESTAMP, ts, null, null))
                        db.insert(WeatherDatabase.TABLE_TIMESTAMP, null, ts);
                }

                if (report.location.days != null) {
                    for (Day day: report.location.days) {
                        long ins;
                        if (details > 0) {
                            ins = saveForecastDayPartInDatabase(db, 1, day.date,
                                    new Hour[] {day.get(0), day.get(1)});
                            saveForecastDayPartInDatabase(db, 2, day.date,
                                    new Hour[] {day.get(2), day.get(3)});
                            saveForecastDayPartInDatabase(db, 3, day.date,
                                    new Hour[] {day.get(4), day.get(5)});
                            saveForecastDayPartInDatabase(db, 4, day.date,
                                    new Hour[] {day.get(6), day.get(7)});
                        } else {
                            ins = saveForecastDayPartInDatabase(db, 1, day.date,
                                    new Hour[] {day.get(0), day.get(1),
                                            day.get(2), day.get(3)});
                            saveForecastDayPartInDatabase(db, 2, day.date,
                                    new Hour[] {day.get(4), day.get(5),
                                            day.get(6), day.get(7)});
                        }

                        if (minInsertedId == 0)
                            minInsertedId = ins;
                        details--;
                    }
                }

                if (minInsertedId > 0)
                    db.delete(WeatherDatabase.TABLE_FORECASTS, WeatherDatabase._ID + " < ?",
                            new String[] {String.valueOf(minInsertedId)});

                return true;

            } finally {
                urlConnection.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    protected boolean updateForecastShort(SQLiteDatabase db, Date startDate) {
        try {
            URL url = new URL(String.format("HttpLib://api.pogoda.com/index.php?api_lang=%s&localidad=%s&affiliate_id=%s",
                    language, location, affiliate));
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                InputStream is = new BufferedInputStream(urlConnection.getInputStream());

                Serializer serializer = new Persister();
                ReportVars report = serializer.read(ReportVars.class, is);

                Date dt = new Date(startDate.getTime() + 1000 * 24 * 3600 * 5);
                ContentValues cv = new ContentValues();
                int temp = 0, temp2 = 0;
                Hour h = new Hour();
                h.hour = 12;
                h.symbol = new Symbol();

                cv.put(WeatherDatabase.FLD_DATE, WeatherDatabase.getDbDateFormat().format(dt));
                cv.put(WeatherDatabase.FLD_PERIOD_ID, 1);

                temp = Integer.parseInt(report.get(Var.ICON_TEMPERATURE_MIN, 6).value);
                cv.put(WeatherDatabase.FLD_TEMPERATURE_FROM, temp);
                temp2 = Integer.parseInt(report.get(Var.ICON_TEMPERATURE_MAX, 6).value);
                if (temp2 != temp)
                    cv.put(WeatherDatabase.FLD_TEMPERATURE_TO, temp2);
                h.symbol.icon = Integer.parseInt(report.get(Var.ICON_ICON, 6).id);
                cv.put(WeatherDatabase.FLD_IMAGE, IconMapper.get(new Hour[] {h}));

                db.insert(WeatherDatabase.TABLE_FORECASTS, null, cv);

                cv.clear();

                dt.setTime(dt.getTime() + 1000 * 24 * 3600);
                cv.put(WeatherDatabase.FLD_DATE, WeatherDatabase.getDbDateFormat().format(dt));
                cv.put(WeatherDatabase.FLD_PERIOD_ID, 1);

                temp = Integer.parseInt(report.get(Var.ICON_TEMPERATURE_MIN, 7).value);
                cv.put(WeatherDatabase.FLD_TEMPERATURE_FROM, temp);
                temp2 = Integer.parseInt(report.get(Var.ICON_TEMPERATURE_MAX, 7).value);
                if (temp2 != temp)
                    cv.put(WeatherDatabase.FLD_TEMPERATURE_TO, temp2);
                h.symbol.icon = Integer.parseInt(report.get(Var.ICON_ICON, 7).id);
                cv.put(WeatherDatabase.FLD_IMAGE, IconMapper.get(new Hour[] {h}));

                db.insert(WeatherDatabase.TABLE_FORECASTS, null, cv);

                return true;

            } finally {
                urlConnection.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    protected long saveForecastDayPartInDatabase(SQLiteDatabase db, int periodId, Date dt, Hour[] hours) {
        try {

            ContentValues cv = new ContentValues();

            cv.put(WeatherDatabase.FLD_DATE, WeatherDatabase.getDbDateFormat().format(dt));
            cv.put(WeatherDatabase.FLD_PERIOD_ID, periodId);

            int tempMin = 99, tempMax = -99;
            int windPower = 0;
            String windDir = "";

            for (Hour hour: hours)
                if (hour != null) {
                    tempMin = Math.min(hour.temp.value, tempMin);
                    tempMax = Math.max(hour.temp.value, tempMax);

                    windPower = Math.max(hour.wind.power, windPower);
                    windDir = hour.wind.dir;
                }

            if (tempMin == 99)
                tempMin = 0;
            if (tempMax == -99)
                tempMax = 0;
            if (tempMin != tempMax) {
                cv.put(WeatherDatabase.FLD_TEMPERATURE_FROM, tempMin);
                cv.put(WeatherDatabase.FLD_TEMPERATURE_TO, tempMax);
            } else
                cv.put(WeatherDatabase.FLD_TEMPERATURE_FROM, tempMin);

            cv.put(WeatherDatabase.FLD_IMAGE, IconMapper.get(hours));
            cv.put(WeatherDatabase.FLD_WIND_SPEED, windPower);
            cv.put(WeatherDatabase.FLD_WIND_DIRECTION, windDir.toLowerCase());

            return db.insert(WeatherDatabase.TABLE_FORECASTS, null, cv);
        } catch (Exception e) {e.printStackTrace();}

        return 0;
    }


}
