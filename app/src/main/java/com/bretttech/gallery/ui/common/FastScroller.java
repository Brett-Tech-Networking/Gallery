package com.bretttech.gallery.ui.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A lightweight fast-scroll overlay that appears on the right side whenever the
 * user scrolls a RecyclerView.  Drag the thumb to rapidly jump through content.
 *
 * Usage:
 *   1. Add this view to your layout, overlapping the RecyclerView on the right.
 *   2. Call attachToRecyclerView(recyclerView) in your fragment/activity.
 */
public class FastScroller extends View {

    private static final int HIDE_DELAY_MS = 1500;
    private static final float TRACK_WIDTH_DP = 3f;
    private static final float THUMB_WIDTH_DP = 7f;
    private static final float THUMB_MIN_HEIGHT_DP = 48f;
    private static final float EDGE_MARGIN_DP = 6f;
    private static final float TRACK_PADDING_V_DP = 16f;

    // Normal thumb colour – semi-transparent light grey
    private static final int THUMB_COLOR_IDLE = 0xBBBBBBBB;
    // Slightly more opaque while dragging
    private static final int THUMB_COLOR_DRAG = 0xDDEEEEEE;
    private static final int TRACK_COLOR = 0x30FFFFFF;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF thumbRect = new RectF();

    private RecyclerView recyclerView;
    private final Runnable hideRunnable = this::hide;

    private float trackWidth;
    private float thumbWidth;
    private float thumbMinHeight;
    private float edgeMargin;
    private float trackPaddingV;

    private boolean isDragging = false;
    private float dragOffset;

    private final RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
            if (canScroll()) {
                show();
                updateThumb();
                scheduleHide();
            }
        }
    };

    public FastScroller(Context context) {
        super(context);
        init(context);
    }

    public FastScroller(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FastScroller(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        float d = context.getResources().getDisplayMetrics().density;
        trackWidth = TRACK_WIDTH_DP * d;
        thumbWidth = THUMB_WIDTH_DP * d;
        thumbMinHeight = THUMB_MIN_HEIGHT_DP * d;
        edgeMargin = EDGE_MARGIN_DP * d;
        trackPaddingV = TRACK_PADDING_V_DP * d;

        trackPaint.setColor(TRACK_COLOR);
        thumbPaint.setColor(THUMB_COLOR_IDLE);

        setAlpha(0f);
        setWillNotDraw(false);
    }

    /** Connect this scroller to a RecyclerView. Pass null to detach. */
    public void attachToRecyclerView(@Nullable RecyclerView rv) {
        if (recyclerView != null) {
            recyclerView.removeOnScrollListener(scrollListener);
        }
        recyclerView = rv;
        if (rv != null) {
            rv.addOnScrollListener(scrollListener);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Core logic
    // ──────────────────────────────────────────────────────────────────────────

    private boolean canScroll() {
        if (recyclerView == null) return false;
        return recyclerView.computeVerticalScrollRange() > recyclerView.computeVerticalScrollExtent();
    }

    private void updateThumb() {
        if (recyclerView == null) return;
        int scrollOffset = recyclerView.computeVerticalScrollOffset();
        int scrollExtent = recyclerView.computeVerticalScrollExtent();
        int scrollRange  = recyclerView.computeVerticalScrollRange();
        if (scrollRange <= 0 || scrollExtent <= 0) return;

        float h = getHeight();
        float top    = trackPaddingV;
        float bottom = h - trackPaddingV;
        float availH = bottom - top;

        // Proportional thumb height (min clamped)
        float thumbH = Math.max(thumbMinHeight, availH * (float) scrollExtent / scrollRange);
        float maxTop = bottom - thumbH;

        float fraction = (float) scrollOffset / Math.max(1, scrollRange - scrollExtent);
        fraction = Math.max(0f, Math.min(1f, fraction));

        float thumbTop = top + (maxTop - top) * fraction;
        float left  = getWidth() - edgeMargin - thumbWidth;
        float right = getWidth() - edgeMargin;
        thumbRect.set(left, thumbTop, right, thumbTop + thumbH);
        invalidate();
    }

    private void show() {
        removeCallbacks(hideRunnable);
        animate().cancel();
        animate().alpha(1f).setDuration(150).start();
        setVisibility(VISIBLE);
    }

    private void hide() {
        if (!isDragging) {
            animate().cancel();
            animate().alpha(0f).setDuration(400).start();
        }
    }

    private void scheduleHide() {
        removeCallbacks(hideRunnable);
        postDelayed(hideRunnable, HIDE_DELAY_MS);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Drawing
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (thumbRect.isEmpty()) return;

        float cx    = getWidth() - edgeMargin - thumbWidth / 2f;
        float r     = trackWidth / 2f;
        float top   = trackPaddingV;
        float bottom = getHeight() - trackPaddingV;

        // Subtle track line
        canvas.drawRoundRect(cx - r, top, cx + r, bottom, r, r, trackPaint);

        // Draggable thumb
        canvas.drawRoundRect(thumbRect, thumbWidth / 2f, thumbWidth / 2f, thumbPaint);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Touch handling
    // ──────────────────────────────────────────────────────────────────────────

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!canScroll()) return false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isTouchingThumb(event.getX(), event.getY())) {
                    isDragging = true;
                    dragOffset = event.getY() - thumbRect.top;
                    removeCallbacks(hideRunnable);
                    thumbPaint.setColor(THUMB_COLOR_DRAG);
                    invalidate();
                    return true;
                }
                return false;

            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    float top     = trackPaddingV;
                    float bottom  = getHeight() - trackPaddingV;
                    float thumbH  = thumbRect.height();
                    float maxTop  = bottom - thumbH;
                    float newTop  = Math.max(top, Math.min(event.getY() - dragOffset, maxTop));
                    float fraction = (newTop - top) / Math.max(1f, maxTop - top);

                    int scrollRange  = recyclerView.computeVerticalScrollRange();
                    int scrollExtent = recyclerView.computeVerticalScrollExtent();
                    int targetOffset = (int) (fraction * (scrollRange - scrollExtent));
                    recyclerView.scrollBy(0, targetOffset - recyclerView.computeVerticalScrollOffset());
                    return true;
                }
                return false;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    isDragging = false;
                    thumbPaint.setColor(THUMB_COLOR_IDLE);
                    invalidate();
                    scheduleHide();
                    return true;
                }
                return false;
        }
        return false;
    }

    /** Checks whether the touch coordinates are within the thumb's touch target. */
    private boolean isTouchingThumb(float x, float y) {
        float pad = 16 * getResources().getDisplayMetrics().density;
        return x >= thumbRect.left - pad
                && y >= thumbRect.top    - pad
                && y <= thumbRect.bottom + pad;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (recyclerView != null) {
            recyclerView.removeOnScrollListener(scrollListener);
        }
        removeCallbacks(hideRunnable);
    }
}
