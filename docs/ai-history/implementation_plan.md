# XAGIS HUB 管理员控制后台功能拓展设计方案

本方案旨在回答您关于“数据（包括地图坐标点）从何而来”的疑问，并根据高级产品经理与架构师的认知，将原有的 PC 管理员后台升级为一个**可视化的“AI 数据流与空间情报控制中心”**。

## 核心痛点与优化动因
1. **数据“黑盒”问题**：系统内的数据（工程点、人文回忆、商家位置）是通过后台 Python AI Agents 自动抓取的，但对管理员而言完全是“看不见的黑盒”。
2. **坐标生成感知差**：AI 提取文本地址后，通过地理编码（Geocoding）API 转换为经纬度，并做了合规脱敏微偏。这些转换过程需要一个可视化的测试与操作面板。
3. **市民反馈无法闭环**：小程序端用户提交的“全民随手拍”工单被保存在 SQLite 数据库中，但目前 PC 后台缺乏展示与工单处理（待处理 -> 解决中 -> 已解决）的业务闭环。

---

## 💡 方案详解：新增两大控制板块

我们将管理后台（Full-screen Admin Modal）现有的 Tab（原本只有工程、商户、人文）扩展为 5 个 Tab，新增以下两个高价值板块：

### 板块 1. AI 招投标与空间解析中心 (`AI Ingestion & Geocoding Hub`)
提供对 Python AI 智能体流水线的可视化控制与交互：
* **AI 抓取触发器**：一键请求 FastAPI 的 `/agent/crawl` 接口，唤醒“招投标数据情报官”进行最新招标信息爬取。
* **数据采集审核流**：将抓取到的原始公告（包含项目名、建设地址、核心摘要）及其经过 Geocoding 解析出的**脱敏坐标 (X, Y)** 展示在待办列表里。
* **一键审核发布**：管理员可以对抓取回来的数据进行微调（纠正名称、微调坐标），点击“审核发布”后直接保存至 H2 数据库（写入 WebGIS 地图）。
* **Geocoding 交互沙盒（解析工具）**：提供地址解析器。管理员输入任意文本地址（如“雄安启动区综合服务中心”），即可实时调用 Geocoding API 得到 GCJ-02 脱敏坐标，并可一键填入新建表单中。

### 板块 2. 市民随手拍与城管闭环管理 (`Citizen Incident Manager`)
实现 C 端随手拍上报与 B 端/管理员处理的完美闭环：
* **市民投诉清单**：展示市民在微信小程序里提交的所有关于交通、垃圾、路面破损等工单。
* **坐标定位展示**：点击某个市民投诉工单，大屏 WebGIS 地图会自动平滑飞行定位到事发坐标点，并叠加高亮黄色警告 Marker。
* **工单状态处理**：支持将工单状态一键切换为 `待处理` 🔴、`处理中` 🟡 或 `已解决` 🟢，同步更新 SQLite 数据库。

---

## 🛠️ 拟进行的修改

### 1. Python FastAPI 后端 (`agents/main.py`)
#### [MODIFY] [main.py](file:///Users/zli/Documents/gs/Graduation-Project/agents/main.py)
* 引入新的 Pydantic 模型 `CitizenReportStatusUpdate`。
* 新增 `GET /api/v1/citizen/reports` 接口，按时间倒序查询所有市民上报事件。
* 新增 `POST /api/v1/citizen/reports/{report_id}/status` 接口，支持管理员更新市民工单状态。

### 2. PC 前端 (`frontend-pc/src/App.vue`)
#### [MODIFY] [App.vue](file:///Users/zli/Documents/gs/Graduation-Project/frontend-pc/src/App.vue)
* **Tab 导航**：在管理后台 Modal 中新增 `AI 招投标与空间解析` 与 `市民城管随手拍` 两个功能切换卡。
* **AI 解析交互实现**：
  * 新增 `runAiCrawl()` 方法，控制抓取 Loading 态，并接收抓取结果展现为待审核卡片。
  * 新增 `geocodeCustomAddress()` 方法，调用 FastAPI 解析自定义地名并展示坐标。
  * 新增 `approveAndPublishLead(lead)` 方法，将 AI 待审数据一键上报 Java 后端。
* **随手拍管理实现**：
  * 新增 `loadCitizenReports()` 加载市民问题。
  * 新增 `updateReportStatus(id, status)` 更新工单进度。
  * 新增 `locateReportOnMap(report)` 地图联动定位，让地图图层飞行定位（`view.animate`）到市民投诉的坐标并闪烁。
* **高级 UI 细节**：使用深色微光玻璃卡片、霓虹状态标签（红色-待处理，黄色-处理中，绿色-已解决）和舒适的间距排版，延续整个平台高大上的第一印象。

---

## 🧪 验证方案

### 自动化与接口验证
* 启动 FastAPI 服务，使用 API 测试或前端面板确认 `GET /api/v1/citizen/reports` 正常拉取 SQLite 数据。
* 确认修改状态的 `POST` 接口可以正确更改 SQLite 中 `status` 字段。

### 交互验证
1. 打开 PC 控制后台，进入 `AI 招投标与空间解析`，输入 `容东片区雄安市民服务中心`，点击解析，验证是否生成 `(115.xxxx, 39.xxxx)` 的脱敏火星坐标。
2. 点击“触发 AI 抓取”，等待 3-4 秒后查看渲染出的 3 条待审核招标信息，点击“一键入库”，检查 PC 地图上是否实时渲染出新的橙红色 Marker。
3. 进入 `市民城管随手拍` Tab，点击一个市民投诉项，验证地图是否平滑飞行并准确定位。

---

> [!IMPORTANT]
> **您需要确认的事项：**
> 该方案会直接打通：**“小程序（市民上报坐标）-> FastAPI（保存工单并处理）-> PC端（管理员查看并定位地图）”** 以及 **“公开网招标 -> Python AI抓取与Geocoding -> PC端审核入库 -> PC及小程序地图同步渲染”** 的全生命周期流水线。
>
> 确认无误请批复“同意”，我将立刻开始为您实现！
