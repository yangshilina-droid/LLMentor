package com.lake.knowenginelearn.document.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lake.knowenginelearn.document.entity.KnowledgeSegment;
import com.lake.knowenginelearn.document.mapper.KnowledgeSegmentMapper;
import com.lake.knowenginelearn.document.service.KnowledgeSegmentService;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeSegmentServiceImpl
        extends ServiceImpl<KnowledgeSegmentMapper, KnowledgeSegment>
        implements KnowledgeSegmentService {
}
