package satipsdk.ses.com.satipsdk.dialogs;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import satipsdk.ses.com.satipsdk.R;
import satipsdk.ses.com.satipsdk.SettingsFragment;
import satipsdk.ses.com.satipsdk.adapters.ListAdapter;
import satipsdk.ses.com.satipsdk.databinding.DialogItemListBinding;

public class ItemListDialog extends DialogFragment {

    DialogItemListBinding mBinding;
    SettingsFragment.SettingsCb mCb;
    int mType;


    public ItemListDialog(int type, SettingsFragment.SettingsCb cb) {
        mCb = cb;
        mType = type;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_item_list, container, false);
        mBinding.setHandler(mClickHandler);
        if (mType == ListAdapter.TYPE_SERVER_CUSTOM) {
            mBinding.nameLayout.setHint(getString(R.string.name_hint_server));
            mBinding.urlLayout.setHint(getString(R.string.url_hint_server));
        }
        return mBinding.getRoot();
    }

    @NonNull
    @Override
    public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
        AppCompatDialog dialog = new AppCompatDialog(getActivity(), getTheme());
        dialog.setTitle(getString(mType == ListAdapter.TYPE_CHANNEL_LIST_CUSTOM ? R.string.add_channel : R.string.add_server) );

        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    private ItemListDialog.ClickHandler mClickHandler = new ClickHandler();
    public class ClickHandler {
        public void onClick(View v) {
            String name = mBinding.nameEdittext.getText().toString();
            String url = mBinding.urlEdittext.getText().toString();
            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(url))
                return;
            mCb.addItem(mType, name, url);
            dismiss();
        }
    }
}
