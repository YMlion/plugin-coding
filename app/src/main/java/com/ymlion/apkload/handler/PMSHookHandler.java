package com.ymlion.apkload.handler;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.util.Log;
import com.ymlion.apkload.base.AppPlugin;
import com.ymlion.apkload.base.PluginManager;
import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by YMlion on 2018/2/24.
 */

public class PMSHookHandler implements InvocationHandler {
    private static final String TAG = "PMSHookHandler";
    private Object base;

    public PMSHookHandler(Object base) {
        this.base = base;
    }

    @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Log.d(TAG, "hook : " + base.getClass().getName() + "." + method.getName() + "()");
        if ("getActivityInfo".equals(method.getName())) {
            ComponentName target = null;
            int i = 0;
            for (; i < args.length; i++) {
                if (args[i] instanceof ComponentName) {
                    target = (ComponentName) args[i];
                    break;
                }
            }
            if (target != null) {
                if (target.getClassName().endsWith("New1Activity")) {
                    ComponentName old = new ComponentName("com.ymlion.apkload",
                            "com.ymlion.apkload.StubActivity");
                    args[i] = old;
                } else if (target.getPackageName().endsWith("pluginuninstalled")) {
                    AppPlugin appPlugin =
                            PluginManager.getInstance().getCachePlugin(target.getPackageName());
                    if (appPlugin != null) {
                        for (ActivityInfo activityInfo : appPlugin.mActivityInfos) {
                            if (target.getClassName().equals(activityInfo.name)) {
                                Log.d(TAG, "get the activity info " + activityInfo.name);
                                return activityInfo;
                            }
                        }
                    }
                    ComponentName old = new ComponentName("com.ymlion.apkload",
                            "com.ymlion.apkload.StubActivity");
                    args[i] = old;
                }
            }
        } else if ("getPackageInfo".equals(method.getName())) {
            for (Object arg : args) {
                if (arg instanceof String) {
                    if (((String) arg).endsWith("pluginuninstalled")) {
                        Log.d(TAG, "getPackageInfo: " + arg);
                        return getPackageInfo((String) arg);
                    }
                }
            }
        }
        return method.invoke(base, args);
    }

    private Object getPackageInfo(String packageName) {
        AppPlugin plugin = PluginManager.getInstance().getCachePlugin(packageName);
        if (plugin == null) {
            return new PackageInfo();
        }

        try {
            Class<?> ppClazz = Class.forName("android.content.pm.PackageParser");
            Class<?> pusClazz = Class.forName("android.content.pm.PackageUserState");
            //public static PackageInfo generatePackageInfo(PackageParser.Package p,
            //int gids[], int flags, long firstInstallTime, long lastUpdateTime,
            //Set<String> grantedPermissions, PackageUserState state)
            Class<?> setClass = Set.class;
            if (Build.VERSION.SDK_INT == 22) { // android5.1，ArraySet is hide
                setClass = Class.forName("android.util.ArraySet");
            } else if (Build.VERSION.SDK_INT <= 21) {// below android5.0 use HashSet
                setClass = HashSet.class;
            }
            Method generatePackageInfo =
                    ppClazz.getDeclaredMethod("generatePackageInfo", plugin.getPackage().getClass(),
                            int[].class, int.class, long.class, long.class, setClass, pusClazz);
            generatePackageInfo.setAccessible(true);
            Context host = PluginManager.getInstance().getBase();
            PackageInfo hostPi = host.getPackageManager().getPackageInfo(host.getPackageName(), 0);
            File file = new File(plugin.getApplicationInfo().sourceDir);
            Object pus = pusClazz.newInstance();
            PackageInfo result =
                    (PackageInfo) generatePackageInfo.invoke(null, plugin.getPackage(), hostPi.gids,
                            0, file.lastModified(), file.lastModified(), null, pus);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new PackageInfo();
    }
}
