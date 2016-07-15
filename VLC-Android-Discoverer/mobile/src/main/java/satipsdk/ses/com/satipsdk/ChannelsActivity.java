package satipsdk.ses.com.satipsdk;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;

import satipsdk.ses.com.satipsdk.databinding.ActivityChannelsBinding;

public class ChannelsActivity extends AppCompatActivity {

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
    }

    @Override
    public void onBackPressed() {
        if (mBinding.pager.getCurrentItem() == 0 && ((ChannelsFragment)mFragments[0]).isExpanded()) {
            ((ChannelsFragment)mFragments[0]).toggleFullscreen();
        } else
            super.onBackPressed();
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
