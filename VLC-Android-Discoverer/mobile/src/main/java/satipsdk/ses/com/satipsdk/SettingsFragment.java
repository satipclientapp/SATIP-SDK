package satipsdk.ses.com.satipsdk;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.videolan.libvlc.Media;
import org.videolan.libvlc.util.MediaBrowser;

import java.util.ArrayList;

import satipsdk.ses.com.satipsdk.adapters.ListAdapter;
import satipsdk.ses.com.satipsdk.databinding.FragmentSettingsBinding;
import satipsdk.ses.com.satipsdk.util.Util;

public class SettingsFragment extends Fragment implements TabFragment, MediaBrowser.EventListener {

    private FragmentSettingsBinding mBinding;

    MediaBrowser mMediaBrowser;
    ListAdapter mServerListAdapter, mChannelListAdapter;

    public SettingsFragment() {}

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Servers
        ArrayList<ListAdapter.Item> serverList = new ArrayList<>();
//        serverList.add(new ListAdapter.Item(ListAdapter.TYPE_SERVER, "Astra 19\"2 E", null, "http://www.satip.info/Playlists/ASTRA_19_2E.m3u", null));
//        serverList.add(new ListAdapter.Item(ListAdapter.TYPE_SERVER, "Astra 19\"2 E", null, "http://www.satip.info/Playlists/ASTRA_19_2E.m3u", null));
        mBinding.serverList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mServerListAdapter = new ListAdapter(serverList);
        mBinding.serverList.setAdapter(mServerListAdapter);
        mServerListAdapter.notifyDataSetChanged();
        // Channels
        ArrayList<ListAdapter.Item> channelList = new ArrayList<>();
        channelList.add(new ListAdapter.Item(ListAdapter.TYPE_CHANNEL, "ZDF HD", null, "rtsp://sat.ip/?src=1&freq=11362&pol=h&ro=0.35&msys=dvbs2&mtype=8psk&plts=on&sr=22000&fec=23&pids=0,17,18,6100,6110,6120,6130", null));
        channelList.add(new ListAdapter.Item(ListAdapter.TYPE_CHANNEL, "RTL Television", null, "rtsp://sat.ip/?src=1&freq=12188&pol=h&ro=0.35&msys=dvbs&mtype=qpsk&plts=off&sr=27500&fec=34&pids=0,17,18,163,104,44,105", null));
        channelList.add(new ListAdapter.Item(ListAdapter.TYPE_CHANNEL, "ZDF HD", null, "rtsp://sat.ip/?src=1&freq=11362&pol=h&ro=0.35&msys=dvbs2&mtype=8psk&plts=on&sr=22000&fec=23&pids=0,17,18,6100,6110,6120,6130", null));
        channelList.add(new ListAdapter.Item(ListAdapter.TYPE_CHANNEL, "RTL Television", null, "rtsp://sat.ip/?src=1&freq=12188&pol=h&ro=0.35&msys=dvbs&mtype=qpsk&plts=off&sr=27500&fec=34&pids=0,17,18,163,104,44,105", null));
        channelList.add(new ListAdapter.Item(ListAdapter.TYPE_CHANNEL, "ZDF HD", null, "rtsp://sat.ip/?src=1&freq=11362&pol=h&ro=0.35&msys=dvbs2&mtype=8psk&plts=on&sr=22000&fec=23&pids=0,17,18,6100,6110,6120,6130", null));
        channelList.add(new ListAdapter.Item(ListAdapter.TYPE_CHANNEL, "RTL Television", null, "rtsp://sat.ip/?src=1&freq=12188&pol=h&ro=0.35&msys=dvbs&mtype=qpsk&plts=off&sr=27500&fec=34&pids=0,17,18,163,104,44,105", null));
        mBinding.channelList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mChannelListAdapter = new ListAdapter(channelList);
        mBinding.channelList.setAdapter(mChannelListAdapter);
        mChannelListAdapter.notifyDataSetChanged();

    }

    @Override
    public String getTitle() {
        return SatIpApplication.get().getString(R.string.settings);
    }

    @Override
    public void onStart() {
        super.onStart();
        mServerListAdapter.clear();
        if (mMediaBrowser == null)
            mMediaBrowser = new MediaBrowser(VLCInstance.get(), this);
        if (Util.hasLANConnection())
            mMediaBrowser.discoverNetworkShares();
        mBinding.serverList.requestFocus();
    }

    @Override
    public void onStop() {
        super.onStop();
        releaseBrowser();
    }

    private void releaseBrowser() {
        if (mMediaBrowser != null) {
            mMediaBrowser.release();
            mMediaBrowser = null;
        }
    }

    @Override
    public void onMediaAdded(int i, Media media) {
        if (TextUtils.equals(media.getMeta(Media.Meta.Setting), "urn:ses-com:device:SatIPServer:1")) {
            mServerListAdapter.add(i, new ListAdapter.Item(ListAdapter.TYPE_SERVER, media.getMeta(Media.Meta.Title), null, media.getUri().toString(), media.getMeta(Media.Meta.ArtworkURL)));
        }
    }

    @Override
    public void onMediaRemoved(int i, Media media) {
        mServerListAdapter.remove(i);
    }

    @Override
    public void onBrowseEnd() {
        releaseBrowser();
    }
}
