package com.cinecraze.free.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ContinueWatchingStore {
    private static final String PREFS = "continue_watching";
    private static final String KEY_ITEMS = "items"; // JSON object keyed by entryId

    private ContinueWatchingStore() {}

    public static void saveProgress(Context context, int entryId, String title, String imageUrl, long positionMs) {
        if (context == null || entryId == 0) return;
        try {
            SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String json = sp.getString(KEY_ITEMS, "{}");
            JSONObject root = new JSONObject(json);
            JSONObject item = new JSONObject();
            item.put("id", entryId);
            item.put("title", title != null ? title : "");
            item.put("image", imageUrl != null ? imageUrl : "");
            item.put("position", positionMs);
            item.put("updatedAt", System.currentTimeMillis());
            root.put(String.valueOf(entryId), item);
            sp.edit().putString(KEY_ITEMS, root.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public static Progress getProgress(Context context, int entryId) {
        if (context == null || entryId == 0) return null;
        try {
            SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String json = sp.getString(KEY_ITEMS, "{}");
            JSONObject root = new JSONObject(json);
            JSONObject item = root.optJSONObject(String.valueOf(entryId));
            if (item == null) return null;
            Progress p = new Progress();
            p.entryId = entryId;
            p.title = item.optString("title", "");
            p.imageUrl = item.optString("image", "");
            p.positionMs = item.optLong("position", 0);
            p.updatedAt = item.optLong("updatedAt", 0);
            return p;
        } catch (JSONException e) {
            return null;
        }
    }

    public static List<Progress> getRecent(Context context, int limit) {
        List<Progress> result = new ArrayList<>();
        if (context == null) return result;
        try {
            SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String json = sp.getString(KEY_ITEMS, "{}");
            JSONObject root = new JSONObject(json);
            // Collect and sort by updatedAt desc (simple selection sort for small N)
            List<Progress> all = new ArrayList<>();
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject item = root.optJSONObject(key);
                if (item == null) continue;
                Progress p = new Progress();
                p.entryId = item.optInt("id", 0);
                p.title = item.optString("title", "");
                p.imageUrl = item.optString("image", "");
                p.positionMs = item.optLong("position", 0);
                p.updatedAt = item.optLong("updatedAt", 0);
                all.add(p);
            }
            all.sort((a, b) -> Long.compare(b.updatedAt, a.updatedAt));
            for (int i = 0; i < all.size() && i < limit; i++) {
                result.add(all.get(i));
            }
        } catch (JSONException ignored) {
        }
        return result;
    }

    public static class Progress {
        public int entryId;
        public String title;
        public String imageUrl;
        public long positionMs;
        public long updatedAt;
    }
}