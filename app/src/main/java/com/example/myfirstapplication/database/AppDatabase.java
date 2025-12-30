package com.example.myfirstapplication.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.myfirstapplication.model.ChatMessage;
import com.example.myfirstapplication.model.ChatReadPosition;
import com.example.myfirstapplication.model.Friend;
import com.example.myfirstapplication.model.FriendRequestEntity;
import com.example.myfirstapplication.model.Group; // 新增
import com.example.myfirstapplication.model.GroupUser; // 新增
import com.example.myfirstapplication.model.User;

/**
 * 数据库主类（适配所有新Model）
 */
@TypeConverters(DateTypeConverter.class)
@Database(
        entities = {
                ChatMessage.class,
                ChatReadPosition.class,
                Friend.class,
                FriendRequestEntity.class,
                User.class,
                Group.class, // 新增群组实体
                GroupUser.class // 新增群成员实体
        },
        version = 7, // 核心：版本号从 6 递增到 7
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract ChatDao chatDao();
    public abstract FriendDao friendDao();
    public abstract FriendRequestDao friendRequestDao();
    public abstract UserDao userDao();
    public abstract ChatReadPositionDao chatReadPositionDao();
    public abstract GroupDao groupDao(); // 新增群组Dao
    public abstract GroupUserDao groupUserDao(); // 新增群成员Dao

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "wechat_db"
                            )
                            .fallbackToDestructiveMigration() // 自动删除旧数据库重建（开发环境）
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}