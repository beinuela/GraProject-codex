package com.campus.material.modules.log.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.material.common.PageQuery;
import com.campus.material.common.PageResult;
import com.campus.material.modules.log.entity.OperationLog;
import com.campus.material.modules.log.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

@Service
public class OperationLogQueryService {

    private final OperationLogMapper operationLogMapper;

    public OperationLogQueryService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    public PageResult<OperationLog> list(PageQuery pageQuery, String keyword) {
        Page<OperationLog> page = operationLogMapper.selectPage(
                new Page<>(pageQuery.getPage(), pageQuery.getSize()),
                new LambdaQueryWrapper<OperationLog>()
                        .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                                .like(OperationLog::getModule, keyword)
                                .or()
                                .like(OperationLog::getOperation, keyword)
                                .or()
                                .like(OperationLog::getDetail, keyword))
                        .orderByDesc(OperationLog::getId)
        );
        return PageResult.from(page);
    }
}
