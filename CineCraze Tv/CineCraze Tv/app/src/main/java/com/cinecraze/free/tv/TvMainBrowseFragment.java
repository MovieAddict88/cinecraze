package com.cinecraze.free.tv;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.cinecraze.free.R;
import com.cinecraze.free.models.Entry;
import com.cinecraze.free.repository.DataRepository;
import com.cinecraze.free.utils.ContinueWatchingStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TvMainBrowseFragment extends BrowseSupportFragment {

    private DataRepository dataRepository;
    private ArrayObjectAdapter rowsAdapter;

    private BackgroundManager backgroundManager;
    private Handler backgroundHandler;
    private Runnable backgroundRunnable;

    // Pagination state per category
    private static class CategoryRowState {
        String category;
        int page = 0;
        boolean hasMore = false;
        int totalCount = 0;
        ArrayObjectAdapter cardsAdapter;
        ListRow row;
        HeaderItem header;
        CategoryRowState(Context ctx, String category, String title) {
            this.category = category;
            ClassPresenterSelector selector = new ClassPresenterSelector();
            selector.addClassPresenter(Entry.class, new PosterCardPresenter());
            selector.addClassPresenter(LoadMoreItem.class, new LoadMorePresenter());
            this.cardsAdapter = new ArrayObjectAdapter(selector);
            this.header = new HeaderItem(title);
            this.row = new ListRow(header, cardsAdapter);
        }
    }
    private final Map<String, CategoryRowState> categoryStateMap = new HashMap<>();

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setTitle("CineCraze");
        dataRepository = new DataRepository(requireContext());

        backgroundManager = BackgroundManager.getInstance(requireActivity());
        backgroundManager.attach(requireActivity().getWindow());
        Drawable defaultBg = ContextCompat.getDrawable(requireContext(), android.R.color.black);
        backgroundManager.setDrawable(defaultBg);
        backgroundHandler = new Handler(Looper.getMainLooper());

        rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(rowsAdapter);

        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
                if (item instanceof Entry) {
                    android.content.Intent i = new android.content.Intent(requireContext(), com.cinecraze.free.tv.TvDetailsActivity.class);
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    i.putExtra("entry", gson.toJson((Entry) item));
                    startActivity(i);
                } else if (item instanceof ContinueItem) {
                    ContinueItem ci = (ContinueItem) item;
                    Entry e = dataRepository.findEntryByHashId(ci.entryId);
                    if (e != null) {
                        android.content.Intent i = new android.content.Intent(requireContext(), com.cinecraze.free.tv.TvDetailsActivity.class);
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        i.putExtra("entry", gson.toJson(e));
                        i.putExtra("resume_position_ms", ci.positionMs);
                        startActivity(i);
                    }
                } else if (item instanceof LoadMoreItem) {
                    LoadMoreItem more = (LoadMoreItem) item;
                    loadNextCategoryPage(more.category);
                }
            }
        });

        setOnItemViewSelectedListener(new OnItemViewSelectedListener() {
            @Override
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
                if (item instanceof Entry) {
                    scheduleBackgroundUpdate(((Entry) item).getImageUrl());
                } else if (item instanceof ContinueItem) {
                    scheduleBackgroundUpdate(((ContinueItem) item).imageUrl);
                }
            }
        });

        buildRows();

        if (rowsAdapter.size() == 0) {
            dataRepository.ensureDataAvailable(new DataRepository.DataCallback() {
                @Override
                public void onSuccess(List<Entry> entries) {
                    rowsAdapter.clear();
                    buildRows();
                }

                @Override
                public void onError(String error) {
                }
            });
        }

        setBadgeDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_search));
        setOnSearchClickedListener(v -> {
            android.content.Intent i = new android.content.Intent(requireContext(), TvSearchActivity.class);
            startActivity(i);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshContinueWatchingRow();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (backgroundHandler != null && backgroundRunnable != null) {
            backgroundHandler.removeCallbacks(backgroundRunnable);
        }
    }

    private void scheduleBackgroundUpdate(String imageUrl) {
        if (backgroundHandler == null) return;
        if (backgroundRunnable != null) {
            backgroundHandler.removeCallbacks(backgroundRunnable);
        }
        backgroundRunnable = () -> loadBackground(imageUrl);
        backgroundHandler.postDelayed(backgroundRunnable, 250);
    }

    private void loadBackground(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return;
        Glide.with(this)
            .load(imageUrl)
            .into(new CustomTarget<Drawable>() {
                @Override
                public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                    backgroundManager.setDrawable(resource);
                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {
                }
            });
    }

    private void buildRows() {
        addContinueWatchingRow();
        addRowStatic(getString(R.string.recently_added), dataRepository.getRecentlyAdded(30));

        // Paginated rows for categories
        createCategoryRow(getString(R.string.movies), "Movies");
        createCategoryRow(getString(R.string.tv_shows), "TV Shows");
        createCategoryRow(getString(R.string.live), "Live");
    }

    private void createCategoryRow(String title, String category) {
        CategoryRowState state = new CategoryRowState(requireContext(), category, title);
        categoryStateMap.put(category, state);
        rowsAdapter.add(state.row);
        // load first page
        loadCategoryPage(category, 0);
    }

    private void loadCategoryPage(String category, int page) {
        final int pageSize = 20;
        dataRepository.getPaginatedDataByCategory(category, page, pageSize, new DataRepository.PaginatedDataCallback() {
            @Override
            public void onSuccess(List<Entry> entries, boolean hasMorePages, int totalCount) {
                CategoryRowState state = categoryStateMap.get(category);
                if (state == null) return;
                // remove existing load more if present
                removeLoadMoreIfPresent(state);
                for (Entry e : entries) {
                    state.cardsAdapter.add(e);
                }
                state.page = page;
                state.hasMore = hasMorePages;
                state.totalCount = totalCount;
                if (hasMorePages) {
                    state.cardsAdapter.add(new LoadMoreItem(category));
                }
            }

            @Override
            public void onError(String error) {
                // no-op for TV home
            }
        });
    }

    private void loadNextCategoryPage(String category) {
        CategoryRowState state = categoryStateMap.get(category);
        if (state == null) return;
        int nextPage = state.page + 1;
        loadCategoryPage(category, nextPage);
    }

    private void removeLoadMoreIfPresent(CategoryRowState state) {
        for (int i = state.cardsAdapter.size() - 1; i >= 0; i--) {
            Object obj = state.cardsAdapter.get(i);
            if (obj instanceof LoadMoreItem) {
                state.cardsAdapter.removeItems(i, 1);
                break;
            }
        }
    }

    private void addRowStatic(String title, List<Entry> entries) {
        if (entries == null || entries.isEmpty()) return;
        ArrayObjectAdapter cardRowAdapter = new ArrayObjectAdapter(new PosterCardPresenter());
        for (Entry e : entries) cardRowAdapter.add(e);
        rowsAdapter.add(new ListRow(new HeaderItem(title), cardRowAdapter));
    }

    private void addContinueWatchingRow() {
        List<ContinueWatchingStore.Progress> recent = ContinueWatchingStore.getRecent(requireContext(), 15);
        if (recent.isEmpty()) return;
        ArrayObjectAdapter cardRowAdapter = new ArrayObjectAdapter(new ContinueCardPresenter());
        for (ContinueWatchingStore.Progress p : recent) {
            cardRowAdapter.add(new ContinueItem(p.entryId, p.title, p.imageUrl, p.positionMs));
        }
        rowsAdapter.add(0, new ListRow(new HeaderItem(getString(R.string.continue_watching)), cardRowAdapter));
    }

    private void refreshContinueWatchingRow() {
        for (int i = 0; i < rowsAdapter.size(); i++) {
            Object row = rowsAdapter.get(i);
            if (row instanceof ListRow) {
                HeaderItem header = ((ListRow) row).getHeaderItem();
                if (header != null && getString(R.string.continue_watching).contentEquals(header.getName())) {
                    rowsAdapter.removeItems(i, 1);
                    break;
                }
            }
        }
        addContinueWatchingRow();
    }

    public static class PosterCardPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent) {
            ImageCardView cardView = new ImageCardView(parent.getContext());
            cardView.setFocusable(true);
            cardView.setFocusableInTouchMode(true);
            int w = (int) parent.getResources().getDimension(R.dimen.tv_card_width);
            int h = (int) parent.getResources().getDimension(R.dimen.tv_card_height);
            cardView.setMainImageDimensions(w, h);
            cardView.setOnFocusChangeListener((v, hasFocus) -> {
                v.animate().scaleX(hasFocus ? 1.06f : 1.0f).scaleY(hasFocus ? 1.06f : 1.0f).setDuration(120).start();
            });
            return new ViewHolder(cardView);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            Entry entry = (Entry) item;
            ImageCardView cardView = (ImageCardView) viewHolder.view;
            cardView.setTitleText(entry.getTitle());
            cardView.setContentText(entry.getYearString());
            Glide.with(cardView.getContext())
                    .load(entry.getImageUrl())
                    .centerCrop()
                    .into(cardView.getMainImageView());
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
            ImageCardView cv = (ImageCardView) viewHolder.view;
            cv.setMainImage(null);
        }
    }

    private class ContinueCardPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent) {
            ImageCardView cardView = new ImageCardView(parent.getContext());
            cardView.setFocusable(true);
            cardView.setFocusableInTouchMode(true);
            int w = (int) parent.getResources().getDimension(R.dimen.tv_card_width);
            int h = (int) parent.getResources().getDimension(R.dimen.tv_card_height);
            cardView.setMainImageDimensions(w, h);
            cardView.setOnFocusChangeListener((v, hasFocus) -> {
                v.animate().scaleX(hasFocus ? 1.06f : 1.0f).scaleY(hasFocus ? 1.06f : 1.0f).setDuration(120).start();
            });
            return new ViewHolder(cardView);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            ContinueItem p = (ContinueItem) item;
            ImageCardView cardView = (ImageCardView) viewHolder.view;
            cardView.setTitleText(p.title);
            cardView.setContentText(formatTime(p.positionMs));
            Glide.with(cardView.getContext())
                    .load(p.imageUrl)
                    .centerCrop()
                    .into(cardView.getMainImageView());
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
            ImageCardView cv = (ImageCardView) viewHolder.view;
            cv.setMainImage(null);
        }

        private String formatTime(long ms) {
            long totalSec = ms / 1000;
            long minutes = totalSec / 60;
            long seconds = totalSec % 60;
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    private static class ContinueItem {
        final int entryId;
        final String title;
        final String imageUrl;
        final long positionMs;
        ContinueItem(int entryId, String title, String imageUrl, long positionMs) {
            this.entryId = entryId;
            this.title = title;
            this.imageUrl = imageUrl;
            this.positionMs = positionMs;
        }
    }

    private static class LoadMoreItem {
        final String category;
        LoadMoreItem(String category) { this.category = category; }
    }

    private static class LoadMorePresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent) {
            android.widget.LinearLayout container = new android.widget.LinearLayout(parent.getContext());
            container.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            container.setGravity(android.view.Gravity.CENTER_VERTICAL);
            container.setFocusable(true);
            container.setFocusableInTouchMode(true);
            int padH = (int) (parent.getResources().getDisplayMetrics().density * 16);
            int padV = (int) (parent.getResources().getDisplayMetrics().density * 12);
            container.setPadding(padH, padV, padH, padV);

            android.widget.ImageView icon = new android.widget.ImageView(parent.getContext());
            icon.setImageResource(R.drawable.ic_more);
            android.widget.LinearLayout.LayoutParams ip = new android.widget.LinearLayout.LayoutParams((int)(24*parent.getResources().getDisplayMetrics().density), (int)(24*parent.getResources().getDisplayMetrics().density));
            ip.rightMargin = (int)(8*parent.getResources().getDisplayMetrics().density);
            container.addView(icon, ip);

            android.widget.TextView tv = new android.widget.TextView(parent.getContext());
            tv.setTextSize(16f);
            tv.setTextColor(android.graphics.Color.WHITE);
            tv.setText(parent.getContext().getString(R.string.load_more));
            container.addView(tv);

            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(android.graphics.Color.parseColor("#333333"));
            bg.setCornerRadius(padV);
            container.setBackground(bg);

            container.setOnFocusChangeListener((v, hasFocus) -> v.animate().scaleX(hasFocus?1.06f:1f).scaleY(hasFocus?1.06f:1f).setDuration(120).start());

            return new ViewHolder(container);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {}

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {}
    }
}