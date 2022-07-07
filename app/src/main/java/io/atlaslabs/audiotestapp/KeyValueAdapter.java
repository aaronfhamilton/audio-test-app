package io.atlaslabs.audiotestapp;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;

import io.atlaslabs.audiotestapp.util.KeyValuePair;

public class KeyValueAdapter<K, V> extends ArrayAdapter<KeyValuePair<K, V>> {
	public KeyValueAdapter(@NonNull Context context, int resource, @NonNull List<KeyValuePair<K, V>> objects) {
		super(context, resource, objects);
	}

	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		return super.getView(position, convertView, parent);
	}
}
