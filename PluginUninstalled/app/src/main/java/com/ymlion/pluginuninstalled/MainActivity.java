package com.ymlion.pluginuninstalled;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    //adb push app/build/outputs/apk/debug/app-debug.apk /sdcard/apkload_plugin.apk
}
