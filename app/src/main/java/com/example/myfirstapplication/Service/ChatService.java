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

import com.example.myfirstapplication.database.AppDatabase; // 导入你之前写的AppDatabase
import com.example.myfirstapplication.model.request.AiRequest;
import com.example.myfirstapplication.model.response.AiResponse;
import com.example.myfirstapplication.model.ChatMessage;
import com.example.myfirstapplication.model.response.BaseResponse;
import com.example.myfirstapplication.network.ApiService; // 假设你有这个网络服务类

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChatService extends Service {
    private ApiService apiService; // 网络请求服务
    private Handler mainHandler;   // 主线程处理器（更新UI用）
    private final AppDatabase db = AppDatabase.getInstance(this);

    // 在类顶部定义
    private static final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化Retrofit（根据你的实际地址修改）
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://your-api-domain.com/") // 替换成你的AI接口地址
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        // 初始化主线程Handler
        mainHandler = new Handler(Looper.getMainLooper());
    }

    // 模拟消息接收（实际可替换为WebSocket/推送回调）
    private void onMessageReceived(String friendId, String text) {
        dbExecutor.execute(() -> {
            // 1. 存入接收消息
            saveToDb(friendId, text, TYPE_RECEIVED, STATUS_SUCCESS);

            // 2. 检查开关
            boolean isAutoReply = getSharedPreferences("AI_CONFIG", MODE_PRIVATE)
                    .getBoolean("auto_" + friendId, false);

            if (isAutoReply) {
                // 3. 存入占位消息并获取 ID
                final long placeholderId = saveToDb(friendId, "AI正在生成回复...", TYPE_SENT, STATUS_THINKING);

                // 4. 发送网络请求 (Retrofit 已经在内部异步了，不需要放在 dbExecutor 里)
                callAiApi(text, response -> {
                    // 5. 结果回来后，切回数据库线程更新内容
                    dbExecutor.execute(() -> {
                        updateDb(placeholderId, response, STATUS_SUCCESS);
                    });
                });
            }
        });
    }

    private Handler syncHandler = new Handler(Looper.getMainLooper());
    private Runnable syncRunnable;
    private long lastSyncTimestamp = 0; // 记录最后一次同步时间，后端查询增量用

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 1. 如果已经启动了轮询，先移除之前的任务，防止重复开启
        if (syncRunnable != null) {
            syncHandler.removeCallbacks(syncRunnable);
        }

        // 2. 定义轮询逻辑
        syncRunnable = new Runnable() {
            @Override
            public void run() {
                // 执行网络请求，从你的服务器拉取新消息
                fetchMessagesFromServer();

                // 5秒后再次执行
                syncHandler.postDelayed(this, 5000);
            }
        };

        // 3. 立即开始第一次执行
        syncHandler.post(syncRunnable);

        // 4. 返回 START_STICKY：如果 Service 被系统杀死，系统会尝试重新创建它
        return START_STICKY;
    }

    // 替换ChatService中的fetchMessagesFromServer方法
    private void fetchMessagesFromServer() {
        String token = getSharedPreferences("USER_INFO", MODE_PRIVATE).getString("token", "");
        if (token.isEmpty()) return;

        // 拉取所有会话的新消息
        apiService.getUnreadMessages("Bearer " + token, lastSyncTimestamp).enqueue(new Callback<BaseResponse<List<ChatMessage>>>() {
            @Override
            public void onResponse(Call<BaseResponse<List<ChatMessage>>> call, Response<BaseResponse<List<ChatMessage>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    List<ChatMessage> newMessages = response.body().getData();
                    if (newMessages != null && !newMessages.isEmpty()) {
                        lastSyncTimestamp = System.currentTimeMillis();
                        // 插入本地并通知UI
                        dbExecutor.execute(() -> {
                            for (ChatMessage msg : newMessages) {
                                db.chatDao().insert(msg);
                            }
                            // 发送广播通知对应ChatActivity刷新
                            Intent intent = new Intent("com.example.REFRESH_CHAT");
                            intent.putExtra("friendId", newMessages.get(0).getFriendId());
                            sendBroadcast(intent);
                        });
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
        // 5. Service 销毁时停止轮询，防止内存泄漏
        if (syncHandler != null && syncRunnable != null) {
            syncHandler.removeCallbacks(syncRunnable);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    // 关键修改：status参数改为int类型
    private long saveToDb(String friendId, String text, int type, int status) {
        ChatMessage msg = new ChatMessage(friendId, text, type, status);
        // Room操作必须在子线程（这里已经在子线程中执行）
        return AppDatabase.getInstance(this).chatDao().insert(msg);
    }

    // 关键修改：status参数改为int类型
    private void updateDb(long placeholderId, String response, int status) {
        AppDatabase.getInstance(this).chatDao().updateMessage(placeholderId, response, status);
        // 如果需要通知UI刷新，可通过广播/EventBus实现
        mainHandler.post(() -> {
            // 示例：发送广播通知Activity刷新列表
            Intent refreshIntent = new Intent("com.example.REFRESH_CHAT_LIST");
            sendBroadcast(refreshIntent);
        });
    }

    private void callAiApi(String userText, AiCallback callback) {
        // 1. 获取配置
        SharedPreferences sp = getSharedPreferences("AI_CONFIG", MODE_PRIVATE);
        String apiKey = sp.getString("api_key", "");
        String aiStyle = sp.getString("ai_style", "默认风格");

        // 2. 构建请求体
        AiRequest request = new AiRequest(aiStyle, userText);

        // 3. 发送请求
        apiService.getAiResponse("Bearer " + apiKey, request).enqueue(new Callback<AiResponse>() {
            @Override
            public void onResponse(Call<AiResponse> call, Response<AiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().getAnswer());
                } else {
                    callback.onSuccess("AI回复获取失败，请重试");
                }
            }

            @Override
            public void onFailure(Call<AiResponse> call, Throwable t) {
                callback.onSuccess("AI暂时开小差了...");
            }
        });
    }

    // AI回调接口
    interface AiCallback {
        void onSuccess(String response);
    }
}