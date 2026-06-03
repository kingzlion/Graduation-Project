import random
import math
import requests
import logging
from agents.config import BAIDU_MAP_AK

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("geocoding_agent")

def bd09_to_gcj02(bd_lon: float, bd_lat: float) -> tuple:
    """
    将百度地图坐标系 (BD-09) 转换为国测局火星坐标系 (GCJ-02)
    这确保了在微信小程序及开源 H5 地图上的精准无偏呈现。
    """
    x_pi = 3.14159265358979324 * 3000.0 / 180.0
    x = bd_lon - 0.0065
    y = bd_lat - 0.006
    z = math.sqrt(x * x + y * y) - 0.00002 * math.sin(y * x_pi)
    theta = math.atan2(y, x) - 0.000003 * math.cos(x * x_pi)
    gcj_lon = z * math.cos(theta)
    gcj_lat = z * math.sin(theta)
    return gcj_lon, gcj_lat

def resolve_address_to_coords(address: str) -> tuple:
    """
    将非结构化文本地址通过百度地图 Geocoding API 解析为经纬度，并转换坐标系、加偏移脱敏。
    返回: (longitude, latitude) 元组
    """
    if not address:
        return None, None

    # 1. 调用百度地图地理编码 API
    if BAIDU_MAP_AK and BAIDU_MAP_AK != "NpRgVsjFQU8cwHM8gXuRbxtpQpYkCZTb_MOCK":
        url = "https://api.map.baidu.com/geocoding/v3/"
        params = {
            "address": address,
            "output": "json",
            "ak": BAIDU_MAP_AK,
            "city": "雄安新区"
        }
        try:
            res = requests.get(url, params=params, timeout=5)
            if res.status_code == 200:
                data = res.json()
                if data.get("status") == 0 and "result" in data:
                    loc = data["result"]["location"]
                    bd_lon = float(loc["lng"])
                    bd_lat = float(loc["lat"])
                    
                    # 坐标系转换：BD-09 -> GCJ-02
                    gcj_lon, gcj_lat = bd09_to_gcj02(bd_lon, bd_lat)
                    
                    # 空间数据脱敏算法：引入微小随机偏移 (约合 100-300米范围波动)
                    lon_obfuscated = gcj_lon + random.uniform(-0.002, 0.002)
                    lat_obfuscated = gcj_lat + random.uniform(-0.002, 0.002)
                    
                    logger.info(f"Baidu Geocode Success: '{address}' -> ({lon_obfuscated:.6f}, {lat_obfuscated:.6f})")
                    return round(lon_obfuscated, 6), round(lat_obfuscated, 6)
                else:
                    logger.warning(f"Baidu Geocode Status Error: {data.get('status')}, msg: {data.get('msg')}")
        except Exception as e:
            logger.error(f"Baidu Geocode Exception: {e}")

    # 2. 备用机制 (如果 API 失败或未配 Key): 智能区域模糊位置打点
    logger.warning(f"Using mockup fallback coordinates for address: '{address}'")
    base_lon = 115.932145
    base_lat = 38.956795
    
    if "启动区" in address:
        base_lon, base_lat = 115.918999, 39.048761
    elif "雄安高铁" in address or "昝岗" in address:
        base_lon, base_lat = 116.154126, 39.053709
    elif "白洋淀" in address:
        base_lon, base_lat = 115.990475, 38.928901
    elif "寨里" in address:
        base_lon, base_lat = 115.787437, 38.955143
    elif "容城" in address:
        base_lon, base_lat = 115.829980, 39.057786
        
    lon_obfuscated = base_lon + random.uniform(-0.005, 0.005)
    lat_obfuscated = base_lat + random.uniform(-0.005, 0.005)
    
    return round(lon_obfuscated, 6), round(lat_obfuscated, 6)
