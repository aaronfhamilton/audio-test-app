package io.atlaslabs.audiotestapp.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

public class KeyValuePair<K, V> {
	private final K mKey;
	private final V mValue;

	public KeyValuePair(@NonNull K key, V value) {
		mKey = key;
		mValue = value;
	}

	public V getValue() {
		return mValue;
	}

	public K getKey() {
		return mKey;
	}

	@Override
	public boolean equals(@Nullable @org.jetbrains.annotations.Nullable Object obj) {
		if (obj == null || getClass() != obj.getClass())
			return false;

		KeyValuePair<K, V> other = (KeyValuePair<K, V>) obj;

		return other.mKey == mKey;
	}

	@Override
	public int hashCode() {
		return mKey.hashCode();
	}

	@NonNull
	@NotNull
	@Override
	public String toString() {
		return mValue == null ? "" : mValue.toString();
	}
}
