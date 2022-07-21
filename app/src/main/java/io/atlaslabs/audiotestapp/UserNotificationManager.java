package io.atlaslabs.audiotestapp;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioRouting;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

import io.atlaslabs.audiotestapp.activities.MainActivity;
import io.atlaslabs.audiotestapp.util.Utils;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Handles notifications and audio:
 * - AudioManager - provides access to volume and ringer mode control
 * - MediaPlayer - controls playback of audio/video files and streams. Not thread safe - must
 * access player instances from within same thread. If registering callbacks, thread must have a looper.
 */
public class UserNotificationManager implements MediaPlayer.OnErrorListener,
		MediaPlayer.OnInfoListener, MediaPlayer.OnCompletionListener {
	public static final int PERSISTENT_NOTIFICATION_ID = 1;
	public static final String CHANNEL_ID = "io.atlaslabs.audiotestapp";

	private static UserNotificationManager mInstance = null;
	private final String mUriPrefix;
	private final Ringtone mRingtone;
	private final Uri mSoundUri;
	private final Uri mDefaultAlarmUri;
	private final Uri mDefaultRingtone;
	private final Uri mDefaultNotification;
	private final Context mContext;
	private final NotificationChannel mNotificationChannel;
	private final NotificationManagerCompat mNotificationManager;

	private Notification mPersistentNotification = null;

	private final CompositeDisposable mDisposables = new CompositeDisposable();

	private UserNotificationManager(Application appContext) {

		mContext = appContext;
		mUriPrefix = ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + appContext.getPackageName() + "/";
		mRingtone = RingtoneManager.getRingtone(appContext, Uri.parse(mUriPrefix + R.raw.chime));
		mSoundUri = Uri.parse(mUriPrefix + R.raw.chime);

		mDefaultAlarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
		mDefaultNotification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		mDefaultRingtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

		mNotificationManager = NotificationManagerCompat.from(mContext);

		mNotificationChannel = Utils.isAtLeastO() ? createNotificationChannel(mContext, mNotificationManager) : null;
	}

	@RequiresApi(Build.VERSION_CODES.O)
	private static final AudioRouting.OnRoutingChangedListener mRoutingListener = audioRouting -> Timber.i("Audio routing change: %s", audioRouting);

	public static void setup(Application app) {
		if (mInstance != null)
			return;

		mInstance = new UserNotificationManager(app);
	}

	public static UserNotificationManager getInstance() {
		return mInstance;
	}

	public void cleanup() {
		mDisposables.dispose();
	}

	@Override
	public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
		Timber.e("Media player error: what=%d, extra=%d", what, extra);
		return false;
	}

	@Override
	public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra) {
		Timber.w("Media player info: what(type)=%d, extra=%d", what, extra);
		return false;
	}

	@Override
	public void onCompletion(MediaPlayer mediaPlayer) {
		Timber.d("MediaPlayer session ID %d completed", mediaPlayer.getAudioSessionId());
		mediaPlayer.release();
	}

	@RequiresApi(Build.VERSION_CODES.O)
	public Observable<Integer> playMobilis() {
		return Observable.fromCallable(() -> {
			AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
			int sessionId = am.generateAudioSessionId();
			int streamType = AudioManager.STREAM_ALARM;

			AudioAttributes attrib = new AudioAttributes.Builder()
					.setUsage(AudioAttributes.USAGE_ALARM)
					.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
					.build();

			Ringtone ringtone = RingtoneManager.getRingtone(mContext, Uri.parse(mUriPrefix + R.raw.woopwoop));
			ringtone.setAudioAttributes(attrib);

			int maxVolume = am.getStreamMaxVolume(streamType);
			int currentVolume = am.getStreamVolume(streamType);

			Timber.d("Stream %d volumes: current = %d, max = %d", streamType, currentVolume, maxVolume);
			am.setStreamVolume(streamType, currentVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

			ringtone.play();

			return sessionId;
				});
	}

	@RequiresApi(Build.VERSION_CODES.O)
	public void playMedia(Uri soundUri) {
		if (soundUri == null) {
			Utils.showToast(mContext, "No sound Uri selected. Playing app default %s", mSoundUri);
			soundUri = mSoundUri;
		}

		Uri finalSoundUri = soundUri;
		mDisposables.add(Observable.fromCallable(() -> {
			AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
			int sessionId = am.generateAudioSessionId();

			AudioAttributes attrib = new AudioAttributes.Builder()
					.setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
					.build();

			// In prepared state when create is used
			MediaPlayer mp = MediaPlayer.create(mContext, R.raw.chime, attrib, sessionId);
			mp.setOnErrorListener(this);
			mp.setOnCompletionListener(this);

			Timber.i("Media %s: usage=%d, vcs=%d, flags=%d, contentType=%d", finalSoundUri,
					attrib.getUsage(), attrib.getVolumeControlStream(), attrib.getFlags(), attrib.getContentType());
			mp.start();
			List<AudioPlaybackConfiguration> configs = am.getActivePlaybackConfigurations();
			Timber.i("AudioManager: ringermode=%d, mode=%d", am.getRingerMode(), am.getMode());

			// Timber.i("Volumes: ", am.getStreamVolume(), am.getStreamMinVolume());

			return sessionId;
		})
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(result -> { },
						throwable -> Timber.e(throwable, "Error playing media: %s", throwable.getLocalizedMessage()),
						() -> { }));

	}

	public void playRingtone(Uri soundUri) {
		if (soundUri == null) {
			Utils.showToast(mContext, "No sound Uri selected. Playing app default %s", mSoundUri);
			soundUri = mSoundUri;
		}

		try {
			Ringtone ringtone = RingtoneManager.getRingtone(mContext, soundUri);
			if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				AudioAttributes attrib = ringtone.getAudioAttributes();
				Utils.showToast(mContext, "Default ringtone %s: usage=%d, vcs=%d, flags=%d, contentType=%d", soundUri,
						attrib.getUsage(), attrib.getVolumeControlStream(), attrib.getFlags(), attrib.getContentType());

				attrib = new AudioAttributes.Builder()
						.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
						.setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
						.build();

				ringtone.setAudioAttributes(attrib);

				Utils.showToast(mContext, "New ringtone %s: usage=%d, vcs=%d, flags=%d, contentType=%d", soundUri,
						attrib.getUsage(), attrib.getVolumeControlStream(), attrib.getFlags(), attrib.getContentType());
			} else {
				Utils.showToast(mContext, "Playing ringtone %s", ringtone);
			}

			ringtone.play();
			Utils.showToast(mContext, "Is Playing: %s", ringtone.isPlaying());
		} catch (Exception ex) {
			Utils.showToast(mContext, "Error playing ringtone Uri %s: %s", soundUri, ex.getLocalizedMessage());
		}
	}

	public void playRingtone() {
		playRingtone(mSoundUri);
	}

	public Notification notify(String title, String description) {
		if (Utils.isNullOrEmpty(title))
			title = mContext.getString(R.string.app_name);
		if (Utils.isNullOrEmpty(description))
			description = mContext.getString(R.string.app_is_running);

		mPersistentNotification = buildNotification(title, description)
				.build();

		Timber.d("Notifying with title \"%s\", description \"%s\"", title, description);
		mNotificationManager.notify(PERSISTENT_NOTIFICATION_ID, mPersistentNotification);

		return mPersistentNotification;
	}

	private NotificationCompat.Builder buildNotification(String title, String description) {
		Intent intent = new Intent(mContext, MainActivity.class);
		intent.setAction(Intent.ACTION_MAIN);
		PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, CHANNEL_ID)
				.setColor(ContextCompat.getColor(mContext, R.color.notificationBar))
				.setStyle(new NotificationCompat.BigTextStyle().bigText(description))
				.setSmallIcon(android.R.drawable.ic_dialog_alert)
				.setContentTitle(title)
				.setContentText(description)
				.setContentIntent(pendingIntent)
				.setPriority(NotificationCompat.PRIORITY_HIGH);

		// Determines whether notification is affected by Do Not Disturb mode
		if (Utils.isAtLeastL())
			builder.setCategory(Notification.CATEGORY_ALARM);

		// Set sound to play on default stream. On Oreo and newer, this value is ignored in favor of
		// the value set on the notification channel
		builder.setSound(mSoundUri);

		return builder;
	}

	public void cancelForegroundNotification() {
		if (mNotificationManager != null)
			mNotificationManager.cancel(PERSISTENT_NOTIFICATION_ID);
	}

	@RequiresApi(Build.VERSION_CODES.O)
	private NotificationChannel createNotificationChannel(Context context, NotificationManagerCompat nm) {
		String name = context.getString(R.string.app_name);
		String description = context.getString(R.string.app_is_running);
		Timber.v("Creating notification channel %s", CHANNEL_ID);

		AudioAttributes attribs = new AudioAttributes.Builder()
				.setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
				.build();

		description = String.format("Flags: %X, Usage: %X, VCS: %X", attribs.getFlags(), attribs.getUsage(), attribs.getVolumeControlStream());
		NotificationChannel ch = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);
		ch.setDescription(description);
		ch.enableLights(true);
		ch.enableVibration(true);
		ch.setSound(mSoundUri, attribs);

		// Register the channel with the system; Can't change the importance or other notification behaviors after this
		if (nm != null) {
			nm.createNotificationChannel(ch);
			return ch;
		}

		Timber.e("Error getting system notification service (null received)");
		return null;
	}
}
