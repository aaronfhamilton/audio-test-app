package io.atlaslabs.audiotestapp;

import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import io.atlaslabs.audiotestapp.databinding.FragmentTestingBinding;
import io.atlaslabs.audiotestapp.util.KeyValuePair;

public class TestingFragment extends Fragment implements
		AdapterView.OnItemSelectedListener {
	static final List<KeyValuePair<Integer, String>> mRingtoneTypes;
	private static final int RINGTONE_REQUEST_CODE = 1;

	static {
		mRingtoneTypes = new ArrayList<>();
		mRingtoneTypes.add(new KeyValuePair<>(RingtoneManager.TYPE_ALL, "All"));
		mRingtoneTypes.add(new KeyValuePair<>(RingtoneManager.TYPE_ALARM, "Alarm"));
		mRingtoneTypes.add(new KeyValuePair<>(RingtoneManager.TYPE_NOTIFICATION, "Notification"));
		mRingtoneTypes.add(new KeyValuePair<>(RingtoneManager.TYPE_RINGTONE, "Ringtone"));
	}

	private FragmentTestingBinding binding;
	private UserNotificationManager mUserNotificationMgr;

	@Override
	public View onCreateView(
			LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState
	) {
		binding = FragmentTestingBinding.inflate(inflater, container, false);
		loadSpinner(binding.getRoot().getContext());
		return binding.getRoot();
	}

	private void loadSpinner(Context context) {
		KeyValueAdapter<Integer, String> adapter = new KeyValueAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, mRingtoneTypes);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		binding.spinnerRingtoneType.setAdapter(adapter);
		// binding.spinnerRingtoneType.setOnItemClickListener(this::onItemSelected);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}

	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		mUserNotificationMgr = new UserNotificationManager(view.getContext());

		binding.buttonPlayAlarm.setOnClickListener(view1 -> {
			// NavHostFragment.findNavController(FirstFragment.this)
			//		.navigate(R.id.action_FirstFragment_to_SecondFragment);
		});

		binding.buttonPlayRingtone.setOnClickListener(view1 -> {
			mUserNotificationMgr.playRingtone();
		});
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}

	@Override
	public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
		KeyValuePair<Integer, String> selected = (KeyValuePair<Integer, String>) binding.spinnerRingtoneType.getSelectedItem();
		String title = getString(R.string.x_ringtone, selected.getValue());
		Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, selected.getKey());
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, title);
		startActivityForResult(intent, RINGTONE_REQUEST_CODE);
	}

	@Override
	public void onNothingSelected(AdapterView<?> adapterView) {
	}
}