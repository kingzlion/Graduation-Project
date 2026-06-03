import os

# ----------------------------------------------------------------
# AI Agent 核心配置文件 (已更新为用户提供的 API Keys)
# ----------------------------------------------------------------

# 雄安 Spring Boot 后端数据同步接口地址
BACKEND_API_BASE = os.environ.get("BACKEND_API_BASE", "http://localhost:8080/api")

# 百度地图开放平台 AK (Baidu Map AK, 用于地理位置解析 Geocoding)
BAIDU_MAP_AK = os.environ.get("BAIDU_MAP_AK", "NpRgVsjFQU8cwHM8gXuRbxtpQpYkCZTb")

# DeepSeek 大模型 API 配置 (用于招标公告自动摘要与文本地名提取)
LLM_API_KEY = os.environ.get("LLM_API_KEY", "sk-8edae599642c4d78ae982dde988f30ce")
LLM_BASE_URL = os.environ.get("LLM_BASE_URL", "https://api.deepseek.com/v1")
LLM_MODEL = os.environ.get("LLM_MODEL", "deepseek-chat")
