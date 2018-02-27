package com.ymlion.apkload.handler;

import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * hook AMS，在启动activity时判断是否是插件中的activity
 *
 * Created by YMlion on 2018/2/24.
 */

public class AMSHookHandler implements InvocationHandler {

    private static final String TAG = "AMSHookHandler";
    private Object base;

    public AMSHookHandler(Object base) {
        this.base = base;
    }

    @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("startActivity".equals(method.getName())) {
            Log.e(TAG, "start activity");
            Intent intent = null;
            for (Object arg : args) {
                if (arg instanceof Intent) {
                    intent = (Intent) arg;
                    break;
                }
            }
            if (intent != null) {
                ComponentName component = intent.getComponent();
                if (component != null && component.getClassName().endsWith("New1Activity")) {
                    ComponentName newComponent =
                            new ComponentName(component.getPackageName(),
                                    "com.ymlion.apkload.StubActivity");
                    intent.setComponent(newComponent)
                            .putExtra("targetClass", component.getClassName());
                }
            }
        }
        return method.invoke(base, args);
    }
}
