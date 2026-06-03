# 后台管理系统功能拓展任务清单

- [x] 1. 修改 Python FastAPI 后端 (`agents/main.py`)
  - [x] 1.1 增加 `CitizenReportStatusUpdate` 数据模型
  - [x] 1.2 增加 `GET /api/v1/citizen/reports` 接口
  - [x] 1.3 增加 `POST /api/v1/citizen/reports/{report_id}/status` 接口
- [x] 2. 修改 PC 前端 (`frontend-pc/src/App.vue`)
  - [x] 2.1 在管理员控制台界面增加两个新 Tab (`ai-agent` 与 `citizen-reports`)
  - [x] 2.2 实现 “AI 招投标与空间解析” 面板：
    - [x] 包含 “触发 AI 抓取” 按钮及状态
    - [x] 展示抓取出的待审核 Leads 列表与编辑表单
    - [x] 实现一键审核上架逻辑 (调用 Java `/manage/zdgc`)
    - [x] 包含 “在线地理编码 (Geocoding) 沙盒” 工具
  - [x] 2.3 实现 “市民城管随手拍” 面板：
    - [x] 渲染市民反馈工单列表
    - [x] 实现状态切换按钮及 SQLite 状态更新调用
    - [x] 实现点击工单后，地图飞行平滑定位并闪烁标记的联动交互
- [x] 3. 系统联调与推送
  - [x] 3.1 运行测试验证抓取、解析、审核、市民定位的完整链路
  - [x] 3.2 提交最新代码并同步到 GitHub 仓库
