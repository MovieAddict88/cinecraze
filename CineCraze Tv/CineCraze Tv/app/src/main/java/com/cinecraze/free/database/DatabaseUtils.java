package com.cinecraze.free.database;

import com.cinecraze.free.database.entities.EntryEntity;
import com.cinecraze.free.database.entities.EntryLight;
import com.cinecraze.free.models.Entry;
import com.cinecraze.free.models.Server;
import com.cinecraze.free.models.Season;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DatabaseUtils {
    
    private static final Gson gson = new Gson();
    
    /**
     * Convert Entry API model to EntryEntity database entity
     */
    public static EntryEntity entryToEntity(Entry entry, String mainCategory) {
        EntryEntity entity = new EntryEntity();
        
        entity.setTitle(entry.getTitle());
        entity.setSubCategory(entry.getSubCategory());
        entity.setCountry(entry.getCountry());
        entity.setDescription(entry.getDescription());
        entity.setPoster(entry.getPoster());
        entity.setThumbnail(entry.getThumbnail());
        entity.setRating(entry.getRatingString());
        entity.setDuration(entry.getDuration());
        entity.setYear(entry.getYearString());
        entity.setMainCategory(mainCategory);
        
        // Convert complex objects to JSON strings
        entity.setServersJson(gson.toJson(entry.getServers()));
        // Only store seasons for series to reduce footprint
        if (mainCategory != null && (mainCategory.toLowerCase().contains("series") || mainCategory.toLowerCase().contains("tv"))) {
            entity.setSeasonsJson(gson.toJson(entry.getSeasons()));
        } else {
            entity.setSeasonsJson(null);
        }
        entity.setRelatedJson(gson.toJson(entry.getRelated()));
        
        return entity;
    }
    
    /**
     * Convert EntryEntity database entity to Entry API model
     */
    public static Entry entityToEntry(EntryEntity entity) {
        Entry entry = new Entry();
        
        // Use proper setter methods
        entry.setTitle(entity.getTitle());
        entry.setSubCategory(entity.getSubCategory());
        entry.setMainCategory(entity.getMainCategory());
        entry.setCountry(entity.getCountry());
        entry.setDescription(entity.getDescription());
        entry.setPoster(entity.getPoster());
        entry.setThumbnail(entity.getThumbnail());
        entry.setRating(entity.getRating());
        entry.setDuration(entity.getDuration());
        entry.setYear(entity.getYear());
        
        // Convert JSON strings back to objects
        try {
            Type serverListType = new TypeToken<List<Server>>(){}.getType();
            List<Server> servers = gson.fromJson(entity.getServersJson(), serverListType);
            entry.setServers(servers);
            
            Type seasonListType = new TypeToken<List<Season>>(){}.getType();
            List<Season> seasons = gson.fromJson(entity.getSeasonsJson(), seasonListType);
            entry.setSeasons(seasons);
            
            Type entryListType = new TypeToken<List<Entry>>(){}.getType();
            List<Entry> related = gson.fromJson(entity.getRelatedJson(), entryListType);
            entry.setRelated(related);
        } catch (Exception e) {
            // Handle JSON parsing errors gracefully
            entry.setServers(new ArrayList<>());
            entry.setSeasons(new ArrayList<>());
            entry.setRelated(new ArrayList<>());
        }
        
        return entry;
    }
    
    /**
     * Convert lightweight projection to Entry (no heavy JSON fields)
     */
    public static Entry lightToEntry(EntryLight light) {
        Entry entry = new Entry();
        entry.setTitle(light.getTitle());
        entry.setSubCategory(light.getSubCategory());
        entry.setMainCategory(light.getMainCategory());
        entry.setCountry(light.getCountry());
        entry.setDescription(light.getDescription());
        entry.setPoster(light.getPoster());
        entry.setThumbnail(light.getThumbnail());
        entry.setRating(light.getRating());
        entry.setDuration(light.getDuration());
        entry.setYear(light.getYear());
        return entry;
    }
    
    /**
     * Convert list of EntryEntity to list of Entry
     */
    public static List<Entry> entitiesToEntries(List<EntryEntity> entities) {
        List<Entry> entries = new ArrayList<>();
        for (EntryEntity entity : entities) {
            entries.add(entityToEntry(entity));
        }
        return entries;
    }

    /**
     * Convert list of EntryLight to list of Entry
     */
    public static List<Entry> lightsToEntries(List<EntryLight> lights) {
        List<Entry> entries = new ArrayList<>();
        for (EntryLight l : lights) {
            entries.add(lightToEntry(l));
        }
        return entries;
    }
}