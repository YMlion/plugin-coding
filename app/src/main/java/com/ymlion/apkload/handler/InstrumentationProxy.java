package com.ymlion.apkload.handler;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import com.ymlion.apkload.model.AppPlugin;
import com.ymlion.apkload.util.HookUtil;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

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

    @Override public void callActivityOnCreate(Activity activity, Bundle icicle) {
        if (activity.getPackageName().endsWith("pluginuninstalled")) {
            try {
                Context context = activity.getBaseContext();
                Log.e("TAG",
                        "callActivityOnCreate: " + context.getClassLoader().getClass().getName());
                String apkPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + File.separator
                        + "apkload_plugin.apk";
                Resources resources = getPluginResources(oldContext, apkPath);
                Field resourcesF = context.getClass().getDeclaredField("mResources");
                resourcesF.setAccessible(true);
                resourcesF.set(context, resources);

                String name = activity.getClass().getName();
                AppPlugin appPlugin = AppPlugin.mPluginMap.get(context.getPackageName());
                for (ActivityInfo info : appPlugin.mActivityInfos) {
                    if (name.equals(info.name)) {
                        HookUtil.setField(Activity.class, "mActivityInfo", activity, info);
                        break;
                    }
                }
                /*ActivityInfo ai =
                        (ActivityInfo) HookUtil.getField(Activity.class, "mActivityInfo", activity);
                Object loadApk = AppPlugin.apkCache.get(context.getPackageName());
                ApplicationInfo targetAi =
                        (ApplicationInfo) HookUtil.getField(loadApk.getClass(), "mApplicationInfo",
                                loadApk);
                ai.applicationInfo = targetAi;*/
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        proxy.callActivityOnCreate(activity, icicle);
    }

    private Resources getPluginResources(Context context, String apkPath) {
        try {
            AssetManager am = AssetManager.class.newInstance();
            Method addAsset = am.getClass().getDeclaredMethod("addAssetPath", String.class);
            addAsset.invoke(am, apkPath);
            Resources res = context.getResources();
            return new Resources(am, res.getDisplayMetrics(), res.getConfiguration());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override public Activity newActivity(ClassLoader cl, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Log.e("InstrumentationProxy", "newActivity: " + className);
        /*String targetClass = intent.getStringExtra("targetClass");
        if (targetClass != null && targetClass.length() > 0) {
            className = targetClass;
        }
        if (!className.startsWith("com.ymlion.apkload")) {
            String dexDir = oldContext.getDir("dex", Context.MODE_PRIVATE).getAbsolutePath();
            String apkPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + File.separator
                    + "apkload_plugin.apk";
            cl = new DexClassLoader(apkPath, dexDir, null, ClassLoader.getSystemClassLoader());
        }*/
        return proxy.newActivity(cl, className, intent);
    }
}
