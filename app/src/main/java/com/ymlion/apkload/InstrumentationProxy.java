package com.ymlion.apkload;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import dalvik.system.DexClassLoader;
import java.io.File;

/**
 * Instrumentation代理类，目前只区判断是否是插桩activity
 * Created by YMlion on 2018/2/23.
 */

public class InstrumentationProxy extends Instrumentation {

    private Instrumentation proxy;
    private Context oldContext;

    public InstrumentationProxy(Instrumentation proxy, Context context) {
        this.proxy = proxy;
        oldContext = context;
    }

    @Override public Activity newActivity(ClassLoader cl, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Log.e("InstrumentationProxy", "newActivity: " + className);
        String targetClass = intent.getStringExtra("targetClass");
        if (targetClass != null && targetClass.length() > 0) {
            className = targetClass;
        }
        if (!className.startsWith("com.ymlion.apkload")) {
            String dexDir = oldContext.getDir("dex", Context.MODE_PRIVATE).getAbsolutePath();
            String apkPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + File.separator
                    + "apkload_plugin.apk";
            cl = new DexClassLoader(apkPath, dexDir, null, ClassLoader.getSystemClassLoader());
        }
        return proxy.newActivity(cl, className, intent);
    }
}
