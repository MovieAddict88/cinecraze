package com.cinecraze.free.tv;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.leanback.widget.Presenter;

public class TvTextCardPresenter extends Presenter {

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Context context = parent.getContext();
        TextView tv = new TextView(context);
        tv.setFocusable(true);
        tv.setFocusableInTouchMode(true);
        tv.setPadding(32, 24, 32, 24);
        tv.setTextSize(18f);
        tv.setTextColor(0xFFFFFFFF);
        return new ViewHolder(tv);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        TextView tv = (TextView) viewHolder.view;
        tv.setText(String.valueOf(item));
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        // No-op
    }
}