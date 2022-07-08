package io.atlaslabs.audiotestapp.util;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import java.io.InputStream;
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

	public static boolean isNullOrEmpty(String string) {
		return string == null || string.isEmpty();
	}

	public static boolean isAtLeastM() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
	}

	/**
	 * Returns true if contents at URI exist
	 * @param uri
	 */
	public static boolean uriExists(Context context, Uri uri) {
		if (uri == null)
			return false;

		try {
			InputStream inputStream = context.getContentResolver().openInputStream(uri);
			inputStream.close();
			return true;
		} catch (Exception ex) {
			Timber.e(ex, "Error checking existance of Uri %s: %s", uri, ex.getLocalizedMessage());
		}

		return false;
	}

	public static void showToast(Context context, String format, Object... args) {
		String message = String.format(Locale.getDefault(), format, args);

		Timber.i(message);
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}
}
