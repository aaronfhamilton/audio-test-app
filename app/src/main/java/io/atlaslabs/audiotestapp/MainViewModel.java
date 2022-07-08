package io.atlaslabs.audiotestapp;

import android.app.Application;
import android.app.Notification;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

public class MainViewModel extends AndroidViewModel {
	private final Application mApp;

	public MainViewModel(@NonNull Application app) {
		super(app);

		mApp = app;
	}

	public void startService() {
		Intent startIntent = new Intent(mApp, AppService.class);
		startIntent.putExtra(AppService.EXTRA_MESSAGE_ID, AppService.MSG.START_FOREGROUND);
		mApp.startService(startIntent);
	}

	public void stopService() {
		Intent stopIntent = new Intent(mApp, AppService.class);
		stopIntent.putExtra(AppService.EXTRA_MESSAGE_ID, AppService.MSG.STOP_SERVICE);
		mApp.startService(stopIntent);
	}
}
