package com.example.myfirstapplication.Fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.myfirstapplication.R;
import com.example.myfirstapplication.activity.LoginActivity;
import com.example.myfirstapplication.model.User;
import com.example.myfirstapplication.model.request.ChangePwdRequest;
import com.example.myfirstapplication.model.response.ApiAiModelResponse;
import com.example.myfirstapplication.model.response.BaseResponse;
import com.example.myfirstapplication.model.response.ChangePwdResponse;
import com.example.myfirstapplication.network.ApiService;
import com.example.myfirstapplication.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MeFragment extends Fragment {
    // 控件声明
    private ImageView ivAvatar;
    private TextView tvNickname, tvUserId, tvEditInfo, tvChangePwd, tvLogout;
    private LinearLayout llInfoEditor;
    private EditText etNickname, etSignature, etPhone, etApiKey, etAiPrompt;
    private Spinner spAiModel;
    private Button btnSaveInfo, btnSaveAiConfig;

    // 数据
    private User currentUser;
    private String token;
    private ApiService apiService;
    private List<String> aiModelList = new ArrayList<>(); // AI模型列表
    private static final int REQUEST_CODE_AVATAR = 1001; // 头像选择请求码

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_me, container, false);
        initView(view); // 初始化控件
        initData();     // 初始化数据（获取登录用户信息）
        initListener(); // 绑定点击事件
        return view;
    }

    // 1. 初始化控件
    private void initView(View view) {
        ivAvatar = view.findViewById(R.id.iv_avatar);
        tvNickname = view.findViewById(R.id.tv_nickname);
        tvUserId = view.findViewById(R.id.tv_user_id);
        tvEditInfo = view.findViewById(R.id.tv_edit_info);
        tvChangePwd = view.findViewById(R.id.tv_change_pwd);
        tvLogout = view.findViewById(R.id.tv_logout);

        llInfoEditor = view.findViewById(R.id.ll_info_editor);
        etNickname = view.findViewById(R.id.et_nickname);
        etSignature = view.findViewById(R.id.et_signature);
        etPhone = view.findViewById(R.id.et_phone);

        etApiKey = view.findViewById(R.id.et_api_key);
        spAiModel = view.findViewById(R.id.sp_ai_model);
        etAiPrompt = view.findViewById(R.id.et_ai_prompt);

        btnSaveInfo = view.findViewById(R.id.btn_save_info);
        btnSaveAiConfig = view.findViewById(R.id.btn_save_ai_config);

        // 初始化ApiService
        apiService = NetworkUtils.getApiService();
    }

    // 2. 初始化数据（获取用户信息+AI模型列表）
    private void initData() {
        // 获取本地Token和用户ID
        SharedPreferences sp = getContext().getSharedPreferences("USER_INFO", 0);
        token = sp.getString("token", "");
        String userId = sp.getString("userId", "");

        // 未登录：跳转到登录页
        if (token.isEmpty() || userId.isEmpty()) {
            jumpToLogin();
            return;
        }

        // 已登录：获取用户信息
        apiService.getUserInfo("Bearer " + token).enqueue(new Callback<BaseResponse<User>>() {
            @Override
            public void onResponse(Call<BaseResponse<User>> call, Response<BaseResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<User> res = response.body();
                    if (res.getCode() == 200) {
                        currentUser = res.getData();
                        if (currentUser != null) {
                            // 填充个人信息
                            tvNickname.setText(currentUser.getNickname() != null ? currentUser.getNickname() : "默认昵称");
                            tvUserId.setText("ID：" + currentUser.getUserId());
                            etNickname.setText(currentUser.getNickname());
                            etSignature.setText(currentUser.getSignature());
                            etPhone.setText(currentUser.getPhoneNumber() + "");

                            // 填充AI配置
                            etApiKey.setText(currentUser.getApiKey());
                            etAiPrompt.setText(currentUser.getAiPrompt());

                            // 加载头像（Glide需添加依赖：implementation 'com.github.bumptech.glide:glide:4.16.0'）
                            if (currentUser.getAvatarUrl() != null && !currentUser.getAvatarUrl().isEmpty()) {
                                Glide.with(MeFragment.this).load(currentUser.getAvatarUrl()).into(ivAvatar);
                            }

                            // 查询AI模型列表（如果有ApiKey）
                            if (currentUser.getApiKey() != null && !currentUser.getApiKey().isEmpty()) {
                                getAiModels(currentUser.getApiKey());
                            }
                        }
                    } else {
                        Toast.makeText(getContext(), res.getMsg(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<User>> call, Throwable t) {
                Toast.makeText(getContext(), "获取用户信息失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 3. 绑定所有点击事件
    private void initListener() {
        // 3.1 头像点击：选择/拍照更换
        ivAvatar.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("选择头像")
                    .setItems(new String[]{"从相册选择", "拍照"}, (dialog, which) -> {
                        Intent intent = new Intent();
                        if (which == 0) { // 相册
                            intent.setAction(Intent.ACTION_PICK);
                            intent.setType("image/*");
                        } else { // 拍照
                            intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                        }
                        startActivityForResult(intent, REQUEST_CODE_AVATAR);
                    }).show();
        });

        // 3.2 编辑个人信息：显示/隐藏编辑区
        tvEditInfo.setOnClickListener(v -> {
            if (llInfoEditor.getVisibility() == View.GONE) {
                llInfoEditor.setVisibility(View.VISIBLE);
                tvEditInfo.setText("取消");
            } else {
                llInfoEditor.setVisibility(View.GONE);
                tvEditInfo.setText("编辑");
            }
        });

        // 3.3 保存个人信息
        btnSaveInfo.setOnClickListener(v -> {
            if (currentUser == null) return;
            // 更新用户信息（排除userId）
            currentUser.setNickname(etNickname.getText().toString().trim());
            currentUser.setSignature(etSignature.getText().toString().trim());
            currentUser.setPhoneNumber(Long.parseLong(etPhone.getText().toString().trim()));

            // 调用接口保存
            apiService.updateUserInfo("Bearer " + token, currentUser).enqueue(new Callback<BaseResponse>() {
                @Override
                public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        BaseResponse res = response.body();
                        Toast.makeText(getContext(), res.getMsg(), Toast.LENGTH_SHORT).show();
                        if (res.getCode() == 200) {
                            // 更新UI+本地缓存
                            tvNickname.setText(currentUser.getNickname());
                            llInfoEditor.setVisibility(View.GONE);
                            tvEditInfo.setText("编辑");
                        }
                    }
                }

                @Override
                public void onFailure(Call<BaseResponse> call, Throwable t) {
                    Toast.makeText(getContext(), "保存失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        // 3.4 API Key输入后查询模型列表
        etApiKey.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) { // 失去焦点时查询
                String apiKey = etApiKey.getText().toString().trim();
                if (!apiKey.isEmpty()) {
                    getAiModels(apiKey);
                }
            }
        });

        // 3.5 保存AI配置
        btnSaveAiConfig.setOnClickListener(v -> {
            if (currentUser == null) return;
            String apiKey = etApiKey.getText().toString().trim();
            String aiModel = (String) spAiModel.getSelectedItem();
            String aiPrompt = etAiPrompt.getText().toString().trim();

            // 更新用户AI配置
            currentUser.setApiKey(apiKey);
            currentUser.setAiModel(aiModel);
            currentUser.setAiPrompt(aiPrompt);

            // 调用接口保存
            apiService.saveAiConfig("Bearer " + token, currentUser).enqueue(new Callback<BaseResponse>() {
                @Override
                public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        BaseResponse res = response.body();
                        Toast.makeText(getContext(), res.getMsg(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<BaseResponse> call, Throwable t) {
                    Toast.makeText(getContext(), "保存AI配置失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        // 3.6 修改密码
        tvChangePwd.setOnClickListener(v -> {
            // 弹出密码修改对话框
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_pwd, null);
            EditText etOldPwd = dialogView.findViewById(R.id.et_old_pwd);
            EditText etNewPwd = dialogView.findViewById(R.id.et_new_pwd);
            EditText etConfirmPwd = dialogView.findViewById(R.id.et_confirm_pwd);

            new AlertDialog.Builder(getContext())
                    .setTitle("修改密码")
                    .setView(dialogView)
                    .setPositiveButton("确认", (dialog, which) -> {
                        String oldPwd = etOldPwd.getText().toString().trim();
                        String newPwd = etNewPwd.getText().toString().trim();
                        String confirmPwd = etConfirmPwd.getText().toString().trim();

                        // 校验
                        if (oldPwd.isEmpty() || newPwd.isEmpty() || confirmPwd.isEmpty()) {
                            Toast.makeText(getContext(), "密码不能为空", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (!newPwd.equals(confirmPwd)) {
                            Toast.makeText(getContext(), "两次密码不一致", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // 调用修改密码接口
                        ChangePwdRequest request = new ChangePwdRequest();
                        request.setUserId(currentUser.getUserId());
                        request.setOldPwd(oldPwd);
                        request.setNewPwd(newPwd);

                        apiService.changePassword("Bearer " + token, request).enqueue(new Callback<ChangePwdResponse>() {
                            @Override
                            public void onResponse(Call<ChangePwdResponse> call, Response<ChangePwdResponse> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    ChangePwdResponse res = response.body();
                                    Toast.makeText(getContext(), res.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<ChangePwdResponse> call, Throwable t) {
                                Toast.makeText(getContext(), "修改失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        // 3.7 退出登录
        tvLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("确认退出")
                    .setMessage("是否退出当前账号？")
                    .setPositiveButton("退出", (dialog, which) -> {
                        // 调用后端退出接口（可选）
                        apiService.logout("Bearer " + token).enqueue(new Callback<BaseResponse>() {
                            @Override
                            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                                // 清理本地缓存
                                SharedPreferences sp = getContext().getSharedPreferences("USER_INFO", 0);
                                sp.edit().clear().apply();
                                // 跳转到登录页
                                jumpToLogin();
                            }

                            @Override
                            public void onFailure(Call<BaseResponse> call, Throwable t) {
                                // 接口失败也清理本地缓存
                                SharedPreferences sp = getContext().getSharedPreferences("USER_INFO", 0);
                                sp.edit().clear().apply();
                                jumpToLogin();
                            }
                        });
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    // 辅助：查询AI模型列表（硅基流动）
    private void getAiModels(String apiKey) {
        apiService.getAiModels(apiKey).enqueue(new Callback<ApiAiModelResponse>() {
            @Override
            public void onResponse(Call<ApiAiModelResponse> call, Response<ApiAiModelResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiAiModelResponse res = response.body();
                    if (res.getCode() == 200) {
                        aiModelList = res.getData();
                        // 填充下拉框
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                                android.R.layout.simple_spinner_item, aiModelList);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spAiModel.setAdapter(adapter);

                        // 选中当前用户的模型
                        if (currentUser.getAiModel() != null) {
                            int position = aiModelList.indexOf(currentUser.getAiModel());
                            if (position != -1) {
                                spAiModel.setSelection(position);
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiAiModelResponse> call, Throwable t) {
                Toast.makeText(getContext(), "查询模型列表失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 辅助：跳转到登录页
    private void jumpToLogin() {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);
        getActivity().finish(); // 关闭当前页面
    }

    // 处理头像选择返回
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_AVATAR && resultCode == getActivity().RESULT_OK && data != null) {
            // 获取头像Uri（实际项目中需上传到服务器，返回URL后更新user.avatarUrl）
            Uri uri = data.getData();
            if (uri != null) {
                ivAvatar.setImageURI(uri);
                // 此处省略上传头像到服务器的逻辑，上传成功后更新currentUser.avatarUrl并调用updateUserInfo
            }
        }
    }
}