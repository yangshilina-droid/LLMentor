package com.lake.knowengine.document.service.impl;

import com.lake.knowengine.document.constant.FileType;
import com.lake.knowengine.document.constant.KnowledgeBaseType;
import com.lake.knowengine.document.entity.KnowledgeDocument;
import com.lake.knowengine.document.mapper.TableMetaMapper;
import com.lake.knowengine.document.service.FileProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.util.regex.Pattern;

/**
 * Excel处理服务实现类
 */
@Slf4j
@Service
public class ExcelProcessServiceImpl implements FileProcessService {

    @Autowired
    private TableMetaMapper tableMetaMapper;
    

    // 表名前缀
    private static final String TABLE_PREFIX = "custom_data_query_";
    // 有效的表名正则表达式
    private static final Pattern VALID_TABLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");


    /**
     * 根据逻辑表名生成物理表名
     * <p>
     * 同一逻辑表在所有版本中复用同一个物理表名。
     */
    public String generatePhysicalTableName(String originalFilename) {
        String baseName = originalFilename;
        // 去掉扩展名
        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = baseName.substring(0, dotIndex);
        }
        // 清理非法字符
        baseName = sanitizeTableName(baseName);
        // 限制 baseName 长度，确保加上前缀后不超过 MySQL 表名上限 64
        int maxBaseLength = 64 - TABLE_PREFIX.length();
        if (baseName.length() > maxBaseLength) {
            baseName = baseName.substring(0, maxBaseLength);
        }
        baseName = baseName.replaceAll("_+$", "");
        if (baseName.isEmpty()) {
            baseName = "table";
        }
        return TABLE_PREFIX + baseName;
    }

    /**
     * 清理表名，确保符合MySQL命名规范
     */
    private String sanitizeTableName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "table_" + System.currentTimeMillis();
        }

        // 转换为小写
        String sanitized = name.toLowerCase();
        // 替换非法字符为下划线
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9_]", "_");
        // 确保以字母或下划线开头
        if (!sanitized.matches("^[a-zA-Z_].*")) {
            sanitized = "t_" + sanitized;
        }
        // 限制长度（MySQL表名最大64字符）
        if (sanitized.length() > 60) {
            sanitized = sanitized.substring(0, 60);
        }
        // 去掉末尾的下划线
        sanitized = sanitized.replaceAll("_+$", "");

        return sanitized;
    }

    @Autowired
    protected TransactionTemplate transactionTemplate;

    @Override
    public String processDocument(KnowledgeDocument document, InputStream inputStream) {
        return null;
    }

    @Override
    public boolean supports(FileType fileType, KnowledgeBaseType knowledgeBaseType) {
        return knowledgeBaseType == KnowledgeBaseType.DATA_QUERY
                && (fileType == FileType.XLS || fileType == FileType.XLSX || fileType == FileType.CSV);
    }

    public void dropTable(String tableName) {
        // 安全检查
        if (!isValidTableName(tableName)) {
            throw new IllegalArgumentException("无效的表名: " + tableName);
        }

        transactionTemplate.executeWithoutResult((status) -> {
            // 1. 删除物理表
            tableMetaMapper.dropTable(tableName);
            log.info("物理表 {} 删除成功", tableName);

            // 2. 物理删除元数据记录（BaseEntity 开启了逻辑删除，必须绕过 @TableLogic）
            tableMetaMapper.physicalDeleteByTableName(tableName);
            log.info("表 {} 的元数据删除成功", tableName);
        });
    }

    /**
     * 验证表名是否有效
     */
    private boolean isValidTableName(String tableName) {
        return tableName != null && VALID_TABLE_NAME_PATTERN.matcher(tableName).matches();
    }

}
