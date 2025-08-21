package com.cinecraze.free.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;

/**
 * Helper class to locate and manage the downloaded playlist.db file
 */
public class DatabaseLocationHelper {
    
    private static final String TAG = "DatabaseLocationHelper";
    private static final String DATABASE_NAME = "playlist.db";
    
    private Context context;
    
    public DatabaseLocationHelper(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * Get the exact file path where playlist.db is stored
     */
    public String getDatabasePath() {
        File dbFile = getDatabaseFile();
        return dbFile.getAbsolutePath();
    }
    
    /**
     * Get the database file object
     */
    public File getDatabaseFile() {
        return new File(context.getFilesDir(), DATABASE_NAME);
    }
    
    /**
     * Get the app's internal files directory path
     */
    public String getAppFilesDirectory() {
        return context.getFilesDir().getAbsolutePath();
    }
    
    /**
     * Check if database exists
     */
    public boolean isDatabaseExists() {
        File dbFile = getDatabaseFile();
        return dbFile.exists() && dbFile.length() > 0;
    }
    
    /**
     * Get database file size in bytes
     */
    public long getDatabaseSize() {
        File dbFile = getDatabaseFile();
        if (dbFile.exists()) {
            return dbFile.length();
        }
        return 0;
    }
    
    /**
     * Get database file size in MB
     */
    public float getDatabaseSizeMB() {
        long sizeBytes = getDatabaseSize();
        return sizeBytes / (1024.0f * 1024.0f);
    }
    
    /**
     * Get database last modified time
     */
    public long getDatabaseLastModified() {
        File dbFile = getDatabaseFile();
        if (dbFile.exists()) {
            return dbFile.lastModified();
        }
        return 0;
    }
    
    /**
     * Print database location information to logcat
     */
    public void logDatabaseInfo() {
        Log.i(TAG, "=== Database Location Information ===");
        Log.i(TAG, "App Files Directory: " + getAppFilesDirectory());
        Log.i(TAG, "Database File Path: " + getDatabasePath());
        Log.i(TAG, "Database Exists: " + isDatabaseExists());
        
        if (isDatabaseExists()) {
            Log.i(TAG, "Database Size: " + getDatabaseSizeMB() + " MB");
            Log.i(TAG, "Database Size (bytes): " + getDatabaseSize());
            Log.i(TAG, "Last Modified: " + getDatabaseLastModified());
        }
        Log.i(TAG, "=====================================");
    }
    
    /**
     * Get human-readable database information
     */
    public String getDatabaseInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Database Location Information:\n");
        info.append("App Files Directory: ").append(getAppFilesDirectory()).append("\n");
        info.append("Database File Path: ").append(getDatabasePath()).append("\n");
        info.append("Database Exists: ").append(isDatabaseExists()).append("\n");
        
        if (isDatabaseExists()) {
            info.append("Database Size: ").append(String.format("%.2f", getDatabaseSizeMB())).append(" MB\n");
            info.append("Last Modified: ").append(getDatabaseLastModified()).append("\n");
        }
        
        return info.toString();
    }
    
    /**
     * Get the typical Android data folder path for this app
     * This is the path you would see in file managers
     */
    public String getTypicalDataFolderPath() {
        String packageName = context.getPackageName();
        return "/data/data/" + packageName + "/files/";
    }
    
    /**
     * Get the typical Android data folder path with database name
     */
    public String getTypicalDatabasePath() {
        return getTypicalDataFolderPath() + DATABASE_NAME;
    }
    
    /**
     * Check if database is accessible (readable)
     */
    public boolean isDatabaseReadable() {
        File dbFile = getDatabaseFile();
        return dbFile.exists() && dbFile.canRead();
    }
    
    /**
     * Check if database is writable
     */
    public boolean isDatabaseWritable() {
        File dbFile = getDatabaseFile();
        return dbFile.exists() && dbFile.canWrite();
    }
    
    /**
     * Get database file permissions
     */
    public String getDatabasePermissions() {
        File dbFile = getDatabaseFile();
        if (!dbFile.exists()) {
            return "File does not exist";
        }
        
        StringBuilder permissions = new StringBuilder();
        permissions.append(dbFile.canRead() ? "r" : "-");
        permissions.append(dbFile.canWrite() ? "w" : "-");
        permissions.append(dbFile.canExecute() ? "x" : "-");
        
        return permissions.toString();
    }
}