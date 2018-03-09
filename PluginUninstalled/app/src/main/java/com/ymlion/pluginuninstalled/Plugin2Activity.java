package com.ymlion.pluginuninstalled;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class Plugin2Activity extends AppCompatActivity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin1);
        TextView tv = findViewById(R.id.textView);
        tv.setText("Hello, this is plugin 2 activity.");
    }
}
