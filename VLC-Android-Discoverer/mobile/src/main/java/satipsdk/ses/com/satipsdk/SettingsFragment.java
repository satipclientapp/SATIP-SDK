package satipsdk.ses.com.satipsdk;

import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaList;
import org.videolan.libvlc.util.MediaBrowser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import satipsdk.ses.com.satipsdk.adapters.ListAdapter;
import satipsdk.ses.com.satipsdk.databinding.FragmentSettingsBinding;
import satipsdk.ses.com.satipsdk.dialogs.ItemListDialog;
import satipsdk.ses.com.satipsdk.util.Util;

public class SettingsFragment extends Fragment implements TabFragment, MediaBrowser.EventListener {

    private static final String TAG = "SettingsFragment";

    private static final String KEY_CHANNELS_NAMES = "key_channels_names";
    private static final String KEY_CHANNELS_URLS = "key_channels_URLs";
    private static final String KEY_SERVERS_NAMES = "key_server_names";
    private static final String KEY_SERVERS_URLS = "key_server_URLs";
    public static final String KEY_CURRENT_DEVICE = "key_current_device";
    public static final String KEY_CURRENT_CHANNEL_LIST_ADDRESS = "key_current_channel_list_address";
    public static final String KEY_SELECTED_DEVICE = "key_selected_device";
    public static final String KEY_SELECTED_CHANNEL_LIST = "key_selected_channel_list";
    public static final String KEY_SELECTED_CHANNEL = "key_selected_channel";
    public static final String KEY_LAST_CHANNEL_URL = "key_last_channel_url";

    private FragmentSettingsBinding mBinding;

    MediaBrowser mMediaBrowser;
    ListAdapter mServerListAdapter, mChannelListAdapter;
    private SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(SatIpApplication.get());

