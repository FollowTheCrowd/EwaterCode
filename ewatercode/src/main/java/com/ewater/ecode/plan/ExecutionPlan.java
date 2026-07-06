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


    /**
     * 可视化计划
     */
    public String visualize() {
        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════════════════════════╗\n");
        sb.append(String.format("║  执行计划: %-46s║%n", goal.length() > 46 ? goal.substring(0, 43) + "..." : goal));
        sb.append("╠══════════════════════════════════════════════════════════╣\n");

        List<String> order = getExecutionOrder();
        for (int i = 0; i < order.size(); i++) {
            String taskId = order.get(i);
            Task task = tasks.get(taskId);
            String statusIcon = getStatusIcon(task.getStatus());
            String deps = task.getDependencies().isEmpty() ? "无" :
                    String.join(",", task.getDependencies());

            sb.append(String.format("║  %d. %s %-20s ", i + 1, statusIcon, task.getId()));
            sb.append(String.format("[%-10s] 依赖: %-15s║%n",
                    task.getType(), deps));
            String desc = task.getDescription().length() > 50
                    ? task.getDescription().substring(0, 47) + "..."
                    : task.getDescription();
            sb.append(String.format("║     %-53s║%n", desc));
        }

        sb.append("╚══════════════════════════════════════════════════════════╝\n");
        sb.append(String.format("   进度: %.0f%% | 状态: %s%n",
                getProgress() * 100, status));

        return sb.toString();
    }

    private String getStatusIcon(Task.TaskStatus status) {
        return switch (status) {
            case PENDING -> "⏳";
            case RUNNING -> "▶️";
            case COMPLETED -> "✅";
            case FAILED -> "❌";
            case SKIPPED -> "⏭️";
        };
    }

    /**
     * 获取执行进度
     */
    public double getProgress() {
        if (tasks.isEmpty()) return 1.0;
        long completed = tasks.values().stream()
                .filter(t -> t.getStatus() == Task.TaskStatus.COMPLETED)
                .count();
        return (double) completed / tasks.size();
    }

    /**
     * 是否全部完成
     */
    public boolean isAllCompleted() {
        return tasks.values().stream()
                .allMatch(t -> t.getStatus() == Task.TaskStatus.COMPLETED);
    }

    /**
     * 是否有失败任务
     */
    public boolean hasFailed() {
        return tasks.values().stream()
                .anyMatch(t -> t.getStatus() == Task.TaskStatus.FAILED);
    }

    /**
     * 标记开始执行
     */
    public void markStarted() {
        this.status = PlanStatus.RUNNING;
        this.startTime = System.currentTimeMillis();
    }

    /**
     * 标记完成
     */
    public void markCompleted() {
        this.status = PlanStatus.COMPLETED;
        this.endTime = System.currentTimeMillis();
    }

    /**
     * 标记失败
     */
    public void markFailed() {
        this.status = PlanStatus.FAILED;
        this.endTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return String.format("ExecutionPlan[%s: %s] (%d tasks, %s)",
                id, goal, tasks.size(), status);
    }


    /**
     * 获取执行顺序
     */
    public List<String> getExecutionOrder() {
        if (executionOrder.isEmpty()) {
            computeExecutionOrder();
        }
        return new ArrayList<>(executionOrder);
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
            if(depTask != null){
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
