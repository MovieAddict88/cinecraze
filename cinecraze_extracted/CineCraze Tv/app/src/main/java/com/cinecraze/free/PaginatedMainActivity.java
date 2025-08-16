package com.cinecraze.free;

import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import androidx.appcompat.app.AlertDialog;
import com.cinecraze.free.utils.UiUtils;

import com.cinecraze.free.models.Entry;
import com.cinecraze.free.net.ApiService;
import com.cinecraze.free.net.RetrofitClient;
import com.cinecraze.free.repository.DataRepository;
import com.cinecraze.free.R;
import com.gauravk.bubblenavigation.BubbleNavigationConstraintView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaginatedMainActivity extends AppCompatActivity implements PaginatedMovieAdapter.PaginationListener {

    private RecyclerView recyclerView;
    private PaginatedMovieAdapter movieAdapter;
    private List<Entry> currentPageEntries = new ArrayList<>();
    private ViewPager2 carouselViewPager;
    private CarouselAdapter carouselAdapter;
    private ImageView gridViewIcon;
    private ImageView listViewIcon;
    private BubbleNavigationConstraintView bottomNavigationView;
    private ImageView searchIcon;
    private ImageView closeSearchIcon;
    private LinearLayout titleLayout;
    private LinearLayout searchLayout;
    private AutoCompleteTextView searchBar;

    private boolean isGridView = true;
    private boolean isSearchVisible = false;
    private int retryCount = 0;
    private static final int MAX_RETRY_COUNT = 3;
    private DataRepository dataRepository;

    // Pagination variables
    private int currentPage = 0;
    private int pageSize = DataRepository.DEFAULT_PAGE_SIZE;
    private boolean hasMorePages = false;
    private int totalCount = 0;
    private String currentCategory = "";
    private String currentSearchQuery = "";
    private boolean isLoading = false;

    private AlertDialog downloadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up our custom toolbar (no default ActionBar since we use NoActionBar theme)
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            // Hide the default title since we have our custom title layout
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }
        }

        recyclerView = findViewById(R.id.recycler_view);
        carouselViewPager = findViewById(R.id.carousel_view_pager);
        gridViewIcon = findViewById(R.id.grid_view_icon);
        listViewIcon = findViewById(R.id.list_view_icon);
        bottomNavigationView = (BubbleNavigationConstraintView) findViewById(R.id.bottom_navigation);
        searchIcon = findViewById(R.id.search_icon);
        closeSearchIcon = findViewById(R.id.close_search_icon);
        titleLayout = findViewById(R.id.title_layout);
        searchLayout = findViewById(R.id.search_layout);
        searchBar = findViewById(R.id.search_bar);

        setupRecyclerView();
        setupCarousel();
        setupBottomNavigation();
        setupViewSwitch();
        setupSearchToggle();

        // Initialize repository
        dataRepository = new DataRepository(this);

        // Load initial data and first page
        loadInitialData();
    }

    private void setupRecyclerView() {
        movieAdapter = new PaginatedMovieAdapter(this, currentPageEntries, isGridView);
        movieAdapter.setPaginationListener(this);

        if (isGridView) {
            int spanCount = UiUtils.calculateSpanCountByItemWidthPx(this, getResources().getDimensionPixelSize(R.dimen.grid_item_min_width));
            recyclerView.setLayoutManager(new GridLayoutManager(this, spanCount));
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
        recyclerView.setAdapter(movieAdapter);
    }

    private void setupCarousel() {
        carouselAdapter = new CarouselAdapter(this, new ArrayList<>());
        carouselViewPager.setAdapter(carouselAdapter);
    }

    private void setupBottomNavigation() {
        // Set up navigation change listener
        bottomNavigationView.setNavigationChangeListener((view, position) -> {
            // Handle navigation item selection
            String category = "";
            if (position == 0) {
                category = "";
            } else if (position == 1) {
                category = "Movies";
            } else if (position == 2) {
                category = "TV Shows";
            } else if (position == 3) {
                category = "Live";
            }

            filterByCategory(category);
        });
    }

    private void setupViewSwitch() {
        gridViewIcon.setOnClickListener(v -> {
            if (!isGridView) {
                isGridView = true;
                updateViewMode();
            }
        });

        listViewIcon.setOnClickListener(v -> {
            if (isGridView) {
                isGridView = false;
                updateViewMode();
            }
        });
    }

    private void updateViewMode() {
        movieAdapter.setGridView(isGridView);

        if (isGridView) {
            int spanCount = UiUtils.calculateSpanCountByItemWidthPx(this, getResources().getDimensionPixelSize(R.dimen.grid_item_min_width));
            recyclerView.setLayoutManager(new GridLayoutManager(this, spanCount));
            gridViewIcon.setVisibility(View.GONE);
            listViewIcon.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            gridViewIcon.setVisibility(View.VISIBLE);
            listViewIcon.setVisibility(View.GONE);
        }
    }

    private void setupSearchToggle() {
        searchIcon.setOnClickListener(v -> showSearchBar());
        closeSearchIcon.setOnClickListener(v -> hideSearchBar());

        searchBar.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.length() > 2) {
                    performSearch(query);
                } else if (query.isEmpty()) {
                    clearSearch();
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void showSearchBar() {
        try {
            if (!isSearchVisible && titleLayout != null && searchLayout != null) {
                titleLayout.setVisibility(View.GONE);
                searchLayout.setVisibility(View.VISIBLE);
                isSearchVisible = true;
                searchBar.requestFocus();
            }
        } catch (Exception e) {
            Log.e("PaginatedMainActivity", "Error showing search bar: " + e.getMessage(), e);
        }
    }

    private void hideSearchBar() {
        try {
            if (isSearchVisible && titleLayout != null && searchLayout != null) {
                searchLayout.setVisibility(View.GONE);
                titleLayout.setVisibility(View.VISIBLE);
                isSearchVisible = false;

                if (searchBar != null) {
                    searchBar.setText("");
                    searchBar.clearFocus();
                    android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
                    }
                }

                clearSearch();
            }
        } catch (Exception e) {
            Log.e("PaginatedMainActivity", "Error hiding search bar: " + e.getMessage(), e);
        }
    }

    private void performSearch(String query) {
        currentSearchQuery = query.trim();
        currentPage = 0;
        loadSearchResults();
    }

    private void clearSearch() {
        currentSearchQuery = "";
        currentPage = 0;
        loadPage();
    }

    private void filterByCategory(String category) {
        currentCategory = category;
        currentPage = 0;
        currentSearchQuery = "";
        loadPage();
    }

    private void loadInitialData() {
        Log.d("PaginatedMainActivity", "Initializing paginated data");
        findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);

        if (dataRepository.hasValidCache()) {
            Log.d("PaginatedMainActivity", "Valid cache found - loading first page without download");
            loadFirstPage();
            setupCarouselFromCache();
            return;
        }

        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.headPlaylist().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                long contentLength = 0L;
                try {
                    String len = response.headers().get("Content-Length");
                    if (len != null) contentLength = Long.parseLong(len);
                } catch (Exception ignored) {}
                showDownloadPrompt(contentLength > 0 ? contentLength : -1L);
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.w("PaginatedMainActivity", "HEAD preflight failed: " + (t != null ? t.getMessage() : "unknown"));
                showDownloadPrompt(-1L);
            }
        });
    }

    private void setupCarouselFromCache() {
        // Setup carousel with only first few items - load efficiently
        dataRepository.getPaginatedData(0, 5, new DataRepository.PaginatedDataCallback() {
            @Override
            public void onSuccess(List<Entry> carouselEntries, boolean hasMorePages, int totalCount) {
                // Use only the first 5 entries for carousel - no need to load all data
                Log.d("PaginatedMainActivity", "Carousel loaded with " + carouselEntries.size() + " items");
                carouselAdapter = new CarouselAdapter(PaginatedMainActivity.this, carouselEntries);
                carouselViewPager.setAdapter(carouselAdapter);
                carouselAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                Log.e("PaginatedMainActivity", "Error loading carousel: " + error);
                // Initialize empty carousel
                carouselAdapter = new CarouselAdapter(PaginatedMainActivity.this, new ArrayList<>());
                carouselViewPager.setAdapter(carouselAdapter);
            }
        });
    }

    private void loadFirstPage() {
        currentPage = 0;
        loadPage();
    }

    private void loadPage() {
        if (isLoading) return;

        isLoading = true;
        movieAdapter.setLoading(true);

        if (!currentSearchQuery.isEmpty()) {
            loadSearchResults();
        } else if (!currentCategory.isEmpty()) {
            loadCategoryPage();
        } else {
            loadAllEntriesPage();
        }
    }

    private void loadAllEntriesPage() {
        dataRepository.getPaginatedData(currentPage, pageSize, new DataRepository.PaginatedDataCallback() {
            @Override
            public void onSuccess(List<Entry> entries, boolean hasMorePages, int totalCount) {
                findViewById(R.id.progress_bar).setVisibility(View.GONE);
                updatePageData(entries, hasMorePages, totalCount);
            }

            @Override
            public void onError(String error) {
                findViewById(R.id.progress_bar).setVisibility(View.GONE);
                handlePageLoadError(error);
            }
        });
    }

    private void loadCategoryPage() {
        dataRepository.getPaginatedDataByCategory(currentCategory, currentPage, pageSize, new DataRepository.PaginatedDataCallback() {
            @Override
            public void onSuccess(List<Entry> entries, boolean hasMorePages, int totalCount) {
                findViewById(R.id.progress_bar).setVisibility(View.GONE);
                updatePageData(entries, hasMorePages, totalCount);
            }

            @Override
            public void onError(String error) {
                findViewById(R.id.progress_bar).setVisibility(View.GONE);
                handlePageLoadError(error);
            }
        });
    }

    private void loadSearchResults() {
        dataRepository.searchPaginated(currentSearchQuery, currentPage, pageSize, new DataRepository.PaginatedDataCallback() {
            @Override
            public void onSuccess(List<Entry> entries, boolean hasMorePages, int totalCount) {
                findViewById(R.id.progress_bar).setVisibility(View.GONE);
                updatePageData(entries, hasMorePages, totalCount);
            }

            @Override
            public void onError(String error) {
                findViewById(R.id.progress_bar).setVisibility(View.GONE);
                handlePageLoadError(error);
            }
        });
    }

    private void updatePageData(List<Entry> entries, boolean hasMorePages, int totalCount) {
        this.hasMorePages = hasMorePages;
        this.totalCount = totalCount;
        this.isLoading = false;

        currentPageEntries.clear();
        currentPageEntries.addAll(entries);

        movieAdapter.setEntryList(currentPageEntries);
        movieAdapter.updatePaginationState(currentPage, hasMorePages, totalCount);

        // Scroll to top of the list
        recyclerView.scrollToPosition(0);

        Log.d("PaginatedMainActivity", "Loaded page " + currentPage + " with " + entries.size() + " items");
    }

    private void handlePageLoadError(String error) {
        isLoading = false;
        movieAdapter.setLoading(false);
        Log.e("PaginatedMainActivity", "Error loading page: " + error);
        Toast.makeText(this, "Failed to load page: " + error, Toast.LENGTH_SHORT).show();
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
                findViewById(R.id.progress_bar).setVisibility(View.GONE);
                Toast.makeText(this, "Download canceled. Pull to refresh to try again.", Toast.LENGTH_LONG).show();
            })
            .setCancelable(false)
            .show();
    }

    private void startInitialDownload(long estimatedBytes) {
        showDownloadingDialog(estimatedBytes);
        dataRepository.ensureDataAvailable(new DataRepository.DataCallback() {
            @Override
            public void onSuccess(List<Entry> entries) {
                if (downloadingDialog != null && downloadingDialog.isShowing()) {
                    downloadingDialog.dismiss();
                }
                // Cache ready, now load only first page
                loadFirstPage();
                setupCarouselFromCache();
            }

            @Override
            public void onError(String error) {
                if (downloadingDialog != null && downloadingDialog.isShowing()) {
                    downloadingDialog.dismiss();
                }
                findViewById(R.id.progress_bar).setVisibility(View.GONE);
                Log.e("PaginatedMainActivity", "Error initializing data: " + error);
                Toast.makeText(PaginatedMainActivity.this, "Failed to initialize data: " + error, Toast.LENGTH_LONG).show();
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

        android.widget.TextView message = new android.widget.TextView(this);
        message.setText("Downloading data (" + sizeText + ")...\nPlease wait, this may take a moment.");
        message.setTextSize(14);

        container.addView(progressBar);
        container.addView(message);

        downloadingDialog = new AlertDialog.Builder(this)
            .setTitle("Downloading")
            .setView(container)
            .setCancelable(false)
            .create();
        downloadingDialog.setCanceledOnTouchOutside(false);
        downloadingDialog.show();
    }

    // PaginationListener implementation
    @Override
    public void onPreviousPage() {
        if (currentPage > 0 && !isLoading) {
            currentPage--;
            loadPage();
        }
    }

    @Override
    public void onNextPage() {
        if (hasMorePages && !isLoading) {
            currentPage++;
            loadPage();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if we need to refresh data
        if (currentPageEntries.isEmpty()) {
            loadInitialData();
        }
    }

    public void refreshData() {
        retryCount = 0;
        currentPage = 0;
        if (dataRepository != null) {
            findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
            dataRepository.refreshData(new DataRepository.DataCallback() {
                @Override
                public void onSuccess(List<Entry> entries) {
                    // After refresh, load only first page - don't return all data
                    loadFirstPage();
                    setupCarouselFromCache();
                    Toast.makeText(PaginatedMainActivity.this, "Data refreshed (" + dataRepository.getTotalEntriesCount() + " items)", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String error) {
                    findViewById(R.id.progress_bar).setVisibility(View.GONE);
                    Toast.makeText(PaginatedMainActivity.this, "Failed to refresh: " + error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        try {
            if (isSearchVisible) {
                hideSearchBar();
            } else {
                super.onBackPressed();
            }
        } catch (Exception e) {
            Log.e("PaginatedMainActivity", "Error handling back press: " + e.getMessage(), e);
            super.onBackPressed();
        }
    }
}