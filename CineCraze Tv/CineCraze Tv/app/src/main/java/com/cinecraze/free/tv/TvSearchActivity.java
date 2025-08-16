package com.cinecraze.free.tv;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import com.cinecraze.free.R;

public class TvSearchActivity extends FragmentActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tv_search);
        if (savedInstanceState == null) {
            TvSearchFragment fragment = new TvSearchFragment();
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.tv_search_container, fragment)
                .commitNow();
        }
    }
}