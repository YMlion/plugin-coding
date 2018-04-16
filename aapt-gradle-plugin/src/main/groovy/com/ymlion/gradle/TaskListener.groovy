package com.ymlion.gradle

import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.android.build.gradle.internal.scope.TaskOutputHolder
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.build.gradle.tasks.ProcessAndroidResources
import com.android.sdklib.BuildToolInfo
import com.ymlion.gradle.aapt.Aapt
import com.ymlion.gradle.aapt.SymbolParser
import groovy.io.FileType
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.TaskState

public class TaskListener implements TaskExecutionListener {
    private static final int UNSET_TYPEID = 99
    private static final int UNSET_ENTRYID = -1

    Project project
    String taskName = ''
    ApplicationVariantImpl apkVariant

    LinkedHashMap<Integer, Integer> idMaps
    LinkedHashMap<String, String> idStrMaps
    ArrayList retainedTypes
    ArrayList retainedStyleables
    /** List of all resource types */
    ArrayList allTypes

    /** List of all resource styleables */
    ArrayList allStyleables

    TaskListener(Project project) {
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
        prepareSplit(symbolFile)
        // build/generated/source/r/debug/
        File sourceOutputDir = aaptTask.sourceOutputDir
        def packagePath = apkVariant.applicationId.replaceAll('\\.', '/')
        File rJavaFile = new File(sourceOutputDir, "${packagePath}/R.java")
        def rev = project.android.buildToolsRevision
        def filteredResources = new HashSet()
        def updatedResources = new HashSet()

        Aapt aapt = new Aapt(unzipApDir, rJavaFile, symbolFile, rev)
        if (this.retainedTypes != null && this.retainedTypes.size() > 0) {
            aapt.filterResources(this.retainedTypes, filteredResources)
            println "[${project.name}] split library res files..."

            aapt.filterPackage(this.retainedTypes, this.project.aapt.packageId, this.idMaps, null,
                this.retainedStyleables, updatedResources)

            println "[${project.name}] slice asset package and reset package id..."

            String pkg = apkVariant.applicationId
            // Overwrite the aapt-generated R.java with full edition
            aapt.generateRJava(rJavaFile, pkg, this.allTypes, this.allStyleables)

            // Overwrite the retained vendor R.java
            def retainedRFiles = [rJavaFile]

            // Remove unused R.java to fix the reference of shared library resource, issue #63
            sourceOutputDir.eachFileRecurse(FileType.FILES) { file ->
                if (!retainedRFiles.contains(file)) {
                    file.delete()
                }
            }

            println "[${project.name}] split library R.java files..."
        } else {
            if (sourceOutputDir.deleteDir()) {
                println "[${project.name}] remove R.java..."
            }

            symbolFile.delete() // also delete the generated R.txt
        }

        String aaptExe = aaptTask.buildTools.getPath(BuildToolInfo.PathId.AAPT)

        // Delete filtered entries.
        // Cause there is no `aapt update' command supported, so for the updated resources
        // we also delete first and run `aapt add' later.
        filteredResources.addAll(updatedResources)
        ZipUtils.with(apFile).deleteAll(filteredResources)

        // Re-add updated entries.
        // $ aapt add resources.ap_ file1 file2 ...
        def nullOutput = new ByteArrayOutputStream()
        if (System.properties['os.name'].toLowerCase().contains('windows')) {
            // Avoid the command becomes too long to execute on Windows.
            updatedResources.each { res ->
                project.exec {
                    executable aaptExe
                    workingDir unzipApDir
                    args 'add', apFile.path, res

                    standardOutput = nullOutput
                }
            }
        } else {
            project.exec {
                executable aaptExe
                workingDir unzipApDir
                args 'add', apFile.path
                args updatedResources

                // store the output instead of printing to the console
                standardOutput = new ByteArrayOutputStream()
            }
        }
    }

