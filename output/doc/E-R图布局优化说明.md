# E-R图布局优化说明

- 工作底稿：`校园物资智能管理系统设计与实现-E-R图规范化版.docx`
- 备份文件：`校园物资智能管理系统设计与实现-E-R图规范化版-E-R图布局优化前备份-20260429104829.docx`
- 输出文件：`校园物资智能管理系统设计与实现-E-R图布局优化版.docx`

## 修改结果

- 共优化 `3` 张 E-R 图。

### 重点布局调整

- `图5-1 用户角色与组织 E-R 图`：已完成 中心实体与下游实体的辐射式重排。
- `图5-2 库存与批次 E-R 图`：已完成 主档-库存-批次的分层式重排。
- `图5-3 业务单据、预警与通知 E-R 图`：已完成 上下两个业务模块的分区式重排。

### 图题、图号与正文引用

- 图题与图号未新增也未改号，仍保持 `图5-1` 至 `图5-3` 连续编号。
- 正文引用校验结果：`已保留原有正确引用`。
- 本轮未补充新的图题、图号或正文引用文本。

### 排版优化

- 保持原有 Chen 风格不变，仅优化实体、属性、联系和基数标注的位置分布。
- 已重点压缩大面积空白，拉开局部拥挤区域，并减少连线交叉、穿越和绕路问题。
- 图片段落和图题段落结构保持原位，仅替换内嵌图片内容。

### 图源更新路径

- `output/doc/figures/fig_3_3_rbac_er.png`
- `output/doc/figures/fig_3_4_inventory_er.png`
- `output/doc/figures/fig_3_5_business_er.png`
- `output/doc/figures/drawio/fig_3_3_rbac_er.{yaml,spec.yaml,drawio,svg,arch.json}`
- `output/doc/figures/drawio/fig_3_4_inventory_er.{yaml,spec.yaml,drawio,svg,arch.json}`
- `output/doc/figures/drawio/fig_3_5_business_er.{yaml,spec.yaml,drawio,svg,arch.json}`

### 媒体替换校验

- `word/media/image7.png` -> `fig_3_3_rbac_er.png`，MD5=`054c4e1d12f7ebfc6ae9f215cd88a485`。
- `word/media/image8.png` -> `fig_3_4_inventory_er.png`，MD5=`965f9f0f3a4d4a6adf91beb9d1b6bc0a`。
- `word/media/image9.png` -> `fig_3_5_business_er.png`，MD5=`96244c497f3fa180c660220244e235eb`。

### 视觉复核提示

- 当前环境未提供 Word 页面级渲染工具，建议在本地 Word 中打开输出文件，对第 5 章分页、图内留白和长线段观感做一次最终视觉复核。