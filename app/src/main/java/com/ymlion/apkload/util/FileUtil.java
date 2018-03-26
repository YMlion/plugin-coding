package com.ymlion.apkload.util;

import android.content.Context;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by YMlion on 2018/3/26.
 */

public class FileUtil {
    private static final String TAG = "FileUtil";

    public static File getPluginFile(Context context, String apkPath) {
        File filesDir = context.getFilesDir();
        String apkName = apkPath.substring(apkPath.lastIndexOf(File.separator));
        File plugin = new File(filesDir, apkName);
        if (!plugin.exists()) {
            Log.d(TAG, "copy apk file from sdcard to files dir.");
            copyFile(apkPath, plugin.getAbsolutePath());
        }
        return plugin;
    }

    private static void copyFile(String srcPath, String desPath) {
        BufferedInputStream bi = null;
        BufferedOutputStream bo = null;
        try {
            bi = new BufferedInputStream(new FileInputStream(srcPath));
            bo = new BufferedOutputStream(new FileOutputStream(desPath));

            while (true) {
                int l = bi.read();
                if (l == -1) {
                    break;
                }
                bo.write(l);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (bi != null) {
                    bi.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (bo != null) {
                    bo.flush();
                    bo.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
