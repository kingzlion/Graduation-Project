import time
import requests
import logging
from config import BACKEND_API_BASE, LLM_API_KEY, LLM_BASE_URL, LLM_MODEL
from geocoding_agent import resolve_address_to_coords

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("crawling_agent")

def run_mock_crawl() -> list:
    """
    模拟抓取雄安最新建设项目招标和新闻公告的数据流水线。
    解析出结构化的地名与内容后，调用地名空间解析 Agent 得到脱敏坐标并上报后端。
    """
    # 模拟从“雄安新区公共资源交易网”抓取到的 3 条最新重大工程招标信息文本
    raw_announcements = [
        {
            "title": "雄安新区启动区东北部居住片区配套绿化工程招标公告",
            "content": "建设地点：河北雄安新区启动区东部组团北侧。建设规模：本绿化项目总占地面积约12.5万平方米，主要包括种植乔木、灌木、绿篱、草坪以及园路铺装、景观小品、绿化灌溉工程。项目总投资约4500万元，资金来源为财政性资金。",
            "source_url": "http://www.xiongan.gov.cn/tenders/2026/05-28/001.html"
        },
        {
            "title": "雄安新区容西片区D组团社区服务中心二次装修工程设计施工总承包",
            "content": "项目概况：容西片区D组团北侧临近容城边界。建设内容为社区服务中心室内装修工程、给排水工程、暖通工程及配套弱电电气工程，总建筑面积为3200平方米。本次招标控制价为780万元。",
            "source_url": "http://www.xiongan.gov.cn/tenders/2026/05-29/002.html"
        },
        {
            "title": "雄安高铁站昝岗组团物流仓储分拨中心基坑开挖与支护工程招标",
            "content": "本工程位于雄安高铁站枢纽昝岗镇东侧。开挖深度约6.5米，主要施工内容包括排桩支护、三轴搅拌桩止水帷幕、土石方开挖及降水。诚邀雄安周边建材与大型塔吊挖机队伍参与分包对接。",
            "source_url": "http://www.xiongan.gov.cn/tenders/2026/05-30/003.html"
        }
    ]

    synced_projects = []

    for ann in raw_announcements:
        logger.info(f"Processing scraped announcement: {ann['title']}")
        
        # 1. 默认降级匹配解析器 (无大模型 Key 时使用): 规则解析
        name = ann["title"]
        content = ann["content"]
        
        # 简单提取建设地址
        address = "雄安新区启动区"
        if "启动区" in content or "启动区" in name:
            address = "河北雄安新区启动区东部组团"
            category = "城市绿化"
        elif "容西" in content or "容西" in name:
            address = "河北雄安新区容西片区D组团"
            category = "市政配套"
        elif "高铁" in content or "昝岗" in name:
            address = "河北雄安新区昝岗镇雄安高铁站东侧"
            category = "交通物流"
        else:
            category = "其他工程"

        # 2. 如果配置了大模型 Key，则调用大模型进行精准摘要和实体抽取
        if LLM_API_KEY and LLM_API_KEY != "YOUR_LLM_API_KEY":
            try:
                # 调用 LLM API 提取结构化数据
                headers = {"Authorization": f"Bearer {LLM_API_KEY}", "Content-Type": "application/json"}
                prompt = (
                    "你是一个地理信息和基建招投标数据清洗智能体。请从以下新闻中提取：\n"
                    "1. 项目规范名称 (name)\n"
                    "2. 详细的物理建设地址 (address)\n"
                    "3. 项目分类 (category - 如 交通物流, 城市绿化, 市政配套 等)\n"
                    "4. 150字以内的一句话核心建设内容摘要 (introduction)\n\n"
                    f"新闻标题: {ann['title']}\n"
                    f"新闻正文: {ann['content']}\n\n"
                    "请以 JSON 格式输出，格式严格为: {\"name\": \"...\", \"address\": \"...\", \"category\": \"...\", \"introduction\": \"...\"}"
                )
                payload = {
                    "model": LLM_MODEL,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.2,
                    "response_format": {"type": "json_object"}
                }
                res = requests.post(f"{LLM_BASE_URL}/chat/completions", json=payload, headers=headers, timeout=10)
                if res.status_code == 200:
                    result = res.json()["choices"][0]["message"]["content"]
                    import json
                    parsed = json.loads(result)
                    name = parsed.get("name", name)
                    address = parsed.get("address", address)
                    category = parsed.get("category", category)
                    content = parsed.get("introduction", content)
                    logger.info("LLM extraction successful.")
            except Exception as e:
                logger.error(f"LLM extraction failed, using default regex parsing. Error: {e}")

        # 3. 调用地名空间解析 Agent (Geocoding + 脱敏) 获取空间经纬度
        lon, lat = resolve_address_to_coords(address)

        if lon and lat:
            # 4. 构建数据模型，同步上报给 Spring Boot 后端
            payload_to_backend = {
                "name": name,
                "category": category,
                "address": address,
                "x": str(lon),
                "y": str(lat),
                "jieshao": content,
                "src": "/images/zdgc/10.jpg",  # 默认配图
                "zoom": 13
            }

            try:
                backend_url = f"{BACKEND_API_BASE}/agent/zdgc"
                response = requests.post(backend_url, json=payload_to_backend, timeout=5)
                if response.status_code == 200:
                    logger.info(f"Successfully synced to Spring Boot backend: {name}")
                    synced_projects.append(payload_to_backend)
                else:
                    logger.error(f"Failed to sync. Backend response: {response.text}")
            except Exception as e:
                logger.error(f"Failed to connect to backend: {e}")
        
        time.sleep(1) # 礼貌限频，防止过快请求

    return synced_projects
