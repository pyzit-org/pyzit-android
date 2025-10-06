package com.pyzit.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;

public class LogoLoadingView extends View {

    private Paint paint;
    private RectF rectF;
    private float sweepAngle = 0;
    private boolean growing = true;
    private RotateAnimation rotateAnimation;

    public LogoLoadingView(Context context) {
        super(context);
        init();
    }

    public LogoLoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LogoLoadingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.parseColor("#667eea"));
        paint.setStrokeWidth(8);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);

        rectF = new RectF();

        // Create rotation animation
        rotateAnimation = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setDuration(2000);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        rotateAnimation.setInterpolator(new LinearInterpolator());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float radius = Math.min(centerX, centerY) - paint.getStrokeWidth();

        rectF.set(centerX - radius, centerY - radius,
                centerX + radius, centerY + radius);

        // Draw rotating arc
        canvas.drawArc(rectF, 0, sweepAngle, false, paint);

        // Animate the sweep angle
        if (growing) {
            sweepAngle += 4;
            if (sweepAngle >= 300) {
                growing = false;
            }
        } else {
            sweepAngle -= 4;
            if (sweepAngle <= 20) {
                growing = true;
            }
        }

        // Draw center dot
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerX, centerY, radius / 6, paint);
        paint.setStyle(Paint.Style.STROKE);

        invalidate(); // Continue animation
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.startAnimation(rotateAnimation);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.clearAnimation();
    }
}