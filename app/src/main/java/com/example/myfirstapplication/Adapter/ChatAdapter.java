package com.example.myfirstapplication.Adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myfirstapplication.R;
import com.example.myfirstapplication.model.ChatMessage;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ChatMessage> messageList;

    public ChatAdapter(List<ChatMessage> messageList) {
        this.messageList = messageList;
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ChatMessage.TYPE_SENT) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_right, parent, false);
            return new SentViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_left, parent, false);
            return new ReceivedViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messageList.get(position);

        if (holder instanceof SentViewHolder) {
            SentViewHolder vh = (SentViewHolder) holder;
            vh.tvContent.setText(msg.content);

            // 关键修改：用ChatMessage的常量替代ChatStatus
            if (msg.status == ChatMessage.STATUS_THINKING) {
                vh.pbLoading.setVisibility(View.VISIBLE);
                vh.tvContent.setTextColor(Color.GRAY);
            } else {
                vh.pbLoading.setVisibility(View.GONE);
                vh.tvContent.setTextColor(Color.BLACK);
            }
        } else if (holder instanceof ReceivedViewHolder) {
            ReceivedViewHolder vh = (ReceivedViewHolder) holder;
            vh.tvContent.setText(msg.content);
        }
    }

    @Override
    public int getItemCount() { return messageList.size(); }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent;
        ProgressBar pbLoading;

        SentViewHolder(View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_content);
            pbLoading = itemView.findViewById(R.id.pb_loading);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent;

        ReceivedViewHolder(View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_content);
        }
    }

    // 新增：更新数据的方法（供Service更新后刷新UI）
    public void setMessageList(List<ChatMessage> newList) {
        this.messageList = newList;
        notifyDataSetChanged();
    }
}