package com.ymlion.pluginuninstalled;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

public class Plugin2Activity extends AppCompatActivity {

    private static final String TAG = "Plugin2Activity";
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin2);
    }

    @Override protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        Log.d(TAG, "attachBaseContext: " + getClassLoader() + "; parent : " + getClassLoader().getParent());
    }
}
