package com.ymlion.apkload;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.util.Log;

/**
 * Instrumentation代理类，目前只区判断是否是插桩activity
 * Created by YMlion on 2018/2/23.
 */

public class InstrumentationProxy extends Instrumentation {

    private Instrumentation proxy;

    public InstrumentationProxy(Instrumentation proxy) {
        this.proxy = proxy;
    }

    @Override public Activity newActivity(ClassLoader cl, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Log.e("InstrumentationProxy", "newActivity: " + className);
        String targetClass = intent.getStringExtra("targetClass");
        if (targetClass != null && targetClass.length() > 0) {
            className = targetClass;
        }
        return proxy.newActivity(cl, className, intent);
    }
}
