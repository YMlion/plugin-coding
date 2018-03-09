package com.ymlion.apkload.model;

import android.content.pm.ActivityInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.util.Log;
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

    public List<ActivityInfo> mActivityInfos;
    public List<ServiceInfo> mServiceInfos;
    public List<ActivityInfo> mReceiverInfos;
    public List<ProviderInfo> mProviderInfos;

    public AppPlugin() {
        mActivityInfos = new ArrayList<>();
        mServiceInfos = new ArrayList<>();
        mReceiverInfos = new ArrayList<>();
        mProviderInfos = new ArrayList<>();
    }

    public static void parsePackage(String packageName, Object pkg) {
        if (pkg == null) {
            return;
        }
        try {
            AppPlugin appPlugin = new AppPlugin();
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
