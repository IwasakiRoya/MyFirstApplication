package com.example.myfirstapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myfirstapplication.Adapter.ChatAdapter;
import com.example.myfirstapplication.database.AppDatabase;
import com.example.myfirstapplication.model.ChatMessage;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView rvChat;
    private ChatAdapter adapter;
    private List<ChatMessage> messageList = new ArrayList<>();
    private EditText etInput;
    private String currentFriendId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 这里可以保留自动生成的 EdgeToEdge 处理，或者直接 setContentView
        setContentView(R.layout.activity_chat);

        // 获取传过来的好友 ID
        currentFriendId = getIntent().getStringExtra("friendId");
        String friendName = getIntent().getStringExtra("friendName");
        setTitle(friendName);

        rvChat = findViewById(R.id.rv_chat);
        etInput = findViewById(R.id.et_input);
        Button btnSend = findViewById(R.id.btn_send);

        adapter = new ChatAdapter(messageList);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(adapter);

        // ========== 核心：监听数据库 (观察者模式) ==========
        AppDatabase.getInstance(this).chatDao().getMessagesByFriend(currentFriendId)
                .observe(this, messages -> {
                    // 当数据库内容变化时，这个回调会立即触发
                    messageList.clear();
                    messageList.addAll(messages);
                    adapter.notifyDataSetChanged();
                    // 自动滚动到底部
                    if (messageList.size() > 0) {
                        rvChat.scrollToPosition(messageList.size() - 1);
                    }
                });

        // 发送逻辑：只需要写入数据库即可，不需要手动刷新 List
        btnSend.setOnClickListener(v -> {
            String text = etInput.getText().toString();
            if (!text.isEmpty()) {
                // 1. 封装消息对象
                ChatMessage myMsg = new ChatMessage(currentFriendId, text, ChatMessage.TYPE_SENT, ChatMessage.STATUS_SUCCESS);

                // 2. 写入数据库（异步）
                new Thread(() -> {
                    AppDatabase.getInstance(this).chatDao().insert(myMsg);
                }).start();

                etInput.setText("");
            }
        });
    }
}