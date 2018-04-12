package com.ymlion.gradle

import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by YMlion on 2018/4/11.*/
public class AaptPlugin implements Plugin<Project> {

    @Override void apply(Project project) {

        def listener = new TaskListener(project)
        project.gradle.addListener(listener)

        def android = project.android

        android.applicationVariants.all { BaseVariant variant ->
            def pr = project.tasks["process${variant.name.capitalize()}Resources"]
            if (pr != null) {
                listener.setTaskName(pr.name)
                listener.setApkVariant(variant)
                listener.setPackageName(variant.applicationId)
            }
        }
    }
}
