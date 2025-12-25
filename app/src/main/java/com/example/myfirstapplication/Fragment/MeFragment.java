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
import com.example.myfirstapplication.model.response.BaseResponse;
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
    private List<String> aiModelList = new ArrayList<>();
    private static final int REQUEST_CODE_AVATAR = 1001;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_me, container, false);
        initView(view);
        initData();
        initListener();
        return view;
    }

    private void initView(View view) {
        // 控件绑定
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

        // 获取ApiService
        apiService = NetworkUtils.getApiService();
    }

    private void initData() {
        // 空上下文校验（核心修复1）
        if (getContext() == null) return;

        // 获取本地Token和用户ID
        SharedPreferences sp = getContext().getSharedPreferences("USER_INFO", 0);
        token = sp.getString("token", "");
        String userId = sp.getString("userId", "");

        // 未登录：跳转到登录页
        if (token.isEmpty() || userId.isEmpty()) {
            if (getActivity() != null && !getActivity().isFinishing()) {
                jumpToLogin();
            }
            return;
        }

        // 调用后端接口获取用户信息（核心修复2：泛型匹配BaseResponse<User>）
        apiService.getUserInfo("Bearer " + token).enqueue(new Callback<BaseResponse<User>>() {
            @Override
            public void onResponse(Call<BaseResponse<User>> call, Response<BaseResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<User> res = response.body();
                    if (res.getCode() == 200) {
                        // 保存真实用户信息
                        currentUser = res.getData();
                        // 填充UI（空值兜底）
                        tvNickname.setText(currentUser.getNickname() != null ? currentUser.getNickname() : "默认昵称");
                        tvUserId.setText("ID：" + currentUser.getUserId());
                        etNickname.setText(currentUser.getNickname() != null ? currentUser.getNickname() : "");
                        etSignature.setText(currentUser.getSignature() != null ? currentUser.getSignature() : "");
                        etPhone.setText(currentUser.getPhoneNumber() != null ? currentUser.getPhoneNumber() + "" : "");
                        etApiKey.setText(currentUser.getApiKey() != null ? currentUser.getApiKey() : "");
                        etAiPrompt.setText(currentUser.getAiPrompt() != null ? currentUser.getAiPrompt() : "");

                        // 加载头像
                        if (currentUser.getAvatarUrl() != null && !currentUser.getAvatarUrl().isEmpty()) {
                            Glide.with(MeFragment.this).load(currentUser.getAvatarUrl()).into(ivAvatar);
                        }
                    } else {
                        // 核心修复3：字段名从getMsg()改为getMessage()
                        Toast.makeText(getContext(), "获取用户信息失败：" + res.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "获取用户信息失败：服务器响应异常", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<User>> call, Throwable t) {
                Toast.makeText(getContext(), "网络请求失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                // 降级：使用本地模拟数据
                initMockData(userId);
            }
        });
    }

    // 降级方案：模拟数据
    private void initMockData(String userId) {
        currentUser = new User();
        currentUser.setUserId(userId);
        currentUser.setNickname("默认昵称");
        currentUser.setAvatarUrl("");
        currentUser.setSignature("这是模拟的个性签名");
        currentUser.setPhoneNumber(13800138000L);
        currentUser.setApiKey("");
        currentUser.setAiModel("");
        currentUser.setAiPrompt("");

        // 填充UI
        tvNickname.setText(currentUser.getNickname());
        tvUserId.setText("ID：" + currentUser.getUserId());
        etNickname.setText(currentUser.getNickname());
        etSignature.setText(currentUser.getSignature());
        etPhone.setText(currentUser.getPhoneNumber() + "");
        etApiKey.setText(currentUser.getApiKey());
        etAiPrompt.setText(currentUser.getAiPrompt());
    }

    private void initListener() {
        // 头像点击
        ivAvatar.setOnClickListener(v -> {
            if (currentUser == null || getContext() == null) return;
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("选择头像")
                    .setItems(new String[]{"从相册选择", "拍照"}, (dialog, which) -> {
                        Intent intent = new Intent();
                        if (which == 0) {
                            intent.setAction(Intent.ACTION_PICK);
                            intent.setType("image/*");
                        } else {
                            intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                        }
                        startActivityForResult(intent, REQUEST_CODE_AVATAR);
                    }).show();
        });

        // 编辑个人信息
        tvEditInfo.setOnClickListener(v -> {
            if (llInfoEditor.getVisibility() == View.GONE) {
                llInfoEditor.setVisibility(View.VISIBLE);
                tvEditInfo.setText("取消");
            } else {
                llInfoEditor.setVisibility(View.GONE);
                tvEditInfo.setText("编辑");
            }
        });

        // 保存个人信息
        btnSaveInfo.setOnClickListener(v -> {
            if (currentUser == null || getContext() == null) return;
            String newNickname = etNickname.getText().toString().trim();
            String newSignature = etSignature.getText().toString().trim();
            String newPhone = etPhone.getText().toString().trim();

            // 校验参数
            if (newNickname.isEmpty()) {
                Toast.makeText(getContext(), "昵称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            // 更新用户信息
            currentUser.setNickname(newNickname);
            currentUser.setSignature(newSignature);
            try {
                currentUser.setPhoneNumber(newPhone.isEmpty() ? null : Long.parseLong(newPhone));
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "手机号格式错误", Toast.LENGTH_SHORT).show();
                return;
            }

            // 调用后端修改个人信息接口（核心修复4：泛型BaseResponse<Void>）
            apiService.updateUserInfo("Bearer " + token, currentUser).enqueue(new Callback<BaseResponse<Void>>() {
                @Override
                public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        BaseResponse<Void> res = response.body();
                        if (res.getCode() == 200) {
                            // 更新UI
                            tvNickname.setText(currentUser.getNickname());
                            llInfoEditor.setVisibility(View.GONE);
                            tvEditInfo.setText("编辑");
                            Toast.makeText(getContext(), "个人信息更新成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "更新失败：" + res.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "更新失败：服务器响应异常", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
                    Toast.makeText(getContext(), "网络请求失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        // API Key输入提示
        etApiKey.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && getContext() != null) {
                String apiKey = etApiKey.getText().toString().trim();
                if (!apiKey.isEmpty()) {
                    Toast.makeText(getContext(), "后端未实现，暂不查询AI模型", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 保存AI配置
        btnSaveAiConfig.setOnClickListener(v -> {
            if (currentUser == null || getContext() == null) return;
            String apiKey = etApiKey.getText().toString().trim();
            String aiModel = (String) spAiModel.getSelectedItem();
            String aiPrompt = etAiPrompt.getText().toString().trim();

            currentUser.setApiKey(apiKey);
            currentUser.setAiModel(aiModel);
            currentUser.setAiPrompt(aiPrompt);
            Toast.makeText(getContext(), "AI配置已本地保存（后端未实现）", Toast.LENGTH_SHORT).show();
        });

        // 修改密码
        tvChangePwd.setOnClickListener(v -> {
            if (getContext() == null) return;
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

                        // 校验参数
                        if (oldPwd.isEmpty() || newPwd.isEmpty() || confirmPwd.isEmpty()) {
                            Toast.makeText(getContext(), "密码不能为空", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (!newPwd.equals(confirmPwd)) {
                            Toast.makeText(getContext(), "两次密码不一致", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // 构建请求体
                        ChangePwdRequest request = new ChangePwdRequest(oldPwd, newPwd);
                        // 调用后端修改密码接口（核心修复5：ChangePwdResponse改为BaseResponse<Void>）
                        apiService.changePassword("Bearer " + token, request).enqueue(new Callback<BaseResponse<Void>>() {
                            @Override
                            public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    BaseResponse<Void> res = response.body();
                                    if (res.getCode() == 200) {
                                        Toast.makeText(getContext(), "密码修改成功，请重新登录", Toast.LENGTH_SHORT).show();
                                        // 退出登录
                                        SharedPreferences sp = getContext().getSharedPreferences("USER_INFO", 0);
                                        sp.edit().clear().apply();
                                        jumpToLogin();
                                    } else {
                                        Toast.makeText(getContext(), "修改失败：" + res.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(getContext(), "修改失败：服务器响应异常", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
                                Toast.makeText(getContext(), "网络请求失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        // 退出登录
        tvLogout.setOnClickListener(v -> {
            if (getContext() == null) return;
            new AlertDialog.Builder(getContext())
                    .setTitle("确认退出")
                    .setMessage("是否退出当前账号？")
                    .setPositiveButton("退出", (dialog, which) -> {
                        // 调用后端退出登录接口（核心修复6：泛型BaseResponse<Void>）
                        apiService.logout("Bearer " + token).enqueue(new Callback<BaseResponse<Void>>() {
                            @Override
                            public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
                                // 无论后端是否成功，都清理本地缓存
                                SharedPreferences sp = getContext().getSharedPreferences("USER_INFO", 0);
                                sp.edit().clear().apply();
                                jumpToLogin();
                                Toast.makeText(getContext(), "已退出登录", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
                                // 网络失败，仍清理本地缓存
                                SharedPreferences sp = getContext().getSharedPreferences("USER_INFO", 0);
                                sp.edit().clear().apply();
                                jumpToLogin();
                                Toast.makeText(getContext(), "已退出登录（本地）", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    // 跳转登录页
    private void jumpToLogin() {
        if (getActivity() == null || getActivity().isFinishing()) return;
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        getActivity().finish();
    }

    // 处理头像选择返回
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_AVATAR && resultCode == getActivity().RESULT_OK && data != null && getContext() != null) {
            Uri uri = data.getData();
            if (uri != null) {
                ivAvatar.setImageURI(uri);
                Toast.makeText(getContext(), "头像已本地更换（后端未实现上传）", Toast.LENGTH_SHORT).show();
            }
        }
    }
}