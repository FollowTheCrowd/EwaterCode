package com.ewater.ecode.memory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 功能
 * 作者：八滴水
 * 日期： 2026/7/11 23:36
 */
public class ConversationMemory  implements Memory {
    private final LinkedHashMap<String,MemoryEntry> entries;
    private  int maxTokens;
    private  int currentTokens;
    private final List<MemoryEntry> compressedSummaries;

    /**
     * @param maxTokens 最大 token 预算，超出时触发压缩
     */
    public ConversationMemory(int maxTokens) {
        this.entries = new LinkedHashMap<>();
        this.maxTokens = maxTokens;
        this.currentTokens = 0;
        this.compressedSummaries = new ArrayList<>();
    }

    @Override
    public void store(MemoryEntry entry) {
        entries.put(entry.getId(), entry);
        currentTokens += entry.getTokenCount();

        //超预算则自动淘汰最旧的条目
        while(currentTokens >= maxTokens && entries.size() > 1) {
            evictOldest();
        }

    }

    @Override
    public Optional<MemoryEntry> retrieve(String id) {
        return Optional.ofNullable(entries.get(id));
    }

    @Override
    public List<MemoryEntry> search(String query, int limit){
        Set<String> queryTokens = MemoryQueryTokenizer.tokenize(query);
        return entries.values().stream()
                .filter(entry -> MemoryQueryTokenizer.matches(entry.getContent(),queryTokens))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<MemoryEntry> getAll() {
        return new ArrayList<>(entries.values());
    }

    @Override
    public boolean delete(String id) {
        MemoryEntry removed = entries.remove(id);
        if (removed != null) {
            currentTokens -= removed.getTokenCount();
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        entries.clear();
        currentTokens = 0;
        compressedSummaries.clear();
    }

    @Override
    public int getTokenCount() {
        return currentTokens;
    }

    @Override
    public int size() {
        return entries.size();
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be positive");
        }
        this.maxTokens = maxTokens;
        while (currentTokens > maxTokens && entries.size() > 1) {
            evictOldest();
        }
    }


    /**
     * 淘汰最旧的记忆，并且加入压缩摘要
     */
    private void evictOldest() {
        if(!entries.isEmpty()) {
            String firstKey = entries.keySet().iterator().next();
            MemoryEntry oldest = entries.get(firstKey);
            entries.remove(firstKey);
            currentTokens -= oldest.getTokenCount();
            compressedSummaries.add(oldest);
        }

    }

}
