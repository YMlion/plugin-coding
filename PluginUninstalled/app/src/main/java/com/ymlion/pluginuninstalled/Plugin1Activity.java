package com.ymlion.pluginuninstalled;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class Plugin1Activity extends Activity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin1);
        findViewById(R.id.open_p2).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(Plugin1Activity.this, Plugin2Activity.class));
            }
        });
    }
}
