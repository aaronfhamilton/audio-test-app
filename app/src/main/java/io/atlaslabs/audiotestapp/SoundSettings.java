package io.atlaslabs.audiotestapp;

import android.content.Context;
import android.content.res.Resources;

public class SoundSettings {
	private static final int MAX_INTERVAL = 120;        // in seconds

	private boolean _isEnabled = true;      // This must be true to enable sound; otherwise, all other properties have no effect.
	private float _volume;      // Set between 0.0 and 1.0 to represent a fraction of the device's maximum volume.  Set to -1.0 to not change the current volume of the device.
	private boolean _looping;
	private int _interval;        // in seconds; if looping is true, interval determines the pause time between play iterations; otherwise, interval has no effect
	private float defaultVolume;
	private float maxVolume;
	private Context _context;

	SoundSettings(Context context, float volume, boolean looping, int interval_sec) {
		_context = context;
		initDefaultMaxValues();
		setVolume(volume);
		setLooping(looping);
		setInterval(interval_sec);
	}

	SoundSettings(Context context, SoundSettings settings) {
		_context = context;
		initDefaultMaxValues();
		setIsEnabled(settings.getIsEnabled());
		setVolume(settings.getVolume());
		setLooping(settings.getLooping());
		setInterval(settings.getInterval());
	}

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

	private void initDefaultMaxValues() {
		Resources res = _context.getResources();
		defaultVolume = Float.parseFloat(res.getString(R.string.DEFAULT_VOLUME));
		maxVolume = Float.parseFloat(res.getString(R.string.MAX_VOLUME));
	}
}
