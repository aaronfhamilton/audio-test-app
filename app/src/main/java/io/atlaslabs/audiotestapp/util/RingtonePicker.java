package io.atlaslabs.audiotestapp.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class RingtonePicker extends ActivityResultContract<Integer, Uri> {

	public static final Map<Integer, String> RingtoneTypes = new LinkedHashMap<Integer, String>() {{
		put(RingtoneManager.TYPE_ALL, "All");
		put(RingtoneManager.TYPE_ALARM, "Alarm");
		put(RingtoneManager.TYPE_NOTIFICATION, "Notification");
		put(RingtoneManager.TYPE_RINGTONE, "Ringtone");
	}};

	@NonNull
	@Override
	public Intent createIntent(@NonNull Context context, @NonNull Integer ringtoneType) {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, ringtoneType.intValue());
		return intent;
	}

	@Override
	public Uri parseResult(int resultCode, @Nullable Intent result) {
		if (resultCode != Activity.RESULT_OK || result == null) {
			return null;
		}
		return result.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
	}
}
