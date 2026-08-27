package com.lake.knowenginelearn.rag.modules;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.aggregator.ContentAggregator;
import dev.langchain4j.rag.query.Query;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 带进度通知的内容聚合器
 * 本质是带进度通知的代理，它自己不执行内容排序，而是在真实聚合器前后插入进度通知
 *
 * <p>
 * 在委托执行 {@link ContentAggregator#aggregate(Map)} 前后发送进度通知，
 * 用于流式返回前端当前处理阶段，减少用户等待焦虑。
 * <p>
 * 进度通知顺序：
 * <ol>
 *   <li>聚合前：{@code [PROGRESS]:正在排序筛选结果...}</li>
 *   <li>聚合后：{@code [PROGRESS]:正在生成回答...}（聚合完成后即将进入LLM生成阶段）</li>
 * </ol>
 *
 * @see ContentAggregator
 */
public class ProgressAwareContentAggregator implements ContentAggregator {

    // 被代理对象
    private final ContentAggregator delegate;
    private final Consumer<String> progressCallback;

    public ProgressAwareContentAggregator(ContentAggregator delegate, Consumer<String> progressCallback) {
        this.delegate = delegate;
        this.progressCallback = progressCallback;
    }

    @Override
    public List<Content> aggregate(Map<Query, Collection<List<Content>>> queryToContents) {
        // 发送进度：开始重排序/聚合
        if (progressCallback != null) {
            progressCallback.accept("[PROGRESS]:正在排序筛选结果...");
            System.out.println("[PROGRESS]:正在排序筛选结果...");
        }

        List<Content> result = delegate.aggregate(queryToContents);

        // 发送进度：聚合完成，即将进入LLM生成
        if (progressCallback != null) {
            progressCallback.accept("[PROGRESS]:正在生成回答...");
            System.out.println("[PROGRESS]:正在生成回答...");
        }

        return result;
    }
}

