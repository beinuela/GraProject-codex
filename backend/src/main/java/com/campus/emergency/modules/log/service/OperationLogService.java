package com.campus.emergency.modules.log.service;

import com.campus.emergency.modules.log.entity.OperationLog;
import com.campus.emergency.modules.log.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

@Service
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    public OperationLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    public void log(Long operatorId, String module, String operation, String detail) {
        OperationLog record = new OperationLog();
        record.setOperatorId(operatorId);
        record.setModule(module);
        record.setOperation(operation);
        record.setDetail(detail);
        operationLogMapper.insert(record);
    }
}
