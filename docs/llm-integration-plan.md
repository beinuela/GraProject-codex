# 校园物资管理系统大模型接入改造方案

## 1. 改造目标

本次改造的目标不是单独增加一个“智能问答”弹窗，而是把大模型能力嵌入现有业务流程，使系统从“规则驱动的智能管理”升级为“规则 + 大模型协同的辅助决策系统”。

面向答辩和论文，建议把本次改造定义为以下三类能力：

- `AI 预警处置助手`：对低库存、临期、异常出库等预警生成原因分析、风险等级和处置建议。
- `AI 调拨决策助手`：对现有调拨推荐结果补充调拨理由、备选方案和执行风险说明。
- `AI 统计分析助手`：对统计分析页面现有图表和指标生成运营摘要、补货建议和异常波动解释。

这样做的价值在于：

- 复用现有业务数据和业务规则，避免做成脱离系统的数据玩具。
- 大模型只负责“解释、归纳、建议”，不直接改库存、不直接审批，风险可控。
- 工作量能体现在后端集成、提示词编排、权限设计、审计留痕、前端交互和测试补齐上。

## 2. 现有基础与切入点

仓库内已经具备适合接入大模型的基础能力：

- `backend/src/main/java/com/campus/material/modules/smart/` 已有补货建议与移动平均预测接口。
- `backend/src/main/java/com/campus/material/modules/transfer/service/TransferService.java` 已有基于距离和库存的调拨推荐逻辑。
- `frontend/src/views/analytics/AnalyticsView.vue` 已展示补货建议和趋势预测。
- `frontend/src/views/transfer/TransferView.vue` 已展示推荐调出仓。
- `frontend/src/views/warning/WarningView.vue` 可作为预警分析的核心挂载页面。

因此，推荐路线不是推翻重做，而是在现有规则推荐之上补一层 `AI 解释与决策辅助层`。

## 3. 业务改造设计

### 3.1 AI 预警处置助手

接入位置：

- `frontend/src/views/warning/WarningView.vue`

新增交互：

- 每条预警增加 `AI 分析` 按钮。
- 预警详情弹窗增加 `风险等级`、`可能原因`、`建议动作`、`建议负责人`、`建议完成时限`。
- 增加 `采纳建议`、`部分采纳`、`不采纳` 反馈入口。

后端分析输入建议：

- `warning_record`
- 对应 `material_info`
- 对应仓库 `inventory`
- 最近 30 天 `stock_out_item`
- 对应批次 `inventory_batch`
- 未完成申领单 / 调拨单数量

建议输出固定为结构化 JSON：

```json
{
  "riskLevel": "HIGH",
  "summary": "某仓库医用口罩库存已低于安全库存，且近7日出库速度明显高于近30日均值。",
  "possibleCauses": [
    "短期集中领用",
    "补货周期偏长"
  ],
  "actions": [
    "优先执行紧急补货",
    "从东风校区仓调拨 200 件",
    "通知仓管员复核安全库存阈值"
  ],
  "ownerRole": "WAREHOUSE_ADMIN",
  "deadlineHours": 24
}
```

### 3.2 AI 调拨决策助手

接入位置：

- `frontend/src/views/transfer/TransferView.vue`

新增交互：

- 在现有“智能推荐”结果旁增加 `AI 生成调拨说明`。
- 用户选择目标仓、物资和数量后，系统生成 `推荐理由`、`备选仓排序说明`、`执行风险`、`是否建议拆单`。

后端分析输入建议：

- `TransferService.recommendTransfer(...)` 返回结果
- 目标仓与候选源仓信息
- 候选仓库存数量
- 对应物资的近期出库速度
- 候选仓临期批次数量
- 当前未完成申领 / 调拨单压力

建议输出固定为结构化 JSON：

```json
{
  "recommendedWarehouseId": 3,
  "decisionSummary": "优先建议从中心物资周转区调出，距离更短且库存冗余更高。",
  "reasonPoints": [
    "运输距离最短",
    "可用库存覆盖本次需求",
    "对来源仓安全库存影响较小"
  ],
  "alternativePlans": [
    {
      "warehouseId": 2,
      "reason": "可作为备选，但该仓近期领用波动较大。"
    }
  ],
  "risks": [
    "若本周继续发生集中申领，来源仓可能逼近安全库存"
  ],
  "suggestSplitTransfer": false
}
```

### 3.3 AI 统计分析助手

接入位置：

- `frontend/src/views/analytics/AnalyticsView.vue`

新增交互：

