package satipsdk.ses.com.satipsdk.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import satipsdk.ses.com.satipsdk.R;
import satipsdk.ses.com.satipsdk.SatIpApplication;
import satipsdk.ses.com.satipsdk.databinding.ListItemBinding;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

    private static final String TAG = "ListAdapter";

    private static final int COLOR_PURE_WHITE = SatIpApplication.get().getResources().getColor(R.color.pure_white);

    public static final int TYPE_SERVER = 0;
    public static final int TYPE_CHANNEL = 1;
    public static final int TYPE_CHANNEL_LIST = 2;
    public static final int TYPE_SERVER_CUSTOM = 3;
    public static final int TYPE_CHANNEL_LIST_CUSTOM = 4;

    private ArrayList<Item> mItemList;
    private LayoutInflater mInflater;
    private SparseIntArray mItemsIndex = new SparseIntArray();
    private ItemClickCb mItemClickCb;
    private int mSelectedPosition = -1;

    public interface ItemClickCb {
        void onItemClick(int position, Item item);
    }

    public ListAdapter() {
        this(new ArrayList<Item>());
    }

    public ListAdapter(ArrayList<Item> serverList) {
        super();
        mItemList = serverList;
    }

    public void setItemClickHandler(ItemClickCb itemClickCb) {
        mItemClickCb = itemClickCb;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mInflater == null)
            mInflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return new ViewHolder((ListItemBinding) DataBindingUtil.inflate(mInflater, R.layout.list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Item item = mItemList.get(position);
        holder.binding.setItem(item);
        setItemViewBackground(holder.itemView, position);
        if (item.logoUrl != null)
            Glide.with(holder.itemView.getContext())
            .load(item.logoUrl)
            .fitCenter()
            .crossFade()
            .into(holder.binding.itemLogo);
        else if (item.type == TYPE_CHANNEL_LIST) {
            holder.binding.itemLogo.setVisibility(View.VISIBLE);
            holder.binding.itemLogo.setImageResource(R.drawable.ses_logo);
        } else
            holder.binding.itemLogo.setVisibility(View.GONE);
    }

    private void setItemViewBackground(View view, int position) {
        if (mSelectedPosition == position)
            view.setBackgroundResource(R.drawable.gradient_cloud);
        else
            view.setBackgroundColor(COLOR_PURE_WHITE);
    }

    public void add(Item item) {
        mItemList.add(item);
        notifyItemInserted(mItemList.size()-1);
    }

    public void add(int position, Item item) {
        int actualPosition = mItemList.size();
        mItemList.add(item);
        mItemsIndex.put(position, actualPosition);
        notifyItemInserted(actualPosition);
    }

    public void remove(int position) {
        mItemList.remove(position);
        notifyItemRemoved(position);
    }

    public void removeServer(int position) {
        int actualPosition = mItemsIndex.get(position);
        if (actualPosition >= mItemList.size() || actualPosition < 0)
            return;
        mItemList.remove(actualPosition);
        notifyItemRemoved(actualPosition);
        mItemsIndex.delete(position);
    }

    public void clear() {
        if (mItemList.isEmpty())
            return;
        mItemList.clear();
        notifyDataSetChanged();
    }

    public ArrayList<Item> getAll() {
        return mItemList;
    }

    @Override
    public int getItemCount() {
        return mItemList.size();
    }

    public Item getItem(int position) {
        return mItemList.get(position);
    }

    public Item getNextItem() {
        return mItemList.get(mSelectedPosition+1);
    }

    public Item getPreviousItem() {
        return mItemList.get(mSelectedPosition-1);
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener, View.OnFocusChangeListener {
        ListItemBinding binding;

        public ViewHolder(ListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            binding.itemDelete.setOnClickListener(this);
            itemView.setOnFocusChangeListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mItemClickCb != null) {
                if (v.getId() == itemView.getId()) {
                    v.setFocusableInTouchMode(true);
                    v.requestFocus();
                    select(getAdapterPosition());
                    mItemClickCb.onItemClick(getAdapterPosition(), binding.getItem());
                } else if (v.getId() == binding.itemDelete.getId()) {
                    delete(v);
                }
            }
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (mSelectedPosition != getAdapterPosition()) {
                v.setBackgroundResource(hasFocus ? R.drawable.background_light_gray : R.drawable.background_pure_white);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                v.setElevation(hasFocus ? 10.0f : 0.0f);
            if (!hasFocus)
                v.setFocusableInTouchMode(false);
        }

        private void delete(View v) {
            final int position = getAdapterPosition();
            final Item item = binding.getItem();
            remove(position);
            Snackbar.make(v, item.type == TYPE_SERVER_CUSTOM ? R.string.server_removed : R.string.channel_list_removed,
                    Snackbar.LENGTH_LONG).setAction(android.R.string.cancel, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    add(position, item);
                }
            }).show();
        }

        @Override
        public boolean onLongClick(final View v) {
            int type = binding.getItem().type;
            if (type != TYPE_CHANNEL_LIST_CUSTOM && type != TYPE_SERVER_CUSTOM)
                return false;
            Resources res = v.getResources();
            String mediaType = type == TYPE_CHANNEL_LIST_CUSTOM ?
                    res.getString(R.string.channel_list)
                    : res.getString(R.string.server);
            new AlertDialog.Builder(v.getContext(), R.style.AlertDialogStyle)
                    .setTitle(String.format(res.getString(R.string.delete_item), binding.getItem().title))
                    .setMessage(String.format(res.getString(R.string.delete_message), mediaType))
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            delete(v);
                        }
                    })
                    .show();
            return true;
        }
    }

    public void select(int position) {
        if (position == -1)
            return;
        int previous = mSelectedPosition;
        mSelectedPosition = position;
        notifyItemChanged(previous);
        notifyItemChanged(position);
    }

    public int getSelectedPosition() {
        return mSelectedPosition;
    }

    public static class Item {
        public int type;
        public String title, logoUrl;
        public Uri uri;

        public Item(int type, String title, Uri uri, String logoUrl) {
            this.type = type;
            this.title = title;
            this.logoUrl = type == TYPE_CHANNEL ? generateLogoUrl(title) : logoUrl;
            this.uri = uri;
        }

        String generateLogoUrl(String title) {
            StringBuilder sb = new StringBuilder("http://www.satip.info/Playlists/Channellogos/")
                    .append(title.replace(' ', '-').replace('.', '-').toLowerCase())
                    .append(".png");
            return sb.toString();
        }
    }
}
