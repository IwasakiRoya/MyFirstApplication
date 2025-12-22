package com.example.myfirstapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myfirstapplication.POJO.LoginRequest;
import com.example.myfirstapplication.POJO.UserResponse;
import com.example.myfirstapplication.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RegisterActivity extends AppCompatActivity {
    private EditText etUser, etPwd;
    private boolean isMockMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register); // 布局复用登录页的逻辑

        etUser = findViewById(R.id.et_username);
        etPwd = findViewById(R.id.et_password);
        Button btnRegister = findViewById(R.id.btn_register);

        btnRegister.setOnClickListener(v -> {
            String user = etUser.getText().toString();
            String pwd = etPwd.getText().toString();

            if (isMockMode) {
                Toast.makeText(this, "Mock注册成功，请登录", Toast.LENGTH_SHORT).show();
                finish(); // 注册成功回到登录页
            } else {
                performRealRegister(user, pwd);
            }
        });
    }

    private void performRealRegister(String user, String pwd) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        apiService.register(new LoginRequest(user, pwd)).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body().code == 200) {
                    Toast.makeText(RegisterActivity.this, "注册成功", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "注册异常", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
