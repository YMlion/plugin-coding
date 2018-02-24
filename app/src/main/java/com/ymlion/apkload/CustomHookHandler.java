package com.ymlion.apkload;

import android.util.Log;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by YMlion on 2018/2/24.
 */

public class CustomHookHandler implements InvocationHandler {
    private static final String TAG = "CustomHookHandler";
    private Object base;

    public CustomHookHandler(Object base) {
        this.base = base;
    }

    @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Log.d(TAG, "Hi, you are hooked!!!");
        Log.d(TAG, "invoke : " + base.getClass().getName() + "." + method.getName() + "()");
        return method.invoke(base, args);
    }
}
