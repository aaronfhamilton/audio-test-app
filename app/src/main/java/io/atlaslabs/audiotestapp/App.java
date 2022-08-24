package io.atlaslabs.audiotestapp;

import android.app.Application;

import timber.log.Timber;

public class App extends Application {
	@Override
	public void onCreate() {
		super.onCreate();

		if (BuildConfig.DEBUG)
			Timber.plant(new Timber.DebugTree());

		SoundTest.setup(this);
		UserNotificationManager.setup(this);
	}

	@Override
	public void onTerminate() {
		UserNotificationManager.getInstance().cleanup();
		super.onTerminate();
	}
}
