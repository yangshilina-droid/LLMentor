package com.lake.knowenginelearn.document.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lake.knowenginelearn.document.entity.TableMeta;
import com.lake.knowenginelearn.document.mapper.TableMetaMapper;
import com.lake.knowenginelearn.document.service.KnowEngineTableMetaService;
import org.springframework.stereotype.Service;

/**
 * 知识片段表 Service 实现类
 */
@Service
public class KnowEngineTableMetaServiceImpl extends ServiceImpl<TableMetaMapper, TableMeta> implements
        KnowEngineTableMetaService {
}