package com.example.healthmate.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.healthmate.R;

import java.text.NumberFormat;
import java.util.Locale;

/*
 * Fills the small cards (item_stat_tile.xml and item_chart_card.xml),
 * so the Progress and Sleep screens don't repeat the same code.
 */
public final class CardBinder {

    private CardBinder() {
    }

    public static void bindStatTile(@NonNull View tile, @DrawableRes int iconRes,
                                    @ColorRes int colorRes, @ColorRes int lightColorRes,
                                    @NonNull String value, @NonNull String label) {
        tintIcon(tile.findViewById(R.id.stat_icon), iconRes, colorRes, lightColorRes);
        ((TextView) tile.findViewById(R.id.stat_value)).setText(value);
        ((TextView) tile.findViewById(R.id.stat_label)).setText(label);
        tile.setContentDescription(label + ": " + value);
    }

    public static void bindChart(@NonNull View card, @DrawableRes int iconRes,
                                 @ColorRes int colorRes, @ColorRes int lightColorRes,
                                 @NonNull String title, @NonNull String goalText,
                                 @NonNull float[] values, @NonNull boolean[] present,
                                 @NonNull String[] labels, float goal,
                                 @NonNull BarChartView.ValueFormatter formatter) {
        Context context = card.getContext();
        tintIcon(card.findViewById(R.id.chart_icon), iconRes, colorRes, lightColorRes);
        ((TextView) card.findViewById(R.id.chart_title)).setText(title);
        ((TextView) card.findViewById(R.id.chart_goal)).setText(goalText);

        BarChartView chart = card.findViewById(R.id.chart_view);
        chart.setData(values, present, labels, goal,
                ContextCompat.getColor(context, colorRes), formatter,
                context.getString(R.string.chart_goal));

        // for screen readers: say every bar in words
        StringBuilder description = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            description.append(labels[i]).append(' ')
                    .append(present[i] ? formatter.format(values[i]) : "–");
            if (i < values.length - 1) {
                description.append(", ");
            }
        }
        chart.setContentDescription(context.getString(R.string.chart_cd, title, description));
    }

    private static void tintIcon(ImageView icon, @DrawableRes int iconRes,
                                 @ColorRes int colorRes, @ColorRes int lightColorRes) {
        Context context = icon.getContext();
        icon.setImageResource(iconRes);
        icon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, colorRes)));
        icon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, lightColorRes)));
    }

    // short numbers for the charts: 8420 -> "8.4k", 950 -> "950"
    public static String compact(float value) {
        if (value >= 1000) {
            float thousands = Math.round(value / 100f) / 10f;
            return (thousands == (int) thousands
                    ? String.valueOf((int) thousands)
                    : String.format(Locale.getDefault(), "%.1f", thousands)) + "k";
        }
        return NumberFormat.getIntegerInstance(Locale.getDefault()).format(Math.round(value));
    }
}
