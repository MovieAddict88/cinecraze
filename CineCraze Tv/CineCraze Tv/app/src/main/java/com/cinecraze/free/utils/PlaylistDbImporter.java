package com.cinecraze.free.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.cinecraze.free.database.CineCrazeDatabase;
import com.cinecraze.free.database.dao.EntryDao;
import com.cinecraze.free.database.dao.CacheMetadataDao;
import com.cinecraze.free.database.entities.EntryEntity;
import com.cinecraze.free.database.entities.CacheMetadataEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PlaylistDbImporter {
	private static final String TAG = "PlaylistDbImporter";
	private static final String CACHE_KEY_PLAYLIST = "playlist_data";

	public static void importIntoRoom(Context context) throws Exception {
		DatabaseLocationHelper locationHelper = new DatabaseLocationHelper(context);
		File dbFile = locationHelper.getDatabaseFile();
		if (!dbFile.exists() || dbFile.length() == 0) {
			throw new IllegalStateException("playlist.db not found");
		}

		SQLiteDatabase sqlite = null;
		Cursor cursor = null;
		try {
			sqlite = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
			cursor = sqlite.rawQuery("SELECT id, title, sub_category, country, description, poster, thumbnail, rating, duration, year, main_category, servers_json, seasons_json, related_json FROM entries GROUP BY title", null);

			List<EntryEntity> entities = new ArrayList<>();
			if (cursor.moveToFirst()) {
				do {
					EntryEntity e = new EntryEntity();
					// Note: We do not preserve original id; Room will autogenerate.
					e.setTitle(getString(cursor, "title"));
					e.setSubCategory(getString(cursor, "sub_category"));
					e.setCountry(getString(cursor, "country"));
					e.setDescription(getString(cursor, "description"));
					e.setPoster(getString(cursor, "poster"));
					e.setThumbnail(getString(cursor, "thumbnail"));
					e.setRating(getString(cursor, "rating"));
					e.setDuration(getString(cursor, "duration"));
					e.setYear(getString(cursor, "year"));
					String originalCategory = getString(cursor, "main_category");
					String seasonsJson = getString(cursor, "seasons_json");
					String normalizedCategory = normalizeCategory(originalCategory, seasonsJson);
					e.setMainCategory(normalizedCategory);
					e.setServersJson(getString(cursor, "servers_json"));
					e.setSeasonsJson(seasonsJson);
					e.setRelatedJson(getString(cursor, "related_json"));
					entities.add(e);
				} while (cursor.moveToNext());
			}

			CineCrazeDatabase room = CineCrazeDatabase.getInstance(context);
			EntryDao entryDao = room.entryDao();
			CacheMetadataDao cacheDao = room.cacheMetadataDao();

			entryDao.deleteAll();
			if (!entities.isEmpty()) {
				entryDao.insertAll(entities);
			}
			CacheMetadataEntity metadata = new CacheMetadataEntity(CACHE_KEY_PLAYLIST, System.currentTimeMillis(), "db");
			cacheDao.insert(metadata);
			Log.i(TAG, "Imported " + entities.size() + " entries from playlist.db into Room");
		} finally {
			if (cursor != null) cursor.close();
			if (sqlite != null) sqlite.close();
		}
	}

	private static String getString(Cursor c, String col) {
		int idx = c.getColumnIndex(col);
		return idx >= 0 ? c.getString(idx) : null;
	}

	private static boolean hasText(String s) { return s != null && s.trim().length() > 0; }

	private static String normalizeCategory(String original, String seasonsJson) {
		String o = original != null ? original.trim().toLowerCase() : "";
		if (o.contains("live")) return "Live TV";
		if (o.equals("movies") || o.equals("movie")) return "Movies";
		if (o.equals("tv shows") || o.equals("tv show") || o.equals("tv series") || o.equals("series") || o.equals("shows") || o.equals("show")) return "TV Series";
		// Fallback: decide based on seasons_json
		if (hasText(seasonsJson) && !"[]".equals(seasonsJson.trim())) return "TV Series";
		return "Movies";
	}
}