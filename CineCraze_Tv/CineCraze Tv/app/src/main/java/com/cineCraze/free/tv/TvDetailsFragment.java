package com.cinecraze.free.tv;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.DetailsSupportFragment;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.DetailsOverviewRow;
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.Presenter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.cinecraze.free.DetailsActivity;
import com.cinecraze.free.R;
import com.cinecraze.free.models.Entry;
import com.cinecraze.free.repository.DataRepository;
import com.cinecraze.free.utils.ContinueWatchingStore;
import com.google.gson.Gson;

import java.util.List;

public class TvDetailsFragment extends DetailsSupportFragment {

    private static final long ACTION_PLAY = 1L;
    private static final long ACTION_RESUME = 2L;

    private ArrayObjectAdapter rowsAdapter;
    private DetailsOverviewRow detailsRow;
    private Entry entry;
    private long resumeMs;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String json = getActivity().getIntent().getStringExtra("entry");
        if (json != null) {
            entry = new Gson().fromJson(json, Entry.class);
        }
        resumeMs = getActivity().getIntent().getLongExtra("resume_position_ms", 0L);
        if (resumeMs == 0 && entry != null) {
            ContinueWatchingStore.Progress p = ContinueWatchingStore.getProgress(requireContext(), entry.getId());
            if (p != null) resumeMs = p.positionMs;
        }

        ClassPresenterSelector selector = new ClassPresenterSelector();
        FullWidthDetailsOverviewRowPresenter dorPresenter = new OverviewPresenter();
        dorPresenter.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.black));

        selector.addClassPresenter(DetailsOverviewRow.class, dorPresenter);
        selector.addClassPresenter(ListRow.class, new ListRowPresenter());
        rowsAdapter = new ArrayObjectAdapter(selector);
        setAdapter(rowsAdapter);

        buildDetails();
        buildRelated();
    }

    private void buildDetails() {
        if (entry == null) return;
        detailsRow = new DetailsOverviewRow(entry.getTitle());
        detailsRow.setImageDrawable(ContextCompat.getDrawable(requireContext(), android.R.color.darker_gray));
        detailsRow.setItem(entry.getDescription());

        Glide.with(this)
                .load(entry.getImageUrl())
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        detailsRow.setImageDrawable(resource);
                        rowsAdapter.notifyArrayItemRangeChanged(0, rowsAdapter.size());
                        BackgroundManager.getInstance(requireActivity()).setDrawable(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) { }
                });

        ArrayObjectAdapter actions = new ArrayObjectAdapter();
        actions.add(new Action(ACTION_PLAY, getString(R.string.play)));
        if (resumeMs > 0) {
            String label = getString(R.string.resume_time, (resumeMs/1000)/60, (resumeMs/1000)%60);
            actions.add(new Action(ACTION_RESUME, label));
        }
        detailsRow.setActionsAdapter(actions);

        rowsAdapter.add(detailsRow);
    }

    private void buildRelated() {
        if (entry == null) return;
        DataRepository repo = new DataRepository(requireContext());
        List<Entry> related = repo.getEntriesByCategory(entry.getMainCategory());
        if (related == null || related.isEmpty()) return;
        ArrayObjectAdapter cards = new ArrayObjectAdapter(new PosterPresenter());
        for (Entry e : related) {
            if (e.getId() == entry.getId()) continue;
            cards.add(e);
        }
        if (cards.size() > 0) {
            rowsAdapter.add(new ListRow(new HeaderItem(getString(R.string.related)), cards));
        }
    }

    private class OverviewPresenter extends FullWidthDetailsOverviewRowPresenter {
        OverviewPresenter() { super(new Presenter() {
            @Override
            public ViewHolder onCreateViewHolder(android.view.ViewGroup parent) {
                ImageCardView v = new ImageCardView(parent.getContext());
                v.setTitleText(entry != null ? entry.getTitle() : "");
                v.setContentText(entry != null ? entry.getDescription() : "");
                return new ViewHolder(v);
            }

            @Override
            public void onBindViewHolder(ViewHolder viewHolder, Object item) {}

            @Override
            public void onUnbindViewHolder(ViewHolder viewHolder) {}
        });
            setOnActionClickedListener(action -> {
                if (entry == null || action == null) return;
                if (action.getId() == ACTION_PLAY) {
                    openDetails(0L);
                } else if (action.getId() == ACTION_RESUME) {
                    openDetails(resumeMs);
                }
            });
        }
    }

    private class PosterPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent) {
            ImageCardView card = new ImageCardView(parent.getContext());
            card.setFocusable(true);
            card.setMainImageDimensions(313, 176);
            return new ViewHolder(card);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            Entry e = (Entry) item;
            ImageCardView card = (ImageCardView) viewHolder.view;
            card.setTitleText(e.getTitle());
            card.setContentText(e.getYearString());
            Glide.with(card.getContext()).load(e.getImageUrl()).centerCrop().into(card.getMainImageView());
            card.setOnClickListener(v -> openAnotherDetails(e));
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) { }
    }

    private void openDetails(long resume) {
        android.content.Intent i = new android.content.Intent(requireContext(), DetailsActivity.class);
        i.putExtra("entry", new Gson().toJson(entry));
        if (resume > 0) i.putExtra("resume_position_ms", resume);
        startActivity(i);
    }

    private void openAnotherDetails(Entry e) {
        android.content.Intent i = new android.content.Intent(requireContext(), TvDetailsActivity.class);
        i.putExtra("entry", new Gson().toJson(e));
        startActivity(i);
    }
}