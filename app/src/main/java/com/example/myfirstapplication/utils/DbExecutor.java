package com.example.myfirstapplication.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 全局数据库线程池
 * ⚠️ 生命周期 = Application
 * Retrofit 回调 / Room / Broadcast 都可以安全使用
 */
public final class DbExecutor {

    private static final ExecutorService DB_EXECUTOR =
            Executors.newSingleThreadExecutor();

    private DbExecutor() {
        // 禁止实例化
    }

    public static void execute(Runnable task) {
        DB_EXECUTOR.execute(task);
    }
}
