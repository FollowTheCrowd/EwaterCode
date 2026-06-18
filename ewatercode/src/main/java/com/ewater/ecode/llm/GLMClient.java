package com.ewater.ecode.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 功能
 * 作者：八滴水
 * 日期： 2026/6/17 01:46
 */
public class GLMClient {
    private static final String API_URI =
            "https://open.bigmodel.cn/api/paas/v4/chat/completions";
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
        public static Message tool(String content,String toolCallId){
            return new Message("tool",content,null,toolCallId);
        }
        public static Message assistant(String content){
            return new Message("assistant",content,null,null);
        }
        public static Message assistant(String content,List<ToolCall> toolCalls){
            return new Message("assistant",content,toolCalls,null);
        }

    }

    /**
     *
     * @param name : 工具名
     * @param description : 工具描述，用来让 AI 理解这个工具是干嘛的
     * @param parameters : 工具的参数结构（JSON Schema）
     */
    public record Tool(String name, String description, JsonNode parameters) {}

    public record ToolCall(String id, Function function) {
        /**
         * name:工具名
         * arguments:参数
         */
        public record Function(String name, String arguments) {}
    }

    /**
     *
     * @param role :角色
     * @param content : 正文文本
     * @param reasoningContent : 思考内容
     * @param toolCalls : 调用的工具列表
     * @param inputTokens : 输入token
     * @param outputTokens : 输出token
     * @param cachedInputTokens : 命中缓存的token
     */

    public record ChatResponse(String role,
                        String content,
                        String reasoningContent,
                        List<ToolCall> toolCalls,
                        int inputTokens,
                        int outputTokens,
                        int cachedInputTokens
                        ){
        public ChatResponse(String role, String content, List<ToolCall> toolCalls,
                         int inputTokens, int outputTokens){
            this(role,content,null,toolCalls,inputTokens,outputTokens,0);
        }
        public ChatResponse(String role, String content, String reasoningContent, List<ToolCall> toolCalls,
                            int inputTokens, int outputTokens) {
            this(role, content, reasoningContent, toolCalls, inputTokens, outputTokens, 0);
        }

        public boolean hasToolCalls(){
            return toolCalls != null && !toolCalls.isEmpty();
        }

    }

    public ChatResponse chat(List<Message> messages,List<Tool> tools) throws IOException {
        //构建请求体
        ObjectNode requestBody = mapper.createObjectNode();
        requestBody.put("model",MODEL);

        //添加历史消息
        ArrayNode messageArray = requestBody.putArray("messages");
        for(Message message : messages) {
            ObjectNode msgNode = messageArray.addObject()
                    .put("role", message.role)
                    .put("content", message.content);
            //如果有工具调用
            if (message.toolCalls() != null && !message.toolCalls.isEmpty()) {
                ArrayNode toolCallsArray = msgNode.putArray("tool_calls");
                for (ToolCall toolCall : message.toolCalls) {
                    ObjectNode toolCallNode = toolCallsArray.addObject()
                            .put("id", toolCall.id)
                            .put("type", "function");
                    ObjectNode functionNode = toolCallNode.putObject("function");
                    functionNode.put("name", toolCall.function().name());
                    functionNode.put("arguments", toolCall.function().arguments());

                }
            }
            //如果是工具结果，添加tool_call_id
            if (message.toolCallId() != null) {
                msgNode.put("tool_call_id", message.toolCallId());
            }
        }

            //添加工具定义
             if(tools!=null && !tools.isEmpty()){
                 ArrayNode toolsArray = requestBody.putArray("tools");
                 for(Tool tool : tools) {
                     ObjectNode toolNode = toolsArray.addObject()
                             .put("type", "function");
                     ObjectNode functionNode = toolNode.putObject("function");
                     functionNode.put("name", tool.name);
                     functionNode.put("description", tool.description);
                     functionNode.put("parameters", tool.parameters);
                 }

             }
             //test
        System.out.println("请求体: " + requestBody.toString());

             //发送Http请求
        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(API_URI)
                .header("Authorization","Bearer "+ apiKey)
                .post(body)
                .build();

        //解析响应
        try(Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "";
                throw new IOException("API 请求失败: " + response.code() + " " + err);
            }
            String responseBody = response.body().string();
            JsonNode root = mapper.readTree(responseBody);

            //取choice的msg
            JsonNode msg = root.path("choices").get(0).path("message");
            //取得content
            String content = msg.path("content").asText("");
            //取reasoning
            String reasoning = msg.path("reasoning").asText("");

            //工具调用
            List<ToolCall> toolCalls = new ArrayList<>();
            JsonNode tcArray = msg.path("tool_calls");
            for(JsonNode tc : tcArray) {
                String id = tc.path("id").asText();
                String name = tc.path("function").path("name").asText();
                String arguments = tc.path("function").path("arguments").asText();
                toolCalls.add(new ToolCall(id,new ToolCall.Function(name,arguments)));
            }
            // token 用量
            JsonNode usage = root.path("usage");
            int in = usage.path("prompt_tokens").asInt(0);
            int out = usage.path("completion_tokens").asInt(0);

            return new ChatResponse("assistant", content, reasoning, toolCalls, in, out);


        }


    }



}
