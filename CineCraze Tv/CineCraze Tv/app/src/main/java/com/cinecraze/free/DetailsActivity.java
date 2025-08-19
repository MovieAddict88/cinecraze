package com.cinecraze.free;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cinecraze.free.models.Entry;
import com.google.gson.Gson;
import com.bumptech.glide.Glide;

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
        setContentView(R.layout.activity_details);

        TextView titleView = findViewById(R.id.text_view_activity_movie_title);
        TextView descView = findViewById(R.id.text_view_activity_movie_description);
        TextView yearView = findViewById(R.id.text_view_activity_movie_year);
        TextView durationView = findViewById(R.id.text_view_activity_movie_duration);
        RatingBar ratingBar = findViewById(R.id.rating_bar_activity_movie_rating);
        ImageView cover = findViewById(R.id.image_view_activity_movie_cover);
        ImageView bg = findViewById(R.id.image_view_activity_movie_background);

        try {
            String json = getIntent().getStringExtra("entry");
            if (json != null && !json.isEmpty()) {
                Entry entry = new Gson().fromJson(json, Entry.class);
                if (entry != null) {
                    if (titleView != null) titleView.setText(entry.getTitle());
                    if (descView != null) descView.setText(entry.getDescription());
                    if (yearView != null) yearView.setText(entry.getYearString());
                    if (durationView != null) durationView.setText(entry.getDuration());
                    if (ratingBar != null) ratingBar.setRating(entry.getRating());
                    if (cover != null && entry.getImageUrl() != null) {
                        Glide.with(this).load(entry.getImageUrl()).into(cover);
                    }
                    if (bg != null && entry.getImageUrl() != null) {
                        Glide.with(this).load(entry.getImageUrl()).into(bg);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing entry from intent", e);
        }
    }
}