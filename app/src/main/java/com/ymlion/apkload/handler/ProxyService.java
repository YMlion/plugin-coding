package com.ymlion.apkload.handler;

import android.app.Application;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.IInterface;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import com.ymlion.apkload.AppContext;
import com.ymlion.apkload.base.AppPlugin;
import com.ymlion.apkload.base.PluginManager;
import com.ymlion.apkload.util.HookUtil;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ProxyService extends Service {
    private static final String TAG = "ProxyService";
    public static final String COMMAND = "command";
    public static final int COMMAND_START = 1;
    public static final int COMMAND_STOP = 2;
    public static Map<String, Service> cacheServices = new HashMap<>();

    public ProxyService() {
    }

    @Nullable @Override public IBinder onBind(Intent intent) {
        return null;
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return super.onStartCommand(intent, flags, startId);
        }
        String targetClass = intent.getStringExtra("targetClass");
        String targetPkg = intent.getStringExtra("targetPackage");

        if (TextUtils.isEmpty(targetClass)) {
            return super.onStartCommand(intent, flags, startId);
        }
        Log.d(TAG, "Proxy Service onStartCommand: " + targetClass);
        AppPlugin appPlugin = PluginManager.getInstance().getCachePlugin(targetPkg);
        switch (intent.getIntExtra(COMMAND, 1)) {
            case COMMAND_START:
                try {
                    Service service = cacheServices.get(targetClass);
                    if (service == null) {
                        service = (Service) appPlugin.getClassLoader()
                                .loadClass(targetClass)
                                .newInstance();
                        Class<?> at = Class.forName("android.app.ActivityThread");
                        Method attach = Service.class.getDeclaredMethod("attach", Context.class, at,
                                String.class, IBinder.class, Application.class, Object.class);

                        Object activityThread = HookUtil.getField(at, "sCurrentActivityThread");
                        IInterface appThread =
                                (IInterface) HookUtil.getField(at, "mAppThread", activityThread);
                        attach.invoke(service, PluginManager.getInstance().getBase(),
                                activityThread, targetClass,
                                appThread.asBinder(), AppContext.getInstance(), HookUtil.getAMS());
                        service.onCreate();
                        intent.setComponent(new ComponentName(targetPkg, targetClass));
                        intent.setExtrasClassLoader(appPlugin.getClassLoader());
                        cacheServices.put(targetClass, service);
                    }
                    service.onStartCommand(intent, flags, startId);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            case COMMAND_STOP:
                break;
            default:
                break;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "proxy service destroy");
    }
}