- 页面顶部增加 `生成 AI 运营摘要`。
- 在图表下展示 `本周库存风险摘要`、`补货关注物资`、`异常波动说明`、`管理建议`。
- 支持导出文本到答辩演示材料或周报。

后端分析输入建议：

- `/api/analytics/overview`
- 库存占比、仓库分布、出入库趋势
- 临期统计
- `smart/replenishment-suggestions`
- 选定物资的历史预测数据

建议输出固定为结构化 JSON：

```json
{
  "summary": "系统整体库存稳定，但存在临期风险集中和个别物资补货压力。",
  "highlights": [
    "待处理预警数量较上周上升",
    "A 类防疫物资出库趋势抬升"
  ],
  "attentionMaterials": [
    "一次性医用口罩",
    "消毒液"
  ],
  "managementActions": [
    "优先补货口罩和消毒液",
    "检查龙子湖校区临期批次周转策略"
  ]
}
```

## 4. 技术架构设计

## 4.1 后端模块划分

建议新增：

- `backend/src/main/java/com/campus/material/modules/llm/config`
- `backend/src/main/java/com/campus/material/modules/llm/controller`
- `backend/src/main/java/com/campus/material/modules/llm/service`
- `backend/src/main/java/com/campus/material/modules/llm/dto`
- `backend/src/main/java/com/campus/material/modules/llm/entity`
- `backend/src/main/java/com/campus/material/modules/llm/mapper`

建议核心类：

- `LlmProperties`：读取 `baseUrl`、`apiKey`、`model`、`timeout`、`enabled`
- `LlmClient`：统一封装 OpenAI-compatible 请求
- `PromptTemplateService`：封装预警、调拨、统计三类提示词模板
- `AiAnalysisService`：组织业务数据、调用模型、解析结构化输出
- `AiAuditService`：落库审计、记录耗时、token 用量、采纳反馈
- `AiAssistantController`：对前端暴露统一接口

实现建议：

- 基于 Spring Boot 3 自带 `RestClient` 实现，不必引入额外 HTTP SDK。
- 要求模型返回 JSON，再由后端做字段校验，避免前端直接消费自由文本。
- 对超时、空响应、JSON 解析失败要做降级，页面允许显示“规则推荐仍可用”。

## 4.2 前端改造点

建议新增通用能力：

- `frontend/src/api/ai.js`：封装 AI 接口
- `frontend/src/components/ui/AiResultCard.vue`：统一展示 AI 分析结果
- `frontend/src/components/ui/AiFeedbackBar.vue`：统一采纳反馈

建议页面改造：

- `WarningView.vue`：增加预警 AI 分析入口
- `TransferView.vue`：增加调拨说明生成入口
- `AnalyticsView.vue`：增加运营摘要生成入口

如果希望答辩时更“完整”，可追加：

- `frontend/src/views/ai/AiCenterView.vue`

该页面用于展示：

- 历史 AI 分析记录
- 按业务类型筛选
- 采纳率统计
- 模型调用耗时和失败率

这个页面不是必须，但很适合体现工程量和平台化设计。

## 4.3 权限与安全

角色建议：

- `ADMIN`：可查看全部 AI 能力和调用日志
- `WAREHOUSE_ADMIN`：可用预警分析、调拨分析、统计摘要
- `APPROVER`：可用预警分析、统计摘要
- 普通部门用户默认不开放 AI 决策接口

安全边界建议：

- 不把完整库表原始数据直接传给前端，再由前端拼 prompt。
- API Key 仅保存在后端环境变量中，不进入前端构建产物。
- 业务提示词中不传密码、手机号等敏感信息。
- 大模型只给建议，不直接落业务状态变更。

## 5. 数据库与审计设计

不建议把所有模型调用直接塞进现有 `operation_log`，最好单独建表，后续论文也更好写。

推荐新增以下表：

### 5.1 `ai_analysis_task`

用途：

- 记录一次 AI 分析任务
- 便于后续扩展为异步任务和历史查询

建议字段：

- `id`
- `biz_type`：`WARNING` / `TRANSFER` / `ANALYTICS`
- `biz_id`
- `request_snapshot`：输入摘要 JSON
- `status`：`PENDING` / `SUCCESS` / `FAILED`
- `result_json`
- `error_message`
- `created_by`
- `started_at`
- `finished_at`
- `created_at`

### 5.2 `ai_call_log`

用途：

- 记录模型调用审计信息
- 支撑性能统计、失败排查和论文中的“系统评估”

建议字段：

- `id`
- `task_id`
- `provider_name`
- `model_name`
- `prompt_template_code`
- `prompt_tokens`
- `completion_tokens`
- `latency_ms`
- `success_flag`
- `error_message`
- `created_at`

