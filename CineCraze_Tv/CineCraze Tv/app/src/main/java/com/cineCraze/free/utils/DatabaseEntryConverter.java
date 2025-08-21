package com.cinecraze.free.utils;

import android.database.Cursor;
import android.util.Log;

import com.cinecraze.free.models.Entry;
import com.cinecraze.free.models.Server;
import com.cinecraze.free.models.Season;
import com.cinecraze.free.models.Episode;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to convert database cursor results to Entry objects
 */
public class DatabaseEntryConverter {
    
    private static final String TAG = "DatabaseEntryConverter";
    private static final Gson gson = new Gson();
    
    /**
     * Convert cursor to Entry object
     */
    public static Entry cursorToEntry(Cursor cursor) {
        if (cursor == null || cursor.isClosed()) {
            return null;
        }
        
        try {
            Entry entry = new Entry();
            
            // Basic fields
            entry.setTitle(getString(cursor, "title"));
            entry.setSubCategory(getString(cursor, "sub_category"));
            entry.setMainCategory(getString(cursor, "main_category"));
            entry.setCountry(getString(cursor, "country"));
            entry.setDescription(getString(cursor, "description"));
            entry.setPoster(getString(cursor, "poster"));
            entry.setThumbnail(getString(cursor, "thumbnail"));
            entry.setDuration(getString(cursor, "duration"));
            
            // Rating and Year (can be mixed types)
            String ratingStr = getString(cursor, "rating");
            if (ratingStr != null && !ratingStr.isEmpty()) {
                try {
                    // Try to parse as number first
                    float rating = Float.parseFloat(ratingStr);
                    entry.setRating(rating);
                } catch (NumberFormatException e) {
                    // If not a number, set as string
                    entry.setRating(ratingStr);
                }
            }
            
            String yearStr = getString(cursor, "year");
            if (yearStr != null && !yearStr.isEmpty()) {
                try {
                    // Try to parse as number first
                    int year = Integer.parseInt(yearStr);
                    entry.setYear(year);
                } catch (NumberFormatException e) {
                    // If not a number, set as string
                    entry.setYear(yearStr);
                }
            }
            
            // Parse JSON fields
            entry.setServers(parseServers(getString(cursor, "servers_json")));
            entry.setSeasons(parseSeasons(getString(cursor, "seasons_json")));
            entry.setRelated(parseRelatedEntries(getString(cursor, "related_json")));
            
            return entry;
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting cursor to Entry", e);
            return null;
        }
    }
    
    /**
     * Convert cursor to list of Entry objects
     */
    public static List<Entry> cursorToEntryList(Cursor cursor) {
        List<Entry> entries = new ArrayList<>();
        
        if (cursor == null || cursor.isClosed()) {
            return entries;
        }
        
        try {
            if (cursor.moveToFirst()) {
                do {
                    Entry entry = cursorToEntry(cursor);
                    if (entry != null) {
                        entries.add(entry);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error converting cursor to Entry list", e);
        }
        
        return entries;
    }
    
    /**
     * Parse servers JSON string to List<Server>
     */
    private static List<Server> parseServers(String serversJson) {
        if (serversJson == null || serversJson.isEmpty() || serversJson.equals("[]")) {
            return new ArrayList<>();
        }
        
        try {
            Type listType = new TypeToken<List<Server>>(){}.getType();
            return gson.fromJson(serversJson, listType);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing servers JSON: " + serversJson, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Parse seasons JSON string to List<Season>
     */
    private static List<Season> parseSeasons(String seasonsJson) {
        if (seasonsJson == null || seasonsJson.isEmpty() || seasonsJson.equals("[]")) {
            return new ArrayList<>();
        }
        
        try {
            Type listType = new TypeToken<List<Season>>(){}.getType();
            return gson.fromJson(seasonsJson, listType);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing seasons JSON: " + seasonsJson, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Parse related entries JSON string to List<Entry>
     */
    private static List<Entry> parseRelatedEntries(String relatedJson) {
        if (relatedJson == null || relatedJson.isEmpty() || relatedJson.equals("[]")) {
            return new ArrayList<>();
        }
        
        try {
            Type listType = new TypeToken<List<Entry>>(){}.getType();
            return gson.fromJson(relatedJson, listType);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing related entries JSON: " + relatedJson, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Safe string getter from cursor
     */
    private static String getString(Cursor cursor, String columnName) {
        try {
            int columnIndex = cursor.getColumnIndex(columnName);
            if (columnIndex >= 0) {
                return cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting column: " + columnName, e);
        }
        return null;
    }
    
    /**
     * Safe integer getter from cursor
     */
    private static int getInt(Cursor cursor, String columnName) {
        try {
            int columnIndex = cursor.getColumnIndex(columnName);
            if (columnIndex >= 0) {
                return cursor.getInt(columnIndex);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting column: " + columnName, e);
        }
        return 0;
    }
    
    /**
     * Safe float getter from cursor
     */
    private static float getFloat(Cursor cursor, String columnName) {
        try {
            int columnIndex = cursor.getColumnIndex(columnName);
            if (columnIndex >= 0) {
                return cursor.getFloat(columnIndex);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting column: " + columnName, e);
        }
        return 0.0f;
    }
    
    /**
     * Get entry ID from cursor
     */
    public static int getEntryId(Cursor cursor) {
        return getInt(cursor, "id");
    }
    
    /**
     * Check if entry is a TV series (has seasons)
     */
    public static boolean isTvSeries(Cursor cursor) {
        String seasonsJson = getString(cursor, "seasons_json");
        return seasonsJson != null && !seasonsJson.isEmpty() && !seasonsJson.equals("[]");
    }
    
    /**
     * Check if entry has servers
     */
    public static boolean hasServers(Cursor cursor) {
        String serversJson = getString(cursor, "servers_json");
        return serversJson != null && !serversJson.isEmpty() && !serversJson.equals("[]");
    }
    
    /**
     * Get entry type (Movie, TV Series, Live TV)
     */
    public static String getEntryType(Cursor cursor) {
        String mainCategory = getString(cursor, "main_category");
        if (mainCategory != null) {
            return mainCategory;
        }
        
        // Fallback: determine by seasons
        if (isTvSeries(cursor)) {
            return "TV Series";
        }
        
        return "Movie";
    }
}