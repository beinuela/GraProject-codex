# Keep-Rewrite-Delete矩阵

| 类型 | 内容位置/主题 | 当前问题 | 处理建议 | 依据 |
|---|---|---|---|---|
| Keep | 题目与系统定位 | 项目仓库、SQL 和页面均围绕校园物资管理 | 保留并扩写为前后端分离管理系统 | README、pom、package、schema |
| Keep | 申领审批、库存批次、调拨、预警 | 有代码和测试支撑 | 作为论文核心业务 | ApplyService、InventoryService、TransferService、WarningService |
| Keep | JWT/RBAC 权限 | 有认证、角色菜单和测试 | 保留并扩写安全设计 | AuthService、SecurityConfig、JwtTokenProvider |
| Rewrite | 智能表述 | 容易被写成算法论文 | 改为规则化预警、FEFO、候选仓排序和移动平均 | SmartService、WarningService |
| Rewrite | 数据库章节 | 旧稿表字段可能不全 | 按 schema.sql 重写关键表关系与字段 | sql/schema.sql |
| Rewrite | 测试章节 | 需使用最新执行结果 | 写 49 后端测试、前端构建、8 单测、4 E2E | 本次命令输出 |
| Delete | 独立配送任务模块 | 无配送任务表、角色和接口 | 删除，不作为已实现功能 | schema、Controller 列表 |
| Delete | 移动端、小程序、扫码柜、复杂 AI | 无代码和配置证据 | 仅可作为展望或不写 | 仓库证据不足 |
| Delete | 新增外部参考文献 | 用户限制只允许 CNKI txt | 删除所有非指定来源 | CNKI-20260426111148676.txt |
