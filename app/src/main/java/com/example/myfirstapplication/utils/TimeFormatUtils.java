package com.example.myfirstapplication.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 时间格式化工具类，用于将时间戳转换为友好显示格式
 */
public class TimeFormatUtils {

    /**
     * 将时间戳转换为 "HH:mm" 格式（如 18:05）
     */
    public static String formatTimestampToHHmm(long timestamp) {
        if (timestamp <= 0) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.CHINA);
        return sdf.format(new Date(timestamp));
    }

    /**
     * 将时间戳转换为 "yyyy-MM-dd HH:mm" 格式（可选，用于跨天消息）
     */
    public static String formatTimestampToYMDHHmm(long timestamp) {
        if (timestamp <= 0) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
        return sdf.format(new Date(timestamp));
    }
}