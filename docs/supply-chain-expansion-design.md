# 商机供应链模块扩展设计文档

> **编写日期**: 2026-05-30
> **状态**: 待实现
> **实现者**: (由另一位开发者接手)

---

## 目录

1. [概述](#1-概述)
2. [现状分析](#2-现状分析)
3. [FR-2.1 重点工程时空打点与全生命周期追踪](#3-fr-21-重点工程时空打点与全生命周期追踪)
4. [FR-2.2 供应链周边空间检索](#4-fr-22-供应链周边空间检索)
5. [FR-2.3 商家高亮入驻与黄页展示](#5-fr-23-商家高亮入驻与黄页展示)
6. [数据库变更总览](#6-数据库变更总览)
7. [API 接口总览](#7-api-接口总览)
8. [前端改动要点](#8-前端改动要点)
9. [实现优先级建议](#9-实现优先级建议)

---

## 1. 概述

### 1.1 目标

在现有「工地商机雷达」基础上，将商机供应链从**简单的附近商家查询**升级为**全生命周期工程追踪 + 空间缓冲区检索 + 商家黄页营销**三位一体的 B 端核心模块。

### 1.2 涉及范围

| 层级 | 影响 |
|------|------|
| 后端 Entity | 新增 `ProjectLifecycle` / 扩展现有 `Zdgc`；扩展 `SupplyMerchant` |
| 后端 Repository | 新增空间查询、分类筛选、VIP 排序 |
| 后端 Service | 新增时空检索、黄页管理、入驻审核 |
| 后端 Controller | 新增 B 端专属 API 组 |
| 前端 uni-app (H5/小程序) | 地图上三色 Marker、工程 InfoWindow、半径检索 UI、黄页列表 |
| 前端 PC (Vue3+OpenLayers) | 同步适配 |
| 管理后台 (admin.html) | 新增工程全生命周期编辑、商家入驻审核 |

---

## 2. 现状分析

### 2.1 已存在的能力

| 能力 | 说明 |
|------|------|
| `SupplyMerchant` 实体 | 含 merchantName, businessType, contactPerson, phone, address, x/y, serviceRadius, isVip |
| 附近商家 Haversine 检索 | `ApiService.findNearbyMerchants()` 内存计算，无索引 |
| 商家列表 API | `GET/POST /api/merchant/list` |
| 附近商家 API | `GET /api/merchant/nearby?longitude=X&latitude=Y&distance=30` |
| `Zdgc` 工程实体 | 含 id, name, x, y, jieshao, zoom, src |
| 管理后台 | 可 CRUD `Zdgc` 和 `Rwjg`，**但无商家管理** |
| 5 条种子商家数据 | 覆盖五金建材、重型机械租赁、大宗建材、劳务输出 |

### 2.2 当前缺失/需改进的点

| # | 问题 | 严重程度 |
|---|------|----------|
| 1 | `Zdgc` 没有工程状态字段（无法红黄绿三色标注） | ❗️阻断 |
| 2 | `Zdgc` 没有工期字段（开工/竣工时间、中标金额、总包单位） | ❗️阻断 |
| 3 | `Zdgc` 没有分类字段（移动端硬编码了 4 个分类但实体无对应字段） | ⚠️高 |
| 4 | 附近商家是内存 Haversine 计算，大数据量不可扩展 | ⚠️高 |
| 5 | 管理后台没有商家入驻审核功能 | ⚠️高 |
| 6 | 商家坐标存为 String，每次要 parseDouble | 🔧中 |
| 7 | PC 前端调用了错误的 `/api/supply-merchant/list` 端点 | 🐛阻断 |
| 8 | 入驻表单只有一个静态弹窗和假客服电话 | ❗️阻断 |
| 9 | 商家和工程之间没有关联表（M2M 匹配记录） | ⚠️高 |

---

## 3. FR-2.1 重点工程时空打点与全生命周期追踪

### 3.1 需求描述

> 地图上以红、黄、绿三色动态标注工程状态：
> - 🟥 **红** = 规划征迁中
> - 🟨 **黄** = 火热在建中
> - 🟩 **绿** = 已交付成果
>
> 点击工程图标（Marker），B 端用户可查阅：
> 项目名称、总包单位、中标金额、当前工况、开竣工时间。

### 3.2 数据库设计

#### 方案 A：扩展 `Zdgc` 实体（推荐 ✅）

直接在 `ZDGC` 表上新增字段，保持单表简单。

```sql
-- ZDGC 表新增字段
ALTER TABLE ZDGC ADD COLUMN status          VARCHAR(20)  DEFAULT 'planning';   -- planning/under_construction/completed
ALTER TABLE ZDGC ADD COLUMN contractor      VARCHAR(255);                     -- 总包单位
ALTER TABLE ZDGC ADD COLUMN bid_amount      DECIMAL(15,2);                    -- 中标金额（万元）
ALTER TABLE ZDGC ADD COLUMN work_condition  TEXT;                             -- 当前工况描述
ALTER TABLE ZDGC ADD COLUMN start_date      DATE;                             -- 开工日期
ALTER TABLE ZDGC ADD COLUMN end_date        DATE;                             -- 竣工日期
ALTER TABLE ZDGC ADD COLUMN category        VARCHAR(50);                      -- 工程分类（城市绿化/市政配套/交通物流/安置房）
ALTER TABLE ZDGC ADD COLUMN updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
```

#### 方案 B：新建 `ProjectLifecycle` 实体（更规范，但冗余）

```sql
CREATE TABLE PROJECT_LIFECYCLE (
    id              VARCHAR(64) PRIMARY KEY,
    project_id      VARCHAR(6)  NOT NULL,       -- 关联 ZDGC.id
    status          VARCHAR(20) NOT NULL DEFAULT 'planning',
    contractor      VARCHAR(255),
    bid_amount      DECIMAL(15,2),
    work_condition  TEXT,
    start_date      DATE,
    end_date        DATE,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES ZDGC(id)
);
```

**推荐方案 A**，理由：
- 目前 `Zdgc` 记录仅 27 条，扩展字段不会造成维护负担
- 单表查询无 JOIN，前端加载全量工程时更高效
- 工程和生命周期是 1:1 关系，没必要拆表

### 3.3 实体层（Java）

```java
// Zdgc.java — 新增字段
@Entity
@Table(name = "ZDGC")
public class Zdgc {
    // ... 现有字段保持不变 ...

    @Column(name = "status", length = 20)
    private String status;           // "planning" | "under_construction" | "completed"

    @Column(name = "contractor", length = 255)
    private String contractor;       // 总包单位

    @Column(name = "bid_amount")
    private BigDecimal bidAmount;    // 中标金额（万元）

    @Column(name = "work_condition")
    private String workCondition;    // 当前工况

    @Column(name = "start_date")
    private LocalDate startDate;     // 开工日期

    @Column(name = "end_date")
    private LocalDate endDate;       // 竣工日期

    @Column(name = "category", length = 50)
    private String category;         // 工程分类

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // getters/setters ...
}
```

### 3.4 Marker 三色规则

前端根据 `status` 字段渲染不同颜色的 Marker：

| status 值 | 显示色 | 含义 | Marker 图标 |
|-----------|--------|------|-------------|
| `planning` | 🔴 红 | 规划征迁中 | `marker-red.png` 或 CSS 动态着色 |
| `under_construction` | 🟡 黄 | 火热在建中 | `marker-yellow.png` |
| `completed` | 🟢 绿 | 已交付成果 | `marker-green.png` |

### 3.5 API 设计

```http
### 获取工程详情（含生命周期全字段）
GET /api/zdgc/detail?id={id}
Response:
{
  "code": 200,
  "data": {
    "id": "P001",
    "name": "雄安站枢纽片区市政道路",
    "status": "under_construction",     // 红黄绿状态
    "contractor": "中建八局",
    "bidAmount": 28500.00,
    "workCondition": "管廊主体结构完成85%，路面铺设中",
    "startDate": "2025-03-01",
    "endDate": "2027-06-30",
    "category": "交通物流",
    "x": "116.154",
    "y": "39.054",
    "zoom": 14
  }
}

### 按状态筛选工程列表
GET /api/zdgc/list?status=under_construction

### 按分类筛选
GET /api/zdgc/list?category=交通物流
```

---

## 4. FR-2.2 供应链周边空间检索

### 4.1 需求描述

> 系统提供空间缓冲区分析能力。总包方或设备商可一键检索某一在建工程
> 「方圆 5 公里 / 10 公里」范围内已入驻的五金店、挖机租赁商、建筑网片厂。

### 4.2 空间查询优化

#### 现状：内存 Haversine（ findAll + 逐条计算）
- 数据量 < 1000 条时可接受
- 不适合未来商家规模扩展

#### 推荐方案：两步走

**阶段 1（当前即可用）**：优化现有 Haversine 实现
- 添加 `businessType` 过滤参数
- 添加分页支持
- 保持内存计算，因为目前仅 5 条商家数据

**阶段 2（未来数据量大时）**：引入 H2 空间扩展或 MySQL
- H2 支持 `H2GIS` 扩展，可用 `ST_DWithin()` 函数
- 或直接切换到 MySQL，使用 `ST_Distance_Sphere()`

### 4.3 API 设计

```http
### 周边检索（增强版）
GET /api/merchant/nearby
    ?longitude=116.154
    &latitude=39.054
    &distance=10           ← 新增：支持 5/10 可选
    &businessType=五金建材  ← 新增：按业态筛选
    &page=0&size=20       ← 新增：分页

Response:
{
  "code": 200,
  "data": {
    "content": [
      {
        "id": "m1",
        "merchantName": "雄安顺达五金建材",
        "businessType": "五金建材",
        "distance": 3.2,          // ← 新增：距离（km）
        "contactPerson": "张经理",
        "contactPhone": "1883123XXXX",
        "address": "雄安新区容东片区",
        "isVip": true,
        "serviceRadius": 25.0
      }
    ],
    "totalElements": 3,
    "totalPages": 1,
    "number": 0,
    "size": 20
  }
}
```

### 4.4 Service 层核心逻辑

```java
// ApiServiceImpl.java — 增强 findNearbyMerchants
public Page<SupplyMerchant> findNearbyMerchants(
        double longitude, double latitude, double maxDistanceKm,
        String businessType, int page, int size) {

    // 1. 获取所有商家（阶段1），或 WHERE businessType=?（阶段1.5）
    List<SupplyMerchant> all;
    if (businessType != null && !businessType.isEmpty()) {
        all = supplyMerchantRepository.findByBusinessType(businessType);
    } else {
        all = supplyMerchantRepository.findAll();
    }

    // 2. Haversine 过滤 + 计算距离
    List<SupplyMerchant> filtered = all.stream()
        .filter(m -> m.getX() != null && m.getY() != null)
        .map(m -> {
            double dist = calculateHaversineDistance(
                latitude, longitude,
                Double.parseDouble(m.getY()), Double.parseDouble(m.getX()));
            // 将距离设置到 merchant 对象（需要新增 distance 字段，非持久化）
            m.setDistance(dist);
            return m;
        })
        .filter(m -> m.getDistance() <= maxDistanceKm)
        .sorted(Comparator.comparingDouble(SupplyMerchant::getDistance))
        .collect(Collectors.toList());

    // 3. 内存分页
    int start = page * size;
    int end = Math.min(start + size, filtered.size());
    List<SupplyMerchant> pageContent = filtered.subList(start, end);

    return new PageImpl<>(pageContent, PageRequest.of(page, size), filtered.size());
}
```

### 4.5 前端半径选择器

```html
<!-- uni-app 前端 B 端面板 → 附近商家查询 -->
<view class="radius-selector">
  <text class="label">检索半径</text>
  <view class="radio-group">
    <text class="radio-item" :class="{active: searchRadius === 5}"
          @tap="searchRadius = 5; loadNearbyMerchants()">5公里</text>
    <text class="radio-item" :class="{active: searchRadius === 10}"
          @tap="searchRadius = 10; loadNearbyMerchants()">10公里</text>
    <text class="radio-item" :class="{active: searchRadius === 30}"
          @tap="searchRadius = 30; loadNearbyMerchants()">30公里</text>
  </view>
</view>

<!-- 业态过滤 -->
<scroll-view class="type-filter" scroll-x>
  <text class="type-tag" v-for="type in businessTypes"
        :class="{active: selectedType === type}"
        @tap="selectedType = type; loadNearbyMerchants()">
    {{ type }}
  </text>
</scroll-view>
```

---

## 5. FR-2.3 商家高亮入驻与黄页展示

### 5.1 需求描述

> 商家高亮入驻：VIP 商家在地图和列表中以醒目样式展示。
> 黄页展示：按业态分类展示所有入驻商家的信息黄页，支持搜索与筛选。

### 5.2 SupplyMerchant 扩展

```java
@Entity
@Table(name = "SUPPLY_MERCHANT")
public class SupplyMerchant {
    // 现有字段保持不变...

    @Column(name = "distance")   // 非持久化字段，仅用于 API 响应
    @Transient
    private Double distance;     // 距离当前检索点的距离（km）

    @Column(name = "logo_url", length = 500)
    private String logoUrl;      // 商家 Logo

    @Column(name = "description", length = 2000)
    private String description;  // 商家简介

    @Column(name = "business_hours", length = 100)
    private String businessHours; // 营业时间

    @Column(name = "rating")
    private Double rating;        // 评分 1-5

    @Column(name = "verified")
    private Boolean verified;     // 是否认证入驻

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

### 5.3 Repository 增强

```java
public interface SupplyMerchantRepository extends JpaRepository<SupplyMerchant, String> {

    // VIP 商家优先，按名称排序
    List<SupplyMerchant> findAllByOrderByIsVipDescMerchantNameAsc();

    // 按业态查询
    List<SupplyMerchant> findByBusinessType(String businessType);

    // 模糊搜索（黄页搜索）
    List<SupplyMerchant> findByMerchantNameContainingOrBusinessTypeContaining(
            String name, String type);

    // 已认证商家列表
    List<SupplyMerchant> findByVerifiedTrue();

    // 统计业态分类
    @Query("SELECT s.businessType, COUNT(s) FROM SupplyMerchant s GROUP BY s.businessType")
    List<Object[]> countByBusinessType();
}
```

### 5.4 黄页 API

```http
### 黄页首页：按业态分类聚合
GET /api/merchant/directory
Response:
{
  "code": 200,
  "data": {
    "categories": [
      {
        "type": "五金建材",
        "count": 12,
        "merchants": [ ... ]       // 前 4 个推荐
      },
      {
        "type": "重型机械租赁",
        "count": 8,
        "merchants": [ ... ]
      }
    ],
    "vipMerchants": [ ... ]        // VIP 商家置顶
  }
}

### 黄页搜索
GET /api/merchant/search?keyword=挖机&page=0&size=20

### 商家详情
GET /api/merchant/detail?id=m1
Response:
{
  "code": 200,
  "data": {
    "id": "m1",
    "merchantName": "雄安顺达五金建材",
    "businessType": "五金建材",
    "contactPerson": "张经理",
    "contactPhone": "1883123XXXX",
    "address": "雄安新区容东片区",
    "isVip": true,
    "serviceRadius": 25.0,
    "logoUrl": "/images/merchants/m1.png",
    "description": "主营建筑五金、管件阀门、电工电料",
    "businessHours": "08:00-19:00",
    "rating": 4.5,
    "verified": true,
    "nearbyProjects": [             // 周边匹配的工程
      {
        "id": "P001",
        "name": "雄安站枢纽片区市政道路",
        "distance": 3.2
      }
    ]
  }
}
```

### 5.5 商家入驻/管理 API

```http
### 商家自主入驻（公开接口）
POST /api/merchant/register
Body: { merchantName, businessType, contactPerson, contactPhone, address, x, y, serviceRadius }

### 商家入驻审核（管理后台）
POST /manage/merchant/audit
Body: { merchantId, action: "approve"|"reject", reason: "..." }
Auth: Admin

### 商家管理 CRUD（管理后台）
GET    /manage/merchant/list?status=pending   ← 待审核商家
POST   /manage/merchant/save                   ← 新增/编辑商家
DELETE /manage/merchant/{id}                   ← 删除商家
POST   /manage/merchant/set-vip                ← 设置 VIP
```

### 5.6 前端黄页 UI 示意

```
┌─────────────────────────────────────────┐
│  🔍 搜索商家/品类...                      │
├─────────────────────────────────────────┤
│  ⭐ 推荐商家（VIP 置顶）                   │
│  ┌──────────────────────────────────┐   │
│  │ ⭐ 雄安顺达五金建材    距离 3.2km │   │
│  │   五金建材 · 张经理 · 1851234   │   │
│  │   ⏰ 08:00-19:00          联系→│   │
│  └──────────────────────────────────┘   │
├─────────────────────────────────────────┤
│  📂 分类黄页                             │
│  ┌──────────────────────────────────┐   │
│  │ 🔧 五金建材             12 家 →│   │
│  ├──────────────────────────────────┤   │
│  │ 🏗️ 重型机械租赁        8 家  →│   │
│  ├──────────────────────────────────┤   │
│  │ 🧱 大宗建材              6 家  →│   │
│  ├──────────────────────────────────┤   │
│  │ 👷 劳务输出              5 家  →│   │
│  └──────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

### 5.7 管理后台「商家入驻审核」页面

需要在 `admin.html` 新增一个审核面板：

```
┌─────────────────────────────────────────────┐
│ 商家管理                                      │
│ ┌─────┬──────┬────────┬──────┬──────────────┐│
│ │ 状态 │ 商家名 │ 业态   │ 联系人 │ 操作         ││
│ ├─────┼──────┼────────┼──────┼──────────────┤│
│ │ ⏳待审 │ 雄安XX│ 五金   │ 李四  │ [通过][拒绝] ││
│ │ ✅已过 │ 顺达XX│ 机械   │ 张三  │ [编辑][删除] ││
│ │ ❌拒绝 │ XX公司│ 建材   │ 王五  │ [编辑]       ││
│ └─────┴──────┴────────┴──────┴──────────────┘│
│ [新增商家]                                     │
└─────────────────────────────────────────────┘
```

---

## 6. 数据库变更总览

### 6.1 ZDGC 表变更

| 操作 | 字段 | 类型 | 说明 |
|------|------|------|------|
| ✅ 新增 | `status` | VARCHAR(20) | 工程状态：planning / under_construction / completed |
| ✅ 新增 | `contractor` | VARCHAR(255) | 总包单位名称 |
| ✅ 新增 | `bid_amount` | DECIMAL(15,2) | 中标金额（万元） |
| ✅ 新增 | `work_condition` | TEXT | 当前工况描述 |
| ✅ 新增 | `start_date` | DATE | 开工日期 |
| ✅ 新增 | `end_date` | DATE | 竣工日期 |
| ✅ 新增 | `category` | VARCHAR(50) | 工程分类 |
| ✅ 新增 | `updated_at` | TIMESTAMP | 更新时间 |

### 6.2 SUPPLY_MERCHANT 表变更

| 操作 | 字段 | 类型 | 说明 |
|------|------|------|------|
| ⚠️ 修改 | `POINT_X` | DOUBLE (原 VARCHAR) | 可选：坐标改为数值类型 |
| ⚠️ 修改 | `POINT_Y` | DOUBLE (原 VARCHAR) | 可选：坐标改为数值类型 |
| ✅ 新增 | `logo_url` | VARCHAR(500) | 商家 Logo |
| ✅ 新增 | `description` | VARCHAR(2000) | 商家简介 |
| ✅ 新增 | `business_hours` | VARCHAR(100) | 营业时间 |
| ✅ 新增 | `rating` | DOUBLE | 评分 1-5 |
| ✅ 新增 | `verified` | BOOLEAN | 是否认证入驻 |
| ✅ 新增 | `created_at` | TIMESTAMP | 入驻时间 |
| ✅ 新增 | `updated_at` | TIMESTAMP | 更新时间 |

> **关于坐标改数值类型**：由于 JPA `ddl-auto: update` 不会自动转换列类型，建议方案：
> 1. 保留现有 String 字段 `x` / `y`
> 2. 新增 `@Transient` 的 `getXDouble()` / `getYDouble()` 做转换
> 3. 未来迁移时再加双写逻辑

### 6.3 新增种子数据（DataInitializer）

```java
// DataInitializer.java — 补充
// 为已有 Zdgc 设置 status/category
// 已有 27 条，随机分配一些为 planing/under_construction/completed

// 补充更多商家种子数据到 20+ 条，覆盖更多业态
```

---

## 7. API 接口总览

### 7.1 新增/修改接口

| 方法 | 路径 | 说明 | 所在 Controller |
|------|------|------|----------------|
| `GET` | `/api/zdgc/detail?id=` | 获取工程全生命周期详情 | ApiController |
| `GET` | `/api/zdgc/list?status=&category=` | 按状态/分类筛选工程列表 | ApiController |
| `GET` | `/api/merchant/nearby?+businessType&page&size` | 增强版周边检索 | ApiController |
| `GET` | `/api/merchant/directory` | 黄页首页（分类聚合+VIP） | ApiController |
| `GET` | `/api/merchant/search?keyword=` | 黄页搜索 | ApiController |
| `GET` | `/api/merchant/detail?id=` | 商家详情（含周边工程） | ApiController |
| `POST` | `/api/merchant/register` | 商家自主入驻 | ApiController |
| `GET` | `/manage/merchant/list?status=` | 管理后台商家列表 | ManagementController |
| `POST` | `/manage/merchant/save` | 管理后台新增/编辑商家 | ManagementController |
| `POST` | `/manage/merchant/audit` | 入驻审核（通过/拒绝） | ManagementController |
| `DELETE` | `/manage/merchant/{id}` | 删除商家 | ManagementController |
| `POST` | `/manage/merchant/set-vip` | 设置 VIP | ManagementController |

### 7.2 需要修复的问题

| # | 问题 | 修复方式 |
|---|------|----------|
| 🐛 | PC 前端调用 `/api/supply-merchant/list` 不存在 | 改为 `/api/merchant/list` |
| 🐛 | 移动端按 `item.category` 筛选工程，但 Zdgc 无此字段 | 增加 category 字段 + 种子数据 |

---

## 8. 前端改动要点

### 8.1 移动端 uni-app（H5/小程序）

**文件位置**: `frontend/src/pages/index/index.vue`

| 改动 | 描述 |
|------|------|
| Marker 三色 | 根据 `item.status` 动态设置 marker 图标为红/黄/绿 |
| InfoWindow 详情 | 点击 Marker 弹窗展示：项目名、总包单位、中标金额、工况、工期 |
| 半径选择器 | 在附近商家区域添加 5km/10km/30km radio |
| 业态过滤 | 在商家列表上方加横向滚动业态标签 |
| 黄页入口 | 新增「商家黄页」页面/路由，展示分类聚合和搜索 |
| VIP 高亮 | VIP 商家卡片加金色边框和 ⭐ 标识 |
| 入驻表单 | 替换假弹窗为真实的注册表单，调 `/api/merchant/register` |

### 8.2 PC 端 Vue3 + OpenLayers

**文件位置**: `frontend-pc/src/App.vue`

| 改动 | 描述 |
|------|------|
| 🐛 修复 API 路径 | `/api/supply-merchant/list` → `/api/merchant/list` |
| Marker 三色 | 同步移动端三色逻辑 |
| 工程详情面板 | 点击 Marker 展示全生命周期信息 |
| 黄页面板 | 新增商家黄页页面 |
| 管理后台 | 补充商家管理和入驻审核功能 |

### 8.3 管理后台（admin.html / admin.js）

| 改动 | 描述 |
|------|------|
| 商家列表 | 展示所有商家，标注 VIP、认证状态 |
| 商家编辑表单 | 新增/编辑商家全部字段 |
| 入驻审核 | 待审核列表 + 通过/拒绝操作 |
| VIP 管理 | 一键设置/取消 VIP |

---

## 9. 实现优先级建议

### Phase 1 — 核心功能（MVP）

| 排序 | 任务 | 估计工时 | 依赖 |
|------|------|----------|------|
| P1 | `Zdgc` 扩展字段 + 种子数据更新 | 2h | 无 |
| P1 | 三色 Marker + 工程详情 InfoWindow | 4h | P1-1 |
| P1 | 商家入驻注册 API + 前端表单 | 3h | 无 |
| P1 | 🐛 修复 PC 前端 API 路径 | 0.5h | 无 |
| P1 | 🐛 修复 category 过滤（字段+数据） | 1h | P1-1 |

### Phase 2 — 空间检索增强

| 排序 | 任务 | 估计工时 | 依赖 |
|------|------|----------|------|
| P2 | 半径选择器（5/10/30 km） | 2h | P1-2 |
| P2 | 业态过滤参数 | 1.5h | 无 |
| P2 | 分页支持 | 1h | 无 |

### Phase 3 — 黄页与运营

| 排序 | 任务 | 估计工时 | 依赖 |
|------|------|----------|------|
| P3 | 黄页首页（分类聚合） | 3h | P2 |
| P3 | 黄页搜索 | 2h | P2 |
| P3 | 管理后台：商家 CRUD + 审核 + VIP | 4h | P1-3 |
| P3 | VIP 商家高亮展示 | 1h | P3-3 |
| P3 | SupplyMerchant 扩展字段（logo、描述、评分等） | 2h | 无 |

---

## 附录 A：现有 Bug 清单

以下是在代码审查中发现的、影响供应链功能的 Bug，建议在 Phase 1 优先修复：

| ID | 文件 | 行号 | 问题 | 修复 |
|----|------|------|------|------|
| B1 | `frontend-pc/src/App.vue` | — | 调用 `/api/supply-merchant/list`，但该端点不存在 | 改为 `/api/merchant/list` |
| B2 | `frontend/src/pages/index/index.vue` | B端过滤 | 按 `item.category` 筛选，但 Zdgc 实体无 category 字段 | 实体增加 category + 种子数据填充 |
| B3 | `SupplyMerchant.java` | 31-35 | x/y 存为 String，每次查询要 parseDouble，可能抛 NumberFormatException | 增加 `@Transient getXDouble()` 安全转换方法 |

---

> **说明**：本文档记录了商机供应链模块从现有状态到完整三 Feature 的实现设计。
> 另一位开发者接手后，建议按 Phase 1 → 2 → 3 的顺序推进，每个 Phase 完成后进行接口测试。
> 如有疑问，可对照现有代码文件：
> - Entity: `backend/src/main/java/com/example/gis/entity/`
> - Service: `backend/src/main/java/com/example/gis/service/impl/ApiServiceImpl.java`
> - Controller: `backend/src/main/java/com/example/gis/controller/`
> - 移动端: `frontend/src/pages/index/index.vue`
> - PC 端: `frontend-pc/src/App.vue`
