package satipsdk.ses.com.satipsdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import java.util.ArrayList;

import satipsdk.ses.com.satipsdk.adapters.ListAdapter;
import satipsdk.ses.com.satipsdk.databinding.FragmentChannelsBinding;

public class ChannelsFragment extends Fragment implements TabFragment, View.OnFocusChangeListener, View.OnClickListener {

    private static final String TAG = "ChannelsFragment";

    private FragmentChannelsBinding mBinding;
    private ViewDimensions mViewDimensions;
    private int mScreenWidth, mScreenHeight;
    private boolean expanded = false;

    public ChannelsFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowManager windowManager = (WindowManager) getActivity().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        windowManager.getDefaultDisplay().getSize(size);
        mScreenHeight = size.y;
        mScreenWidth = size.x;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_channels, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        ArrayList<ListAdapter.Item> channelList = new ArrayList<>();
        channelList.add(new ListAdapter.Item(ListAdapter.TYPE_CHANNEL, "ZDF HD", null, "rtsp://sat.ip/?src=1&freq=11362&pol=h&ro=0.35&msys=dvbs2&mtype=8psk&plts=on&sr=22000&fec=23&pids=0,17,18,6100,6110,6120,6130", null));
        channelList.add(new ListAdapter.Item(ListAdapter.TYPE_CHANNEL, "RTL Television", null, "rtsp://sat.ip/?src=1&freq=12188&pol=h&ro=0.35&msys=dvbs&mtype=qpsk&plts=off&sr=27500&fec=34&pids=0,17,18,163,104,44,105", null));
        channelList.add(new ListAdapter.Item(ListAdapter.TYPE_CHANNEL, "ZDF HD", null, "rtsp://sat.ip/?src=1&freq=11362&pol=h&ro=0.35&msys=dvbs2&mtype=8psk&plts=on&sr=22000&fec=23&pids=0,17,18,6100,6110,6120,6130", null));
        channelList.add(new ListAdapter.Item(ListAdapter.TYPE_CHANNEL, "RTL Television", null, "rtsp://sat.ip/?src=1&freq=12188&pol=h&ro=0.35&msys=dvbs&mtype=qpsk&plts=off&sr=27500&fec=34&pids=0,17,18,163,104,44,105", null));
        channelList.add(new ListAdapter.Item(ListAdapter.TYPE_CHANNEL, "ZDF HD", null, "rtsp://sat.ip/?src=1&freq=11362&pol=h&ro=0.35&msys=dvbs2&mtype=8psk&plts=on&sr=22000&fec=23&pids=0,17,18,6100,6110,6120,6130", null));
        channelList.add(new ListAdapter.Item(ListAdapter.TYPE_CHANNEL, "RTL Television", null, "rtsp://sat.ip/?src=1&freq=12188&pol=h&ro=0.35&msys=dvbs&mtype=qpsk&plts=off&sr=27500&fec=34&pids=0,17,18,163,104,44,105", null));
        mBinding.channelList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mBinding.channelList.setAdapter(new ListAdapter(channelList));
        mBinding.channelList.getAdapter().notifyDataSetChanged();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            mBinding.videoSurfaceFrame.setOnFocusChangeListener(this);
        mBinding.videoSurfaceFrame.setOnClickListener(this);
        mViewDimensions = new ViewDimensions((ViewGroup.MarginLayoutParams) mBinding.videoSurfaceFrame.getLayoutParams());
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
        expanded = !expanded;
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mBinding.videoSurfaceFrame.getLayoutParams();
        if (expanded) {
            lp.width = RelativeLayout.LayoutParams.MATCH_PARENT;
            lp.height = RelativeLayout.LayoutParams.MATCH_PARENT;
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
        ((ChannelsActivity)getActivity()).toggleFullscreen(expanded);
        mBinding.channelList.setVisibility(expanded ? View.GONE : View.VISIBLE);
        mBinding.sesLogo.setVisibility(expanded ? View.GONE : View.VISIBLE);
        mBinding.videoSurfaceFrame.setLayoutParams(lp);
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
}
