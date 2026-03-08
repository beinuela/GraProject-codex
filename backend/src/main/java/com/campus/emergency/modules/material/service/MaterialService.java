package com.campus.emergency.modules.material.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.emergency.modules.log.service.OperationLogService;
import com.campus.emergency.modules.material.entity.MaterialCategory;
import com.campus.emergency.modules.material.entity.MaterialInfo;
import com.campus.emergency.modules.material.mapper.MaterialCategoryMapper;
import com.campus.emergency.modules.material.mapper.MaterialInfoMapper;
import com.campus.emergency.security.AuthUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MaterialService {

    private final MaterialCategoryMapper categoryMapper;
    private final MaterialInfoMapper materialInfoMapper;
    private final OperationLogService operationLogService;

    public MaterialService(MaterialCategoryMapper categoryMapper, MaterialInfoMapper materialInfoMapper, OperationLogService operationLogService) {
        this.categoryMapper = categoryMapper;
        this.materialInfoMapper = materialInfoMapper;
        this.operationLogService = operationLogService;
    }

    public List<MaterialCategory> listCategory() {
        return categoryMapper.selectList(new LambdaQueryWrapper<MaterialCategory>().orderByAsc(MaterialCategory::getId));
    }

    public MaterialCategory saveCategory(MaterialCategory category) {
        if (category.getId() == null) {
            categoryMapper.insert(category);
            operationLogService.log(AuthUtil.currentUserId(), "MATERIAL", "CREATE_CATEGORY", category.getCategoryName());
        } else {
            categoryMapper.updateById(category);
            operationLogService.log(AuthUtil.currentUserId(), "MATERIAL", "UPDATE_CATEGORY", category.getCategoryName());
        }
        return categoryMapper.selectById(category.getId());
    }

    public void deleteCategory(Long id) {
        categoryMapper.deleteById(id);
        operationLogService.log(AuthUtil.currentUserId(), "MATERIAL", "DELETE_CATEGORY", String.valueOf(id));
    }

    public List<MaterialInfo> listMaterial(String keyword) {
        LambdaQueryWrapper<MaterialInfo> wrapper = new LambdaQueryWrapper<MaterialInfo>().orderByDesc(MaterialInfo::getId);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(MaterialInfo::getMaterialName, keyword).or().like(MaterialInfo::getMaterialCode, keyword);
        }
        return materialInfoMapper.selectList(wrapper);
    }

    public MaterialInfo saveMaterial(MaterialInfo info) {
        if (info.getId() == null) {
            materialInfoMapper.insert(info);
            operationLogService.log(AuthUtil.currentUserId(), "MATERIAL", "CREATE_MATERIAL", info.getMaterialName());
        } else {
            materialInfoMapper.updateById(info);
            operationLogService.log(AuthUtil.currentUserId(), "MATERIAL", "UPDATE_MATERIAL", info.getMaterialName());
        }
        return materialInfoMapper.selectById(info.getId());
    }

    public void deleteMaterial(Long id) {
        materialInfoMapper.deleteById(id);
        operationLogService.log(AuthUtil.currentUserId(), "MATERIAL", "DELETE_MATERIAL", String.valueOf(id));
    }
}
