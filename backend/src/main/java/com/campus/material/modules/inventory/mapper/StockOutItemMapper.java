package com.campus.material.modules.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.material.modules.inventory.entity.StockOutItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockOutItemMapper extends BaseMapper<StockOutItem> {
}
