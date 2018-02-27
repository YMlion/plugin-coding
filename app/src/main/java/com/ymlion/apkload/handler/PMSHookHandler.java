package com.ymlion.apkload.handler;

import android.content.ComponentName;
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
            if (target != null && target.getClassName().endsWith("New1Activity")) {
                ComponentName old = new ComponentName(target.getPackageName(),
                        target.getPackageName() + ".StubActivity");
                args[i] = old;
            }
        }
        return method.invoke(base, args);
    }
}
