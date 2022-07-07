package io.atlaslabs.audiotestapp.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.atlaslabs.audiotestapp.R;
import io.atlaslabs.audiotestapp.activities.PermissionsActivity;
import io.reactivex.Observable;
import timber.log.Timber;

public class PermissionUtil {
	public static final int PERMISSIONS_REQUEST_SINGLE_CODE = 100;
	public static final int PERMISSIONS_REQUEST_MULTIPLE_CODE = 101;
	public static final int PERMISSIONS_WRITE_SETTINGS_CODE = 102;
	public static final int PERMISSIONS_DRAW_OVERLAYS_CODE = 103;

	// Max number of times a particular permission will be requested before giving up.
	public static final int MAX_PERMISSION_REQUEST_COUNT = 3;

	/**
	 * User friendly descriptions of runtime permissions (displayed when requested permission is not granted, but needed).
	 */
	public static final Map<String, String> runtimePermissionsMap = new HashMap<String, String>() {{
		put(Manifest.permission.GET_ACCOUNTS, "Get Phone Accounts");
		put(Manifest.permission.WRITE_EXTERNAL_STORAGE, "Write Storage");
		put(Manifest.permission.READ_EXTERNAL_STORAGE, "Read Storage");
		put(Manifest.permission.ACCESS_FINE_LOCATION, "Access Fine Location");
		put(Manifest.permission.ACCESS_COARSE_LOCATION, "Access Course Location");
		put(Manifest.permission.READ_CONTACTS, "Read Contacts");
		put(Manifest.permission.WRITE_CONTACTS, "Write Contacts");
		put(Manifest.permission.WRITE_SETTINGS, "Write System Settings");
		put(Manifest.permission.SYSTEM_ALERT_WINDOW, "Overlay System Windows");
		put(Manifest.permission.READ_LOGS, "Read Logs");
		put(Manifest.permission.CALL_PHONE, "Initiate Phone Calls");
		put(Manifest.permission.READ_PHONE_STATE, "Read Phone State");
		put(Manifest.permission.CAMERA, "Access Camera");
	}};

	public static final List<String> optionalPermissions = new ArrayList<>(Arrays.asList(
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.CALL_PHONE,
			Manifest.permission.READ_CONTACTS
	));

	public static final Map<String, String> neededPermissions = new HashMap<>();
	/**
	 * Number of times each permission has been requested. Used to ensure we don't keep asking for the
	 * same permission over and over.
	 */
	public static final Map<String, Integer> permissionRequestCount = new HashMap<>();
	/**
	 * Permissions that have been requested, but not yet processed. This is used to ensure we only
	 * request one permission at a time (since the MTCD/E hardware fails otherwise)
	 */
	private static final Map<String, String> pendingPermissions = new HashMap<>();

	private PermissionUtil() {
	}

	public static boolean hasPendingRequests() {
		return pendingPermissions.size() > 0;
	}

	public static String getPermissionDisplay(String permission) {
		String display = runtimePermissionsMap.get(permission);

		return display != null ? display : permission;
	}

	/**
	 * Check if specified permission can be requested (hasn't already been asked too many times), and request
	 * if so.
	 *
	 * @param activity
	 * @param permission
	 * @param requestCode
	 * @return True if permission request can be made
	 */
	public static boolean checkAndRequestPermission(@NonNull Activity activity, String permission, int requestCode) {
		int count = permissionRequestCount.containsKey(permission) ? permissionRequestCount.get(permission) : 0;
		boolean needsPermission = ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED;
		boolean canRequest = ++count < MAX_PERMISSION_REQUEST_COUNT && needsPermission;

		if (canRequest) {
			Timber.d("Requesting permission %s (currently %d requests pending)", permission, pendingPermissions.size());
			// Keep track of which permission requests are outstanding so we don't attempt more than one request
			// at time
			pendingPermissions.put(permission, Thread.currentThread().getName());
			ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
			permissionRequestCount.put(permission, count);
		} else if (needsPermission) {
			Timber.d("Permission %s already granted", permission);
		}

		return canRequest;
	}

	/**
	 * Permissions often come in staggered, so don't assume permissions/grantResults are all present. When
	 * this returns, the neededPermissions map will contain an updated list of permissions that are still
	 * needed, but not yet granted.
	 *
	 * @param context
	 * @param permissions
	 * @param grantResults
	 * @return True when no additional permissions are still required
	 */
	public static boolean onRequestPermissionsResult(Context context, int requestCode, String[] permissions, int[] grantResults) {
		StringBuilder sb = new StringBuilder("Permissions result: ");
		for (int i = 0; i < permissions.length; i++) {
			String permission = permissions[i];
			String description = getPermissionDisplay(permission);

			pendingPermissions.remove(permission);

			if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
				if (context instanceof Activity) {
					Activity activity = (Activity) context;
					// showMessageOKCancel(activity, activity.getString(R.string.permissions_required_var, description), (dlgInterface, j) -> { });
				}
				if (optionalPermissions.contains(permission)) {
					neededPermissions.remove(permission);
				} else {
					neededPermissions.put(permission, description == null ? permission : description);
					Toast.makeText(context, context.getString(R.string.permission_var, description, context.getString(R.string.required)), Toast.LENGTH_LONG).show();
				}

				sb.append("\t" + permission + " NOT granted\n");
			} else {
				neededPermissions.remove(permission);
				permissionRequestCount.remove(permission);
				sb.append("\t" + permission + " granted\n");
			}
		}

