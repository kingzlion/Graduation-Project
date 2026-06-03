# 商机供应链扩展 — 变更记录

> 每次改动一完成就追加在此文件，最终交给另一位开发者核对。

## Phase 1 — 核心功能

### [2026-05-30] Zdgc 实体扩展 — 全生命周期字段

**改动文件**: `backend/src/main/java/com/example/gis/entity/Zdgc.java`

**新增字段**:
| 字段 | 类型 | 说明 |
|------|------|------|
| `status` | String | 工程状态: planning / under_construction / completed |
| `contractor` | String | 总包单位 |
| `bidAmount` | BigDecimal | 中标金额（万元） |
| `workCondition` | String (Lob) | 当前工况描述 |
| `startDate` | LocalDate | 开工日期 |
| `endDate` | LocalDate | 竣工日期 |
| `category` | String | 工程分类（城市绿化/市政配套/交通物流/安置房） |
| `updatedAt` | LocalDateTime | 更新时间 |

---

### [2026-05-30] SupplyMerchant 实体扩展 — 黄页展示字段

**改动文件**: `backend/src/main/java/com/example/gis/entity/SupplyMerchant.java`

**新增字段**:
| 字段 | 类型 | 说明 |
|------|------|------|
| `logoUrl` | String | 商家 Logo URL |
| `description` | String | 商家简介 |
| `businessHours` | String | 营业时间 |
| `rating` | Double | 评分 1-5 |
| `verified` | Boolean | 是否认证入驻 |
| `createdAt` | LocalDateTime | 入驻时间 |
| `updatedAt` | LocalDateTime | 更新时间 |
| `distance` | Double (@Transient) | 距离检索点的距离(km)，非持久化 |

**新增工具方法**: `getXDouble()` / `getYDouble()` — 安全坐标转换

---

### [2026-05-30] Repository 扩展

**改动文件**:
- `backend/src/main/java/com/example/gis/repository/ZdgcRepository.java`
- `backend/src/main/java/com/example/gis/repository/SupplyMerchantRepository.java`

**新增方法**:
| Repository | 方法 | 说明 |
|-----------|------|------|
| ZdgcRepository | `findByStatus()` | 按状态筛选 |
| ZdgcRepository | `findByCategory()` | 按分类筛选 |
| ZdgcRepository | `findByStatusAndCategory()` | 联合筛选 |
| SupplyMerchantRepository | `findAllByOrderByIsVipDescMerchantNameAsc()` | VIP 优先排序 |
| SupplyMerchantRepository | `findByBusinessType()` | 按业态查询 |
| SupplyMerchantRepository | `findByMerchantNameContainingOrBusinessTypeContaining()` | 模糊搜索 |
| SupplyMerchantRepository | `findByVerifiedTrue()` | 已认证列表 |
| SupplyMerchantRepository | `countByBusinessType()` | 业态统计 |

---

### [2026-05-30] Service 层扩展

**改动文件**:
- `backend/src/main/java/com/example/gis/service/ApiService.java`
- `backend/src/main/java/com/example/gis/service/impl/ApiServiceImpl.java`
- `backend/src/main/java/com/example/gis/service/ManagementService.java`
- `backend/src/main/java/com/example/gis/service/impl/ManagementServiceImpl.java`

**新增/增强方法**:

**ApiService**:
- `listByStatus(status)` — 按状态筛选工程 (FR-2.1)
- `listByCategory(category)` — 按分类筛选工程 (FR-2.1)
- `listMerchantsSorted()` — VIP 优先商家列表 (FR-2.3)
- `registerMerchant(merchant)` — 商家自主入驻 (FR-2.3)
- `findNearbyMerchantsEnhanced()` — 增强版周边检索，支持业态过滤+距离计算 (FR-2.2)
- `getMerchantDirectory()` — 黄页首页（分类聚合+VIP 推荐）(FR-2.3)
- `searchMerchants(keyword)` — 黄页搜索 (FR-2.3)
- `getMerchantDetail(id)` — 商家详情（含周边工程匹配）(FR-2.3)

