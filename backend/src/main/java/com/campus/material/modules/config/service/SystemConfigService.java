package com.campus.material.modules.config.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.material.common.BizException;
import com.campus.material.modules.config.entity.SystemConfig;
import com.campus.material.modules.config.mapper.SystemConfigMapper;
import com.campus.material.modules.log.service.OperationLogService;
import com.campus.material.security.AuthUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SystemConfigService {

    private final SystemConfigMapper configMapper;
    private final OperationLogService logService;

    public SystemConfigService(SystemConfigMapper configMapper, OperationLogService logService) {
        this.configMapper = configMapper;
        this.logService = logService;
    }

    public List<SystemConfig> list(String group) {
        LambdaQueryWrapper<SystemConfig> qw = new LambdaQueryWrapper<>();
        if (group != null && !group.isBlank()) {
            qw.eq(SystemConfig::getConfigGroup, group);
        }
        return configMapper.selectList(qw.orderByAsc(SystemConfig::getConfigGroup).orderByAsc(SystemConfig::getId));
    }

    public String getValue(String key) {
        SystemConfig config = configMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key)
        );
        return config != null ? config.getConfigValue() : null;
    }

    public void save(SystemConfig config) {
        validateConfig(config);
        Long uid = AuthUtil.currentUserId();
        if (config.getId() == null) {
            configMapper.insert(config);
            logService.log(uid, "CONFIG", "CREATE", "閺傛澘顤冮柊宥囩枂:" + config.getConfigKey());
        } else {
            configMapper.updateById(config);
            logService.log(uid, "CONFIG", "UPDATE", "娣囶喗鏁奸柊宥囩枂:" + config.getConfigKey());
        }
    }

    public void delete(Long id) {
        configMapper.deleteById(id);
        logService.log(AuthUtil.currentUserId(), "CONFIG", "DELETE", "閸掔娀娅庨柊宥囩枂:" + id);
    }

    private void validateConfig(SystemConfig config) {
        if (config == null) {
            throw new BizException(400, "系统配置不能为空");
        }
        if (config.getConfigKey() == null || config.getConfigKey().isBlank()) {
            throw new BizException(400, "配置键不能为空");
        }
        if (config.getConfigName() == null || config.getConfigName().isBlank()) {
            throw new BizException(400, "配置名称不能为空");
        }
        if (config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            throw new BizException(400, "配置值不能为空");
        }
        if (config.getConfigGroup() == null || config.getConfigGroup().isBlank()) {
            throw new BizException(400, "配置分组不能为空");
        }

        String configKey = config.getConfigKey().trim();
        SystemConfig duplicated = configMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, configKey)
                .ne(config.getId() != null, SystemConfig::getId, config.getId())
                .last("limit 1"));
        if (duplicated != null) {
            throw new BizException(409, "配置键已存在，请勿重复添加");
        }

        config.setConfigKey(configKey);
        config.setConfigName(config.getConfigName().trim());
        config.setConfigValue(config.getConfigValue().trim());
        config.setConfigGroup(config.getConfigGroup().trim());
        if (config.getRemark() != null) {
            config.setRemark(config.getRemark().trim());
        }
    }
}
