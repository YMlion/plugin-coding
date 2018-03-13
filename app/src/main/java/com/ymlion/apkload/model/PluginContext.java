package com.ymlion.apkload.model;

import android.content.ContextWrapper;
import android.content.res.Resources;

/**
 * Created by YMlion on 2018/3/13.
 */

public class PluginContext extends ContextWrapper {
    private AppPlugin mAppPlugin;

    public PluginContext(AppPlugin appPlugin) {
        super(appPlugin.getBase());
        mAppPlugin = appPlugin;
    }

    @Override public Resources getResources() {
        return mAppPlugin.getResources();
    }

    @Override public ClassLoader getClassLoader() {
        return mAppPlugin.getClassLoader();
    }

    @Override public Resources.Theme getTheme() {
        Resources.Theme theme = mAppPlugin.getResources().newTheme();
        theme.applyStyle(mAppPlugin.mApplicationInfo.theme, false);
        return theme;
    }

    @Override public String getPackageName() {
        return mAppPlugin.mApplicationInfo.packageName;
    }
}
