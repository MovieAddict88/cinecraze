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

import com.cinecraze.free.utils.EnhancedUpdateManagerFlexible;

/**
 * SWIPE-ENABLED FRAGMENT-BASED IMPLEMENTATION
 *
 * This activity uses ViewPager2 with fragments for swipe navigation:
 * - HomeFragment: Shows all content with carousel
 * - MovieFragment: Shows only movies
 * - SeriesFragment: Shows only TV series
 * - LiveTVFragment: Shows only live TV channels
 *
 * Features:
 * - Swipe between sections
 * - Bottom navigation clicks sync with swipe position
 * - Better memory management with FragmentStateAdapter
 * - Smooth transitions and animations
 */
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
    private boolean isProgrammaticChange = false; // Flag to prevent infinite loops

    private DataRepository dataRepository;

    // Ads related variables
    private AdsManager adsManager;
    private AdsApiService adsApiService;
    private LinearLayout bannerAdContainer;
    private AdView bannerAdView;

    // Playlist DB first-run manager and state
    private com.cinecraze.free.utils.EnhancedUpdateManagerFlexible playlistUpdateManager;
    private com.cinecraze.free.utils.EnhancedUpdateManagerFlexible.ManifestInfo initialManifestInfo;
    private android.widget.TextView downloadingMessageText;

    private static final String PREFS_APP_UPDATE = "app_open_update_prefs";
    private static final String KEY_LAST_HANDLED_MANIFEST_VERSION = "last_handled_manifest_version";
    private static final long MANIFEST_POLL_INTERVAL_MS = 5 * 60 * 1000; // 5 minutes
    private static final String MANIFEST_URL = "https://raw.githubusercontent.com/MovieAddict88/Movie-Source/main/manifest.json";

    private final Handler manifestHandler = new Handler(Looper.getMainLooper());
    private final Runnable manifestPoller = new Runnable() {
        @Override
        public void run() {
            if (!isFinishing() && !isDestroyed()) {
                checkManifestAndMaybeForceUpdate();
                manifestHandler.postDelayed(this, MANIFEST_POLL_INTERVAL_MS);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up status bar and navigation bar
        setupStatusBar();

        setContentView(R.layout.activity_main_fragment);

        dataRepository = new DataRepository(this);
        playlistUpdateManager = new com.cinecraze.free.utils.EnhancedUpdateManagerFlexible(this);

        initializeViews();

        // Preflight: if playlist.db exists, start app; otherwise prompt to download DB first
        if (playlistUpdateManager.isDatabaseExists()) {
            startFragments();
            // Start manifest watcher since user is in app already
            manifestHandler.postDelayed(manifestPoller, MANIFEST_POLL_INTERVAL_MS);
        } else {
            preflightAndPrompt();
        }
    }

    private void startFragments() {
        setupViewPager();
        setupBottomNavigation();
        setupSearch();
        initializeAds();
        applyInitialTabFromIntent();
        
        // Show interstitial ad on app launch (if ready and enabled)
        // Delay to ensure ads are loaded first
        new android.os.Handler().postDelayed(() -> {
            showInterstitialAdIfReady();
        }, 2000); // 2 second delay
        // Immediately check for manifest update on app open
        checkManifestAndMaybeForceUpdate();
        // Ensure manifest watcher is running when main UI is active
        manifestHandler.removeCallbacks(manifestPoller);
        manifestHandler.postDelayed(manifestPoller, MANIFEST_POLL_INTERVAL_MS);
    }

    private void preflightAndPrompt() {
        // Fetch manifest to get size/hash and then prompt user
        playlistUpdateManager.checkForUpdates(new com.cinecraze.free.utils.EnhancedUpdateManagerFlexible.UpdateCallback() {
            @Override
            public void onUpdateCheckStarted() { }

            @Override
            public void onUpdateAvailable(com.cinecraze.free.utils.EnhancedUpdateManagerFlexible.ManifestInfo manifestInfo) {
                initialManifestInfo = manifestInfo;
                long bytes = -1L;
                if (manifestInfo != null && manifestInfo.database != null) {
                    if (manifestInfo.database.sizeBytes > 0) {
                        bytes = manifestInfo.database.sizeBytes;
                    } else if (manifestInfo.database.sizeMb > 0) {
                        bytes = (long) (manifestInfo.database.sizeMb * 1024 * 1024);
                    }
                }
                final long bytesForUi = bytes;
                runOnUiThread(() -> showDownloadPrompt(bytesForUi));
            }

            @Override
            public void onNoUpdateAvailable() {
                // Should not happen on first run; proceed just in case
                runOnUiThread(() -> startFragments());
            }

            @Override
            public void onUpdateCheckFailed(String error) {
                runOnUiThread(() -> {
                    android.widget.Toast.makeText(FragmentMainActivity.this, "Failed to check update: " + error, android.widget.Toast.LENGTH_LONG).show();
                    finish();
                });
            }

            @Override public void onUpdateDownloadStarted() { }
            @Override public void onUpdateDownloadProgress(int progress) { }
            @Override public void onUpdateDownloadCompleted() { }
            @Override public void onUpdateDownloadFailed(String error) { }
        });
    }

    private void showDownloadPrompt(long contentLengthBytes) {
        String sizeText;
        if (contentLengthBytes > 0) {
            double mb = contentLengthBytes / (1024.0 * 1024.0);
            sizeText = String.format(Locale.getDefault(), "%.1f MB", mb);
        } else {
            sizeText = "unknown size";
        }

        new AlertDialog.Builder(this)
            .setTitle("Download required")
            .setMessage("Initial data needs to be downloaded (" + sizeText + "). Continue?")
            .setPositiveButton("Download", (dialog, which) -> startInitialDownload(contentLengthBytes))
            .setNegativeButton("Cancel", (dialog, which) -> {
                // User canceled; you can finish or keep minimal UI
                finish();
            })
            .setCancelable(false)
            .show();
    }

    private AlertDialog downloadingDialog;

    private void startInitialDownload(long estimatedBytes) {
        showDownloadingDialog(estimatedBytes);
        if (initialManifestInfo == null) {
            // Safety fallback
            if (downloadingDialog != null && downloadingDialog.isShowing()) {
                downloadingDialog.dismiss();
            }
            android.widget.Toast.makeText(this, "Missing manifest info", android.widget.Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        playlistUpdateManager.downloadUpdate(initialManifestInfo, new com.cinecraze.free.utils.EnhancedUpdateManagerFlexible.UpdateCallback() {
            @Override public void onUpdateCheckStarted() { }
            @Override public void onUpdateAvailable(com.cinecraze.free.utils.EnhancedUpdateManagerFlexible.ManifestInfo manifestInfo) { }
            @Override public void onNoUpdateAvailable() { }
            @Override public void onUpdateCheckFailed(String error) { }

            @Override
            public void onUpdateDownloadStarted() { }

            @Override
            public void onUpdateDownloadProgress(int progress) {
                runOnUiThread(() -> {
                    if (downloadingMessageText != null) {
                        String sizeText;
                        if (estimatedBytes > 0) {
                            double mb = estimatedBytes / (1024.0 * 1024.0);
                            sizeText = String.format(java.util.Locale.getDefault(), "%.1f MB", mb);
                        } else {
                            sizeText = "unknown size";
                        }
                        downloadingMessageText.setText("Downloading data (" + sizeText + ")...\n" + progress + "%");
                    }
                });
            }

            @Override
            public void onUpdateDownloadCompleted() {
                runOnUiThread(() -> {
                    if (downloadingDialog != null && downloadingDialog.isShowing()) {
                        downloadingDialog.dismiss();
                    }
                    try {
                        // Import downloaded playlist.db into Room cache before proceeding
                        PlaylistDbImporter.importIntoRoom(FragmentMainActivity.this);
                    } catch (Exception e) {
                        android.util.Log.e("FragmentMainActivity", "Import failed: " + e.getMessage(), e);
                        android.widget.Toast.makeText(FragmentMainActivity.this, "Import failed: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                    }
                    // Mark this manifest version as handled so app-open update won't immediately trigger
                    try {
                        if (initialManifestInfo != null && initialManifestInfo.version != null && !initialManifestInfo.version.isEmpty()) {
                            SharedPreferences sp = getSharedPreferences(PREFS_APP_UPDATE, MODE_PRIVATE);
                            sp.edit().putString(KEY_LAST_HANDLED_MANIFEST_VERSION, initialManifestInfo.version).apply();
                        }
                    } catch (Exception ignored) {}
                    startFragments();
                });
            }

            @Override
            public void onUpdateDownloadFailed(String error) {
                runOnUiThread(() -> {
                    if (downloadingDialog != null && downloadingDialog.isShowing()) {
                        downloadingDialog.dismiss();
                    }
                    android.widget.Toast.makeText(FragmentMainActivity.this, "Download failed: " + error, android.widget.Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    private void showDownloadingDialog(long estimatedBytes) {
        String sizeText;
        if (estimatedBytes > 0) {
            double mb = estimatedBytes / (1024.0 * 1024.0);
            sizeText = String.format(Locale.getDefault(), "%.1f MB", mb);
        } else {
            sizeText = "unknown size";
        }

        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, padding);

        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(this);
        progressBar.setIndeterminate(true);
        android.widget.LinearLayout.LayoutParams pbParams = new android.widget.LinearLayout.LayoutParams(
            (int) (24 * getResources().getDisplayMetrics().density),
            (int) (24 * getResources().getDisplayMetrics().density)
        );
        pbParams.setMargins(0, 0, padding, 0);
        progressBar.setLayoutParams(pbParams);

        downloadingMessageText = new android.widget.TextView(this);
        downloadingMessageText.setText("Downloading data (" + sizeText + ")...\nPlease wait, this may take a moment.");
        downloadingMessageText.setTextSize(14);

        container.addView(progressBar);
        container.addView(downloadingMessageText);

        downloadingDialog = new AlertDialog.Builder(this)
            .setTitle("Downloading")
            .setView(container)
            .setCancelable(false)
            .create();
        downloadingDialog.setCanceledOnTouchOutside(false);
        downloadingDialog.show();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(android.graphics.Color.BLACK);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decor = window.getDecorView();
                decor.setSystemUiVisibility(0); // Clear light status bar flag
            }
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
        mainViewPager.setAdapter(pagerAdapter);

        // Enable smooth scrolling and configure ViewPager2
        mainViewPager.setOffscreenPageLimit(1); // Keep adjacent fragments in memory

        // Add page change callback to sync with bottom navigation
        pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                // Only update bottom navigation if this is not a programmatic change
                if (!isProgrammaticChange) {
                    bottomNavigationView.setCurrentActiveItem(position);
                }

                // Update floating search icon visibility
                updateSearchIconVisibility(position);
            }
        };
        mainViewPager.registerOnPageChangeCallback(pageChangeCallback);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setNavigationChangeListener((view, position) -> {
            // Set flag to prevent infinite loop
            isProgrammaticChange = true;

            // Show interstitial ad if available
            showInterstitialAdIfReady();

            // Smoothly scroll to the selected page
            mainViewPager.setCurrentItem(position, true);

            // Update floating search icon visibility
            updateSearchIconVisibility(position);

            // Reset flag after a short delay to allow ViewPager to settle
            mainViewPager.post(() -> isProgrammaticChange = false);
        });

        // Ensure we start on Home
        bottomNavigationView.setCurrentActiveItem(0);
    }

    private void updateSearchIconVisibility(int position) {
        // Show search icon only on Home tab (position 0)
        if (position == 0) {
            floatingSearchIcon.setVisibility(View.VISIBLE);
        } else {
            floatingSearchIcon.setVisibility(View.GONE);
        }
    }

    private void setupSearch() {
        floatingSearchIcon.setOnClickListener(v -> toggleSearch());

        closeSearchIcon.setOnClickListener(v -> hideSearch());

        searchBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                performSearch(searchBar.getText().toString().trim());
                return true;
            }
            return false;
        });
    }

    private void toggleSearch() {
        if (isSearchVisible) {
            hideSearch();
        } else {
            showSearch();
        }
    }

    private void showSearch() {
        searchLayout.setVisibility(View.VISIBLE);
        searchLayout.animate()
            .translationY(0)
            .alpha(1.0f)
            .setDuration(300)
            .start();

        searchBar.requestFocus();
        isSearchVisible = true;
    }

    private void hideSearch() {
        searchLayout.animate()
            .translationY(-searchLayout.getHeight())
            .alpha(0.0f)
            .setDuration(300)
            .withEndAction(() -> {
                searchLayout.setVisibility(View.GONE);
                searchBar.setText("");
            })
            .start();

        isSearchVisible = false;
    }

    private void performSearch(String query) {
        // Get the current fragment and perform search
        int currentItem = mainViewPager.getCurrentItem();
        androidx.fragment.app.Fragment currentFragment = pagerAdapter.getFragmentAt(currentItem);

        if (currentFragment instanceof com.cinecraze.free.ui.BaseFragment) {
            ((com.cinecraze.free.ui.BaseFragment) currentFragment).performSearch(query);
        }
        hideSearch();
        
        // Show interstitial ad after search (if ready and enabled)
        showInterstitialAdIfReady();
    }

    private void applyInitialTabFromIntent() {
        try {
            int tab = getIntent().getIntExtra("initial_tab", 0);
            if (tab >= 0 && tab < pagerAdapter.getItemCount()) {
                isProgrammaticChange = true;
                mainViewPager.setCurrentItem(tab, false);
                bottomNavigationView.setCurrentActiveItem(tab);
                updateSearchIconVisibility(tab);
                mainViewPager.post(() -> isProgrammaticChange = false);
            }
        } catch (Exception ignored) {}
    }

    private void initializeAds() {
        // Initialize AdsManager and AdsApiService
        adsManager = new AdsManager(this);
        adsApiService = new AdsApiService();

        // Set up banner ad container
        bannerAdContainer = findViewById(R.id.banner_ad_container);
        
        // Fetch ads configuration and set up banner ad
        adsApiService.fetchAdsConfig(new AdsApiService.AdsConfigCallback() {
            @Override
            public void onSuccess(AdsConfig config) {
                runOnUiThread(() -> {
                    if (config != null && config.getAdmob() != null) {
                        // Set up banner ad if enabled
                        if (config.getAdmob().getBannerId() != null && !config.getAdmob().getBannerId().isEmpty()) {
                            setupBannerAd(config.getAdmob().getBannerId());
                        }
                        
                        // Load interstitial ad if enabled
                        if (config.getAdmob().getInterstitialId() != null && !config.getAdmob().getInterstitialId().isEmpty()) {
                            loadInterstitialAd(config.getAdmob().getInterstitialId());
                        }
                        
                        // Load rewarded ad if enabled
                        if (config.getAdmob().getRewardedId() != null && !config.getAdmob().getRewardedId().isEmpty()) {
                            loadRewardedAd(config.getAdmob().getRewardedId());
                        }
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                // Log error but don't crash the app
                android.util.Log.e("FragmentMainActivity", "Failed to fetch ads config: " + error);
            }
        });
    }

    private void setupBannerAd(String adUnitId) {
        if (bannerAdContainer != null && adUnitId != null && !adUnitId.isEmpty()) {
            bannerAdView = new AdView(this);
            bannerAdView.setAdSize(com.google.android.gms.ads.AdSize.BANNER);
            bannerAdView.setAdUnitId(adUnitId);
            
            // Load the banner ad
            com.google.android.gms.ads.AdRequest adRequest = new com.google.android.gms.ads.AdRequest.Builder().build();
            bannerAdView.loadAd(adRequest);
            
            // Add the banner ad to the container
            bannerAdContainer.addView(bannerAdView);
            
            // Show the banner ad container
            bannerAdContainer.setVisibility(View.VISIBLE);
        }
    }

    private void loadInterstitialAd(String adUnitId) {
        if (adsManager != null && adUnitId != null && !adUnitId.isEmpty()) {
            adsManager.loadInterstitialAd(adUnitId);
        }
    }

    private void loadRewardedAd(String adUnitId) {
        if (adsManager != null && adUnitId != null && !adUnitId.isEmpty()) {
            adsManager.loadRewardedAd(adUnitId);
        }
    }

    private void showInterstitialAdIfReady() {
        if (adsManager != null && adsManager.isInterstitialAdReady()) {
            adsManager.showInterstitialAd(this);
        }
    }

    private void showRewardedAdIfReady() {
        if (adsManager != null && adsManager.isRewardedAdReady()) {
            adsManager.showRewardedAd(this, new com.google.android.gms.ads.OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull com.google.android.gms.ads.rewarded.RewardItem rewardItem) {
                    // Handle reward if needed
                    android.util.Log.d("FragmentMainActivity", "User earned reward: " + rewardItem.getAmount() + " " + rewardItem.getType());
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (isSearchVisible) {
            hideSearch();
        } else {
            // Show interstitial ad before going back (if ready and enabled)
            showInterstitialAdIfReady();
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister ViewPager callback to prevent memory leaks
        if (mainViewPager != null && pageChangeCallback != null) {
            mainViewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        }

        // Clean up ads
        if (bannerAdView != null) {
            bannerAdView.destroy();
        }
        if (adsManager != null) {
            adsManager.destroyAds();
        }
        manifestHandler.removeCallbacks(manifestPoller);
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Resume banner ad if available
        if (bannerAdView != null) {
            bannerAdView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        // Pause banner ad if available
        if (bannerAdView != null) {
            bannerAdView.pause();
        }
    }

    public void hideBottomNavigation() {
        if (bottomNavigationView.getVisibility() == View.VISIBLE) {
            bottomNavigationView.animate()
                .translationY(bottomNavigationView.getHeight())
                .setDuration(300)
                .withEndAction(() -> bottomNavigationView.setVisibility(View.GONE))
                .start();
            setViewPagerMargin(0);
        }
    }

    public void showBottomNavigation() {
        if (bottomNavigationView.getVisibility() == View.GONE) {
            bottomNavigationView.setVisibility(View.VISIBLE);
            bottomNavigationView.animate()
                .translationY(0)
                .setDuration(300)
                .start();
            setViewPagerMargin(60);
        }
    }

    private void setViewPagerMargin(int bottomMarginInDp) {
        if (mainViewPager != null) {
            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) mainViewPager.getLayoutParams();
            float density = getResources().getDisplayMetrics().density;
            int bottomMarginInPixels = (int) (bottomMarginInDp * density);
            params.setMargins(0, 0, 0, bottomMarginInPixels);
            mainViewPager.setLayoutParams(params);
        }
    }

    private void checkManifestAndMaybeForceUpdate() {
        try {
            // Only check when DB exists
            EnhancedUpdateManagerFlexible updateManager = new EnhancedUpdateManagerFlexible(this);
            if (!updateManager.isDatabaseExists()) {
                return;
            }
            new Thread(() -> {
                HttpURLConnection connection = null;
                InputStream inputStream = null;
                try {
                    URL url = new URL(MANIFEST_URL);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(15000);
                    connection.setRequestProperty("User-Agent", "CineCraze-Android-App");
                    int responseCode = connection.getResponseCode();
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        return;
                    }
                    inputStream = connection.getInputStream();
                    byte[] buffer = new byte[8192];
                    StringBuilder result = new StringBuilder();
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        result.append(new String(buffer, 0, bytesRead));
                    }
                    Gson gson = new Gson();
                    com.cinecraze.free.utils.EnhancedUpdateManagerFlexible.ManifestInfo manifest =
                        gson.fromJson(result.toString(), com.cinecraze.free.utils.EnhancedUpdateManagerFlexible.ManifestInfo.class);
                    if (manifest == null || manifest.version == null || manifest.version.isEmpty()) {
                        return;
                    }
                    SharedPreferences sp = getSharedPreferences(PREFS_APP_UPDATE, MODE_PRIVATE);
                    String lastHandled = sp.getString(KEY_LAST_HANDLED_MANIFEST_VERSION, "1");
                    if (!manifest.version.equals(lastHandled)) {
                        // Mark new version as handled to enforce one-time prompt per version
                        sp.edit().putString(KEY_LAST_HANDLED_MANIFEST_VERSION, manifest.version).apply();
                        runOnUiThread(() -> {
                            Intent intent = new Intent(FragmentMainActivity.this, com.cinecraze.free.ui.PlaylistDownloadActivityFlexible.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        });
                    }
                } catch (Exception ignored) {
                } finally {
                    try {
                        if (inputStream != null) inputStream.close();
                        if (connection != null) connection.disconnect();
                    } catch (IOException ignored2) {}
                }
            }).start();
        } catch (Exception ignored) { }
    }
}