package io.atlaslabs.audiotestapp.util;

import android.content.Context;
import android.os.Build;
import android.widget.Toast;

import java.util.Locale;

import timber.log.Timber;

public class Utils {
	// Can't be instantiated
	private Utils() {
	}

	public static long nowUtc() {
		return System.currentTimeMillis();
	}

	public static boolean isAtLeastO() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
	}

	public static boolean isAtLeastL() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
	}

	public static boolean isAtLeastM() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
	}

	public static void showToast(Context context, String format, Object... args) {
		String message = String.format(Locale.getDefault(), format, args);

		Timber.d(format, args);
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}
}
