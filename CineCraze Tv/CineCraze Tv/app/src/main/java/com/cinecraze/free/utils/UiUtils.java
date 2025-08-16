package com.cinecraze.free.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;

public final class UiUtils {
    private UiUtils() {}

    public static int calculateSpanCountByItemWidthPx(Context context, int desiredItemWidthPx) {
        if (context == null || desiredItemWidthPx <= 0) {
            return 2;
        }
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int screenWidthPx = metrics.widthPixels;
        int spanCount = screenWidthPx / desiredItemWidthPx;
        if (spanCount < 1) spanCount = 1;
        return spanCount;
    }

    public static int calculateSpanCount(Context context, float desiredItemWidthDp) {
        if (context == null || desiredItemWidthDp <= 0f) {
            return 2;
        }
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int desiredPx = (int) (desiredItemWidthDp * metrics.density + 0.5f);
        return calculateSpanCountByItemWidthPx(context, desiredPx);
    }

    public static boolean isTelevision(Context context) {
        if (context == null) return false;
        int uiModeType = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_TYPE_MASK;
        return uiModeType == Configuration.UI_MODE_TYPE_TELEVISION;
    }
}