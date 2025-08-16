package com.cinecraze.free.tv;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.tvprovider.media.tv.PreviewProgram;
import androidx.tvprovider.media.tv.TvContractCompat;

import com.cinecraze.free.models.Entry;

public final class WatchNextHelper {
    private WatchNextHelper() {}

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Uri publishToWatchNext(Context context, Entry entry) {
        if (context == null || entry == null) return null;
        PreviewProgram program = new PreviewProgram.Builder()
                .setChannelId(0)
                .setType(TvContractCompat.PreviewProgramColumns.TYPE_CLIP)
                .setTitle(entry.getTitle())
                .setDescription(entry.getDescription())
                .setPosterArtUri(Uri.parse(entry.getImageUrl()))
                .setIntentUri(Uri.parse("cinecraze://details?id=" + entry.getId()))
                .build();

        ContentResolver resolver = context.getContentResolver();
        return resolver.insert(TvContractCompat.WatchNextPrograms.CONTENT_URI, program.toContentValues());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void markAsPlaying(Context context, Uri programUri) {
        if (context == null || programUri == null) return;
        try {
            // Attempt to fetch constants via reflection for tvprovider 1.0.0 compatibility
            Class<?> cls = TvContractCompat.WatchNextPrograms.class;
            int continueVal;
            try {
                java.lang.reflect.Field fType = cls.getField("WATCH_NEXT_TYPE_CONTINUE");
                continueVal = fType.getInt(null);
            } catch (Throwable t) {
                // Fallback to 1 (CONTINUE) if constant not found
                continueVal = 1;
            }

            String colName = "watch_next_type"; // fallback column name
            try {
                java.lang.reflect.Field fCol = cls.getField("COLUMN_WATCH_NEXT_TYPE");
                Object v = fCol.get(null);
                if (v instanceof String) colName = (String) v;
            } catch (Throwable ignored) {}

            ContentValues values = new ContentValues();
            values.put(colName, continueVal);
            context.getContentResolver().update(programUri, values, null, null);
        } catch (Throwable ignored) {}
    }
}