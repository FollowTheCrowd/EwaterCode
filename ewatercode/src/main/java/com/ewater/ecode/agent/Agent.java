package com.ewater.ecode.agent;

import com.ewater.ecode.llm.GLMClient;
import com.ewater.ecode.tool.ToolRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * 功能
 * 作者：八滴水
 * 日期： 2026/6/17 23:07
 */
public class Agent {
    private final GLMClient llmClient;
    private final ToolRegistry toolRegistry;
    private final List<GLMClient.Message> conversationHistory;
    private  static  final  int MAX_ITERATIONS = 10;
    private static final String SYSTEM_PROMPT = """
    你是一个智能编程助手，可以帮助用户完成各种任务。

    你可以使用以下工具来完成任务：
    1. read_file - 读取文件内容
    2. write_file - 写入文件内容
    3. list_dir - 列出目录内容
    4. execute_command - 执行Shell命令
    5. create_project - 创建新项目结构

    当需要操作文件、执行命令或创建项目时，请使用工具调用。
    使用工具后，根据工具返回的结果继续思考下一步行动。

    请用中文回复用户。
    """;

    public Agent(GLMClient llmClient, ToolRegistry toolRegistry) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        conversationHistory = new ArrayList<GLMClient.Message>();

        //系统提示
        conversationHistory.add(GLMClient.Message.system(SYSTEM_PROMPT))
    }
}
