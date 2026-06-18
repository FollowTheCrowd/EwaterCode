package com.ewater.ecode.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 功能
 * 作者：八滴水
 * 日期： 2026/6/17 20:39
 */
public class ToolRegistry {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final Map<String,Tool> tools=new HashMap<String,Tool>();
    public ToolRegistry(){
        registerFileTools();
        registerShellTools();
        registerCodeTools();
    }

    private void registerFileTools(){
        //read_file工具
        tools.put("read_file",new Tool(
                "read_file",
                "读取文件内容以查看代码、配置文件等",
                createParameters(new Param("Path","String","文件路径",true)),
                args -> {
                    String Path = args.get("Path");
                    try {
                        String content = Files.readString(Paths.get(Path));
                        return "文件内容为:" + content;
                    }catch (Exception e){
                        return "文件读取失败"+ e.getMessage();
                    }
                }
                ));
        tools.put("write_file",new Tool(
                "write_file",
                "写入文件内容",
                createParameters(
                        new Param("Path","String","文件路径",true),
                        new Param("content","String","文件内容",true)
                ),
                args -> {
                    String path = args.get("Path");
                    String content = args.get("content");
                    Files.writeString(Path.of(path),content);
                    return "文件已写入: " + path;
                }
        ));
    }

    private void registerShellTools(){
        tools.put("execute_command",new Tool(
                "execute_command",
                "执行Shell命令，可用于编译运行等",
                createParameters(new Param("command","String","要执行的命令",true)),
                args -> {
                    String command = args.get("command");
                    try {
                        ProcessBuilder pb = new ProcessBuilder("bash","-c",command );
                        // 报错也合并到输出里
                        pb.redirectErrorStream(true);
                        Process process = pb.start();

                        //读取命令输出
                        StringBuilder output = new StringBuilder();
                        try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                output.append(line).append("\n");
                            }
                        }
                        int exitCode = process.waitFor();
                        return String.format("命令执行完成(exit code: %d)\n%s",exitCode,output);
                    } catch (IOException e) {
                        return "执行命令失败: " + e.getMessage();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
        ));
    }

    private void registerCodeTools(){

    }

    //参数定义方法
    private JsonNode createParameters(Param... params){
        ObjectNode parameters=mapper.createObjectNode();
        parameters.put("type","Object");
        ObjectNode properties = parameters.putObject("properties");
        ArrayNode required = parameters.putArray("required");
        for(Param param:params){
            ObjectNode prop = properties.putObject(param.name);
            prop.put("type","String");
            prop.put("description",param.description);
            if(param.required){
                required.add(prop);
            }

        }
        return parameters;

    }

    private record Param(
            String name,
            String type,
            String description,
            boolean required
    ){}

    public record Tool(
        String name,
        String description,
        JsonNode parameters,
        ToolExecutor executor){

    }
    public interface ToolExecutor{
        String execute(Map<String,String> args) throws IOException;
    }

}