package com.cinecraze.free.repository;

import android.content.Context;
import android.util.Log;

import com.cinecraze.free.database.CineCrazeDatabase;
import com.cinecraze.free.database.DatabaseUtils;
import com.cinecraze.free.database.PlaylistDatabaseManager;
import com.cinecraze.free.utils.PlaylistDownloadManager;
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
    private PlaylistDatabaseManager playlistManager;
    private PlaylistDownloadManager downloadManager;
    private Context context;

    public interface DataCallback {
        void onSuccess(List<Entry> entries);
        void onError(String error);
    }

    public interface PaginatedDataCallback {
        void onSuccess(List<Entry> entries, boolean hasMorePages, int totalCount);
        void onError(String error);
    }

    public DataRepository(Context context) {
        this.context = context.getApplicationContext();
        this.database = CineCrazeDatabase.getInstance(context);
        this.playlistManager = new PlaylistDatabaseManager(context);
        this.downloadManager = new PlaylistDownloadManager(context);
    }

    /**
     * Check if playlist.db exists and is valid
     */
    public boolean hasValidCache() {
        try {
            // Check if playlist.db exists and is valid
            if (!downloadManager.isDatabaseExists()) {
                Log.d(TAG, "Playlist database does not exist");
                return false;
            }
            
            if (downloadManager.isDatabaseCorrupted()) {
                Log.d(TAG, "Playlist database is corrupted");
                return false;
            }
            
            // Initialize the database manager
            if (!playlistManager.initializeDatabase()) {
                Log.d(TAG, "Failed to initialize playlist database");
                return false;
            }
            
            // Check if database has data
            PlaylistDatabaseManager.DatabaseStats stats = playlistManager.getDatabaseStats();
            if (stats.totalEntries == 0) {
                Log.d(TAG, "Playlist database is empty");
                return false;
            }
            
            Log.d(TAG, "Playlist database is valid with " + stats.totalEntries + " entries");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking playlist database validity: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get playlist data from playlist.db
     */
    public void getPlaylistData(DataCallback callback) {
        try {
            if (!hasValidCache()) {
                Log.d(TAG, "No valid playlist database available");
                callback.onError("No playlist database available");
                return;
            }
            
            Log.d(TAG, "Loading data from playlist database");
            loadFromPlaylistDatabase(callback);
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting playlist data: " + e.getMessage(), e);
            callback.onError("Error loading playlist data: " + e.getMessage());
        }
    }

    /**
     * Force refresh data - this would trigger a new download
     */
    public void forceRefreshData(DataCallback callback) {
        Log.d(TAG, "forceRefreshData - would trigger playlist.db download");
        callback.onError("Refresh requires playlist.db download");
    }

    /**
     * Check if data is available and initialize if needed
     */
    public void ensureDataAvailable(DataCallback callback) {
        if (hasValidCache()) {
            Log.d(TAG, "Playlist database is available and valid");
            callback.onSuccess(new ArrayList<>()); // Return empty list to indicate success
        } else {
            Log.d(TAG, "No valid playlist database - needs download");
            callback.onError("No playlist database available");
        }
    }

    /**
     * Load data from playlist database
     */
    private void loadFromPlaylistDatabase(DataCallback callback) {
        try {
            // Get all entries from playlist database
            android.database.Cursor cursor = playlistManager.getAllEntries();
            if (cursor == null) {
                callback.onError("Failed to load playlist data");
                return;
            }
            
            List<Entry> entries = new ArrayList<>();
            while (cursor.moveToNext()) {
                Entry entry = cursorToEntry(cursor);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            cursor.close();
            
            Log.d(TAG, "Loaded " + entries.size() + " entries from playlist database");
            callback.onSuccess(entries);
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading from playlist database: " + e.getMessage(), e);
            callback.onError("Error loading playlist data: " + e.getMessage());
        }
    }

    /**
     * Convert cursor to Entry object
     */
    private Entry cursorToEntry(android.database.Cursor cursor) {
        try {
            Entry entry = new Entry();
            
            // Set basic fields using correct column names
            entry.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
            entry.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
            entry.setMainCategory(cursor.getString(cursor.getColumnIndexOrThrow("main_category")));
            entry.setSubCategory(cursor.getString(cursor.getColumnIndexOrThrow("sub_category")));
            entry.setCountry(cursor.getString(cursor.getColumnIndexOrThrow("country")));
            
            // Set image URL (use poster or thumbnail)
            String poster = cursor.getString(cursor.getColumnIndexOrThrow("poster"));
            String thumbnail = cursor.getString(cursor.getColumnIndexOrThrow("thumbnail"));
            if (poster != null && !poster.isEmpty()) {
                entry.setPoster(poster);
            } else if (thumbnail != null && !thumbnail.isEmpty()) {
                entry.setThumbnail(thumbnail);
            }
            
            // Set year
            String yearStr = cursor.getString(cursor.getColumnIndexOrThrow("year"));
            if (yearStr != null && !yearStr.isEmpty()) {
                entry.setYear(yearStr);
            }
            
            // Set rating
            String ratingStr = cursor.getString(cursor.getColumnIndexOrThrow("rating"));
            if (ratingStr != null && !ratingStr.isEmpty()) {
                entry.setRating(ratingStr);
            }
            
            // Set duration
            String duration = cursor.getString(cursor.getColumnIndexOrThrow("duration"));
            if (duration != null && !duration.isEmpty()) {
                entry.setDuration(duration);
            }
            
            return entry;
        } catch (Exception e) {
            Log.e(TAG, "Error converting cursor to entry: " + e.getMessage());
            return null;
        }
    }

    // Pagination APIs using playlist database
    public void getPaginatedData(int page, int pageSize, PaginatedDataCallback callback) {
        try {
            if (!hasValidCache()) {
                callback.onError("No playlist database available");
                return;
            }
            
            int offset = page * pageSize;
            android.database.Cursor cursor = playlistManager.getRecentEntries(offset + pageSize);
            if (cursor == null) {
                callback.onError("Failed to load paginated data");
                return;
            }
            
            List<Entry> entries = new ArrayList<>();
            int count = 0;
            while (cursor.moveToNext() && count < pageSize) {
                Entry entry = cursorToEntry(cursor);
                if (entry != null) {
                    entries.add(entry);
                }
                count++;
            }
            cursor.close();
            
            // Get total count
            PlaylistDatabaseManager.DatabaseStats stats = playlistManager.getDatabaseStats();
            int totalCount = stats.totalEntries;
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
            if (!hasValidCache()) {
                callback.onError("No playlist database available");
                return;
            }
            
            android.database.Cursor cursor = playlistManager.getEntriesByCategory(category);
            if (cursor == null) {
                callback.onError("Failed to load category data");
                return;
            }
            
            List<Entry> entries = new ArrayList<>();
            int offset = page * pageSize;
            int count = 0;
            int totalCount = 0;
            
            while (cursor.moveToNext()) {
                totalCount++;
                if (totalCount > offset && count < pageSize) {
                    Entry entry = cursorToEntry(cursor);
                    if (entry != null) {
                        entries.add(entry);
                    }
                    count++;
                }
            }
            cursor.close();
            
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
            if (!hasValidCache()) {
                callback.onError("No playlist database available");
                return;
            }
            
            android.database.Cursor cursor = playlistManager.searchEntries(searchQuery);
            if (cursor == null) {
                callback.onError("Failed to search data");
                return;
            }
            
            List<Entry> entries = new ArrayList<>();
            int offset = page * pageSize;
            int count = 0;
            int totalCount = 0;
            
            while (cursor.moveToNext()) {
                totalCount++;
                if (totalCount > offset && count < pageSize) {
                    Entry entry = cursorToEntry(cursor);
                    if (entry != null) {
                        entries.add(entry);
                    }
                    count++;
                }
            }
            cursor.close();
            
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
        try {
            if (!hasValidCache()) {
                return new ArrayList<>();
            }
            
            android.database.Cursor cursor = playlistManager.getEntriesByCategory(category);
            if (cursor == null) {
                return new ArrayList<>();
            }
            
            List<Entry> entries = new ArrayList<>();
            while (cursor.moveToNext()) {
                Entry entry = cursorToEntry(cursor);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            cursor.close();
            return entries;
        } catch (Exception e) {
            Log.e(TAG, "Error getting entries by category: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<Entry> searchByTitle(String title) {
        try {
            if (!hasValidCache()) {
                return new ArrayList<>();
            }
            
            android.database.Cursor cursor = playlistManager.searchEntries(title);
            if (cursor == null) {
                return new ArrayList<>();
            }
            
            List<Entry> entries = new ArrayList<>();
            while (cursor.moveToNext()) {
                Entry entry = cursorToEntry(cursor);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            cursor.close();
            return entries;
        } catch (Exception e) {
            Log.e(TAG, "Error searching by title: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<Entry> getAllCachedEntries() {
        try {
            if (!hasValidCache()) {
                return new ArrayList<>();
            }
            
            android.database.Cursor cursor = playlistManager.getAllEntries();
            if (cursor == null) {
                return new ArrayList<>();
            }
            
            List<Entry> entries = new ArrayList<>();
            while (cursor.moveToNext()) {
                Entry entry = cursorToEntry(cursor);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            cursor.close();
            return entries;
        } catch (Exception e) {
            Log.e(TAG, "Error getting all entries: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public int getTotalEntriesCount() {
        try {
            if (!hasValidCache()) {
                return 0;
            }
            
            PlaylistDatabaseManager.DatabaseStats stats = playlistManager.getDatabaseStats();
            return stats.totalEntries;
        } catch (Exception e) {
            Log.e(TAG, "Error getting total entries count: " + e.getMessage(), e);
            return 0;
        }
    }

    public List<String> getUniqueGenres() {
        try {
            if (!hasValidCache()) {
                return new ArrayList<>();
            }
            
            // Get all entries and extract unique genres
            List<Entry> entries = getAllCachedEntries();
            List<String> genres = new ArrayList<>();
            for (Entry entry : entries) {
                if (entry.getMainCategory() != null && !entry.getMainCategory().trim().isEmpty() && 
                    !entry.getMainCategory().equalsIgnoreCase("null") && !genres.contains(entry.getMainCategory())) {
                    genres.add(entry.getMainCategory().trim());
                }
            }
            return genres;
        } catch (Exception e) {
            Log.e(TAG, "Error getting unique genres: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<String> getUniqueCountries() {
        try {
            if (!hasValidCache()) {
                return new ArrayList<>();
            }
            
            // For now, return empty list as country data might not be available
            return new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Error getting unique countries: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<String> getUniqueYears() {
        try {
            if (!hasValidCache()) {
                return new ArrayList<>();
            }
            
            // Get all entries and extract unique years
            List<Entry> entries = getAllCachedEntries();
            List<String> years = new ArrayList<>();
            for (Entry entry : entries) {
                String yearStr = entry.getYearString();
                if (yearStr != null && !yearStr.trim().isEmpty() && 
                    !yearStr.equalsIgnoreCase("null") && !yearStr.equals("0") && 
                    !years.contains(yearStr)) {
                    years.add(yearStr.trim());
                }
            }
            return years;
        } catch (Exception e) {
            Log.e(TAG, "Error getting unique years: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<Entry> getTopRatedEntries(int count) {
        try {
            if (!hasValidCache()) {
                return new ArrayList<>();
            }
            
            // Get recent entries as a simple implementation
            android.database.Cursor cursor = playlistManager.getRecentEntries(count);
            if (cursor == null) {
                return new ArrayList<>();
            }
            
            List<Entry> entries = new ArrayList<>();
            while (cursor.moveToNext()) {
                Entry entry = cursorToEntry(cursor);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            cursor.close();
            return entries;
        } catch (Exception e) {
            Log.e(TAG, "Error getting top rated entries: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<Entry> getRecentlyAdded(int count) {
        try {
            if (!hasValidCache()) {
                return new ArrayList<>();
            }
            
            // Get recent entries
            android.database.Cursor cursor = playlistManager.getRecentEntries(count);
            if (cursor == null) {
                return new ArrayList<>();
            }
            
            List<Entry> entries = new ArrayList<>();
            while (cursor.moveToNext()) {
                Entry entry = cursorToEntry(cursor);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            cursor.close();
            return entries;
        } catch (Exception e) {
            Log.e(TAG, "Error getting recently added entries: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public void getPaginatedFilteredData(String genre, String country, String year, int page, int pageSize, PaginatedDataCallback callback) {
        try {
            if (!hasValidCache()) {
                callback.onError("No playlist database available");
                return;
            }
            
            // For now, just get all entries and filter in memory
            List<Entry> allEntries = getAllCachedEntries();
            List<Entry> filteredEntries = new ArrayList<>();
            
            for (Entry entry : allEntries) {
                boolean matchesGenre = genre == null || genre.isEmpty() || 
                    (entry.getMainCategory() != null && entry.getMainCategory().equals(genre));
                boolean matchesCountry = country == null || country.isEmpty(); // Country not available in playlist.db
                boolean matchesYear = year == null || year.isEmpty() || 
                    (entry.getYearString() != null && entry.getYearString().equals(year));
                
                if (matchesGenre && matchesCountry && matchesYear) {
                    filteredEntries.add(entry);
                }
            }
            
            int offset = page * pageSize;
            int totalCount = filteredEntries.size();
            int endIndex = Math.min(offset + pageSize, totalCount);
            int startIndex = Math.min(offset, totalCount);
            
            List<Entry> pageEntries = filteredEntries.subList(startIndex, endIndex);
            boolean hasMorePages = (offset + pageSize) < totalCount;
            
            callback.onSuccess(pageEntries, hasMorePages, totalCount);
        } catch (Exception e) {
            Log.e(TAG, "Error loading filtered data: " + e.getMessage(), e);
            callback.onError("Error loading filtered data: " + e.getMessage());
        }
    }

    public Entry findEntryByHashId(int hashId) {
        try {
            if (!hasValidCache()) {
                return null;
            }
            
            android.database.Cursor cursor = playlistManager.getEntryById(hashId);
            if (cursor == null || !cursor.moveToFirst()) {
                return null;
            }
            
            Entry entry = cursorToEntry(cursor);
            cursor.close();
            return entry;
        } catch (Exception e) {
            Log.e(TAG, "Error finding entry by hash ID: " + e.getMessage(), e);
            return null;
        }
    }
}