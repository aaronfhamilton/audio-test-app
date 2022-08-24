package io.atlaslabs.audiotestapp.fragments;

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
import java.util.Date;
import java.util.Map;

import io.atlaslabs.audiotestapp.MainViewModel;
import io.atlaslabs.audiotestapp.R;
import io.atlaslabs.audiotestapp.SoundTest;
import io.atlaslabs.audiotestapp.UserNotificationManager;
import io.atlaslabs.audiotestapp.databinding.FragmentTestingBinding;
import io.atlaslabs.audiotestapp.util.KeyValueAdapter;
import io.atlaslabs.audiotestapp.util.RingtonePicker;
import io.atlaslabs.audiotestapp.util.Utils;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.SerialDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class TestingFragment extends Fragment implements
		AdapterView.OnItemSelectedListener {

	private static final int RINGTONE_REQUEST_CODE = 1;
	private final SerialDisposable mDisposable = new SerialDisposable();
	private FragmentTestingBinding binding;
	private Uri mSoundUri = null;
	private MainViewModel mViewModel;
	private Thread mAudioPlaybackThread = null;

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

		loadSpinner(binding.getRoot().getContext());
		// binding.spinnerRingtoneType.setOnItemSelectedListener(this);

		mViewModel = new MainViewModel(getActivity().getApplication());

		binding.buttonNotify.setOnClickListener(view1 -> {
			UserNotificationManager.getInstance().notify("Notification Update",
					String.format("Notification updated at %s", new Date()));
		});

		binding.buttonPlayMedia.setOnClickListener(view1 -> {
			if (Utils.isAtLeastO())
				UserNotificationManager.getInstance().playMedia(mSoundUri);
		});

		binding.buttonPlayRingtone.setOnClickListener(view1 -> {
			UserNotificationManager.getInstance().playRingtone(mSoundUri);
		});

		binding.buttonCloseActivity.setOnClickListener(view1 -> {
			getActivity().finish();
		});

		if (Utils.isAtLeastO()) {
			// int sessionId = SoundTest.getInstance().playMobilisAudio();

			// Timber.i("Playing Mobilis sound using session ID %d", sessionId);

			mAudioPlaybackThread = SoundTest.getInstance().playAudioOnBackground(null);
			mAudioPlaybackThread.start();
		}
	}

	private void loadSpinner(Context context) {
		KeyValueAdapter<Integer, String> adapter = new KeyValueAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>(RingtonePicker.RingtoneTypes.entrySet()));
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		binding.spinnerRingtoneType.setAdapter(adapter);

		// binding.spinnerRingtoneType.setOnItemSelectedListener(this);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent result) {
		if (resultCode != Activity.RESULT_OK || result == null) {
			mSoundUri = null;
			return;
		}

		mSoundUri = result.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
		// mUserNotificationMgr.playMedia(mSoundUri);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		if (mAudioPlaybackThread != null && mAudioPlaybackThread.isAlive()) {
			try { mAudioPlaybackThread.join(30000); }
			catch (Exception ignored) { }
		}

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