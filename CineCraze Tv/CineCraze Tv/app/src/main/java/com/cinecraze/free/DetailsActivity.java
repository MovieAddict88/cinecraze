package com.cinecraze.free;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cinecraze.free.models.Entry;
import com.google.gson.Gson;

/**
 * Minimal DetailsActivity placeholder to satisfy module references.
 * This can be expanded to show full details UI. For now, it simply
 * accepts an Entry JSON extra and exists so other components can start it.
 */
public class DetailsActivity extends AppCompatActivity {

    private static final String TAG = "DetailsActivity";

    public static void start(Context context, Entry entry) {
        Intent intent = new Intent(context, DetailsActivity.class);
        if (entry != null) {
            intent.putExtra("entry", new Gson().toJson(entry));
        }
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Intentionally no setContentView to avoid layout dependency; this
        // class primarily exists to be a navigation target and to unblock build.
        try {
            String json = getIntent().getStringExtra("entry");
            if (json != null && !json.isEmpty()) {
                Entry entry = new Gson().fromJson(json, Entry.class);
                Log.d(TAG, "Opened with entry: " + (entry != null ? entry.getTitle() : "null"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing entry from intent", e);
        }
    }
}
