package com.ymlion.pluginuninstalled;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class Plugin3Activity extends Activity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin3);
        sendBroadcast(
                new Intent("com.ymlion.pluginuninstalled.PLUGIN_RECEIVER_1").putExtra("content",
                        "hello receiver, this is from plugin3 activity"));
        sendBroadcast(
                new Intent("com.ymlion.pluginuninstalled.PLUGIN_ACTIVITY_1").putExtra("content",
                        "hello activity, this is from plugin3 activity"));
    }
}
