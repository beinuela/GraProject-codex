package com.campus.material.modules.campus.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.material.modules.campus.entity.Campus;
import com.campus.material.modules.campus.mapper.CampusMapper;
import com.campus.material.modules.log.service.OperationLogService;
import com.campus.material.security.AuthUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CampusService {

    private final CampusMapper campusMapper;
    private final OperationLogService logService;

    public CampusService(CampusMapper campusMapper, OperationLogService logService) {
        this.campusMapper = campusMapper;
        this.logService = logService;
    }

    public List<Campus> list() {
        return campusMapper.selectList(new LambdaQueryWrapper<Campus>().orderByAsc(Campus::getId));
    }

    public void save(Campus campus) {
        Long uid = AuthUtil.currentUserId();
        if (campus.getId() == null) {
            campusMapper.insert(campus);
            logService.log(uid, "CAMPUS", "CREATE", "閺傛澘顤冮弽鈥冲隘:" + campus.getCampusName());
        } else {
            campusMapper.updateById(campus);
            logService.log(uid, "CAMPUS", "UPDATE", "娣囶喗鏁奸弽鈥冲隘:" + campus.getId());
        }
    }

    public void delete(Long id) {
        campusMapper.deleteById(id);
        logService.log(AuthUtil.currentUserId(), "CAMPUS", "DELETE", "閸掔娀娅庨弽鈥冲隘:" + id);
    }
}
