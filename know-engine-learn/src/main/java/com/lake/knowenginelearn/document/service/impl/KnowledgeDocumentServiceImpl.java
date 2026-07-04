package com.lake.knowenginelearn.document.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lake.knowenginelearn.document.entity.KnowledgeDocument;
import com.lake.knowenginelearn.document.mapper.KnowledgeDocumentMapper;
import com.lake.knowenginelearn.document.service.KnowledgeDocumentService;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeDocumentServiceImpl
        extends ServiceImpl<KnowledgeDocumentMapper, KnowledgeDocument>
        implements KnowledgeDocumentService {
}
