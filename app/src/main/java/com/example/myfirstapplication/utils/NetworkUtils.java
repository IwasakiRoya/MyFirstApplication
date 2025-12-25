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
    // 核心修改：确保和后端IP/端口一致（10.0.2.2是模拟器访问本地PC的地址）
    private static final String BASE_URL = "http://localhost:8080/";

    /**
     * 获取ApiService实例（单例）
     */
    public static ApiService getApiService() {
        if (apiService == null) {
            // 初始化Retrofit
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL) // 已对齐后端地址
                    .addConverterFactory(GsonConverterFactory.create()) // 解析JSON
                    .build();
            // 创建ApiService
            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }

    // 以下回调逻辑保持不变...
    public static void syncFriendRequests(Context context, String token, OnSyncListener listener) {
        getApiService().getFriendRequests("Bearer " + token)
                .enqueue(new Callback<BaseResponse>() {
                    @Override
                    public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse res = response.body();
                            if (res.getCode() == 200) {
                                if (listener != null) {
                                    listener.onSuccess(res);
                                }
                            } else {
                                if (listener != null) {
                                    listener.onError(res.getMsg());
                                }
                            }
                        } else {
                            if (listener != null) {
                                listener.onError("服务器响应异常");
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse> call, Throwable t) {
                        if (listener != null) {
                            listener.onError("网络请求失败：" + t.getMessage());
                        }
                    }
                });
    }

    public static void searchUser(Context context, String token, String keyword, OnSearchResultListener listener) {
        getApiService().searchUser("Bearer " + token, keyword)
                .enqueue(new Callback<BaseResponse>() {
                    @Override
                    public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse res = response.body();
                            if (res.getCode() == 200) {
                                listener.onResult(res);
                            } else {
                                listener.onError(res.getMessage());
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

    public interface OnSyncListener {
        void onSuccess(BaseResponse response);
        void onError(String errorMsg);
    }

    public interface OnSearchResultListener {
        void onResult(BaseResponse response);
        void onError(String errorMsg);
    }

    public static void syncFriendRequests(Context context, String token) {
        syncFriendRequests(context, token, null);
    }
}