package com.example.myfirstapplication.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myfirstapplication.model.Group;

import java.util.List;

@Dao
public interface GroupDao {
    // 插入或更新群组（冲突时替换）
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(Group group);

    // 批量插入/更新群组
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateBatch(List<Group> groups);

    // 插入群组（单独方法，按需使用）
    @Insert
    void insert(Group group);

    // 更新群组
    @Update
    void update(Group group);

    // 查询当前用户加入的所有群组（关联 GroupUser 表）
    @Query("SELECT g.* FROM t_group g " +
            "INNER JOIN t_group_user gu ON g.groupId = gu.groupId " +
            "WHERE gu.userId = :myUserId AND gu.isQuit = 0 AND g.isDelete = 0")
    LiveData<List<Group>> getMyGroups(String myUserId);

    // 根据群组ID查询群组详情
    @Query("SELECT * FROM t_group WHERE groupId = :groupId AND isDelete = 0 LIMIT 1")
    Group getGroupById(String groupId);

    // 根据群组ID删除（逻辑删除，更新 isDelete 字段）
    @Query("UPDATE t_group SET isDelete = 1 WHERE groupId = :groupId")
    void deleteGroup(String groupId);
}