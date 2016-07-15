package satipsdk.ses.com.satipsdk;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;

import org.videolan.libvlc.Media;
import org.videolan.libvlc.util.MediaBrowser;

import java.util.ArrayList;

import satipsdk.ses.com.satipsdk.adapters.ListAdapter;
import satipsdk.ses.com.satipsdk.databinding.ActivityServerBinding;
import satipsdk.ses.com.satipsdk.util.Util;

public class ServerActivity extends AppCompatActivity implements MediaBrowser.EventListener {

    private static final String TAG = "ServerActivity";

    MediaBrowser mMediaBrowser;
    ActivityServerBinding mBinding;
    ListAdapter mListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_server);
        ArrayList<ListAdapter.Item> serverList = new ArrayList<>();
//        serverList.add(new ListAdapter.Item(ListAdapter.TYPE_SERVER, "Astra 19\"2 E", null, "http://www.satip.info/Playlists/ASTRA_19_2E.m3u", null));
//        serverList.add(new ListAdapter.Item(ListAdapter.TYPE_SERVER, "Astra 19\"2 E", null, "http://www.satip.info/Playlists/ASTRA_19_2E.m3u", null));
        mBinding.serverList.setLayoutManager(new LinearLayoutManager(this));
        mListAdapter = new ListAdapter(serverList);
        mBinding.serverList.setAdapter(mListAdapter);
        mListAdapter.notifyDataSetChanged();

    }

    @Override
    protected void onStart() {
        super.onStart();
        mListAdapter.clear();
        if (mMediaBrowser == null)
            mMediaBrowser = new MediaBrowser(VLCInstance.get(), this);
        if (Util.hasLANConnection())
            mMediaBrowser.discoverNetworkShares();
        mBinding.serverList.requestFocus();
    }

    @Override
    protected void onStop() {
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
            mListAdapter.add(i, new ListAdapter.Item(ListAdapter.TYPE_SERVER, media.getMeta(Media.Meta.Title), null, media.getUri().toString(), media.getMeta(Media.Meta.ArtworkURL)));
        }
    }

    @Override
    public void onMediaRemoved(int i, Media media) {
        mListAdapter.remove(i);
    }

    @Override
    public void onBrowseEnd() {
        releaseBrowser();
    }
}
