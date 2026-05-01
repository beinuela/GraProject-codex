package com.campus.material.modules.log.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.material.common.PageQuery;
import com.campus.material.common.PageResult;
import com.campus.material.modules.log.entity.LoginLog;
import com.campus.material.modules.log.mapper.LoginLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LoginLogService {

    private final LoginLogMapper loginLogMapper;

    public LoginLogService(LoginLogMapper loginLogMapper) {
        this.loginLogMapper = loginLogMapper;
    }

    public PageResult<LoginLog> list(PageQuery pageQuery) {
        Page<LoginLog> page = loginLogMapper.selectPage(
                new Page<>(pageQuery.getPage(), pageQuery.getSize()),
                new LambdaQueryWrapper<LoginLog>().orderByDesc(LoginLog::getLoginTime)
        );
        return PageResult.from(page);
    }

    public void record(Long userId, String username, String ip, String status, String userAgent) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setLoginIp(ip);
        log.setLoginStatus(status);
        log.setLoginTime(LocalDateTime.now());
        log.setUserAgent(userAgent);
        loginLogMapper.insert(log);
    }
}
