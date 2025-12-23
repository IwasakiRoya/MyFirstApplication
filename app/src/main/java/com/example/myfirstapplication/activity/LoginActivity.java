package com.example.myfirstapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myfirstapplication.R;
import com.example.myfirstapplication.model.request.LoginRequest;
import com.example.myfirstapplication.model.response.UserResponse;
import com.example.myfirstapplication.network.ApiService;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private final boolean isMockMode = true; // 设置为 true 则使用本地假逻辑

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
                // --- 本地假逻辑 ---
                if (user.equals("admin") && pwd.equals("123456")) {
                    handleLoginSuccess();
                } else {
                    Toast.makeText(this, "用户名或密码错误(admin/123456)", Toast.LENGTH_SHORT).show();
                }
            } else {
                // --- 以后部署接口后的真逻辑 ---
                performRealLogin(user, pwd);
            }
        });

        // 在 LoginActivity 的 onCreate 中添加
        TextView tvRegister = findViewById(R.id.tv_register);
        tvRegister.setOnClickListener(v -> {
            // 跳转到注册页面
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void handleLoginSuccess() {
        Toast.makeText(this, "登录成功！", Toast.LENGTH_SHORT).show();
        // 跳转到主界面
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish(); // 销毁登录页
    }

    private void performRealLogin(String user, String pwd) {
        // 1. 初始化 Retrofit
        // 注意：如果是电脑本地运行后端，模拟器访问电脑 IP 通常用 10.0.2.2
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/") // 替换为你后端的实际地址
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        // 2. 构建请求体
        LoginRequest loginRequest = new LoginRequest(user, pwd);

        // 3. 执行异步请求
        apiService.login(loginRequest).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse result = response.body();
                    if (result.code == 200) {
                        // 保存 Token (可选) 并跳转
                        handleLoginSuccess();
                    } else {
                        Toast.makeText(LoginActivity.this, "登录失败: " + result.message, Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                // 网络异常（如断网、服务器宕机）
                Toast.makeText(LoginActivity.this, "网络请求失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("NetworkError", Objects.requireNonNull(t.getMessage()));
            }
        });
    }
}