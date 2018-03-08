package com.ymlion.apkload.handler;

import android.content.ComponentName;
import android.content.pm.PackageInfo;
import android.util.Log;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

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
            if (target != null && (target.getClassName().endsWith("New1Activity")
                    || target.getClassName().endsWith("Plugin1Activity"))) {
                ComponentName old =
                        new ComponentName("com.ymlion.apkload", "com.ymlion.apkload.StubActivity");
                args[i] = old;
            }
        } else if ("getPackageInfo".equals(method.getName())) {
            for (Object arg : args) {
                if (arg instanceof String) {
                    if (((String) arg).endsWith("pluginuninstalled")) {
                        return new PackageInfo();
                    }
                }
            }
        }
        return method.invoke(base, args);
    }
}
