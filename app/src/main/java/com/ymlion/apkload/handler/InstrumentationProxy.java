package com.ymlion.apkload.handler;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextThemeWrapper;
import com.ymlion.apkload.base.AppPlugin;
import com.ymlion.apkload.base.PluginManager;
import com.ymlion.apkload.util.HookUtil;

/**
 * Instrumentation代理类，目前只区判断是否是插桩activity
 * Created by YMlion on 2018/2/23.
 */

public class InstrumentationProxy extends Instrumentation {
    private static final String TAG = "InstrumentationProxy";

    private Instrumentation proxy;

    public InstrumentationProxy(Instrumentation proxy) {
        this.proxy = proxy;
    }

    @Override public void callActivityOnCreate(Activity activity, Bundle icicle) {
        if (activity.getPackageName().endsWith("pluginuninstalled")) {
            try {
                Context context = activity.getBaseContext();
                AppPlugin appPlugin =
                        PluginManager.getInstance().getCachePlugin(context.getPackageName());
                Resources resources = appPlugin.getResources();
                // android21 no effect
                HookUtil.setFieldWithoutException(context.getClass(), "mResources", context,
                        resources);
                if (Build.VERSION.SDK_INT <= 19) {
                    HookUtil.setFieldWithoutException(ContextThemeWrapper.class, "mBase", activity,
                            appPlugin.getPluginContext());
                }
                // TODO: 2018/3/13 After set the mBase, should override the getPackageName in plugin activity
                HookUtil.setField(ContextWrapper.class, "mBase", activity,
                        appPlugin.getPluginContext());
                //HookUtil.setField(Activity.class, "mApplication", activity,
                //        appPlugin.getApplication());

                String name = activity.getClass().getName();
                for (ActivityInfo info : appPlugin.mActivityInfos) {
                    if (name.equals(info.name)) {
                        HookUtil.setField(Activity.class, "mActivityInfo", activity, info);
                        break;
                    }
                }
                Log.d(TAG, "callActivityOnCreate: theme is " + HookUtil.getField(
                        ContextThemeWrapper.class, "mTheme", activity));
                /*ActivityInfo ai =
                        (ActivityInfo) HookUtil.getField(Activity.class, "mActivityInfo", activity);
                Object loadApk = AppPlugin.apkCache.get(context.getPackageName());
                ApplicationInfo targetAi =
                        (ApplicationInfo) HookUtil.getField(loadApk.getClass(), "mApplicationInfo",
                                loadApk);
                ai.applicationInfo = targetAi;*/
                ClassLoader c1 = AppCompatActivity.class.getClassLoader();
                Log.d(TAG, "appcompat activity class loader is " + c1);
                Log.d(TAG, "activity father is " + activity.getClass().getSuperclass().getName());
                Log.d(TAG, "activity father loader is " + activity.getClass()
                        .getSuperclass()
                        .getClassLoader());
                ClassLoader c2 = activity.getClass().getSuperclass().getClassLoader();
                Log.d(TAG, "activity father loader's parent is " + c2.getParent());
                if (c2.getParent() != null) {
                    Log.d(TAG, "Appcompat is appcompat? " + (AppCompatActivity.class
                            == activity.getClass().getSuperclass()));
                    Log.d(TAG, "Appcompat is appcompat? " + (c2.loadClass(
                            "android.support.v7.app.AppCompatActivity") == activity.getClass()
                            .getSuperclass()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        proxy.callActivityOnCreate(activity, icicle);
    }

    @Override public Activity newActivity(ClassLoader cl, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Log.e("InstrumentationProxy",
                "newActivity: " + className + "; classLoader : " + cl.getClass().getName());
        Activity activity = null;
        ComponentName component = intent.getComponent();
        AppPlugin appPlugin = null;
        if (component != null) {
            appPlugin = PluginManager.getInstance().getCachePlugin(component.getPackageName());
        }
        try {
            activity = proxy.newActivity(cl, className, intent);
        } catch (Exception e) {
            e.printStackTrace();
            if (appPlugin != null) {
                activity = proxy.newActivity(appPlugin.getClassLoader(), className, intent);
                activity.setIntent(intent);
            }
        }
        if (appPlugin != null) {//android21 no effect
            HookUtil.setFieldWithoutException(ContextThemeWrapper.class, "mResources", activity,
                    appPlugin.getResources());
        }
        return activity;
    }

    @Override public Context getContext() {
        return proxy.getContext();
    }

    @Override public Context getTargetContext() {
        return proxy.getTargetContext();
    }

    @Override public ComponentName getComponentName() {
        return proxy.getComponentName();
    }
}
