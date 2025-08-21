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

import java.io.File;
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

    // Column projection identical to PlaylistDatabaseManager for list/search queries
    private static final String ENTRY_LIST_COLUMNS = "id, title, description, main_category, sub_category, country, poster, thumbnail, rating, duration, year, servers_json";
    // Full projection including large JSON fields for detail fetches
    private static final String ENTRY_DETAIL_COLUMNS = "id, title, description, main_category, sub_category, country, poster, thumbnail, rating, duration, year, servers_json, seasons_json, related_json";

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
            Log.d(TAG, "=== CHECKING PLAYLIST DATABASE AVAILABILITY ===");
            
            // Check file path
            File dbFile = downloadManager.getLocalDatabaseFile();
            Log.d(TAG, "Expected database file path: " + dbFile.getAbsolutePath());
            Log.d(TAG, "File exists: " + dbFile.exists());
            if (dbFile.exists()) {
                Log.d(TAG, "File size: " + dbFile.length() + " bytes");
            }
            
            // Check if playlist.db exists and is valid
            boolean exists = downloadManager.isDatabaseExists();
            Log.d(TAG, "Database file exists: " + exists);
            
            if (!exists) {
                Log.d(TAG, "Playlist database file does not exist - will be created from assets by manager");
                // Let the manager attempt to create/copy from assets during initialize
            }
            
            boolean corrupted = downloadManager.isDatabaseCorrupted();
            Log.d(TAG, "Database file corrupted: " + corrupted);
            
            if (corrupted) {
                Log.d(TAG, "Playlist database file appears small; proceeding as bundled asset may be compact");
            }
            
            // Initialize the database manager
            Log.d(TAG, "Attempting to initialize playlist database...");
            boolean initialized = playlistManager.initializeDatabase();
            Log.d(TAG, "Database initialization result: " + initialized);
            
            if (!initialized) {
                Log.d(TAG, "Failed to initialize playlist database");
                return false;
            }
            
            // Check if database has data
            PlaylistDatabaseManager.DatabaseStats stats = playlistManager.getDatabaseStats();
            Log.d(TAG, "Database stats: " + stats.toString());
            
            // Allow empty DB; UI can handle no data states
            
            Log.d(TAG, "✅ Playlist database is valid with " + stats.totalEntries + " entries");
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
        Log.d(TAG, "forceRefreshData - triggering playlist.db download");
        triggerDownload(callback);
    }

    /**
     * Force download the playlist database
     */
    public void forceDownloadDatabase(DataCallback callback) {
        Log.i(TAG, "Force download playlist database from remote");
        triggerDownload(callback);
    }

    /**
     * Check if data is available and initialize if needed
     */
    public void ensureDataAvailable(DataCallback callback) {
        if (hasValidCache()) {
            Log.d(TAG, "Playlist database is available and valid");
            callback.onSuccess(new ArrayList<>()); // Return empty list to indicate success
        } else {
            Log.d(TAG, "No valid playlist database available");
            callback.onError("No playlist database available");
        }
    }

    private void triggerDownload(DataCallback callback) {
        try {
            // Ensure any cached Room data is cleared so we always read from the freshly downloaded DB
            try {
                CineCrazeDatabase.getInstance(context).clearAllTables();
            } catch (Exception ignored) {}

            downloadManager.downloadDatabase(new PlaylistDownloadManager.DownloadCallback() {
                @Override
                public void onDownloadStarted() { }

                @Override
                public void onDownloadProgress(int progress) { }

                @Override
                public void onDownloadCompleted(File dbFile) {
                    // After download, initialize database and signal success
                    try {
                        CineCrazeDatabase.getInstance(context).clearAllTables();
                    } catch (Exception ignored) {}
                    boolean initialized = playlistManager.initializeDatabase();
                    if (initialized) {
                        callback.onSuccess(new ArrayList<>());
                    } else {
                        callback.onError("Failed to initialize downloaded database");
                    }
                }

                @Override
                public void onDownloadFailed(String error) {
                    callback.onError(error);
                }

                @Override
                public void onUpdateAvailable() { }
            });
        } catch (Exception e) {
            callback.onError(e.getMessage());
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
            
            // Map servers and seasons if present as JSON columns
            int serversIndex = cursor.getColumnIndex("servers_json");
            if (serversIndex >= 0) {
                String serversJson = cursor.getString(serversIndex);
                if (serversJson != null && !serversJson.isEmpty()) {
                    try {
                        java.lang.reflect.Type serverListType = new com.google.gson.reflect.TypeToken<java.util.List<com.cinecraze.free.models.Server>>(){}.getType();
                        java.util.List<com.cinecraze.free.models.Server> servers = new com.google.gson.Gson().fromJson(serversJson, serverListType);
                        entry.setServers(servers);
                    } catch (Exception ignored) {}
                }
            }
            int seasonsIndex = cursor.getColumnIndex("seasons_json");
            if (seasonsIndex >= 0) {
                String seasonsJson = cursor.getString(seasonsIndex);
                if (seasonsJson != null && !seasonsJson.isEmpty()) {
                    try {
                        java.lang.reflect.Type seasonListType = new com.google.gson.reflect.TypeToken<java.util.List<com.cinecraze.free.models.Season>>(){}.getType();
                        java.util.List<com.cinecraze.free.models.Season> seasons = new com.google.gson.Gson().fromJson(seasonsJson, seasonListType);
                        entry.setSeasons(seasons);
                    } catch (Exception ignored) {}
                }
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
            int skipped = 0;
            while (cursor.moveToNext()) {
                if (skipped < offset) { skipped++; continue; }
                if (count >= pageSize) break;
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
            
            // Extract unique genres from sub_category (not main_category)
            android.database.Cursor cursor = playlistManager.getAllEntries();
            if (cursor == null) return new ArrayList<>();
            java.util.LinkedHashSet<String> genres = new java.util.LinkedHashSet<>();
            while (cursor.moveToNext()) {
                int idx = cursor.getColumnIndex("sub_category");
                if (idx >= 0) {
                    String sub = cursor.getString(idx);
                    if (sub != null) {
                        String value = sub.trim();
                        if (!value.isEmpty() && !value.equalsIgnoreCase("null")) {
                            genres.add(value);
                        }
                    }
                }
            }
            cursor.close();
            return new ArrayList<>(genres);
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
            
            // Use SQL-side filtering via PlaylistDatabaseManager for efficiency
            java.util.List<Entry> filteredEntries = new java.util.ArrayList<>();
            int offset = page * pageSize;
            android.database.Cursor cursor;
            if ((genre == null || genre.isEmpty()) && (country == null || country.isEmpty()) && (year == null || year.isEmpty())) {
                cursor = playlistManager.getRecentEntries(offset + pageSize);
            } else {
                // Fallback: simple where clauses built dynamically (case-sensitive match on stored strings)
                StringBuilder sql = new StringBuilder("SELECT " + ENTRY_LIST_COLUMNS + " FROM entries WHERE 1=1");
                java.util.List<String> args = new java.util.ArrayList<>();
                if (genre != null && !genre.isEmpty()) { sql.append(" AND sub_category = ?"); args.add(genre); }
                if (country != null && !country.isEmpty()) { sql.append(" AND country = ?"); args.add(country); }
                if (year != null && !year.isEmpty()) { sql.append(" AND year = ?"); args.add(year); }
                sql.append(" ORDER BY title ASC LIMIT ? OFFSET ?");
                args.add(String.valueOf(pageSize));
                args.add(String.valueOf(offset));
                cursor = playlistManager.rawQuery(sql.toString(), args.toArray(new String[0]));
            }
            int totalCount = 0;
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    Entry entry = cursorToEntry(cursor);
                    if (entry != null) filteredEntries.add(entry);
                    totalCount++;
                }
                cursor.close();
            }
            boolean hasMorePages = filteredEntries.size() == pageSize;
            callback.onSuccess(filteredEntries, hasMorePages, totalCount);
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

    /**
     * Load a single full entry by exact title and optional year, including JSON fields
     */
    public Entry loadFullEntry(String title, String yearString) {
        try {
            if (!hasValidCache()) {
                return null;
            }
            if (title == null || title.trim().isEmpty()) {
                return null;
            }
            StringBuilder sql = new StringBuilder("SELECT " + ENTRY_DETAIL_COLUMNS + " FROM entries WHERE title = ?");
            java.util.List<String> args = new java.util.ArrayList<>();
            args.add(title);
            if (yearString != null && !yearString.trim().isEmpty()) {
                sql.append(" AND year = ?");
                args.add(yearString.trim());
            }
            sql.append(" ORDER BY rowid DESC LIMIT 1");
            android.database.Cursor cursor = playlistManager.rawQuery(sql.toString(), args.toArray(new String[0]));
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        Entry entry = cursorToEntry(cursor);
                        return entry;
                    }
                } finally {
                    cursor.close();
                }
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error loading full entry by title/year: " + e.getMessage(), e);
            return null;
        }
    }
}