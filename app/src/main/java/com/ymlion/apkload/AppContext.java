package com.ymlion.apkload;

import android.app.Application;

/**
 * Created by YMlion on 2018/3/13.
 */

public class AppContext extends Application {

    private static AppContext appContext;

    public static AppContext getInstance() {
        return appContext;
    }

    @Override public void onCreate() {
        super.onCreate();
        appContext = this;
    }
}
