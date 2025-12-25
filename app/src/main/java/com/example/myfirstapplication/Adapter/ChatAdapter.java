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
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.myfirstapplication.R;
import com.example.myfirstapplication.model.ChatMessage;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // 消息类型常量（和ChatMessage对齐）
    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private List<ChatMessage> messageList;
    private String currentUserAvatar; // 当前用户头像URL
    private String friendAvatar;       // 好友头像URL
    private OnMessageResendListener onMessageResendListener; // 重发回调

    // 构造方法：新增头像参数
    public ChatAdapter(List<ChatMessage> messageList, String currentUserAvatar, String friendAvatar) {
        this.messageList = messageList == null ? new java.util.ArrayList<>() : messageList;
        this.currentUserAvatar = currentUserAvatar == null ? "" : currentUserAvatar;
        this.friendAvatar = friendAvatar == null ? "" : friendAvatar;
    }

    // 简化构造（兼容无头像场景）
    public ChatAdapter(List<ChatMessage> messageList) {
        this(messageList, "", "");
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).getType(); // 使用getter避免直接访问字段
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SENT) {
            // 加载右侧布局（我方发送）
            View v = inflater.inflate(R.layout.item_chat_right, parent, false);
            return new SentViewHolder(v);
        } else {
            // 加载左侧布局（对方发送）
            View v = inflater.inflate(R.layout.item_chat_left, parent, false);
            return new ReceivedViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messageList.get(position);
        if (msg == null) return;

        if (holder instanceof SentViewHolder) {
            bindSentMessage((SentViewHolder) holder, msg);
        } else if (holder instanceof ReceivedViewHolder) {
            bindReceivedMessage((ReceivedViewHolder) holder, msg);
        }
    }

    /**
     * 绑定发送方消息
     */
    private void bindSentMessage(SentViewHolder holder, ChatMessage msg) {
        // 1. 设置消息内容
        holder.tvContent.setText(msg.getContent() == null ? "" : msg.getContent());

        // 2. 设置头像（Glide优化：添加错误占位、圆形裁剪）
        RequestOptions options = new RequestOptions()
                .transform(new CircleCrop())
                .error(R.mipmap.ic_launcher_round);

        if (!currentUserAvatar.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(currentUserAvatar)
                    .apply(options)
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
        }

        // 3. 处理消息状态
        int status = msg.getStatus();
        if (status == ChatMessage.STATUS_THINKING) {
            // AI思考中/发送中
            holder.pbLoading.setVisibility(View.VISIBLE);
            holder.tvContent.setTextColor(Color.GRAY);
            holder.bubbleLayout.setOnClickListener(null); // 禁用点击
        } else if (status == ChatMessage.STATUS_FAILED) {
            // 发送失败
            holder.pbLoading.setVisibility(View.GONE);
            holder.tvContent.setTextColor(Color.RED);
            // 点击重发
            holder.bubbleLayout.setOnClickListener(v -> {
                if (onMessageResendListener != null) {
                    onMessageResendListener.onResend(msg);
                }
            });
        } else {
            // 发送成功
            holder.pbLoading.setVisibility(View.GONE);
            holder.tvContent.setTextColor(Color.BLACK);
            holder.bubbleLayout.setOnClickListener(null); // 禁用点击
        }
    }

    /**
     * 绑定接收方消息
     */
    private void bindReceivedMessage(ReceivedViewHolder holder, ChatMessage msg) {
        // 1. 设置消息内容
        holder.tvContent.setText(msg.getContent() == null ? "" : msg.getContent());

        // 2. 设置好友头像
        RequestOptions options = new RequestOptions()
                .transform(new CircleCrop())
                .error(R.mipmap.ic_launcher_round);

        if (!friendAvatar.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(friendAvatar)
                    .apply(options)
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
        }

        // 3. 对方消息隐藏加载框
        holder.pbLoading.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
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
    /**
     * 更新消息列表（增量更新，避免全量刷新）
     */
    public void updateMessageList(List<ChatMessage> newList) {
        if (newList == null) return;
        this.messageList.clear();
        this.messageList.addAll(newList);
        notifyDataSetChanged();
    }

    /**
     * 追加单条消息（聊天时用）
     */
    public void addMessage(ChatMessage msg) {
        if (msg == null) return;
        this.messageList.add(msg);
        notifyItemInserted(messageList.size() - 1);
    }

    /**
     * 更新头像
     */
    public void setAvatars(String currentUserAvatar, String friendAvatar) {
        this.currentUserAvatar = currentUserAvatar == null ? "" : currentUserAvatar;
        this.friendAvatar = friendAvatar == null ? "" : friendAvatar;
        notifyDataSetChanged();
    }

    /**
     * 设置重发回调
     */
    public void setOnMessageResendListener(OnMessageResendListener listener) {
        this.onMessageResendListener = listener;
    }

    // ========== 重发回调接口 ==========
    public interface OnMessageResendListener {
        void onResend(ChatMessage message);
    }
}