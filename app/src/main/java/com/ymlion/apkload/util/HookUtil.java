package com.ymlion.apkload.util;

import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import com.ymlion.apkload.InstrumentationProxy;
import com.ymlion.apkload.handler.AMSHookHandler;
import com.ymlion.apkload.handler.ActivityThreadHandlerCallback;
import com.ymlion.apkload.handler.BinderProxyHandler;
import com.ymlion.apkload.handler.PMSHookHandler;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;

/**
 * hook utils
 *
 * Created by YMlion on 2018/2/23.
 */

public class HookUtil {

    /**
     * hook clipboard service
     */
    public static void hookClipboard() {
        try {
            Class<?> sm = Class.forName("android.os.ServiceManager");
            Method getService = sm.getDeclaredMethod("getService", String.class);
            IBinder clipboard = (IBinder) getService.invoke(null, "clipboard");

            IBinder hookedBinder = (IBinder) Proxy.newProxyInstance(sm.getClassLoader(),
                    new Class[] { IBinder.class }, new BinderProxyHandler(clipboard));

            Field sCache = sm.getDeclaredField("sCache");
            sCache.setAccessible(true);
            HashMap<String, IBinder> map = (HashMap<String, IBinder>) sCache.get(null);
            map.put("clipboard", hookedBinder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * hook ams, android 26 is different
     */
    public static void hookAMS() {
        try {
            Object gDefault;
            if (Build.VERSION.SDK_INT <= 25) {
                Class<?> amn = Class.forName("android.app.ActivityManagerNative");
                Field gDefaultField = amn.getDeclaredField("gDefault");
                gDefaultField.setAccessible(true);
                gDefault = gDefaultField.get(null);
            } else {
                Class<?> am = Class.forName("android.app.ActivityManager");
                Field iams = am.getDeclaredField("IActivityManagerSingleton");
                iams.setAccessible(true);
                gDefault = iams.get(null);
            }

            Class<?> singleton = Class.forName("android.util.Singleton");
            Field instanceField = singleton.getDeclaredField("mInstance");
            instanceField.setAccessible(true);
            Object iActivityManager = instanceField.get(gDefault);

            Class<?> iActivityManagerProxy = Class.forName("android.app.IActivityManager");
            Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                    new Class[] { iActivityManagerProxy }, new AMSHookHandler(iActivityManager));

            instanceField.set(gDefault, proxy);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * hook pms
     *
     * @param context context
     */
    public static void hookPMS(Context context) {
        try {
            Class<?> atClazz = Class.forName("android.app.ActivityThread");
            Method catMethod = atClazz.getDeclaredMethod("currentActivityThread");
            Object cat = catMethod.invoke(null);
            Field spm = atClazz.getDeclaredField("sPackageManager");
            spm.setAccessible(true);
            Object ipm = spm.get(cat);

            Class<?> ipmClazz = Class.forName("android.content.pm.IPackageManager");
            Object proxy =
                    Proxy.newProxyInstance(ipmClazz.getClassLoader(), new Class[] { ipmClazz },
                            new PMSHookHandler(ipm));
            spm.set(cat, proxy);

            PackageManager pm = context.getPackageManager();
            Field mPM = pm.getClass().getDeclaredField("mPM");
            mPM.setAccessible(true);
            mPM.set(pm, proxy);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * hook instrumentation
     */
    public static void hookInstrumentation() {
        try {
            Class<?> clazz = Class.forName("android.app.ActivityThread");
            Method ca = clazz.getDeclaredMethod("currentActivityThread");
            ca.setAccessible(true);
            Object currentAT = ca.invoke(null);

            Field mInstrumentation = clazz.getDeclaredField("mInstrumentation");
            mInstrumentation.setAccessible(true);
            Instrumentation instrumentation = (Instrumentation) mInstrumentation.get(currentAT);

            InstrumentationProxy proxy = new InstrumentationProxy(instrumentation);
            mInstrumentation.set(currentAT, proxy);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * hook mH
     */
    public static void hookActivityThreadHandler() {
        try {
            Class<?> clazz = Class.forName("android.app.ActivityThread");
            Field atField = clazz.getDeclaredField("sCurrentActivityThread");
            atField.setAccessible(true);
            Object at = atField.get(null);

            Field handler = clazz.getDeclaredField("mH");
            handler.setAccessible(true);
            Handler mH = (Handler) handler.get(at);

            Field callback = Handler.class.getDeclaredField("mCallback");
            callback.setAccessible(true);
            callback.set(mH, new ActivityThreadHandlerCallback(mH));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
