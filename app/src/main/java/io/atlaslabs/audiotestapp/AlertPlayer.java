import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

class AlertPlayer {
    private static final String TAG = "AlertPlayer";
    static final int AUDIO_STREAM_TYPE = AudioManager.STREAM_ALARM;

    private final Context _applicationContext;
    private SoundPlayer _soundPlayer;
    private float defaultVolume;
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private float maxVolume;

    AlertPlayer(Context context) {
        Resources res = context.getResources();
        _applicationContext = context.getApplicationContext();
        defaultVolume = Float.parseFloat(res.getString(R.string.DEFAULT_VOLUME));
        maxVolume = Float.parseFloat(res.getString(R.string.MAX_VOLUME));
    }

    void startSound(SoundSettings soundSettings) {
        if (_soundPlayer == null)
            _soundPlayer = new SoundPlayer(_applicationContext, AUDIO_STREAM_TYPE);

        _soundPlayer.start(soundSettings);
    }


    void stopSound() {
        if (_soundPlayer != null)
            _soundPlayer.stop();
    }


    // ********** SoundPlayer **********

    static class SoundSettings {
        private static final int MAX_INTERVAL = 120;        // in seconds

        private boolean _isEnabled = true;      // This must be true to enable sound; otherwise, all other properties have no effect.
        private float _volume;      // Set between 0.0 and 1.0 to represent a fraction of the device's maximum volume.  Set to -1.0 to not change the current volume of the device.
        private boolean _looping;
        private int _interval;        // in seconds; if looping is true, interval determines the pause time between play iterations; otherwise, interval has no effect
        private float defaultVolume;
        private float maxVolume;
        private Context _context;

        boolean getIsEnabled() {
            return _isEnabled;
        }

        void setIsEnabled(boolean value) {
            _isEnabled = value;
        }

        float getVolume() {
            return _volume;
        }

        void setVolume(float value) {
            _volume = Math.min(value, maxVolume);

            if (_volume < 0)
                _volume = defaultVolume;
        }

        boolean getLooping() {
            return _looping;
        }

        void setLooping(boolean value) {
            _looping = value;
        }

        int getInterval() {
            return _interval;
        }

        void setInterval(int value) {
            _interval = Math.min(Math.max(value, 0), MAX_INTERVAL);
        }

        private void InitDefaultMaxValues() {
            Resources res = _context.getResources();
            defaultVolume = Float.parseFloat(res.getString(R.string.DEFAULT_VOLUME));
            maxVolume = Float.parseFloat(res.getString(R.string.MAX_VOLUME));
        }

        SoundSettings(Context context, float volume, boolean looping, int interval) {
            _context = context;
            InitDefaultMaxValues();
            setVolume(volume);
            setLooping(looping);
            setInterval(interval);
        }

        SoundSettings(Context context, SoundSettings settings) {
            _context = context;
            InitDefaultMaxValues();
            setIsEnabled(settings.getIsEnabled());
            setVolume(settings.getVolume());
            setLooping(settings.getLooping());
            setInterval(settings.getInterval());
        }
    }

    private class SoundPlayer {
        private static final int AUDIO_RESOURCE_ID = R.raw.woopwoop;

        private final int _audioStreamType;
        private SoundSettings _settings;
        private final AudioManager _audioManager;
        private int _originalVolume = 0;
        private final int _originalRingerMode;
        private final Ringtone _ringtone;
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
                    showDoNotDisturbWarning("setCurrentRingerMode");
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
            _ringtone.stop();

            if (_settings.getIsEnabled() && _settings.getVolume() != 0) {
                _isStopped = false;

                if (_settings.getVolume() != defaultVolume) {
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
                                        if (!_isStopped)
                                            _ringtone.play();
                                    } catch (InterruptedException ignored) {
                                    }
                                }
                            }
                        }
                    }
                });

                playCompletionThread.start();
            }
        }

        void stop() {
            _isStopped = true;
            _ringtone.stop();

            if (_settings.getVolume() != defaultVolume) {
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