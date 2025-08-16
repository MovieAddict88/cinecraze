package com.cinecraze.free.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.cinecraze.free.database.entities.EntryEntity;
import com.cinecraze.free.database.entities.EntryLight;

import java.util.List;

@Dao
public interface EntryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<EntryEntity> entries);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EntryEntity entry);
    
    @Query("SELECT title, sub_category, country, description, poster, thumbnail, rating, duration, year, main_category FROM entries")
    List<EntryLight> getAllEntries();
    
    @Query("SELECT title, sub_category, country, description, poster, thumbnail, rating, duration, year, main_category FROM entries WHERE main_category = :category")
    List<EntryLight> getEntriesByCategory(String category);
    
    @Query("SELECT title, sub_category, country, description, poster, thumbnail, rating, duration, year, main_category FROM entries WHERE title LIKE '%' || :title || '%'")
    List<EntryLight> searchByTitle(String title);

    // Fetch the full row including JSON columns for a specific entry
    @Query("SELECT * FROM entries WHERE title = :title AND year = :year LIMIT 1")
    EntryEntity getFullEntryByTitleYear(String title, String year);
    
    @Query("SELECT COUNT(*) FROM entries")
    int getEntriesCount();
    
    @Query("DELETE FROM entries")
    void deleteAll();
    
    @Query("DELETE FROM entries WHERE main_category = :category")
    void deleteByCategory(String category);
    
    // Pagination queries
    @Query("SELECT title, sub_category, country, description, poster, thumbnail, rating, duration, year, main_category FROM entries ORDER BY title ASC LIMIT :limit OFFSET :offset")
    List<EntryLight> getEntriesPaged(int limit, int offset);
    
    @Query("SELECT title, sub_category, country, description, poster, thumbnail, rating, duration, year, main_category FROM entries WHERE main_category = :category ORDER BY title ASC LIMIT :limit OFFSET :offset")
    List<EntryLight> getEntriesByCategoryPaged(String category, int limit, int offset);
    
    @Query("SELECT title, sub_category, country, description, poster, thumbnail, rating, duration, year, main_category FROM entries WHERE title LIKE '%' || :title || '%' ORDER BY title ASC LIMIT :limit OFFSET :offset")
    List<EntryLight> searchByTitlePaged(String title, int limit, int offset);
    
    @Query("SELECT COUNT(*) FROM entries WHERE main_category = :category")
    int getEntriesCountByCategory(String category);
    
    @Query("SELECT COUNT(*) FROM entries WHERE title LIKE '%' || :title || '%'")
    int getSearchResultsCount(String title);
    
    // Filter queries for unique values
    @Query("SELECT DISTINCT sub_category FROM entries WHERE sub_category IS NOT NULL AND sub_category != '' ORDER BY sub_category ASC")
    List<String> getUniqueGenres();
    
    @Query("SELECT DISTINCT country FROM entries WHERE country IS NOT NULL AND country != '' ORDER BY country ASC")
    List<String> getUniqueCountries();
    
    @Query("SELECT DISTINCT year FROM entries WHERE year IS NOT NULL AND year != '' AND year != '0' ORDER BY year DESC")
    List<String> getUniqueYears();
    
    // Filtered pagination queries
    @Query("SELECT title, sub_category, country, description, poster, thumbnail, rating, duration, year, main_category FROM entries WHERE " +
           "(:genre IS NULL OR sub_category = :genre) AND " +
           "(:country IS NULL OR country = :country) AND " +
           "(:year IS NULL OR year = :year) " +
           "ORDER BY title ASC LIMIT :limit OFFSET :offset")
    List<EntryLight> getEntriesFilteredPaged(String genre, String country, String year, int limit, int offset);
    
    @Query("SELECT COUNT(*) FROM entries WHERE " +
           "(:genre IS NULL OR sub_category = :genre) AND " +
           "(:country IS NULL OR country = :country) AND " +
           "(:year IS NULL OR year = :year)")
    int getEntriesFilteredCount(String genre, String country, String year);

    @Query("SELECT title, sub_category, country, description, poster, thumbnail, rating, duration, year, main_category FROM entries ORDER BY CAST(rating AS REAL) DESC LIMIT :count")
    List<EntryLight> getTopRatedEntries(int count);

    // Recently added using implicit rowid order (fallback since we do not store timestamps)
    @Query("SELECT title, sub_category, country, description, poster, thumbnail, rating, duration, year, main_category FROM entries ORDER BY rowid DESC LIMIT :count")
    List<EntryLight> getRecentlyAdded(int count);
}