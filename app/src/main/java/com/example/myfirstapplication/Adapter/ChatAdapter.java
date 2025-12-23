package com.example.myfirstapplication.Adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myfirstapplication.R;
import com.example.myfirstapplication.model.ChatMessage;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ChatMessage> messageList;
    private String currentUserAvatar; // 当前用户头像URL
    private String friendAvatar;       // 好友头像URL

    // 构造方法：新增头像参数
    public ChatAdapter(List<ChatMessage> messageList, String currentUserAvatar, String friendAvatar) {
        this.messageList = messageList;
        this.currentUserAvatar = currentUserAvatar;
        this.friendAvatar = friendAvatar;
    }

    // 简化构造（兼容无头像场景）
    public ChatAdapter(List<ChatMessage> messageList) {
        this.messageList = messageList;
        this.currentUserAvatar = "";
        this.friendAvatar = "";
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ChatMessage.TYPE_SENT) {
            // 加载右侧布局（我方发送）
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_right, parent, false);
            return new SentViewHolder(v);
        } else {
            // 加载左侧布局（对方发送）
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_left, parent, false);
            return new ReceivedViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messageList.get(position);

        if (holder instanceof SentViewHolder) {
            SentViewHolder vh = (SentViewHolder) holder;
            // 1. 设置消息内容
            vh.tvContent.setText(msg.content);

            // 2. 设置头像（优先加载网络URL，无则用默认）
            if (currentUserAvatar != null && !currentUserAvatar.isEmpty()) {
                Glide.with(vh.itemView.getContext())
                        .load(currentUserAvatar)
                        .circleCrop() // 圆形裁剪
                        .into(vh.ivAvatar);
            } else {
                vh.ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
            }

            // 3. 处理消息状态
            if (msg.status == ChatMessage.STATUS_THINKING) {
                // AI思考中/发送中
                vh.pbLoading.setVisibility(View.VISIBLE);
                vh.tvContent.setTextColor(Color.GRAY);
            } else if (msg.status == ChatMessage.STATUS_FAILED) {
                // 发送失败
                vh.pbLoading.setVisibility(View.GONE);
                vh.tvContent.setTextColor(Color.RED);
                // 点击重发（可选）
                vh.bubbleLayout.setOnClickListener(v -> {
                    if (onMessageResendListener != null) {
                        onMessageResendListener.onResend(msg);
                    }
                });
            } else {
                // 发送成功
                vh.pbLoading.setVisibility(View.GONE);
                vh.tvContent.setTextColor(Color.BLACK);
            }
        } else if (holder instanceof ReceivedViewHolder) {
            ReceivedViewHolder vh = (ReceivedViewHolder) holder;
            // 1. 设置消息内容
            vh.tvContent.setText(msg.content);

            // 2. 设置好友头像
            if (friendAvatar != null && !friendAvatar.isEmpty()) {
                Glide.with(vh.itemView.getContext())
                        .load(friendAvatar)
                        .circleCrop()
                        .into(vh.ivAvatar);
            } else {
                vh.ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
            }

            // 3. 对方消息隐藏加载框
            vh.pbLoading.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return messageList == null ? 0 : messageList.size();
    }

    // ========== ViewHolder定义 ==========
    static class SentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        LinearLayout bubbleLayout;
        TextView tvContent;
        ProgressBar pbLoading;

        SentViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            bubbleLayout = itemView.findViewById(R.id.bubble_layout);
            tvContent = itemView.findViewById(R.id.tv_content);
            pbLoading = itemView.findViewById(R.id.pb_loading);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvContent;
        ProgressBar pbLoading;

        ReceivedViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvContent = itemView.findViewById(R.id.tv_content);
            pbLoading = itemView.findViewById(R.id.pb_loading);
        }
    }

    // ========== 辅助方法 ==========
    // 更新消息列表
    public void setMessageList(List<ChatMessage> newList) {
        this.messageList = newList;
        notifyDataSetChanged();
    }

    // 更新头像
    public void setAvatars(String currentUserAvatar, String friendAvatar) {
        this.currentUserAvatar = currentUserAvatar;
        this.friendAvatar = friendAvatar;
        notifyDataSetChanged();
    }

    // ========== 重发回调 ==========
    public interface OnMessageResendListener {
        void onResend(ChatMessage message);
    }

    private OnMessageResendListener onMessageResendListener;

}