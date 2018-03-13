package com.ymlion.apkload.model;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.util.Log;
import com.ymlion.apkload.AppContext;
import com.ymlion.apkload.util.HookUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by YMlion on 2018/3/9.
 */

public class AppPlugin {
    private static final String TAG = "AppPlugin";
    public static Map<String, Object> apkCache;
    public static Map<String, AppPlugin> mPluginMap;

    private Context mBase;
    public ApplicationInfo mApplicationInfo;
    private Resources mResources;
    private ClassLoader mClassLoader;
    private Context mPluginContext;

    public List<ActivityInfo> mActivityInfos;
    public List<ServiceInfo> mServiceInfos;
    public List<ActivityInfo> mReceiverInfos;
    public List<ProviderInfo> mProviderInfos;

    public AppPlugin() {
        mBase = AppContext.getInstance().getBaseContext();
        mActivityInfos = new ArrayList<>();
        mServiceInfos = new ArrayList<>();
        mReceiverInfos = new ArrayList<>();
        mProviderInfos = new ArrayList<>();
        mPluginContext = new PluginContext(this);
    }

    public void setResources(Resources resources) {
        mResources = resources;
    }

    public Resources getResources() {
        return mResources;
    }

    public void setClassLoader(ClassLoader classLoader) {
        mClassLoader = classLoader;
    }

    public ClassLoader getClassLoader() {
        return mClassLoader;
    }

    public Context getBase() {
        return mBase;
    }

    public Context getPluginContext() {
        return mPluginContext;
    }

    public static AppPlugin parsePackage(String packageName, Object pkg) {
        if (pkg == null) {
            return null;
        }
        try {
            AppPlugin appPlugin = new AppPlugin();
            ApplicationInfo applicationInfo =
                    (ApplicationInfo) HookUtil.getField(pkg.getClass(), "applicationInfo", pkg);
            appPlugin.mApplicationInfo = applicationInfo;

            List<?> activities = (List<?>) HookUtil.getField(pkg.getClass(), "activities", pkg);
            for (Object activity : activities) {
                ActivityInfo info =
                        (ActivityInfo) HookUtil.getField(activity.getClass(), "info", activity);
                appPlugin.mActivityInfos.add(info);
                Log.d(TAG, "parsePackage: " + info.name);
            }
            List<?> services = (List<?>) HookUtil.getField(pkg.getClass(), "services", pkg);
            for (Object service : services) {
                appPlugin.mServiceInfos.add(
                        (ServiceInfo) HookUtil.getField(service.getClass(), "info", service));
            }
            List<?> providers = (List<?>) HookUtil.getField(pkg.getClass(), "providers", pkg);
            for (Object provider : providers) {
                appPlugin.mProviderInfos.add(
                        (ProviderInfo) HookUtil.getField(provider.getClass(), "info", provider));
            }
            List<?> receivers = (List<?>) HookUtil.getField(pkg.getClass(), "receivers", pkg);
            for (Object receiver : receivers) {
                appPlugin.mReceiverInfos.add(
                        (ActivityInfo) HookUtil.getField(receiver.getClass(), "info", receiver));
            }
            if (mPluginMap == null) {
                mPluginMap = new HashMap<>();
            }
            mPluginMap.put(packageName, appPlugin);
            return appPlugin;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
