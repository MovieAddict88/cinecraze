package com.cinecraze.free.repository;

import android.content.Context;
import android.util.Log;

import com.cinecraze.free.database.CineCrazeDatabase;
import com.cinecraze.free.database.DatabaseUtils;
import com.cinecraze.free.database.entities.CacheMetadataEntity;
import com.cinecraze.free.database.entities.EntryEntity;
import com.cinecraze.free.models.Entry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DataRepository {

    private static final String TAG = "DataRepository";
    private static final String CACHE_KEY_PLAYLIST = "playlist_data";
    private static final long CACHE_EXPIRY_HOURS = 24; // Cache expires after 24 hours
    public static final int DEFAULT_PAGE_SIZE = 20; // Default items per page

    private CineCrazeDatabase database;

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
     * Get playlist data - checks cache first, no JSON fallback
     */
    public void getPlaylistData(DataCallback callback) {
        // Only load from cache; do not fetch JSON
        CacheMetadataEntity metadata = database.cacheMetadataDao().getMetadata(CACHE_KEY_PLAYLIST);

        if (metadata != null && isCacheValid(metadata.getLastUpdated())) {
            Log.d(TAG, "Using cached data");
            loadFromCache(callback);
        } else {
            Log.d(TAG, "No valid cache and JSON fetch disabled");
            callback.onError("No cached data available");
        }
    }

    /**
     * Force refresh data is disabled (no JSON)
     */
    public void forceRefreshData(DataCallback callback) {
        Log.d(TAG, "forceRefreshData disabled: JSON fetch removed");
        callback.onError("Refresh disabled");
    }

    /**
     * Check if data is available in cache and initialize if needed
     * For DB-first design, if cache empty return success (empty) so UI can proceed
     */
    public void ensureDataAvailable(DataCallback callback) {
        CacheMetadataEntity metadata = database.cacheMetadataDao().getMetadata(CACHE_KEY_PLAYLIST);

        if (metadata != null && isCacheValid(metadata.getLastUpdated())) {
            Log.d(TAG, "Cache is available and valid - ready for pagination");
            callback.onSuccess(new ArrayList<>());
        } else {
            Log.d(TAG, "No valid cache - JSON fetch disabled, caller should rely on playlist.db");
            callback.onError("No cached data");
        }
    }

    private boolean isCacheValid(long lastUpdatedMillis) {
        long currentTimeMillis = System.currentTimeMillis();
        long diffMillis = currentTimeMillis - lastUpdatedMillis;
        long diffHours = TimeUnit.MILLISECONDS.toHours(diffMillis);
        return diffHours < CACHE_EXPIRY_HOURS;
    }

    private void loadFromCache(DataCallback callback) {
        try {
            List<EntryEntity> entities = database.entryDao().getAllEntries();
            List<Entry> entries = DatabaseUtils.entitiesToEntries(entities);

            if (!entries.isEmpty()) {
                callback.onSuccess(entries);
            } else {
                Log.d(TAG, "Cache is empty and JSON fetch disabled");
                callback.onError("Cache empty");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading from cache: " + e.getMessage(), e);
            callback.onError("Error loading cache");
        }
    }

    // Pagination APIs remain the same (read-only from cache)
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

    public void refreshData(DataCallback callback) {
        Log.d(TAG, "refreshData disabled: JSON fetch removed");
        callback.onError("Refresh disabled");
    }

    public List<Entry> getEntriesByCategory(String category) {
        List<EntryEntity> entities = database.entryDao().getEntriesByCategory(category);
        return DatabaseUtils.entitiesToEntries(entities);
    }

    public List<Entry> searchByTitle(String title) {
        List<EntryEntity> entities = database.entryDao().searchByTitle(title);
        return DatabaseUtils.entitiesToEntries(entities);
    }

    public List<Entry> getAllCachedEntries() {
        List<EntryEntity> entities = database.entryDao().getAllEntries();
        return DatabaseUtils.entitiesToEntries(entities);
    }

    public int getTotalEntriesCount() {
        return database.entryDao().getEntriesCount();
    }

    public List<String> getUniqueGenres() {
        try {
            List<String> genres = database.entryDao().getUniqueGenres();
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

    public List<String> getUniqueCountries() {
        try {
            List<String> countries = database.entryDao().getUniqueCountries();
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

    public List<String> getUniqueYears() {
        try {
            List<String> years = database.entryDao().getUniqueYears();
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
        List<EntryEntity> entities = database.entryDao().getTopRatedEntries(count);
        return DatabaseUtils.entitiesToEntries(entities);
    }

    public List<Entry> getRecentlyAdded(int count) {
        List<EntryEntity> entities = database.entryDao().getRecentlyAdded(count);
        return DatabaseUtils.entitiesToEntries(entities);
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
}