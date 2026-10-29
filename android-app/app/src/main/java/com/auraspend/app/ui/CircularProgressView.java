package com.auraspend.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class CircularProgressView extends View {

    private int progress = 0; // 0 to 100+
    private boolean isLight = false;

    private Paint trackPaint;
    private Paint progressPaint;
    private Paint textPaint;
    private Paint subtextPaint;
    private RectF arcBounds;

    public CircularProgressView(Context context) {
        super(context);
        init();
    }

    public CircularProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        float strokeWidth = 7f * density;

        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokeWidth);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setColor(0xFFBD8BFF); // #bd8bff purple accent

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextSize(15f * density);

        subtextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subtextPaint.setTextAlign(Paint.Align.CENTER);
        subtextPaint.setTextSize(9f * density);

        arcBounds = new RectF();
        updateThemeColors();
    }

    public void setTheme(boolean light) {
        this.isLight = light;
        updateThemeColors();
        invalidate();
    }

    private void updateThemeColors() {
        if (isLight) {
            trackPaint.setColor(0xFFE8DEF8);
            textPaint.setColor(0xFF201632);
            subtextPaint.setColor(0xFF786B81);
        } else {
            trackPaint.setColor(0x2AFFFFFF);
            textPaint.setColor(0xFFFFFFFF);
            subtextPaint.setColor(0xFFA99DB8);
        }
    }

    public void setProgress(int percent) {
        this.progress = Math.max(0, percent);
        invalidate();
    }

    public int getProgress() {
        return progress;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float density = getResources().getDisplayMetrics().density;
        int defaultSize = (int) (90 * density);
        int width = resolveSize(defaultSize, widthMeasureSpec);
        int height = resolveSize(defaultSize, heightMeasureSpec);
        int size = Math.min(width, height);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float strokeWidth = trackPaint.getStrokeWidth();
        float pad = strokeWidth / 2f + 2f;
        arcBounds.set(pad, pad, getWidth() - pad, getHeight() - pad);

        // Draw background circle
        canvas.drawOval(arcBounds, trackPaint);

        // Draw progress arc
        float sweep = (float) Math.min(360.0, 360.0 * (Math.min(100, progress) / 100.0));
        if (sweep > 0) {
            canvas.drawArc(arcBounds, -90, sweep, false, progressPaint);
        }

        // Draw center text: e.g. "72%" and "budget"
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        String percentText = progress + "%";
        String subText = "budget";

        canvas.drawText(percentText, centerX, centerY - 2f, textPaint);
        canvas.drawText(subText, centerX, centerY + (subtextPaint.getTextSize() + 4f), subtextPaint);
    }
}
