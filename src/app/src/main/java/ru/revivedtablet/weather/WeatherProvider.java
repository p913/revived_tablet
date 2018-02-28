package ru.revivedtablet.weather;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import ru.revivedtablet.RevivedTabletApp;

import java.text.ParseException;
import java.util.Date;

public class WeatherProvider {
	private WeatherRemoteSource source;
	
	public WeatherProvider(WeatherRemoteSource source) {
		this.source = source;
	}

	public static class WeatherCursorWrapper {
		private SQLiteDatabase db; 
		private Cursor cursor;
		private Date timestamp;
		private String cityName;
		
		public WeatherCursorWrapper(SQLiteDatabase db, Cursor cursor, Date timestamp, String cityName) {
			this.db = db ; 
			this.cursor = cursor;
			this.timestamp = timestamp;
			this.cityName = cityName;
		};
		
		public void close() {
			cursor.close();
			db.close();
		};
		
		public Cursor getCursor() {
			return cursor;
		}

		public Date getTimestamp() {
			return timestamp;
		}

		public String getCityName() {
			return cityName;
		}
	}

	public interface WeatherUpdatedListener {
		public void updated();
	}
	
	public void updateForecastFromServer(final WeatherUpdatedListener listener) {
		AsyncTask<Void, Integer, Boolean> get = new  AsyncTask<Void, Integer, Boolean>() {

			@Override
			protected Boolean doInBackground(Void ... dummy) {
				WeatherDatabase wdb = new WeatherDatabase(RevivedTabletApp.getContext());
				SQLiteDatabase db = wdb.getWritableDatabase();
				try {					
					return source.updateFromRemote(db);
				} 
				finally {
					db.close();
				}
			}
			
			@Override
			public void onPostExecute(Boolean res) {
				if (res)
					listener.updated();
			} 
			
		};
		get.execute();
	}

	public static WeatherCursorWrapper getForecastLocally() {
		WeatherDatabase wdb = new WeatherDatabase(RevivedTabletApp.getContext());
		SQLiteDatabase db = wdb.getReadableDatabase();
		String[] fields = new String[] {WeatherDatabase._ID, WeatherDatabase.FLD_DATE, WeatherDatabase.FLD_PERIOD_ID,
				WeatherDatabase.FLD_TEMPERATURE_FROM, WeatherDatabase.FLD_TEMPERATURE_TO,
				WeatherDatabase.FLD_IMAGE, WeatherDatabase.FLD_WIND_DIRECTION, WeatherDatabase.FLD_WIND_SPEED
		};

		Cursor res =  db.query(WeatherDatabase.TABLE_FORECASTS, fields, null, null, null, null,
				WeatherDatabase.FLD_DATE + ", " + WeatherDatabase.FLD_PERIOD_ID);
		
		String cityName = "?";
		Date timestamp = new Date();
		Cursor ts =  db.query(WeatherDatabase.TABLE_TIMESTAMP, new String[] {WeatherDatabase._ID, WeatherDatabase.FLD_TIMESTAMP, WeatherDatabase.FLD_CITY_NAME}, null, null, null, null, null);
		try {
			if (ts.moveToFirst()) {
				cityName = ts.getString(ts.getColumnIndex((WeatherDatabase.FLD_CITY_NAME)));
				try {
					timestamp = WeatherDatabase.getDbDateTimeFormat().parse(ts.getString(ts.getColumnIndex((WeatherDatabase.FLD_TIMESTAMP))));
				} catch (ParseException e) {					
				}
			}
		} finally {
			ts.close();
		}

		return new WeatherCursorWrapper(db, res, timestamp, cityName);
	}
	
	
}

