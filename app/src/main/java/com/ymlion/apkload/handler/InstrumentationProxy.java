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
import android.view.LayoutInflater;
import android.view.View;
import com.ymlion.apkload.base.AppPlugin;
import com.ymlion.apkload.base.PluginManager;
import com.ymlion.apkload.util.HookUtil;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Instrumentation代理类，目前只区判断是否是插桩activity
 * Created by YMlion on 2018/2/23.
 */

public class InstrumentationProxy extends Instrumentation {
    private static final String TAG = "InstrumentationProxy";
    private static int pluginActivities = 0;
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
            pluginActivities++;
        }
        if (Build.VERSION.SDK_INT <= 23) {
            try {// 清除掉LayoutInflater.sConstructorMap中的第三方数据，来自sdk26源码
                HashMap<String, Constructor<? extends View>> sConstructorMap =
                        (HashMap<String, Constructor<? extends View>>) HookUtil.getField(
                                LayoutInflater.class, "sConstructorMap");
                if (!sConstructorMap.isEmpty()) {
                    Set<Map.Entry<String, Constructor<? extends View>>> entrySet =
                            sConstructorMap.entrySet();
                    Iterator<Map.Entry<String, Constructor<? extends View>>> iterator =
                            entrySet.iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<String, Constructor<? extends View>> entry = iterator.next();
                        if (!verifyClassLoader(entry.getValue(), activity)) {
                            iterator.remove();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        proxy.callActivityOnCreate(activity, icicle);
    }

    private static final ClassLoader BOOT_CLASS_LOADER = LayoutInflater.class.getClassLoader();

    private final boolean verifyClassLoader(Constructor<? extends View> constructor,
            Context context) {
        final ClassLoader constructorLoader = constructor.getDeclaringClass().getClassLoader();
        if (constructorLoader == BOOT_CLASS_LOADER) {
            // fast path for boot class loader (most common case?) - always ok
            return true;
        }
        // in all normal cases (no dynamic code loading), we will exit the following loop on the
        // first iteration (i.e. when the declaring classloader is the contexts class loader).
        ClassLoader cl = context.getClassLoader();
        do {
            if (constructorLoader == cl) {
                return true;
            }
            cl = cl.getParent();
        } while (cl != null);
        return false;
    }

    @Override public Activity newActivity(ClassLoader cl, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Log.d(TAG, "newActivity: " + className + "; classLoader : " + cl.getClass().getName());
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

    @Override public void callActivityOnDestroy(Activity activity) {
        pluginActivities--;
        proxy.callActivityOnDestroy(activity);
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
