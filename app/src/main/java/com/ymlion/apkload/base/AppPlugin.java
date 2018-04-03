package com.ymlion.apkload.base;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.util.Log;
import com.ymlion.apkload.util.HookUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by YMlion on 2018/3/9.
 */

public class AppPlugin {
    private static final String TAG = "AppPlugin";

    public ApplicationInfo mApplicationInfo;
    private Resources mResources;
    private ClassLoader mClassLoader;
    private Context mPluginContext;

    public List<ActivityInfo> mActivityInfos;
    public List<ServiceInfo> mServiceInfos;
    public List<ActivityInfo> mReceiverInfos;
    public List<ProviderInfo> mProviderInfos;
    private Application mApplication;
    private Object mPackage;

    public AppPlugin(ClassLoader classLoader, Resources resources) {
        mActivityInfos = new ArrayList<>();
        mServiceInfos = new ArrayList<>();
        mReceiverInfos = new ArrayList<>();
        mProviderInfos = new ArrayList<>();
        mPluginContext = new PluginContext(this);
        this.mClassLoader = classLoader;
        this.mResources = resources;
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

    public Context getPluginContext() {
        return mPluginContext;
    }

    public Object getPackage() {
        return mPackage;
    }

    public ApplicationInfo getApplicationInfo() {
        return mApplicationInfo;
    }

    public void setApplicationInfo(ApplicationInfo applicationInfo) {
        mApplicationInfo = applicationInfo;
    }

    public void parsePackage(String packageName, Object pkg) {
        if (pkg == null) {
            return;
        }
        try {
            mPackage = pkg;

            List<?> activities = (List<?>) HookUtil.getField(pkg.getClass(), "activities", pkg);
            for (Object activity : activities) {
                ActivityInfo info =
                        (ActivityInfo) HookUtil.getField(activity.getClass(), "info", activity);
                mActivityInfos.add(info);
                Log.d(TAG, "parsePackage: " + info.name);
            }
            List<?> services = (List<?>) HookUtil.getField(pkg.getClass(), "services", pkg);
            for (Object service : services) {
                mServiceInfos.add(
                        (ServiceInfo) HookUtil.getField(service.getClass(), "info", service));
            }
            List<?> providers = (List<?>) HookUtil.getField(pkg.getClass(), "providers", pkg);
            for (Object provider : providers) {
                mProviderInfos.add(
                        (ProviderInfo) HookUtil.getField(provider.getClass(), "info", provider));
            }
            List<?> receivers = (List<?>) HookUtil.getField(pkg.getClass(), "receivers", pkg);
            for (Object receiver : receivers) {
                ActivityInfo info =
                        (ActivityInfo) HookUtil.getField(receiver.getClass(), "info", receiver);
                mReceiverInfos.add(info);
                try {
                    BroadcastReceiver br = BroadcastReceiver.class.cast(
                            mClassLoader.loadClass(info.name).newInstance());
                    ArrayList<? extends IntentFilter> intentFilters =
                            (ArrayList<? extends IntentFilter>) HookUtil.getField(
                                    "android.content.pm.PackageParser$Component", "intents",
                                    receiver);
                    for (IntentFilter filter : intentFilters) {
                        PluginManager.getInstance().registerReceiver(br, filter);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            PluginManager.getInstance().cachePlugin(packageName, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setApplication(Application application) {
        mApplication = application;
    }

    public Application getApplication() {
        return mApplication;
    }
}
