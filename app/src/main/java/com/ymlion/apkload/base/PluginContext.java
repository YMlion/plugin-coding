package com.ymlion.apkload.base;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;

/**
 * Created by YMlion on 2018/3/13.
 */

public class PluginContext extends ContextWrapper {
    private static final String TAG = "PluginContext";
    private AppPlugin mAppPlugin;

    public PluginContext(AppPlugin appPlugin) {
        super(PluginManager.getInstance().getBase());
        mAppPlugin = appPlugin;
    }

    @Override public Resources getResources() {
        Log.d(TAG, "getResources");
        return mAppPlugin.getResources();
    }

    @Override public ClassLoader getClassLoader() {
        Log.d(TAG, "getClassLoader");
        return mAppPlugin.getClassLoader();
    }

    @Override public Resources.Theme getTheme() {
        Log.d(TAG, "get the plugin theme");
        Resources.Theme theme = mAppPlugin.getResources().newTheme();
        theme.applyStyle(mAppPlugin.mApplicationInfo.theme, false);
        return theme;
    }

    @Override public String getPackageName() {
        return mAppPlugin.mApplicationInfo.packageName;
    }

    @Override public Context createConfigurationContext(Configuration overrideConfiguration) {
        Log.d(TAG, "createConfigurationContext");
        return super.createConfigurationContext(overrideConfiguration);
    }

    @Override public Object getSystemService(String name) {
        Log.d(TAG, "getSystemService: " + name);
        return super.getSystemService(name);
    }
}
