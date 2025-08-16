package com.cinecraze.free.tv;

import android.os.Bundle;

import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;

import com.cinecraze.free.models.Entry;
import com.cinecraze.free.repository.DataRepository;

import java.util.List;

public class TvSearchFragment extends SearchSupportFragment implements SearchSupportFragment.SearchResultProvider {

    private ArrayObjectAdapter rowsAdapter;
    private DataRepository repository;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        repository = new DataRepository(requireContext());
        setSearchResultProvider(this);
        setOnItemViewClickedListener((itemViewHolder, item, rowViewHolder, row) -> {
            if (item instanceof Entry) {
                android.content.Intent i = new android.content.Intent(requireContext(), TvDetailsActivity.class);
                i.putExtra("entry", new com.google.gson.Gson().toJson((Entry) item));
                startActivity(i);
            }
        });
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return rowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        return handleQuery(newQuery);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return handleQuery(query);
    }

    private boolean handleQuery(String q) {
        rowsAdapter.clear();
        if (q == null || q.trim().length() < 2) return true;
        List<Entry> results = repository.searchByTitle(q.trim());
        if (results != null && !results.isEmpty()) {
            ArrayObjectAdapter cards = new ArrayObjectAdapter(new TvMainBrowseFragment.PosterCardPresenter());
            for (Entry e : results) cards.add(e);
            rowsAdapter.add(new ListRow(new HeaderItem(getString(com.cinecraze.free.R.string.results)), cards));
        }
        return true;
    }
}