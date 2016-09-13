package satipsdk.ses.com.satipsdk.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class CustomPager extends ViewPager {

    private boolean mTouchScrollEnabled = true;
    public CustomPager(Context context) {
        super(context);
    }

    public CustomPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!mTouchScrollEnabled)
            return false;

        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!mTouchScrollEnabled)
            return false;

        return super.onTouchEvent(ev);
    }

    public void setSwipeEnabled(boolean enabled) {
        mTouchScrollEnabled = enabled;
    }
}
