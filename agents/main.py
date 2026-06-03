from fastapi import FastAPI, HTTPException, Query
from pydantic import BaseModel
from typing import Optional, List
import uvicorn
import sqlite3
import json
from datetime import datetime
import asyncpg
import os
import math
import requests

from crawling_agent import run_mock_crawl
from geocoding_agent import resolve_address_to_coords
from marketing_agent import generate_village_marketing_copy

from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(
    title="雄安时空变迁与建设情报平台 AI Agent 集群服务",
    description="基于 FastAPI 与大语言模型，自动监控公开基建招投标与时空变化 data",
    version="1.0.0"
)

# 允许前端跨域访问
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ==============================================================================
# SQLite 数据库初始化与种子数据 (用于本地测试与轻量化 GeoJSON 服务)
# ==============================================================================
DB_PATH = "spatial_villages.db"

def init_db():
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    # 1. 创建村庄时空变迁表
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS spatial_village_history (
        id VARCHAR(64) PRIMARY KEY,
        village_name VARCHAR(255) NOT NULL,
        status INT NOT NULL, -- 0-已拆迁, 1-拆迁中, 2-暂未拆迁/计划保留
        plan_demolition_year INT,
        geom_json TEXT NOT NULL, -- 存储 GeoJSON Polygon coordinates
        history TEXT
    )
    """)
    
    # 2. 创建村庄留言表
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS spatial_village_comments (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        village_id VARCHAR(64) NOT NULL,
        author VARCHAR(255) NOT NULL,
        content TEXT NOT NULL,
        created_at TEXT NOT NULL
    )
    """)
    
    # 4. 创建商户入驻申请表 (本地 SQLite 备份/Fallback)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS applied_merchants (
        id VARCHAR(64) PRIMARY KEY,
        merchant_name VARCHAR(255) NOT NULL,
        business_type VARCHAR(100),
        contact_person VARCHAR(50),
        contact_phone VARCHAR(50),
        address VARCHAR(255),
        x VARCHAR(32),
        y VARCHAR(32),
        service_radius REAL,
        is_vip INT,
        sub_category VARCHAR(50),
        source_region VARCHAR(50)
    )
    """)
    
    # 5. 创建物资求购发布表
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS spatial_demand (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        project_id VARCHAR(64) NOT NULL,
        material_desc TEXT NOT NULL,
        valid_period VARCHAR(100) NOT NULL,
        contact_phone VARCHAR(50) NOT NULL,
        created_at TEXT NOT NULL
    )
    """)
    
    # 6. 创建随手拍（市民城管）工单表
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS citizen_reports (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        issue_type VARCHAR(100) NOT NULL,
        description TEXT NOT NULL,
        reporter_name VARCHAR(100),
        contact_phone VARCHAR(50),
        x VARCHAR(32) NOT NULL,
        y VARCHAR(32) NOT NULL,
        status INT DEFAULT 0, -- 0: 待处理, 1: 处理中, 2: 已解决
        created_at TEXT NOT NULL
    )
    """)
    
    # 7. 创建招商引资地块表
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS investment_parcels (
        id VARCHAR(64) PRIMARY KEY,
        parcel_name VARCHAR(255) NOT NULL,
        land_use VARCHAR(100),
        area_sqm REAL,
        far REAL, -- 容积率 Floor Area Ratio
        geom_json TEXT NOT NULL,
        status INT DEFAULT 0 -- 0: 待出让, 1: 洽谈中, 2: 已出让
    )
    """)
    
    # 8. 创建招商引资意向表
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS investment_intents (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        parcel_id VARCHAR(64) NOT NULL,
        company_name VARCHAR(255) NOT NULL,
        contact_person VARCHAR(50) NOT NULL,
        contact_phone VARCHAR(50) NOT NULL,
        intent_desc TEXT NOT NULL,
        created_at TEXT NOT NULL
    )
    """)
    
    # 3. 检查是否有数据，若没有则注入种子村落面要素
    cursor.execute("SELECT COUNT(*) FROM spatial_village_history")
    if cursor.fetchone()[0] == 0:
        # 定义 5 个分布在雄安核心片区的多边形村落坐标，轻量化防止卡顿
        seed_villages = [
            {
                "id": "v_dawang",
                "village_name": "大王镇旧址",
                "status": 0,
                "plan_demolition_year": 2018,
                "history": "大王镇是雄安新区征迁拆迁的‘第一镇’。2018年开始整体搬迁，原址现已拔地而起建设为容东片区核心商业街与现代化高层回迁住宅小区，承载了数十万回迁居民对过去麦田瓦房的最初回忆。",
                "geom": [[[116.125, 38.985], [116.135, 38.985], [116.135, 38.975], [116.125, 38.975], [116.125, 38.985]]]
            },
            {
                "id": "v_xiaowang",
                "village_name": "小王庄村",
                "status": 0,
                "plan_demolition_year": 2020,
                "history": "小王庄村位于容城县东侧，于2020年完成整体征拆腾退。作为生态绿化建设的重要节点，该区域已蜕变成为绿油油的‘千年秀林’核心林区，鸟语花香，实现了人与自然的生态融合发展。",
                "geom": [[[116.105, 38.970], [116.115, 38.970], [116.115, 38.960], [116.105, 38.960], [116.105, 38.970]]]
            },
            {
                "id": "v_nanzhang",
                "village_name": "南张庄村",
                "status": 0,
                "plan_demolition_year": 2021,
                "history": "南张庄村在2021年雄安大规模安置区建设中进行腾退。如今，原址上建立起了宏伟的容西片区现代化社区，配备了智能化垃圾分类、无障碍人行步道与高水准公立学校，生活品质跨越升级。",
                "geom": [[[116.085, 38.980], [116.095, 38.980], [116.095, 38.970], [116.085, 38.970], [116.085, 38.980]]]
            },
            {
                "id": "v_huyue",
                "village_name": "胡阅村",
                "status": 1,
                "plan_demolition_year": 2024,
                "history": "胡阅村目前正处于紧张的拆迁流转与过渡补偿交房阶段。村民们正分批次搬入崭新的容东安置小区，村内的几棵百年古槐树已被妥善挂牌原地保护，作为村落变迁的活化石予以留存。",
                "geom": [[[116.140, 38.965], [116.150, 38.965], [116.150, 38.955], [116.140, 38.955], [116.140, 38.965]]]
            },
            {
                "id": "v_pingwang",
                "village_name": "平王群落",
                "status": 2,
                "plan_demolition_year": 2030,
                "history": "平王群落属于雄安整体规划中的‘建制保留特色保护村’。暂无拆迁计划，将作为华北传统民居文化记忆示范区，进行风貌修缮与古村落活化开发，打造成文旅融合的网红打卡村。",
                "geom": [[[116.155, 38.985], [116.165, 38.985], [116.165, 38.975], [116.155, 38.975], [116.155, 38.985]]]
            }
        ]
        for v in seed_villages:
            cursor.execute(
                "INSERT INTO spatial_village_history (id, village_name, status, plan_demolition_year, geom_json, history) VALUES (?, ?, ?, ?, ?, ?)",
                (v["id"], v["village_name"], v["status"], v["plan_demolition_year"], json.dumps(v["geom"]), v["history"])
            )
            
        # 4. 插入部分初始留言评论
        seed_comments = [
            ("v_dawang", "回迁户老张", "昔日大王镇的老水井还在吗？大王镇的父老乡亲们如今都住进容东啦，生活环境好太多了，但还是想念当年的炊烟。"),
            ("v_dawang", "雄安新青年", "看着当年大王镇的原址现在建起了这么漂亮的摩天大楼和双向六车道，真的对国家发展充满信心！"),
            ("v_xiaowang", "王秀兰", "我家的旧平房当年就在这片‘千年秀林’原址里。现在经常带孙子来秀林散步，树林茂密，也算是一种另类的团聚。"),
            ("v_nanzhang", "安置新居民", "从南张庄搬到容西已经三年了，小区里全是智能人脸识别和恒温跑道，感谢国家的回迁好政策。")
        ]
        for v_id, author, content in seed_comments:
            cursor.execute(
                "INSERT INTO spatial_village_comments (village_id, author, content, created_at) VALUES (?, ?, ?, ?)",
                (v_id, author, content, datetime.now().isoformat())
            )
            
        # 5. 注入招商引资地块种子数据
        seed_parcels = [
            {
                "id": "parcel_001",
                "parcel_name": "容东商业综合体核心地块 A",
                "land_use": "商业/办公",
                "area_sqm": 25000.5,
                "far": 3.5,
                "geom": [[[116.126, 38.976], [116.128, 38.976], [116.128, 38.974], [116.126, 38.974], [116.126, 38.976]]]
            },
            {
                "id": "parcel_002",
                "parcel_name": "容东滨水科技研发总部用地",
                "land_use": "科研/办公",
                "area_sqm": 42000.0,
                "far": 2.0,
                "geom": [[[116.130, 38.973], [116.133, 38.973], [116.133, 38.971], [116.130, 38.971], [116.130, 38.973]]]
            }
        ]
        for p in seed_parcels:
            cursor.execute(
                "INSERT INTO investment_parcels (id, parcel_name, land_use, area_sqm, far, geom_json, status) VALUES (?, ?, ?, ?, ?, ?, 0)",
                (p["id"], p["parcel_name"], p["land_use"], p["area_sqm"], p["far"], json.dumps(p["geom"]))
            )

        conn.commit()
    conn.close()

# 激活数据库初始化
init_db()

# ==============================================================================
# Pydantic 模型
# ==============================================================================
class AddressRequest(BaseModel):
    address: str

class SyncResult(BaseModel):
    name: str
    category: str
    address: str
    x: str
    y: str
    jieshao: str

class MarketingRequest(BaseModel):
    village_name: str
    township: str
    current_district: str
    history: str

class CommentSubmit(BaseModel):
    author: str
    content: str

class MerchantApplyRequest(BaseModel):
    merchant_name: str
    business_type: str
    sub_category: str
    source_region: str
    contact_person: str
    contact_phone: str
    address: str
    x: str
    y: str
    service_radius: Optional[float] = 15.0

class DemandPublishRequest(BaseModel):
    project_id: str
    material_desc: str
    valid_period: str
    contact_phone: str

class CitizenReportSubmit(BaseModel):
    issue_type: str
    description: str
    reporter_name: Optional[str] = "热心市民"
    contact_phone: Optional[str] = ""
    x: str
    y: str

class InvestmentIntentSubmit(BaseModel):
    parcel_id: str
    company_name: str
    contact_person: str
    contact_phone: str
    intent_desc: str

class CitizenReportStatusUpdate(BaseModel):
    status: int

class SpatialVillageSubmit(BaseModel):
    id: str
    village_name: str
    status: int
    plan_demolition_year: Optional[int] = None
    geom_json: str
    history: Optional[str] = ""

class InvestmentParcelSubmit(BaseModel):
    id: str
    parcel_name: str
    land_use: Optional[str] = ""
    area_sqm: Optional[float] = 0.0
    far: Optional[float] = 0.0
    geom_json: str
    status: Optional[int] = 0

# ==============================================================================
# 既有 AI Agent 核心路由 (保留 100% 原始逻辑)
# ==============================================================================
@app.get("/")
def read_root():
    return {
        "status": "online",
        "service": "Xiongan Spatiotemporal AI Agent Cluster",
        "description": "Running geocoding, crawling, and matching agents."
    }

@app.post("/agent/crawl", response_model=List[SyncResult])
def trigger_crawl():
    try:
        synced = run_mock_crawl()
        return synced
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/agent/geocode")
def geocode_address(address: str = Query(..., description="要解析的空间文字地址")):
    lon, lat = resolve_address_to_coords(address)
    if lon is None or lat is None:
        raise HTTPException(status_code=400, detail="地名无法解析，请确保地址包含有效的地标词汇。")
    return {
        "address": address,
        "longitude": lon,
        "latitude": lat,
        "obfuscated": True,
        "coordinate_system": "GCJ-02"
    }

@app.post("/agent/generate-marketing-copy")
def generate_marketing_copy(req: MarketingRequest):
    try:
        copy_result = generate_village_marketing_copy(
            req.village_name,
            req.township,
            req.current_district,
            req.history
        )
        return copy_result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ==============================================================================
# 2. 新增：时空村落谱 GeoJSON 矢量面接口
# ==============================================================================
@app.get("/api/v1/spatial/villages")
def get_spatial_villages(
    status: Optional[int] = Query(None, description="状态过滤：0-已拆迁，1-拆迁中，2-暂未拆迁/保留"),
    demolition_year: Optional[int] = Query(None, description="预计或实际拆迁年份过滤")
):
    """
    轻量化 GeoJSON 动态输出接口。
    支持状态与拆迁年份双向筛选，专门面向 OpenLayers 时空滑块进行高性能分片渲染。
    """
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    query = "SELECT id, village_name, status, plan_demolition_year, geom_json, history FROM spatial_village_history WHERE 1=1"
    params = []
    
    if status is not None:
        query += " AND status = ?"
        params.append(status)
    if demolition_year is not None:
        query += " AND plan_demolition_year = ?"
        params.append(demolition_year)
        
    cursor.execute(query, params)
    rows = cursor.fetchall()
    conn.close()
    
    # 封装为标准的 GeoJSON FeatureCollection
    features = []
    for r in rows:
        v_id, name, stat, year, geom_str, history = r
        geom = json.loads(geom_str)
        features.append({
            "type": "Feature",
            "id": v_id,
            "geometry": {
                "type": "Polygon",
                "coordinates": geom
            },
            "properties": {
                "id": v_id,
                "village_name": name,
                "status": stat,
                "plan_demolition_year": year,
                "history": history
            }
        })
        
    return {
        "type": "FeatureCollection",
        "features": features
    }

# ==============================================================================
# 3. 新增：时空村落情感留言板接口 (GET / POST)
# ==============================================================================
@app.get("/api/v1/spatial/villages/{village_id}/comments")
def get_village_comments(village_id: str):
    """
    拉取指定村庄面要素的专属回忆留言板列表。
    """
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute(
        "SELECT id, author, content, created_at FROM spatial_village_comments WHERE village_id = ? ORDER BY id DESC",
        (village_id,)
    )
    rows = cursor.fetchall()
    conn.close()
    
    comments_list = []
    for r in rows:
        c_id, author, content, created_at = r
        comments_list.append({
            "id": c_id,
            "author": author,
            "content": content,
            "createdAt": created_at
        })
    return comments_list

@app.post("/api/v1/spatial/villages/{village_id}/comments")
def add_village_comment(village_id: str, req: CommentSubmit):
    """
    向指定拆迁老村落的情感回忆墙提交留言。
    """
    if not req.author.strip() or not req.content.strip():
        raise HTTPException(status_code=400, detail="昵称与评论内容不能为空。")
        
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    created_at = datetime.now().isoformat()
    cursor.execute(
        "INSERT INTO spatial_village_comments (village_id, author, content, created_at) VALUES (?, ?, ?, ?)",
        (village_id, req.author.strip(), req.content.strip(), created_at)
    )
    conn.commit()
    conn.close()
    
    return {"message": "回忆留言已成功留存，寄托思念上墙！", "author": req.author, "content": req.content}

# ==============================================================================
# 4. 新增：大基建商业服务生态中心 (ToB High-Performance Core APIs)
# ==============================================================================

# PostgreSQL Connection String from docker-compose or environment
DB_DSN = os.getenv("DATABASE_URL", "postgresql://gis_user:gis_secure_pwd_2026@localhost:5432/xagis_db")
pg_pool = None

@app.on_event("startup")
async def startup_event():
    global pg_pool
    # Initialize SQLite schema
    init_db()
    # Try initializing PostgreSQL pool
    try:
        pg_pool = await asyncpg.create_pool(dsn=DB_DSN, min_size=1, max_size=5, timeout=5)
        print("✅ PostgreSQL PostGIS connection pool established successfully via asyncpg.")
    except Exception as e:
        print(f"⚠️ PostgreSQL/asyncpg connection failed: {e}. Falling back to high-fidelity simulated services.")
        pg_pool = None

# Fallback helpers fetching live synchronized H2 data from Spring Boot
def get_fallback_projects():
    try:
        res = requests.get("http://localhost:8080/api/zdgc/list")
        if res.status_code == 200:
            data = res.json()
            if data.get("code") == 200:
                raw_list = data.get("data") or []
                enriched = []
                for idx, p in enumerate(raw_list):
                    stage = idx % 4
                    workers = 300 + (int(p["id"]) * 149) % 1800
                    px = float(p["x"])
                    py = float(p["y"])
                    gate_x = px + 0.0012
                    gate_y = py - 0.0008
                    saturation = (idx + 1) % 2
                    enriched.append({
                        "id": p["id"],
                        "name": p["name"],
                        "x": p["x"],
                        "y": p["y"],
                        "approval_stage": stage,
                        "worker_scale_est": workers,
                        "gate_geom": {"x": gate_x, "y": gate_y},
                        "catering_saturation": saturation,
                        "bid_amount": p.get("bidAmount") or 5000,
                        "jieshao": p.get("jieshao") or ""
                    })
                return enriched
    except Exception as e:
        print(f"Error fetching H2 projects: {e}")
    
    # Static mockup seeds as final safety net
    return [
        {
            "id": "1",
            "name": "雄安商服中心核心基建项目",
            "x": "116.1274",
            "y": "38.9751",
            "approval_stage": 0,
            "worker_scale_est": 1200,
            "gate_geom": {"x": 116.1286, "y": 38.9743},
            "catering_saturation": 0,
            "bid_amount": 120000,
            "jieshao": "雄安新区地标级商业综合开发项目"
        },
        {
            "id": "2",
            "name": "起步区第五城市配套综合管廊",
            "x": "116.1012",
            "y": "38.9912",
            "approval_stage": 1,
            "worker_scale_est": 600,
            "gate_geom": {"x": 116.1024, "y": 38.9904},
            "catering_saturation": 1,
            "bid_amount": 45000,
            "jieshao": "地下城市生命线配套管廊施工"
        }
    ]

def get_fallback_merchants():
    fallback_list = []
    # 1. Fetch from Spring Boot H2 list (if alive)
    try:
        res = requests.get("http://localhost:8080/api/merchant/list")
        if res.status_code == 200:
            data = res.json()
            if data.get("code") == 200:
                raw_list = data.get("data") or []
                for idx, m in enumerate(raw_list):
                    cats = ["heavy_machinery", "hardware", "ceylon_bento", "minisuper", "local_labor"]
                    sub_cat = cats[idx % len(cats)]
                    regions = ["雄安本地", "保定仓", "定州基地"]
                    region = regions[idx % len(regions)]
                    fallback_list.append({
                        "id": m["id"],
                        "merchantName": m["merchantName"],
                        "businessType": m["businessType"],
                        "contactPerson": m["contactPerson"],
                        "contactPhone": m["contactPhone"],
                        "address": m["address"],
                        "x": m["x"],
                        "y": m["y"],
                        "serviceRadius": m.get("serviceRadius") or 15,
                        "isVip": m.get("isVip") or False,
                        "sub_category": sub_cat,
                        "source_region": region
                    })
    except Exception as e:
        print(f"Error fetching H2 merchants: {e}")
        # Static seeds if H2 fails
        fallback_list = [
            {
                "id": "m1",
                "merchantName": "雄安宏远重型机械租赁行",
                "businessType": "机械租赁",
                "contactPerson": "张经理",
                "contactPhone": "13900001111",
                "address": "容城县核心工业园北区",
                "x": "116.1320",
                "y": "38.9720",
                "serviceRadius": 20,
                "isVip": True,
                "sub_category": "heavy_machinery",
                "source_region": "雄安本地"
            },
            {
                "id": "m2",
                "merchantName": "保定长城吊装装配基地",
                "businessType": "设备租赁",
                "contactPerson": "李主管",
                "contactPhone": "13811112222",
                "address": "保定市徐水经济开发区",
                "x": "115.8000",
                "y": "38.9000",
                "serviceRadius": 100,
                "isVip": True,
                "sub_category": "heavy_machinery",
                "source_region": "保定仓"
            }
        ]
        
    # 2. Append from local SQLite applied_merchants table
    try:
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        cursor.execute("SELECT id, merchant_name, business_type, contact_person, contact_phone, address, x, y, service_radius, is_vip, sub_category, source_region FROM applied_merchants")
        rows = cursor.fetchall()
        for r in rows:
            m_id, name, b_type, c_person, c_phone, addr, mx, my, rad, vip, sub_cat, reg = r
            fallback_list.append({
                "id": m_id,
                "merchantName": name,
                "businessType": b_type,
                "contactPerson": c_person,
                "contactPhone": c_phone,
                "address": addr,
                "x": mx,
                "y": my,
                "serviceRadius": rad,
                "isVip": bool(vip),
                "sub_category": sub_cat,
                "source_region": reg
            })
        conn.close()
    except Exception as e:
        print(f"Error fetching SQLite applied merchants: {e}")
        
    return fallback_list

# 1. 【工程情报接口】: GET /api/v1/tob/projects/intelligence
@app.get("/api/v1/tob/projects/intelligence")
async def get_projects_intelligence(
    approval_stage: Optional[int] = Query(None, description="审批/建设阶段：0-规划许可, 1-主体施工, 2-验收, 3-完工"),
    total_investment: Optional[str] = Query(None, description="投资规模筛选：billion(百亿级), hundred_million(十亿级)")
):
    """
    ToB 高性能工程情报接口。
    以 Standard GeoJSON 格式返回工地打点，携带审批阶段、常驻工人规模等高端决策字段，支持前端呼吸灯动效与 timeline 绘制。
    """
    if pg_pool:
        try:
            async with pg_pool.acquire() as conn:
                query = """
                    SELECT id, name, x, y, approval_stage, worker_scale_est, catering_saturation, bid_amount, jieshao,
                           ST_AsGeoJSON(COALESCE(gate_geom, ST_SetSRID(ST_Point(CAST(x AS DOUBLE PRECISION), CAST(y AS DOUBLE PRECISION)), 4326))) as gate_geojson
                    FROM ZDGC WHERE 1=1
                """
                params = []
                param_idx = 1
                if approval_stage is not None:
                    query += f" AND approval_stage = ${param_idx}"
                    params.append(approval_stage)
                    param_idx += 1
                if total_investment is not None:
                    if total_investment == "billion":
                        query += f" AND bid_amount >= 100000"
                    elif total_investment == "hundred_million":
                        query += f" AND bid_amount < 100000"
                
                rows = await conn.fetch(query, *params)
                features = []
                for r in rows:
                    gate_geom = json.loads(r["gate_geojson"]) if r["gate_geojson"] else {"type": "Point", "coordinates": [float(r["x"]), float(r["y"])]}
                    features.append({
                        "type": "Feature",
                        "id": r["id"],
                        "geometry": {
                            "type": "Point",
                            "coordinates": [float(r["x"]), float(r["y"])]
                        },
                        "properties": {
                            "id": r["id"],
                            "name": r["name"],
                            "approval_stage": r["approval_stage"],
                            "worker_scale_est": r["worker_scale_est"],
                            "catering_saturation": r["catering_saturation"],
                            "bid_amount": float(r["bid_amount"]) if r["bid_amount"] else 0,
                            "jieshao": r["jieshao"],
                            "gate_geom": gate_geom["coordinates"]
                        }
                    })
                return {"type": "FeatureCollection", "features": features}
        except Exception as e:
            print(f"PostgreSQL fetch failed: {e}, falling back to simulated data.")
            
    # Mock / SQLite Fallback
    features = []
    projects_list = get_fallback_projects()
    for p in projects_list:
        # Filter logic
        if approval_stage is not None and p["approval_stage"] != approval_stage:
            continue
        if total_investment is not None:
            if total_investment == "billion" and p["bid_amount"] < 100000:
                continue
            if total_investment == "hundred_million" and p["bid_amount"] >= 100000:
                continue
                
        features.append({
            "type": "Feature",
            "id": p["id"],
            "geometry": {
                "type": "Point",
                "coordinates": [float(p["x"]), float(p["y"])]
            },
            "properties": {
                "id": p["id"],
                "name": p["name"],
                "approval_stage": p["approval_stage"],
                "worker_scale_est": p["worker_scale_est"],
                "catering_saturation": p["catering_saturation"],
                "bid_amount": float(p["bid_amount"]),
                "jieshao": p["jieshao"],
                "gate_geom": [p["gate_geom"]["x"], p["gate_geom"]["y"]]
            }
        })
    return {"type": "FeatureCollection", "features": features}

# 2. 【供应链雷达接口】: GET /api/v1/tob/spatial/radar
@app.get("/api/v1/tob/spatial/radar")
async def get_supply_chain_radar(
    project_id: str = Query(..., description="目标工地项目ID"),
    radius_meters: float = Query(3000, description="空间检索半径(米)"),
    sub_category: str = Query("all", description="商户分类筛选: all(全部), heavy_machinery, hardware, ceylon_bento, minisuper, local_labor")
):
    """
    ToB 高性能供应链雷达空间计算接口。
    若 PostgreSQL/PostGIS 可用，利用 ST_DWithin 进行 GIST 空间检索；
    否则利用 GCJ-02 坐标进行高拟真度的数学欧几里得距离计算。
    """
    if pg_pool:
        try:
            async with pg_pool.acquire() as conn:
                # 取得项目的大门坐标
                query = """
                    WITH p_gate AS (
                      SELECT COALESCE(gate_geom, ST_SetSRID(ST_Point(CAST(x AS DOUBLE PRECISION), CAST(y AS DOUBLE PRECISION)), 4326)) as geom
                      FROM ZDGC WHERE id = $1
                    )
                    SELECT id, merchant_name as "merchantName", business_type as "businessType", 
                           contact_person as "contactPerson", contact_phone as "contactPhone", address, 
                           POINT_X as x, POINT_Y as y, service_radius as "serviceRadius", is_vip as "isVip", 
                           sub_category, source_region,
                           ST_Distance(location, (SELECT geom FROM p_gate)) * 111000.0 as distance_meters
                    FROM SUPPLY_MERCHANT, p_gate
                    WHERE ST_DWithin(location, (SELECT geom FROM p_gate), $2 / 111000.0)
                      AND ($3 = 'all' OR sub_category = $3)
                    ORDER BY is_vip DESC, distance_meters ASC
                """
                rows = await conn.fetch(query, project_id, radius_meters, sub_category)
                merchants_list = [dict(r) for r in rows]
                return {
                    "project_id": project_id,
                    "radius_meters": radius_meters,
                    "sub_category": sub_category,
                    "merchants": merchants_list
                }
        except Exception as e:
            print(f"PostgreSQL radar query failed: {e}, falling back to simulated logic.")
            
    # Mock / SQLite Fallback
    projects_list = get_fallback_projects()
    project = next((p for p in projects_list if p["id"] == project_id), None)
    if not project:
        raise HTTPException(status_code=404, detail="指定项目不存在")
        
    gate_x = project["gate_geom"]["x"]
    gate_y = project["gate_geom"]["y"]
    
    merchants_list = get_fallback_merchants()
    nearby = []
    for m in merchants_list:
        # Filter sub_category
        if sub_category != "all" and m["sub_category"] != sub_category:
            continue
            
        # Math distance approximation (1 degree ≈ 111,000 meters)
        mx = float(m["x"])
        my = float(m["y"])
        dist_deg = math.sqrt((mx - gate_x)**2 + (my - gate_y)**2)
        dist_meters = dist_deg * 111000.0
        
        if dist_meters <= radius_meters:
            nearby.append({
                "id": m["id"],
                "merchantName": m["merchantName"],
                "businessType": m["businessType"],
                "contactPerson": m["contactPerson"],
                "contactPhone": m["contactPhone"],
                "address": m["address"],
                "x": m["x"],
                "y": m["y"],
                "serviceRadius": m["serviceRadius"],
                "isVip": m["isVip"],
                "sub_category": m["sub_category"],
                "source_region": m["source_region"],
                "distance_meters": round(dist_meters, 2)
            })
            
    # Sort by VIP desc, distance asc
    nearby.sort(key=lambda x: (not x["isVip"], x["distance_meters"]))
    return {
        "project_id": project_id,
        "radius_meters": radius_meters,
        "sub_category": sub_category,
        "merchants": nearby
    }

# 3. 【工地配套圈接口】: GET /api/v1/tob/projects/living-circle
@app.get("/api/v1/tob/projects/living-circle")
async def get_project_living_circle(project_id: str = Query(..., description="目标工地项目ID")):
    """
    ToB 高性能生活配套区数据接口。
    返回工地常驻人数、大门坐标，以及实时模拟的项目部后勤求购/灵工广播内容，赋能工地快餐与平价商超精准营销。
    """
    projects_list = get_fallback_projects()
    project = next((p for p in projects_list if p["id"] == project_id), None)
    if not project:
        raise HTTPException(status_code=404, detail="指定项目不存在")
        
    custom_broadcasts = []
    try:
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        cursor.execute(
            "SELECT id, material_desc, valid_period, contact_phone, created_at FROM spatial_demand WHERE project_id = ? ORDER BY id DESC",
            (project_id,)
        )
        rows = cursor.fetchall()
        conn.close()
        for idx, r in enumerate(rows):
            d_id, desc, period, phone, created_at = r
            custom_broadcasts.append({
                "id": f"c_{d_id}",
                "type": "materials",
                "content": f"📢 紧急求购：{desc}。账期要求: {period}，联系电话: {phone}！",
                "time": "刚刚"
            })
    except Exception as e:
        print(f"Error loading custom demands from SQLite: {e}")

    # Generate mock purchase/labor broadcasts based on project properties
    broadcasts = [
        {
            "id": 1,
            "type": "catering",
            "content": f"🍱 紧急求购：项目部订购今日中午 {project['worker_scale_est'] // 3} 份工地大锅盒饭，要求11:20送达大门处！",
            "time": "15分钟前"
        },
        {
            "id": 2,
            "type": "materials",
            "content": "🦺 物资采购：二期施工段急需 300 套加厚安全帽与防刺工作鞋，有保定仓或本地货源者速联系！",
            "time": "1小时前"
        },
        {
            "id": 3,
            "type": "labor",
            "content": "🏗️ 灵工招募：由于大干冲刺，项目部急招 20 名熟练钢筋工，日结薪资 350-400 元/天，提供宿舍！",
            "time": "2小时前"
        },
        {
            "id": 4,
            "type": "grocery",
            "content": "🛒 货架求购：项目生活区求购 5 台二手冷饮冰柜与大量消暑饮料、毛巾，支持送货进场。",
            "time": "3小时前"
        }
    ]
    
    return {
        "project_id": project["id"],
        "project_name": project["name"],
        "worker_scale_est": project["worker_scale_est"],
        "catering_saturation": project["catering_saturation"],
        "gate_geom": [project["gate_geom"]["x"], project["gate_geom"]["y"]],
        "broadcasts": custom_broadcasts + broadcasts
    }

# ==============================================================================
# 5. ToB Commercial Endpoints: Merchant Apply & Demand Publish
# ==============================================================================
@app.post("/api/v1/tob/merchant/apply")
async def apply_merchant(req: MerchantApplyRequest):
    if not req.merchant_name.strip() or not req.contact_phone.strip():
        raise HTTPException(status_code=400, detail="企业名称与联系电话不能为空。")
    
    m_id = f"m_{int(datetime.now().timestamp())}"
    
    # 1. Write to local SQLite database
    try:
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        cursor.execute("""
            INSERT INTO applied_merchants (id, merchant_name, business_type, contact_person, contact_phone, address, x, y, service_radius, is_vip, sub_category, source_region)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (m_id, req.merchant_name.strip(), req.business_type.strip(), req.contact_person.strip(), req.contact_phone.strip(), req.address.strip(), req.x.strip(), req.y.strip(), req.service_radius, 0, req.sub_category, req.source_region))
        conn.commit()
        conn.close()
    except Exception as e:
        print(f"SQLite merchant apply write failed: {e}")
        raise HTTPException(status_code=500, detail=f"本地存储写入失败: {str(e)}")

    # 2. Write to PostgreSQL if active
    if pg_pool:
        try:
            async with pg_pool.acquire() as conn:
                query = """
                    INSERT INTO SUPPLY_MERCHANT (id, merchant_name, business_type, contact_person, contact_phone, address, POINT_X, POINT_Y, service_radius, is_vip, sub_category, source_region, location)
                    VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, ST_SetSRID(ST_Point(CAST($7 AS DOUBLE PRECISION), CAST($8 AS DOUBLE PRECISION)), 4326))
                """
                await conn.execute(query, m_id, req.merchant_name.strip(), req.business_type.strip(), req.contact_person.strip(), req.contact_phone.strip(), req.address.strip(), req.x.strip(), req.y.strip(), req.service_radius, False, req.sub_category, req.source_region)
                print(f"✅ Successfully inserted merchant {req.merchant_name} into PostgreSQL.")
        except Exception as e:
            print(f"⚠️ PostgreSQL merchant apply write failed: {e}")

    # 3. Synchronize to Spring Boot H2 if available
    try:
        sb_payload = {
            "id": m_id,
            "merchantName": req.merchant_name.strip(),
            "businessType": req.business_type.strip(),
            "contactPerson": req.contact_person.strip(),
            "contactPhone": req.contact_phone.strip(),
            "address": req.address.strip(),
            "x": req.x.strip(),
            "y": req.y.strip(),
            "serviceRadius": req.service_radius,
            "isVip": False
        }
        res = requests.post("http://localhost:8080/api/merchant/register", json=sb_payload, timeout=2)
        if res.status_code == 200:
            print("✅ Successfully synchronized merchant to Spring Boot H2.")
    except Exception as e:
        print(f"⚠️ Failed to synchronize merchant to Spring Boot: {e}")

    return {"message": "商户自主入驻申请提交成功！", "merchant_id": m_id}

