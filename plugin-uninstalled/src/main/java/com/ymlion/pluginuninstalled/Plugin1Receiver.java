package com.ymlion.pluginuninstalled;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by YMlion on 2018/3/15.
 */

public class Plugin1Receiver extends BroadcastReceiver {
    private static final String TAG = "Plugin1Receiver";
    @Override public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: " + intent.getStringExtra("content"));
    }
}
