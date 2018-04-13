package com.ymlion.apkload.handler;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;
import com.ymlion.apkload.AppContext;
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
        Log.d(TAG, "invoke AMS method : " + method.getName());
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
                                "com.ymlion.apkload.S1StubActivity");
                        intent.setComponent(newComponent)
                                .putExtra("targetClass", targetClass)
                                .putExtra("targetPackage", component.getPackageName());
                    } else if (!targetClass.startsWith("com.ymlion.apkload")) {
                        int flags = intent.getFlags();
                        String stub = "com.ymlion.apkload.S1StubActivity";
                        if ((flags & Intent.FLAG_ACTIVITY_NEW_TASK) != 0 && targetClass.endsWith(
                                "MainActivity")) {// 是否是启动main activity
                            stub = "com.ymlion.apkload.StubActivity";
                        }
                        ComponentName newComponent = new ComponentName("com.ymlion.apkload", stub);
                        Log.d(TAG, "start target package is " + component.getPackageName());
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
        } else if ("stopServiceToken".equals(method.getName())) {// stop service
            ComponentName cn = null;
            for (Object arg : args) {
                if (arg instanceof ComponentName) {
                    cn = (ComponentName) arg;
                    break;
                }
            }
            if (cn != null && !cn.getClassName().startsWith("com.ymlion.apkload")) {
                Service service = ProxyService.cacheServices.remove(cn.getClassName());
                if (service != null) {
                    service.onDestroy();
                }
                if (ProxyService.cacheServices.size() <= 0) {
                    AppContext.getInstance()
                            .stopService(new Intent(AppContext.getInstance(), ProxyService.class));
                }
            }
        } else if ("stopService".equals(method.getName())) {// stop service
            Intent intent = null;
            for (Object arg : args) {
                if (arg instanceof Intent) {
                    intent = (Intent) arg;
                    break;
                }
            }
            if (intent != null) {
                ComponentName cn = intent.getComponent();
                if (cn != null && !cn.getClassName().startsWith("com.ymlion.apkload")) {
                    Service service = ProxyService.cacheServices.remove(cn.getClassName());
                    if (service != null) {
                        service.onDestroy();
                    }
                    if (ProxyService.cacheServices.size() <= 0) {
                        AppContext.getInstance()
                                .stopService(
                                        new Intent(AppContext.getInstance(), ProxyService.class));
                    }
                }
            }
        }
        return method.invoke(base, args);
    }
}
