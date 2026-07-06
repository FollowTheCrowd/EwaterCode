package com.ewater.ecode.cli;

import com.ewater.ecode.agent.Agent;
import com.ewater.ecode.agent.PlanExecuteAgent;
import com.ewater.ecode.llm.GLMClient;
import com.ewater.ecode.tool.ToolRegistry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

/**
 * 功能
 * 作者：八滴水
 * 日期： 2026/6/19 03:10
 */
public class Main {
    public static void main(String[] args) throws IOException {
        System.out.println("工作目录: " + System.getProperty("user.dir"));


        //logo
        printBanner();

        String apiKey = loadApiKey();

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("❌ 错误: 未找到 GLM_API_KEY");
            System.exit(1);
        }

        //创立agent对象
        GLMClient llmClient = new GLMClient(apiKey);
        ToolRegistry toolRegistry = new ToolRegistry();
        Agent agent = new Agent(llmClient, toolRegistry);
        PlanExecuteAgent planExecuteAgent = new PlanExecuteAgent(llmClient, toolRegistry);

        Scanner scanner = new Scanner(System.in);
        System.out.println("提示: 输入 'clear' 清空历史, 'exit' 退出\n");

        while (true) {
            System.out.print("👤 你: ");
            String input = scanner.nextLine().trim();
            if(input.isEmpty()) {
                continue;
            }
            if (input.equalsIgnoreCase("exit")) break;
            if (input.equalsIgnoreCase("clear")) {
                agent.clearHistory();
                System.out.println("🗑️ 历史已清空\n");
                continue;
            }
            String response;
            if (shouldPlan(input)) {
                response = planExecuteAgent.run(input);
            } else {
                response = agent.run(input);           // 简单对话，原来的 ReAct Agent
            }
            System.out.println("🤖 Agent: " + response + "\n");
            System.out.println("🤖 Agent: " + response + "\n");


        }


    }
    private static void printBanner() {
        System.out.println("""
	 ███████╗ ██████╗ ██╗     ██╗
	 ██╔════╝██╔════╝ ██║     ██║
	 █████╗  ██║      ██║     ██║
	 ██╔══╝  ██║      ██║     ██╗
	 ███████╗╚██████╔ ███████╗██╗
	 ╚══════╝ ╚═════╝ ╚══════╝╚═╝
		    
		   Ewater Code 
        """);
    }
    private static String loadApiKey() {
        // 先尝试从当前目录读取 .env
        File envFile = new File(".env");
        if (envFile.exists()) {
            return readApiKeyFromFile("GLM_API_KEY");
        }

        // 再尝试从环境变量读取
        return System.getenv("GLM_API_KEY");
    }

    private static String readApiKeyFromFile(String key) {
        File file = new File(".env");  // 当前工作目录下的 .env
        if (!file.exists()) return null;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;  // 跳过空行和注释
                if (line.startsWith(key + "=")) {
                    return line.substring((key + "=").length()).trim();
                }
            }
        } catch (IOException e) {
            System.err.println("读取 .env 失败: " + e.getMessage());
        }
        return null;
    }
    private static boolean  shouldPlan(String input) {
        // 包含多个动作关键词或长度超过50字符，需要规划
        String[] keywords = {"创建", "写", "读", "执行", "然后", "接着"};
        int actionCount = 0;
        for (String keyword : keywords) {
            if (input.contains(keyword)) actionCount++;
        }
        return actionCount >= 3 || input.length() > 50;
    }

}