### 5.3 `ai_feedback`

用途：

- 记录人工是否采纳 AI 建议
- 可形成“人机协同闭环”亮点

建议字段：

- `id`
- `task_id`
- `feedback_type`：`ACCEPTED` / `PARTIAL` / `REJECTED`
- `feedback_remark`
- `feedback_user_id`
- `created_at`

### 5.4 `ai_prompt_template`

用途：

- 管理不同业务模板
- 便于后续对提示词做版本化

建议字段：

- `id`
- `template_code`
- `template_name`
- `system_prompt`
- `user_prompt`
- `version_no`
- `enabled`
- `created_at`

如果时间紧，可先不做页面维护，仅在数据库和初始化 SQL 中保留。

## 6. API 设计建议

建议接口：

- `POST /api/ai/warnings/{id}/analyze`
- `POST /api/ai/transfers/decision`
- `POST /api/ai/analytics/summary`
- `GET /api/ai/tasks/{id}`
- `GET /api/ai/tasks`
- `POST /api/ai/tasks/{id}/feedback`

统一响应建议：

```json
{
  "taskId": 1001,
  "status": "SUCCESS",
  "bizType": "WARNING",
  "result": {
    "riskLevel": "HIGH",
    "summary": "库存低于安全阈值且近7日消耗偏快"
  }
}
```

## 7. 提示词工程建议

提示词不要让模型“自由发挥”，而要强调以下规则：

- 你是校园物资管理辅助决策助手，不得编造不存在的库存数据。
- 所有结论必须基于输入 JSON。
- 不允许输出 SQL、代码或审批结论。
- 输出必须是合法 JSON。
- 若信息不足，应明确返回“信息不足”而不是猜测。

推荐模式：

- `System Prompt`：约束角色、边界、输出格式
- `User Prompt`：放业务数据 JSON 和本次任务目标
- 后端校验：字段完整性、枚举值、长度限制

## 8. 实施分期

### 第一阶段：可演示 MVP

目标：

- 打通后端模型调用
- 实现预警分析
- 在预警页面展示 AI 结果

交付内容：

- `llm` 模块基础设施
- `ai_analysis_task`、`ai_call_log`
- `POST /api/ai/warnings/{id}/analyze`
- `WarningView.vue` AI 分析按钮和结果卡片

### 第二阶段：论文增强版

目标：

- 扩展到调拨和统计分析
- 增加人工反馈和历史记录

交付内容：

- `POST /api/ai/transfers/decision`
- `POST /api/ai/analytics/summary`
- `ai_feedback`
- `AiCenterView.vue`

### 第三阶段：工程化加强版

目标：

- 增加失败重试、缓存、超时统计、模型配置切换

交付内容：

- 模型配置项
- 调用熔断和降级
- 结果缓存
- 单测、接口测试、前端交互测试

## 9. 论文与答辩写法建议

建议把这部分写成一个独立章节，例如：

- `4.x 大模型辅助决策模块设计`
- `4.x.1 模块目标与业务定位`
- `4.x.2 提示词模板与数据组织`
- `4.x.3 AI 分析任务与审计留痕设计`
- `4.x.4 人机协同反馈机制`
- `4.x.5 安全边界与降级策略`

答辩时可以强调：

- 本项目不是把大模型当聊天机器人，而是作为业务辅助决策引擎。
- 现有规则算法负责“算”，大模型负责“解释”和“归纳建议”。
- 系统保留人工确认环节，避免 AI 直接改业务数据。
- 所有 AI 调用都有日志和反馈闭环，具备可审计性。

## 10. 为什么这套方案比“聊天框”更好

- 与现有申领、调拨、预警、统计模块强绑定，业务关联更真实。
- 后端、前端、数据库、权限、日志、测试都有改动，工作量清晰。
- 能把“智能管理系统”从规则型升级到模型辅助型，题目更匹配。
- 即使现场网络不好，也可以保留原规则推荐作为降级路径，不影响演示。

## 11. 推荐实施顺序

如果只做一轮，我建议按下面顺序推进：

1. 先做 `AI 预警处置助手`，这是最容易讲清楚业务价值的点。
2. 再做 `AI 调拨决策助手`，和现有调拨推荐形成“算法 + LLM 解释”组合。
3. 最后做 `AI 统计分析助手`，用于答辩展示和论文扩写。

这三步做完后，项目就不再只是“能接大模型”，而是已经形成一套可以写进论文、答辩也能讲得通的智能化改造方案。
