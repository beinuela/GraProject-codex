# E-R 图严格布局优化与中文化修改说明

- 工作底稿：`校园物资智能管理系统设计与实现-E-R图中文规范优化版.docx`
- 备份文件：`校园物资智能管理系统设计与实现-E-R图中文规范优化版-人工精修前备份-20260429175136.docx`
- 输出文件：`校园物资智能管理系统设计与实现-E-R图人工精修版.docx`

## 修改结果

- 共检查 `3` 张 E-R 图。
- 共优化 `3` 张 E-R 图。

### 重点布局调整

- `图5-1 用户角色与组织 E-R 图`：已完成 完成中文化，并按人工精修方式重排用户、角色、部门与日志/令牌支路。
- `图5-2 库存与批次 E-R 图`：已完成 完成中文化，并重点清理库存附近的主干线与属性线冲突。
- `图5-3 业务单据、预警与通知 E-R 图`：已完成 完成中文化，并重绘申领/出库与调拨/预警/通知两个模块。

### 中文化处理

- `图5-1`、`图5-2`、`图5-3` 的实体名、属性名和联系名已全部改为中文。
- 已移除实体框中的英文表名，不再保留中文与英文表名混排形式。
- 已将字段类英文标签统一转换为规范中文，例如“编号、仓库名称、当前库存、锁定库存、操作人编号、目标用户编号”等。

### 图题、图号与正文引用

- 图题与图号未新增也未改号，仍保持 `图5-1` 至 `图5-3` 连续编号。
- 正文引用校验结果：`已保留原有正确引用`。
- 本轮未补充新的图题、图号或正文引用文本。

### 排版优化

- 保持 Chen 风格不变，但最终论文内嵌图片已改为人工级几何重绘，不再依赖原先的 SVG 截图连线效果。
- 已重点处理关系主干交叉、长斜线、仓库和库存附近的穿框观感，以及属性椭圆过远和局部拥挤问题。
- 图片段落和图题段落结构保持原位，仅替换内嵌图片内容。

### 图源更新路径

- `output/doc/figures/fig_3_3_rbac_er.png`
- `output/doc/figures/fig_3_4_inventory_er.png`
- `output/doc/figures/fig_3_5_business_er.png`
- `output/doc/figures/drawio/fig_3_3_rbac_er.{yaml,spec.yaml,drawio,svg,arch.json}`
- `output/doc/figures/drawio/fig_3_4_inventory_er.{yaml,spec.yaml,drawio,svg,arch.json}`
- `output/doc/figures/drawio/fig_3_5_business_er.{yaml,spec.yaml,drawio,svg,arch.json}`

### 媒体替换校验

- `word/media/image7.png` -> `fig_3_3_rbac_er.png`，MD5=`6e0ce18d85738548130d06c337608b67`。
- `word/media/image8.png` -> `fig_3_4_inventory_er.png`，MD5=`9e24704cfed4a5dc49bc32e2f9ad3b9d`。
- `word/media/image9.png` -> `fig_3_5_business_er.png`，MD5=`c96b42e308fe53c49956d1522f1b71eb`。

### 视觉复核提示

- 当前环境未提供 Word 页面级渲染工具，建议在本地 Word 中打开输出文件，对第 5 章分页以及 `图5-2`、`图5-3` 的页内缩放观感做一次最终视觉复核。