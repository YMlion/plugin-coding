package com.ymlion.pluginuninstalled;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

public class Plugin1Service extends Service {
    private static final String TAG = "Plugin1Service";
    private boolean flag = true;
    public Plugin1Service() {
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }

    @Override public void onCreate() {
        super.onCreate();
        Log.d(TAG, "plugin1 service onCreate");
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "plugin1 service onStartCommand");
        new Thread(() -> {
            int i = 0;
            while (flag) {
                SystemClock.sleep(1000);
                Log.d(TAG, "run " + i++);
                if (i == 10) {
                    stopSelf();
                }
            }
            Log.d(TAG, "plugin1 service stop the background thread.");
        }).start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override public void onDestroy() {
        flag = false;
        super.onDestroy();
        Log.d(TAG, "plugin1 service onDestroy.");
    }
}
