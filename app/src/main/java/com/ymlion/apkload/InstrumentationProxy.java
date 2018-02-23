package com.ymlion.apkload;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.util.Log;

/**
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
        return proxy.newActivity(cl, className, intent);
    }
}
