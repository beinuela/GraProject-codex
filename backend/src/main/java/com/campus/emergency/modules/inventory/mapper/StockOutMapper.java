package com.campus.emergency.modules.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.emergency.modules.inventory.entity.StockOut;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockOutMapper extends BaseMapper<StockOut> {
}
