package com.ymlion.apkload.handler;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import com.ymlion.apkload.model.AppPlugin;
import com.ymlion.apkload.util.HookUtil;

/**
 * Instrumentation代理类，目前只区判断是否是插桩activity
 * Created by YMlion on 2018/2/23.
 */

public class InstrumentationProxy extends Instrumentation {
    private static final String TAG = "InstrumentationProxy";

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
                AppPlugin appPlugin = AppPlugin.mPluginMap.get(context.getPackageName());
                Resources resources = appPlugin.getResources();
                HookUtil.setField(context.getClass(), "mResources", context, resources);
                // TODO: 2018/3/13 After set the mBase, should override the getPackageName in plugin activity
                HookUtil.setField(ContextWrapper.class, "mBase", activity,
                        appPlugin.getPluginContext());

                String name = activity.getClass().getName();
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

    @Override public Activity newActivity(ClassLoader cl, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Log.e("InstrumentationProxy", "newActivity: " + className);
        try {
            return proxy.newActivity(cl, className, intent);
        } catch (Exception e) {
            e.printStackTrace();
            String targetPkg = intent.getComponent().getPackageName();
            AppPlugin appPlugin = AppPlugin.mPluginMap.get(targetPkg);
            if (appPlugin != null) {
                Activity activity =
                        proxy.newActivity(appPlugin.getClassLoader(), className, intent);
                activity.setIntent(intent);
                return activity;
            }
            return null;
        }
    }
}
