package com.example.myfirstapplication.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.myfirstapplication.model.Friend;
import java.util.List;

@Dao
public interface FriendDao {
    // 添加好友（双向关系，需插入两条）
    @Insert
    void insert(Friend friend);

    // 查询我的好友列表
    @Query("SELECT * FROM friends WHERE myId = :myId")
    LiveData<List<Friend>> getMyFriends(String myId);

    // 检查是否已添加好友
    @Query("SELECT COUNT(*) FROM friends WHERE myId = :myId AND friendId = :friendId")
    int isFriend(String myId, String friendId);

    // 修改好友备注
    @Query("UPDATE friends SET friendNickname = :nickname WHERE myId = :myId AND friendId = :friendId")
    void updateNickname(String myId, String friendId, String nickname);
}