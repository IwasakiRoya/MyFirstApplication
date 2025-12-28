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

    // 核心修复：对齐ChatMessage中的常量值，解决类型不匹配
    private static final int TYPE_SENT = ChatMessage.TYPE_SENT;       // 1：我方发送
    private static final int TYPE_RECEIVED = ChatMessage.TYPE_RECEIVED; // 0：对方接收

    private List<ChatMessage> messageList;
    private String currentUserAvatar; // 当前用户头像URL
    private String friendAvatar;       // 好友头像URL
    private String currentUserId;      // 当前登录用户ID（核心：用于消息类型判断）
    private OnMessageResendListener onMessageResendListener; // 重发回调

    // 构造方法：新增头像+当前用户ID参数（优先使用，适配聊天场景）
    public ChatAdapter(List<ChatMessage> messageList, String currentUserAvatar, String friendAvatar, String currentUserId) {
        this.messageList = messageList == null ? new java.util.ArrayList<>() : messageList;
        this.currentUserAvatar = currentUserAvatar == null ? "" : currentUserAvatar;
        this.friendAvatar = friendAvatar == null ? "" : friendAvatar;
        this.currentUserId = currentUserId == null ? "" : currentUserId;
    }

    // 简化构造（兼容无头像场景）
    public ChatAdapter(List<ChatMessage> messageList) {
        this(messageList, "", "", "");
    }

    // 简化构造（兼容无头像但有用户ID场景）
    public ChatAdapter(List<ChatMessage> messageList, String currentUserId) {
        this(messageList, "", "", currentUserId);
    }

    // ========== 核心优化：优先按 userId 判断消息类型，忽略后端无效 type 字段 ==========
    @Override
    public int getItemViewType(int position) {
        if (position >= messageList.size()) return TYPE_RECEIVED;
        ChatMessage msg = messageList.get(position);
        if (msg == null || msg.getUserId() == null || currentUserId.isEmpty()) {
            // 打印空值日志，方便排查
            android.util.Log.d("ChatAdapterDebug", "currentUserId: " + currentUserId + ", msg.userId: " + (msg == null ? "null" : msg.getUserId()));
            return TYPE_RECEIVED;
        }

        // 核心：添加日志，验证匹配结果（关键排查依据）
        boolean isCurrentUserMsg = currentUserId.equals(msg.getUserId());
        android.util.Log.d("ChatAdapterDebug", "currentUserId: " + currentUserId + ", msg.userId: " + msg.getUserId() + ", 匹配结果：" + isCurrentUserMsg);

        // 逻辑保持不变，确保无反向
        return isCurrentUserMsg ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SENT) {
            // 加载右侧布局（我方发送）：绑定你的本地布局 R.layout.item_chat_right
            View v = inflater.inflate(R.layout.item_chat_right, parent, false);
            return new SentViewHolder(v);
        } else {
            // 加载左侧布局（对方发送）：绑定你的本地布局 R.layout.item_chat_left
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
     * 绑定发送方消息（修复：转圈样式仅在我方发送中显示，无额外资源）
     */
    private void bindSentMessage(SentViewHolder holder, ChatMessage msg) {
        // 1. 设置消息内容（非空兜底）
        holder.tvContent.setText(msg.getContent() == null ? "" : msg.getContent());

        // 2. 设置头像（Glide基础用法，无额外图标，空头像时显示系统默认）
        RequestOptions options = new RequestOptions()
                .circleCrop() // Glide内置圆形裁剪，无额外xml
                .error(R.drawable.ic_menu_person); // 系统默认头像，无需自定义xml

        if (!currentUserAvatar.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(currentUserAvatar)
                    .apply(options)
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_menu_person); // 系统默认图标
        }

        // 3. 处理消息状态（仅我方消息显示转圈，对方消息永不显示）
        int status = msg.getStatus() == null ? ChatMessage.STATUS_SUCCESS : msg.getStatus();
        if (status == ChatMessage.STATUS_THINKING) {
            // 发送中：显示转圈进度条
            holder.pbLoading.setVisibility(View.VISIBLE);
            holder.tvContent.setTextColor(Color.GRAY);
            holder.bubbleLayout.setOnClickListener(null); // 禁用点击
        } else if (status == ChatMessage.STATUS_FAILED) {
            // 发送失败：隐藏转圈，红色文字
            holder.pbLoading.setVisibility(View.GONE);
            holder.tvContent.setTextColor(Color.RED);
            // 点击重发
            holder.bubbleLayout.setOnClickListener(v -> {
                if (onMessageResendListener != null) {
                    onMessageResendListener.onResend(msg);
                }
            });
        } else {
            // 发送成功：隐藏转圈，黑色文字
            holder.pbLoading.setVisibility(View.GONE);
            holder.tvContent.setTextColor(Color.BLACK);
            holder.bubbleLayout.setOnClickListener(null); // 禁用点击
        }
    }

    /**
     * 绑定接收方消息（修复：强制隐藏转圈，确保对方消息无转圈样式）
     */
    private void bindReceivedMessage(ReceivedViewHolder holder, ChatMessage msg) {
        // 1. 设置消息内容（非空兜底）
        holder.tvContent.setText(msg.getContent() == null ? "" : msg.getContent());

        // 2. 设置好友头像（系统默认图标，无额外资源）
        RequestOptions options = new RequestOptions()
                .circleCrop()
                .error(R.drawable.ic_menu_person); // 系统默认头像

        if (!friendAvatar.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(friendAvatar)
                    .apply(options)
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_menu_person); // 系统默认图标
        }

        // 3. 强制隐藏对方消息的加载框，永不转圈（核心修复）
        holder.pbLoading.setVisibility(View.GONE);
        holder.tvContent.setTextColor(Color.BLACK); // 固定黑色文字，无额外样式
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // ========== ViewHolder定义（需你绑定本地布局的控件ID） ==========
    static class SentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar; // 头像控件（对应 item_chat_right 中的 iv_avatar）
        LinearLayout bubbleLayout; // 消息气泡布局（对应 item_chat_right 中的 bubble_layout）
        TextView tvContent; // 消息内容控件（对应 item_chat_right 中的 tv_content）
        ProgressBar pbLoading; // 转圈进度条（对应 item_chat_right 中的 pb_loading）

        SentViewHolder(View itemView) {
            super(itemView);
            // ******** 需你自行绑定本地布局 item_chat_right 的控件ID ********
            ivAvatar = itemView.findViewById(R.id.iv_avatar); // 你的右侧布局头像ID
            bubbleLayout = itemView.findViewById(R.id.bubble_layout); // 你的右侧布局气泡ID
            tvContent = itemView.findViewById(R.id.tv_content); // 你的右侧布局内容ID
            pbLoading = itemView.findViewById(R.id.pb_loading); // 你的右侧布局进度条ID
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar; // 头像控件（对应 item_chat_left 中的 iv_avatar）
        TextView tvContent; // 消息内容控件（对应 item_chat_left 中的 tv_content）
        ProgressBar pbLoading; // 转圈进度条（对应 item_chat_left 中的 pb_loading，强制隐藏）

        ReceivedViewHolder(View itemView) {
            super(itemView);
            // ******** 需你自行绑定本地布局 item_chat_left 的控件ID ********
            ivAvatar = itemView.findViewById(R.id.iv_avatar); // 你的左侧布局头像ID
            tvContent = itemView.findViewById(R.id.tv_content); // 你的左侧布局内容ID
            pbLoading = itemView.findViewById(R.id.pb_loading); // 你的左侧布局进度条ID（可保留，强制隐藏）
        }
    }

    // ========== 辅助方法（无额外功能，仅优化刷新） ==========
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
     * 更新当前用户ID（用于切换账号后刷新）
     */
    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId == null ? "" : currentUserId;
        notifyDataSetChanged();
    }

    /**
     * 设置重发回调
     */
    public void setOnMessageResendListener(OnMessageResendListener listener) {
        this.onMessageResendListener = listener;
    }

    // ========== 重发回调接口（无额外实现） ==========
    public interface OnMessageResendListener {
        void onResend(ChatMessage message);
    }
}