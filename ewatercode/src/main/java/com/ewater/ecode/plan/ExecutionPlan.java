package com.ewater.ecode.plan;

import java.util.*;

/**
 * 功能
 * 作者：八滴水
 * 日期： 2026/6/26 00:16
 */
public class ExecutionPlan {
    private final String id;
    private final String goal;  //计划目标
    private final Map<String,Task> tasks; //全部任务
    private final List<String> executionOrder; //执行顺序
    private PlanStatus status;
    private String summary;
    private long startTime;
    private long endTime;



    public enum PlanStatus {
        CREATED,   //创建
        RUNNING, //执行
        COMPLETED, //全部完成
        FAILED, //存在任务失败
        CANCELLED //被取消
    }

    public ExecutionPlan(String id, String goal) {
        this.id = id;
        this.goal = goal;
        this.tasks = new LinkedHashMap<>();  // 保持插入顺序
        this.executionOrder = new ArrayList<>();
        this.status = PlanStatus.CREATED;
    }
    // Getters
    public String getId() { return id; }
    public String getGoal() { return goal; }
    public PlanStatus getStatus() { return status; }
    public String getSummary() { return summary; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }

    public void setSummary(String summary) { this.summary = summary; }
    public void setStatus(PlanStatus status) { this.status = status; }

    /**
     * 添加任务
     */
    public void  addTask(Task task) {
        tasks.put(task.getId(), task);
        //更新依赖
        for(String depID : task.getDependencies()){
            Task depTask = tasks.get(depID);
            if(depTask != null){
                depTask.addDependent(task.getId());
            }
        }
    }

    /**
     * 获取任务
     */
    public  Task getTask(String taskId) {
        return tasks.get(taskId);
    }

    /**
     * 获取全部任务
     */
    public Collection<Task> getAllTasks() {
        return tasks.values();
    }

    /**
     * 获取无依赖任务
     */
    public List<Task> getRootTasks() {
        return tasks.values().stream()
                .filter(t -> t.getDependencies().isEmpty())
                .toList();
    }

    /**
     * 获取可执行任务(依赖已经完成)
     */
    public List<Task> getExecutableTasks() {
        return tasks.values().stream()
                .filter(t -> t.isExecutable(tasks))
                .toList();
    }

    //DAG任务执行
    public boolean computeExecutionOrder() {
        executionOrder.clear();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();

        for(Task task : tasks.values()){
            if(!visited.contains(task.getId())){
                if(!topologicalSort(task,visited,visiting)){
                    return false;
                }
            }
        }
        return true;
    }

    private boolean topologicalSort(Task task,Set<String> visited,Set<String> visiting){
        //看是否有环
        if(visiting.contains(task.getId())) return false;

        //看是否已经完成了
        if (visited.contains(task.getId())) return true;

        visiting.add(task.getId());

        //开始遍历处理依赖
        for(String depID : task.getDependencies()){
            Task depTask = tasks.get(depID);
            if(!visited.contains(depID)){
                if(!topologicalSort(depTask,visited,visiting)){
                    return false;
                }
            }
        }
        visiting.remove(task.getId());
        visited.add(task.getId());
        executionOrder.add(task.getId());
        return true;
    }



}
