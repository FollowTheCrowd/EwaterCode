package com.ewater.ecode.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 功能
 * 作者：八滴水
 * 日期： 2026/6/25 00:17
 */
public class Task {
    private final String id; //agent调用任务的唯一标识
    private final String description; //任务描述
    private final TaskType type;  //执行状态
    private TaskStatus status; //执行状态
    private String result; //执行结果
    private String error; //错误信息
    private final List<String> dependencies; //依赖的任务ID
    private final List<String> dependents; //被依赖的任务ID
    private long startTime; //开始时间
    private long endTime; //结束时间

    public Task(String id, String description, TaskType type) {
        this.id = id;
        this.description = description;
        this.type = type;
        this.status = TaskStatus.PENDING;
        this.dependencies = new ArrayList<>();
        this.dependents = new ArrayList<>();
    }


    public enum TaskType {
        PLANNING,  // 规划任务
        FILE_READ, // 读取文件
        FILE_WRITE, // 写入文件
        COMMAND, //  执行命令
        ANALYSIS, //分析结果
        VERIFICATION //验证结果
    }

    public enum TaskStatus {
        PENDING, //等待执行
        RUNNING, //执行中
        COMPLETED, //已完成
        FAILED, //执行失败
        SKIPPED, //被跳过
    }

    public void markStarted(){
        this.startTime = System.currentTimeMillis();
        this.status = TaskStatus.RUNNING;
    }

    public void markCompleted(String result){
        this.status = TaskStatus.COMPLETED;
        this.result = result;
        this.endTime = System.currentTimeMillis();
    }

    public void markFailed(String error){
        this.status = TaskStatus.FAILED;
        this.error = error;
        this.endTime = System.currentTimeMillis();
    }

    //Getters
    public String getId() { return id; }
    public String getDescription() { return description; }
    public TaskType getType() { return type; }
    public TaskStatus getStatus() { return status; }
    public String getResult() { return result; }
    public String getError() { return error; }
    public List<String> getDependencies() { return new ArrayList<>(dependencies); }
    public List<String> getDependents() { return new ArrayList<>(dependents); }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }

    //Setters
    public void setStatus(TaskStatus status) { this.status = status; }
    public void setResult(String result) { this.result = result; }
    public void setError(String error) { this.error = error; }

    public void addDependent(String taskId) {
        if (!dependents.contains(taskId)) {
            dependents.add(taskId);
        }
    }

    public void addDependency(String taskId) {
        if (!dependencies.contains(taskId)) {
            dependencies.add(taskId);
        }
    }


    public boolean isExecutable(Map<String,Task> AllTasks) {
        if(status != TaskStatus.PENDING) return false;
        for(String depId : dependencies){
            Task dep = AllTasks.get(depId);
            if(dep == null || dep.getStatus() != TaskStatus.COMPLETED) return false;
        }
        return true;
    }
}
