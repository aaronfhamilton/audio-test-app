package io.atlaslabs.audiotestapp.activities;

import android.Manifest;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.atlaslabs.audiotestapp.R;
import io.atlaslabs.audiotestapp.util.KeyValueAdapter;
import io.atlaslabs.audiotestapp.util.KeyValuePair;
import io.atlaslabs.audiotestapp.util.PermissionUtil;
import io.atlaslabs.audiotestapp.util.Utils;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class PermissionsActivity extends ListActivity {
	private static final long sMAX_PERMISSION_PROCESSING_DELAY_MS = 5000;
	private final List<Map.Entry<String, String>> mNeededPermissions = new ArrayList<>();
	private KeyValueAdapter<String, String> mPermissionsAdapter;
	private Disposable mDisposable;

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (permissions.length == 0)
			return;

		if (requestCode == PermissionUtil.PERMISSIONS_REQUEST_SINGLE_CODE ||
				requestCode == PermissionUtil.PERMISSIONS_REQUEST_MULTIPLE_CODE ||
				requestCode == PermissionUtil.PERMISSIONS_WRITE_SETTINGS_CODE ||
				requestCode == PermissionUtil.PERMISSIONS_DRAW_OVERLAYS_CODE) {
			// Handle result and request next permission (if any)
			if (!PermissionUtil.onRequestPermissionsResult(this, requestCode, permissions, grantResults)) {
				Timber.i("Permission(s) %s processed", TextUtils.join(", ", permissions));
			}
		} else
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (PermissionUtil.neededPermissions.isEmpty()) {
			setResult(Activity.RESULT_OK);
			finish();

			return;
		}

		mNeededPermissions.clear();
		mNeededPermissions.addAll(PermissionUtil.neededPermissions.entrySet());
		mPermissionsAdapter.notifyDataSetChanged();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		if (!Utils.isAtLeastM())
			finish();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_permissions);

		// Show list of permissions we need, but aren't granted
		mPermissionsAdapter = new KeyValueAdapter<String, String>(this, R.layout.layout_row, R.id.listRowTextView, mNeededPermissions);
		setListAdapter(mPermissionsAdapter);

		if (!Utils.isAtLeastM()) {
			finish();
			return;
		}

		// Ask for one permission at a time
		mDisposable = PermissionUtil.getNeededRuntimePermissions(this, PermissionUtil.PERMISSIONS_REQUEST_SINGLE_CODE)
				.filter(pair -> PermissionUtil.checkAndRequestPermission(this, pair.first, PermissionUtil.PERMISSIONS_REQUEST_SINGLE_CODE))
				.map(pair -> {
					// Wait until no additional requests are outstanding before allowing iteration to next permission. This forces the app to only
					// request a single permission a time (due to odd error with MTCD/E devices)
					long startUtc = Utils.nowUtc();
					while (PermissionUtil.hasPendingRequests() && Utils.nowUtc() - startUtc < sMAX_PERMISSION_PROCESSING_DELAY_MS) {
						Timber.w("One or more permission requests already pending. Thread %s sleeping.", Thread.currentThread().getName());
						Thread.sleep(500);
					}
					Timber.d("Permission %s processed after %d ms delay", pair.first, Utils.nowUtc() - startUtc);
					return pair;
				})
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.doOnComplete(() -> {
					// Check if any remaining
					if (PermissionUtil.neededPermissions.size() == 0){
						Timber.i("No addition permissions required");
						finish();
					}
					mPermissionsAdapter.notifyDataSetChanged();
				})
				.subscribe(pair -> {
					Timber.i("Permission %s requested", pair.first);
				}, throwable -> Timber.e(throwable, "Error getting package runtime permissions: %s", throwable.getLocalizedMessage()));
	}

	@Override
	protected void onDestroy() {
		if (mDisposable != null && !mDisposable.isDisposed())
			mDisposable.dispose();
		super.onDestroy();
	}

	public void dismiss(View view) {
		setResult(Activity.RESULT_OK);
		finish();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		// Close permissions activity when everything is good
		if (!PermissionUtil.arePermissionsOkay(this)) {
			setResult(Activity.RESULT_OK);
			finish();
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Map.Entry<String, String> item = mPermissionsAdapter.getItem(position);

		if (item == null || item.getValue() == null)
			return;

		if (Manifest.permission.WRITE_SETTINGS.equals(item.getKey())) {
			Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
			intent.setData(Uri.parse("package:" + getPackageName()));
			startActivityForResult(intent, PermissionUtil.PERMISSIONS_WRITE_SETTINGS_CODE);
		} else {
			ActivityCompat.requestPermissions(this, new String[]{item.getKey()}, PermissionUtil.PERMISSIONS_REQUEST_SINGLE_CODE);
		}
	}
}