package com.lake.knowenginelearn.document.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lake.knowenginelearn.document.entity.KnowledgeSegment;

import java.io.Serializable;

public interface KnowledgeSegmentService extends IService<KnowledgeSegment> {

    public String getTextByChunkId(Serializable chunkId);

}
