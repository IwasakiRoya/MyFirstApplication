package com.example.myfirstapplication.model;

import com.example.myfirstapplication.model.response.GroupResponse;

import java.io.Serializable;

/**
 * 统一搜索结果封装类（支持用户/群组）
 */
public class SearchResult implements Serializable {
    private static final long serialVersionUID = 1L;

    // 结果类型：0-用户，1-群组
    public static final int TYPE_USER = 0;
    public static final int TYPE_GROUP = 1;

    private int resultType; // 结果类型
    private User user; // 用户数据（类型为0时有效）
    private GroupResponse group; // 群组数据（类型为1时有效）

    // 空构造函数
    public SearchResult() {}

    // 用户结果构造方法
    public SearchResult(User user) {
        this.resultType = TYPE_USER;
        this.user = user;
    }

    // 群组结果构造方法
    public SearchResult(GroupResponse group) {
        this.resultType = TYPE_GROUP;
        this.group = group;
    }

    // Getter & Setter
    public int getResultType() {
        return resultType;
    }

    public void setResultType(int resultType) {
        this.resultType = resultType;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public GroupResponse getGroup() {
        return group;
    }

    public void setGroup(GroupResponse group) {
        this.group = group;
    }
}