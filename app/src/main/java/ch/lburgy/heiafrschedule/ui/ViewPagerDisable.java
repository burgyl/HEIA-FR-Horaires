package ch.lburgy.heiafrschedule.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;

public class ViewPagerDisable extends ViewPager {
    private boolean enable = true;

    public ViewPagerDisable(Context context) {
        super(context);
    }

    public ViewPagerDisable(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return enable && super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return enable && super.onTouchEvent(event);
    }

    @Override
    public boolean executeKeyEvent(@NonNull KeyEvent event) {
        return enable && super.executeKeyEvent(event);
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}