    /**
     * Prepare retained resource types and resource id maps for package slicing*/
    protected void prepareSplit(File symbolFile) {
        def idsFile = symbolFile
        if (!idsFile.exists()) return

        def bundleEntries = SymbolParser.getResourceEntries(idsFile)
        def staticIdMaps = [:]
        def staticIdStrMaps = [:]
        def retainedEntries = []
        def retainedPublicEntries = []
        def retainedStyleables = []

        bundleEntries.each { k, Map be ->
            be._typeId = UNSET_TYPEID // for sort
            be._entryId = UNSET_ENTRYID
            be.isStyleable ? retainedStyleables.add(be) : retainedEntries.add(be)
        }

        if (retainedEntries.size() == 0 && retainedPublicEntries.size() == 0) {
            this.retainedTypes = [] // Doesn't have any resources
            return
        }

        // Prepare public types
        def publicTypes = [:]
        def maxPublicTypeId = 0
        def unusedTypeIds = [] as Queue
        if (retainedPublicEntries.size() > 0) {
            retainedPublicEntries.each { e ->
                def typeId = e._typeId
                def entryId = e._entryId
                def type = publicTypes[e.type]
                if (type == null) {
                    publicTypes[e.type] = [id      : typeId, maxEntryId: entryId,
                                           entryIds: [entryId], unusedEntryIds: [] as Queue]
                    maxPublicTypeId = Math.max(typeId, maxPublicTypeId)
                } else {
                    type.maxEntryId = Math.max(entryId, type.maxEntryId)
                    type.entryIds.add(entryId)
                }
            }
            if (maxPublicTypeId != publicTypes.size()) {
                for (int i = 1; i < maxPublicTypeId; i++) {
                    if (publicTypes.find { k, t -> t.id == i } == null) unusedTypeIds.add(i)
                }
            }
            publicTypes.each { k, t ->
                if (t.maxEntryId != t.entryIds.size()) {
                    for (int i = 0; i < t.maxEntryId; i++) {
                        if (!t.entryIds.contains(i)) t.unusedEntryIds.add(i)
                    }
                }
            }
        }

        // First sort with origin(full) resources order
        retainedEntries.sort { a, b -> a.typeId <=> b.typeId ?: a.entryId <=> b.entryId
        }

        // Reassign resource type id (_typeId) and entry id (_entryId)
        def lastEntryIds = [:]
        if (retainedEntries.size() > 0) {
            if (retainedEntries[0].type != 'attr') {
                // reserved for `attr'
                if (maxPublicTypeId == 0) maxPublicTypeId = 1
                if (unusedTypeIds.size() > 0) unusedTypeIds.poll()
            }
            def selfTypes = [:]
            retainedEntries.each { e ->
                // Check if the type has been declared in public.txt
                def type = publicTypes[e.type]
                if (type != null) {
                    e._typeId = type.id
                    if (type.unusedEntryIds.size() > 0) {
                        e._entryId = type.unusedEntryIds.poll()
                    } else {
                        e._entryId = ++type.maxEntryId
                    }
                    return
                }
                // Assign new type with unused type id
                type = selfTypes[e.type]
                if (type != null) {
                    e._typeId = type.id
                } else {
                    if (unusedTypeIds.size() > 0) {
                        e._typeId = unusedTypeIds.poll()
                    } else {
                        e._typeId = ++maxPublicTypeId
                    }
                    selfTypes[e.type] = [id: e._typeId]
                }
                // Simply increase the entry id
                def entryId = lastEntryIds[e.type]
                if (entryId == null) {
                    entryId = 0
                } else {
                    entryId++
                }
                e._entryId = lastEntryIds[e.type] = entryId
            }

            retainedEntries += retainedPublicEntries
        } else {
            retainedEntries = retainedPublicEntries
        }

        // Resort with reassigned resources order
        retainedEntries.sort { a, b -> a._typeId <=> b._typeId ?: a._entryId <=> b._entryId
        }

        // Resort retained resources
        def retainedTypes = []
        def pid = (this.project.aapt.packageId << 24)
        def currType = null
        retainedEntries.each { e ->
            // Prepare entry id maps for resolving resources.arsc and binary xml files
            if (currType == null || currType.name != e.type) {
                // New type
                currType = [type: e.vtype, name: e.type, id: e.typeId, _id: e._typeId, entries: []]
                retainedTypes.add(currType)
            }
            def newResId = pid | (e._typeId << 16) | e._entryId
            def newResIdStr = "0x${Integer.toHexString(newResId)}"
            staticIdMaps.put(e.id, newResId)
            staticIdStrMaps.put(e.idStr, newResIdStr)

            // Prepare styleable id maps for resolving R.java
            if (retainedStyleables.size() > 0 && e.typeId == 1) {
                retainedStyleables.findAll { it.idStrs != null }.each {
                    // Replace `e.idStr' with `newResIdStr'
                    def index = it.idStrs.indexOf(e.idStr)
                    if (index >= 0) {
                        it.idStrs[index] = newResIdStr
                        it.mapped = true
                    }
                }
            }

            def entry = [name: e.key, id: e.entryId, _id: e._entryId, v: e.id, _v: newResId,
                         vs  : e.idStr, _vs: newResIdStr]
            currType.entries.add(entry)
        }

        // Update the id array for styleables
        retainedStyleables.findAll { it.mapped != null }.each {
            it.idStr = "{ ${it.idStrs.join(', ')} }"
            it.idStrs = null
        }

        // Collect all the resources for generating a temporary full edition R.java
        // which required in javac.
        // TODO: Do this only for the modules who's code really use R.xx of lib.*
        def allTypes = []
        def allStyleables = []
        def addedTypes = [:]
        retainedTypes.each { t ->
            def at = addedTypes[t.name]
            if (at != null) {
                at.entries.addAll(t.entries)
            } else {
                allTypes.add(t)
            }
        }
        allStyleables.addAll(retainedStyleables)

        this.idMaps = staticIdMaps
        this.idStrMaps = staticIdStrMaps
        this.retainedTypes = retainedTypes
        this.retainedStyleables = retainedStyleables

        this.allTypes = allTypes
        this.allStyleables = allStyleables
    }
}