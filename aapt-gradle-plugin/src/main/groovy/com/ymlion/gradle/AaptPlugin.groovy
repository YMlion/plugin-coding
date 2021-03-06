package com.ymlion.gradle

import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by YMlion on 2018/4/11.*/
class AaptPlugin implements Plugin<Project> {

    @Override void apply(Project project) {

        def listener = new ResourceListener(project)
        project.gradle.addListener(listener)
        project.extensions.create('aapt', AaptExtension)

        def android = project.android

        android.applicationVariants.all { BaseVariant variant ->
            def pr = project.tasks["process${variant.name.capitalize()}Resources"]
            if (pr != null && variant.name.equalsIgnoreCase('debug')) {
                listener.setTaskName(pr.name)
                listener.setApkVariant(variant)
            }
        }
    }
}
