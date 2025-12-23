package com.example.myfirstapplication.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myfirstapplication.R;
import com.example.myfirstapplication.model.ChatSummary;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
    private List<ChatSummary> list; // 核心：类中变量名是list，不是dataList
    private OnItemClickListener listener;

    // 点击回调接口
    public interface OnItemClickListener {
        void onItemClick(ChatSummary chat);
    }

    // 构造方法
    public MessageAdapter(List<ChatSummary> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    // 新增：设置点击监听
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_summary, parent, false);
        ViewHolder holder = new ViewHolder(v);

        // 绑定点击事件（核心：原有代码缺失点击逻辑）
        v.setOnClickListener(v1 -> {
            int position = holder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION && listener != null) {
                listener.onItemClick(list.get(position));
            }
        });
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // 修复1：用list替代不存在的dataList
        ChatSummary item = list.get(position);
        holder.tvName.setText(item.getName());
        holder.tvLastMsg.setText(item.getLastMessage());
        holder.tvTime.setText(item.getTime());

        // 头像显示：优先网络URL，其次本地资源
        if (item.getAvatarUrl() != null && !item.getAvatarUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(item.getAvatarUrl())
                    .circleCrop() // 圆形裁剪
                    .error(item.getAvatarResId()) // URL加载失败时显示本地资源
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(item.getAvatarResId());
        }
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    // 修复2：ViewHolder中添加ivAvatar声明
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvLastMsg, tvTime;
        ImageView ivAvatar; // 新增：头像控件

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvLastMsg = itemView.findViewById(R.id.tv_last_msg);
            tvTime = itemView.findViewById(R.id.tv_time);
            ivAvatar = itemView.findViewById(R.id.iv_avatar); // 绑定头像控件ID
        }
    }

    // 移除好友请求项
    public void removeFriendRequestItem() {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).isFriendRequest()) {
                list.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    // 新增：更新数据（可选，防止数据重复）
    public void updateData(List<ChatSummary> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }
}