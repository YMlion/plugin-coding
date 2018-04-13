package com.ymlion.pluginuninstalled;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_p1).setOnClickListener(
                v -> startActivity(new Intent(MainActivity.this, Plugin1Activity.class)));
        findViewById(R.id.btn_p2).setOnClickListener(
                v -> startActivity(new Intent(MainActivity.this, Plugin2Activity.class)));
        findViewById(R.id.btn_p4).setOnClickListener(
                v -> startActivity(new Intent(MainActivity.this, Plugin4Activity.class)));
    }
    //adb push plugin-uninstalled/build/outputs/apk/debug/plugin-uninstalled-debug.apk /sdcard/apkload_plugin.apk
}
