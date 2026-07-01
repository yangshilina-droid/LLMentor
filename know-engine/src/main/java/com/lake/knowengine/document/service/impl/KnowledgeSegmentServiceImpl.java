package com.lake.knowengine.document.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lake.knowengine.document.entity.KnowledgeSegment;
import com.lake.knowengine.document.mapper.KnowledgeSegmentMapper;
import com.lake.knowengine.document.service.KnowledgeSegmentService;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeSegmentServiceImpl extends ServiceImpl<KnowledgeSegmentMapper, KnowledgeSegment>
        implements KnowledgeSegmentService {
}
