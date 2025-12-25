package com.example.myfirstapplication.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myfirstapplication.model.FriendRequestEntity;

import java.util.List;

@Dao
public interface FriendRequestDao {
    // 修复：确保参数类型是FriendRequestEntity，返回long类型
    @Insert
    long insert(FriendRequestEntity request);

    // 更新请求状态
    @Update
    void update(FriendRequestEntity request);

    // 查询待处理请求
    @Query("SELECT * FROM friend_requests WHERE toUserId = :myId AND status = 0 ORDER BY createTime DESC")
    LiveData<List<FriendRequestEntity>> getPendingRequests(String myId);

    // 查询所有请求
    @Query("SELECT * FROM friend_requests WHERE toUserId = :myId OR fromUserId = :myId ORDER BY createTime DESC")
    List<FriendRequestEntity> getAllRequests(String myId);

    // 根据ID查询请求
    @Query("SELECT * FROM friend_requests WHERE requestId = :requestId")
    FriendRequestEntity getRequestById(String requestId);
}