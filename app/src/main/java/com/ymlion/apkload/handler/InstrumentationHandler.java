package com.ymlion.apkload.handler;

import android.app.Instrumentation;
import android.content.Intent;
import android.util.Log;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by YMlion on 2018/2/24.
 */

public class InstrumentationHandler implements InvocationHandler {
    private static final String TAG = "InstrumentationHandler";
    private Instrumentation base;
    public InstrumentationHandler(Instrumentation base) {
        this.base = base;
    }

    @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("newActivity".equals(method.getName())) {
            Log.d(TAG, "new activity");
            Intent intent = null;
            String className = "";
            int index = 1;
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof String) {
                    index = i;
                    className = (String) args[i];
                } else if (args[i] instanceof Intent) {
                    intent = (Intent) args[i];
                }
            }
            if (intent != null && className.endsWith("StubActivity")) {
                className = intent.getStringExtra("targetClass");
                if (className != null && className.length() > 0) {
                    args[index] = className;
                }
            }
        }
        return method.invoke(base, args);
    }
}
