package com.lake.knowenginelearn.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lake.knowenginelearn.document.constant.DocumentStatus;
import com.lake.knowenginelearn.document.constant.SegmentStatus;
import com.lake.knowenginelearn.document.entity.KnowledgeDocument;
import com.lake.knowenginelearn.document.entity.KnowledgeSegment;
import com.lake.knowenginelearn.document.mapper.KnowledgeDocumentMapper;
import com.lake.knowenginelearn.document.service.KnowledgeDocumentService;
import com.lake.knowenginelearn.document.service.KnowledgeSegmentService;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeDocumentServiceImpl
        extends ServiceImpl<KnowledgeDocumentMapper, KnowledgeDocument>
        implements KnowledgeDocumentService {

    @Autowired
    private KnowledgeSegmentService knowledgeSegmentService;

    @Autowired
    private ElasticsearchEmbeddingStore elasticsearchEmbeddingStore;

    @Autowired
    private OpenAiEmbeddingModel openAiEmbeddingModel;

    @Override
    public boolean embeddingAndStore(Long docId) {
        //todo 增加分布式锁
        KnowledgeDocument knowledgeDocument = getById(docId);
        if (knowledgeDocument == null) {
            return false;
        }

        if (knowledgeDocument.getStatus() == DocumentStatus.VECTOR_STORED) {
            return true;
        }

        //todo 状态的校验

        //分页扫描全部document_id为docId且status为INIT的文档片段
        LambdaQueryWrapper<KnowledgeSegment> queryWrapper = Wrappers.<KnowledgeSegment>lambdaQuery()
                .eq(KnowledgeSegment::getDocumentId, docId)
                .eq(KnowledgeSegment::getStatus, SegmentStatus.INIT)
                .isNull(KnowledgeSegment::getEmbeddingId)
                .eq(KnowledgeSegment::getSkipEmbedding, 0);

        Page<KnowledgeSegment> page = knowledgeSegmentService.page(new Page<>(1, 100), queryWrapper);

        while (page.getCurrent() == 1 || page.hasNext()) {
            List<KnowledgeSegment> textSegmentsToEmbed = page.getRecords();
            // 构造文本，带元数据
            List<TextSegment> textSegments = textSegmentsToEmbed.stream()
                    .map(segment -> TextSegment.from(segment.getText(), Metadata.from(segment.getMetadataMap())))
                    .toList();
            // 获取嵌入向量-向量模型
            Response<List<Embedding>> embeddingResponse = openAiEmbeddingModel.embedAll(textSegments);

            // 存储嵌入向量
            List<String> embeddingIds = elasticsearchEmbeddingStore.addAll(embeddingResponse.content(), textSegments);

            // 更新文档片段状态
            for (int i = 0; i < textSegmentsToEmbed.size(); i++) {
                String embeddingId = embeddingIds.get(i);
                KnowledgeSegment knowledgeSegment = textSegmentsToEmbed.get(i);
                knowledgeSegment.setEmbeddingId(embeddingId);
                knowledgeSegment.setStatus(SegmentStatus.VECTOR_STORED);
                knowledgeSegmentService.updateById(knowledgeSegment);
            }

            // 继续扫描下一页
            page = knowledgeSegmentService.page(new Page<>(page.getCurrent() + 1, 100), queryWrapper);
        }

        //todo 需要对所有的segment做检查，确保所有的segment都已转换为vector

        // 更新文档状态
        knowledgeDocument.setStatus(DocumentStatus.VECTOR_STORED);
        return updateById(knowledgeDocument);
    }
}
