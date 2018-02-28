package ru.revivedtablet.weather;

import java.text.SimpleDateFormat;
import java.util.Locale;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class WeatherDatabase extends SQLiteOpenHelper implements BaseColumns {
	
	private static final String DATABASE_NAME = "weather.db";
	private static final int DATABASE_VERSION = 2;
	
	public static final String TABLE_FORECASTS = "forecasts";
	public static final String TABLE_TIMESTAMP = "timestamp";
	
	public static final String FLD_DATE = "Date";
	public static final String FLD_PERIOD_ID = "PeriodId";
	public static final String FLD_TEMPERATURE_FROM = "TemperatureFrom";
	public static final String FLD_TEMPERATURE_TO = "TemperatureTo";
	public static final String FLD_IMAGE = "Image";
	public static final String FLD_WIND_DIRECTION = "WindDirection";
	public static final String FLD_WIND_SPEED = "WindSpeed";
	public static final String FLD_WEATHER_TYPE = "WeatherType";
	public static final String FLD_WEATHER_TYPE_SHORT = "WeatherTypeShort";
	
	public static final String FLD_TIMESTAMP = "Timestamp";
	public static final String FLD_CITY_NAME = "CityName";
	
	private static SimpleDateFormat dbDateFormat = new SimpleDateFormat("y-MM-dd", Locale.getDefault());
	private static SimpleDateFormat dbDateTimeFormat = new SimpleDateFormat("y-MM-dd HH:mm", Locale.getDefault());
		
	public WeatherDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		createTableForecasts(db);
		createTableTimestamp(db);

	}
	
	private void createTableForecasts(SQLiteDatabase db) {
		db.execSQL("create table " + TABLE_FORECASTS + " (" +
		          _ID + " integer primary key autoincrement," + 
		          FLD_DATE + " text," +
		          FLD_PERIOD_ID + " integer," +
		          FLD_TEMPERATURE_FROM + " integer," +
		          FLD_TEMPERATURE_TO + " integer," +
		          FLD_IMAGE + " text," +
		          FLD_WIND_DIRECTION + " text," +
		          FLD_WIND_SPEED + " numeric," +
		          FLD_WEATHER_TYPE + " text," +
		          FLD_WEATHER_TYPE_SHORT + " text)");
	}
	
	private void createTableTimestamp(SQLiteDatabase db) {
		db.execSQL("create table " + TABLE_TIMESTAMP + " (" +
		          _ID + " integer primary key autoincrement," + 
		          FLD_TIMESTAMP + " text," +
		          FLD_CITY_NAME + " integer)");
	}
	

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < DATABASE_VERSION)
			createTableTimestamp(db);

	}

	public static final SimpleDateFormat getDbDateFormat () {
		return dbDateFormat;
	}
	
	public static final SimpleDateFormat getDbDateTimeFormat () {
		return dbDateTimeFormat;
	} 
	

}
