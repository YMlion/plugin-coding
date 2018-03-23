package com.ymlion.pluginuninstalled;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class Plugin1Activity extends Activity {

    private static final String TAG = "Plugin1Activity";
    BroadcastReceiver receiver;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin1);
        findViewById(R.id.open_p2).setOnClickListener(
                v -> startActivity(new Intent(Plugin1Activity.this, Plugin2Activity.class)));
        findViewById(R.id.open_p3).setOnClickListener(
                v -> startActivity(new Intent(Plugin1Activity.this, Plugin3Activity.class)));
        receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "哇，收到了！");
                Log.d(TAG, intent.getStringExtra("content"));
            }
        };
        registerReceiver(receiver, new IntentFilter("com.ymlion.pluginuninstalled.PLUGIN_ACTIVITY_1"));
    }

    @Override protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }
}
