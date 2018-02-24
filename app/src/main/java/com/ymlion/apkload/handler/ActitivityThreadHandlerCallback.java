package com.ymlion.apkload.handler;

import android.os.Handler;
import android.os.Message;
import java.lang.reflect.Method;

/**
 * Created by YMlion on 2018/2/24.
 */

public class ActitivityThreadHandlerCallback implements Handler.Callback {

    private Handler base;

    public ActitivityThreadHandlerCallback(Handler base) {
        this.base = base;
    }

    @Override public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case 100:
                handleLaunchActivity(msg);
                break;
        }
        base.handleMessage(msg);
        return true;
    }

    private void handleLaunchActivity(Message msg) {
        Object activityClientRecord = msg.obj;

    }
}
