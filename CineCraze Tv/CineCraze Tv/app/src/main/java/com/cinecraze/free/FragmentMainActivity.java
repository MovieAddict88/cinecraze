package com.cinecraze.free;

import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.viewpager2.widget.ViewPager2;
import androidx.appcompat.app.AlertDialog;

import com.cinecraze.free.R;
import com.cinecraze.free.ui.MainPagerAdapter;
import com.cinecraze.free.repository.DataRepository;
import com.cinecraze.free.ads.AdsManager;
import com.cinecraze.free.ads.AdsApiService;
import com.cinecraze.free.ads.AdsConfig;
import com.google.gson.Gson;
import com.gauravk.bubblenavigation.BubbleNavigationConstraintView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.ads.AdView;
import com.cinecraze.free.utils.PlaylistDbImporter;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

// Update system removed in new plan
import android.util.Log;
import java.io.File;
import com.cinecraze.free.database.PlaylistDatabaseManager;

public class FragmentMainActivity extends AppCompatActivity {
	private BubbleNavigationConstraintView bottomNavigationView;
	private ImageView closeSearchIcon;
	private LinearLayout searchLayout;
	private AutoCompleteTextView searchBar;
	private FloatingActionButton floatingSearchIcon;
	private ViewPager2 mainViewPager;
	private MainPagerAdapter pagerAdapter;
	private ViewPager2.OnPageChangeCallback pageChangeCallback;

	private boolean isSearchVisible = false;
	private boolean isProgrammaticChange = false;

	private DataRepository dataRepository;

	private AdsManager adsManager;
	private AdsApiService adsApiService;
	private LinearLayout bannerAdContainer;
	private AdView bannerAdView;

	private final Handler manifestHandler = new Handler(Looper.getMainLooper());
	private final Runnable manifestPoller = new Runnable() { @Override public void run() {} };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupStatusBar();
		setContentView(R.layout.activity_main_fragment);
		dataRepository = new DataRepository(this);
		initializeViews();
		Log.d("FragmentMainActivity", "Started after initial setup - launching main UI");
		startFragments();
	}

	private void startFragments() {
		setupViewPager();
		setupBottomNavigation();
		setupSearch();
		initializeAds();
		applyInitialTabFromIntent();
	}

	private void checkManifestAndMaybeForceUpdate() { /* removed per new plan */ }

	private void setupStatusBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = getWindow();
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.setStatusBarColor(android.graphics.Color.BLACK);
		}
	}

	private void initializeViews() {
		bottomNavigationView = findViewById(R.id.bottom_navigation);
		closeSearchIcon = findViewById(R.id.close_search_icon);
		searchLayout = findViewById(R.id.search_layout);
		searchBar = findViewById(R.id.search_bar);
		floatingSearchIcon = findViewById(R.id.floating_search_icon);
		mainViewPager = findViewById(R.id.main_viewpager);
		bannerAdContainer = findViewById(R.id.banner_ad_container);
	}

	private void setupViewPager() {
		pagerAdapter = new MainPagerAdapter(this);
		if (mainViewPager != null) {
			mainViewPager.setAdapter(pagerAdapter);
			mainViewPager.setOffscreenPageLimit(1);
		}
	}

	private void setupBottomNavigation() {
		if (bottomNavigationView == null || mainViewPager == null) return;
		// Minimal sync between bottom nav and pager (indexes 0..n)
		bottomNavigationView.setNavigationChangeListener((view, position) -> {
			if (mainViewPager.getAdapter() != null && position < mainViewPager.getAdapter().getItemCount()) {
				mainViewPager.setCurrentItem(position, true);
			}
		});
		pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
			@Override public void onPageSelected(int position) {
				// No-op; keep simple
			}
		};
		mainViewPager.registerOnPageChangeCallback(pageChangeCallback);
	}

	private void setupSearch() {
		if (floatingSearchIcon != null && searchLayout != null) {
			floatingSearchIcon.setOnClickListener(v -> {
				searchLayout.setVisibility(View.VISIBLE);
				isSearchVisible = true;
			});
		}
		if (closeSearchIcon != null && searchLayout != null) {
			closeSearchIcon.setOnClickListener(v -> {
				searchLayout.setVisibility(View.GONE);
				isSearchVisible = false;
			});
		}
	}

	private void initializeAds() {
		// No-op; ads setup removed/optional
	}

	private void applyInitialTabFromIntent() {
		if (mainViewPager == null || mainViewPager.getAdapter() == null) return;
		int initial = getIntent() != null ? getIntent().getIntExtra("initial_tab", 0) : 0;
		if (initial >= 0 && initial < mainViewPager.getAdapter().getItemCount()) {
			mainViewPager.setCurrentItem(initial, false);
		}
	}

	public void hideBottomNavigation() {
		if (bottomNavigationView != null) bottomNavigationView.setVisibility(View.GONE);
	}

	public void showBottomNavigation() {
		if (bottomNavigationView != null) bottomNavigationView.setVisibility(View.VISIBLE);
	}

    // ... existing code ...
}