package com.ewater.ecode.plan;

import com.ewater.ecode.llm.GLMClient;
import com.ewater.ecode.llm.GLMClient.Message;
import com.ewater.ecode.llm.GLMClient.ChatResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static java.lang.System.out;

/**
 * 功能
 * 作者：八滴水
 * 日期： 2026/7/3 23:44
 */
public class Planner {
    private final GLMClient llmClient;
    protected final static ObjectMapper mapper = new ObjectMapper();

    public Planner(GLMClient llmClient) {
        this.llmClient = llmClient;
    }

    public ExecutionPlan createPlan(String goal) throws IOException {
        // 构建规划提示
        List<Message> messages = Arrays.asList(
                Message.system(PLANNING_PROMPT),
                Message.user("请为以下任务制定执行计划：\n" + goal)
        );
        //调用LLM
        ChatResponse response = llmClient.chat(messages,null);

        return  parsePlan(goal,response.content());

    }

    private ExecutionPlan parsePlan(String goal, String planJson) throws IOException {
        //清理markdown代码块
        //此处正则表达式为ai生成，后续需学习
        String cleaned = planJson.replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

        JsonNode root = mapper.readTree(cleaned);
        String summary = root.path("summary").asText();
        JsonNode taskNodes = root.path("tasks");
        ExecutionPlan plan = new ExecutionPlan(generateId(),goal);
        plan.setSummary(summary);

        //第一次不需要设置依赖，防止前向循环，只需要把全部task添加就行
        int taskIndex =1;
        HashMap<String,String> idMap = new HashMap<>();

        for(JsonNode taskNode : taskNodes) {
            String originId = taskNode.path("id").asText();
            String newId = "task_" + taskIndex++;
            idMap.put(originId,newId);
            String description = taskNode.path("description").asText();
            String typeStr = taskNode.path("type").asText();
            Task.TaskType type = parseTaskType(typeStr);
            plan.addTask(new Task(newId,description,type));
        }

        //第二次循环，设置依赖关系
        taskIndex = 1;
        for(JsonNode taskNode : taskNodes) {
            String newId = "task_" + taskIndex++;
            Task task = plan.getTask(newId);

            JsonNode depNodes = taskNode.path("dependencies");
            if(depNodes.isArray()){
                for(JsonNode depNode : depNodes) {
                    String depId = depNode.asText();
                    String depNewId = idMap.getOrDefault(depId,depId);
                    Task dep = plan.getTask(depNewId);
                    if(dep != null){
                        task.addDependency(depNewId);
                        dep.addDependent(task.getId());
                    }

                }

            }


        }

        //计算执行顺序
        if(!plan.computeExecutionOrder()){
            throw new IOException("计划存在依赖循环");
        }
        return plan;
    }

    /**
     *执行失败重写规划执行
     */
    public ExecutionPlan replan(ExecutionPlan failedPlan, String failureReason) throws IOException {
        out.println("🔄 重新规划，原因: " + failureReason + "\n");

        StringBuilder context = new StringBuilder();
        context.append("原任务: ").append(failedPlan.getGoal()).append('\n');
        context.append("失败原因: ").append(failureReason).append('\n');
        context.append("已完成任务:\n");

        for(Task task:failedPlan.getAllTasks()){
            if(task.getStatus() == Task.TaskStatus.COMPLETED){
                context.append("- ").append(task.getId())
                        .append(": ").append(task.getDescription())
                        .append("\n");

            }
        }
        context.append("\n制定新的计划，避开之前的问题");
        return  createPlan(context.toString());

    }

    /**
     * 解析任务类型
     */
    private Task.TaskType parseTaskType(String typeStr) {
        return switch (typeStr.toUpperCase()) {
            case "FILE_READ" -> Task.TaskType.FILE_READ;
            case "FILE_WRITE" -> Task.TaskType.FILE_WRITE;
            case "COMMAND" -> Task.TaskType.COMMAND;
            case "ANALYSIS" -> Task.TaskType.ANALYSIS;
            case "VERIFICATION" -> Task.TaskType.VERIFICATION;
            default -> Task.TaskType.ANALYSIS;
        };
    }

    public String generateId(){return "task_" + System.currentTimeMillis();}

    //提示词
    private static final String PLANNING_PROMPT = """
    你是一个任务规划专家。请将用户的复杂任务分解为一系列可执行的子任务。

    可用任务类型：
    - FILE_READ: 读取文件内容
    - FILE_WRITE: 写入文件内容
    - COMMAND: 执行 Shell 命令
    - ANALYSIS: 分析结果并做出决策
    - VERIFICATION: 验证结果是否正确

    请按以下 JSON 格式输出执行计划：
    {
      "summary": "任务摘要",
      "tasks": [
        {
          "id": "task_1",
          "description": "任务描述",
          "type": "FILE_READ",
          "dependencies": []
        }
      ]
    }

    规则：
    1. 每个任务必须有唯一 id，如 task_1、task_2。
    2. dependencies 列出依赖的任务 id。
    3. 任务应该按执行顺序排列。
    4. 任务描述要具体明确。
    5. 简单任务只生成 1-3 个任务。
    6. 复杂任务拆分为 5-10 个子任务。

    只输出 JSON，不要有其他内容。
    """;


}
