package com.example.myfirstapplication.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myfirstapplication.model.GroupUser;

import java.util.List;

@Dao
public interface GroupUserDao {
    // 插入或更新群成员关系
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(GroupUser groupUser);

    // 批量插入/更新群成员
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateBatch(List<GroupUser> groupUsers);

    // 插入群成员
    @Insert
    void insert(GroupUser groupUser);

    // 更新群成员
    @Update
    void update(GroupUser groupUser);

    // 查询当前用户加入的所有群成员关系（未退出）
    @Query("SELECT * FROM t_group_user WHERE userId = :myUserId AND isQuit = 0")
    LiveData<List<GroupUser>> getMyGroupRelations(String myUserId);

    // 根据群组ID和用户ID查询群成员关系
    @Query("SELECT * FROM t_group_user WHERE groupId = :groupId AND userId = :userId AND isQuit = 0 LIMIT 1")
    GroupUser getGroupUserByGroupIdAndUserId(String groupId, String userId);

    // 退出群组（逻辑删除，更新 isQuit 字段）
    @Query("UPDATE t_group_user SET isQuit = 1 WHERE groupId = :groupId AND userId = :userId")
    void quitGroup(String groupId, String userId);
}