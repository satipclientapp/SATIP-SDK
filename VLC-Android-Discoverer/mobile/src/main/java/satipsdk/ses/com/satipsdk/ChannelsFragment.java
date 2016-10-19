package satipsdk.ses.com.satipsdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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

import java.util.ArrayList;

import satipsdk.ses.com.satipsdk.adapters.ListAdapter;
import satipsdk.ses.com.satipsdk.databinding.FragmentChannelsBinding;

public class ChannelsFragment extends Fragment implements TabFragment, ListAdapter.ItemClickCb,
        View.OnFocusChangeListener, View.OnClickListener, IVLCVout.Callback, View.OnTouchListener,
        GestureDetector.OnGestureListener, MediaPlayer.EventListener {

    private static final String TAG = "ChannelsFragment";
    private static final boolean ENABLE_SUBTITLES = true;

    private static final int TOGGLE_FULLSCREEN_NO_ES = 0;
    private static final int TOGGLE_FULLSCREEN_VOUT_KO = 1;
    private static final int HIDE_ERROR_MSG = 2;

    private static final int FLING_MIN_VELOCITY = 3000;
    private static final int TIMEOUT_ES_MILLIS = 2000;
    private static final int TIMEOUT_VOUT_MILLIS = 5000;

    private FragmentChannelsBinding mBinding;
    private OnScrollListener mScrollListener;
    private SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(SatIpApplication.get());
    private boolean expanded = false;
    private int mScreenWidth, mScreenHeight, mFoldedViewWidth;
    private double mVideoAr = 1.0d;
    private volatile boolean mHasVout = false;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TOGGLE_FULLSCREEN_NO_ES:
                case TOGGLE_FULLSCREEN_VOUT_KO:
                    removeMessages(TOGGLE_FULLSCREEN_NO_ES);
                    removeMessages(TOGGLE_FULLSCREEN_VOUT_KO);
                    mBinding.errorMsg.setVisibility(View.VISIBLE);
                    mBinding.videoSurface.setVisibility(View.INVISIBLE);
                    mBinding.videoSurfaceFrame.setVisibility(View.VISIBLE);
                    mBinding.videoSurfaceFrame.setBackgroundColor(Color.BLACK);
                    mBinding.videoSurfaceFrame.setFocusable(true);
                    mBinding.videoSurfaceFrame.requestFocus();
                    break;
                case HIDE_ERROR_MSG:
                    removeMessages(TOGGLE_FULLSCREEN_NO_ES);
                    removeMessages(TOGGLE_FULLSCREEN_VOUT_KO);
                    mBinding.videoSurface.setVisibility(View.VISIBLE);
                    mBinding.videoSurfaceFrame.setBackgroundResource(0);
                    mBinding.errorMsg.setVisibility(View.GONE);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    public ChannelsFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowManager windowManager = (WindowManager) SatIpApplication.get().getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN)
            windowManager.getDefaultDisplay().getSize(size);
        else
            windowManager.getDefaultDisplay().getRealSize(size);
        mScreenWidth = size.x;
        mScreenHeight = size.y;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_channels, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        if (ENABLE_SUBTITLES) {
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
                    return (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                            keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                            keyCode == KeyEvent.KEYCODE_DPAD_DOWN );
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
        if (mMediaPlayer == null || mMediaPlayer.getMedia() == null)
            return;
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
                final String device = mSharedPreferences.getString(SettingsFragment.KEY_CURRENT_DEVICE, "");
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
                        for (int i = 0; i< ml.getCount(); ++i) {
                            media = ml.getMediaAt(i);
                            media.parse(Media.Parse.ParseNetwork);
                            Uri mediaUri = generateUri(media.getUri(), device);
                            la.add(new ListAdapter.Item(ListAdapter.TYPE_CHANNEL,
                                    media.getMeta(Media.Meta.Title),
                                    mediaUri,
                                    null));
                        }
                        la.select(mSharedPreferences.getInt(SettingsFragment.KEY_SELECTED_CHANNEL, 0));
                        if (focus)
                            focusOnCurrentChannel();
                        String lastUrl = mSharedPreferences.getString(SettingsFragment.KEY_LAST_CHANNEL_URL, null);
                        if (!TextUtils.isEmpty(lastUrl))
                            play(Uri.parse(lastUrl));
                        else if (ml != null && ml.getCount() >0)
                            play(la.getItem(0).uri);
                    }
                });
            }
        }).start();
    }

    private Uri generateUri(Uri mediaUri, String device) {
        boolean replaceScheme = TextUtils.equals(mediaUri.getScheme(), "rtsp");
        boolean replaceAuthority = TextUtils.equals(mediaUri.getEncodedAuthority(), "sat.ip");
        return new Uri.Builder().scheme(replaceScheme ? "satip" : mediaUri.getScheme())
                .encodedAuthority(replaceAuthority ? device : mediaUri.getEncodedAuthority())
                .encodedPath(mediaUri.getPath())
                .encodedQuery(mediaUri.getQuery())
                .build();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMediaPlayer.release();
        mLibVLC.release();
    }

    @Override
    public void onStart() {
        super.onStart();

        final String url = mSharedPreferences.getString(SettingsFragment.KEY_CURRENT_CHANNEL_LIST_ADDRESS, null);
        final String device = mSharedPreferences.getString(SettingsFragment.KEY_CURRENT_DEVICE, null);
        final boolean ready = url != null && device != null;
        if (!ready)
            ((ChannelsActivity)getActivity()).mBinding.pager.setCurrentItem(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                //VLC player init
                mLibVLC = VLCInstance.get();
                mMediaPlayer = new MediaPlayer(mLibVLC);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setupVout();

                        //Load channels
                        if (ready)
                            loadChannelList(Uri.parse(url), true);
                    }
                });
            }
        }).start();
    }

    private void setupVout() {
        final IVLCVout vlcVout = mMediaPlayer.getVLCVout();
        vlcVout.setVideoView(mBinding.videoSurface);
        if (mSubtitlesSurface != null)
            vlcVout.setSubtitlesView(mSubtitlesSurface);
        vlcVout.attachViews();
        vlcVout.addCallback(ChannelsFragment.this);
        mMediaPlayer.setEventListener(ChannelsFragment.this);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (expanded)
            toggleFullscreen();

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
        if (expanded || mBinding == null)
            return;
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
        Context context = mBinding.getRoot().getContext();
        ((ChannelsActivity)getActivity()).toggleFullscreen(expanded);

        mBinding.getRoot().setBackgroundColor(expanded ?
                ContextCompat.getColor(context, android.R.color.black) :
                ContextCompat.getColor(context, R.color.light_gray));
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mBinding.videoSurfaceFrame.getLayoutParams();
        if (expanded) {
            lp.width = mScreenWidth;
            lp.height = mScreenHeight;
            lp.leftMargin = 0;
            lp.bottomMargin = 0;
            lp.rightMargin = 0;
            lp.topMargin = 0;
        } else {
            if (mHasVout) {
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                int margin = SatIpApplication.get().getResources().getDimensionPixelSize(R.dimen.video_frame_margin);
                lp.leftMargin = margin;
                lp.bottomMargin = margin;
                lp.rightMargin = margin;
                lp.topMargin = margin;
                ((ListAdapter) mBinding.channelList.getAdapter()).blockDpadRight(false);
            } else {
                mBinding.videoSurfaceFrame.setFocusable(false);
                mBinding.videoSurfaceFrame.setVisibility(View.INVISIBLE);
            }
        }
        lp.addRule(RelativeLayout.CENTER_VERTICAL, expanded ? RelativeLayout.TRUE : 0);
        mBinding.channelList.setVisibility(expanded ? View.GONE : View.VISIBLE);
        mBinding.sesLogo.setVisibility(expanded ? View.GONE : View.VISIBLE);
        mBinding.videoSurfaceFrame.setLayoutParams(lp);
        updateVideoSurfaces();
        if (!expanded)
            mBinding.videoSurfaceFrame.post(new Runnable() {
                @Override
                public void run() {
                    focusOnCurrentChannel();
                }
            });
    }

    @Override
    public void onEvent(MediaPlayer.Event event) {
        switch(event.type) {
            case MediaPlayer.Event.Playing:
                Log.d(TAG, "onEvent: Playing");
                if (!mMediaPlayer.getVLCVout().areViewsAttached()) {
                    IVLCVout vout = mMediaPlayer.getVLCVout();
                    vout.setVideoView(mBinding.videoSurface);
                    vout.attachViews();
                }
                mHandler.sendEmptyMessageDelayed(TOGGLE_FULLSCREEN_NO_ES, TIMEOUT_ES_MILLIS);
                mHandler.sendEmptyMessageDelayed(TOGGLE_FULLSCREEN_VOUT_KO, TIMEOUT_VOUT_MILLIS);
                break;
            case MediaPlayer.Event.Stopped:
                Log.d(TAG, "onEvent: Stopped");
                mHasVout = false;
                mBinding.videoSurfaceFrame.setFocusable(false);
                mBinding.videoSurfaceFrame.setVisibility(View.INVISIBLE);
                focusOnCurrentChannel();
                break;
            case MediaPlayer.Event.Vout:
                Log.d(TAG, "onEvent: Vout "+event.getVoutCount());
                mHasVout = event.getVoutCount() > 0;
                mBinding.videoSurfaceFrame.setVisibility(View.VISIBLE);
                mBinding.videoSurfaceFrame.setFocusable(true);
                if (!mHasVout)
                    mHandler.sendEmptyMessage(TOGGLE_FULLSCREEN_VOUT_KO);
                else {
                    mBinding.videoSurface.setVisibility(View.VISIBLE);
                    mHandler.removeMessages(TOGGLE_FULLSCREEN_VOUT_KO);
                }
                if (!expanded) {
                    focusOnCurrentChannel();
                    ((ListAdapter) mBinding.channelList.getAdapter()).blockDpadRight(false);
                }
                break;
            case MediaPlayer.Event.EncounteredError:
                Log.d(TAG, "onEvent: EncounteredError");
                mHasVout = false;
                if (expanded)
                    toggleFullscreen();
                else
                    focusOnCurrentChannel();
                mHandler.sendEmptyMessage(TOGGLE_FULLSCREEN_VOUT_KO);
                break;
            case MediaPlayer.Event.ESAdded:
                Log.d(TAG, "onEvent: ESAdded");
                mHandler.removeMessages(TOGGLE_FULLSCREEN_NO_ES);
                break;
        }
    }

    /*
     * Video surface management
     */

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
        ((ListAdapter)mBinding.channelList.getAdapter()).blockDpadRight(true);
        mHandler.sendEmptyMessage(HIDE_ERROR_MSG);
        Media media = new Media(mLibVLC, uri);
        try {
            mMediaPlayer.setMedia(media);
            mMediaPlayer.play();
        } catch (IllegalStateException e) {
            if (expanded)
                toggleFullscreen();
        } finally {
            media.release();
        }
    }

    public void switchToSiblingChannel(final boolean next) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ListAdapter adapter = (ListAdapter)mBinding.channelList.getAdapter();
                final int newPosition = adapter.getSelectedPosition() + (next ? 1 : -1);
                if (newPosition < 0 || newPosition >= adapter.getItemCount())
                    return;
                play(newPosition, adapter.getItem(newPosition));
                adapter.select(newPosition);
            }
        });
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
    }

    @Override
    public void onSurfacesCreated(IVLCVout vlcVout) {

    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vlcVout) {}

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
