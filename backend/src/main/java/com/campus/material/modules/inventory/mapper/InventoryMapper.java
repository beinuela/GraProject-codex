package com.campus.material.modules.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.material.modules.inventory.entity.Inventory;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    @Update("""
            UPDATE inventory
            SET locked_qty = locked_qty + #{quantity},
                version = version + 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{inventoryId}
              AND deleted = 0
              AND version = #{version}
              AND current_qty - locked_qty >= #{quantity}
            """)
    int reserveLockedQty(@Param("inventoryId") Long inventoryId,
                         @Param("quantity") int quantity,
                         @Param("version") int version);

    @Update("""
            UPDATE inventory
            SET locked_qty = locked_qty - #{quantity},
                version = version + 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{inventoryId}
              AND deleted = 0
              AND version = #{version}
              AND locked_qty >= #{quantity}
            """)
    int releaseLockedQty(@Param("inventoryId") Long inventoryId,
                         @Param("quantity") int quantity,
                         @Param("version") int version);
}
