package io.atlaslabs.audiotestapp;

import android.app.Application;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.AndroidViewModel;

import io.reactivex.Observable;

public class MainViewModel extends AndroidViewModel {
	private final Application mApp;
	private final SoundSettings mSoundSettings;

	public MainViewModel(@NonNull Application app) {
		super(app);

		mApp = app;
		mSoundSettings = new SoundSettings(app, 1.0f, true, 10);
	}

	public void startAlert() {
		App.GetAlertPlayer().startSound(mSoundSettings);
	}

	public void stopAlert() {
		App.GetAlertPlayer().stopSound();
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
