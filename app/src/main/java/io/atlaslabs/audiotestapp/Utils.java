package io.atlaslabs.audiotestapp;

import android.os.Build;

public class Utils {
	public static boolean isAtLeastO() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
	}

	public static boolean isAtLeastL() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
	}
}
