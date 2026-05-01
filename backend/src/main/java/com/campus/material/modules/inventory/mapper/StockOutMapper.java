package com.campus.material.modules.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.material.modules.inventory.entity.StockOut;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockOutMapper extends BaseMapper<StockOut> {
}
