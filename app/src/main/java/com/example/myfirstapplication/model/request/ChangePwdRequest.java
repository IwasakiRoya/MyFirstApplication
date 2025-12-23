// 修改密码请求（ChangePwdRequest.java）
package com.example.myfirstapplication.model.request;

import lombok.Data;

@Data
public class ChangePwdRequest {
    private String userId;      // 用户唯一ID
    private String oldPwd;      // 原密码
    private String newPwd;      // 新密码
}
