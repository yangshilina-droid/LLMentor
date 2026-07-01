package com.lake.knowengine.document.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lake.knowengine.document.constant.KnowledgeBaseType;
import com.lake.knowengine.document.entity.KnowledgeDocument;
import com.lake.knowengine.document.mapper.KnowledgeDocumentMapper;
import com.lake.knowengine.document.mapper.KnowledgeDocumentVersionMapper;
import com.lake.knowengine.document.mapper.KnowledgeSegmentMapper;
import com.lake.knowengine.document.service.KnowledgeDocumentService;
import com.lake.knowengine.document.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KnowledgeDocumentServiceImpl extends ServiceImpl<KnowledgeDocumentMapper, KnowledgeDocument>
        implements KnowledgeDocumentService {

    @Autowired
    private KnowledgeSegmentMapper knowledgeSegmentMapper;

    @Autowired
    private KnowledgeDocumentVersionMapper knowledgeDocumentVersionMapper;

    @Autowired
    private ExcelProcessServiceImpl excelProcessServiceImpl;

    @Autowired
    private VectorStoreService vectorStoreService;

    /**
     * 删除文档，并级联物理删除该文档下的所有分段和版本，同时按 docId 清除向量存储中的数据
     *
     * @param docId 文档ID
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeDocumentWithSegments(Long docId) {
        // 按 metadata 中的 docId 删除该文档所有向量
        deleteVectorsByDocId(docId);

        // 物理删除该文档下的所有分段
        knowledgeSegmentMapper.physicalDeleteByDocumentId(docId);

        // 删除该文档对应的 DATA_QUERY 动态物理表
        dropDataQueryTableIfExists(docId);

        // 物理删除该文档的所有版本记录
        knowledgeDocumentVersionMapper.physicalDeleteByDocId(docId);

        // 物理删除文档本身
        return baseMapper.physicalDeleteByDocId(docId) > 0;
    }

    /**
     * 按 metadata 中的 docId 删除该文档所有向量
     */
    private void deleteVectorsByDocId(Long docId) {
        vectorStoreService.removeByDocId(docId);
    }

    /**
     * 如果文档是 DATA_QUERY 类型且配置了表名，则删除对应的动态物理表及元数据
     */
    private void dropDataQueryTableIfExists(Long docId) {
        KnowledgeDocument document = this.getById(docId);
        if (document == null
                || document.getKnowledgeBaseType() != KnowledgeBaseType.DATA_QUERY
                || document.getTableName() == null
                || document.getTableName().isBlank()) {
            return;
        }
        String physicalTableName = excelProcessServiceImpl.generatePhysicalTableName(document.getTableName());
        excelProcessServiceImpl.dropTable(physicalTableName);
    }


}