@app.post("/api/v1/tob/demand/publish")
async def publish_demand(req: DemandPublishRequest):
    if not req.project_id.strip() or not req.material_desc.strip() or not req.contact_phone.strip():
        raise HTTPException(status_code=400, detail="工程项目、急需物资描述以及联系电话不能为空。")
    
    created_at = datetime.now().isoformat()
    
    # Write to local SQLite database
    try:
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        cursor.execute("""
            INSERT INTO spatial_demand (project_id, material_desc, valid_period, contact_phone, created_at)
            VALUES (?, ?, ?, ?, ?)
        """, (req.project_id.strip(), req.material_desc.strip(), req.valid_period.strip(), req.contact_phone.strip(), created_at))
        conn.commit()
        conn.close()
    except Exception as e:
        print(f"SQLite demand publish failed: {e}")
        raise HTTPException(status_code=500, detail=f"求购数据保存失败: {str(e)}")
        
    return {"message": "物资求购发布成功，已实时广播至工地生活圈看板！"}

# ==============================================================================
# 6. 面向公众 (C端) 与 招商引资 (B端) 拓展接口
# ==============================================================================

@app.post("/api/v1/citizen/report")
async def submit_citizen_report(req: CitizenReportSubmit):
    created_at = datetime.now().isoformat()
    try:
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        cursor.execute("""
            INSERT INTO citizen_reports (issue_type, description, reporter_name, contact_phone, x, y, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """, (req.issue_type, req.description, req.reporter_name, req.contact_phone, req.x, req.y, created_at))
        conn.commit()
        conn.close()
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"上报失败: {str(e)}")
        
    return {"message": "随手拍上报成功，感谢您为智慧城市建设贡献力量！"}

