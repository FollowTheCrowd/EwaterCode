package com.ewater.ecode.plan;

import com.ewater.ecode.llm.GLMClient;
import com.ewater.ecode.llm.GLMClient.Message;
import com.ewater.ecode.llm.GLMClient.ChatResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 功能
 * 作者：八滴水
 * 日期： 2026/7/3 23:44
 */
public class Planner {
    private final GLMClient llmClient;


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

    private ExecutionPlan parsePlan(String goal, String content) {
    }

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
