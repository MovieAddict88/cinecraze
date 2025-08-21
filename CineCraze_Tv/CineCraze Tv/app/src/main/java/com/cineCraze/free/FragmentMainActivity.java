package com.cinecraze.free;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.cinecraze.free.ui.MainPagerAdapter;
import com.cinecraze.free.repository.DataRepository;
import com.cinecraze.free.utils.PlaylistDownloadManager;
import com.gauravk.bubblenavigation.BubbleNavigationConstraintView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// If playlist database is missing or corrupted, redirect to blocking download activity
		PlaylistDownloadManager pdm = new PlaylistDownloadManager(this);
		if (!pdm.isDatabaseExists() || pdm.isDatabaseCorrupted()) {
			android.content.Intent i = new android.content.Intent(this, com.cinecraze.free.PlaylistDownloadActivity.class);
			i.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
			finish();
			return;
		}
		setupStatusBar();
		setContentView(R.layout.activity_main_fragment);
		dataRepository = new DataRepository(this);
		initializeViews();
		startFragments();
	}

	private void startFragments() {
		setupViewPager();
		setupBottomNavigation();
		setupSearch();
		applyInitialTabFromIntent();
	}

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
		bottomNavigationView.setNavigationChangeListener((view, position) -> {
			if (mainViewPager.getAdapter() != null && position < mainViewPager.getAdapter().getItemCount()) {
				mainViewPager.setCurrentItem(position, true);
			}
		});
		pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
			@Override public void onPageSelected(int position) {
				try {
					if (bottomNavigationView != null) {
						bottomNavigationView.setCurrentActiveItem(position);
					}
				} catch (Exception ignored) {}
			}
		};
		mainViewPager.registerOnPageChangeCallback(pageChangeCallback);
	}

	private void setupSearch() {
		if (floatingSearchIcon != null && searchLayout != null) {
			floatingSearchIcon.setOnClickListener(v -> {
				searchLayout.setVisibility(View.VISIBLE);
				isSearchVisible = true;
				if (searchBar != null) searchBar.requestFocus();
			});
		}
		if (closeSearchIcon != null && searchLayout != null) {
			closeSearchIcon.setOnClickListener(v -> {
				searchLayout.setVisibility(View.GONE);
				isSearchVisible = false;
				if (searchBar != null) searchBar.setText("");
			});
		}
		if (searchBar != null) {
			searchBar.addTextChangedListener(new android.text.TextWatcher() {
				@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
				@Override public void onTextChanged(CharSequence s, int start, int before, int count) {
					String query = s != null ? s.toString().trim() : "";
					androidx.fragment.app.Fragment current = getSupportFragmentManager().findFragmentByTag("f" + mainViewPager.getCurrentItem());
					if (current instanceof com.cinecraze.free.ui.BaseFragment) {
						com.cinecraze.free.ui.BaseFragment bf = (com.cinecraze.free.ui.BaseFragment) current;
						if (query.length() > 2) {
							bf.performSearch(query);
						} else if (query.isEmpty()) {
							bf.performSearch("");
						}
					}
				}
				@Override public void afterTextChanged(android.text.Editable s) {}
			});
		}
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
}
