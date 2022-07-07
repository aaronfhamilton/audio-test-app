package io.atlaslabs.audiotestapp.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;

import io.atlaslabs.audiotestapp.R;

public class KeyValueAdapter<K, V> extends ArrayAdapter<Map.Entry<K, V>> {
	public KeyValueAdapter(@NonNull Context context, int resource, @NonNull List<Map.Entry<K, V>> objects) {
		super(context, resource, objects);
	}

	public KeyValueAdapter(Context context, int resource, int textViewResourceId, List<Map.Entry<K, V>> values) {
		super(context, resource, textViewResourceId, values);
	}
	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		return super.getView(position, convertView, parent);
		/*
		if (convertView == null)
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.layout_row, parent, false);

		final TextView txtLabel = convertView.findViewById(R.id.listRowTextView);

		Map.Entry<K, V> entry = getItem(position);
		if (entry == null || entry.getValue() == null)
			txtLabel.setText("");
		else
			txtLabel.setText(entry.getValue().toString());

		return convertView;
		 */
	}
}
