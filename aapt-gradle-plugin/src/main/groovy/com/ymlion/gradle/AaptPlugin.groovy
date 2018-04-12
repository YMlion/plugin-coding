package com.ymlion.gradle.plugin

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.tasks.ProcessAndroidResources
import com.ymlion.gradle.TaskListener
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree

/**
 * Created by YMlion on 2018/4/11.*/
class AaptPlugin implements Plugin<Project> {

    @Override void apply(Project project) {

        project.gradle.addListener(new TaskListener())

        def android = project.android

        android.applicationVariants.all { BaseVariant variant ->
            def pr = project.tasks["process${variant.name}Resources"]
            pr.doLast {
                //                modifyRes(it)
                println("do the task!!!!!!!!!")
            }
        }
    }

    void modifyRes(ProcessAndroidResources pr) {
        // Unpack resources.ap_
        File apFile = pr.packageOutputFile
        FileTree apFiles = project.zipTree(apFile)
        File unzipApDir = new File(apFile.parentFile, 'ap_unzip')
        unzipApDir.delete()
        project.copy {
            from apFiles
            into unzipApDir

            include 'AndroidManifest.xml'
            include 'resources.arsc'
            include 'res/**/*'
        }
    }
}
