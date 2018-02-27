package com.ymlion.apkload.handler;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.lang.reflect.Field;

/**
 * Created by YMlion on 2018/2/24.
 */

public class ActivityThreadHandlerCallback implements Handler.Callback {

    private static final String TAG = "ActivityThreadHandlerCa";
    private Handler base;

    public ActivityThreadHandlerCallback(Handler base) {
        this.base = base;
    }

    @Override public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case 100:
                Log.d(TAG, "launch a activity!!!");
                handleLaunchActivity(msg);
                break;
        }
        base.handleMessage(msg);
        return true;
    }

    private void handleLaunchActivity(Message msg) {
        Object activityClientRecord = msg.obj;
        try {
            Field intentField = activityClientRecord.getClass().getDeclaredField("intent");
            intentField.setAccessible(true);
            Intent origin = (Intent) intentField.get(activityClientRecord);
            ComponentName oc = origin.getComponent();
            String targetClass = origin.getStringExtra("targetClass");
            if (targetClass != null && oc != null && oc.getClassName().endsWith("StubActivity")) {
                Log.d(TAG, "handleLaunchActivity: " + targetClass);
                ComponentName tc = new ComponentName(oc.getPackageName(), targetClass);
                origin.setComponent(tc);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
