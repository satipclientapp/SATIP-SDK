package satipsdk.ses.com.satipsdk;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import satipsdk.ses.com.satipsdk.databinding.ActivityChannelsBinding;

public class ChannelsActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener, View.OnTouchListener {

    protected static final int NUM_FRAGMENTS = 2;
    private static final String TAG = "ChannelsActivity";

    ActivityChannelsBinding mBinding;
    TabFragment[] mFragments;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_channels);
        FragmentManager fm = getSupportFragmentManager();
        mFragments = new TabFragment[]{new ChannelsFragment(), new SettingsFragment()};
        mBinding.pager.setAdapter(new ViewPagerAdapter(fm));
        mBinding.slidingTabs.setupWithViewPager(mBinding.pager);
        mBinding.slidingTabs.addOnTabSelectedListener(this);
        mBinding.pager.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                return (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT);
            }
        });
        mBinding.pager.setOnTouchListener(this);
    }

    @Override
    public void onBackPressed() {
        if (mBinding.pager.getCurrentItem() == 0 && ((ChannelsFragment)mFragments[0]).isExpanded()) {
            ((ChannelsFragment)mFragments[0]).toggleFullscreen();
        } else
            super.onBackPressed();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return mBinding.slidingTabs.getVisibility() == View.GONE;
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {

        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return (Fragment) mFragments[position];
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragments[position].getTitle();
        }

        @Override
        public int getCount() {
            return NUM_FRAGMENTS;
        }
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        mFragments[tab.getPosition()].onPageSelected();
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {}

    @Override
    public void onTabReselected(TabLayout.Tab tab) {}

    public void toggleFullscreen(boolean fullscreen) {
        mBinding.slidingTabs.setVisibility(fullscreen ? View.GONE : View.VISIBLE);
        int visibility;
        if (fullscreen) {
            visibility = View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        } else {
            visibility = View.SYSTEM_UI_FLAG_VISIBLE;
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        getWindow().getDecorView().setSystemUiVisibility(visibility);
    }

    @Override
    protected void onDestroy() {
        if (getCurrentFocus() != null)
            getCurrentFocus().clearFocus();
        super.onDestroy();
    }
}

