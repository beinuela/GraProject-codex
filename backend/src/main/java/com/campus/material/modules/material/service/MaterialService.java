package com.campus.material.modules.material.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.material.common.BizException;
import com.campus.material.modules.log.service.OperationLogService;
import com.campus.material.modules.material.entity.MaterialCategory;
import com.campus.material.modules.material.entity.MaterialInfo;
import com.campus.material.modules.material.mapper.MaterialCategoryMapper;
import com.campus.material.modules.material.mapper.MaterialInfoMapper;
import com.campus.material.security.AuthUtil;
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
        validateCategory(category);
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
        validateMaterial(info);
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

    private void validateCategory(MaterialCategory category) {
        if (category.getCategoryName() == null || category.getCategoryName().isBlank()) {
            throw new BizException(400, "分类名称不能为空");
        }
        String categoryName = category.getCategoryName().trim();
        MaterialCategory duplicated = categoryMapper.selectOne(new LambdaQueryWrapper<MaterialCategory>()
                .eq(MaterialCategory::getCategoryName, categoryName)
                .ne(category.getId() != null, MaterialCategory::getId, category.getId())
                .last("limit 1"));
        if (duplicated != null) {
            throw new BizException(409, "分类名称已存在，请勿重复添加");
        }
        category.setCategoryName(categoryName);
    }

    private void validateMaterial(MaterialInfo info) {
        if (info.getMaterialCode() == null || info.getMaterialCode().isBlank()) {
            throw new BizException(400, "物资编码不能为空");
        }
        String materialCode = info.getMaterialCode().trim();
        MaterialInfo duplicated = materialInfoMapper.selectOne(new LambdaQueryWrapper<MaterialInfo>()
                .eq(MaterialInfo::getMaterialCode, materialCode)
                .ne(info.getId() != null, MaterialInfo::getId, info.getId())
                .last("limit 1"));
        if (duplicated != null) {
            throw new BizException(409, "物资编码已存在，请勿重复添加");
        }
        info.setMaterialCode(materialCode);
        if (info.getMaterialName() != null) {
            info.setMaterialName(info.getMaterialName().trim());
        }
    }
}
