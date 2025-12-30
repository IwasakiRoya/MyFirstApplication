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
import com.bumptech.glide.request.RequestOptions;
import com.example.myfirstapplication.R;
import com.example.myfirstapplication.model.ChatMessage;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = ChatMessage.TYPE_SENT;       // 1：我方发送
    private static final int TYPE_RECEIVED = ChatMessage.TYPE_RECEIVED; // 0：对方接收

    private List<ChatMessage> messageList;
    private String currentUserAvatar;
    private String friendAvatar;
    private String currentUserId;
    private OnMessageResendListener onMessageResendListener;

    public ChatAdapter(List<ChatMessage> messageList, String currentUserAvatar, String friendAvatar, String currentUserId) {
        this.messageList = messageList == null ? new java.util.ArrayList<>() : messageList;
        this.currentUserAvatar = currentUserAvatar == null ? "" : currentUserAvatar;
        this.friendAvatar = friendAvatar == null ? "" : friendAvatar;
        this.currentUserId = currentUserId == null ? "" : currentUserId;
    }

    public ChatAdapter(List<ChatMessage> messageList) {
        this(messageList, "", "", "");
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= messageList.size()) return TYPE_RECEIVED;
        ChatMessage msg = messageList.get(position);
        if (msg == null || msg.getUserId() == null || currentUserId.isEmpty()) {
            return TYPE_RECEIVED;
        }
        boolean isCurrentUserMsg = currentUserId.equals(msg.getUserId());
        return isCurrentUserMsg ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SENT) {
            View v = inflater.inflate(R.layout.item_chat_right, parent, false);
            return new SentViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_chat_left, parent, false);
            return new ReceivedViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position >= messageList.size()) return;
        ChatMessage msg = messageList.get(position);
        if (msg == null) return;

        if (holder instanceof SentViewHolder) {
            bindSentMessage((SentViewHolder) holder, msg);
        } else if (holder instanceof ReceivedViewHolder) {
            bindReceivedMessage((ReceivedViewHolder) holder, msg);
        }
    }

    /**
     * 核心修改：绑定发送方消息，支持图片渲染
     */
    private void bindSentMessage(SentViewHolder holder, ChatMessage msg) {
        // --- 1. 处理内容显示：区分文字和图片 ---
        if (msg.getMsgType() == ChatMessage.MSG_TYPE_IMAGE) {
            // 是图片消息
            holder.tvContent.setVisibility(View.GONE);  // 隐藏文本框
            holder.ivChatImage.setVisibility(View.VISIBLE); // 显示图片框

            Glide.with(holder.itemView.getContext())
                    .load(msg.getContent()) // content 存的是 OSS URL
                    .placeholder(R.drawable.ic_avatar_2) // 加载占位图
                    .into(holder.ivChatImage);
        } else {
            // 是文字消息
            holder.tvContent.setVisibility(View.VISIBLE);
            holder.ivChatImage.setVisibility(View.GONE);
            holder.tvContent.setText(msg.getContent() == null ? "" : msg.getContent());
        }

        // --- 2. 设置头像 ---
        RequestOptions avatarOptions = new RequestOptions().circleCrop().error(R.drawable.ic_menu_person);
        Glide.with(holder.itemView.getContext())
                .load(currentUserAvatar.isEmpty() ? R.drawable.ic_menu_person : currentUserAvatar)
                .apply(avatarOptions)
                .into(holder.ivAvatar);

        // --- 3. 处理状态（转圈/重发） ---
        int status = msg.getStatus() == null ? ChatMessage.STATUS_SUCCESS : msg.getStatus();
        if (status == ChatMessage.STATUS_THINKING) {
            holder.pbLoading.setVisibility(View.VISIBLE);
            holder.tvContent.setTextColor(Color.GRAY);
        } else if (status == ChatMessage.STATUS_FAILED) {
            holder.pbLoading.setVisibility(View.GONE);
            holder.tvContent.setTextColor(Color.RED);
            holder.bubbleLayout.setOnClickListener(v -> {
                if (onMessageResendListener != null) onMessageResendListener.onResend(msg);
            });
        } else {
            holder.pbLoading.setVisibility(View.GONE);
            holder.tvContent.setTextColor(Color.BLACK);
            holder.bubbleLayout.setOnClickListener(null);
        }
    }

    /**
     * 核心修改：绑定接收方消息，支持图片渲染
     */
    private void bindReceivedMessage(ReceivedViewHolder holder, ChatMessage msg) {
        // --- 1. 处理内容显示 ---
        if (msg.getMsgType() == ChatMessage.MSG_TYPE_IMAGE) {
            holder.tvContent.setVisibility(View.GONE);
            holder.ivChatImage.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(msg.getContent())
                    .placeholder(R.drawable.ic_avatar_2)
                    .into(holder.ivChatImage);
        } else {
            holder.tvContent.setVisibility(View.VISIBLE);
            holder.ivChatImage.setVisibility(View.GONE);
            holder.tvContent.setText(msg.getContent() == null ? "" : msg.getContent());
        }

        // --- 2. 设置头像 ---
        RequestOptions avatarOptions = new RequestOptions().circleCrop().error(R.drawable.ic_menu_person);
        Glide.with(holder.itemView.getContext())
                .load(friendAvatar.isEmpty() ? R.drawable.ic_menu_person : friendAvatar)
                .apply(avatarOptions)
                .into(holder.ivAvatar);

        holder.pbLoading.setVisibility(View.GONE);
        holder.tvContent.setTextColor(Color.BLACK);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // ========== ViewHolder 增加 ivChatImage 控件 ==========
    static class SentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        ImageView ivChatImage; // 新增：聊天图片显示控件
        LinearLayout bubbleLayout;
        TextView tvContent;
        ProgressBar pbLoading;

        SentViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            ivChatImage = itemView.findViewById(R.id.iv_chat_image); // 需在布局中添加此ID
            bubbleLayout = itemView.findViewById(R.id.bubble_layout);
            tvContent = itemView.findViewById(R.id.tv_content);
            pbLoading = itemView.findViewById(R.id.pb_loading);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        ImageView ivChatImage; // 新增：聊天图片显示控件
        TextView tvContent;
        ProgressBar pbLoading;

        ReceivedViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            ivChatImage = itemView.findViewById(R.id.iv_chat_image); // 需在布局中添加此ID
            tvContent = itemView.findViewById(R.id.tv_content);
            pbLoading = itemView.findViewById(R.id.pb_loading);
        }
    }

    // ... 其他辅助方法保持不变 (updateMessageList, addMessage 等) ...
    public void updateMessageList(List<ChatMessage> newList) {
        if (newList == null) return;
        this.messageList.clear();
        this.messageList.addAll(newList);
        notifyDataSetChanged();
    }

    public interface OnMessageResendListener {
        void onResend(ChatMessage message);
    }
}