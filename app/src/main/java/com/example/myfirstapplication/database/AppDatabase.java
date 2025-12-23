package com.example.myfirstapplication.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.myfirstapplication.model.ChatMessage;
import com.example.myfirstapplication.model.ChatReadPosition;
import com.example.myfirstapplication.model.Friend;
import com.example.myfirstapplication.model.User;
import com.example.myfirstapplication.model.request.FriendRequest;

/**
 * 修复后的数据表：包含所有实体类，版本号升级到4，移除主线程操作
 */
@Database(
        entities = {
                ChatMessage.class,    // 聊天消息
                ChatReadPosition.class, // 阅读位置
                Friend.class,         // 好友
                FriendRequest.class,  // 好友请求
                User.class            // 用户信息
        },
        version = 4,  // 版本号升级（解决版本冲突）
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    // 单例实例（volatile保证多线程可见性）
    private static volatile AppDatabase INSTANCE;

    // 所有DAO抽象方法（确保每个DAO都有对应实现）
    public abstract ChatDao chatDao();
    public abstract FriendDao friendDao();
    public abstract FriendRequestDao friendRequestDao();
    public abstract UserDao userDao();
    public abstract ChatReadPositionDao chatReadPositionDao();

    // 单例获取方法（修复同步逻辑，移除主线程操作）
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "wechat_db" // 数据库名统一
                            )
                            // 开发阶段允许破坏性迁移（正式环境需写Migration）
                            .fallbackToDestructiveMigration()
                            // 移除allowMainThreadQueries，强制异步操作
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}