package com.example.myfirstapplication.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {

    /**
     * 将 Uri 转换为临时 File 文件
     * 适配 Android 10+ 沙盒机制
     */
    public static File getFileFromUri(Context context, Uri uri) {
        if (uri == null) return null;

        // 创建临时文件，存放在外部缓存目录
        String fileName = "upload_" + System.currentTimeMillis() + ".jpg";
        File tempFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName);

        try (InputStream is = context.getContentResolver().openInputStream(uri);
             OutputStream os = new FileOutputStream(tempFile)) {

            if (is == null) return null;

            byte[] buffer = new byte[4096];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();
            return tempFile;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}