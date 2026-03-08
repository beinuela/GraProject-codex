package com.campus.emergency.modules.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.emergency.modules.log.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
