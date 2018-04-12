package com.ymlion.gradle

import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.TaskState

public class TaskListener implements TaskExecutionListener {

    @Override
    void beforeExecute(Task task) {
        println("before ${task.name} execute.")
    }

    @Override
    void afterExecute(Task task, TaskState taskState) {
        println("after ${task.name} execute.")
    }
}