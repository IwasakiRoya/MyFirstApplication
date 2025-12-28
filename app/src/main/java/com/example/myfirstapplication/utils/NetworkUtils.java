package com.example.myfirstapplication.utils;
import android.content.Context;
import android.content.SharedPreferences;

import com.example.myfirstapplication.model.FriendRequestEntity;
import com.example.myfirstapplication.model.User;
import com.example.myfirstapplication.model.response.BaseResponse;
import com.example.myfirstapplication.network.ApiService;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 通用网络工具类：封装Retrofit初始化、通用网络请求逻辑
 */
public class NetworkUtils {
    // 线程安全的单例（AtomicReference + 双重检查锁）
    private static final AtomicReference<ApiService> API_SERVICE_REF = new AtomicReference<>();
    private static final String BASE_URL = "http://10.0.2.2:8080/";

    public static String getTokenFromSharedPref(Context context) {
        if (context == null) return "";
        SharedPreferences sp = context.getSharedPreferences("USER_INFO", 0);
        return sp.getString("token", "").trim();
    }

    /**
     * 从SharedPreferences获取当前用户ID（复用逻辑，避免重复代码）
     */
    public static String getUserIdFromSharedPref(Context context) {
        if (context == null) return "";
        SharedPreferences sp = context.getSharedPreferences("USER_INFO", 0);
        return sp.getString("userId", "").trim();
    }

    /**
     * 获取ApiService实例（线程安全单例）
     */
    public static ApiService getApiService() {
        if (API_SERVICE_REF.get() == null) {
            synchronized (NetworkUtils.class) {
                if (API_SERVICE_REF.get() == null) {
                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                    API_SERVICE_REF.set(retrofit.create(ApiService.class));
                }
            }
        }
        return API_SERVICE_REF.get();
    }

    // ========== 好友请求同步（精准泛型匹配） ==========
    // 定义专属回调接口，明确数据类型
    public interface OnFriendRequestSyncListener {
        void onSuccess(List<FriendRequestEntity> requestList);
        void onError(String errorMsg);
    }

    // 修复核心：使用精准的泛型 Callback<BaseResponse<List<FriendRequestEntity>>>
    public static void syncFriendRequests(Context context, String token, OnFriendRequestSyncListener listener) {
        // 前置空值校验
        if (context == null || listener == null || token == null || token.isEmpty()) {
            if (listener != null) {
                listener.onError("参数不能为空");
            }
            return;
        }

        getApiService().getFriendRequests(token)
                // 关键修复：使用精准泛型，匹配ApiService的返回类型
                .enqueue(new Callback<BaseResponse<List<FriendRequestEntity>>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<List<FriendRequestEntity>>> call,
                                           Response<BaseResponse<List<FriendRequestEntity>>> response) {
                        if (!response.isSuccessful()) {
                            listener.onError("服务器响应异常，状态码：" + response.code());
                            return;
                        }

                        BaseResponse<List<FriendRequestEntity>> res = response.body();
                        if (res == null) {
                            listener.onError("服务器返回空数据");
                            return;
                        }

                        // 使用BaseResponse的isSuccess()方法简化判断
                        if (res.getCode() == 200) {
                            // 兜底空列表，避免调用方处理null
                            listener.onSuccess(res.getData() == null ? List.of() : res.getData());
                        } else {
                            listener.onError(res.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<List<FriendRequestEntity>>> call, Throwable t) {
                        listener.onError("网络请求失败：" + (t.getMessage() != null ? t.getMessage() : "未知错误"));
                    }
                });
    }

    // ========== 用户搜索（精准泛型匹配） ==========
    // 定义专属回调接口，明确返回User类型
    public interface OnUserSearchListener {
        void onResult(User user); // 若返回列表则改为 List<User>
        void onError(String errorMsg);
    }

    public static void searchUser(Context context, String token, String keyword, OnUserSearchListener listener) {
        // 前置空值校验
        if (context == null || listener == null || token == null || keyword == null) {
            if (listener != null) {
                listener.onError("参数不能为空");
            }
            return;
        }

        getApiService().searchUser(token, keyword)
                .enqueue(new Callback<BaseResponse<User>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<User>> call, Response<BaseResponse<User>> response) {
                        if (!response.isSuccessful()) {
                            listener.onError("服务器响应异常，状态码：" + response.code());
                            return;
                        }

                        BaseResponse<User> res = response.body();
                        if (res == null) {
                            listener.onError("服务器返回空数据");
                            return;
                        }

                        if (res.getCode() == 200) {
                            listener.onResult(res.getData());
                        } else {
                            listener.onError(res.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<User>> call, Throwable t) {
                        listener.onError("网络请求失败：" + (t.getMessage() != null ? t.getMessage() : "未知错误"));
                    }
                });
    }

    // ========== 新增：精准获取好友用户信息（基于 getUserInfo 接口） ==========
    public interface OnGetFriendUserInfoListener {
        void onSuccess(User user);
        void onError(String errorMsg);
    }

    public static void getFriendUserInfo(Context context, String token, String userId, OnGetFriendUserInfoListener listener) {
        // 前置空值校验
        if (context == null || listener == null || token == null || token.isEmpty() || userId == null || userId.isEmpty()) {
            if (listener != null) {
                listener.onError("参数不能为空");
            }
            return;
        }

        // 调用精准的 getUserInfo 接口
        getApiService().getUserInfo(token, userId)
                .enqueue(new Callback<BaseResponse<User>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<User>> call, Response<BaseResponse<User>> response) {
                        if (!response.isSuccessful()) {
                            listener.onError("服务器响应异常，状态码：" + response.code());
                            return;
                        }

                        BaseResponse<User> res = response.body();
                        if (res == null) {
                            listener.onError("服务器返回空数据");
                            return;
                        }

                        if (res.getCode() == 200 && res.getData() != null) {
                            listener.onSuccess(res.getData()); // 返回精准的用户信息
                        } else {
                            listener.onError(res.getMessage() != null ? res.getMessage() : "未查询到该用户信息");
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<User>> call, Throwable t) {
                        listener.onError("网络请求失败：" + (t.getMessage() != null ? t.getMessage() : "未知错误"));
                    }
                });
    }

    // 重载方法（保留，供无需监听结果的场景使用）
    public static void syncFriendRequests(Context context, String token) {
        syncFriendRequests(context, token, new OnFriendRequestSyncListener() {
            @Override
            public void onSuccess(List<FriendRequestEntity> requestList) {
                // 默认空实现
            }

            @Override
            public void onError(String errorMsg) {
                // 默认空实现
            }
        });
    }
}