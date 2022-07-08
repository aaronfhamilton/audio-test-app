package io.atlaslabs.audiotestapp;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.LogPrinter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

public class AppService extends Service {
	public static final String EXTRA_MESSAGE_ID = "message_id";

	private static final int MESSAGE_ID_INVALID = -1;

	public interface MSG {
		int STOP_SERVICE = 0;
		int START_SERVICE = 1;
		int START_FOREGROUND = 2;
		int DUMP_STATE = 3;
		int PLAY_MEDIA = 4;
	}

	private HandlerThread mHandlerThread;
	private ServiceHandler mServiceHandler;

	private volatile boolean mRunService = false;

	private final CompositeDisposable mDisposables = new CompositeDisposable();
	private final Binder mBinder = new LocalBinder();

	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(@NonNull Message msg) {
			Timber.d("Handling service message ID %d", msg.what);
			switch (msg.what) {
				case MSG.START_FOREGROUND:
					start(true);
					break;
				case MSG.START_SERVICE:
					start(false);
					break;
				case MSG.STOP_SERVICE:
					// Set flag to stop running the processing loop. Service will stop itself when this flag is no longer set.
					mRunService = false;
					UserNotificationManager.getInstance().cancelForegroundNotification();
					stopSelf();
					break;
				default:
					super.handleMessage(msg);
			}
		}
	}

	private void start(boolean foreground) {
		mRunService = true;
		if (!foreground)
			return;

		Notification n = UserNotificationManager.getInstance().notify(null, null);
		startForeground(UserNotificationManager.PERSISTENT_NOTIFICATION_ID, n);
	}

	private void sendMessage(int msgId, int startId) {
		Message msg = mServiceHandler.obtainMessage(msgId);
		msg.arg1 = startId;

		boolean success = mServiceHandler.sendMessage(msg);
		if (msgId == MSG.DUMP_STATE) {
			mServiceHandler.dump(new LogPrinter(Log.DEBUG, "HandlerDump"), "");
		}

		Timber.d("Message ID %d sent to service handler (success = %s)", msgId, success);
	}

	/**
	 * System calls this method when another component, such as an activity, requests service
	 * be started by calling startService().
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Timber.v("onStartCommand intent %s, flags %d, startId %d", intent, flags, startId);

		// May not have an Intent if the service was killed and restarted
		if (intent == null)
			return Service.START_STICKY;

		int msgId = intent.getIntExtra(EXTRA_MESSAGE_ID, MESSAGE_ID_INVALID);
		sendMessage(msgId, startId);

		// If the service gets killed, be sure it's started again with the previous intent
		return Service.START_REDELIVER_INTENT;
	}

	/**
	 * Called only once at service initial creation. Not called if already running.
	 */
	@Override
	public void onCreate() {
		super.onCreate();

		// Start up the thread running the service.  Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block.  We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		mHandlerThread = new HandlerThread(AppService.class.getName() + "HandlerThread",
				android.os.Process.THREAD_PRIORITY_BACKGROUND);

		mHandlerThread.start();
		mServiceHandler = new ServiceHandler(mHandlerThread.getLooper());
	}

	@Override
	public void onDestroy() {
		mDisposables.clear();

		// Terminate thread looper when all remaining messages are handled
		mHandlerThread.quitSafely();

		super.onDestroy();
	}

	private class LocalBinder extends Binder {
		public AppService getService() {
			return AppService.this;
		}
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
}
