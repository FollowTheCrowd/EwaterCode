package com.ewater.ecode.agent;

import com.ewater.ecode.llm.GLMClient;
import com.ewater.ecode.llm.GLMClient.Message;
import com.ewater.ecode.llm.GLMClient.ChatResponse;
import com.ewater.ecode.llm.GLMClient.ToolCall;
import com.ewater.ecode.plan.ExecutionPlan;
import com.ewater.ecode.plan.Planner;
import com.ewater.ecode.plan.Task;
import com.ewater.ecode.tool.ToolRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Plan-and-Execute Agent —— 先规划后执行
 * 作者：八滴水
 * 日期：2026/7/6 22:55
 */
public class PlanExecuteAgent {
    private final GLMClient llmClient;
    private final ToolRegistry toolRegistry;
    private final Planner planner;
    private static final int MAX_TASK_ITERATIONS = 5;

    private static final String TASK_PROMPT = """
        你是一个任务执行专家，需要完成一个具体的子任务。

        系统会告诉你：
        - 总目标是什么
        - 当前任务是什么
        - 依赖任务的结果是什么

        你可以使用工具读取文件、写入文件、执行命令。

        完成了当前任务后，请输出任务执行结果。如果是简单任务，直接给出结果；如果需要操作文件或执行命令，使用工具调用。

        请用中文回复。
        """;

    // --- 构造函数 ---

    public PlanExecuteAgent(GLMClient llmClient, ToolRegistry toolRegistry) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.planner = new Planner(llmClient);
    }

    // --- 主流程 ---

    public String run(String userInput) throws IOException {
        // 1. 创建计划
        ExecutionPlan plan = planner.createPlan(userInput);

        // 2. 展示计划
        System.out.println("📋 计划摘要: " + (plan.getSummary() != null ? plan.getSummary() : "(无)"));
        System.out.println("📋 任务数: " + plan.getAllTasks().size() + "\n");

        // 3. 计算执行顺序
        if (!plan.computeExecutionOrder()) {
            return "❌ 计划中存在循环依赖，无法执行。";
        }

        // 4. 执行循环
        return executePlan(plan);
    }

    private String executePlan(ExecutionPlan plan) throws IOException {
        plan.markStarted();
        StringBuilder finalResult = new StringBuilder();

        while (true) {
            // 拿到所有"依赖都已完成"的任务，按拓扑序排列
            List<Task> ready = getExecutableTasksInOrder(plan);
            if (ready.isEmpty()) {
                break;  // 没有可执行的任务了
            }

            for (Task task : ready) {
                System.out.println("▶️  执行任务 [" + task.getId() + "]: " + task.getDescription());
                task.markStarted();

                try {
                    String result = executeTask(plan.getGoal(), plan, task);
                    task.markCompleted(result);
                    System.out.println("✅ 完成 [" + task.getId() + "]\n");
                } catch (Exception e) {
                    task.markFailed(e.getMessage());
                    System.err.println("❌ 失败 [" + task.getId() + "]: " + e.getMessage());

                    // 进度不到一半就重规划
                    if (plan.getProgress() < 0.5) {
                        System.out.println("🔄 尝试重新规划...\n");
                        ExecutionPlan replanned = planner.replan(plan, e.getMessage());
                        plan = replanned;
                        break;  // 跳出当前 for，重新用新计划执行
                    }

                    if (finalResult.length() > 0) finalResult.append("\n");
                    finalResult.append("任务 ").append(task.getId()).append(" 失败: ").append(e.getMessage());
                }
            }

            // 如果计划已全部完成，退出
            if (plan.isAllCompleted()) break;
            // 如果有失败且无法继续，退出
            if (plan.hasFailed() && plan.getExecutableTasks().isEmpty()) break;
        }

        // 5. 汇总结果
        if (plan.isAllCompleted()) {
            plan.markCompleted();
            String summary = buildResult(plan);
            return "✅ 计划执行完成！\n" + (summary.isEmpty() ? "" : summary);
        } else if (plan.hasFailed()) {
            plan.markFailed();
            return "⚠️ 计划部分完成，有任务失败。\n" + finalResult;
        }
        return "⚠️ 计划未能完成，存在未执行的任务。";
    }

    // --- 单任务执行（迷你 ReAct 循环） ---

    private String executeTask(String goal, ExecutionPlan plan, Task task) throws IOException {
        // 构建这个任务的上下文（含依赖结果）
        String taskInput = buildTaskContext(goal, plan, task);

        // 对话历史（仅限这个任务内）
        List<Message> messages = new ArrayList<>();
        messages.add(Message.system(TASK_PROMPT));
        messages.add(Message.user(taskInput));

        StringBuilder allResults = new StringBuilder();
        int iteration = 0;

        while (iteration < MAX_TASK_ITERATIONS) {
            iteration++;

            ChatResponse response = llmClient.chat(messages, toolRegistry.getToolDefinitions());

            if (!response.hasToolCalls()) {
                // 无工具调用 → 任务结束，返回 LLM 的文本结果
                String content = response.content() != null ? response.content() : "";
                if (!content.isBlank()) return content;
                if (allResults.length() > 0) return allResults.toString().trim();
                return "任务完成（无输出）";
            }

            // 有工具调用 → 执行并继续
            messages.add(Message.assistant(response.content(), response.toolCalls()));

            for (ToolCall tc : response.toolCalls()) {
                String result = toolRegistry.executeTool(tc.function().name(), tc.function().arguments());
                messages.add(Message.tool(result, tc.id()));
                allResults.append(result).append("\n");
            }
        }
        // 达到最大迭代次数
        return allResults.length() > 0 ? allResults.toString().trim() : "达到最大迭代次数";
    }

    // --- 构建任务上下文 ---

    private String buildTaskContext(String goal, ExecutionPlan plan, Task task) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("总目标：").append(goal).append("\n");
        ctx.append("当前任务：").append(task.getDescription()).append("\n");

        if (task.getDependencies().isEmpty()) {
            ctx.append("依赖任务：无\n");
        } else {
            ctx.append("依赖任务结果：\n");
            for (String depId : task.getDependencies()) {
                Task dep = plan.getTask(depId);
                if (dep == null) continue;
                ctx.append("- ").append(dep.getId())
                        .append(": ").append(dep.getDescription())
                        .append("\n");
                if (dep.getResult() != null && !dep.getResult().isBlank()) {
                    ctx.append("  结果: ").append(dep.getResult()).append("\n");
                }
            }
        }
        ctx.append("\n请执行此任务并给出结果。");
        return ctx.toString();
    }

    // --- 汇总最终结果 ---

    private String buildResult(ExecutionPlan plan) {
        List<Task> leafTasks = plan.getAllTasks().stream()
                .filter(t -> t.getDependents().isEmpty())  // 取叶子节点
                .toList();

        StringBuilder result = new StringBuilder();
        for (Task task : leafTasks) {
            if (task.getResult() == null || task.getResult().isBlank()) continue;
            if (result.length() > 0) result.append("\n");
            result.append("[").append(task.getId()).append("] ").append(task.getResult());
        }
        return result.toString();
    }

    // --- 获取按拓扑序排列的可执行任务 ---

    private List<Task> getExecutableTasksInOrder(ExecutionPlan plan) {
        Set<String> executableIds = plan.getExecutableTasks().stream()
                .map(Task::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return plan.getExecutionOrder().stream()
                .filter(executableIds::contains)
                .map(plan::getTask)
                .toList();
    }
}
