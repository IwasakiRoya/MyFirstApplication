package com.example.myfirstapplication.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.myfirstapplication.DAO.ChatDao;
import com.example.myfirstapplication.model.ChatMessage;

// entities 声明数据库包含哪些表，version 每次改表结构都要升版本
@Database(entities = {ChatMessage.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    // 暴露 DAO
    public abstract ChatDao chatDao();

    // 单例模式：确保全 App 只有一个数据库连接池
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "wechat_db" // 数据库文件名
                    )
                    .allowMainThreadQueries() // 注意：为了演示方便先允许主线程查询，实际开发建议切异步
                    .build();
        }
        return instance;
    }


}