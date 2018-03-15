package com.ymlion.apkload.handler;

import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;
import com.ymlion.apkload.model.ProxyService;
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
        if ("startActivity".equals(method.getName())) {// activity
            Intent intent = null;
            for (Object arg : args) {
                if (arg instanceof Intent) {
                    intent = (Intent) arg;
                    break;
                }
            }
            if (intent != null) {
                ComponentName component = intent.getComponent();
                if (component != null) {
                    String targetClass = component.getClassName();
                    if (targetClass.endsWith("New1Activity")) {
                        ComponentName newComponent = new ComponentName(component.getPackageName(),
                                "com.ymlion.apkload.StubActivity");
                        intent.setComponent(newComponent).putExtra("targetClass", targetClass).putExtra("targetPackage", component.getPackageName());
                    } else if (!targetClass.startsWith("com.ymlion.apkload")) {
                        ComponentName newComponent = new ComponentName("com.ymlion.apkload",
                                "com.ymlion.apkload.StubActivity");
                        Log.d(TAG, "start target package is " + component.getPackageName());
                        // TODO: 2018/3/13  target package is com.ymlion.pluginuninstalled, but component.getPackageName() is com.ymlion.apkload in plugin startActivity
                        intent.setComponent(newComponent)
                                .putExtra("targetClass", targetClass)
                                .putExtra("targetPackage", component.getPackageName());
                    }
                }
            }
        } else if ("startService".equals(method.getName())) {// service
            Intent intent = null;
            for (Object arg : args) {
                if (arg instanceof Intent) {
                    intent = (Intent) arg;
                    break;
                }
            }
            if (intent != null) {
                ComponentName cn = intent.getComponent();
                if (cn != null) {
                    String targetClass = cn.getClassName();
                    String targetPkg = cn.getPackageName();
                    if (!targetClass.startsWith("com.ymlion.apkload")) {
                        intent.setComponent(new ComponentName("com.ymlion.apkload",
                                ProxyService.class.getName()));
                        intent.putExtra("targetClass", targetClass);
                        intent.putExtra("targetPackage", targetPkg);
                        intent.putExtra(ProxyService.COMMAND, ProxyService.COMMAND_START);
                    }
                }
            }
        }
        return method.invoke(base, args);
    }
}
