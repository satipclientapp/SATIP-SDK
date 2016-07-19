package satipsdk.ses.com.satipsdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaList;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.HWDecoderUtil;

import java.util.ArrayList;

import satipsdk.ses.com.satipsdk.adapters.ListAdapter;
import satipsdk.ses.com.satipsdk.databinding.FragmentChannelsBinding;

public class ChannelsFragment extends Fragment implements TabFragment, ListAdapter.ItemClickCb, View.OnFocusChangeListener, View.OnClickListener, IVLCVout.Callback {

    private static final String TAG = "ChannelsFragment";
    private static final boolean ENABLE_SUBTITLES = true;

    private FragmentChannelsBinding mBinding;
    private ViewDimensions mViewDimensions;
    private boolean expanded = false;
    private int mScreenWidth, mScreenHeight;

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
        args.add("-vvv"); //Set VLC to verbose
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

        new Thread(new Runnable() {
            @Override
            public void run() {
                loadChannelList();
            }
        }).start();

        if (ENABLE_SUBTITLES && HWDecoderUtil.HAS_SUBTITLES_SURFACE) {
            final ViewStub stub = (ViewStub) view.findViewById(R.id.subtitles_stub);
            mSubtitlesSurface = (SurfaceView) stub.inflate();
            mSubtitlesSurface.setZOrderMediaOverlay(true);
            mSubtitlesSurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        }

        ArrayList<ListAdapter.Item> channelList = new ArrayList<>();
        mBinding.channelList.setLayoutManager(new LinearLayoutManager(getActivity()));
        ListAdapter channelsAdapter = new ListAdapter(channelList, true);
        channelsAdapter.setItemClickHandler(this);
        mBinding.channelList.setAdapter(channelsAdapter);
        channelsAdapter.notifyDataSetChanged();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            mBinding.videoSurfaceFrame.setOnFocusChangeListener(this);
        mBinding.videoSurfaceFrame.setOnClickListener(this);
    }

    private void loadChannelList() {
        Uri uri = getActivity().getIntent().getData();
        Media playlist = new Media(mLibVLC, uri);
        playlist.parse(Media.Parse.ParseNetwork);
        final MediaList ml = playlist.subItems();
        playlist.release();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ListAdapter la = (ListAdapter) mBinding.channelList.getAdapter();
                Media media;
                String title;
                for (int i = 0; i< ml.getCount(); ++i) {
                    media = ml.getMediaAt(i);
                    title = media.getMeta(Media.Meta.Title);
                    int dot = title.indexOf('.');
                    la.add(new ListAdapter.Item(ListAdapter.TYPE_CHANNEL,
                            media.getMeta(Media.Meta.Title).substring(dot+2),
                            "channel description",
                            media.getUri().toString(),
                            null));
                }
            }
        });
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

        if (mOnLayoutChangeListener == null) {
            mOnLayoutChangeListener = new View.OnLayoutChangeListener() {
                private final Runnable mRunnable = new Runnable() {
                    @Override
                    public void run() {
                        updateVideoSurfaces();
                    }
                };
                @Override
                public void onLayoutChange(View v, int left, int top, int right,
                                           int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                        mHandler.removeCallbacks(mRunnable);
                        mHandler.post(mRunnable);
                    }
                }
            };
        }
        mBinding.videoSurfaceFrame.addOnLayoutChangeListener(mOnLayoutChangeListener);
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
    }

    @Override
    public String getTitle() {
        return SatIpApplication.get().getString(R.string.channels);
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

    public void toggleFullscreen() {
        if (mViewDimensions == null) //Display is not ready
            return;
        expanded = !expanded;
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mBinding.videoSurfaceFrame.getLayoutParams();
        Resources res = mBinding.getRoot().getContext().getResources();
        mBinding.getRoot().setBackgroundColor(expanded ? res.getColor(android.R.color.black) : res.getColor(R.color.light_gray));
        if (expanded) {
            float sar = mViewDimensions.videoHeight / (float) mViewDimensions.videoWidth;
            lp.width = mScreenWidth;
            lp.height = (int) (mScreenWidth * sar);
            lp.leftMargin = 0;
            lp.bottomMargin = 0;
            lp.rightMargin = 0;
            lp.topMargin = 0;
        } else {
            lp.width = mViewDimensions.videoWidth;
            lp.height = mViewDimensions.videoHeight;
            lp.leftMargin = mViewDimensions.leftMargin;
            lp.bottomMargin = mViewDimensions.bottomMargin;
            lp.rightMargin = mViewDimensions.rightMargin;
            lp.topMargin = mViewDimensions.topMargin;
        }
        mVideoWidth = mVideoVisibleWidth = lp.width;
        mVideoHeight = mVideoVisibleHeight = lp.height;
        lp.addRule(RelativeLayout.CENTER_IN_PARENT, expanded ? RelativeLayout.TRUE : 0);
        ((ChannelsActivity)getActivity()).toggleFullscreen(expanded);
        mBinding.channelList.setVisibility(expanded ? View.GONE : View.VISIBLE);
        mBinding.sesLogo.setVisibility(expanded ? View.GONE : View.VISIBLE);
        mBinding.videoSurfaceFrame.setLayoutParams(lp);
        mBinding.videoSurfaceFrame.post(new Runnable() {
            @Override
            public void run() {
                updateVideoSurfaces();
            }
        });
    }

    class ViewDimensions {
        public int videoWidth, videoHeight, leftMargin, bottomMargin, rightMargin, topMargin;

        ViewDimensions(ViewGroup.MarginLayoutParams lp) {
            videoWidth = lp.width;
            videoHeight = lp.height;
            leftMargin = lp.leftMargin;
            bottomMargin = lp.bottomMargin;
            rightMargin = lp.rightMargin;
            topMargin = lp.topMargin;
        }
    }

    /*
     * Video ItemClickCb
     */

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

    public void onItemClick(int position, ListAdapter.Item item) {
        Media media = new Media(mLibVLC, Uri.parse(item.url));
        mMediaPlayer.setMedia(media);
        media.release();
        mMediaPlayer.play();
    }

    private void updateVideoSurfaces() {
        if (mVideoWidth * mVideoHeight == 0 || getActivity() == null)
            return;
        int sw = expanded ? mScreenWidth : mBinding.videoSurfaceFrame.getWidth();
        int var = mVideoWidth / mVideoHeight;
        int sh = expanded ? mScreenHeight : mBinding.videoSurfaceFrame.getWidth() / var;

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
        lp.width  = (int) Math.ceil(dw * mVideoWidth / mVideoVisibleWidth);
        lp.height = (int) Math.ceil(dh * mVideoHeight / mVideoVisibleHeight);
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
        mVideoWidth = width;
        mVideoHeight = height;
        mVideoVisibleWidth = visibleWidth;
        mVideoVisibleHeight = visibleHeight;
        mVideoSarNum = sarNum;
        mVideoSarDen = sarDen;
        updateVideoSurfaces();
        mViewDimensions = new ViewDimensions((ViewGroup.MarginLayoutParams) mBinding.videoSurfaceFrame.getLayoutParams());
    }

    @Override
    public void onSurfacesCreated(IVLCVout vlcVout) {}

    @Override
    public void onSurfacesDestroyed(IVLCVout vlcVout) {}

    @Override
    public void onHardwareAccelerationError(IVLCVout vlcVout) {}
}
