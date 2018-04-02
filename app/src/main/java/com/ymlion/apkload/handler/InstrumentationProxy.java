package com.ymlion.apkload.handler;

import android.app.Activity;
import android.app.Application;
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
        if (activity.getPackageName().endsWith("pluginuninstalled")) {
            try {
                Context context = activity.getBaseContext();
                AppPlugin appPlugin =
                        PluginManager.getInstance().getCachePlugin(context.getPackageName());
                Resources resources = appPlugin.getResources();
                // android21 no effect
                HookUtil.setFieldWithoutException(context.getClass(), "mResources", context,
                        resources);
                /*HookUtil.setFieldWithoutException(ContextThemeWrapper.class, "mResources", activity,
                        resources);
                try {
                    Method selectDefaultTheme = Resources.class.getDeclaredMethod("selectDefaultTheme", int.class, int.class);
                    int theme = (int) selectDefaultTheme.invoke(null, 0, Build.VERSION.SDK_INT);
                    Resources.Theme pluginTheme = resources.newTheme();
                    pluginTheme.applyStyle(theme, false);
                    HookUtil.setField(ContextThemeWrapper.class, "mTheme", activity, pluginTheme);
                } catch (Exception e) {
                    e.printStackTrace();
                }*/
                if (Build.VERSION.SDK_INT <= 19) {
                    HookUtil.setFieldWithoutException(ContextThemeWrapper.class, "mBase", activity,
                            appPlugin.getPluginContext());
                }
                // TODO: 2018/3/13 After set the mBase, should override the getPackageName in plugin activity
                HookUtil.setField(ContextWrapper.class, "mBase", activity,
                        appPlugin.getPluginContext());
                Object loadedapk = HookUtil.getField(Application.class, "mLoadedApk",
                        activity.getApplication());
                ClassLoader appLoader =
                        (ClassLoader) HookUtil.getField(loadedapk.getClass(), "mClassLoader",
                                loadedapk);
                Log.d(TAG, "the activity's application is "
                        + activity.getApplication()
                        + ", class loader is "
                        + appLoader);

                //HookUtil.setField(Activity.class, "mApplication", activity,
                //        appPlugin.getApplication());

                String name = activity.getClass().getName();
                for (ActivityInfo info : appPlugin.mActivityInfos) {
                    if (name.equals(info.name)) {
                        HookUtil.setField(Activity.class, "mActivityInfo", activity, info);
                        break;
                    }
                }

                Class appcompatClazz =
                        activity.getClassLoader().loadClass(AppCompatActivity.class.getName());
                if (activity.getClass().getSuperclass() == appcompatClazz) {
                    Log.e(TAG, "yes");
                    Method getDelegate = appcompatClazz.getDeclaredMethod("getDelegate");
                    Object delegate = getDelegate.invoke(activity);
                    Class delegatBase = activity.getClassLoader()
                            .loadClass("android.support.v7.app.AppCompatDelegateImplBase");
                    Context context1 =
                            (Context) HookUtil.getField(delegatBase, "mContext", delegate);
                    Log.e(TAG, "activity == mContext is " + (context1 == activity));
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
