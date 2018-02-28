package ru.revivedtablet.weather;

import android.database.sqlite.SQLiteDatabase;

public interface WeatherRemoteSource {
    boolean updateFromRemote(SQLiteDatabase db);
}
