package com.ymlion.gradle

import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.android.build.gradle.internal.scope.TaskOutputHolder
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.build.gradle.tasks.ProcessAndroidResources
import com.android.sdklib.BuildToolInfo
import com.ymlion.parser.ArscFile
import com.ymlion.parser.XmlFile
import com.ymlion.parser.util.FileEditor
import groovy.io.FileType
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.TaskState

class ResourceListener implements TaskExecutionListener {

    Project project
    String taskName = ''
    ApplicationVariantImpl apkVariant

    ResourceListener(Project project) {
        this.project = project
    }

    @Override
    void beforeExecute(Task task) {
        if (taskName == task.name) {
            println("before ${task.name} execute.")
        }
    }

    @Override
    void afterExecute(Task task, TaskState taskState) {
        if (taskName == task.name) {
            println("after ${task.name} execute.")
            BaseVariantData variantData = apkVariant.variantData
            variantData.outputScope.getOutputs(TaskOutputHolder.TaskOutputType.PROCESSED_RES).each {
                hookAapt(task, it.outputFile)
            }
        }
    }

    /**
     * Hook aapt task to slice asset package and resolve library resource ids*/
    private def hookAapt(ProcessAndroidResources aaptTask, File apFile) {
        // Unpack resources.ap_
        // build/intermediates/res/debug/resources-debug.ap_
        def unzipApDir = new File(apFile.parentFile, "ap_unzip")
        unzipApDir.deleteDir()
        project.copy {
            from project.zipTree(apFile)
            into unzipApDir

            include 'AndroidManifest.xml'
            include 'resources.arsc'
            include 'res/**/*'
        }

        // Modify assets
        // build/intermediates/symbols/debug/R.txt
        File symbolFile = aaptTask.textSymbolOutputFile

        // build/generated/source/r/debug/
        File sourceOutputDir = aaptTask.sourceOutputDir
        def packagePath = apkVariant.applicationId.replaceAll('\\.', '/')
        File rJavaFile = new File(sourceOutputDir, "${packagePath}/R.java")
        def editor = new FileEditor()
        editor.resetRJava(symbolFile, this.project.aapt.packageId)
        editor.resetRJava(rJavaFile, this.project.aapt.packageId)

        String aaptExe = aaptTask.buildTools.getPath(BuildToolInfo.PathId.AAPT)
        def updatedResources = new HashSet()
        resetAllXmlPackageId(unzipApDir, this.project.aapt.packageId, updatedResources)

        // Re-add updated entries.
        // $ aapt add resources.ap_ file1 file2 ...
        def newApFile = new File(apFile.parentFile, "resources-debug-new.ap_")
        def nullOutput = new ByteArrayOutputStream()
        if (System.properties['os.name'].toLowerCase().contains('windows')) {
            // Avoid the command becomes too long to execute on Windows.
            updatedResources.each { res ->
                println res
                project.exec {
                    executable aaptExe
                    workingDir unzipApDir
                    args 'add', newApFile.path, res

                    standardOutput = nullOutput
                }
            }
        } else {
            project.exec {
                executable aaptExe
                workingDir unzipApDir
                args 'add', newApFile.path
                args updatedResources

                // store the output instead of printing to the console
                standardOutput = new ByteArrayOutputStream()
            }
        }
    }

    /** Reset package id for *.xml */
    private static void resetAllXmlPackageId(File dir, int pp, Set outUpdatedResources) {
        int len = dir.canonicalPath.length() + 1
        // bypass '/'
        def isWindows = (File.separator != '/')
        dir.eachFileRecurse(FileType.FILES) { file ->
            if (file.name.endsWith('.xml')) {
                new XmlFile(file).resetPackageId(pp)
            } else if (file.name.endsWith('.arsc')) {
                new ArscFile(file).resetPackageId(pp)
            }
            if (outUpdatedResources != null) {
                def path = file.canonicalPath.substring(len)
                if (isWindows) {
                    // compat for windows
                    path = path.replaceAll('\\\\', '/')
                }
                outUpdatedResources.add(path)
            }
        }
    }

}