package com.ymlion.apkload;

import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.ymlion.apkload.util.HookUtil;

/**
 * 通过使用插桩来启动
 */
public class New1Activity extends AppCompatActivity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new1);
        HookUtil.hookClipboard();
        HookUtil.hookPMS(this);
        PackageManager pm = getPackageManager();
        pm.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
    }
}
