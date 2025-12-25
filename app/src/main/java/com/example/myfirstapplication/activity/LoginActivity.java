package com.example.myfirstapplication.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myfirstapplication.R;
import com.example.myfirstapplication.model.request.LoginRequest;
import com.example.myfirstapplication.model.response.UserResponse;
import com.example.myfirstapplication.network.ApiService;
import com.example.myfirstapplication.utils.NetworkUtils;

import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    // 核心修改：关闭模拟模式，启用真实接口
    private final boolean isMockMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        Button btnLogin = findViewById(R.id.btn_login);

        btnLogin.setOnClickListener(v -> {
            String user = etUsername.getText().toString();
            String pwd = etPassword.getText().toString();

            if (isMockMode) {
                // 模拟逻辑（已关闭，保留备用）
                if (user.equals("admin") && pwd.equals("123456")) {
                    saveMockLoginInfo(user);
                    handleLoginSuccess();
                } else {
                    Toast.makeText(this, "用户名或密码错误(admin/123456)", Toast.LENGTH_SHORT).show();
                }
            } else {
                // 调用真实登录接口
                performRealLogin(user, pwd);
            }
        });

        // 注册按钮跳转
        TextView tvRegister = findViewById(R.id.tv_register);
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    // 模拟登录信息保存（备用）
    private void saveMockLoginInfo(String username) {
        SharedPreferences sp = getSharedPreferences("USER_INFO", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("userId", "1000");
        editor.putString("token", "mock_token_123456");
        editor.putString("nickname", "管理员");
        editor.putString("avatarUrl", "");
        editor.apply();
    }

    private void handleLoginSuccess() {
        Toast.makeText(this, "登录成功！", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    // 核心修改：使用NetworkUtils获取ApiService，统一管理Retrofit实例
    private void performRealLogin(String user, String pwd) {
        // 1. 获取ApiService实例（单例）
        ApiService apiService = NetworkUtils.getApiService();

        // 2. 构建请求体
        LoginRequest loginRequest = new LoginRequest(user, pwd);

        // 3. 执行异步请求
        apiService.login(loginRequest).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse result = response.body();
                    if (result.getCode() == 200) {
                        // 保存后端返回的真实信息
                        SharedPreferences sp = getSharedPreferences("USER_INFO", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("userId", result.getData().getUserId());
                        editor.putString("token", result.getData().getToken());
                        editor.putString("nickname", result.getData().getNickname());
                        editor.putString("avatarUrl", result.getData().getAvatarUrl());
                        editor.apply();

                        handleLoginSuccess();
                    } else {
                        Toast.makeText(LoginActivity.this, "登录失败: " + result.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "登录失败：服务器返回空数据", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                Toast.makeText(LoginActivity.this, "网络请求失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("LoginError", Objects.requireNonNull(t.getMessage()));
            }
        });
    }
}