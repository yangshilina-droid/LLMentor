package com.lake.knowengine.document.entity;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lake.knowengine.document.constant.DocumentStatus;
import com.lake.knowengine.document.constant.KnowledgeBaseType;
import com.lake.knowengine.document.util.DocumentPermissionUtils;
import com.lake.knowengine.rag.constant.RoleEnum;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@TableName("knowledge_document")
public class KnowledgeDocument extends BaseEntity {

    /**
     * 文档ID
     */
    @TableId(value = "doc_id", type = IdType.AUTO)
    private Long docId;

    /**
     * 文档标题
     */
    private String docTitle;

    /**
     * 文档失效日期
     */
    private LocalDate expireDate;

    /**
     * 状态：INIT, UPLOADED, CONVERTING, CONVERTED, CHUNKED, VECTOR_STORED
     */
    private DocumentStatus status;

    /**
     * 可见范围
     */
    private String accessibleBy;

    /**
     * 文档描述
     */
    private String description;

    /**
     * 知识库类型：DOCUMENT_SEARCH, DATA_QUERY
     */
    private KnowledgeBaseType knowledgeBaseType;

    /**
     * 扩展字段，保存JSON字符串
     */
    private String extension;

    /**
     * 当前激活版本ID，指向 knowledge_document_version.version_id
     */
    private Long currentVersionId;

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

    public KnowledgeDocument create(DocumentUploadParam documentUploadParam) {
        this.setDocTitle(documentUploadParam.title());
        this.setStatus(DocumentStatus.UPLOADED);
        this.setDescription(documentUploadParam.description());
        this.setKnowledgeBaseType(KnowledgeBaseType.valueOf(documentUploadParam.knowledgeBaseType()));
        this.setTableName(documentUploadParam.tableName());
        this.setAccessibleBy(DocumentPermissionUtils.getDocumentPermission(RoleEnum.valueOf(documentUploadParam.accessibleBy())));
        return this;
    }

    @JsonIgnore
    public String getTableName() {
        if (extension != null && !extension.isEmpty()) {
            return (String) JSON.parseObject(extension, Map.class).get("tableName");
        }
        return null;
    }

    @JsonIgnore
    public void setTableName(String tableName) {
        Map<String, Serializable> extensionMap;
        if (extension == null) {
            extensionMap = new HashMap<String, Serializable>();
        } else {
            extensionMap = JSON.parseObject(extension, Map.class);
        }
        extensionMap.put("tableName", tableName);
        this.extension = JSON.toJSONString(extensionMap);
    }

}
