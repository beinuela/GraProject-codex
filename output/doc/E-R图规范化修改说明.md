# E-R图规范化修改说明

- 工作底稿：`校园物资智能管理系统设计与实现-参考文献引用修复版.docx`
- 备份文件：`校园物资智能管理系统设计与实现-参考文献引用修复版-E-R图规范化前备份-20260429103448.docx`
- 输出文件：`校园物资智能管理系统设计与实现-E-R图规范化版.docx`

## 修改结果

- 共替换 `3` 张 E-R 图。

### 由旧样式改为新样式的图

- `图5-1 用户角色与组织 E-R 图`：由“旧样式实体框内列属性”改为“Chen 风格实体-属性-联系图”。
- `图5-2 库存与批次 E-R 图`：由“旧样式实体框内列属性”改为“Chen 风格实体-属性-联系图”。
- `图5-3 业务单据、预警与通知 E-R 图`：由“旧样式实体框内列属性”改为“Chen 风格实体-属性-联系图”。

### 图题、图号与正文引用

- 图题与图号未新增也未改号，仍保持 `图5-1` 至 `图5-3` 连续编号。
- 正文引用校验结果：`已保留原有正确引用`。
- 本轮未补充新的图题、图号或正文引用文本。

### 排版优化

- 已将 3 张图统一为黑白学术风格的 Chen 式 E-R 图。
- 已优化实体、属性、联系与基数的版式布局，减少文字遮挡和关系线混乱。
- 图片段落和图题段落结构保持原位，仅替换内嵌图片内容。

### 图源更新路径

- `output/doc/figures/fig_3_3_rbac_er.png`
- `output/doc/figures/fig_3_4_inventory_er.png`
- `output/doc/figures/fig_3_5_business_er.png`
- `output/doc/figures/drawio/fig_3_3_rbac_er.{yaml,spec.yaml,drawio,svg,arch.json}`
- `output/doc/figures/drawio/fig_3_4_inventory_er.{yaml,spec.yaml,drawio,svg,arch.json}`
- `output/doc/figures/drawio/fig_3_5_business_er.{yaml,spec.yaml,drawio,svg,arch.json}`

### 媒体替换校验

- `word/media/image7.png` -> `fig_3_3_rbac_er.png`，MD5=`515cd7213543057e1e193d9e1c77ea3e`。
- `word/media/image8.png` -> `fig_3_4_inventory_er.png`，MD5=`b328c193358e5dccbaeea6e08fc773b6`。
- `word/media/image9.png` -> `fig_3_5_business_er.png`，MD5=`4dfc609a4ae8eb41d6616456b4e886a2`。

### 视觉复核提示

- 当前环境未提供 Word 页面级渲染工具，建议在本地 Word 中打开输出文件，对第 5 章分页与图形清晰度做一次最终视觉复核。