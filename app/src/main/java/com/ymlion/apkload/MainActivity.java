package com.ymlion.apkload;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;
import com.ymlion.apkload.util.HookUtil;
import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ImageView mImageView;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mImageView = findViewById(R.id.image);
        findViewById(R.id.btn_local).setOnClickListener(
                v -> startActivity(new Intent(MainActivity.this, New1Activity.class)));
        findViewById(R.id.btn_sd).setOnClickListener(v -> {
            // todo 启动非本地的activity，需要修改目标activity的resources、context等，所以现在还无法启动
            Intent intent = new Intent();
            ComponentName componentName = new ComponentName("com.ymlion.pluginuninstalled",
                    "com.ymlion.pluginuninstalled.Plugin1Activity");
            intent.setComponent(componentName);
            //startActivity(intent);
        });
    }

    @Override protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        // 当插件activity继承自AppCompatActivity时，是还会校验该activity是否在manifest中注册，因此启动时会报错
        // 找不到类；改为继承Activity之后则不会出现该问题。若要继承自AppcompatActivity，则需要研究
        // Instrumentation的hook，或者hook掉pms
        HookUtil.hookAMS();
        HookUtil.hookActivityThreadHandler();
        HookUtil.hookPMS(this);
        //HookUtil.hookInstrumentation(this);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_green) {
            Toast.makeText(this, "load green icon", Toast.LENGTH_SHORT).show();
            loadPluginInstalled();
            return true;
        } else if (id == R.id.action_red) {
            Toast.makeText(this, "load red icon", Toast.LENGTH_SHORT).show();
            loadPluginUninstalled();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadPluginInstalled() {
        List<String> plugins = findInstalledPlugins();
        try {
            String pn = plugins.get(0);
            Context pluginContext =
                    createPackageContext(pn, CONTEXT_IGNORE_SECURITY | CONTEXT_INCLUDE_CODE);
            PathClassLoader classLoader =
                    new PathClassLoader(pluginContext.getPackageResourcePath(),
                            ClassLoader.getSystemClassLoader());
            Class<?> clazz = classLoader.loadClass(pn + ".R$drawable");
            Field icon = clazz.getDeclaredField("ic_android");
            int iconId = icon.getInt(R.drawable.class);
            mImageView.setImageDrawable(pluginContext.getResources().getDrawable(iconId));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 找到已安装的插件
     */
    private List<String> findInstalledPlugins() {
        PackageManager pm = getPackageManager();
        List<String> plugins = new ArrayList<>();
        List<PackageInfo> packageInfos =
                pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
        for (PackageInfo packageInfo : packageInfos) {
            String pName = packageInfo.packageName;
            String sharedUid = packageInfo.sharedUserId;
            if ("com.ymlion.apkload".equals(sharedUid) && !pName.equals(sharedUid)) {
                plugins.add(pName);
            }
        }
        return plugins;
    }

    private void loadPluginUninstalled() {
        String apkPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator
                + "apkload_plugin.apk";
        PackageManager pm = getPackageManager();
        PackageInfo packageInfo = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
        if (packageInfo == null) {
            Toast.makeText(this, "未找到插件", Toast.LENGTH_SHORT).show();
            return;
        }
        String pName = packageInfo.packageName;
        String dexDir = getDir("dex", MODE_PRIVATE).getAbsolutePath();
        DexClassLoader classLoader =
                new DexClassLoader(apkPath, dexDir, null, ClassLoader.getSystemClassLoader());
        try {
            Class<?> clazz = classLoader.loadClass(pName + ".R$drawable");
            Field icon = clazz.getDeclaredField("ic_android");
            int iconId = icon.getInt(R.drawable.class);
            Resources resources = getPluginResources(apkPath);
            if (resources == null) {
                Toast.makeText(this, "resource load failed", Toast.LENGTH_SHORT).show();
                return;
            }
            mImageView.setImageDrawable(resources.getDrawable(iconId));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Resources getPluginResources(String apkPath) {
        try {
            AssetManager am = AssetManager.class.newInstance();
            Method addAsset = am.getClass().getDeclaredMethod("addAssetPath", String.class);
            addAsset.invoke(am, apkPath);
            Resources res = getResources();
            Resources resources =
                    new Resources(am, res.getDisplayMetrics(), res.getConfiguration());
            return resources;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
