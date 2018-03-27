package com.ymlion.apkload.base;

import dalvik.system.DexClassLoader;

/**
 * Created by YMlion on 2018/3/9.
 */

public class PluginClassLoader extends DexClassLoader {
    public PluginClassLoader(String dexPath, String optimizedDirectory, String librarySearchPath,
            ClassLoader parent) {
        super(dexPath, optimizedDirectory, librarySearchPath, parent);
    }
}
