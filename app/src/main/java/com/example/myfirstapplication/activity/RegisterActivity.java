package com.example.myfirstapplication.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myfirstapplication.R;
import com.example.myfirstapplication.model.request.LoginRequest;
import com.example.myfirstapplication.model.response.UserResponse;
import com.example.myfirstapplication.network.ApiService;
import com.example.myfirstapplication.utils.NetworkUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private EditText etUser, etPwd;
    // 核心修改1：关闭模拟模式，启用真实接口
    private boolean isMockMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etUser = findViewById(R.id.et_username);
        etPwd = findViewById(R.id.et_password);
        Button btnRegister = findViewById(R.id.btn_register);

        btnRegister.setOnClickListener(v -> {
            String user = etUser.getText().toString().trim();
            String pwd = etPwd.getText().toString().trim();

            // 核心修改2：添加参数校验
            if (user.isEmpty() || pwd.isEmpty()) {
                Toast.makeText(this, "用户名/密码不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isMockMode) {
                Toast.makeText(this, "Mock注册成功，请登录", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                performRealRegister(user, pwd);
            }
        });
    }

    private void performRealRegister(String user, String pwd) {
        // 核心修改3：使用NetworkUtils获取单例ApiService，避免重复创建Retrofit
        ApiService apiService = NetworkUtils.getApiService();

        // 构建注册请求体（复用LoginRequest，后端注册接口参数和登录一致）
        apiService.register(new LoginRequest(user, pwd)).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse result = response.body();
                    // 核心修改4：对齐后端返回码（200成功，其他失败）
                    if (result.getCode() == 200) {
                        Toast.makeText(RegisterActivity.this, "注册成功，请登录", Toast.LENGTH_SHORT).show();
                        finish(); // 返回登录页
                    } else {
                        // 显示后端返回的错误信息（如用户名已存在）
                        Toast.makeText(RegisterActivity.this, "注册失败：" + result.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // 响应为空/状态码非200
                    Toast.makeText(RegisterActivity.this, "注册失败：服务器响应异常", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                // 核心修改5：显示具体的网络错误信息
                Toast.makeText(RegisterActivity.this, "注册异常：" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}