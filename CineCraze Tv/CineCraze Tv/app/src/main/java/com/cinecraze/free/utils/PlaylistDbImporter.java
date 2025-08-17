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
import com.cinecraze.free.models.Season;
import com.cinecraze.free.models.Episode;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaylistDbImporter {
	private static final String TAG = "PlaylistDbImporter";
	private static final String CACHE_KEY_PLAYLIST = "playlist_data";
	private static final Gson gson = new Gson();

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
			cursor = sqlite.rawQuery("SELECT id, title, sub_category, country, description, poster, thumbnail, rating, duration, year, main_category, servers_json, seasons_json, related_json FROM entries", null);

			// Aggregation structures
			Map<String, SeriesAggregator> seriesMap = new HashMap<>();
			Map<String, EntryEntity> nonSeriesBest = new HashMap<>();

			if (cursor.moveToFirst()) {
				do {
					// Read raw fields
					String title = getString(cursor, "title");
					String subCategory = getString(cursor, "sub_category");
					String country = getString(cursor, "country");
					String description = getString(cursor, "description");
					String poster = getString(cursor, "poster");
					String thumbnail = getString(cursor, "thumbnail");
					String rating = getString(cursor, "rating");
					String duration = getString(cursor, "duration");
					String year = getString(cursor, "year");
					String originalCategory = getString(cursor, "main_category");
					String serversJson = getString(cursor, "servers_json");
					String seasonsJson = getString(cursor, "seasons_json");
					String relatedJson = getString(cursor, "related_json");

					String normalizedCategory = normalizeCategory(originalCategory, seasonsJson);
					boolean isSeries = "TV Series".equals(normalizedCategory);

					if (isSeries) {
						String key = baseTitle(title);
						SeriesAggregator agg = seriesMap.get(key);
						if (agg == null) {
							agg = new SeriesAggregator();
							agg.title = key != null && !key.isEmpty() ? key : title;
							agg.subCategory = subCategory;
							agg.country = country;
							agg.description = description;
							agg.poster = poster;
							agg.thumbnail = thumbnail;
							agg.rating = rating;
							agg.duration = duration;
							agg.year = year;
							agg.relatedJson = relatedJson;
							seriesMap.put(key, agg);
						} else {
							// Prefer non-empty poster/servers
							if (isBetter(poster, serversJson, agg.poster, null)) {
								agg.poster = poster;
							}
							if (agg.thumbnail == null || (thumbnail != null && !thumbnail.isEmpty())) {
								agg.thumbnail = thumbnail;
							}
							// Keep earliest year if missing
							if (agg.year == null || agg.year.isEmpty()) agg.year = year;
							if (agg.description == null || agg.description.isEmpty()) agg.description = description;
						}

						// Merge season/episodes from this row
						List<Season> seasons = parseSeasons(seasonsJson);
						if (seasons != null && !seasons.isEmpty()) {
							for (Season s : seasons) {
								if (s == null) continue;
								int seasonNum = s.getSeason();
								List<Episode> eps = s.getEpisodes();
								if (eps == null || eps.isEmpty()) continue;
								agg.mergeSeason(seasonNum, eps);
							}
						} else {
							// Fallback: seasons_json may represent a single episode or an array of episodes
							for (EpisodeWithSeason ews : parseEpisodesFlexible(seasonsJson, title, serversJson)) {
								if (ews == null || ews.episode == null) continue;
								int seasonNum = ews.season > 0 ? ews.season : 1;
								List<Episode> single = new ArrayList<>();
								single.add(ews.episode);
								agg.mergeSeason(seasonNum, single);
							}
						}
					} else {
						// Movies or Live TV: pick the best record per title
						String key = title;
						EntryEntity currentBest = nonSeriesBest.get(key);
						if (currentBest == null || isBetter(poster, serversJson, currentBest.getPoster(), currentBest.getServersJson())) {
							EntryEntity e = new EntryEntity();
							e.setTitle(title);
							e.setSubCategory(subCategory);
							e.setCountry(country);
							e.setDescription(description);
							e.setPoster(poster);
							e.setThumbnail(thumbnail);
							e.setRating(rating);
							e.setDuration(duration);
							e.setYear(year);
							e.setMainCategory(normalizedCategory);
							e.setServersJson(serversJson != null ? serversJson : "[]");
							e.setSeasonsJson("[]");
							e.setRelatedJson(relatedJson != null ? relatedJson : "[]");
							nonSeriesBest.put(key, e);
						}
					}
				} while (cursor.moveToNext());
			}

			// Build final list
			List<EntryEntity> entities = new ArrayList<>();
			// Non-series entries first
			entities.addAll(nonSeriesBest.values());
			// Series entries from aggregators
			for (SeriesAggregator agg : seriesMap.values()) {
				EntryEntity e = new EntryEntity();
				e.setTitle(agg.title);
				e.setSubCategory(agg.subCategory);
				e.setCountry(agg.country);
				e.setDescription(agg.description);
				e.setPoster(agg.poster);
				e.setThumbnail(agg.thumbnail);
				e.setRating(agg.rating);
				e.setDuration(agg.duration);
				e.setYear(agg.year);
				e.setMainCategory("TV Series");
				e.setServersJson("[]");
				e.setSeasonsJson(agg.toSeasonsJson());
				e.setRelatedJson(agg.relatedJson != null ? agg.relatedJson : "[]");
				entities.add(e);
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
			Log.i(TAG, "Imported " + entities.size() + " entries from playlist.db into Room (merged by title)");
		} finally {
			if (cursor != null) cursor.close();
			if (sqlite != null) sqlite.close();
		}
	}

	private static String getString(Cursor c, String col) {
		int idx = c.getColumnIndex(col);
		return idx >= 0 ? c.getString(idx) : null;
	}

	private static boolean isBetter(String posterA, String serversJsonA, String posterB, String serversJsonB) {
		int scoreA = (posterA != null && !posterA.isEmpty() ? 1 : 0) + (serversJsonA != null && serversJsonA.length() > 2 ? 1 : 0);
		int scoreB = (posterB != null && !posterB.isEmpty() ? 1 : 0) + (serversJsonB != null && serversJsonB.length() > 2 ? 1 : 0);
		return scoreA >= scoreB;
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

	private static String baseTitle(String title) {
		if (title == null) return "";
		String t = title;
		// Remove common episode suffix patterns
		t = t.replaceAll("(?i)\\s*[-:–]\\s*episode\\s*\\d+.*$", "");
		t = t.replaceAll("(?i)\\s*S\\d{1,2}E\\d{1,2}.*$", "");
		t = t.replaceAll("(?i)\\s*ep\\s*\\d+.*$", "");
		return t.trim();
	}

	private static List<Season> parseSeasons(String seasonsJson) {
		if (!hasText(seasonsJson)) return new ArrayList<>();
		try {
			Type listType = new TypeToken<List<Season>>(){}.getType();
			List<Season> seasons = gson.fromJson(seasonsJson, listType);
			return seasons != null ? seasons : new ArrayList<>();
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	// Fallback parsing: support episode-shaped JSON or arrays of episodes
	private static List<EpisodeWithSeason> parseEpisodesFlexible(String seasonsJson, String title, String serversJson) {
		List<EpisodeWithSeason> result = new ArrayList<>();
		if (!hasText(seasonsJson)) {
			// Try to create an episode from title + servers if it clearly looks like SxxExx
			EpisodeWithSeason fromTitle = episodeFromTitleAndServers(title, serversJson);
			if (fromTitle != null) result.add(fromTitle);
			return result;
		}
		try {
			com.google.gson.JsonElement root = com.google.gson.JsonParser.parseString(seasonsJson);
			if (root.isJsonObject()) {
				EpisodeWithSeason ews = episodeFromJsonObject(root.getAsJsonObject());
				if (ews != null) result.add(ews);
			} else if (root.isJsonArray()) {
				com.google.gson.JsonArray arr = root.getAsJsonArray();
				for (com.google.gson.JsonElement el : arr) {
					if (el.isJsonObject()) {
						com.google.gson.JsonObject obj = el.getAsJsonObject();
						// If looks like a Season object with Episodes array, expand episodes
						if (obj.has("Episodes") || obj.has("episodes")) {
							try {
								// Convert to Season then expand
								Season season = gson.fromJson(obj, Season.class);
								int sNum = season != null ? season.getSeason() : 1;
								List<Episode> eps = season != null ? season.getEpisodes() : null;
								if (eps != null) {
									for (Episode ep : eps) {
										EpisodeWithSeason ews = new EpisodeWithSeason();
										ews.season = sNum > 0 ? sNum : 1;
										ews.episode = ep;
										result.add(ews);
									}
								}
							} catch (Exception ignored) {}
						} else {
							EpisodeWithSeason ews = episodeFromJsonObject(obj);
							if (ews != null) result.add(ews);
						}
					}
				}
			}
		} catch (Exception ignored) { }
		// As a last resort, derive from title
		if (result.isEmpty()) {
			EpisodeWithSeason fromTitle = episodeFromTitleAndServers(title, serversJson);
			if (fromTitle != null) result.add(fromTitle);
		}
		return result;
	}

	private static EpisodeWithSeason episodeFromJsonObject(com.google.gson.JsonObject obj) {
		try {
			Episode ep = new Episode();
			int seasonNum = 1;
			if (obj.has("season")) seasonNum = safeInt(obj.get("season"));
			else if (obj.has("Season")) seasonNum = safeInt(obj.get("Season"));
			if (obj.has("episode")) ep.setEpisode(safeIntOrString(obj.get("episode")));
			else if (obj.has("Episode")) ep.setEpisode(safeIntOrString(obj.get("Episode")));
			if (obj.has("title")) ep.setTitle(safeString(obj.get("title")));
			else if (obj.has("Title")) ep.setTitle(safeString(obj.get("Title")));
			if (obj.has("duration")) ep.setDuration(safeString(obj.get("duration")));
			else if (obj.has("Duration")) ep.setDuration(safeString(obj.get("Duration")));
			if (obj.has("description")) ep.setDescription(safeString(obj.get("description")));
			else if (obj.has("Description")) ep.setDescription(safeString(obj.get("Description")));
			if (obj.has("thumbnail")) ep.setThumbnail(safeString(obj.get("thumbnail")));
			else if (obj.has("Thumbnail")) ep.setThumbnail(safeString(obj.get("Thumbnail")));
			if (obj.has("servers") || obj.has("Servers")) {
				com.google.gson.JsonElement serversEl = obj.has("servers") ? obj.get("servers") : obj.get("Servers");
				try {
					java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<com.cinecraze.free.models.Server>>(){}.getType();
					List<com.cinecraze.free.models.Server> servers = gson.fromJson(serversEl, listType);
					ep.setServers(servers);
				} catch (Exception ignored) {}
			}
			EpisodeWithSeason ews = new EpisodeWithSeason();
			ews.season = seasonNum > 0 ? seasonNum : 1;
			ews.episode = ep;
			return ews;
		} catch (Exception e) {
			return null;
		}
	}

	private static EpisodeWithSeason episodeFromTitleAndServers(String title, String serversJson) {
		if (title == null) return null;
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i).*S(\\d{1,2})E(\\d{1,2}).*").matcher(title);
		if (!m.matches()) return null;
		int season = 1;
		int epNum = 0;
		try { season = Integer.parseInt(m.group(1)); } catch (Exception ignored) {}
		try { epNum = Integer.parseInt(m.group(2)); } catch (Exception ignored) {}
		Episode ep = new Episode();
		ep.setEpisode(epNum);
		ep.setTitle(title);
		// Try to attach servers from row-level servers_json if available
		if (hasText(serversJson) && !"[]".equals(serversJson.trim())) {
			try {
				java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<com.cinecraze.free.models.Server>>(){}.getType();
				List<com.cinecraze.free.models.Server> servers = gson.fromJson(serversJson, listType);
				ep.setServers(servers);
			} catch (Exception ignored) {}
		}
		EpisodeWithSeason ews = new EpisodeWithSeason();
		ews.season = season;
		ews.episode = ep;
		return ews;
	}

	private static int safeInt(com.google.gson.JsonElement el) {
		try { return el.getAsInt(); } catch (Exception ignored) { try { return Integer.parseInt(el.getAsString()); } catch (Exception e) { return 0; } }
	}
	private static Object safeIntOrString(com.google.gson.JsonElement el) {
		try { return el.getAsInt(); } catch (Exception ignored) { try { return el.getAsString(); } catch (Exception e) { return 0; } }
	}
	private static String safeString(com.google.gson.JsonElement el) {
		try { return el.isJsonNull() ? null : el.getAsString(); } catch (Exception e) { return null; }
	}

	private static class SeriesAggregator {
		String title;
		String subCategory;
		String country;
		String description;
		String poster;
		String thumbnail;
		String rating;
		String duration;
		String year;
		String relatedJson;
		Map<Integer, List<Episode>> seasonToEpisodes = new HashMap<>();

		void mergeSeason(int seasonNum, List<Episode> episodes) {
			if (seasonNum <= 0) seasonNum = 1;
			List<Episode> list = seasonToEpisodes.get(seasonNum);
			if (list == null) {
				list = new ArrayList<>();
				seasonToEpisodes.put(seasonNum, list);
			}
			list.addAll(episodes);
		}

		String toSeasonsJson() {
			List<Season> seasons = new ArrayList<>();
			for (Map.Entry<Integer, List<Episode>> e : seasonToEpisodes.entrySet()) {
				Season s = new Season();
				// Season model has private fields; Gson will still serialize via reflection
				try {
					java.lang.reflect.Field fSeason = Season.class.getDeclaredField("season");
					fSeason.setAccessible(true);
					fSeason.setInt(s, e.getKey());
					java.lang.reflect.Field fEpisodes = Season.class.getDeclaredField("episodes");
					fEpisodes.setAccessible(true);
					fEpisodes.set(s, e.getValue());
				} catch (Exception ignored) {}
				seasons.add(s);
			}
			return gson.toJson(seasons);
		}
	}

	private static class EpisodeWithSeason {
		int season;
		Episode episode;
	}
}