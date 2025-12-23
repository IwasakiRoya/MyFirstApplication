package com.example.myfirstapplication.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.myfirstapplication.model.request.FriendRequest;
import java.util.List;

@Dao
public interface FriendRequestDao {
    // 插入请求
    @Insert
    long insert(FriendRequest request);

    // 更新请求状态（通过/拒绝）
    @Update
    void update(FriendRequest request);

    // 查询当前用户的待处理请求（toUserId=我的ID + 状态=待处理）
    @Query("SELECT * FROM friend_requests WHERE toUserId = :myId AND status = 0 ORDER BY createTime DESC")
    LiveData<List<FriendRequest>> getPendingRequests(String myId);

    // 查询所有请求（用于历史记录）
    @Query("SELECT * FROM friend_requests WHERE toUserId = :myId OR fromUserId = :myId ORDER BY createTime DESC")
    List<FriendRequest> getAllRequests(String myId);

    // 根据请求ID查询
    @Query("SELECT * FROM friend_requests WHERE requestId = :requestId")
    FriendRequest getRequestById(long requestId);
}