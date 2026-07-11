package com.ewater.ecode.memory;
import com.huaban.analysis.jieba.JiebaSegmenter;
import com.ewater.ecode.util.JiebaSegmenterFactory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 功能
 * 作者：八滴水
 * 日期： 2026/7/12 01:25
 */
public class MemoryQueryTokenizer {
    private static final JiebaSegmenter SEGMENTER = JiebaSegmenterFactory.createSilently();

    private MemoryQueryTokenizer(){
    }

    /**
     * 对查询文本进行分词
     */
    static Set<String> tokenize(String query){
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        if(query==null || query.isBlank()){
            return tokens;
        }
        List<String> words = SEGMENTER.sentenceProcess(query.toLowerCase(Locale.ROOT).trim());
        for(String word : words){
            String trimmed = word.trim();
            //过滤单字符和标点
            if(trimmed.length()>=2 && !isPunctuation(trimmed)){
                tokens.add(trimmed);
            }

        }
        return tokens;
    }

    /**
     * 检查文本中是否包含任意一个 query token（子串匹配）。
     */
    static boolean matches(String text, Set<String> queryTokens){
        if(text == null || text.isBlank() || queryTokens == null || queryTokens.isEmpty()){
            return false;
        }
        String normalizedText = text.toLowerCase(Locale.ROOT);
        for(String queryToken : queryTokens){
            if(normalizedText.contains(queryToken)){
                return true;
            }
        }
        return false;
    }




    private static boolean isPunctuation(String s) {
        return s.codePoints().allMatch(cp ->
                !Character.isLetterOrDigit(cp) && Character.UnicodeScript.of(cp) != Character.UnicodeScript.HAN);
    }
}
