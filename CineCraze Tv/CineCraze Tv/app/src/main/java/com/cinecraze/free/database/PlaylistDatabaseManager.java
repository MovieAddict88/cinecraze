package com.cinecraze.free.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.cinecraze.free.utils.PlaylistDownloadManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Manages the local playlist database operations
 * Uses the downloaded playlist.db file instead of creating a new database
 */
public class PlaylistDatabaseManager extends SQLiteOpenHelper {
    
    private static final String TAG = "PlaylistDatabaseManager";
    private static final String DATABASE_NAME = "playlist.db";
    private static final int DATABASE_VERSION = 1;
    
    private Context context;
    private PlaylistDownloadManager downloadManager;
    private SQLiteDatabase database;
    
    public PlaylistDatabaseManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context.getApplicationContext();
        this.downloadManager = new PlaylistDownloadManager(context);
    }
    
    /**
     * Initialize the database using the downloaded playlist.db file
     */
    public boolean initializeDatabase() {
        try {
            Log.d(TAG, "=== INITIALIZING PLAYLIST DATABASE ===");
            
            File localDbFile = downloadManager.getLocalDatabaseFile();
            Log.d(TAG, "Local database file path: " + localDbFile.getAbsolutePath());
            Log.d(TAG, "Local database file exists: " + localDbFile.exists());
            
            if (!localDbFile.exists()) {
                Log.w(TAG, "Local database file not found");
                return false;
            }
            
            Log.d(TAG, "Local database file size: " + localDbFile.length() + " bytes");
            
            // Check if database is corrupted
            boolean corrupted = downloadManager.isDatabaseCorrupted();
            Log.d(TAG, "Database corruption check: " + corrupted);
            
            if (corrupted) {
                Log.e(TAG, "Database file is corrupted");
                return false;
            }
            
            // Open the database
            Log.d(TAG, "Opening database...");
            database = getWritableDatabase();
            Log.d(TAG, "Database opened successfully");
            
            // Verify database integrity
            boolean valid = isDatabaseValid();
            Log.d(TAG, "Database validation result: " + valid);
            
            if (!valid) {
                Log.e(TAG, "Database validation failed");
                return false;
            }
            
            Log.i(TAG, "Database initialized successfully");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing database", e);
            return false;
        }
    }
    
    /**
     * Check if database is valid and has required tables
     */
    private boolean isDatabaseValid() {
        Log.d(TAG, "=== VALIDATING DATABASE ===");
        
        if (database == null || !database.isOpen()) {
            Log.e(TAG, "Database is null or not open");
            return false;
        }
        
        try {
            // First, check what tables actually exist
            android.database.Cursor tableCursor = database.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table'", null
            );
            
            Log.d(TAG, "Available tables in database:");
            while (tableCursor.moveToNext()) {
                String tableName = tableCursor.getString(0);
                Log.d(TAG, "  - " + tableName);
            }
            tableCursor.close();
            
            // Check if required tables exist - only require 'entries' table
            String[] requiredTables = {"entries"};
            
            for (String table : requiredTables) {
                android.database.Cursor cursor = database.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name=?", 
                    new String[]{table}
                );
                
                boolean tableExists = cursor.getCount() > 0;
                cursor.close();
                
                Log.d(TAG, "Required table '" + table + "' exists: " + tableExists);
                
                if (!tableExists) {
                    Log.e(TAG, "Required table missing: " + table);
                    return false;
                }
            }
            
            // Check if entries table has data
            android.database.Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM entries", null);
            if (cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                cursor.close();
                
                Log.d(TAG, "Entries table count: " + count);
                
                if (count == 0) {
                    Log.w(TAG, "Entries table is empty");
                    return false;
                }
                
                Log.i(TAG, "Database contains " + count + " entries");
            } else {
                cursor.close();
                Log.e(TAG, "Could not read entries count");
                return false;
            }
            
            Log.d(TAG, "Database validation successful");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error validating database", e);
            return false;
        }
    }
    
    /**
     * Get database statistics
     */
    public DatabaseStats getDatabaseStats() {
        DatabaseStats stats = new DatabaseStats();
        
        if (database == null || !database.isOpen()) {
            return stats;
        }
        
        try {
            // Total entries
            android.database.Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM entries", null);
            if (cursor.moveToFirst()) {
                stats.totalEntries = cursor.getInt(0);
            }
            cursor.close();
            
            // Entries by category
            cursor = database.rawQuery(
                "SELECT main_category, COUNT(*) FROM entries GROUP BY main_category", 
                null
            );
            
            while (cursor.moveToNext()) {
                String category = cursor.getString(0);
                int count = cursor.getInt(1);
                stats.entriesByCategory.put(category, count);
            }
            cursor.close();
            
            // Database size
            stats.databaseSizeMB = downloadManager.getDatabaseSizeMB();
            
            // Last update time
            stats.lastUpdateTime = downloadManager.getLastUpdateTime();
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting database stats", e);
        }
        
        return stats;
    }
    
    /**
     * Search entries by title
     */
    public android.database.Cursor searchEntries(String query) {
        if (database == null || !database.isOpen()) {
            return null;
        }
        
        String searchQuery = "SELECT * FROM entries WHERE title LIKE ? ORDER BY title";
        return database.rawQuery(searchQuery, new String[]{"%" + query + "%"});
    }
    
    /**
     * Get entries by category
     */
    public android.database.Cursor getEntriesByCategory(String category) {
        if (database == null || !database.isOpen()) {
            return null;
        }
        
        String query = "SELECT * FROM entries WHERE main_category = ? ORDER BY title";
        return database.rawQuery(query, new String[]{category});
    }
    
    /**
     * Get entries by subcategory
     */
    public android.database.Cursor getEntriesBySubCategory(String subCategory) {
        if (database == null || !database.isOpen()) {
            return null;
        }
        
        String query = "SELECT * FROM entries WHERE sub_category = ? ORDER BY title";
        return database.rawQuery(query, new String[]{subCategory});
    }
    
    /**
     * Get all entries
     */
    public android.database.Cursor getAllEntries() {
        if (database == null || !database.isOpen()) {
            return null;
        }
        
        return database.rawQuery("SELECT * FROM entries ORDER BY title", null);
    }
    
    /**
     * Get all categories
     */
    public android.database.Cursor getAllCategories() {
        if (database == null || !database.isOpen()) {
            return null;
        }
        
        return database.rawQuery("SELECT * FROM categories ORDER BY main_category", null);
    }
    
    /**
     * Get entry by ID
     */
    public android.database.Cursor getEntryById(int id) {
        if (database == null || !database.isOpen()) {
            return null;
        }
        
        return database.rawQuery("SELECT * FROM entries WHERE id = ?", new String[]{String.valueOf(id)});
    }
    
    /**
     * Get recent entries (limit by count)
     */
    public android.database.Cursor getRecentEntries(int limit) {
        if (database == null || !database.isOpen()) {
            return null;
        }
        
        return database.rawQuery("SELECT * FROM entries ORDER BY id DESC LIMIT ?", new String[]{String.valueOf(limit)});
    }
    
    /**
     * Get entries by year
     */
    public android.database.Cursor getEntriesByYear(String year) {
        if (database == null || !database.isOpen()) {
            return null;
        }
        
        return database.rawQuery("SELECT * FROM entries WHERE year = ? ORDER BY title", new String[]{year});
    }
    
    /**
     * Get metadata
     */
    public android.database.Cursor getMetadata() {
        if (database == null || !database.isOpen()) {
            return null;
        }
        
        return database.rawQuery("SELECT * FROM metadata ORDER BY id DESC LIMIT 1", null);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        // This method is not used since we're using a pre-built database
        Log.d(TAG, "onCreate called - using pre-built database");
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle database upgrades if needed
        Log.d(TAG, "onUpgrade called from " + oldVersion + " to " + newVersion);
    }
    
    @Override
    public synchronized void close() {
        if (database != null && database.isOpen()) {
            database.close();
        }
        super.close();
    }
    
    /**
     * Database statistics class
     */
    public static class DatabaseStats {
        public int totalEntries = 0;
        public java.util.Map<String, Integer> entriesByCategory = new java.util.HashMap<>();
        public float databaseSizeMB = 0;
        public String lastUpdateTime = "Unknown";
        
        @Override
        public String toString() {
            return String.format("DatabaseStats{totalEntries=%d, size=%.2fMB, lastUpdate='%s'}", 
                totalEntries, databaseSizeMB, lastUpdateTime);
        }
    }
}