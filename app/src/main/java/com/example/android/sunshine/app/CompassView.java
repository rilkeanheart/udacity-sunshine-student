package com.example.android.sunshine.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

/**
 * Created by michaelgreen on 10/9/15.
 */
public class CompassView extends View {
    private ShapeDrawable mDrawable;
    private float direction;

    public CompassView(Context context) {
        super(context);

        int x = 10;
        int y = 10;
        int width = 300;
        int height = 50;

        mDrawable = new ShapeDrawable(new OvalShape());
        mDrawable.getPaint().setColor(0xff74AC23);
        mDrawable.setBounds(x, y, x + width, y + height);
    }

    public CompassView(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    public CompassView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
    }

    @Override
    public void onMeasure(int wMeasureSpec,
                          int hMeasureSpec) {
        int wSpecMode = MeasureSpec.getMode(wMeasureSpec);
        int measuredWidth = MeasureSpec.getSize(wMeasureSpec);

        if(wSpecMode != MeasureSpec.EXACTLY) {
            // Normally we would wrap the content here
            measuredWidth = MeasureSpec.getSize(wMeasureSpec);
        }

        int hSpecMode = MeasureSpec.getMode(hMeasureSpec);
        int measuredHeight = MeasureSpec.getSize(hMeasureSpec);
        if(hSpecMode != MeasureSpec.EXACTLY) measuredHeight = MeasureSpec.getSize(hMeasureSpec);

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    protected void onDraw(Canvas canvas) {
        int w = getMeasuredWidth();
        int h = getMeasuredHeight();
        int r;
        if(w > h){
            r = h/2;
        }else{
            r = w/2;
        }

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(Color.GRAY);

        canvas.drawCircle(w/2, h/2, r, paint);

        paint.setColor(getResources().getColor(R.color.primary_dark));
        canvas.drawLine(
                w / 2,
                h / 2,
                (float) (w / 2 + r * Math.sin(-direction)),
                (float) (h / 2 - r * Math.cos(-direction)),
                paint);


    }

    public void update(float dir){
        AccessibilityManager accessibilityManager =
                (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);

        if(accessibilityManager.isEnabled()) {
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED);
        }
        direction = dir;

        // Call invalidate to force drawing on page.

        invalidate();
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        event.getText().add(String.valueOf(direction));
        return true;
    }
}
