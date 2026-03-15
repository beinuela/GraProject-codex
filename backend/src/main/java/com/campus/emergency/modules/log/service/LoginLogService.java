package com.campus.emergency.modules.log.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.emergency.modules.log.entity.LoginLog;
import com.campus.emergency.modules.log.mapper.LoginLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoginLogService {

    private final LoginLogMapper loginLogMapper;

    public LoginLogService(LoginLogMapper loginLogMapper) {
        this.loginLogMapper = loginLogMapper;
    }

    public List<LoginLog> list() {
        return loginLogMapper.selectList(
                new LambdaQueryWrapper<LoginLog>().orderByDesc(LoginLog::getLoginTime)
        );
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
