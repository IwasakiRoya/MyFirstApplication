package com.example.myfirstapplication.database;

import androidx.room.TypeConverter;

import java.util.Date;

/**
 * Room 日期类型转换器：将 Date 转换为 Long（时间戳）存储
 */
public class DateTypeConverter {
    // Date → Long（存入数据库）
    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    // Long → Date（从数据库读取）
    @TypeConverter
    public static Date timestampToDate(Long timestamp) {
        return timestamp == null ? null : new Date(timestamp);
    }
}