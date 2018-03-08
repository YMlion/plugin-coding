package com.ymlion.apkload.util;

import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import com.ymlion.apkload.InstrumentationProxy;
import com.ymlion.apkload.handler.AMSHookHandler;
import com.ymlion.apkload.handler.ActivityThreadHandlerCallback;
import com.ymlion.apkload.handler.BinderProxyHandler;
import com.ymlion.apkload.handler.PMSHookHandler;
import dalvik.system.DexClassLoader;
import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * hook utils
 *
 * Created by YMlion on 2018/2/23.
 */

public class HookUtil {

    private static final String TAG = "HookUtil";

    private static Map<String, Object> apkCache;

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

            InstrumentationProxy proxy = new InstrumentationProxy(instrumentation, context);
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

    public static void hookPluginActivity(Context context) {
        try {
            Class<?> atClazz = Class.forName("android.app.ActivityThread");
            Object atInstance = getField(atClazz, "sCurrentActivityThread");
            Map mPackages = (Map) getField(atClazz, "mPackages", atInstance);

            Class<?> ppClazz = Class.forName("android.content.pm.PackageParser");
            Object pp = ppClazz.newInstance();
            Method parsePackage = ppClazz.getDeclaredMethod("parsePackage", File.class, int.class);

            String apkPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + File.separator
                    + "apkload_plugin.apk";
            Object pkg = parsePackage.invoke(pp, new File(apkPath), 0);

            //Method collectCertificates = ppClazz.getDeclaredMethod("collectCertificates", pkg.getClass(), int.class);
            //collectCertificates.invoke(null, pkg, 0);

            Class<?> pusClazz = Class.forName("android.content.pm.PackageUserState");
            Object pus = pusClazz.newInstance();

            Method generateApplicationInfo =
                    ppClazz.getDeclaredMethod("generateApplicationInfo", pkg.getClass(), int.class,
                            pusClazz);
            ApplicationInfo ai =
                    (ApplicationInfo) generateApplicationInfo.invoke(null, pkg, 0, pus);
            ai.sourceDir = apkPath;
            ai.publicSourceDir = apkPath;

            Class<?> compatibilityInfoClazz =
                    Class.forName("android.content.res.CompatibilityInfo");
            Field defaultCompatibilityInfoField =
                    compatibilityInfoClazz.getDeclaredField("DEFAULT_COMPATIBILITY_INFO");
            defaultCompatibilityInfoField.setAccessible(true);
            Object defaultCompatibilityInfo = defaultCompatibilityInfoField.get(null);

            Method getPackageInfoNoCheck =
                    atClazz.getDeclaredMethod("getPackageInfoNoCheck", ai.getClass(),
                            compatibilityInfoClazz);
            Object loadedApk =
                    getPackageInfoNoCheck.invoke(atInstance, ai, defaultCompatibilityInfo);

            String dexDir = context.getDir("dex", Context.MODE_PRIVATE).getAbsolutePath();
            DexClassLoader classLoader =
                    new DexClassLoader(apkPath, dexDir, null, ClassLoader.getSystemClassLoader());
            setField(loadedApk.getClass(), "mClassLoader", loadedApk, classLoader);

            if (apkCache == null) {
                apkCache = new HashMap<>();
            }
            apkCache.put(ai.packageName, loadedApk);
            WeakReference ref = new WeakReference(loadedApk);
            mPackages.put(ai.packageName, ref);
            Log.e(TAG, "hookPluginActivity: " + ai.packageName);
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

    /**
     * 待加载插件经过opt优化之后存放odex得路径
     */
    public static File getPluginOptDexDir(String packageName, Context context) {
        return enforceDirExists(new File(getPluginBaseDir(packageName, context), "odex"));
    }

    /**
     * 插件得lib库路径, 这个demo里面没有用
     */
    public static File getPluginLibDir(String packageName, Context context) {
        return enforceDirExists(new File(getPluginBaseDir(packageName, context), "lib"));
    }

    private static File sBaseDir;

    // 需要加载得插件得基本目录 /data/data/<package>/files/plugin/
    private static File getPluginBaseDir(String packageName, Context context) {
        if (sBaseDir == null) {
            sBaseDir = context.getFileStreamPath("plugin");
            enforceDirExists(sBaseDir);
        }
        return enforceDirExists(new File(sBaseDir, packageName));
    }

    private static synchronized File enforceDirExists(File sBaseDir) {
        if (!sBaseDir.exists()) {
            boolean ret = sBaseDir.mkdir();
            if (!ret) {
                throw new RuntimeException("create dir " + sBaseDir + "failed");
            }
        }
        return sBaseDir;
    }
}

