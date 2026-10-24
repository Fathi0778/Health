package com.example.healthmate.ui.common;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.healthmate.R;

/*
 * My own bar chart, drawn with Canvas (no chart library needed).
 * - one rounded bar per day, the value on top, the day name below
 * - a dashed line shows the daily goal
 * - a day with no record gets a small grey bar and "–"
 */
public class BarChartView extends View {

    // makes the short text above a bar (8420 -> "8.4k")
    public interface ValueFormatter {
        String format(float value);
    }

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint baselinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goalLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dayLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint todayLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valueLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path goalPath = new Path();

    private float[] values = new float[0];
    private boolean[] present = new boolean[0];
    private String[] labels = new String[0];
    private float goal;
    @Nullable
    private ValueFormatter formatter;
    private String goalLabel = "";
    // 0 to 1, makes the bars grow up when new data comes in
    private float animationProgress = 1f;
    @Nullable
    private ValueAnimator animator;

    public BarChartView(Context context) {
        this(context, null);
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        int secondaryText = ContextCompat.getColor(context, R.color.hm_text_secondary);
        int primaryText = ContextCompat.getColor(context, R.color.hm_text_primary);

        barPaint.setStyle(Paint.Style.FILL);
        barPaint.setColor(ContextCompat.getColor(context, R.color.hm_primary));

        emptyPaint.setStyle(Paint.Style.FILL);
        emptyPaint.setColor(ContextCompat.getColor(context, R.color.hm_outline));

        baselinePaint.setColor(ContextCompat.getColor(context, R.color.hm_outline));
        baselinePaint.setStrokeWidth(dp(1));

        goalPaint.setStyle(Paint.Style.STROKE);
        goalPaint.setStrokeWidth(dp(1.5f));
        goalPaint.setColor(secondaryText);
        goalPaint.setPathEffect(new DashPathEffect(new float[]{dp(5), dp(4)}, 0));

        goalLabelPaint.setColor(secondaryText);
        goalLabelPaint.setTextSize(sp(10));
        goalLabelPaint.setTextAlign(Paint.Align.RIGHT);

        dayLabelPaint.setColor(secondaryText);
        dayLabelPaint.setTextSize(sp(11));
        dayLabelPaint.setTextAlign(Paint.Align.CENTER);

        todayLabelPaint.set(dayLabelPaint);
        todayLabelPaint.setColor(primaryText);
        todayLabelPaint.setFakeBoldText(true);

        valueLabelPaint.setColor(primaryText);
        valueLabelPaint.setTextSize(sp(10));
        valueLabelPaint.setTextAlign(Paint.Align.CENTER);
        valueLabelPaint.setFakeBoldText(true);
    }

    /*
     * values    = one number per day (oldest first)
     * present   = false if that day has no record
     * labels    = day names, like "Mon"
     * goal      = daily goal for the dashed line (0 = no line)
     * barColor  = colour of the bars
     * formatter = text above each bar
     * goalLabel = text next to the goal line
     */
    public void setData(@NonNull float[] values, @NonNull boolean[] present, @NonNull String[] labels,
                        float goal, @ColorInt int barColor, @NonNull ValueFormatter formatter,
                        @NonNull String goalLabel) {
        this.values = values;
        this.present = present;
        this.labels = labels;
        this.goal = goal;
        this.formatter = formatter;
        this.goalLabel = goalLabel;
        barPaint.setColor(barColor);
        startGrowAnimation();
    }

    private void startGrowAnimation() {
        if (animator != null) {
            animator.cancel();
        }
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(650);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            animationProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
        }
        animationProgress = 1f;
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = (int) dp(190);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int count = values.length;
        if (count == 0) {
            return;
        }

        float left = getPaddingLeft() + dp(4);
        float right = getWidth() - getPaddingRight() - dp(4);
        float top = getPaddingTop() + dp(22);                     // space for the numbers on top
        float bottom = getHeight() - getPaddingBottom() - dp(24); // space for the day names
        float chartHeight = bottom - top;

        float max = goal;
        for (float v : values) {
            max = Math.max(max, v);
        }
        if (max <= 0) {
            max = 1;
        }
        max *= 1.1f; // a bit of space above the tallest bar

        float slot = (right - left) / count;
        float barWidth = Math.min(slot * 0.55f, dp(30));
        float radius = Math.min(barWidth / 2f, dp(8));

        // bottom line
        canvas.drawLine(left, bottom, right, bottom, baselinePaint);

        for (int i = 0; i < count; i++) {
            float centerX = left + slot * (i + 0.5f);
            float barLeft = centerX - barWidth / 2f;
            float barRight = centerX + barWidth / 2f;
            boolean hasValue = i < present.length && present[i] && values[i] > 0;

            if (hasValue) {
                float barHeight = Math.max(dp(4), (values[i] / max) * chartHeight * animationProgress);
                rect.set(barLeft, bottom - barHeight, barRight, bottom);
                canvas.drawRoundRect(rect, radius, radius, barPaint);
                // make the bottom corners square
                if (barHeight > radius) {
                    rect.set(barLeft, bottom - radius, barRight, bottom);
                    canvas.drawRect(rect, barPaint);
                }
                if (formatter != null && animationProgress > 0.6f) {
                    canvas.drawText(formatter.format(values[i]), centerX,
                            bottom - barHeight - dp(6), valueLabelPaint);
                }
            } else {
                // small grey bar for a day with no record (or 0)
                rect.set(barLeft, bottom - dp(4), barRight, bottom);
                canvas.drawRoundRect(rect, dp(2), dp(2), emptyPaint);
                canvas.drawText(i < present.length && present[i] ? "0" : "–",
                        centerX, bottom - dp(10), dayLabelPaint);
            }

            if (i < labels.length) {
                Paint labelPaint = i == count - 1 ? todayLabelPaint : dayLabelPaint;
                canvas.drawText(labels[i], centerX, getHeight() - getPaddingBottom() - dp(6), labelPaint);
            }
        }

        // dashed goal line
        if (goal > 0) {
            float goalY = bottom - (goal / max) * chartHeight;
            goalPath.reset();
            goalPath.moveTo(left, goalY);
            goalPath.lineTo(right, goalY);
            canvas.drawPath(goalPath, goalPaint);
            canvas.drawText(goalLabel, right, goalY - dp(4), goalLabelPaint);
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value,
                getResources().getDisplayMetrics());
    }
}