@app.get("/api/v1/citizen/reports")
async def get_citizen_reports():
    try:
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        cursor.execute("SELECT id, issue_type, description, reporter_name, contact_phone, x, y, status, created_at FROM citizen_reports ORDER BY id DESC")
        rows = cursor.fetchall()
        conn.close()
        return [
            {
                "id": r[0],
                "issue_type": r[1],
                "description": r[2],
                "reporter_name": r[3],
                "contact_phone": r[4],
                "x": r[5],
                "y": r[6],
                "status": r[7],
                "created_at": r[8]
            }
            for r in rows
        ]
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/v1/citizen/reports/{report_id}/status")
async def update_citizen_report_status(report_id: int, req: CitizenReportStatusUpdate):
    try:
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        cursor.execute("UPDATE citizen_reports SET status = ? WHERE id = ?", (req.status, report_id))
        conn.commit()
        conn.close()
        return {"message": "工单状态更新成功！"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/v1/investment/parcels")
async def get_investment_parcels():
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("SELECT id, parcel_name, land_use, area_sqm, far, geom_json, status FROM investment_parcels")
    rows = cursor.fetchall()
    conn.close()
    
    features = []
    for r in rows:
        p_id, name, use, area, far, geom_str, status = r
        geom = json.loads(geom_str)
        features.append({
            "type": "Feature",
            "id": p_id,
            "geometry": {
                "type": "Polygon",
                "coordinates": geom
            },
            "properties": {
                "id": p_id,
                "parcel_name": name,
                "land_use": use,
                "area_sqm": area,
                "far": far,
                "status": status
            }
        })
    return {"type": "FeatureCollection", "features": features}

@app.post("/api/v1/investment/intent")
async def submit_investment_intent(req: InvestmentIntentSubmit):
    created_at = datetime.now().isoformat()
    try:
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        cursor.execute("""
            INSERT INTO investment_intents (parcel_id, company_name, contact_person, contact_phone, intent_desc, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """, (req.parcel_id.strip(), req.company_name.strip(), req.contact_person.strip(), req.contact_phone.strip(), req.intent_desc.strip(), created_at))
        conn.commit()
        conn.close()
        return {"message": f"已收到贵司【{req.company_name}】的投资意向，招商专员将在 24 小时内与您联系！"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ==============================================================================
# 7. 新增：时空老村落/招商引资管理后台 CRUD 接口
# ==============================================================================
@app.post("/api/v1/spatial/villages")
async def save_spatial_village(req: SpatialVillageSubmit):
    try:
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        # Check if village exists
        cursor.execute("SELECT id FROM spatial_village_history WHERE id = ?", (req.id.strip(),))
        exists = cursor.fetchone()
        if exists:
            cursor.execute("""
                UPDATE spatial_village_history 
                SET village_name = ?, status = ?, plan_demolition_year = ?, geom_json = ?, history = ?
                WHERE id = ?
            """, (req.village_name.strip(), req.status, req.plan_demolition_year, req.geom_json.strip(), req.history.strip(), req.id.strip()))
        else:
            cursor.execute("""
                INSERT INTO spatial_village_history (id, village_name, status, plan_demolition_year, geom_json, history)
                VALUES (?, ?, ?, ?, ?, ?)
            """, (req.id.strip(), req.village_name.strip(), req.status, req.plan_demolition_year, req.geom_json.strip(), req.history.strip()))
        conn.commit()
        conn.close()
        return {"message": "村落空间数据保存成功！"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.delete("/api/v1/spatial/villages/{village_id}")
async def delete_spatial_village(village_id: str):
    try:
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        cursor.execute("DELETE FROM spatial_village_history WHERE id = ?", (village_id,))
        conn.commit()
        conn.close()
        return {"message": "村落空间数据已物理删除。"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/v1/investment/parcels")
async def save_investment_parcel(req: InvestmentParcelSubmit):
    try:
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        # Check if parcel exists
        cursor.execute("SELECT id FROM investment_parcels WHERE id = ?", (req.id.strip(),))
        exists = cursor.fetchone()
        if exists:
            cursor.execute("""
                UPDATE investment_parcels 
                SET parcel_name = ?, land_use = ?, area_sqm = ?, far = ?, geom_json = ?, status = ?
                WHERE id = ?
            """, (req.parcel_name.strip(), req.land_use.strip(), req.area_sqm, req.far, req.geom_json.strip(), req.status, req.id.strip()))
        else:
            cursor.execute("""
                INSERT INTO investment_parcels (id, parcel_name, land_use, area_sqm, far, geom_json, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """, (req.id.strip(), req.parcel_name.strip(), req.land_use.strip(), req.area_sqm, req.far, req.geom_json.strip(), req.status))
        conn.commit()
        conn.close()
        return {"message": "招商引资地块保存成功！"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.delete("/api/v1/investment/parcels/{parcel_id}")
async def delete_investment_parcel(parcel_id: str):
    try:
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        cursor.execute("DELETE FROM investment_parcels WHERE id = ?", (parcel_id,))
        conn.commit()
        conn.close()
        return {"message": "招商地块数据已物理删除。"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/v1/investment/intents")
async def get_investment_intents():
    try:
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        cursor.execute("""
            SELECT ii.id, ii.parcel_id, ii.company_name, ii.contact_person, ii.contact_phone, ii.intent_desc, ii.created_at, ip.parcel_name
            FROM investment_intents ii
            LEFT JOIN investment_parcels ip ON ii.parcel_id = ip.id
            ORDER BY ii.id DESC
        """)
        rows = cursor.fetchall()
        conn.close()
        return [
            {
                "id": r[0],
                "parcel_id": r[1],
                "company_name": r[2],
                "contact_person": r[3],
                "contact_phone": r[4],
                "intent_desc": r[5],
                "created_at": r[6],
                "parcel_name": r[7] or r[1]
            } for r in rows
        ]
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.delete("/api/v1/investment/intents/{intent_id}")
async def delete_investment_intent(intent_id: int):
    try:
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        cursor.execute("DELETE FROM investment_intents WHERE id = ?", (intent_id,))
        conn.commit()
        conn.close()
        return {"message": "投资意向记录已删除。"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/v1/cultural/checkin")
async def checkin_cultural_spot(spot_id: str = Query(...)):
    # 模拟打卡与红包发放
    import random
    coupons = ["满50减10快餐券", "免费咖啡兑换券", "超市9折通用券"]
    return {
        "message": "打卡成功！",
        "coupon": random.choice(coupons)
    }

if __name__ == "__main__":
    uvicorn.run("main:app", host="127.0.0.1", port=8000, reload=True)
