package com.example.myfirstapplication.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myfirstapplication.Adapter.ChatAdapter;
import com.example.myfirstapplication.R;
import com.example.myfirstapplication.database.AppDatabase;
import com.example.myfirstapplication.model.ChatMessage;
import com.example.myfirstapplication.model.ChatReadPosition;
import com.example.myfirstapplication.model.response.BaseResponse;
import com.example.myfirstapplication.network.ApiService;
import com.example.myfirstapplication.utils.DbExecutor;
import com.example.myfirstapplication.utils.FileUtils;
import com.example.myfirstapplication.utils.NetworkUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView rvChat;
    private EditText etInput;
    private Button btnSend;
    private Switch switchAi;

    // ========== 图片上传相关 UI ==========
    private ImageView btnAddImage;
    private RelativeLayout layoutPreview;
    private ImageView ivPreview, ivDeletePreview;
    private ProgressBar pbUploading;
    private TextView tvUploadStatus;

    private String friendId;
    private String friendName;
    private String userId;
    private String token;

    private ChatAdapter chatAdapter;
    private final List<ChatMessage> messageList = new ArrayList<>();

    private ApiService apiService;
    private AppDatabase db;

    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private String currentUserAvatar;
    private String friendAvatar;

    private ChatReadPosition readPosition;
    private LinearLayoutManager layoutManager;

    // 轮询与已读逻辑变量
    private Handler pollHandler;
    private Runnable pollRunnable;
    private static final long POLL_INTERVAL = 500;
    private boolean isPolling = false;
    private long lastPullTimestamp = 0;
    private long lastRenderedMessageTimestamp = 0;
    private boolean isChatPageVisible = true;
    private boolean isUserScrollingToHistory = false;
    private int lastScrollY = 0;

    // 图片上传状态变量（替换原有 REQUEST_CODE_PICK_IMAGE）
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private String currentUploadedImageUrl = null;

    private final BroadcastReceiver chatRefreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String refreshFriendId = intent.getStringExtra("friendId");
            if (friendId != null && friendId.equals(refreshFriendId)) {
                loadChatHistory(false);
            }
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 初始化 ActivityResultLauncher（必须在 onCreate 中注册，替代过时的 onActivityResult）
        initImagePickerLauncher();

        friendId = getIntent().getStringExtra("friendId");
        friendName = getIntent().getStringExtra("friendName");
        friendAvatar = getIntent().getStringExtra("friendAvatar");

        if (friendId == null || friendId.isEmpty()) {
            Toast.makeText(this, "好友ID不能为空", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initData();
        initView();
        initListener();

        IntentFilter filter = new IntentFilter("com.example.REFRESH_CHAT");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(chatRefreshReceiver, filter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(chatRefreshReceiver, filter);
        }

        loadChatHistory(true);
        initPollingTask();
    }

    /**
     * 初始化图片选择器（ActivityResultLauncher），替代过时的 onActivityResult
     */
    private void initImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            // 选择图片后，执行图片准备与上传逻辑
                            prepareAndUploadImage(uri);
                        }
                    }
                }
        );
    }

    private void initPollingTask() {
        pollHandler = new Handler(Looper.getMainLooper());
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPolling && !isFinishing() && !isDestroyed() && !token.isEmpty()) {
                    loadChatHistory(false);
                    pollHandler.postDelayed(this, POLL_INTERVAL);
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        isChatPageVisible = true;
        isUserScrollingToHistory = false;
        lastScrollY = 0;
        if (!isPolling) {
            isPolling = true;
            if (pollHandler != null && pollRunnable != null) {
                pollHandler.post(pollRunnable);
            }
        }
        refreshUI(false);
        mainHandler.postDelayed(this::markCurrentRenderedMessagesAsRead, 300);
    }

    @Override
    protected void onPause() {
        super.onPause();
        isChatPageVisible = false;
        if (isPolling) {
            isPolling = false;
            if (pollHandler != null && pollRunnable != null) {
                pollHandler.removeCallbacks(pollRunnable);
            }
        }
        markCurrentRenderedMessagesAsRead();
    }

    private void markCurrentRenderedMessagesAsRead() {
        if (messageList.isEmpty() || friendId.isEmpty() || userId.isEmpty() || token.isEmpty()) return;
        updateLastRenderedMessageTimestamp();
        DbExecutor.execute(() -> {
            long currentRenderedTs = lastRenderedMessageTimestamp;
            if (currentRenderedTs <= 0) currentRenderedTs = System.currentTimeMillis();
            db.chatDao().markChatAsRead(userId, friendId, currentRenderedTs);
            List<ChatMessage> latestMsgList = db.chatDao().getChatMessagesByTwoUsers(userId, friendId);
            int latestMsgId = 0;
            if (!latestMsgList.isEmpty()) {
                ChatMessage latestMsg = latestMsgList.get(latestMsgList.size() - 1);
                latestMsgId = latestMsg.getId() != null ? latestMsg.getId() : 0;
            }
            updateBackendReadPosition(latestMsgId, currentRenderedTs);
            sendChatRefreshBroadcast(friendId);
        });
    }

    private void updateLastRenderedMessageTimestamp() {
        if (rvChat == null || layoutManager == null || messageList.isEmpty()) return;
        int lastVisiblePos = layoutManager.findLastCompletelyVisibleItemPosition();
        if (lastVisiblePos == -1 || lastVisiblePos >= messageList.size()) {
            lastVisiblePos = layoutManager.findLastVisibleItemPosition();
            if (lastVisiblePos == -1 || lastVisiblePos >= messageList.size()) return;
        }
        ChatMessage lastRenderedMessage = messageList.get(lastVisiblePos);
        if (lastRenderedMessage != null && lastRenderedMessage.getTimestamp() != null) {
            if (lastRenderedMessage.getTimestamp() > lastRenderedMessageTimestamp) {
                lastRenderedMessageTimestamp = lastRenderedMessage.getTimestamp();
            }
        }
    }

    private void updateBackendReadPosition(int latestMsgId, long currentTime) {
        if (token.isEmpty() || friendId.isEmpty() || userId.isEmpty()) return;
        apiService.updateReadPosition(token, friendId, latestMsgId)
                .enqueue(new Callback<BaseResponse<Void>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {}
                    @Override
                    public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {}
                });
    }

    private void sendChatRefreshBroadcast(String refreshFriendId) {
        Intent intent = new Intent("com.example.REFRESH_CHAT");
        intent.putExtra("friendId", refreshFriendId);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    private void initView() {
        toolbar = findViewById(R.id.toolbar);
        rvChat = findViewById(R.id.rv_chat);
        etInput = findViewById(R.id.et_input);
        btnSend = findViewById(R.id.btn_send);
        switchAi = findViewById(R.id.switch_ai);

        // 初始化新增图片相关 UI
        btnAddImage = findViewById(R.id.btn_add_image);
        layoutPreview = findViewById(R.id.layout_image_preview);
        ivPreview = findViewById(R.id.iv_preview);
        ivDeletePreview = findViewById(R.id.iv_delete_preview);
        pbUploading = findViewById(R.id.pb_uploading);
        tvUploadStatus = findViewById(R.id.tv_upload_status);

        toolbar.setTitle(friendName == null || friendName.isEmpty() ? "聊天窗口" : friendName);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChat.setLayoutManager(layoutManager);

        chatAdapter = new ChatAdapter(messageList, currentUserAvatar, friendAvatar, userId);
        rvChat.setAdapter(chatAdapter);

        rvChat.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int firstVisiblePos = layoutManager.findFirstVisibleItemPosition();
                    int lastVisiblePos = layoutManager.findLastVisibleItemPosition();
                    int totalItemCount = layoutManager.getItemCount();
                    isUserScrollingToHistory = (firstVisiblePos == 0) || (lastVisiblePos != totalItemCount - 1 && totalItemCount > 0);
                    if (isChatPageVisible) {
                        updateLastRenderedMessageTimestamp();
                        mainHandler.postDelayed(() -> markCurrentRenderedMessagesAsRead(), 200);
                    }
                } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    lastScrollY = recyclerView.computeVerticalScrollOffset();
                }
            }
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy < 0) isUserScrollingToHistory = true;
                else if (dy > 0 && layoutManager.findLastCompletelyVisibleItemPosition() == messageList.size() - 1) isUserScrollingToHistory = false;
                lastScrollY = recyclerView.computeVerticalScrollOffset();
            }
        });
        switchAi.setVisibility(View.GONE);
    }

    private void initData() {
        SharedPreferences sp = getSharedPreferences("USER_INFO", MODE_PRIVATE);
        userId = sp.getString("userId", "");
        token = sp.getString("token", "");
        currentUserAvatar = sp.getString("avatarUrl", "");

        if (userId.isEmpty()) {
            Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = AppDatabase.getInstance(this);
        apiService = NetworkUtils.getApiService();

        DbExecutor.execute(() -> {
            readPosition = db.chatReadPositionDao().getReadPosition(friendId, userId);
            if (readPosition == null) {
                readPosition = new ChatReadPosition();
                readPosition.setFriendId(friendId);
                readPosition.setUserId(userId);
                readPosition.setLastReadMsgId(0);
                readPosition.setLastReadTime(System.currentTimeMillis());
                db.chatReadPositionDao().insertOrUpdate(readPosition);
            }
        });
    }

    private void initListener() {
        toolbar.setNavigationOnClickListener(v -> finish());

        // 新增：点击图标选图（使用 ActivityResultLauncher 启动）
        btnAddImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        // 新增：取消预览
        ivDeletePreview.setOnClickListener(v -> clearPreview());

        // 修改：发送逻辑兼容图片（保留原有文字发送功能）
        btnSend.setOnClickListener(v -> {
            // 优先处理图片发送
            if (layoutPreview.getVisibility() == View.VISIBLE) {
                if (currentUploadedImageUrl != null) {
                    sendPrepareMessage(currentUploadedImageUrl, ChatMessage.MSG_TYPE_IMAGE);
                    clearPreview();
                } else {
                    Toast.makeText(this, "图片上传中，请稍候...", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // 原有文字发送逻辑
            String content = etInput.getText().toString().trim();
            if (content.isEmpty()) return;
            sendPrepareMessage(content, ChatMessage.MSG_TYPE_TEXT);
            etInput.setText("");
        });
    }

    /**
     * 准备并上传图片（适配提供的 FileUtils 工具类，兼容 Android 10+ 沙盒机制）
     * @param uri 选中图片的 Uri
     */
    private void prepareAndUploadImage(Uri uri) {
        // 显示预览布局与上传状态
        layoutPreview.setVisibility(View.VISIBLE);
        ivPreview.setImageURI(uri);
        pbUploading.setVisibility(View.VISIBLE);
        tvUploadStatus.setText("正在上传至OSS...");
        btnSend.setEnabled(false);

        // 调用提供的 FileUtils 将 Uri 转换为 File（适配高版本 Android 沙盒）
        File file = FileUtils.getFileFromUri(this, uri);
        if (file == null) {
            Toast.makeText(this, "文件获取失败", Toast.LENGTH_SHORT).show();
            clearPreview();
            return;
        }

        // 构建 Retrofit 上传请求体
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        // 调用接口上传图片到 OSS
        apiService.uploadFile(token, body).enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(Call<BaseResponse<String>> call, Response<BaseResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    // 上传成功，保存图片 Url 并更新 UI
                    currentUploadedImageUrl = response.body().getData();
                    pbUploading.setVisibility(View.GONE);
                    tvUploadStatus.setText("上传成功，请点击发送");
                    btnSend.setEnabled(true);
                } else {
                    Toast.makeText(ChatActivity.this, "OSS上传失败", Toast.LENGTH_SHORT).show();
                    clearPreview();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<String>> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "网络错误，上传失败", Toast.LENGTH_SHORT).show();
                clearPreview();
            }
        });
    }

    /**
     * 清除图片预览与上传状态
     */
    private void clearPreview() {
        layoutPreview.setVisibility(View.GONE);
        currentUploadedImageUrl = null;
        btnSend.setEnabled(true);
        pbUploading.setVisibility(View.GONE);
        tvUploadStatus.setText("");
    }

    // 统一构建消息实体的方法（保留原有逻辑，支持文字/图片消息类型）
    private void sendPrepareMessage(String content, int msgType) {
        ChatMessage msg = new ChatMessage();
        msg.setFriendId(friendId);
        msg.setUserId(userId);
        msg.setContent(content);
        msg.setMsgType(msgType);
        msg.setType(ChatMessage.TYPE_SENT);
        msg.setStatus(ChatMessage.STATUS_THINKING);
        msg.setTimestamp(System.currentTimeMillis());
        sendMessage(msg);
    }

    private void loadChatHistory(boolean firstLoad) {
        if (token.isEmpty()) return;
        long lastTs = 0;
        if (!firstLoad) {
            lastTs = lastPullTimestamp;
            if (!messageList.isEmpty() && lastTs == 0) {
                ChatMessage latestMsg = messageList.get(messageList.size() - 1);
                lastTs = (latestMsg.getTimestamp() == null) ? 0 : latestMsg.getTimestamp();
            }
        }
        apiService.getChatHistory(token, friendId, lastTs)
                .enqueue(new Callback<BaseResponse<List<ChatMessage>>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<List<ChatMessage>>> call, Response<BaseResponse<List<ChatMessage>>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                            List<ChatMessage> data = response.body().getData();
                            if (data != null && !data.isEmpty()) {
                                DbExecutor.execute(() -> {
                                    for (ChatMessage m : data) {
                                        if (m.getContent() == null || m.getUserId() == null) continue;
                                        if (m.getTimestamp() == null) m.setTimestamp(System.currentTimeMillis());
                                        if (m.getFriendId() == null) m.setFriendId(friendId);
                                        ChatMessage existingMsg = db.chatDao().getMessageByUniqueKey(m.getContent(), m.getTimestamp(), m.getUserId(), m.getFriendId());
                                        if (existingMsg != null) continue;
                                        boolean isCurrentUserMsg = userId.equals(m.getUserId());
                                        m.setType(isCurrentUserMsg ? ChatMessage.TYPE_SENT : ChatMessage.TYPE_RECEIVED);
                                        m.setStatus(ChatMessage.STATUS_SUCCESS);
                                        db.chatDao().insert(m);
                                    }
                                    lastPullTimestamp = data.get(data.size() - 1).getTimestamp();
                                    refreshUI(firstLoad);
                                    if (isChatPageVisible) {
                                        mainHandler.postDelayed(() -> {
                                            updateLastRenderedMessageTimestamp();
                                            markCurrentRenderedMessagesAsRead();
                                            if (!isUserScrollingToHistory && !messageList.isEmpty()) rvChat.scrollToPosition(messageList.size() - 1);
                                        }, 200);
                                    }
                                });
                            } else refreshUI(firstLoad);
                        } else refreshUI(firstLoad);
                    }
                    @Override
                    public void onFailure(Call<BaseResponse<List<ChatMessage>>> call, Throwable t) { refreshUI(firstLoad); }
                });
    }

    private void refreshUI(boolean firstLoad) {
        DbExecutor.execute(() -> {
            List<ChatMessage> latest = db.chatDao().getChatMessagesByTwoUsers(userId, friendId);
            List<ChatMessage> sortedMessages = new ArrayList<>();
            if (latest != null && !latest.isEmpty()) {
                Collections.sort(latest, (msg1, msg2) -> Long.compare(msg1.getTimestamp() != null ? msg1.getTimestamp() : 0, msg2.getTimestamp() != null ? msg2.getTimestamp() : 0));
                sortedMessages.addAll(latest);
            }
            for (ChatMessage msg : sortedMessages) {
                boolean isCurrentUserMsg = userId.equals(msg.getUserId());
                msg.setType(isCurrentUserMsg ? ChatMessage.TYPE_SENT : ChatMessage.TYPE_RECEIVED);
                if (msg.getStatus() == null) msg.setStatus(ChatMessage.STATUS_SUCCESS);
            }
            mainHandler.post(() -> {
                if (!messageList.equals(sortedMessages)) {
                    messageList.clear();
                    messageList.addAll(sortedMessages);
                    chatAdapter.notifyDataSetChanged();
                }
                if ((firstLoad || !sortedMessages.isEmpty()) && !isUserScrollingToHistory) rvChat.scrollToPosition(messageList.size() - 1);
                updateLastRenderedMessageTimestamp();
            });
        });
    }

    private void sendMessage(ChatMessage msg) {
        DbExecutor.execute(() -> {
            long insertResult = db.chatDao().insert(msg);
            if (insertResult == -1) return;
            final int msgId = (int) insertResult;
            msg.setId(msgId);

            mainHandler.post(() -> {
                messageList.add(msg);
                chatAdapter.notifyItemInserted(messageList.size() - 1);
                rvChat.scrollToPosition(messageList.size() - 1);
                isUserScrollingToHistory = false;
                updateLastRenderedMessageToLatest(msg.getTimestamp());
            });

            apiService.sendMessage(token, msg).enqueue(new Callback<BaseResponse<ChatMessage>>() {
                @Override
                public void onResponse(Call<BaseResponse<ChatMessage>> call, Response<BaseResponse<ChatMessage>> response) {
                    DbExecutor.execute(() -> {
                        db.chatDao().updateMessage(msgId, msg.getContent(), ChatMessage.STATUS_SUCCESS);
                        mainHandler.post(() -> {
                            int pos = messageList.indexOf(msg);
                            if (pos != -1) {
                                messageList.get(pos).setStatus(ChatMessage.STATUS_SUCCESS);
                                chatAdapter.notifyItemChanged(pos);
                            }
                            lastPullTimestamp = System.currentTimeMillis();
                            markCurrentRenderedMessagesAsRead();
                            sendChatRefreshBroadcast(friendId);
                        });
                    });
                }
                @Override
                public void onFailure(Call<BaseResponse<ChatMessage>> call, Throwable t) {
                    DbExecutor.execute(() -> {
                        db.chatDao().updateMessage(msgId, msg.getContent(), ChatMessage.STATUS_FAILED);
                        mainHandler.post(() -> {
                            int pos = messageList.indexOf(msg);
                            if (pos != -1) {
                                messageList.get(pos).setStatus(ChatMessage.STATUS_FAILED);
                                chatAdapter.notifyItemChanged(pos);
                            }
                        });
                    });
                }
            });
        });
    }

    private void updateLastRenderedMessageToLatest(Long ts) {
        lastRenderedMessageTimestamp = (ts != null) ? ts : System.currentTimeMillis();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(chatRefreshReceiver); } catch (Exception ignored) {}
        mainHandler.removeCallbacksAndMessages(null);
        if (pollHandler != null) pollHandler.removeCallbacksAndMessages(null);
    }
}