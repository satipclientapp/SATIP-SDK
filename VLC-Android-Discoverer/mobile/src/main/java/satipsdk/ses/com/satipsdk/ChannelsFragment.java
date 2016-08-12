package satipsdk.ses.com.satipsdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaList;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.HWDecoderUtil;

import java.util.ArrayList;

import satipsdk.ses.com.satipsdk.adapters.ListAdapter;
import satipsdk.ses.com.satipsdk.databinding.FragmentChannelsBinding;

public class ChannelsFragment extends Fragment implements TabFragment, ListAdapter.ItemClickCb,
        View.OnFocusChangeListener, View.OnClickListener, IVLCVout.Callback, View.OnTouchListener,
        GestureDetector.OnGestureListener, MediaPlayer.EventListener {

    private static final String TAG = "ChannelsFragment";
    private static final boolean ENABLE_SUBTITLES = true;

    private FragmentChannelsBinding mBinding;
    private ViewDimensions mViewDimensions;
    private OnScrollListener mScrollListener;
    private SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(SatIpApplication.get());
    private boolean expanded = false;
    private int mScreenWidth, mScreenHeight, mFoldedViewWidth;
    private double mVideoAr = 1.0d;

    public ChannelsFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowManager windowManager = (WindowManager) SatIpApplication.get().getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        windowManager.getDefaultDisplay().getSize(size);
        mScreenWidth = size.x;
        mScreenHeight = size.y;

        //VLC player init
        final ArrayList<String> args = new ArrayList<>();
        mLibVLC = new LibVLC(args);
        mMediaPlayer = new MediaPlayer(mLibVLC);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_channels, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        final String url = mSharedPreferences.getString(SettingsFragment.KEY_CURRENT_CHANNEL_LIST_ADDRESS, null);
        final String device = mSharedPreferences.getString(SettingsFragment.KEY_CURRENT_DEVICE, null);

        if (url == null || device == null)
            ((ChannelsActivity)getActivity()).mBinding.pager.setCurrentItem(1);
        else
            loadChannelList(Uri.parse(url+"?"+device), false);

        if (ENABLE_SUBTITLES && HWDecoderUtil.HAS_SUBTITLES_SURFACE) {
            final ViewStub stub = (ViewStub) view.findViewById(R.id.subtitles_stub);
            mSubtitlesSurface = (SurfaceView) stub.inflate();
            mSubtitlesSurface.setZOrderMediaOverlay(true);
            mSubtitlesSurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        }

        ArrayList<ListAdapter.Item> channelList = new ArrayList<>();
        mBinding.channelList.setLayoutManager(new LinearLayoutManager(getActivity()));
        ListAdapter channelsAdapter = new ListAdapter(channelList);
        channelsAdapter.setItemClickHandler(this);
        channelsAdapter.setGlideRequestManager(Glide.with(this));
        mBinding.channelList.setAdapter(channelsAdapter);
        mBinding.channelList.setItemAnimator(null);
        channelsAdapter.notifyDataSetChanged();
        mScrollListener = new ChannelListScrollListener();
        mBinding.channelList.addOnScrollListener(mScrollListener);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            mBinding.videoSurfaceFrame.setOnFocusChangeListener(this);
        mBinding.videoSurfaceFrame.setOnClickListener(this);
        mGestureDetector = new GestureDetectorCompat(getActivity(), this);
        mBinding.videoSurfaceFrame.setOnTouchListener(this);
        mBinding.videoSurfaceFrame.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (!expanded) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        focusOnCurrentChannel();
                        return true;
                    }
                    //Deactivate fragment switch with →
                    return keyCode == KeyEvent.KEYCODE_DPAD_RIGHT;
                }
                if (keyCode != KeyEvent.KEYCODE_DPAD_RIGHT && keyCode != KeyEvent.KEYCODE_DPAD_LEFT)
                    return false;
                if (event.getAction() == KeyEvent.ACTION_DOWN)
                    switchToSiblingChannel(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT);
                return true;
            }
        });
    }

    public void stopPlayback() {
        mMediaPlayer.stop();
        mBinding.videoSurfaceFrame.setVisibility(View.GONE);
        mSharedPreferences.edit()
                .putInt(SettingsFragment.KEY_SELECTED_CHANNEL, 0)
                .putString(SettingsFragment.KEY_LAST_CHANNEL_URL, null)
                .apply();
    }

    public void loadChannelList(final Uri uri, final boolean focus) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Media playlist = new Media(mLibVLC, uri);
                playlist.parse(Media.Parse.ParseNetwork);
                final MediaList ml = playlist.subItems();
                playlist.release();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        ListAdapter la = (ListAdapter) mBinding.channelList.getAdapter();
                        la.clear();
                        Media media;
                        String title;
                        for (int i = 0; i< ml.getCount(); ++i) {
                            media = ml.getMediaAt(i);
                            title = media.getMeta(Media.Meta.Title);
                            int dot = title.indexOf('.');
                            la.add(new ListAdapter.Item(ListAdapter.TYPE_CHANNEL,
                                    media.getMeta(Media.Meta.Title).substring(dot+2),
                                    media.getUri(),
                                    null));
                        }
                        la.select(mSharedPreferences.getInt(SettingsFragment.KEY_SELECTED_CHANNEL, 0));
                        if (focus)
                            focusOnCurrentChannel();
                        String lastUrl = mSharedPreferences.getString(SettingsFragment.KEY_LAST_CHANNEL_URL, null);
                        if (!TextUtils.isEmpty(lastUrl))
                            play(Uri.parse(lastUrl));
                        else if (ml != null && ml.getCount() >0)
                            play(ml.getMediaAt(0).getUri());
                    }
                });
            }
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMediaPlayer.release();
        mLibVLC.release();
    }

    @Override
    public void onResume() {
        super.onResume();

        final IVLCVout vlcVout = mMediaPlayer.getVLCVout();
        vlcVout.setVideoView(mBinding.videoSurface);
        if (mSubtitlesSurface != null)
            vlcVout.setSubtitlesView(mSubtitlesSurface);
        vlcVout.attachViews();
        mMediaPlayer.getVLCVout().addCallback(this);
        mMediaPlayer.setEventListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mOnLayoutChangeListener != null) {
            mBinding.videoSurfaceFrame.removeOnLayoutChangeListener(mOnLayoutChangeListener);
            mOnLayoutChangeListener = null;
        }

        mMediaPlayer.stop();
        mMediaPlayer.getVLCVout().detachViews();
        mMediaPlayer.getVLCVout().removeCallback(this);
        mMediaPlayer.setEventListener(null);
    }

    @Override
    public String getTitle() {
        return SatIpApplication.get().getString(R.string.live_tv_title);
    }

    @Override
    public void onPageSelected() {
        focusOnCurrentChannel();
    }

    private void focusOnCurrentChannel() {
        View v = mBinding.channelList.getChildAt(mSharedPreferences.getInt(SettingsFragment.KEY_SELECTED_CHANNEL, 0));
        if (v != null) {
            v.setFocusableInTouchMode(true);
            v.requestFocus();
        }
    }

    private class ChannelListScrollListener extends RecyclerView.OnScrollListener {
        RequestManager glide = ((ListAdapter) mBinding.channelList.getAdapter()).getGlideRequestManager();
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (newState == RecyclerView.SCROLL_STATE_SETTLING)
               glide.pauseRequests();
            else
               glide.resumeRequests();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        v.setElevation(hasFocus ? 50.0f : 0.0f);
    }

    @Override
    public void onClick(View v) {
        if (v.equals(mBinding.videoSurfaceFrame)){
            toggleFullscreen();
        }
    }

    public boolean isExpanded() {
        return expanded;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void toggleFullscreen() {
        if (mBinding.videoSurfaceFrame.getHeight() < 100)
            return;
        expanded = !expanded;
        Resources res = mBinding.getRoot().getContext().getResources();
        mBinding.getRoot().setBackgroundColor(expanded ? res.getColor(android.R.color.black) : res.getColor(R.color.light_gray));
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mBinding.videoSurfaceFrame.getLayoutParams();
        if (expanded) {
            lp.width = mScreenWidth;
            lp.height = mScreenHeight;
            lp.leftMargin = 0;
            lp.bottomMargin = 0;
            lp.rightMargin = 0;
            lp.topMargin = 0;
        } else {
            if (mViewDimensions != null) {
                lp.width = mViewDimensions.videoWidth;
                lp.height = mViewDimensions.videoHeight;
                lp.leftMargin = mViewDimensions.leftMargin;
                lp.bottomMargin = mViewDimensions.bottomMargin;
                lp.rightMargin = mViewDimensions.rightMargin;
                lp.topMargin = mViewDimensions.topMargin;
            } else {
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            }
            focusOnCurrentChannel();
        }
        lp.addRule(RelativeLayout.CENTER_VERTICAL, expanded ? RelativeLayout.TRUE : 0);
        mBinding.channelList.setVisibility(expanded ? View.GONE : View.VISIBLE);
        mBinding.sesLogo.setVisibility(expanded ? View.GONE : View.VISIBLE);
        ((ChannelsActivity)getActivity()).toggleFullscreen(expanded);
        mBinding.videoSurfaceFrame.setLayoutParams(lp);
        updateVideoSurfaces();
    }

    @Override
    public void onEvent(MediaPlayer.Event event) {
        switch(event.type) {
            case MediaPlayer.Event.Playing:
                mBinding.videoSurfaceFrame.setVisibility(View.VISIBLE);
                mBinding.videoSurfaceFrame.setFocusable(true);
                break;
            case MediaPlayer.Event.Stopped:
                mBinding.videoSurfaceFrame.setVisibility(View.GONE);
                mBinding.videoSurfaceFrame.setFocusable(false);
                break;
        }
    }

    class ViewDimensions {
        public int videoWidth, videoHeight, leftMargin, bottomMargin, rightMargin, topMargin;

        ViewDimensions() {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mBinding.videoSurfaceFrame.getLayoutParams();

            videoWidth = mBinding.videoSurfaceFrame.getMeasuredWidth();
            videoHeight = mBinding.videoSurfaceFrame.getMeasuredHeight();
            leftMargin = lp.leftMargin;
            bottomMargin = lp.bottomMargin;
            rightMargin = lp.rightMargin;
            topMargin = lp.topMargin;
        }
    }

    /*
     * Video surface management
     */

    private static final int FLING_MIN_VELOCITY = 3000;

    private final Handler mHandler = new Handler();
    private View.OnLayoutChangeListener mOnLayoutChangeListener = null;

    private LibVLC mLibVLC = null;
    private MediaPlayer mMediaPlayer = null;
    private int mVideoHeight = 0;
    private int mVideoWidth = 0;
    private int mVideoVisibleHeight = 0;
    private int mVideoVisibleWidth = 0;
    private int mVideoSarNum = 0;
    private int mVideoSarDen = 0;
    private SurfaceView mSubtitlesSurface = null;
    private GestureDetectorCompat mGestureDetector;

    public void onItemClick(int position, ListAdapter.Item item) {
        play(position, item);
    }

    private void play(int position, ListAdapter.Item item) {
        play(item.uri);
        mSharedPreferences.edit()
                .putString(SettingsFragment.KEY_LAST_CHANNEL_URL, item.uri.toString())
                .putInt(SettingsFragment.KEY_SELECTED_CHANNEL, position).apply();
        if (!expanded)
            focusOnCurrentChannel();
    }

    private void play(final Uri uri) {
        mBinding.videoSurfaceFrame.setVisibility(View.INVISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Media media = new Media(mLibVLC, uri);
                try {
                    mMediaPlayer.setMedia(media);
                    mMediaPlayer.play();
                } catch (IllegalStateException e) {
                    try { //retry
                        mMediaPlayer.setMedia(media);
                    mMediaPlayer.play();
                    } catch (IllegalStateException e1) {} //too bad
                } finally {
                    media.release();
                }
            }
        }).start();
    }

    public void switchToSiblingChannel(boolean next) {
        ListAdapter adapter = (ListAdapter)mBinding.channelList.getAdapter();
        int newPosition = adapter.getSelectedPosition() + (next ? 1 : -1);
        ListAdapter.Item item = adapter.getItem(newPosition);
        play(newPosition, item);
        adapter.select(newPosition);
    }

    private void updateVideoSurfaces() {
        mVideoWidth = expanded ? mScreenWidth : mFoldedViewWidth;
        mVideoHeight = expanded ? mScreenHeight : (int) (mFoldedViewWidth / mVideoAr);
        if (mVideoWidth * mVideoHeight == 0 || getActivity() == null)
            return;
        int sw = mVideoWidth;
        int sh =  mVideoHeight;

        mMediaPlayer.getVLCVout().setWindowSize(sw, sh);
        double dw = sw, dh = sh;

        if (sw < sh) {
            dw = sh;
            dh = sw;
        }

        // sanity check
        if (dw * dh == 0) {
            Log.e(TAG, "Invalid surface size");
            return;
        }

        // compute the aspect ratio
        double ar, vw;
        if (mVideoSarDen == mVideoSarNum) {
            /* No indication about the density, assuming 1:1 */
            vw = mVideoVisibleWidth;
            ar = (double)mVideoVisibleWidth / (double)mVideoVisibleHeight;
        } else {
            /* Use the specified aspect ratio */
            vw = mVideoVisibleWidth * (double)mVideoSarNum / mVideoSarDen;
            ar = vw / mVideoVisibleHeight;
        }

        // compute the display aspect ratio
        double dar = dw / dh;

        if (dar < ar)
            dh = dw / ar;
        else
            dw = dh * ar;

        // set display size
        ViewGroup.LayoutParams lp = mBinding.videoSurface.getLayoutParams();
        lp.width = (int) Math.floor(dw);
        lp.height = (int) Math.floor(dh);
        mBinding.videoSurface.setLayoutParams(lp);
        if (mSubtitlesSurface != null)
            mSubtitlesSurface.setLayoutParams(lp);

        // set frame size (crop if necessary)
        lp = mBinding.videoSurfaceFrame.getLayoutParams();
        lp.width = (int) Math.floor(dw);
        lp.height = (int) Math.floor(dh);
        mBinding.videoSurfaceFrame.setLayoutParams(lp);

        mBinding.videoSurface.invalidate();
        if (mSubtitlesSurface != null)
            mSubtitlesSurface.invalidate();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void onNewLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        if (!expanded)
            mFoldedViewWidth = mBinding.videoSurfaceFrame.getWidth();
        mVideoVisibleWidth = visibleWidth;
        mVideoVisibleHeight = visibleHeight;
        mVideoSarNum = sarNum;
        mVideoSarDen = sarDen;
        mVideoAr = width/(double)height;
        updateVideoSurfaces();
        if (mViewDimensions == null && mBinding.videoSurfaceFrame.getMeasuredHeight() > 10)
            mViewDimensions = new ViewDimensions();
    }

    @Override
    public void onSurfacesCreated(IVLCVout vlcVout) {}

    @Override
    public void onSurfacesDestroyed(IVLCVout vlcVout) {}

    @Override
    public void onHardwareAccelerationError(IVLCVout vlcVout) {}

    /*
     * Touch controls
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_MOVE){
           mBinding.getRoot().getParent().requestDisallowInterceptTouchEvent(true);
        }
        if (mGestureDetector != null && mGestureDetector.onTouchEvent(event))
            return true;
        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {}

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {}

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (Math.abs(velocityX) > FLING_MIN_VELOCITY) {
            switchToSiblingChannel(velocityX < 0);
            return true;
        }
        return false;
    }
}
