package com.ymlion.apkload.model;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;

public class ProxyService extends Service {
    public static final String COMMAND = "command";
    public static final int COMMAND_START = 1;
    public static final int COMMAND_STOP = 2;

    public ProxyService() {
    }

    @Nullable @Override public IBinder onBind(Intent intent) {
        return null;
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String targetClass = intent.getStringExtra("targetClass");
        String targetPkg = intent.getStringExtra("targetPackage");

        if (TextUtils.isEmpty(targetClass)) {
            return super.onStartCommand(intent, flags, startId);
        }
        switch (intent.getIntExtra(COMMAND, 1)) {
            case COMMAND_START:

                break;
            case COMMAND_STOP:
                break;
            default:
                break;
        }

        return super.onStartCommand(intent, flags, startId);
    }
}
