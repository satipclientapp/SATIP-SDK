package satipsdk.ses.com.satipsdk.adapters;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import satipsdk.ses.com.satipsdk.ChannelsActivity;
import satipsdk.ses.com.satipsdk.R;
import satipsdk.ses.com.satipsdk.databinding.ListItemBinding;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> implements View.OnFocusChangeListener{

    private static final String TAG = "ListAdapter";

    public static final int TYPE_SERVER = 0;
    public static final int TYPE_CHANNEL = 1;

    ArrayList<Item> mItemList;
    LayoutInflater mInflater;
    SparseIntArray mItemsIndex = new SparseIntArray();

    public ListAdapter(ArrayList<Item> serverList) {
        super();
        mItemList = serverList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mInflater == null)
            mInflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return new ViewHolder((ListItemBinding) DataBindingUtil.inflate(mInflater, R.layout.list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.binding.setItem(mItemList.get(position));
        if (mItemList.get(position).logoUrl != null)
            Glide.with(holder.itemView.getContext())
            .load(holder.binding.getItem().logoUrl)
            .fitCenter()
            .crossFade()
            .into(holder.binding.itemLogo);
        holder.itemView.requestFocus();
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
        int actualPosition = mItemsIndex.get(position);
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

    @Override
    public int getItemCount() {
        return mItemList.size();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus)
            v.setBackground(v.getResources().getDrawable(R.drawable.gradient_cloud));
        else
            v.setBackgroundColor(v.getResources().getColor(R.color.pure_white));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            v.setElevation(hasFocus ? 10.0f : 0.0f);
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ListItemBinding binding;

        public ViewHolder(ListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.getRoot().setOnClickListener(this);
            binding.getRoot().setOnFocusChangeListener(ListAdapter.this);
        }

        @Override
        public void onClick(View v) {
            if (binding.getItem().type == TYPE_SERVER) {
                v.getContext().startActivity(new Intent(v.getContext(), ChannelsActivity.class).setData(Uri.parse(binding.getItem().url)));
            } else {
                //TODO play stream
            }
        }
    }

    public static class Item {
        public int type;
        public String title, description, logoUrl, url;

        public Item(int type, String title, String description, String url, String logoUrl) {
            this.type = type;
            this.title = title;
            this.description = description;
            this.logoUrl = type == TYPE_CHANNEL ? generateLogoUrl(title) : logoUrl;
            this.url = url;
        }

        String generateLogoUrl(String title) {
            StringBuilder sb = new StringBuilder("http://www.satip.info/sites/satip/files/files/Playlists/Channellogos/")
                    .append(title.replace(' ', '-').replace('.', '-').toLowerCase())
                    .append(".png");
            return sb.toString();
        }
    }
}
