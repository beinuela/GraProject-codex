from __future__ import annotations

import shutil
import zipfile
from datetime import datetime
from pathlib import Path

from docx import Document


ROOT = Path(__file__).resolve().parents[1]
DOC_DIR = ROOT / "output" / "doc"

DOC_UPDATES = {
    "校园物资智能管理系统设计与实现-正文扩写定稿版.docx": [
        {
            "index": 97,
            "anchor": "从项目实现看，系统并不是单一库存查询工具",
            "text": "系统并非单一库存查询工具，而是围绕物资档案、仓库、库存、批次、申领、审批、出库、调拨、预警和统计分析构建的校园物资管理业务系统。sql/schema.sql 中的 material_info、warehouse、inventory、inventory_batch、apply_order、transfer_order 和 warning_record 等表表明，系统在记录库存数量的同时，还保留了来源、去向、状态、处理人和时间等业务信息，为流程追踪、批次核对、库存预警和数据统计提供了数据基础。",
        },
        {
            "index": 98,
            "anchor": "在校园后勤或实验室物资管理中，物资流通信息化往往会涉及入库登记",
            "text": "在校园后勤或实验室物资管理中，物资流通信息化通常涉及入库登记、领用审核、库存监控和风险提醒等环节[2][5]。本系统采用 B/S 架构和数据库集中存储方式提升流程透明度，当前实现范围主要覆盖库存、申领、调拨、预警和统计分析等功能，不包括硬件识别、移动端扫码、采购付款和复杂预测模型。",
        },
        {
            "index": 101,
            "anchor": "需要说明的是，本文所称“智能管理”并不等同于复杂人工智能模型",
            "text": "本文所述“智能管理”主要体现为规则化判断、候选仓排序、批次优先扣减、补货建议和图表统计等辅助能力。这些功能具有规则可解释、结果可复核的特点，不涉及复杂人工智能模型。",
        },
        {
            "index": 105,
            "anchor": "从论文写作意义看，扩写后的正文需要避免把系统包装为算法平台",
            "text": "结合 WarningService、SmartService、TransferService 和 InventoryService 的实现，系统的辅助决策能力主要体现在库存风险识别、统计汇总、补货参考和候选仓排序等方面。相关功能均以规则和业务数据为基础，能够直接服务于日常管理场景。",
        },
        {
            "index": 107,
            "anchor": "本课题的工程意义在于将软件工程中的需求分析、数据库设计、前后端分离",
            "text": "本课题的工程重点在于将需求分析、数据库设计、前后端分离、权限控制、事务处理和测试验证结合起来，形成面向校园物资管理的完整业务系统。系统围绕 sys_user、material_info、inventory_batch、apply_order、transfer_order 和 warning_record 等真实数据对象组织功能，需求、设计、实现和测试之间具有明确对应关系。",
        },
        {
            "index": 111,
            "anchor": "Spring Boot 与 Vue 组合在高校信息化系统、事故报告数据库、学术档案管理等系统类论文中较为常见",
            "text": "Spring Boot 与 Vue 的组合已广泛应用于高校信息化和管理系统开发[6][7][8]。结合本项目实际实现，系统重点采用前后端分离、权限控制、库存业务处理和测试验证等工程方案，未使用的技术不纳入本系统实现范围。",
        },
        {
            "index": 124,
            "anchor": "技术选型的依据来自 `backend/pom.xml`、`frontend/package.json` 和后端配置文件",
            "text": "技术选型的依据来自 backend/pom.xml、frontend/package.json 和后端配置文件。后端使用 Spring Boot 3.3.5 和 Java 17，结合 Spring Web、Validation、Security、AOP、Actuator、MyBatis-Plus、MySQL 驱动、JJWT、Knife4j、Bucket4j、Caffeine、Micrometer 和 Sentry 等依赖。前端使用 Vue 3.5、Vite 6、Pinia、Vue Router、Element Plus、Axios、ECharts 和 Playwright/Vitest 测试工具。上述技术共同支撑认证授权、库存管理、申领审批、调拨、预警和统计等核心功能。",
        },
        {
            "index": 127,
            "anchor": "根据 `frontend/package.json` 和 `backend/pom.xml`，系统实际使用的核心技术包括",
            "text": "根据 frontend/package.json 和 backend/pom.xml，系统实际使用的核心技术包括 Vue 3、Vite、Pinia、Element Plus、Axios、ECharts、Spring Boot 3.3.5、Spring Security、JWT、MyBatis-Plus、JdbcTemplate、MySQL 8 和 H2。这些技术分别承担前端页面组织、状态管理、图表展示、认证授权、数据访问、统计查询和测试支撑等职责。",
        },
        {
            "index": 135,
            "anchor": "MyBatis-Plus 在项目中承担基础 CRUD、分页查询、逻辑删除、乐观锁字段更新等常见数据访问工作",
            "text": "MyBatis-Plus 在项目中承担基础 CRUD、分页查询、逻辑删除和乐观锁更新等常见数据访问工作。BaseEntity 中的 deleted、version、created_at 和 updated_at 等字段与 SQL 表结构保持一致，使多数业务表具备软删除、版本控制和审计时间等统一的数据管理能力。",
        },
        {
            "index": 140,
            "anchor": "安全设计还包括 refresh token 持久化与轮换",
            "text": "安全设计还包括 refresh token 持久化与轮换。auth_refresh_token 表保存 token_id、token_hash、expire_at 和 revoked 等字段，刷新成功后撤销旧令牌并签发新令牌。配置文件中还包含 CORS 白名单、登录/刷新/高风险接口限流参数、管理端点暴露范围和安全响应头配置，共同构成系统的安全边界；当前实现范围不包括多因素认证和外部安全认证体系。",
        },
        {
            "index": 142,
            "anchor": "安全设计不能只停留在登录成功",
            "text": "安全设计不仅覆盖登录认证，还包括 refresh token 持久化、刷新令牌轮换、登录日志、操作日志、安全响应头和部分高风险接口限流。结合业务逻辑漏洞防护要求[20]，系统在实现层面同时关注登录认证、业务状态流转和权限边界控制。",
        },
        {
            "index": 145,
            "anchor": "系统真实角色应以 `AuthService.buildMenusByRole` 和种子数据为准",
            "text": "系统角色以 AuthService.buildMenusByRole 和初始化数据为准。管理员 ADMIN 负责用户、部门、校区、物资、供应商、仓库、库存、入库、出库、申领、调拨、预警、事件、统计、日志、通知、系统配置和安全策略等模块的全局管理；仓储管理员 WAREHOUSE_ADMIN 侧重基础物资、仓库、库位、库存、入库、出库、调拨、预警和统计；审批人员 APPROVER 主要处理申领、调拨、预警和统计；部门用户 DEPT_USER 主要提交申领并查看统计。当前实现不包含采购人员、配送人员或独立移动端角色。",
        },
        {
            "index": 146,
            "anchor": "从业务边界看，系统支持的是校园物资从建档、入库、库存汇总、申领审批、出库、签收、调拨、预警到统计分析的闭环",
            "text": "从业务边界看，系统支持校园物资从建档、入库、库存汇总、申领审批、出库、签收、调拨、预警到统计分析的完整闭环。当前 SQL 中没有独立配送任务表，代码中也没有配送人员角色，相关业务主要体现在申领流程中的出库、签收以及调拨流程中的执行和接收。",
        },
        {
            "index": 150,
            "anchor": "库存业务需求包括库存查询、批次查询、入库登记、出库登记和盘点调整",
            "text": "库存业务需求包括库存查询、批次查询、入库登记、出库登记和盘点调整。入库需求要求能够记录仓库、来源类型、操作人、物资明细、批次号、数量、生产日期和过期日期，并同步更新库存汇总。出库需求要求能够校验可用库存，按批次效期顺序扣减，保留出库单与出库明细，并在关联申领单时回写实发数量和单据状态。当前盘点调整功能主要针对库存汇总数量修正，尚未形成独立的盘亏盘盈审批流程。",
        },
        {
            "index": 182,
            "anchor": "系统采用典型 B/S 架构和前后端分离模式，浏览器访问 Vue 构建的前端页面",
            "text": "系统采用典型 B/S 架构和前后端分离模式，浏览器访问 Vue 构建的前端页面，前端通过 Axios 请求后端 REST 接口，后端在 Spring Security 过滤链中完成 JWT 校验，再进入 Controller、Service、Mapper 和 MySQL。该结构使页面展示、业务逻辑和数据持久化分工清晰，便于系统维护和模块扩展。",
        },
        {
            "index": 183,
            "anchor": "后端分层并不意味着系统采用微服务",
            "text": "后端分层并不意味着系统采用微服务。当前项目是单体 Spring Boot 应用，所有模块运行在同一个后端工程中，通过包结构和 Service 分层实现职责划分。配置中包含 Actuator、Prometheus 指标和 Sentry 依赖，主要用于工程观测与异常采集支撑，不涉及分布式部署方案。",
        },
        {
            "index": 193,
            "anchor": "如图4-3所示，前端页面到后端数据库的交互流程包括页面输入、请求封装、令牌携带、接口分发、业务处理、数据访问和页面刷新",
            "text": "如图4-3所示，前端页面到后端数据库的交互流程包括页面输入、请求封装、令牌携带、接口分发、业务处理、数据访问和页面刷新。用户在 Vue 页面输入查询条件或提交表单后，前端请求工具携带 JWT 调用接口；后端先经过安全过滤链，再由 Controller 分发到 Service；Service 在事务中执行业务规则并调用 Mapper；数据库返回结果后，后端统一包装响应，前端根据返回数据更新 Pinia 状态、表格或 ECharts 图表。该流程概括了页面输入、请求鉴权、业务处理和结果返回的完整链路，是各业务模块共享的交互基础。",
        },
        {
            "index": 201,
            "anchor": "调拨推荐的智能性同样需要谨慎表述",
            "text": "调拨推荐的实现重点在于为候选仓选择提供辅助依据。TransferService.recommendTransfer 在候选仓满足库存数量要求的前提下，结合 DijkstraUtil 计算的校区距离进行排序。该功能适合作为仓库选择辅助，不涉及车辆路径优化、配送排班或实时交通调度。",
        },
        {
            "index": 206,
            "anchor": "数据库设计以业务闭环和数据可追溯为基本原则",
            "text": "数据库设计以业务闭环和数据可追溯为基本原则。用户、角色、部门用于承载组织和权限；物资分类、物资信息、供应商、仓库、校区、库位构成基础数据；库存、批次、入库、出库、申领和调拨承载核心业务；预警、通知、登录日志、操作日志、事件记录和系统配置提供支撑能力。SQL 中未定义显式外键约束，各表之间主要通过业务字段形成逻辑关联。",
        },
        {
            "index": 219,
            "anchor": "物资基础数据表包括 `material_category`、`material_info`、`supplier`、`warehouse`、`campus` 和 `storage_location`",
            "text": "物资基础数据表包括 material_category、material_info、supplier、warehouse、campus 和 storage_location。其中 material_info 是核心档案表，material_code 具有唯一约束，safety_stock 直接影响低库存和积压预警，shelf_life_days、production_date 与 expire_date 的组合支撑效期管理；仓库表记录仓库名称、校区、地址和负责人，库位表记录库位编码、容量、已用数量和状态。这些字段共同支撑物资主数据维护、库存分布管理和效期预警逻辑。",
        },
        {
            "index": 222,
            "anchor": "申领相关表包括 `apply_order` 和 `apply_order_item`",
            "text": "申领相关表包括 apply_order 和 apply_order_item。主表保存部门、申请人、紧急等级、状态、原因、场景、快速通道标记、锁定仓库、审批人、审批意见和审批时间；明细表保存物资、申请数量和实发数量。当前实现状态由既有字段控制，不包含“配送中”“已评价”等扩展状态。",
        },
        {
            "index": 230,
            "anchor": "开发环境与运行环境应以项目文件为准",
            "text": "开发环境与运行环境以项目文件为准。后端使用 Java 17、Spring Boot 3.3.5 和 Maven 管理依赖，默认配置由 application.yml 激活 dev profile，可通过环境变量切换数据库、JWT 密钥、CORS、限流和管理端点；测试使用 H2 数据库和 application-test.yml。前端使用 Node.js、Vite、Vue 3、Element Plus 和 ECharts，测试工具包括 Vitest 与 Playwright。当前运行验证主要基于本地开发与测试环境。",
        },
        {
            "index": 231,
            "anchor": "系统在本地演示时通常由后端 8080 端口提供业务接口",
            "text": "系统在本地演示时通常由后端 8080 端口提供业务接口，前端开发或预览服务提供页面访问。配置文件中的 18080 管理端点和 Prometheus 指标暴露主要用于本地观测与接口验证，当前测试范围以本地自动化测试和本地基线场景为主，未涉及线上运行规模验证。",
        },
        {
            "index": 246,
            "anchor": "基础数据模块还包括校区、仓库、库位、供应商、物资分类和物资档案",
            "text": "基础数据模块还包括校区、仓库、库位、供应商、物资分类和物资档案。相应 Controller 多采用列表查询、保存和删除三类接口，Service 使用 MyBatis-Plus 完成基础数据维护。物资档案页面和物资分类页面对应 MaterialController，仓库页面对应 WarehouseController，库位页面对应 StorageLocationController。这些模块虽以基础维护为主，但为库存、申领、调拨和预警等核心业务提供主数据支撑。",
        },
        {
            "index": 247,
            "anchor": "物资档案维护的重点是物资编码唯一、分类归属、安全库存和效期参数",
            "text": "物资档案维护的重点是物资编码唯一、分类归属、安全库存和效期参数。安全库存被低库存和积压规则使用，保质期和批次过期日期共同支撑临期提醒；仓库和校区字段被库存分布和调拨推荐使用。当前实现中供应商模块主要用于维护供货范围与联系方式，尚未形成采购订单闭环。",
        },
        {
            "index": 252,
            "anchor": "入库业务的执行顺序如图6-9所示",
            "text": "入库业务的执行顺序如图6-9所示。该流程对应 stock_in、stock_in_item、inventory 和 inventory_batch 四类表，展示了入库业务对批次管理和库存汇总的同步影响。当前实现范围不包括采购验收、财务付款和供应商结算流程。",
        },
        {
            "index": 265,
            "anchor": "调拨模块用于处理不同仓库之间的物资调剂",
            "text": "调拨模块用于处理不同仓库之间的物资调剂。用户创建调拨单时必须指定调出仓库和调入仓库，二者不能相同，并填写调拨明细。提交后状态由 DRAFT 变为 SUBMITTED，审批通过后进入 APPROVED，驳回则进入 REJECTED；调拨执行后状态为 OUTBOUND，接收后状态为 RECEIVED。这些状态构成当前调拨流程的完整状态集合。",
        },
        {
            "index": 267,
            "anchor": "候选仓排序接口为调拨申请提供参考",
            "text": "候选仓排序接口为调拨申请提供参考。用户给出目标校区、物资和数量后，系统先筛选库存数量满足需求的仓库，再根据固定校区图计算距离并排序。该设计用于为调拨申请提供候选仓参考，不涉及物流优化算法或实时地图调度。",
        },
        {
            "index": 282,
            "anchor": "移动平均预测由 `SmartService.forecast` 实现",
            "text": "移动平均预测由 SmartService.forecast 实现。该方法读取指定物资近 6 个月出库明细，按月份汇总数量，计算历史月均值，并将该均值作为未来若干月的参考数量。该方法适合作为趋势参考，不涉及复杂预测模型。",
        },
        {
            "index": 286,
            "anchor": "补货建议和移动平均预测由 `SmartService` 实现",
            "text": "补货建议和移动平均预测由 SmartService 实现。补货建议根据物资安全库存、当前库存、近 30 天出库量和保障天数计算目标库存与建议补货量；预测接口按近 6 个月出库历史聚合月度数量，并用平均值生成未来月份参考值。该功能用于生成补货参考和基于移动平均的趋势预测，不涉及模型训练。",
        },
        {
            "index": 300,
            "anchor": "通知模块保存系统消息和业务关联信息",
            "text": "通知模块保存系统消息和业务关联信息。notification 表包含标题、内容、消息类型、目标用户、已读状态、业务类型和业务 id，前端通知中心提供列表、未读数量、标记已读、全部已读和删除能力。当前通知模块以站内消息为主，未接入短信、邮件或第三方推送渠道。",
        },
        {
            "index": 301,
            "anchor": "系统配置和事件记录模块属于支撑能力",
            "text": "系统配置和事件记录模块属于支撑能力。配置表保存运行参数分组和值，事件表用于记录事件标题、类型、等级、校区、地点、描述、处理状态、上报人、处理人和处理结果，可为系统运行维护和事件处置提供数据支持。",
        },
        {
            "index": 305,
            "anchor": "测试环境由后端单元/集成测试、前端构建、前端单测和 E2E 流程测试组成",
            "text": "测试环境由后端单元/集成测试、前端构建、前端单测和 E2E 流程测试组成。后端测试使用 Maven、JUnit、Spring Boot Test、H2 数据库和测试 profile；前端构建使用 Vite；前端单测使用 Vitest；端到端测试使用 Playwright 脚本启动后端 screenshot profile 与前端预览服务。各项测试共同覆盖后端业务校验、前端构建完整性和核心流程联调场景。",
        },
        {
            "index": 306,
            "anchor": "本次扩写前重新执行了 `mvn test`、`npm run build`、`npm run test:unit` 和 `npm run test:e2e`",
            "text": "系统测试阶段重新执行了 mvn test、npm run build、npm run test:unit 和 npm run test:e2e。后端测试结果为 49 项通过，失败 0，错误 0，跳过 0；前端构建通过；前端单测为 3 个测试文件、8 项测试通过；E2E 为 4 个场景通过。E2E 运行过程中后端记录的权限拒绝日志属于权限边界触发后的预期日志表现，不影响场景通过结果。",
        },
        {
            "index": 311,
            "anchor": "后端测试没有给出正式性能指标",
            "text": "现有测试结果主要用于验证功能正确性、异常处理和核心流程一致性，尚不足以推导特定并发规模下的性能结论。",
        },
        {
            "index": 315,
            "anchor": "前端构建验证了 Vue 单文件组件、路由懒加载、Element Plus、ECharts 和样式资源能够被 Vite 正常打包",
            "text": "前端构建验证了 Vue 单文件组件、路由懒加载、Element Plus、ECharts 和样式资源能够被 Vite 正常打包。构建输出覆盖登录页、运营总览、库存、入库、出库、申领、调拨、预警、统计、日志和通知等页面资源，说明前端各功能模块已完成集成打包。",
        },
        {
            "index": 316,
            "anchor": "前端单测主要覆盖 HTTP 请求封装和登录页面交互",
            "text": "前端单测主要覆盖 HTTP 请求封装和登录页面交互。虽然测试数量不多，但能验证请求工具、登录页面基本状态和组件行为没有构建层面的错误。E2E 脚本覆盖管理员登录查看库存、部门用户创建申领单、仓储管理员创建调拨单和仓储管理员处理预警四个典型流程，这些场景覆盖了系统核心业务闭环。",
        },
        {
            "index": 317,
            "anchor": "权限测试在 E2E 和后端日志中也有所体现",
            "text": "权限测试在 E2E 和后端日志中也有所体现。部门用户访问不属于自身角色的仓库或日志接口时，后端记录权限拒绝异常，页面流程仍能继续完成预期动作。这表明权限边界控制已生效，日志中的权限拒绝记录属于预期安全响应，不构成系统错误。",
        },
        {
            "index": 321,
            "anchor": "业务场景验证主要围绕论文中的核心流程",
            "text": "业务场景验证主要围绕系统核心流程。申领审批场景验证从部门用户创建申领单到提交、审批、出库和签收的状态变化；调拨场景验证来源仓扣减、目标仓增加和批次镜像生成；预警场景验证手动扫描和处理动作；认证场景验证登录、刷新、旧 refresh token 失效和安全响应头。",
        },
        {
            "index": 324,
            "anchor": "性能部分只按仓库已有 `tests/performance` 脚本和 `docs/performance-baseline.md` 记录进行保守描述",
            "text": "项目提供登录、库存列表、预警列表和操作日志列表等接口的本地性能基线脚本，并在 docs/performance-baseline.md 中保留了对应测试材料。当前未追加新的并发压测结果，因此性能部分主要用于说明项目具备接口级基线验证能力。",
        },
        {
            "index": 326,
            "anchor": "仓库中提供了 `tests/performance` 下的 k6 脚本和 `docs/performance-baseline.md` 基线记录",
            "text": "仓库中提供了 tests/performance 下的 k6 脚本和 docs/performance-baseline.md 基线记录。该基线记录采集于本地 screenshot profile 和隔离 H2 数据集，场景包括登录、库存分页列表、预警分页列表和操作日志分页列表，适用于相同环境下的基线对比，不用于推导生产服务承诺或大规模并发结论。",
        },
        {
            "index": 333,
            "anchor": "从实现结果看，系统已经能够支持多角色登录和菜单边界控制",
            "text": "从实现结果看，系统已经能够支持多角色登录和菜单边界控制，并完成库存入库、批次维护、按效期优先出库、申领审批闭环、跨仓调拨、库存风险预警和补货参考等核心功能。测试验证表明，后端自动化测试、前端构建、前端单测和端到端流程均已通过。",
        },
        {
            "index": 336,
            "anchor": "在毕业设计和论文撰写过程中",
            "text": "在毕业设计过程中，指导教师在选题确定、系统实现、结构组织和格式规范方面给予了耐心指导。软件学院提供的课程训练和毕业设计要求，使我能够将软件工程方法、数据库设计、前后端开发和测试验证结合起来完成本课题。",
        },
        {
            "index": 337,
            "anchor": "同时感谢同学和家人在资料整理、系统演示和论文修改过程中的支持",
            "text": "同时感谢同学和家人在资料整理与系统演示过程中的支持。通过本次毕业设计，我进一步认识到工程项目应以真实需求和可验证证据为基础，并在实现过程中保持规范和严谨。",
        },
    ],
    "校园物资智能管理系统设计与实现-正式定稿版.docx": [
        {
            "index": 98,
            "anchor": "需要说明的是，本文所称“智能管理”并不等同于复杂人工智能模型",
            "text": "本文所述“智能管理”主要体现为规则化判断、候选仓排序、批次优先扣减、补货建议和图表统计等辅助能力。这些功能具有规则可解释、结果可复核的特点，不涉及复杂人工智能模型。",
        },
        {
            "index": 101,
            "anchor": "本课题的工程意义在于将软件工程中的需求分析、数据库设计、前后端分离",
            "text": "本课题的工程重点在于将需求分析、数据库设计、前后端分离、权限控制、事务处理和测试验证结合起来，形成面向校园物资管理的完整业务系统。系统围绕 sys_user、material_info、inventory_batch、apply_order、transfer_order 和 warning_record 等真实数据对象组织功能，需求、设计、实现和测试之间具有明确对应关系。",
        },
        {
            "index": 107,
            "anchor": "国内近年的智能管理研究也开始关注预警、流程自动化和数据分析",
            "text": "国内近年的智能管理研究也开始关注预警、流程自动化和数据分析。例如入厂煤智能管理平台将流程管控和预警模型结合起来[21]，智慧实验室管理研究讨论了更复杂的人工智能方法[22]。与这些研究相比，本系统的实现范围更聚焦于本科毕业设计场景，主要采用固定阈值、时间窗口、移动平均和候选排序等轻量方法。",
        },
        {
            "index": 119,
            "anchor": "Vite 用于前端工程构建",
            "text": "Vite 用于前端工程构建，能够提供较快的本地开发启动和生产构建能力，适合本系统这类多页面管理端。ECharts 用于库存占比、出入库趋势、仓库分布、部门排行和效期统计等图表展示，相关功能主要承担统计分析和可视化展示任务，不涉及实时大数据分析平台。",
        },
        {
            "index": 125,
            "anchor": "安全设计不能只停留在登录成功",
            "text": "安全设计不仅覆盖登录认证，还包括 refresh token 持久化、刷新令牌轮换、登录日志、操作日志、安全响应头和部分高风险接口限流。结合业务逻辑漏洞防护要求[20]，系统在实现层面同时关注登录认证、业务状态流转和权限边界控制。",
        },
        {
            "index": 213,
            "anchor": "补货建议和移动平均预测由 `SmartService` 实现",
            "text": "补货建议和移动平均预测由 SmartService 实现。补货建议根据物资安全库存、当前库存、近 30 天出库量和保障天数计算目标库存与建议补货量；预测接口按近 6 个月出库历史聚合月度数量，并用平均值生成未来月份参考值。该功能用于生成补货参考和基于移动平均的趋势预测，不涉及模型训练。",
        },
        {
            "index": 228,
            "anchor": "业务场景验证主要围绕论文中的核心流程",
            "text": "业务场景验证主要围绕系统核心流程。申领审批场景验证从部门用户创建申领单到提交、审批、出库和签收的状态变化；调拨场景验证来源仓扣减、目标仓增加和批次镜像生成；预警场景验证手动扫描和处理动作；认证场景验证登录、刷新、旧 refresh token 失效和安全响应头。",
        },
        {
            "index": 231,
            "anchor": "仓库中提供了 `tests/performance` 下的 k6 脚本和 `docs/performance-baseline.md` 基线记录",
            "text": "仓库中提供了 tests/performance 下的 k6 脚本和 docs/performance-baseline.md 基线记录。该基线记录采集于本地 screenshot profile 和隔离 H2 数据集，场景包括登录、库存分页列表、预警分页列表和操作日志分页列表，适用于相同环境下的基线对比，不用于推导生产服务承诺或大规模并发结论。",
        },
        {
            "index": 235,
            "anchor": "从实现结果看，系统已经能够支持多角色登录和菜单边界控制",
            "text": "从实现结果看，系统已经能够支持多角色登录和菜单边界控制，并完成库存入库、批次维护、按效期优先出库、申领审批闭环、跨仓调拨、库存风险预警和补货参考等核心功能。测试验证表明，后端自动化测试、前端构建、前端单测和端到端流程均已通过。",
        },
        {
            "index": 238,
            "anchor": "在毕业设计和论文撰写过程中",
            "text": "在毕业设计过程中，指导教师在选题确定、系统实现、结构组织和格式规范方面给予了耐心指导。软件学院提供的课程训练和毕业设计要求，使我能够将软件工程方法、数据库设计、前后端开发和测试验证结合起来完成本课题。",
        },
        {
            "index": 239,
            "anchor": "同时感谢同学和家人在资料整理、系统演示和论文修改过程中的支持",
            "text": "同时感谢同学和家人在资料整理与系统演示过程中的支持。通过本次毕业设计，我进一步认识到工程项目应以真实需求和可验证证据为基础，并在实现过程中保持规范和严谨。",
        },
    ],
}

