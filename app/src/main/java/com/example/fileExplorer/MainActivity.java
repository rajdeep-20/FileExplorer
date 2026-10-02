package com.example.fileExplorer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.fileExplorer.Remote.DeltaSyncManager;
import com.example.fileExplorer.Remote.SyncScheduler;
import com.example.fileExplorer.fragments.BaseFileFragment;
import com.example.fileExplorer.fragments.HomeFragment;
import com.example.fileExplorer.fragments.NavigableFragment;
import com.example.fileExplorer.fragments.SortingOrder;
import com.example.fileExplorer.Remote.DeviceIdentityManager;
import com.google.android.material.navigation.NavigationView;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private static final String PREFS_NAME = "rfe_sync_prefs";
    private static final String KEY_LAST_SYNC_TIME = "last_sync_time_ms";
    private static final long SYNC_COOLDOWN_MS = 60 * 60 * 1000L; // 1 hour

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        DeviceIdentityManager.initialize(this);
        
        drawerLayout = findViewById(R.id.drawer_layout);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.openDrawer, R.string.closeDrawer);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        if (savedInstanceState == null) {
            navigateTo(new HomeFragment());
            navigationView.setCheckedItem(R.id.nav_home);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (current instanceof HomeFragment) {
                        moveTaskToBack(true);
                    } else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                        getSupportFragmentManager().popBackStack();
                    } else {
                        navigateTo(new HomeFragment());
                    }
                }
            }
        });
    }

    /** Replaces the fragment container with the given fragment (no back stack). */
    private void navigateTo(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return android.os.Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private boolean shouldSync() {
        long lastSync = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getLong(KEY_LAST_SYNC_TIME, 0);
        return (System.currentTimeMillis() - lastSync) > SYNC_COOLDOWN_MS;
    }

    private void markSynced() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis())
                .apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
            DeltaSyncManager.getInstance(this).start();
        
        if (hasStoragePermission() && shouldSync()) {
            SyncScheduler.triggerImmediateSync(this);
            markSynced();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        DeltaSyncManager.getInstance(this).stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        SortingOrder.SortingOrderEnum order = null;

        if (id == R.id.sort_name_asc) order = SortingOrder.SortingOrderEnum.NAME_ASC;
        else if (id == R.id.sort_name_desc) order = SortingOrder.SortingOrderEnum.NAME_DESC;
        else if (id == R.id.sort_time_asc) order = SortingOrder.SortingOrderEnum.TIME_ASC;
        else if (id == R.id.sort_time_desc) order =SortingOrder.SortingOrderEnum.TIME_DESC;
        else if (id == R.id.sort_size_asc) order = SortingOrder.SortingOrderEnum.SIZE_ASC;
        else if (id == R.id.sort_size_desc) order = SortingOrder.SortingOrderEnum.SIZE_DESC;

        if (order != null) {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment instanceof BaseFileFragment) {
                ((BaseFileFragment) currentFragment).sortFiles(order);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        if (id == R.id.nav_home) {
            navigateTo(new HomeFragment());
        } else if (id == R.id.nav_internal) {
            navigateTo(NavigableFragment.newInstance(NavigableFragment.TYPE_INTERNAL));
        } else if (id == R.id.nav_card) {
            navigateTo(NavigableFragment.newInstance(NavigableFragment.TYPE_SDCARD));
        } else {
            Toast.makeText(this, R.string.menuAbout, Toast.LENGTH_SHORT).show();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}
