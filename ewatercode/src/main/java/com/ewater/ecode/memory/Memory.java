package com.ewater.ecode.memory;

import java.util.List;
import java.util.Optional;

public interface Memory {

    //存一条记忆
    void store(MemoryEntry entry);

    //通过ID检索记忆
    Optional<MemoryEntry> retrieve(String id);

    //搜索相关记忆
    List<MemoryEntry> search(String query,int limit);

    //获取所有记忆
    List<MemoryEntry> getAll();

    //删除指定记忆
    boolean delete(String id);

    //清空记忆
    void clear();

    //获取当前的token总数
    int getTokenCount();

    //获取记忆条数
    int size();

}
