package io.atlaslabs.audiotestapp;

import android.app.Application;

import androidx.viewbinding.BuildConfig;

import timber.log.Timber;

public class App extends Application {
	@Override
	public void onCreate() {
		super.onCreate();

		if (BuildConfig.DEBUG)
			Timber.plant(new Timber.DebugTree());
	}
}
