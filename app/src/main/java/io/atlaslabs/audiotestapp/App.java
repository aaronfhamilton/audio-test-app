package io.atlaslabs.audiotestapp;

import android.app.Application;
import android.os.Build;

import timber.log.Timber;

public class App extends Application {
	static IAlertPlayer mAlertPlayer = null;

	@Override
	public void onCreate() {
		super.onCreate();

		if ("rockchip".equals(Build.MANUFACTURER)) {
			Timber.wtf("Bend over backwards");
		}

		if (BuildConfig.DEBUG)
			Timber.plant(new Timber.DebugTree());

		mAlertPlayer = new AlertPlayer(this);
		SoundTest.setup(this);
		UserNotificationManager.setup(this);
	}

	@Override
	public void onTerminate() {
		UserNotificationManager.getInstance().cleanup();
		super.onTerminate();
	}

	public static IAlertPlayer GetAlertPlayer() {
		return mAlertPlayer;
	}
}
