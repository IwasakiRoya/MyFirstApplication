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
import com.example.myfirstapplication.model.User;

/**
 * 数据库主类（适配所有新Model）
 */
// 关键修复：添加 @TypeConverters 注解，注册日期转换器
@TypeConverters(DateTypeConverter.class)
@Database(
        entities = {
                ChatMessage.class,
                ChatReadPosition.class,
                Friend.class,
                FriendRequestEntity.class,
                User.class
        },
        version = 5, // 核心修复：版本号从 4 递增到 5（必须大于旧版本）
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract ChatDao chatDao();
    public abstract FriendDao friendDao();
    public abstract FriendRequestDao friendRequestDao();
    public abstract UserDao userDao();
    public abstract ChatReadPositionDao chatReadPositionDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "wechat_db"
                            )
                            .fallbackToDestructiveMigration() // 保留该配置，自动删除旧数据库重建
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}