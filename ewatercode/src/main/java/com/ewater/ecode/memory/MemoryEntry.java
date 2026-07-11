package com.ewater.ecode.memory;

import java.time.Instant;
import java.util.Map;

/**
 * 功能
 * 作者：八滴水
 * 日期： 2026/7/11 22:56
 */
public class MemoryEntry {
    private final String id;
    private final String content;
    private final MemoryType type;
    private final Instant timestamp;
    private final Map<String, String> metadata;
    private final int tokenCount;

    public enum MemoryType {
        CONVERSATION,  // 对话记忆
        FACT,          // 事实记忆（用户偏好、项目信息）
        SUMMARY,       // 摘要记忆
        TOOL_RESULT    // 工具执行结果
    }

    public MemoryEntry(String id, String content, MemoryType type, Map<String, String> metadata, int tokenCount) {
        this(id, content, type, Instant.now(), metadata, tokenCount);
    }

    public MemoryEntry(String id, String content, MemoryType type, Instant timestamp,
                       Map<String, String> metadata, int tokenCount) {
        this.id = id;
        this.content = content;
        this.type = type;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.metadata = metadata != null ? metadata : Map.of();
        this.tokenCount = tokenCount;
    }

    public String getId() { return id; }
    public String getContent() { return content; }
    public MemoryType getType() { return type; }
    public Instant getTimestamp() { return timestamp; }
    public Map<String, String> getMetadata() { return metadata; }
    public int getTokenCount() { return tokenCount; }


    //统计token
    public static int estimateToken(String text){
        if(text == null || text.isEmpty()) return 0;
        long chineseNumber = text.chars()
                .filter(c -> c> 0x4E00 && c < 0x9FFF).count();
        long othersNumber = text.length() - chineseNumber;
        return (int)Math.ceil(chineseNumber / 1.5 + othersNumber / 4.0);
    }
}
