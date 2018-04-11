package com.ymlion.gradle.plugin

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.tasks.ProcessAndroidResources
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree

/**
 * Created by YMlion on 2018/4/11.*/
public class AaptPlugin implements Plugin<Project> {

    @Override public void apply(Project project) {
        def android = project.android

        android.applicationVariants.all { BaseVariant variant ->
            def pr = project.tasks["process${variant.name}Resources"]
            pr.doLast {
                modifyRes(it)
            }
        }
    }

    public void modifyRes(ProcessAndroidResources pr) {
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
