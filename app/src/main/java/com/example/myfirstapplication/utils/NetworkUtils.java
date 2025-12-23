package com.example.myfirstapplication.utils;

import android.content.Context;

import com.example.myfirstapplication.model.response.BaseResponse;
import com.example.myfirstapplication.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 通用网络工具类：封装Retrofit初始化、通用网络请求逻辑
 */
public class NetworkUtils {
    // 单例模式：避免重复创建Retrofit实例
    private static ApiService apiService;
    // 后端接口基础地址（根据你的实际后端地址修改！）
    private static final String BASE_URL = "http://10.0.2.2:8080/";

    /**
     * 获取ApiService实例（单例）
     */
    public static ApiService getApiService() {
        if (apiService == null) {
            // 初始化Retrofit
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL) // 替换为你的后端实际地址
                    .addConverterFactory(GsonConverterFactory.create()) // 解析JSON
                    .build();
            // 创建ApiService
            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }

    /**
     * 通用：同步后端好友请求到本地
     * @param context 上下文
     * @param token 鉴权Token
     * @param listener 回调（可选，用于通知同步结果）
     */
    public static void syncFriendRequests(Context context, String token, OnSyncListener listener) {
        getApiService().getFriendRequests("Bearer " + token)
                .enqueue(new Callback<BaseResponse>() {
                    @Override
                    public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse res = response.body();
                            if (res.getCode() == 200) {
                                // 同步成功，通过回调返回结果
                                if (listener != null) {
                                    listener.onSuccess(res);
                                }
                            } else {
                                // 业务错误（如Token失效）
                                if (listener != null) {
                                    listener.onError(res.getMsg());
                                }
                            }
                        } else {
                            // 网络请求成功，但响应异常
                            if (listener != null) {
                                listener.onError("服务器响应异常");
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse> call, Throwable t) {
                        // 网络请求失败（如无网络、服务器宕机）
                        if (listener != null) {
                            listener.onError("网络请求失败：" + t.getMessage());
                        }
                    }
                });
    }

    /**
     * 通用：搜索用户（关键词）
     * @param context 上下文
     * @param token 鉴权Token
     * @param keyword 搜索关键词（ID/手机号/昵称）
     * @param listener 搜索结果回调
     */
    public static void searchUser(Context context, String token, String keyword, OnSearchResultListener listener) {
        getApiService().searchUser("Bearer " + token, keyword)
                .enqueue(new Callback<BaseResponse>() {
                    @Override
                    public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse res = response.body();
                            if (res.getCode() == 200) {
                                // 搜索成功，返回结果
                                listener.onResult(res);
                            } else {
                                // 业务错误
                                listener.onError(res.getMsg());
                            }
                        } else {
                            listener.onError("服务器响应异常");
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse> call, Throwable t) {
                        listener.onError("网络请求失败：" + t.getMessage());
                    }
                });
    }

    // ========== 回调接口定义 ==========
    /**
     * 同步操作回调（如同步好友请求）
     */
    public interface OnSyncListener {
        void onSuccess(BaseResponse response); // 同步成功
        void onError(String errorMsg);         // 同步失败
    }

    /**
     * 搜索用户回调
     */
    public interface OnSearchResultListener {
        void onResult(BaseResponse response);  // 搜索成功
        void onError(String errorMsg);         // 搜索失败
    }

    // ========== 可选：简化版（无回调） ==========
    /**
     * 简化版：同步好友请求（无回调，仅静默同步）
     */
    public static void syncFriendRequests(Context context, String token) {
        syncFriendRequests(context, token, null);
    }
}