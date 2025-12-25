package com.example.myfirstapplication.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myfirstapplication.model.Friend;

import java.util.List;

@Dao
public interface FriendDao {
    // 插入好友
    @Insert
    void insert(Friend friend);

    // 修复：实现update方法（Room会自动生成）
    @Update
    void update(Friend friend);

    // 查询当前用户所有好友
    @Query("SELECT * FROM friends WHERE myId = :myId")
    LiveData<List<Friend>> getAllFriends(String myId);

    // 修复：补充业务代码中调用的getMyFriends方法（和getAllFriends逻辑一致）
    @Query("SELECT * FROM friends WHERE myId = :myId")
    LiveData<List<Friend>> getMyFriends(String myId);

    // 根据ID查询好友
    @Query("SELECT * FROM friends WHERE myId = :myId AND friendId = :friendId LIMIT 1")
    Friend getFriendById(String myId, String friendId);

    // 删除好友
    @Query("DELETE FROM friends WHERE myId = :myId AND friendId = :friendId")
    void deleteFriend(String myId, String friendId);
}