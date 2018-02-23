package com.ymlion.apkload;

import android.content.ClipData;
import android.os.IBinder;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by YMlion on 2018/2/23.
 */

public class BinderHandler implements InvocationHandler {

    private Object base;

    BinderHandler(IBinder iBinder, Class<?> subclass) {
        try {
            Method method = subclass.getDeclaredMethod("asInterface", IBinder.class);
            base = method.invoke(null, iBinder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("getPrimaryClip")) {
            return ClipData.newPlainText("hook text", "hello, hook world!");
        } else if (method.getName().equals("hasPrimaryClip")) {
            return true;
        }
        return method.invoke(base, args);
    }
}
