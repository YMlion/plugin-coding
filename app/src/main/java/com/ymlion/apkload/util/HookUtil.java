package com.ymlion.apkload.util;

import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import com.ymlion.apkload.handler.AMSHookHandler;
import com.ymlion.apkload.handler.ActivityThreadHandlerCallback;
import com.ymlion.apkload.handler.BinderProxyHandler;
import com.ymlion.apkload.handler.InstrumentationProxy;
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

    private static final String TAG = "HookUtil";

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
                gDefault = getField("android.app.ActivityManagerNative", "gDefault");
            } else {
                gDefault = getField("android.app.ActivityManager", "IActivityManagerSingleton");
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

    public static Object getAMS() {
        try {
            Object gDefault;
            if (Build.VERSION.SDK_INT <= 25) {
                gDefault = getField("android.app.ActivityManagerNative", "gDefault");
            } else {
                gDefault = getField("android.app.ActivityManager", "IActivityManagerSingleton");
            }

            Class<?> singleton = Class.forName("android.util.Singleton");
            Field instanceField = singleton.getDeclaredField("mInstance");
            instanceField.setAccessible(true);
            return instanceField.get(gDefault);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
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
    public static void hookInstrumentation(Context context) {
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
            Object at = getField("android.app.ActivityThread", "sCurrentActivityThread");
            Handler mH = (Handler) getField("android.app.ActivityThread", "mH", at);
            setField(Handler.class, "mCallback", mH, new ActivityThreadHandlerCallback());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Object getField(String className, String fieldName)
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        return getField(className, fieldName, null);
    }

    public static Object getField(String className, String fieldName, Object obj)
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        Class<?> clazz = Class.forName(className);
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }

    public static Object getField(Class clazz, String fieldName)
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        return getField(clazz, fieldName, null);
    }

    public static Object getField(Class clazz, String fieldName, Object obj)
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }

    public static void setField(String className, String fieldName, Object value)
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        setField(className, fieldName, null, value);
    }

    public static void setField(String className, String fieldName, Object obj, Object value)
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        Class<?> clazz = Class.forName(className);
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    public static void setField(Class clazz, String fieldName, Object value)
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        setField(clazz, fieldName, null, value);
    }

    public static void setField(Class clazz, String fieldName, Object obj, Object value)
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    public static void setFieldWithoutException(Class clazz, String fieldName, Object obj,
            Object value) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Resources getPluginResources(Context context, String apkPath) {
        try {
            AssetManager am = AssetManager.class.newInstance();
            Method addAsset = am.getClass().getDeclaredMethod("addAssetPath", String.class);
            addAsset.invoke(am, apkPath);
            Resources res = context.getResources();
            return new Resources(am, res.getDisplayMetrics(), res.getConfiguration());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

