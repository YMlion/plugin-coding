package com.ymlion.apkload;

import android.app.Application;
import android.util.Log;
import com.ymlion.apkload.base.PluginManager;

/**
 * Created by YMlion on 2018/3/13.
 */

public class AppContext extends Application {

    private static final String TAG = "AppContext";
    private static AppContext appContext;

    public static AppContext getInstance() {
        return appContext;
    }

    @Override public void onCreate() {
        super.onCreate();
        appContext = this;
        Log.d(TAG, "App context is created.");
        PluginManager.getInstance().init(this);
    }
}