    public SettingsFragment() {}

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings, container, false);
        mBinding.setHandler(mClickHandler);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Servers
        mBinding.serverList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mServerListAdapter = new ListAdapter();
        mBinding.serverList.setAdapter(mServerListAdapter);
        mBinding.serverList.setItemAnimator(null);
        mServerListAdapter.notifyDataSetChanged();
        mServerListAdapter.setItemClickHandler(mServerListClickCb);
        // Channels List
        mChannelListAdapter = new ListAdapter();
        mBinding.channelList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mBinding.channelList.setAdapter(mChannelListAdapter);
        mBinding.channelList.setItemAnimator(null);
        mChannelListAdapter.setItemClickHandler(mChannelListClickCb);
        //Channels display
        mBinding.channelDisplayList.setAdapter(new ListAdapter());
        mBinding.channelDisplayList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mBinding.channelDisplayList.setItemAnimator(null);
    }

    @Override
    public String getTitle() {
        return SatIpApplication.get().getString(R.string.settings);
    }

    @Override
    public void onPageSelected() {}

    @Override
    public void onResume() {
        super.onResume();

        final String url = mSharedPreferences.getString(SettingsFragment.KEY_CURRENT_CHANNEL_LIST_ADDRESS, null);
        final String device = mSharedPreferences.getString(SettingsFragment.KEY_CURRENT_DEVICE, null);
        if (url == null || device == null)
            Snackbar.make(getView(), R.string.warning_list_selection, Snackbar.LENGTH_LONG).show();
        else
            parseChannelList(Uri.parse(url));
        refreshServers();
        refreshChannels();
        mServerListAdapter.select(mSharedPreferences.getInt(KEY_SELECTED_DEVICE, -1));
        mChannelListAdapter.select(mSharedPreferences.getInt(KEY_SELECTED_CHANNEL_LIST, -1));
    }

    private void refreshServers() {
        mServerListAdapter.clear();
        Set<String> prefListsNames = mSharedPreferences.getStringSet(KEY_SERVERS_NAMES, null);
        Set<String> prefListsUrls = mSharedPreferences.getStringSet(KEY_SERVERS_URLS, null);
        if (prefListsNames != null && prefListsUrls != null) {
            Object[] names = prefListsNames.toArray();
            Object[] urls = prefListsUrls.toArray();
            for (int i = 0; i<prefListsNames.size(); ++i)
                mServerListAdapter.add(new ListAdapter.Item(ListAdapter.TYPE_SERVER_CUSTOM, (String) names[i], Uri.parse((String) urls[i]), null));
        }
        if (mMediaBrowser == null)
            mMediaBrowser = new MediaBrowser(VLCInstance.get(), this);
        if (Util.hasLANConnection())
            mMediaBrowser.discoverNetworkShares();
        mBinding.serverList.requestFocus();
    }

    private void refreshChannels() {
        mChannelListAdapter.clear();
        Set<String> prefListsNames = mSharedPreferences.getStringSet(KEY_CHANNELS_NAMES, null);
        Set<String> prefListsUrls = mSharedPreferences.getStringSet(KEY_CHANNELS_URLS, null);

        mChannelListAdapter.add(new ListAdapter.Item(ListAdapter.TYPE_CHANNEL_LIST, "Astra 19.2°E", Uri.parse("http://www.satip.info/Playlists/ASTRA_19_2E.m3u"), null));
        mChannelListAdapter.add(new ListAdapter.Item(ListAdapter.TYPE_CHANNEL_LIST, "Astra 28.2°E", Uri.parse("http://www.satip.info/Playlists/ASTRA_28_2E.m3u"), null));
        if (!Util.isCollectionEmpty(prefListsNames) && !Util.isCollectionEmpty(prefListsUrls)) {
            Object[] names = prefListsNames.toArray();
            Object[] urls = prefListsUrls.toArray();
            for (int i = 0; i<prefListsNames.size(); ++i)
                mChannelListAdapter.add(new ListAdapter.Item(ListAdapter.TYPE_CHANNEL_LIST_CUSTOM, (String) names[i], Uri.parse((String) urls[i]), null));
        }
        mChannelListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        releaseBrowser();
    }

    @Override
    public void onStop() {
        super.onStop();
        saveItems();
    }

    private void saveItems() {
        ArrayList<ListAdapter.Item> items = ((ListAdapter)mBinding.channelList.getAdapter()).getAll();
        Set<String> chanListNames = new HashSet<>();
        Set<String> chanListUrls = new HashSet<>();
        for (ListAdapter.Item item : items) {
            if (item.type != ListAdapter.TYPE_CHANNEL_LIST_CUSTOM)
                continue;
            chanListNames.add(item.title);
            chanListUrls.add(item.uri.toString());
        }
        Set<String> serverNames = new HashSet<>();
        Set<String> serverUrls = new HashSet<>();
        items = ((ListAdapter)mBinding.serverList.getAdapter()).getAll();
        for (ListAdapter.Item item : items) {
            if (item.type != ListAdapter.TYPE_SERVER_CUSTOM)
                continue;
            serverNames.add(item.title);
            serverUrls.add(item.uri.toString());
        }
        //Selected positions
        int selectedServer = mServerListAdapter.getSelectedPosition();
        int selectedChannelList = mChannelListAdapter.getSelectedPosition();
        mSharedPreferences.edit()
                .putStringSet(KEY_CHANNELS_NAMES, chanListNames)
                .putStringSet(KEY_CHANNELS_URLS, chanListUrls)
                .putStringSet(KEY_SERVERS_NAMES, serverNames)
                .putStringSet(KEY_SERVERS_URLS, serverUrls)
                .putInt(KEY_SELECTED_DEVICE, selectedServer)
                .putInt(KEY_SELECTED_CHANNEL_LIST, selectedChannelList)
                .apply();
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
            mServerListAdapter.add(i, new ListAdapter.Item(ListAdapter.TYPE_SERVER, media.getMeta(Media.Meta.Title), media.getUri(), media.getMeta(Media.Meta.ArtworkURL)));
        }
    }

    @Override
    public void onMediaRemoved(int i, Media media) {
        mServerListAdapter.removeServer(i);
    }

    @Override
    public void onBrowseEnd() {
        releaseBrowser();
    }

    private Handler mHandler = new Handler();
    private ListAdapter.ItemClickCb mServerListClickCb = new ListAdapter.ItemClickCb() {
        @Override
        public void onItemClick(int position, ListAdapter.Item item) {
            mSharedPreferences.edit().putString(KEY_CURRENT_DEVICE, item.uri.getQuery()).apply();
            reloadChannels();
        }
    };
    private ListAdapter.ItemClickCb mChannelListClickCb = new ListAdapter.ItemClickCb() {
        @Override
        public void onItemClick(final int position, final ListAdapter.Item item) {
            mSharedPreferences.edit().putString(KEY_CURRENT_CHANNEL_LIST_ADDRESS, item.uri.toString()).apply();
            reloadChannels();
            parseChannelList(item.uri);
        }
    };

    private void parseChannelList(final Uri uri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Media playlist = new Media(VLCInstance.get(), uri);
                playlist.parse(Media.Parse.ParseNetwork);
                final MediaList ml = playlist.subItems();
                playlist.release();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        ListAdapter la = (ListAdapter) mBinding.channelDisplayList.getAdapter();
                        la.clear();
                        Media media;
                        String title;
                        for (int i = 0; i < ml.getCount(); ++i) {
                            media = ml.getMediaAt(i);
                            title = media.getMeta(Media.Meta.Title);
                            int dot = title.indexOf('.');
                            la.add(new ListAdapter.Item(ListAdapter.TYPE_CHANNEL,
                                    media.getMeta(Media.Meta.Title).substring(dot + 2),
                                    media.getUri(),
                                    null));
                        }
                    }
                });
            }
        }).start();
    }

    private void reloadChannels() {
        final String url = mSharedPreferences.getString(SettingsFragment.KEY_CURRENT_CHANNEL_LIST_ADDRESS, null);
        final String device = mSharedPreferences.getString(SettingsFragment.KEY_CURRENT_DEVICE, null);
        mSharedPreferences.edit().putInt(KEY_SELECTED_CHANNEL, 0).apply();
        if (url == null || device == null)
            return;
        ChannelsFragment cf = (ChannelsFragment) ((ChannelsActivity)getActivity()).mFragments[0];
        cf.stopPlayback();
        cf.loadChannelList(Uri.parse(url+"?"+device), false);
    }

    private ClickHandler mClickHandler = new ClickHandler();
    public class ClickHandler {
        public void openChannelsDialog(View v) {
            new ItemListDialog(ListAdapter.TYPE_CHANNEL_LIST_CUSTOM, mScb).show(getActivity().getSupportFragmentManager(), "add_channels_dialog");
        }
        public void openServerDialog(View v) {
            new ItemListDialog(ListAdapter.TYPE_SERVER_CUSTOM, mScb).show(getActivity().getSupportFragmentManager(), "add_channels_dialog");
        }
    }

    private SettingsCb mScb = new SettingsCb();
    public class SettingsCb {
        public void addItem(int type, String name, Uri uri) {
            ListAdapter adapter = (ListAdapter) (type == ListAdapter.TYPE_CHANNEL_LIST_CUSTOM ?
                    mBinding.channelList.getAdapter() : mBinding.serverList.getAdapter());
            adapter.add(new ListAdapter.Item(type, name, uri, null));
        }
    }
}
