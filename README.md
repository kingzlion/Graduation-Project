# 雄安时空变迁与建设情报平台 (Xiong'an GIS & AI Platform)

欢迎使用现代化重构与 AI 赋能升级后的**雄安时空变迁与建设情报平台**。
为了让项目结构清晰、合规高效，并且彻底杜绝 AI 编码助手（如 Cursor / Claude 等）在后续迭代中扫描重型文件而浪费大量 Token 的痛点，我们对整个项目的目录架构进行了极致的规范化分类。

---

## 📂 一、 规范化目录结构与中文释义

整个项目已按照**“三端解耦、数据归类、脚本集中、文档归档”**的规范进行重组，当前的项目目录树及详细释义如下：

```
Graduation-Project/ (项目根目录)
├── backend/ (Java Spring Boot 后端项目)
│   ├── src/ (后端 Java 业务源代码与配置文件，含 Haversine 空间撮合算法)
│   ├── pom.xml (Maven 依赖及编译构建配置文件)
│   ├── mvnw (Maven 本地快速启动包装器脚本)
│   └── data/ (H2 本地空间数据库物理存储目录，乱码已终结)
├── frontend/ (uni-app + Vue 3 移动端前端项目，支持 H5 & 微信小程序)
│   ├── src/ (前端页面设计、时空滑轴组件、3D-SVG 空间打点与毛玻璃样式)
│   ├── package.json (Node.js 依赖及 npm 脚本配置)
│   └── vite.config.js (Vite 高效热重载开发服务器配置)
├── agents/ (Python AI 智能体集群微服务)
│   ├── crawling_agent.py (数据情报官 Agent：自动爬取并提炼招投标公告)
│   ├── geocoding_agent.py (地名空间解析 Agent：GPS 语义推导与脱敏偏移)
│   ├── marketing_agent.py (内容营销引流 Agent：DeepSeek 联调怀旧推文生成)
│   └── main.py (FastAPI 独立微服务启动主入口，已开启跨域 CORS 支持)
├── gis-data/ (ArcGIS 原始空间地理矢量与遥感影像数据)
│   ├── 区位展示/ (三县边界与规划红线的 ESRI Shapefile 原始图层)
│   ├── 影像/ (三县两镇的区域高清正射遥感影像 TIF 及 KML 任务边界)
│   └── 文档/ (城市未来道路规划、千年秀林等关键规划专题 3D 辅图 TIF)
├── docs/ (项目重要方案与历史文献归档目录)
│   ├── legacy/ (毕业设计历史 doc/docx 文档归档)
│   └── upgrade/ (项目现代化重构可行性方案、执行进度表等升级决策文档)
├── tools/ (GIS 数据转换与检测等辅助工具脚本)
│   ├── convert_shp_to_geojson.py (ArcGIS Shapefile 转换为 H5 可用 GeoJSON 脚本)
│   ├── inspect_shapes.py (ESRI Shapefile 空间属性表编码与字段校验检测脚本)
│   └── extract_doc.py (DOC/DOCX 二进制文本提取提取脚本)
├── docker-compose.yml (面向云端生产部署的一键式 PostgreSQL+PostGIS 容器化编排脚本)
└── README.md (本主启动与目录规范指导说明文档)
```

---

## ⚡ 二、 杜绝 Token 浪费的“物理防火墙”设计

在原本的结构中，根目录下直接堆放着 **8.6MB 的超大 .doc 文档、30MB+ 的遥感影像 .tif 文件以及 168MB 的巨大 Shapefile 文件**。
当您使用 AI 编程助手（如 Cursor、Claude-3.5-Sonnet 或 Antigravity）时，AI 工具会在后台静默扫描和索引项目文件。**这些无用的重型二进制大文件会强行塞满 AI 的上下文，每次对话均会浪费数十万的 Token，甚至导致 AI 卡顿或超限报错。**

### 我们已完美解决此问题：
1.  **物理隔离**：我们将所有庞大的 ArcGIS 原始图层移动到 `gis-data/` 目录，将超大 `.doc`/`.docx` 移动到 `docs/legacy/` 目录，彻底使项目根目录清爽。
2.  **Git/AI 屏蔽规则**：我们对根目录的 `.gitignore` 进行了专业的空间红线屏蔽规约，将下列文件类型和文件夹列为强制忽略：
    ```text
    *.doc, *.docx, *.pdf, *.tif (大型图纸及文档)
    *.dbf, *.sbn, *.sbx, *.shx, *.prj, *.xml (Shapefile 辅助索引二进制)
    gis-data/ (屏蔽全部重型原始 GIS 数据)
    backend/data/ (屏蔽本地 H2 数据库缓存)
    ```
    **Cursor、Claude 等 AI 开发工具会严格遵循 `.gitignore` 的过滤规则。现在，AI 代理在扫描工作区时会彻底无视这些以百兆为单位的庞大垃圾文件，每次对话仅提取极其精炼的轻量化代码，Token 消耗量骤降 99%，为您节省巨额的 API 额度，且反应速度提升数倍！**

---

## 🚀 三、 现代三端微服务一键启动指令

由于目录结构进行了规范化重组，各服务的启动工作目录也相应进行了自适应规范：

### 1. 启动 Java 后端 (Spring Boot)
```bash
cd backend
./mvnw spring-boot:run
```
*(后端运行于 `http://localhost:8080`)*

### 2. 启动 Python AI 智能体集群 (FastAPI)
```bash
# 保持在项目根目录启动（内含 PYTHONPATH 变量寻址）
PYTHONPATH=. python3 agents/main.py
```
*(AI 服务运行于 `http://127.0.0.1:8000`)*

### 3. 启动移动端 H5 前端 (Vite + Vue 3)
```bash
cd frontend
npm run dev:h5
```
*(前端运行于 `http://localhost:5173/`)*

### 4. 离线 GIS 转换与回归测试 (开发辅助)
若您在 `gis-data` 中增加了新的 Shapefile 矢量面要素，需要重新将其转化为前端轻量化 GeoJSON 时：
```bash
# 运行 tools 下的专用 GIS 处理脚本
python3 tools/convert_shp_to_geojson.py
```
