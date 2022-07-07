package io.atlaslabs.audiotestapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Map;

import io.atlaslabs.audiotestapp.databinding.FragmentTestingBinding;
import io.atlaslabs.audiotestapp.util.KeyValueAdapter;
import io.atlaslabs.audiotestapp.util.RingtonePicker;

public class TestingFragment extends Fragment implements
		AdapterView.OnItemSelectedListener {

	private static final int RINGTONE_REQUEST_CODE = 1;

	private FragmentTestingBinding binding;
	private UserNotificationManager mUserNotificationMgr;

	@Override
	public View onCreateView(
			LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState
	) {
		binding = FragmentTestingBinding.inflate(inflater, container, false);
		return binding.getRoot();
	}

	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		mUserNotificationMgr = new UserNotificationManager(view.getContext());
		binding.spinnerRingtoneType.setOnItemSelectedListener(this);
		loadSpinner(binding.getRoot().getContext());

		binding.buttonPlayMedia.setOnClickListener(view1 -> {
			// mUserNotificationMgr.playDefaultMedia();
		});

		binding.buttonPlayRingtone.setOnClickListener(view1 -> {
			mUserNotificationMgr.playRingtone();
		});
	}

	private void loadSpinner(Context context) {
		KeyValueAdapter<Integer, String> adapter = new KeyValueAdapter<Integer, String>(context, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>(RingtonePicker.RingtoneTypes.entrySet()));
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		binding.spinnerRingtoneType.setAdapter(adapter);

		binding.spinnerRingtoneType.setOnItemSelectedListener(this);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent result) {
		if (resultCode != Activity.RESULT_OK || result == null)
			return;

		Uri soundUri = result.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
		mUserNotificationMgr.playMedia(soundUri);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		Map.Entry<Integer, String> selected = (Map.Entry<Integer, String>) parent.getItemAtPosition(pos);
		String title = getString(R.string.x_ringtone, selected.getValue());
		Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
		intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, selected.getKey());
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, title);
		startActivityForResult(intent, RINGTONE_REQUEST_CODE);
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
	}
}