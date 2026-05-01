# E-R图减交叉精修说明

- 工作底稿：`校园物资智能管理系统设计与实现-E-R图布局优化版.docx`
- 备份文件：`校园物资智能管理系统设计与实现-E-R图布局优化版-减交叉精修前备份-20260429173049.docx`
- 输出文件：`校园物资智能管理系统设计与实现-E-R图减交叉精修版.docx`

## 修改结果

- 共优化 `3` 张 E-R 图。

### 重点布局调整

- `图5-1 用户角色与组织 E-R 图`：已完成 中心辐射式压缩重排，缩短上下支路并压缩空白。
- `图5-2 库存与批次 E-R 图`：已完成 主档-库存-批次纵向主干重排，清空中心关系通道。
- `图5-3 业务单据、预警与通知 E-R 图`：已完成 上下双模块重排，拉直底部业务主线。

### 图题、图号与正文引用

- 图题与图号未新增也未改号，仍保持 `图5-1` 至 `图5-3` 连续编号。
- 正文引用校验结果：`已保留原有正确引用`。
- 本轮未补充新的图题、图号或正文引用文本。

### 排版优化

- 保持原有 Chen 风格不变，仅重排实体、属性、联系和基数标注的位置与连线组织。
- 本轮优先处理关系主干交叉、长斜线和局部拥挤问题，尽量让中心关系区保持净空。
- 图片段落和图题段落结构保持原位，仅替换内嵌图片内容。

### 图源更新路径

- `output/doc/figures/fig_3_3_rbac_er.png`
- `output/doc/figures/fig_3_4_inventory_er.png`
- `output/doc/figures/fig_3_5_business_er.png`
- `output/doc/figures/drawio/fig_3_3_rbac_er.{yaml,spec.yaml,drawio,svg,arch.json}`
- `output/doc/figures/drawio/fig_3_4_inventory_er.{yaml,spec.yaml,drawio,svg,arch.json}`
- `output/doc/figures/drawio/fig_3_5_business_er.{yaml,spec.yaml,drawio,svg,arch.json}`

### 媒体替换校验

- `word/media/image7.png` -> `fig_3_3_rbac_er.png`，MD5=`cb29546a3ac9a44e9b7850d4a96edbf2`。
- `word/media/image8.png` -> `fig_3_4_inventory_er.png`，MD5=`e4f4120a885f776373c3ee82cbd70b0f`。
- `word/media/image9.png` -> `fig_3_5_business_er.png`，MD5=`2b6779182c0cf1e602852587677a3f17`。

### 视觉复核提示

- 当前环境未提供 Word 页面级渲染工具，建议在本地 Word 中打开输出文件，对第 5 章分页、图内留白和 `图5-3` 下方业务模块的页内缩放观感做一次最终视觉复核。