RESIDUAL_PATTERNS = [
    "从论文写作意义看",
    "扩写后的正文",
    "本次扩写",
    "扩写版论文",
    "论文写作因此",
    "论文扩写时",
    "论文后续会",
    "论文正文只能",
    "论文正文不写成",
    "论文中可以写",
    "论文中只写",
    "论文只能称为",
    "论文不得",
    "不写成",
    "本节需要",
    "本文应该",
    "写作时可以",
    "修改过程中的支持",
]


def update_document(doc_path: Path, updates: list[dict[str, object]], timestamp: str) -> Path:
    backup_path = doc_path.with_name(f"{doc_path.stem}-写作痕迹清理备份-{timestamp}{doc_path.suffix}")
    shutil.copy2(doc_path, backup_path)

    document = Document(doc_path)
    for item in updates:
        index = int(item["index"])
        anchor = str(item["anchor"])
        new_text = str(item["text"])
        paragraph = document.paragraphs[index]
        current = paragraph.text.strip()
        if anchor not in current:
            raise RuntimeError(f"{doc_path.name} 段落 {index} 未命中锚点: {anchor}\n实际内容: {current}")
        paragraph.text = new_text

    document.save(doc_path)
    return backup_path


def scan_residuals(doc_path: Path) -> list[str]:
    hits: list[str] = []
    with zipfile.ZipFile(doc_path) as archive:
        for entry in archive.namelist():
            if not entry.startswith("word/") or not entry.endswith(".xml"):
                continue
            content = archive.read(entry).decode("utf-8", errors="ignore")
            for pattern in RESIDUAL_PATTERNS:
                if pattern in content:
                    hits.append(f"{doc_path.name}::{entry}::{pattern}")
    return hits


def main() -> None:
    timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
    residual_hits: list[str] = []
    for filename, updates in DOC_UPDATES.items():
        doc_path = DOC_DIR / filename
        backup_path = update_document(doc_path, updates, timestamp)
        hits = scan_residuals(doc_path)
        residual_hits.extend(hits)
        print(f"[updated] {doc_path.name}")
        print(f"  backup: {backup_path.name}")
        print(f"  paragraphs: {len(updates)}")

    if residual_hits:
        print("[residual-hits]")
        for hit in residual_hits:
            print(f"  {hit}")
    else:
        print("[residual-hits] none")


if __name__ == "__main__":
    main()
