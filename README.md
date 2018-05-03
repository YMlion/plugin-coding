[plugin-uninstalled](https://github.com/YMlion/plugin-coding/tree/master/plugin-uninstalled)是插件工程，生成的apk放在sd卡根目录；

[app](https://github.com/YMlion/plugin-coding/tree/master/app)则是宿主及插件化的主要实现。

[aapt-gradle-plugin](https://github.com/YMlion/plugin-coding/tree/master/aapt-gradle-plugin)是根据[Small](https://github.com/wequick/Small)中的gradle插件来简单修改的一个用于修改插件资源id的gradle插件，其中主要依赖的修改二进制资源文件package id的库是[android-res-parser](https://github.com/YMlion/android-res-parser)，该库使用`kotlin`开发。