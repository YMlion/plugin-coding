package com.ymlion.apkload.base;

import android.app.Application;
import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.util.ArrayMap;
import android.util.Log;
import com.ymlion.apkload.AppContext;
import com.ymlion.apkload.util.FileUtil;
import com.ymlion.apkload.util.HookUtil;
import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created by YMlion on 2018/3/27.
 */

public class PluginManager {
    private static final String TAG = "PluginManager";
    private Map<String, Object> apkCache;
    private Map<String, AppPlugin> mPluginMap;

    private Context mBase;

    private static PluginManager INSTANCE = null;

    public static PluginManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PluginManager();
        }

        return INSTANCE;
    }

    private PluginManager() {
        mBase = AppContext.getInstance().getBaseContext();
        apkCache = new ArrayMap<>();
        mPluginMap = new ArrayMap<>();
    }

    public void init(Context context) {
        // 当插件activity继承自AppCompatActivity时，是还会校验该activity是否在manifest中注册，因此启动时会报错
        // 找不到类；改为继承Activity之后则不会出现该问题。若要继承自AppcompatActivity，则需要研究
        // Instrumentation的hook，或者hook掉pms
        HookUtil.hookAMS();
        HookUtil.hookActivityThreadHandler();
        HookUtil.hookPMS(context);
        HookUtil.hookInstrumentation(context);
        preLoad(context);
    }

    private void preLoad(Context context) {
        File[] files = context.getFilesDir().listFiles();
        for (File file : files) {
            String path = file.getAbsolutePath();
            if (path.endsWith(".apk") || path.endsWith(".jar")) {
                installPlugin(context, path);
            }
        }
    }

    public void loadPlugin(Context context, String apkPath) {
        apkPath = FileUtil.getPluginFile(context, apkPath).getAbsolutePath();
        installPlugin(context, apkPath);
    }

    public void removeAllPlugin(Context context) {
        File[] files = context.getFilesDir().listFiles();
        for (File file : files) {
            if (file.getName().endsWith(".apk") || file.getName().endsWith(".jar")) {
                file.delete();
            }
        }
        if (apkCache != null) {
            apkCache.clear();
        }
        if (mPluginMap != null) {
            mPluginMap.clear();
        }
    }

    private void installPlugin(Context context, String apkPath) {
        try {
            Class<?> atClazz = Class.forName("android.app.ActivityThread");
            Object atInstance = HookUtil.getField(atClazz, "sCurrentActivityThread");
            Map mPackages = (Map) HookUtil.getField(atClazz, "mPackages", atInstance);

            Class<?> ppClazz = Class.forName("android.content.pm.PackageParser");
            Object pp = ppClazz.newInstance();
            Method parsePackage = ppClazz.getDeclaredMethod("parsePackage", File.class, int.class);

            Object pkg = parsePackage.invoke(pp, new File(apkPath), 0);

            //Method collectCertificates = ppClazz.getDeclaredMethod("collectCertificates", pkg.getClass(), int.class);
            //collectCertificates.setAccessible(true);
            //collectCertificates.invoke(null, pkg, 0);

            Class<?> pusClazz = Class.forName("android.content.pm.PackageUserState");
            Object pus = pusClazz.newInstance();

            Method generateApplicationInfo =
                    ppClazz.getDeclaredMethod("generateApplicationInfo", pkg.getClass(), int.class,
                            pusClazz);
            ApplicationInfo ai =
                    (ApplicationInfo) generateApplicationInfo.invoke(null, pkg, 0, pus);

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

            ai.sourceDir = apkPath;
            ai.publicSourceDir = apkPath;

            String dexDir = context.getDir("dex", Context.MODE_PRIVATE).getAbsolutePath();
            // TODO: 2018/3/26 想了好久，context.getClassLoader().getParent()就可以加载AppcompatActivity
            //PluginClassLoader classLoader = new PluginClassLoader(apkPath, dexDir, null,
            //        context.getClassLoader().getParent());
            PluginClassLoader classLoader = new PluginClassLoader(apkPath, dexDir, null,
                    ClassLoader.getSystemClassLoader());
            HookUtil.setField(loadedApk.getClass(), "mClassLoader", loadedApk, classLoader);
            Log.d(TAG, "hookPluginActivity: "
                    + context.getClassLoader().toString()
                    + "; parent : "
                    + context.getClassLoader().getParent());

            PluginManager.getInstance().cachePackage(ai.packageName, loadedApk);
            AppPlugin appPlugin =
                    new AppPlugin(classLoader, HookUtil.getPluginResources(context, apkPath));
            appPlugin.parsePackage(ai.packageName, pkg);

            Instrumentation mInstrumentation =
                    (Instrumentation) HookUtil.getField(atClazz, "mInstrumentation", atInstance);
            Application app = mInstrumentation.newApplication(classLoader,
                    ai.className == null ? "android.app.Application" : ai.className,
                    appPlugin.getPluginContext());
            mInstrumentation.callApplicationOnCreate(app);
            appPlugin.setApplication(app);

            WeakReference ref = new WeakReference(loadedApk);
            mPackages.put(ai.packageName, ref);
            Log.e(TAG, "hookPluginActivity: " + ai.packageName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Context getBase() {
        return mBase;
    }

    public void cachePlugin(String key, AppPlugin plugin) {
        mPluginMap.put(key, plugin);
    }

    public AppPlugin getCachePlugin(String key) {
        return mPluginMap.get(key);
    }

    public void cachePackage(String key, Object pkg) {
        apkCache.put(key, pkg);
    }

    public Object getCachePackage(String key) {
        return apkCache.get(key);
    }

    public void registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        mBase.registerReceiver(receiver, filter);
    }
}
