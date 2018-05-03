### modules

[plugin-uninstalled](https://github.com/YMlion/plugin-coding/tree/master/plugin-uninstalled)是插件工程，生成的apk放在sd卡根目录；

[app](https://github.com/YMlion/plugin-coding/tree/master/app)则是宿主及插件化的主要实现。

[aapt-gradle-plugin](https://github.com/YMlion/plugin-coding/tree/master/aapt-gradle-plugin)是根据[Small](https://github.com/wequick/Small)中的gradle插件来简单修改的一个用于修改插件资源id的gradle插件，其中主要依赖的修改二进制资源文件package id的库是[android-res-parser](https://github.com/YMlion/android-res-parser)，该库使用`kotlin`开发。

### 生成jar包遇到的问题

`aapt-gradle-plugin`被正常打包成jar文件之后，在其他module中使用时，在用到`android-res-parser`中的类时，会报`java.lang.NoClassDefFoundError`，出现该问题是因为在运行时找不到对应的类，而编译时没有问题。出现该问题就说明在module依赖中没有`android-res-parser`对应的jar包，所以`aapt-gradle-plugin`在被打包成jar文件时，所依赖的`libs/*.jar`没有被打包到jar文件中。

知道了原因，就好解决了，因为`aapt-gradle-plugin`是一个`gradle`插件，所以并不能简单地将`android-res-parser`的jar文件添加到module的依赖中就可以了。只能把所依赖的jar文件也打包到`aapt-gradle-plugin`生成的jar/aar文件中，实现也很简单，在插件对应的`build.gradle`添加：

```groovy
jar {// 生成jar配置
    /*from {
        //添加所有的依懒到打包文件
        configurations.compile.collect {
            it.isDirectory() ? it : zipTree(it)
        }
    }*/
    // 这里只需要添加该jar包就可以了
    from(zipTree('libs/parser.jar'))
}
```