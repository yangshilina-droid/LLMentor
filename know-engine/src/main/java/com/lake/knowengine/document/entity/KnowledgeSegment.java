package com.lake.knowengine.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("knowledge_segment")
public class KnowledgeSegment extends BaseEntity {

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
     * 所属文档版本ID（knowledge_document_version.version_id）
     */
    private Long documentVersion;

    /**
     * 顺序
     */
    private Integer chunkOrder;

    /**
     * 嵌入ID
     */
    private String embeddingId;

    /**
     * 状态：STORED, VECTOR_STORED
     */
    private String status;

    /**
     * 是否跳过嵌入生成
     */
    private Integer skipEmbedding;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 修改时间
     */
    private LocalDateTime updatedAt;

    /**
     * 乐观锁版本号
     */
    @Version
    private Integer lockVersion;

    /**
     * 是否删除：0-未删除，1-已删除
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
