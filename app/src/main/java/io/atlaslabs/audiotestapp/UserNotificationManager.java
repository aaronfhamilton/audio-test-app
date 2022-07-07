package io.atlaslabs.audiotestapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import timber.log.Timber;

public class UserNotificationManager {
	private static final String CHANNEL_ID = "io.atlaslabs.audiotestapp";
	public static final int PERSISTENT_NOTIFICATION_ID = 1;

	private final String mUriPrefix;
	private final Ringtone mRingtone;
	private final Uri mSoundUri;

	private final Uri mDefaultAlarmUri;
	private final Uri mDefaultRingtone;
	private final Uri mDefaultNotification;

	private final Context mContext;

	private final NotificationChannel mNotificationChannel;
	private final NotificationManager mNotificationManager;

	private Notification mPersistentNotification = null;

	public UserNotificationManager(Context context){
		mContext = context;
		mUriPrefix = ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/";
		mRingtone = RingtoneManager.getRingtone(context, Uri.parse(mUriPrefix + R.raw.chime));
		mSoundUri = Uri.parse(mUriPrefix + R.raw.chime);

		mDefaultAlarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
		mDefaultNotification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		mDefaultRingtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

		mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		mNotificationChannel = Utils.isAtLeastO() ? createNotificationChannel(mContext, mNotificationManager) : null;
	}

	public void playMedia(){
		MediaPlayer mp = MediaPlayer.create(mContext, mSoundUri);
		mp.start();
	}

	public void playRingtone(){
		try {
			Toast.makeText(mContext, "Playing Default Ringtone", Toast.LENGTH_LONG).show();
			mRingtone.play();
		} catch (Exception ex) {
			Timber.e(ex, "Error playing default ringtone: %s", ex.getLocalizedMessage());
		}
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

		if (Utils.isAtLeastL())
			builder.setCategory(Notification.CATEGORY_SERVICE);

		if (Utils.isAtLeastO())
			builder.setSound(Uri.parse(mUriPrefix + R.raw.chime));

		return builder;
	}

	public void cancelForegroundNotification() {
		if (mNotificationManager != null)
			mNotificationManager.cancel(PERSISTENT_NOTIFICATION_ID);
	}

	@RequiresApi(Build.VERSION_CODES.O)
	private NotificationChannel createNotificationChannel(Context context, NotificationManager nm) {
		String name = context.getString(R.string.app_name);
		String description = context.getString(R.string.app_is_running);
		Timber.v("Creating notification channel %s", CHANNEL_ID);

		AudioAttributes attribs = new AudioAttributes.Builder()
				.setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
				.build();

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
