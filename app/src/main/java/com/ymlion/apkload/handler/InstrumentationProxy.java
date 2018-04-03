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
import android.util.Log;
import android.view.ContextThemeWrapper;
import com.ymlion.apkload.base.AppPlugin;
import com.ymlion.apkload.base.PluginManager;
import com.ymlion.apkload.util.HookUtil;
import java.lang.reflect.Method;

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
        // 如何在android6.0及以下正常启动AppcompatActivity，除了设置单独的进程，还可以修改LayoutInflate
        if (activity.getPackageName().endsWith("pluginuninstalled")) {
            try {
                Context context = activity.getBaseContext();
                AppPlugin appPlugin =
                        PluginManager.getInstance().getCachePlugin(context.getPackageName());
                Resources resources = appPlugin.getResources();
                // android21 no effect
                HookUtil.setFieldWithoutException(context.getClass(), "mResources", context,
                        resources);
                HookUtil.setFieldWithoutException(ContextThemeWrapper.class, "mResources", activity,
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

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        proxy.callActivityOnCreate(activity, icicle);
    }

    @Override public Activity newActivity(ClassLoader cl, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Log.d(TAG,
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
            try {
                Method selectDefaultTheme =
                        Resources.class.getDeclaredMethod("selectDefaultTheme", int.class,
                                int.class);
                int theme = (int) selectDefaultTheme.invoke(null, 0, Build.VERSION.SDK_INT);
                Resources.Theme pluginTheme = appPlugin.getResources().newTheme();
                pluginTheme.applyStyle(theme, false);
                HookUtil.setField(ContextThemeWrapper.class, "mTheme", activity, pluginTheme);
            } catch (Exception e) {
                e.printStackTrace();
            }
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
