import requests
import json
import sys

print("================================================================")
print("雄安时空变迁与建设情报平台 - AI Agent 与后端集成联调验证工具")
print("================================================================")

BACKEND_URL = "http://localhost:8080/api"

# 1. 验证后端服务是否在线
try:
    print("正在检测 Spring Boot 后端服务 (localhost:8080)...")
    res = requests.get(f"{BACKEND_URL}/zdgc/list", timeout=3)
    if res.status_code == 200:
        print("✅ 后端在线！已成功连接。")
        projects = res.json().get("data", [])
        print(f"当前数据库中工程项目数量: {len(projects)}")
    else:
        print(f"❌ 后端响应异常: HTTP {res.status_code}")
except Exception as e:
    print(f"❌ 无法连接到 Spring Boot 后端: {e}")
    print("💡 请先启动后端项目 (运行 mvn spring-boot:run 或在IDE中启动 GisApplication)")
    sys.exit(1)

# 2. 模拟 [地名空间解析 Agent] 手动解析
print("\n正在测试 [地名空间解析 Agent] 本地脱敏解析逻辑...")
from geocoding_agent import resolve_address_to_coords
address_to_test = "河北雄安新区启动区东部组团北侧工地"
lon, lat = resolve_address_to_coords(address_to_test)
print(f"输入地址: '{address_to_test}'")
print(f"解析结果: 经度={lon}, 纬度={lat} (已附加微小偏移脱敏)")

# 3. 模拟 [数据情报官 Agent] 自动抓取与上报
print("\n正在测试 [数据情报官 Agent] 模拟抓取与 Spring Boot 后端数据同步流水线...")
from crawling_agent import run_mock_crawl
try:
    synced = run_mock_crawl()
    print(f"🎉 联调同步测试成功！本次成功解析并上报了 {len(synced)} 条数据到 H2 数据库！")
    print("上报同步明细:")
    print(json.dumps(synced, indent=2, ensure_ascii=False))
except Exception as e:
    print(f"❌ 联调数据同步失败: {e}")
print("================================================================")
