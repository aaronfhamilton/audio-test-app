package io.atlaslabs.audiotestapp;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
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

import java.util.List;

import io.atlaslabs.audiotestapp.util.Utils;
import timber.log.Timber;

public class SoundTest implements MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener,
		MediaPlayer.OnInfoListener, MediaPlayer.OnCompletionListener {
	@RequiresApi(Build.VERSION_CODES.O)
	private static final AudioRouting.OnRoutingChangedListener mRoutingListener = audioRouting -> Timber.i("Audio routing change: %s", audioRouting);
	private static final int mDelayMilliSec = 10000;
	private static SoundTest mInstance = null;
	private static volatile boolean mSoundLoop = true;
	private final Context mContext;
	private final Uri mSoundUri;
	private Thread mPlaybackThread = null;

	private SoundTest(Application app) {
		mContext = app;
		String uriPrefix = ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + app.getPackageName() + "/";
		mSoundUri = Uri.parse(uriPrefix + R.raw.woopwoop);
	}

	public static void setup(Application app) {
		if (mInstance != null)
			return;

		mInstance = new SoundTest(app);
	}

	public static SoundTest getInstance() {
		return mInstance;
	}

	public void play() {
		mSoundLoop = true;

		if (!Utils.isAtLeastO() || mPlaybackThread != null)
			return;

		mPlaybackThread = getPlaybackThread("SoundTest Playback");
		mPlaybackThread.start();
	}

	public void stop() {
		mSoundLoop = false;

		if (mPlaybackThread != null && mPlaybackThread.isAlive()) {
			try {
				mPlaybackThread.join(30000);
			} catch (InterruptedException ignored) {
			}

			mPlaybackThread = null;
		}
	}

	@RequiresApi(Build.VERSION_CODES.O)
	private Thread getPlaybackThread(String threadName) {
		Thread playAudio = new Thread(() -> {
			AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

			AudioAttributes attrib = new AudioAttributes.Builder()
					.setUsage(AudioAttributes.USAGE_ALARM)
					//.setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
					.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
					.build();

			int streamType = attrib.getVolumeControlStream();
			Ringtone ringtone = RingtoneManager.getRingtone(mContext, mSoundUri);
			ringtone.setStreamType(RingtoneManager.TYPE_ALARM);

			AudioAttributes existingAttribs = ringtone.getAudioAttributes();

			Timber.i("AudioAttributes (existing): vcs = %d, flags = %d, content = %d, usage = %d",
					existingAttribs.getVolumeControlStream(),
					existingAttribs.getFlags(), existingAttribs.getContentType(), existingAttribs.getUsage());
			ringtone.setAudioAttributes(attrib);

			Timber.i("AudioAttributes (new): vcs = %d", attrib.getVolumeControlStream());
			int maxVolume = am.getStreamMaxVolume(streamType);
			int currentVolume = am.getStreamVolume(streamType);

			Timber.i("Stream type %d volumes: current = %d, max = %d", streamType, currentVolume, maxVolume);
			am.setStreamVolume(streamType, currentVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

			List<AudioPlaybackConfiguration> configs = am.getActivePlaybackConfigurations();

			for (AudioPlaybackConfiguration c : configs) {
				AudioAttributes attr = c.getAudioAttributes();
				int vcs = attr.getVolumeControlStream();
				Timber.i("AudioPlaybackConfiguration:\r\n" +
								"\tAudioAttributes: vcs = %d, content = %d, usage = %d, flags = %d",
						vcs, attr.getContentType(), attr.getUsage(), attr.getFlags()
				);

				Timber.i("Volumes for VCS %d:\r\n" +
						"\tCurrent: %d\r\n" +
						"\tMax: %d", vcs, am.getStreamVolume(vcs), am.getStreamMaxVolume(vcs));
			}

			Timber.i("AudioManager:\r\n" +
							"\tMode: %d\r\n" +
							"\tRinger Mode: %d\r\n" +
							"\tisStreamMute: %s\r\n"
					, am.getMode(), am.getRingerMode(), am.isStreamMute(streamType));

			AudioDeviceInfo[] devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
			for (AudioDeviceInfo d : devices) {
				Timber.i("AudioDeviceInfo (ID %d): \r\n" +
								"\tProduct Name: %s\r\n" +
								"\tType: %d\r\n" +
								"\tisSink: %s", d.getId(),
						d.getProductName(), d.getType(), d.isSink());
			}

			do {
				// On rockchip hardware, this only seems to work once
				ringtone.play();

				do {
					AudioAttributes ringtoneAttribs = ringtone.getAudioAttributes();

					Timber.i("Playing ringtone on thread \"%s\" (priority %d):\r\n" +
									"\tusage = %d\r\n" +
									"\tcontent type = %d\r\n" +
									"\tflags = %d\r\n" +
									"\tstream type = %d\r\n" +
									"\tAudioManager mode: %d\r\n" +
									"\tAudioManager ringer mode: %d\r\n" +
									"\tisStreamMute (stream %d): %s\r\n" +
									"\tisMusicActive: %s",
							Thread.currentThread().getName(), Thread.currentThread().getPriority(),
							ringtoneAttribs.getUsage(), ringtoneAttribs.getContentType(), ringtoneAttribs.getFlags(),
							ringtone.getStreamType(), am.getMode(), am.getRingerMode(), streamType, am.isStreamMute(streamType),
							am.isMusicActive());

					try {
						Thread.sleep(500);
					} catch (InterruptedException ignore) {
						break;
					}
				} while (ringtone.isPlaying());

				try {
					Thread.sleep(mDelayMilliSec);
				} catch (InterruptedException ignore) {
					break;
				}
			} while (mSoundLoop);

			Timber.w("Ringtone no longer playing");
		});

		playAudio.setName((threadName != null && !threadName.isEmpty() ? threadName : "Audio playback thread"));

		return playAudio;
	}

	@Override
	public void onPrepared(MediaPlayer mediaPlayer) {
		Timber.i("MediaPlayer session ID %d onPrepared", mediaPlayer.getAudioSessionId());
	}

	@Override
	public void onCompletion(MediaPlayer mediaPlayer) {
		Timber.i("MediaPlayer onCompletion callback for session ID %d", mediaPlayer.getAudioSessionId());
	}

	/**
	 * Error occurred during an asynchronous operation
	 *
	 * @param mediaPlayer
	 * @param what        type of error that occurred:
	 *                    - MediaPlayer.MEDIA_ERROR_UNKNOWN, or
	 *                    - MediaPlayer.MEDIA_ERROR_SERVER_DIED
	 * @param extra       code specific to the error (typically implementation dependent)
	 *                    - MediaPlayer.MEDIA_ERROR_IO
	 *                    - MediaPlayer.MEDIA_ERROR_MALFORMED
	 *                    - MediaPlayer.MEDIA_ERROR_UNSUPPORTED
	 *                    - MediaPlayer.MEDIA_ERROR_TIMED_OUT
	 *                    - MEDIA_ERROR_SYSTEM (-2147483648) - low-level system error
	 * @return True if the method handled the error, false if it didn't. Returning false,
	 * or not having an OnErrorListener at all, will cause the OnCompletionListener to be called
	 */
	@Override
	public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
		Timber.e("MediaPlayer onError callback for session ID %d: what = %d, extra = %d", mediaPlayer.getAudioSessionId(), what, extra);

		// To allow playing after error, we must reset and return true
		mediaPlayer.reset();

		return true;
	}

	/**
	 * Called to indicate info or warning on async operation
	 *
	 * @param mediaPlayer
	 * @param what        the type of info or warning:
	 *                    - MediaPlayer.MEDIA_INFO_UNKNOWN
	 *                    - MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING
	 *                    - MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START
	 *                    - MediaPlayer.MEDIA_INFO_BUFFERING_START
	 *                    - MediaPlayer.MEDIA_INFO_BUFFERING_END
	 *                    - MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING
	 *                    - MediaPlayer.MEDIA_INFO_NOT_SEEKABLE
	 *                    - MediaPlayer.MEDIA_INFO_METADATA_UPDATE
	 *                    - MediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE
	 *                    - MediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT
	 * @param extra       an extra code, specific to the info/warning
	 * @return True if the method handled the info, false if it didn't. Returning false, or not
	 * having an OnInfoListener at all, will cause the info to be discarded.
	 */
	@Override
	public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra) {
		Timber.i("MediaPlayer onInfo callback for session ID %d: what = %d, extra = %d", mediaPlayer.getAudioSessionId(), what, extra);
		return false;
	}
}