		Timber.i(sb.toString());

		return neededPermissions.isEmpty();
	}

	/**
	 * Get ungranted, runtime permissions from PackageManager for this app.
	 *
	 * @return Pair where first is permission string and second is user-friendly display
	 */
	private static Observable<Pair<String, String>> getUngrantedPackageManagerPermissions(Context context) {
		if (!Utils.isAtLeastM())
			return Observable.empty();

		return Observable.create(emitter -> {
			try {
				neededPermissions.clear();

				PackageManager pm = context.getPackageManager();
				PackageInfo packageInfo =
						pm.getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
				if (packageInfo != null && packageInfo.requestedPermissions != null && packageInfo.requestedPermissions.length > 0) {
					for (int i = 0; i < packageInfo.requestedPermissions.length; i++) {
						String permission = packageInfo.requestedPermissions[i];
						int flags = packageInfo.requestedPermissionsFlags[i];
						if ((flags & PackageInfo.REQUESTED_PERMISSION_GRANTED) > 0)
							continue;

						if (!isRuntimePermission(pm, permission))
							continue;

						Pair<String, String> pair = new Pair<>(permission, getPermissionDisplay(permission));
						neededPermissions.put(pair.first, pair.second);
						emitter.onNext(pair);
					}
				}
				emitter.onComplete();
			} catch (Exception ex) {
				Timber.e(ex, "Error reading package permissions: %s", ex.getLocalizedMessage());
				emitter.tryOnError(ex);
			}
		});
	}

	private static boolean isRuntimePermission(PackageManager packageManager, String permission) {
		try {
			PermissionInfo pInfo = packageManager.getPermissionInfo(permission, 0);
			if (pInfo != null) {
				if ((pInfo.protectionLevel & PermissionInfo.PROTECTION_MASK_BASE)
						== PermissionInfo.PROTECTION_DANGEROUS) {
					return true;
				}
			}
		} catch (PackageManager.NameNotFoundException ignored) {
		}
		return false;
	}

	/**
	 * Check if needed runtime permissions are enabled, as well as special permissions (ability to write system settings)
	 *
	 * @param context
	 * @return
	 */
	public static boolean arePermissionsOkay(Context context) {
		if (!Utils.isAtLeastM())
			return true;

		return getUngrantedPackageManagerPermissions(context)
				.filter(pair -> ContextCompat.checkSelfPermission(context, pair.first) != PackageManager.PERMISSION_GRANTED)
				.filter(pair -> !optionalPermissions.contains(pair.first))
				.count().blockingGet() == 0;
	}

	public static boolean getNeededRuntimePermissions(Context context) {
		if (arePermissionsOkay(context))
			return true;

		Intent intent = new Intent(context, PermissionsActivity.class);
		context.startActivity(intent);

		return false;
	}

	/**
	 * Check all required app runtime permissions and attempt to request automatically. Return list
	 * of ungranted permissions and their respective descriptions.
	 *
	 * @return Observable pair of permissions where 1st = permission, 2nd = UI label/description of permission
	 */
	public static Observable<Pair<String, String>> getNeededRuntimePermissions(final Activity activity, final int requestCode) {
		if (!Utils.isAtLeastM())
			return Observable.empty();

		// First time this is run, all permissions will be ungranted
		// 1st element = permission, 2nd = user interface display of permission
		return getUngrantedPackageManagerPermissions(activity)
				.filter(pair -> ActivityCompat.checkSelfPermission(activity, pair.first) != PackageManager.PERMISSION_GRANTED)
				//.filter(permission -> !ignoredPermissions.contains(permission.first))
				.map(pair -> {
					if (ActivityCompat.shouldShowRequestPermissionRationale(activity, pair.first)) {
						showMessageOKCancel(activity, activity.getString(R.string.permissions_required_var, pair.second),
								(((dialogInterface, i) -> ActivityCompat.requestPermissions(activity, new String[]{pair.first}, requestCode))));
					} /* else {
						Timber.i("Requesting permission %s", pair.first);
						ActivityCompat.requestPermissions(activity, new String[]{pair.first}, requestCode);
					}*/
					return pair;
				});
	}

	private static void showMessageOKCancel(Activity activity, String message, DialogInterface.OnClickListener okListener) {
		new AlertDialog.Builder(activity)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.title_permissions_dialog)
				.setMessage(message)
				.setPositiveButton(activity.getString(R.string.ok), okListener)
				.setNegativeButton(activity.getString(R.string.cancel), null)
				// .create()
				.show();
	}
}
