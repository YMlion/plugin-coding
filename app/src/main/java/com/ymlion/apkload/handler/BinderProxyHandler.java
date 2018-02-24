package com.ymlion.apkload.handler;

import android.os.IBinder;
import android.os.IInterface;
import android.util.Log;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by YMlion on 2018/2/23.
 */

public class BinderProxyHandler implements InvocationHandler {

    private IBinder base;
    private Class<?> stub;
    private Class<?> iinterface;

    public BinderProxyHandler(IBinder obj) {
        base = obj;
        try {
            stub = Class.forName("android.content.IClipboard$Stub");
            iinterface = Class.forName("android.content.IClipboard");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("queryLocalInterface".equals(method.getName())) {
            return Proxy.newProxyInstance(proxy.getClass().getClassLoader(), new Class[] {
                    IBinder.class, IInterface.class, iinterface
            }, new BinderHandler(base, stub));
        }
        Log.d("BinderProxy", "invoke: " + method.getName());
        return method.invoke(base, args);
    }
}