**ManagementService**:
- `listMerchants(status?)` — 商家列表，支持按审核状态筛选
- `listAllMerchants()` — 全部商家
- `findMerchant(id)` — 查找商家
- `saveMerchant(merchant)` — 新增/编辑商家
- `deleteMerchant(id)` — 删除商家
- `auditMerchant(id, approved, reason)` — 入驻审核
- `setVip(id, isVip)` — 设置 VIP

---

### [2026-05-30] Controller 扩展

**改动文件**:
- `backend/src/main/java/com/example/gis/controller/ApiController.java`
- `backend/src/main/java/com/example/gis/controller/ManagementController.java`

**新增 API**:

**ApiController**:
| 方法 | 路径 | 说明 |
|------|------|------|
| GET/POST | `/api/zdgc/list?status=&category=` | 按状态/分类筛选工程列表 |
| GET | `/api/zdgc/detail?id=` | 工程全生命周期详情 |
| GET/POST | `/api/merchant/list` | VIP 优先排序的商家列表 |
| POST | `/api/merchant/register` | 商家自主入驻 |
| GET | `/api/merchant/nearby?+businessType` | 增强版周边检索 |
| GET | `/api/merchant/directory` | 黄页首页 |
| GET | `/api/merchant/search?keyword=` | 黄页搜索 |
| GET | `/api/merchant/detail?id=` | 商家详情（含周边工程） |

**ManagementController**:
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/manage/merchant/list?status=` | 商家列表 |
| POST | `/manage/merchant/save` | 新增/编辑商家 |
| DELETE | `/manage/merchant/{id}` | 删除商家 |
| POST | `/manage/merchant/audit` | 入驻审核 |
| POST | `/manage/merchant/set-vip` | 设置 VIP |

---

### [2026-05-30] 种子数据更新

**改动文件**: `backend/src/main/java/com/example/gis/utils/DataInitializer.java`

**重点工程 (Zdgc)**: 27 条
- 所有占位名称（撒法发、发生发等）替换为真实工程名
- 每一个都设置了 status/contractor/bidAmount/category/workCondition/startDate/endDate
- 状态分布: 4 已交付 🟢 / 8 在建 🟡 / 15 规划 🔴

**商家 (SupplyMerchant)**: 19 条
- 14 家已认证（含 4 家 VIP）+ 5 家待审核
- 覆盖 6 种业态：五金建材、重型机械租赁、大宗建材、劳务输出、城市绿化、交通物流
- 所有扩展字段（logoUrl、description、businessHours、rating 等）已填充

**人文景观 (Rwjg)**: 10 条（不变）
**留言 (NostalgiaComment)**: 4 条（不变）

---

### [2026-05-30] Bug 修复

**B1**: PC 前端调用了不存在的端点
- 文件: `frontend-pc/src/App.vue`
- 修复: `/api/supply-merchant/list` → `/api/merchant/list`

**B2**: 移动端按 `item.category` 筛选工程，但 Zdgc 无此字段
- 修复: Zdgc 实体已增加 `category` 字段 + 种子数据已填充
- 移动端硬编码分类 `['城市绿化', '市政配套', '交通物流', '安置房']` 与种子数据一致

**B3**: SupplyMerchant 坐标存为 String，每次要 parseDouble
- 修复: 增加 `@Transient getXDouble()` / `getYDouble()` 安全转换方法

---

### [2026-05-30] 管理后台扩展

**改动文件**:
- `backend/src/main/resources/static/admin.html`
- `backend/src/main/resources/static/admin.js`

**新增内容**:
- 侧边栏新增「商家管理」入口
- 统计卡片新增「入驻商家」计数
- 商家管理表格：商家名、业态、联系人、电话、坐标、VIP、认证状态、评分、操作
- 商家编辑弹窗：全部字段可编辑
- 操作按钮：编辑、删除、入驻审核（通过）、VIP 切换
- 筛选下拉：全部/待审核/已认证
- 重点工程编辑表单同步升级：新增状态、分类、总包单位、中标金额、开工/竣工日期、工况字段
