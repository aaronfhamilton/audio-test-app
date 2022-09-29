package io.atlaslabs.audiotestapp;

import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.util.Calendar;

import timber.log.Timber;

public class AlertPlayer implements IAlertPlayer {
	static final int AUDIO_STREAM_TYPE = AudioManager.STREAM_ALARM;
	private static final String TAG = "AlertPlayer";
	private final Context _applicationContext;
	private final float _defaultVolume;
	@SuppressWarnings({"unused", "FieldCanBeLocal"})
	private final float _maxVolume;
	private SoundPlayer _soundPlayer;

	public AlertPlayer(Context context) {
		Resources res = context.getResources();
		_applicationContext = context.getApplicationContext();
		_defaultVolume = Float.parseFloat(res.getString(R.string.DEFAULT_VOLUME));
		_maxVolume = Float.parseFloat(res.getString(R.string.MAX_VOLUME));
	}

	public void startSound(SoundSettings soundSettings) {
		if (_soundPlayer == null)
			_soundPlayer = new SoundPlayer(_applicationContext, AUDIO_STREAM_TYPE);

		_soundPlayer.start(soundSettings);
	}

	public void stopSound() {
		if (_soundPlayer != null)
			_soundPlayer.stop();
	}

	private class SoundPlayer {
		private static final int AUDIO_RESOURCE_ID = R.raw.woopwoop;

		private final int _audioStreamType;
		private final AudioManager _audioManager;
		private final int _originalRingerMode;
		private final Ringtone _ringtone;
		private SoundSettings _settings;
		private int _originalVolume = 0;
		private NotificationManager mNotificationManager;

		private boolean _isStopped = true;
		private long _currentPlayID;
		private Context context;
		private int interruptionFilter;

