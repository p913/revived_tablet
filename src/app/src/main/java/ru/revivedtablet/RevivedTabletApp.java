package ru.revivedtablet;

import android.app.Application;
import android.content.Context;

public class RevivedTabletApp extends Application {

	private static RevivedTabletApp instance;

    public RevivedTabletApp() {
    	instance = this;
    }

    public static Context getContext() {
    	return instance;
    }
}
