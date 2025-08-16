package com.cinecraze.free.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.cinecraze.free.database.dao.EntryDao;
import com.cinecraze.free.database.dao.CacheMetadataDao;
import com.cinecraze.free.database.entities.EntryEntity;
import com.cinecraze.free.database.entities.CacheMetadataEntity;

@Database(
    entities = {EntryEntity.class, CacheMetadataEntity.class, com.cinecraze.free.database.entities.DownloadItemEntity.class},
    version = 2,
    exportSchema = false
)
public abstract class CineCrazeDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "cinecraze_database";
    private static CineCrazeDatabase instance;
    
    public abstract EntryDao entryDao();
    public abstract CacheMetadataDao cacheMetadataDao();
    public abstract com.cinecraze.free.database.dao.DownloadItemDao downloadItemDao();
    
    public static synchronized CineCrazeDatabase getInstance(Context context) {
        if (instance == null) {
            // Create a prepackaged copy of installed DB into internal files dir for Room
            java.io.File installed = context.getDatabasePath(DATABASE_NAME);
            java.io.File prepack = new java.io.File(context.getFilesDir(), com.cinecraze.free.remote.RemoteDbConfig.PREPACK_FILE_NAME);
            try {
                if (installed.exists()) {
                    prepack.getParentFile().mkdirs();
                    try (java.io.InputStream in = new java.io.FileInputStream(installed);
                         java.io.OutputStream out = new java.io.FileOutputStream(prepack)) {
                        byte[] buf = new byte[8192]; int len;
                        while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
                    }
                }
            } catch (Exception ignored) {}

            RoomDatabase.Builder<CineCrazeDatabase> builder = Room.databaseBuilder(
                context.getApplicationContext(),
                CineCrazeDatabase.class,
                DATABASE_NAME
            );

            if (prepack.exists()) {
                builder.createFromFile(prepack);
            }

            instance = builder
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries() // For simplicity, but ideally use background threads
                .build();
        }
        return instance;
    }
}