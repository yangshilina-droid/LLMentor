package com.lake.knowenginelearn.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lake.knowenginelearn.document.constant.SegmentStatus;
import lombok.Data;

/**
 * 知识片段表
 */
@Data
@TableName("knowledge_segment")
public class KnowledgeSegment {

    /**
     * 片段ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文本内容
     */
    private String text;

    /**
     * 分片ID
     */
    private String chunkId;

    /**
     * 元数据
     */
    private String metadata;

    /**
     * 所属文档ID
     */
    private Long documentId;

    /**
     * 顺序
     */
    private Integer chunkOrder;

    /**
     * 嵌入ID
     */
    private String embeddingId;

    /**
     * 状态：INIT, VECTOR_STORED
     */
    private SegmentStatus status;

    /**
     * 是否跳过嵌入生成
     */
    private Integer skipEmbedding;
}
