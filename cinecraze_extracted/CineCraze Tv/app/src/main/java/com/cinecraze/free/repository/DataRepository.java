package com.cinecraze.free.repository;

import android.content.Context;
import android.util.Log;

import com.cinecraze.free.database.CineCrazeDatabase;
import com.cinecraze.free.database.DatabaseUtils;
import com.cinecraze.free.database.entities.CacheMetadataEntity;
import com.cinecraze.free.database.entities.EntryEntity;
import com.cinecraze.free.models.Category;
import com.cinecraze.free.models.Entry;
import com.cinecraze.free.models.Playlist;
import com.cinecraze.free.net.ApiService;
import com.cinecraze.free.net.RetrofitClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DataRepository {

    private static final String TAG = "DataRepository";
    private static final String CACHE_KEY_PLAYLIST = "playlist_data";
    private static final long CACHE_EXPIRY_HOURS = 24; // Cache expires after 24 hours
    public static final int DEFAULT_PAGE_SIZE = 20; // Default items per page

    private CineCrazeDatabase database;
    private ApiService apiService;

    public interface DataCallback {
        void onSuccess(List<Entry> entries);
        void onError(String error);
    }

    public interface PaginatedDataCallback {
        void onSuccess(List<Entry> entries, boolean hasMorePages, int totalCount);
        void onError(String error);
    }

    public DataRepository(Context context) {
        database = CineCrazeDatabase.getInstance(context);
        apiService = RetrofitClient.getClient().create(ApiService.class);
    }

    /**
     * Expose cache validity so UI can decide whether to prompt before downloading
     */
    public boolean hasValidCache() {
        try {
            CacheMetadataEntity metadata = database.cacheMetadataDao().getMetadata(CACHE_KEY_PLAYLIST);
            return metadata != null && isCacheValid(metadata.getLastUpdated());
        } catch (Exception e) {
            Log.e(TAG, "Error checking cache validity: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get playlist data - checks cache first, then fetches from API if needed
     */
    public void getPlaylistData(DataCallback callback) {
        // Check if we have cached data and if it's still valid
        CacheMetadataEntity metadata = database.cacheMetadataDao().getMetadata(CACHE_KEY_PLAYLIST);

        if (metadata != null && isCacheValid(metadata.getLastUpdated())) {
            Log.d(TAG, "Using cached data");
            loadFromCache(callback);
        } else {
            Log.d(TAG, "Cache expired or empty, fetching from API");
            fetchFromApi(callback);
        }
    }

    /**
     * Force refresh data from API (ignores cache)
     * This method is used for pull-to-refresh functionality
     */
    public void forceRefreshData(DataCallback callback) {
        Log.d(TAG, "Force refreshing data from API");
        fetchFromApi(callback);
    }

    /**
     * Check if data is available in cache and initialize if needed
     * This method only loads data if cache is empty, otherwise just confirms cache exists
     */
    public void ensureDataAvailable(DataCallback callback) {
        CacheMetadataEntity metadata = database.cacheMetadataDao().getMetadata(CACHE_KEY_PLAYLIST);

        if (metadata != null && isCacheValid(metadata.getLastUpdated())) {
            // Cache exists and is valid - just return success without loading all data
            Log.d(TAG, "Cache is available and valid - ready for pagination");
            callback.onSuccess(new ArrayList<>()); // Empty list, pagination will load actual data
        } else {
            // No valid cache - need to fetch all data once to populate cache
            Log.d(TAG, "No valid cache - fetching data to populate cache");
            fetchFromApi(new DataCallback() {
                @Override
                public void onSuccess(List<Entry> entries) {
                    Log.d(TAG, "Data cached successfully - ready for pagination");
                    callback.onSuccess(new ArrayList<>()); // Empty list, pagination will load actual data
                }

                @Override
                public void onError(String error) {
                    callback.onError(error);
                }
            });
        }
    }

    /**
     * Get paginated data from cache
     */
    public void getPaginatedData(int page, int pageSize, PaginatedDataCallback callback) {
        try {
            int offset = page * pageSize;
            List<EntryEntity> entities = database.entryDao().getEntriesPaged(pageSize, offset);
            List<Entry> entries = DatabaseUtils.entitiesToEntries(entities);
            int totalCount = database.entryDao().getEntriesCount();
            boolean hasMorePages = (offset + pageSize) < totalCount;

            Log.d(TAG, "Loaded page " + page + " with " + entries.size() + " items. Total: " + totalCount + ", HasMore: " + hasMorePages);
            callback.onSuccess(entries, hasMorePages, totalCount);
        } catch (Exception e) {
            Log.e(TAG, "Error loading paginated data: " + e.getMessage(), e);
            callback.onError("Error loading page: " + e.getMessage());
        }
    }

    /**
     * Get paginated data by category
     */
    public void getPaginatedDataByCategory(String category, int page, int pageSize, PaginatedDataCallback callback) {
        try {
            int offset = page * pageSize;
            List<EntryEntity> entities = database.entryDao().getEntriesByCategoryPaged(category, pageSize, offset);
            List<Entry> entries = DatabaseUtils.entitiesToEntries(entities);
            int totalCount = database.entryDao().getEntriesCountByCategory(category);
            boolean hasMorePages = (offset + pageSize) < totalCount;

            Log.d(TAG, "Loaded category '" + category + "' page " + page + " with " + entries.size() + " items. Total: " + totalCount);
            callback.onSuccess(entries, hasMorePages, totalCount);
        } catch (Exception e) {
            Log.e(TAG, "Error loading paginated category data: " + e.getMessage(), e);
            callback.onError("Error loading category page: " + e.getMessage());
        }
    }

    /**
     * Search with pagination
     */
    public void searchPaginated(String searchQuery, int page, int pageSize, PaginatedDataCallback callback) {
        try {
            int offset = page * pageSize;
            List<EntryEntity> entities = database.entryDao().searchByTitlePaged(searchQuery, pageSize, offset);
            List<Entry> entries = DatabaseUtils.entitiesToEntries(entities);
            int totalCount = database.entryDao().getSearchResultsCount(searchQuery);
            boolean hasMorePages = (offset + pageSize) < totalCount;

            Log.d(TAG, "Search '" + searchQuery + "' page " + page + " with " + entries.size() + " results. Total: " + totalCount);
            callback.onSuccess(entries, hasMorePages, totalCount);
        } catch (Exception e) {
            Log.e(TAG, "Error searching with pagination: " + e.getMessage(), e);
            callback.onError("Error searching: " + e.getMessage());
        }
    }

    /**
     * Force refresh data from API
     */
    public void refreshData(DataCallback callback) {
        Log.d(TAG, "Force refreshing data from API");
        fetchFromApi(callback);
    }

    /**
     * Get entries by category from cache
     */
    public List<Entry> getEntriesByCategory(String category) {
        List<EntryEntity> entities = database.entryDao().getEntriesByCategory(category);
        return DatabaseUtils.entitiesToEntries(entities);
    }

    /**
     * Search entries by title from cache
     */
    public List<Entry> searchByTitle(String title) {
        List<EntryEntity> entities = database.entryDao().searchByTitle(title);
        return DatabaseUtils.entitiesToEntries(entities);
    }

    /**
     * Get all cached entries
     */
    public List<Entry> getAllCachedEntries() {
        List<EntryEntity> entities = database.entryDao().getAllEntries();
        return DatabaseUtils.entitiesToEntries(entities);
    }

    /**
     * Get total count of cached entries
     */
    public int getTotalEntriesCount() {
        return database.entryDao().getEntriesCount();
    }

    /**
     * Get unique genres from cached data
     */
    public List<String> getUniqueGenres() {
        try {
            List<String> genres = database.entryDao().getUniqueGenres();
            // Filter out null and empty values
            List<String> filteredGenres = new ArrayList<>();
            for (String genre : genres) {
                if (genre != null && !genre.trim().isEmpty() && !genre.equalsIgnoreCase("null")) {
                    filteredGenres.add(genre.trim());
                }
            }
            return filteredGenres;
        } catch (Exception e) {
            Log.e(TAG, "Error getting unique genres: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get unique countries from cached data
     */
    public List<String> getUniqueCountries() {
        try {
            List<String> countries = database.entryDao().getUniqueCountries();
            // Filter out null and empty values
            List<String> filteredCountries = new ArrayList<>();
            for (String country : countries) {
                if (country != null && !country.trim().isEmpty() && !country.equalsIgnoreCase("null")) {
                    filteredCountries.add(country.trim());
                }
            }
            return filteredCountries;
        } catch (Exception e) {
            Log.e(TAG, "Error getting unique countries: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get unique years from cached data
     */
    public List<String> getUniqueYears() {
        try {
            List<String> years = database.entryDao().getUniqueYears();
            // Filter out null, empty, and zero values
            List<String> filteredYears = new ArrayList<>();
            for (String year : years) {
                if (year != null && !year.trim().isEmpty() && !year.equalsIgnoreCase("null") && !year.equals("0")) {
                    filteredYears.add(year.trim());
                }
            }
            return filteredYears;
        } catch (Exception e) {
            Log.e(TAG, "Error getting unique years: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get paginated data filtered by genre, country, and year
     */
    public void getPaginatedFilteredData(String genre, String country, String year, int page, int pageSize, PaginatedDataCallback callback) {
        try {
            int offset = page * pageSize;
            List<EntryEntity> entities = database.entryDao().getEntriesFilteredPaged(
                genre == null || genre.isEmpty() ? null : genre,
                country == null || country.isEmpty() ? null : country,
                year == null || year.isEmpty() ? null : year,
                pageSize, offset
            );
            List<Entry> entries = DatabaseUtils.entitiesToEntries(entities);
            int totalCount = database.entryDao().getEntriesFilteredCount(
                genre == null || genre.isEmpty() ? null : genre,
                country == null || country.isEmpty() ? null : country,
                year == null || year.isEmpty() ? null : year
            );
            boolean hasMorePages = (offset + pageSize) < totalCount;

            Log.d(TAG, "Loaded filtered page " + page + " with " + entries.size() + " items. Total: " + totalCount);
            callback.onSuccess(entries, hasMorePages, totalCount);
        } catch (Exception e) {
            Log.e(TAG, "Error loading filtered paginated data: " + e.getMessage(), e);
            callback.onError("Error loading filtered page: " + e.getMessage());
        }
    }

    public List<Entry> getTopRatedEntries(int count) {
        try {
            List<EntryEntity> entities = database.entryDao().getTopRatedEntries(count);
            return DatabaseUtils.entitiesToEntries(entities);
        } catch (Exception e) {
            Log.e(TAG, "Error getting top rated entries: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<Entry> getRecentlyAdded(int count) {
        try {
            List<EntryEntity> entities = database.entryDao().getRecentlyAdded(count);
            return DatabaseUtils.entitiesToEntries(entities);
        } catch (Exception e) {
            Log.e(TAG, "Error getting recently added: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public Entry findEntryByHashId(int hashId) {
        try {
            List<Entry> all = getAllCachedEntries();
            for (Entry e : all) {
                if (e != null && e.getId() == hashId) {
                    return e;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error finding entry by hash id: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Check if cache is still valid
     */
    private boolean isCacheValid(long lastUpdated) {
        long currentTime = System.currentTimeMillis();
        long cacheAge = currentTime - lastUpdated;
        long expiryTime = TimeUnit.HOURS.toMillis(CACHE_EXPIRY_HOURS);
        return cacheAge < expiryTime;
    }

    /**
     * Load data from local cache
     */
    private void loadFromCache(DataCallback callback) {
        try {
            List<EntryEntity> entities = database.entryDao().getAllEntries();
            List<Entry> entries = DatabaseUtils.entitiesToEntries(entities);

            if (!entries.isEmpty()) {
                callback.onSuccess(entries);
            } else {
                // Cache is empty, fetch from API
                Log.d(TAG, "Cache is empty, fetching from API");
                fetchFromApi(callback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading from cache: " + e.getMessage(), e);
            fetchFromApi(callback);
        }
    }

    /**
     * Fetch data from API and cache it
     */
    private void fetchFromApi(DataCallback callback) {
        Call<Playlist> call = apiService.getPlaylist();
        call.enqueue(new Callback<Playlist>() {
            @Override
            public void onResponse(Call<Playlist> call, Response<Playlist> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Playlist playlist = response.body();
                        List<Entry> allEntries = new ArrayList<>();

                        // Extract all entries from categories
                        if (playlist.getCategories() != null) {
                            for (Category category : playlist.getCategories()) {
                                if (category != null && category.getEntries() != null) {
                                    allEntries.addAll(category.getEntries());
                                }
                            }
                        }

                        // Cache the data
                        cacheData(playlist);

                        Log.d(TAG, "Data fetched and cached successfully: " + allEntries.size() + " entries");
                        callback.onSuccess(allEntries);

                    } catch (Exception e) {
                        Log.e(TAG, "Error processing API response: " + e.getMessage(), e);
                        callback.onError("Error processing data: " + e.getMessage());
                    }
                } else {
                    Log.e(TAG, "API response failed: " + response.code());
                    callback.onError("Failed to load data: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Playlist> call, Throwable t) {
                Log.e(TAG, "API call failed: " + t.getMessage(), t);

                // Try to load from cache as fallback
                List<Entry> cachedEntries = getAllCachedEntries();
                if (!cachedEntries.isEmpty()) {
                    Log.d(TAG, "API failed, using cached data as fallback");
                    callback.onSuccess(cachedEntries);
                } else {
                    String errorMessage = t != null ? t.getMessage() : "Unknown error";
                    String prefix = (t instanceof com.google.gson.JsonSyntaxException || t instanceof NumberFormatException)
                            ? "Data parsing error: "
                            : "Network error: ";
                    callback.onError(prefix + errorMessage);
                }
            }
        });
    }

    /**
     * Cache the playlist data to local database
     */
    private void cacheData(Playlist playlist) {
        try {
            // Clear existing data
            database.entryDao().deleteAll();

            // Convert and save entries
            List<EntryEntity> entitiesToInsert = new ArrayList<>();

            if (playlist.getCategories() != null) {
                for (Category category : playlist.getCategories()) {
                    if (category != null && category.getEntries() != null) {
                        String mainCategory = category.getMainCategory();

                        for (Entry entry : category.getEntries()) {
                            if (entry != null) {
                                EntryEntity entity = DatabaseUtils.entryToEntity(entry, mainCategory);
                                entitiesToInsert.add(entity);
                            }
                        }
                    }
                }
            }

            // Batch insert all entries
            if (!entitiesToInsert.isEmpty()) {
                database.entryDao().insertAll(entitiesToInsert);
            }

            // Update cache metadata
            CacheMetadataEntity metadata = new CacheMetadataEntity(
                CACHE_KEY_PLAYLIST,
                System.currentTimeMillis(),
                "1.0"
            );
            database.cacheMetadataDao().insert(metadata);

            Log.d(TAG, "Data cached successfully: " + entitiesToInsert.size() + " entries");

        } catch (Exception e) {
            Log.e(TAG, "Error caching data: " + e.getMessage(), e);
        }
    }
}