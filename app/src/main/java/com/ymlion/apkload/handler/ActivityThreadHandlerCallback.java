package com.ymlion.apkload.handler;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.ymlion.apkload.model.AppPlugin;
import java.lang.reflect.Field;

/**
 * Created by YMlion on 2018/2/24.
 */

public class ActivityThreadHandlerCallback implements Handler.Callback {

    private static final String TAG = "ActivityThreadHandler";

    @Override public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case 100:
                Log.d(TAG, "launch a activity!!!");
                handleLaunchActivity(msg);
                break;
        }
        return false;
    }

    private void handleLaunchActivity(Message msg) {
        Object activityClientRecord = msg.obj;
        try {
            Field activityInfoF = activityClientRecord.getClass().getDeclaredField("activityInfo");
            activityInfoF.setAccessible(true);
            ActivityInfo activityInfo = (ActivityInfo) activityInfoF.get(activityClientRecord);

            Field intentField = activityClientRecord.getClass().getDeclaredField("intent");
            intentField.setAccessible(true);
            Intent origin = (Intent) intentField.get(activityClientRecord);
            ComponentName oc = origin.getComponent();
            String targetClass = origin.getStringExtra("targetClass");
            if (targetClass != null && oc != null && oc.getClassName().endsWith("StubActivity")) {
                String targetPkg = origin.getStringExtra("targetPackage");
                AppPlugin appPlugin = AppPlugin.mPluginMap.get(targetPkg);
                if (appPlugin != null) {
                    Log.d(TAG, "handleLaunchActivity: "
                            + activityInfo.applicationInfo.packageName
                            + " ; theme is "
                            + activityInfo.theme
                            + "; after is "
                            + appPlugin.mApplicationInfo.theme);
                    for (ActivityInfo ai : appPlugin.mActivityInfos) {
                        if (targetClass.equals(ai.name)) {
                            activityInfo.theme = ai.theme;
                            break;
                        }
                    }
                    if (activityInfo.theme <= 0) {
                        activityInfo.theme = appPlugin.mApplicationInfo.theme;
                    }
                }

                Log.d(TAG, "handleLaunchActivity: " + targetPkg + " : " + targetClass);
                ComponentName tc = new ComponentName(targetPkg, targetClass);
                origin.setComponent(tc);
                activityInfo.applicationInfo.packageName = targetPkg;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
