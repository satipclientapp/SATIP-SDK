package satipsdk.ses.com.satipsdk;

import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
    private static final String KEY_SERVERSS_URLS = "key_server_URLs";
    private FragmentSettingsBinding mBinding;

    MediaBrowser mMediaBrowser;
    ListAdapter mServerListAdapter, mChannelListAdapter;
    private SharedPreferences mSharedPreferences;

    public SettingsFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

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
        mServerListAdapter = new ListAdapter(false);
        mBinding.serverList.setAdapter(mServerListAdapter);
        mServerListAdapter.notifyDataSetChanged();
        // Channels
       mChannelListAdapter = new ListAdapter(false);

        mBinding.channelList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mBinding.channelList.setAdapter(mChannelListAdapter);

    }

    @Override
    public String getTitle() {
        return SatIpApplication.get().getString(R.string.settings);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshServers();
        if (mChannelListAdapter.getItemCount() > 0)
            refreshChannels();
    }

    private void refreshServers() {
        mServerListAdapter.clear();
        Set<String> prefListsNames = mSharedPreferences.getStringSet(KEY_SERVERS_NAMES, null);
        Set<String> prefListsUrls = mSharedPreferences.getStringSet(KEY_SERVERSS_URLS, null);
        if (prefListsNames != null && prefListsUrls != null) {
            Object[] names = prefListsNames.toArray();
            Object[] urls = prefListsUrls.toArray();
            for (int i = 0; i<prefListsNames.size(); ++i)
                mServerListAdapter.add(new ListAdapter.Item(ListAdapter.TYPE_SERVER, (String) names[i], null, (String) urls[i], null));
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
        if (prefListsNames == null || prefListsUrls == null) {
            mChannelListAdapter.add(new ListAdapter.Item(ListAdapter.TYPE_CHANNEL_LIST, "Astra 19°2E", null, "http://www.satip.info/Playlists/ASTRA_19_2E.m3u", null));
            mChannelListAdapter.add(new ListAdapter.Item(ListAdapter.TYPE_CHANNEL_LIST, "Astra 28°2E", null, "http://www.satip.info/Playlists/ASTRA_28_2E.m3u", null));
            mChannelListAdapter.add(new ListAdapter.Item(ListAdapter.TYPE_CHANNEL_LIST, "Astra 23°5E", null, "http://www.satip.info/Playlists/ASTRA_23_5E.m3u", null));
        } else {
            Object[] names = prefListsNames.toArray();
            Object[] urls = prefListsUrls.toArray();
            for (int i = 0; i<prefListsNames.size(); ++i)
                mChannelListAdapter.add(new ListAdapter.Item(ListAdapter.TYPE_CHANNEL_LIST, (String) names[i], null, (String) urls[i], null));
        }
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
            chanListNames.add(item.title);
            chanListUrls.add(item.url);
        }
        Set<String> serverNames = new HashSet<>();
        Set<String> serverUrls = new HashSet<>();
        items = ((ListAdapter)mBinding.serverList.getAdapter()).getAll();
        for (ListAdapter.Item item : items) {
            if (item.type != ListAdapter.TYPE_SERVER_CUSTOM)
                continue;
            serverNames.add(item.title);
            serverUrls.add(item.url);
        }
        mSharedPreferences.edit()
                .putStringSet(KEY_CHANNELS_NAMES, chanListNames)
                .putStringSet(KEY_CHANNELS_URLS, chanListUrls)
                .putStringSet(KEY_SERVERS_NAMES, serverNames)
                .putStringSet(KEY_SERVERSS_URLS, serverUrls)
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

    private ClickHandler mClickHandler = new ClickHandler();
    public class ClickHandler {
        public void openChannelsDialog(View v) {
            new ItemListDialog(ListAdapter.TYPE_CHANNEL_LIST, mScb).show(getActivity().getSupportFragmentManager(), "add_channels_dialog");
        }
        public void openServerDialog(View v) {
            new ItemListDialog(ListAdapter.TYPE_SERVER_CUSTOM, mScb).show(getActivity().getSupportFragmentManager(), "add_channels_dialog");
        }
    }

    private SettingsCb mScb = new SettingsCb();
    public class SettingsCb {
        public void addItem(int type, String name, String url) {
            ListAdapter adapter = (ListAdapter) (type == ListAdapter.TYPE_CHANNEL_LIST ?
                                mBinding.channelList.getAdapter() : mBinding.serverList.getAdapter());
            adapter.add(new ListAdapter.Item(type, name, null, url, null));
        }
    }
}
