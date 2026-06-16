package com.ewater.ecode.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 功能
 * 作者：八滴水
 * 日期： 2026/6/17 01:46
 */
public class GLMClient {
    private static final String API_URI =
            "https://open.bigmodel.cn/api/paas/v4";
    private static final String MODEL = "glm-5.1";
    private final String apiKey;
    private final OkHttpClient client;
    protected final static ObjectMapper mapper = new ObjectMapper();

    public GLMClient(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
    }

    public record Message(String role, String content,
                          List<ToolCall> toolCalls, String toolCallId){
        /** system:系统提示，定义Agent的身份和能力
         *  user:用户输入
         *  assistant:助手回复，可以包含文本或工具调用
         *  tool:工具执行结果
        **/
        public static Message system(String content){
            return new Message("system",content,null,null);
        }

        public static Message user(String content){
            return new Message("user",content,null,null);
        }
        public static Message tool(String content){
            return new Message("tool",content,null,null);
        }
        public static Message assistant(String content){
            return new Message("assistant",content,null,null);
        }

    }

    /**
     *
     * @param name : 工具名
     * @param description : 工具描述，用来让 AI 理解这个工具是干嘛的
     * @param parameters : 工具的参数结构（JSON Schema）
     */
    record Tool(String name, String description, JsonNode parameters) {}

    record ToolCall(String id, Function function) {
        /**
         * name:工具名
         * arguments:参数
         */
        public record Function(String name, String arguments) {}
    }


}