		SoundPlayer(Context _context, int audioStreamType) {
			context = _context;
			Uri audioUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + AUDIO_RESOURCE_ID);
			_audioStreamType = audioStreamType;
			_audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
			mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			try {
				_originalVolume = _audioManager != null ? _audioManager.getStreamVolume(_audioStreamType) : 0;
			} catch (Exception e) {
				Log.e(TAG, "SoundPlayer.getStreamVolume: " + e.getMessage());
			}
			_originalRingerMode = _audioManager != null ? _audioManager.getRingerMode() : 0;
			_ringtone = RingtoneManager.getRingtone(context, audioUri);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				AudioAttributes aa = new AudioAttributes.Builder()
						.setUsage(AudioAttributes.USAGE_ALARM)
						.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
						.build();
				_ringtone.setAudioAttributes(aa);
			} else {
				//noinspection deprecation
				_ringtone.setStreamType(AudioManager.STREAM_ALARM);
			}
		}

		private void setSoundVolume(int volume) {
			setSoundVolume(volume, false);
		}

		private void setSoundVolume(int volume, boolean suppressMessages) {
			boolean okToSetVolume = true;
			if (android.os.Build.VERSION.SDK_INT < 23) {
				int dndSetting;
				try {
					dndSetting = Settings.Global.getInt(context.getContentResolver(), "zen_mode");
				} catch (Exception e) {
					dndSetting = -1; // -1 will be interpreted later as NULL
					okToSetVolume = false;
				}
				if (dndSetting == 2) { // total silence
					okToSetVolume = false;
				}
			} else {
				try {
					interruptionFilter = mNotificationManager.getCurrentInterruptionFilter();
					if (interruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE || interruptionFilter == NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
						if (mNotificationManager.isNotificationPolicyAccessGranted()) {
							mNotificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
						} else {
							okToSetVolume = false;
						}
					}
				} catch (Exception e) {
					okToSetVolume = false;
				}
			}
			if (okToSetVolume) {
				try {
					_audioManager.setStreamVolume(_audioStreamType, volume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
				} catch (Exception e) {
					// removed
				}
			}
		}

		private void setCurrentRingerMode(int ringerMode) {
			boolean okToSetRinger = true;
			if (android.os.Build.VERSION.SDK_INT < 23) {
				int dndSetting;
				try {
					dndSetting = Settings.Global.getInt(context.getContentResolver(), "zen_mode");
				} catch (Exception e) {
					dndSetting = -1; // -1 will be interpreted later as NULL
					okToSetRinger = false;
				}
				if (dndSetting == 2) { // total silence
					okToSetRinger = false;
					// showDoNotDisturbWarning("setCurrentRingerMode");
				}
			} else {
				if (!mNotificationManager.isNotificationPolicyAccessGranted()) {
					okToSetRinger = false;
					//showDoNotDisturbWarning("setCurrentRingerMode");
				} else {
					try {
						interruptionFilter = mNotificationManager.getCurrentInterruptionFilter();
						if (interruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
							if (mNotificationManager.isNotificationPolicyAccessGranted()) {
								mNotificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
							}
						}
					} catch (Exception e) {
						okToSetRinger = false;
					}
				}
			}
			if (okToSetRinger) {
				try {
					_audioManager.setRingerMode(ringerMode);
				} catch (Exception e) {
					//removed
				}
			}
		}

		public void start(SoundSettings settings) {

			final long playID = Calendar.getInstance().getTimeInMillis();

			_settings = settings;
			_currentPlayID = playID;
			// _ringtone.stop();    <---- prevents ringtone from playing

			if (_settings.getIsEnabled() && _settings.getVolume() != 0) {
				_isStopped = false;

				if (_settings.getVolume() != _defaultVolume) {
					int volume = (int) (_audioManager.getStreamMaxVolume(_audioStreamType) * _settings.getVolume());
					try {
						setSoundVolume(volume);
					} catch (Exception e) {
						Log.e(TAG, "SoundPlayer.start.setStreamVolume: " + e.getMessage());
					}
					try {
						setCurrentRingerMode(AudioManager.RINGER_MODE_NORMAL);
					} catch (Exception e) {
						Log.e(TAG, "SoundPlayer.start.setRingerMode: " + e.getMessage());
					}
				}

				_ringtone.play();

				Thread playCompletionThread = new Thread(new Runnable() {
					@Override
					public void run() {
						Thread.currentThread().setName("playCompletionThread");

						boolean finished = false;
						while (!finished) {
							while (_ringtone.isPlaying()) {
								try {
									Thread.sleep(100);
								} catch (InterruptedException ignored) {
								}
							}

							// If playID does not equal _currentPlayID, it indicates that start() was subsequently called; i.e., a new sound was started.
							if (playID != _currentPlayID) {
								finished = true;
							} else if (!_isStopped) {
								if (!_settings.getLooping()) {
									stop();
								} else {
									try {
										Thread.sleep(_settings.getInterval() * 1000);
										if (!_isStopped) {
											Timber.i("Playing ringtone on thread \"%s\" ", Thread.currentThread().getName());
											_ringtone.play();
										}
									} catch (InterruptedException ignored) {
									}
								}
							}
						}

						Timber.w("Finished playing ringtone on thread \"%s\"", Thread.currentThread().getName());
					}
				});

				playCompletionThread.start();
			}
		}

		void stop() {
			_isStopped = true;
			_ringtone.stop();

			if (_settings.getVolume() != _defaultVolume) {
				try {
					setSoundVolume(_originalVolume, true);
				} catch (Exception e) {
					Log.e(TAG, "SoundPlayer.stop.setStreamVolume: " + e.getMessage());
				}
				try {
					//_audioManager.setRingerMode(_originalRingerMode);
					setCurrentRingerMode(_originalRingerMode);
				} catch (Exception e) {
					Log.e(TAG, "SoundPlayer.stop.setRingerMode: " + e.getMessage());
				}
			}
			if (android.os.Build.VERSION.SDK_INT > 23 && interruptionFilter > NotificationManager.INTERRUPTION_FILTER_ALL) {
				if (mNotificationManager.isNotificationPolicyAccessGranted()) {
					mNotificationManager.setInterruptionFilter(interruptionFilter);
				}
			}
		}
	}
}