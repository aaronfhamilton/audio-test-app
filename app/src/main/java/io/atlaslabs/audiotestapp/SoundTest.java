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

import timber.log.Timber;

public class SoundTest implements MediaPlayer.OnErrorListener,
		MediaPlayer.OnInfoListener, MediaPlayer.OnCompletionListener {
	@RequiresApi(Build.VERSION_CODES.O)
	private static final AudioRouting.OnRoutingChangedListener mRoutingListener = audioRouting -> Timber.i("Audio routing change: %s", audioRouting);
	private static SoundTest mInstance = null;
	private final Context mContext;
	private final Uri mSoundUri;

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

	@RequiresApi(Build.VERSION_CODES.O)
	public Thread playAudioOnBackground(String threadName) {
		Thread playAudio = new Thread(() -> {
			AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

			int sessionId = am.generateAudioSessionId();

			AudioAttributes attrib = new AudioAttributes.Builder()
					.setUsage(AudioAttributes.USAGE_ALARM)
					//.setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
					.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
					.build();

			int streamType = attrib.getVolumeControlStream();
			Ringtone ringtone = RingtoneManager.getRingtone(mContext, mSoundUri);

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

			ringtone.play();

			AudioAttributes ringtoneAttribs = ringtone.getAudioAttributes();

			do {
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

				try { Thread.sleep(500); }
				catch (Exception ignore) {
					break;
				}
			} while (ringtone.isPlaying());

			Timber.w("Ringtone no longer playing");
		});

		playAudio.setName((threadName != null && !threadName.isEmpty() ? threadName : "Audio playback thread"));

		return playAudio;
	}

	@RequiresApi(Build.VERSION_CODES.O)
	public int playMobilisAudio() {
		AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		int sessionId = am.generateAudioSessionId();
		int streamType = AudioManager.STREAM_ALARM;

		AudioAttributes attrib = new AudioAttributes.Builder()
				.setUsage(AudioAttributes.USAGE_ALARM)
				//.setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
				.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
				.build();

		Ringtone ringtone = RingtoneManager.getRingtone(mContext, mSoundUri);

		Timber.e("Ringtone: streamType = %d", ringtone.getStreamType());

		AudioAttributes existingAttribs = ringtone.getAudioAttributes();

		Timber.d("Existing attribs: vcs = %d, flags = %d, content = %d, usage = %d",
				existingAttribs.getVolumeControlStream(),
				existingAttribs.getFlags(), existingAttribs.getContentType(), existingAttribs.getUsage());
		ringtone.setAudioAttributes(attrib);

		Timber.d("Attribs: vcs = %d", attrib.getVolumeControlStream());
		int maxVolume = am.getStreamMaxVolume(streamType);
		int currentVolume = am.getStreamVolume(streamType);

		Timber.d("Stream %d volumes: current = %d, max = %d", streamType, currentVolume, maxVolume);
		am.setStreamVolume(streamType, currentVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

		Timber.wtf("Playing ringtone on stream type = %d", ringtone.getStreamType());
		// ringtone.setLooping(true);
		ringtone.play();
		AudioAttributes ringtoneAttribs = ringtone.getAudioAttributes();

		Timber.d("Ringtone: usage = %d, contentType = %d", ringtoneAttribs.getUsage(), ringtoneAttribs.getContentType());

		Thread playLoggingThread = new Thread(() -> {

			boolean finished = false;

			while (!finished) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException ignored) {
				}

				Timber.e("Ringtone player considers the audio is %s playing", ringtone.isPlaying() ? "" : "NOT");
			}
		});

		playLoggingThread.start();

		return sessionId;
	}

	@Override
	public void onCompletion(MediaPlayer mediaPlayer) {
		Timber.i("MediaPlayer onCompletion callback for session ID %d", mediaPlayer.getAudioSessionId());
	}

	@Override
	public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
		Timber.e("MediaPlayer onError callback for session ID %d: what = %d, extra = %d", mediaPlayer.getAudioSessionId(), what, extra);
		return false;
	}

	@Override
	public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra) {
		Timber.i("MediaPlayer onInfo callback for session ID %d: what = %d, extra = %d", mediaPlayer.getAudioSessionId(), what, extra);
		return false;
	}
}
