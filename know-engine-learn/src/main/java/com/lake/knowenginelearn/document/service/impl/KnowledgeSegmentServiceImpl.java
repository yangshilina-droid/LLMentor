package com.lake.knowenginelearn.document.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lake.knowenginelearn.document.entity.KnowledgeSegment;
import com.lake.knowenginelearn.document.mapper.KnowledgeSegmentMapper;
import com.lake.knowenginelearn.document.service.KnowledgeSegmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.Serializable;

@Service
public class KnowledgeSegmentServiceImpl
        extends ServiceImpl<KnowledgeSegmentMapper, KnowledgeSegment>
        implements KnowledgeSegmentService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public String getTextByChunkId(Serializable chunkId) {
        //todo
        String text = stringRedisTemplate.opsForValue().get(chunkId);
        if (StringUtils.hasText(text)) {
            return text;
        }

        KnowledgeSegment segment = super.getById(chunkId);

        if (segment != null) {
            stringRedisTemplate.opsForValue().set(chunkId.toString(), segment.getText());
            return segment.getText();
        } else {
            // 缓存空值，避免缓存击穿，重复查询数据库
            stringRedisTemplate.opsForValue().set(chunkId.toString(), "");
        }

        return null;
    }

}
