package com.campus.material.modules.log.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.material.modules.log.entity.OperationLog;
import com.campus.material.modules.log.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OperationLogQueryService {

    private final OperationLogMapper operationLogMapper;

    public OperationLogQueryService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    public List<OperationLog> list() {
        return operationLogMapper.selectList(new LambdaQueryWrapper<OperationLog>().orderByDesc(OperationLog::getId));
    }
}
