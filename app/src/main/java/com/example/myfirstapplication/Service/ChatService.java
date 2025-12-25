package com.example.myfirstapplication.Service;

import static com.example.myfirstapplication.model.ChatMessage.TYPE_RECEIVED;
import static com.example.myfirstapplication.model.ChatMessage.TYPE_SENT;
import static com.example.myfirstapplication.model.ChatMessage.STATUS_SUCCESS;
import static com.example.myfirstapplication.model.ChatMessage.STATUS_THINKING;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.example.myfirstapplication.database.AppDatabase;
import com.example.myfirstapplication.model.request.AiRequest;
import com.example.myfirstapplication.model.response.AiResponse;
import com.example.myfirstapplication.model.ChatMessage;
import com.example.myfirstapplication.model.response.BaseResponse;
import com.example.myfirstapplication.network.ApiService;
import com.example.myfirstapplication.utils.NetworkUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatService extends Service {
    private ApiService apiService;
    private Handler mainHandler;
    private final AppDatabase db = AppDatabase.getInstance(this);
    private static final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        // 核心修改：使用NetworkUtils获取ApiService
        apiService = NetworkUtils.getApiService();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    // 模拟消息接收（保持不变）
    private void onMessageReceived(String friendId, String text) {
        dbExecutor.execute(() -> {
            saveToDb(friendId, text, TYPE_RECEIVED, STATUS_SUCCESS);

            boolean isAutoReply = getSharedPreferences("AI_CONFIG", MODE_PRIVATE)
                    .getBoolean("auto_" + friendId, false);

            if (isAutoReply) {
                final long placeholderId = saveToDb(friendId, "AI正在生成回复...", TYPE_SENT, STATUS_THINKING);

                callAiApi(text, response -> {
                    dbExecutor.execute(() -> {
                        updateDb(placeholderId, response, STATUS_SUCCESS);
                    });
                });
            }
        });
    }

    private Handler syncHandler = new Handler(Looper.getMainLooper());
    private Runnable syncRunnable;
    private long lastSyncTimestamp = 0;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (syncRunnable != null) {
            syncHandler.removeCallbacks(syncRunnable);
        }

        syncRunnable = new Runnable() {
            @Override
            public void run() {
                fetchMessagesFromServer();
                syncHandler.postDelayed(this, 5000);
            }
        };

        syncHandler.post(syncRunnable);
        return START_STICKY;
    }

    // 核心修改：修复接口调用逻辑，对齐后端返回值
    private void fetchMessagesFromServer() {
        String token = getSharedPreferences("USER_INFO", MODE_PRIVATE).getString("token", "");
        if (token.isEmpty()) return;

        // 调用后端未读消息接口
        apiService.getUnreadMessages("Bearer " + token, lastSyncTimestamp).enqueue(new Callback<BaseResponse<List<ChatMessage>>>() {
            @Override
            public void onResponse(Call<BaseResponse<List<ChatMessage>>> call, Response<BaseResponse<List<ChatMessage>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<List<ChatMessage>> res = response.body();
                    if (res.getCode() == 200) {
                        List<ChatMessage> newMessages = res.getData();
                        if (newMessages != null && !newMessages.isEmpty()) {
                            lastSyncTimestamp = System.currentTimeMillis();
                            // 插入本地数据库并通知UI
                            dbExecutor.execute(() -> {
                                for (ChatMessage msg : newMessages) {
                                    db.chatDao().insert(msg);
                                }
                                // 发送广播通知刷新
                                Intent intent = new Intent("com.example.REFRESH_CHAT");
                                intent.putExtra("friendId", newMessages.get(0).getFriendId());
                                sendBroadcast(intent);
                            });
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<List<ChatMessage>>> call, Throwable t) {
                // 忽略失败，下次轮询重试
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (syncHandler != null && syncRunnable != null) {
            syncHandler.removeCallbacks(syncRunnable);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    // 1. 保存消息到数据库（修复构造方法和参数传递）
    private long saveToDb(String friendId, String text, int type, int status) {
        // 获取当前用户ID
        SharedPreferences sp = getSharedPreferences("USER_INFO", MODE_PRIVATE);
        String userId = sp.getString("userId", "");

        // 构造消息对象
        ChatMessage msg = new ChatMessage(friendId, text, type, status, userId);

        // 用Thread执行数据库操作，通过原子类获取返回值
        final java.util.concurrent.atomic.AtomicLong insertId = new java.util.concurrent.atomic.AtomicLong(0);
        new Thread(() -> {
            insertId.set(AppDatabase.getInstance(this).chatDao().insert(msg));
        }).start();

        // 短暂等待确保插入完成（简单兜底，也可改用回调）
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return insertId.get();
    }

    // 更新消息（完全适配Service）
    private void updateDb(long placeholderId, String response, int status) {
        // 转换long到Integer（匹配ChatDao参数）
        Integer msgId = (placeholderId > 0) ? (int) placeholderId : 0;

        // 子线程执行数据库更新
        new Thread(() -> {
            AppDatabase.getInstance(this).chatDao().updateMessage(msgId, response, status);

            // Service中切换到主线程的两种方式（任选其一）
            // 方式1：临时创建主线程Handler（推荐）
            new Handler(Looper.getMainLooper()).post(() -> {
                Intent refreshIntent = new Intent("com.example.REFRESH_CHAT_LIST");
                // Service发送广播必须用getApplicationContext()，避免内存泄漏
                getApplicationContext().sendBroadcast(refreshIntent);
            });

        }).start();
    }

    // AI接口调用（原生JSONObject，无Gson）
    private void callAiApi(String userText, AiCallback callback) {
        SharedPreferences sp = getSharedPreferences("AI_CONFIG", MODE_PRIVATE);
        String apiKey = sp.getString("api_key", "");
        String aiStyle = sp.getString("ai_style", "默认风格");

        // 原生JSONObject构造请求体
        String requestJson = "";
        try {
            JSONObject requestObj = new JSONObject();
            requestObj.put("aiStyle", aiStyle);
            requestObj.put("userText", userText);
            requestJson = requestObj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            callback.onSuccess("请求参数构造失败，请重试");
            return;
        }

        // 调用接口
        apiService.getAiResponse("Bearer " + apiKey, requestJson)
                .enqueue(new Callback<BaseResponse<String>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<String>> call, Response<BaseResponse<String>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse<String> baseRes = response.body();
                            if (baseRes.getCode() == 200) {
                                callback.onSuccess(baseRes.getData() != null ? baseRes.getData() : "AI回复为空");
                            } else {
                                callback.onSuccess("AI回复获取失败：" + baseRes.getMessage());
                            }
                        } else {
                            callback.onSuccess("AI回复获取失败，请重试");
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<String>> call, Throwable t) {
                        callback.onSuccess("AI暂时开小差了...");
                    }
                });
    }

    // 补充：AiCallback接口定义（确保回调方法存在）
    public interface AiCallback {
        void onSuccess(String answer);
    }

    // 补充：AiRequest实体类（示例，保证能转JSON）
    class AiRequest {
        private String aiStyle;
        private String userText;

        public AiRequest(String aiStyle, String userText) {
            this.aiStyle = aiStyle;
            this.userText = userText;
        }

        // Getter（Gson序列化需要）
        public String getAiStyle() {
            return aiStyle;
        }

        public String getUserText() {
            return userText;
        }
    }
}