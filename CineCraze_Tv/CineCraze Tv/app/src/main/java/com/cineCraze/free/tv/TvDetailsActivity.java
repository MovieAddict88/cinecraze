package com.cinecraze.free.tv;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.cinecraze.free.R;
import com.cinecraze.free.models.Entry;
import com.cinecraze.free.repository.DataRepository;
import com.google.gson.Gson;

public class TvDetailsActivity extends FragmentActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tv_details);

        if (savedInstanceState == null) {
            // If launched via deep link and no entry JSON is provided, resolve via id parameter
            if (getIntent().getStringExtra("entry") == null && getIntent().getData() != null) {
                Uri data = getIntent().getData();
                String idStr = data.getQueryParameter("id");
                try {
                    int hashId = Integer.parseInt(idStr);
                    Entry e = new DataRepository(this).findEntryByHashId(hashId);
                    if (e != null) {
                        getIntent().putExtra("entry", new Gson().toJson(e));
                    }
                } catch (Exception ignored) {}
            }

            TvDetailsFragment fragment = new TvDetailsFragment();
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.tv_details_container, fragment)
                    .commitNow();
        }
    }
}