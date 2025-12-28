package com.example.myfirstapplication.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myfirstapplication.model.Friend;

import java.util.List;

@Dao
public interface FriendDao {
    // 核心修复：添加 insertOrUpdate 方法（冲突时替换，实现插入/更新）
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(Friend friend);

    // 批量插入/更新（可选，优化批量同步效率）
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateBatch(List<Friend> friends);

    // 原有插入方法（保留，按需使用）
    @Insert
    void insert(Friend friend);

    // 原有更新方法（保留）
    @Update
    void update(Friend friend);

    // 查询当前用户所有好友
    @Query("SELECT * FROM friends WHERE myId = :myId")
    LiveData<List<Friend>> getAllFriends(String myId);

    // 业务代码调用的 getMyFriends 方法
    @Query("SELECT * FROM friends WHERE myId = :myId")
    LiveData<List<Friend>> getMyFriends(String myId);

    // 根据ID查询好友
    @Query("SELECT * FROM friends WHERE myId = :myId AND friendId = :friendId LIMIT 1")
    Friend getFriendById(String myId, String friendId);

    // 删除好友
    @Query("DELETE FROM friends WHERE myId = :myId AND friendId = :friendId")
    void deleteFriend(String myId, String friendId);
}