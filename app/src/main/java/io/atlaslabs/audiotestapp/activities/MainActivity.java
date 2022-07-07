package io.atlaslabs.audiotestapp.activities;

import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import io.atlaslabs.audiotestapp.R;
import io.atlaslabs.audiotestapp.databinding.ActivityMainBinding;
import io.atlaslabs.audiotestapp.util.PermissionUtil;
import io.atlaslabs.audiotestapp.util.Utils;

import android.view.Menu;
import android.view.MenuItem;

import org.jetbrains.annotations.NotNull;

public class MainActivity extends AppCompatActivity {

	private AppBarConfiguration appBarConfiguration;
	private ActivityMainBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		binding = ActivityMainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		setSupportActionBar(binding.toolbar);

		NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
		appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
		NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

		binding.fab.setOnClickListener(view -> Snackbar.make(view, "Empty action", Snackbar.LENGTH_LONG)
				.setAction("Action", listener -> {
				}).show());

		if (!PermissionUtil.getNeededRuntimePermissions(this)) {
			Utils.showToast(this, "Missing one or more required runtime permissions");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
		if (permissions.length == 0)
			return;

		if (requestCode == PermissionUtil.PERMISSIONS_REQUEST_SINGLE_CODE)
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onSupportNavigateUp() {
		NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
		return NavigationUI.navigateUp(navController, appBarConfiguration)
				|| super.onSupportNavigateUp();
	}
}