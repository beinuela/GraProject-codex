package com.campus.material.modules.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.material.modules.auth.entity.AuthRefreshToken;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthRefreshTokenMapper extends BaseMapper<AuthRefreshToken> {
}
