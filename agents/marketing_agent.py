import requests
import logging
from config import LLM_API_KEY, LLM_BASE_URL, LLM_MODEL

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("marketing_agent")

def generate_village_marketing_copy(village_name: str, township: str, current_district: str, history: str) -> dict:
    """
    根据被拆迁村庄的历史和信息，调用 DeepSeek 大模型自动生成小红书和抖音视频的怀旧推广文案，
    为 C 端平台零获客成本引流。
    """
    default_xiaohongshu = (
        f"📢【寻找消失的家园：原雄安新区{township}{village_name}的乡亲们，你们在哪？】\n\n"
        f"时间过得真快，转眼间村子已经拆迁多年。曾经的大榕树、村口的小卖部、夏天响个不停的知了……"
        f"都在那场为了新城建设的搬迁中，化作了心底最温暖的记忆。\n\n"
        f"如今，我们搬进了宽敞明亮的{current_district}安置新居。楼房高了，道路宽了，但每当夜深人静，"
        f"脑海里浮现的依然是{village_name}那条长长的黄泥路和邻里间热腾腾的饭菜香。\n\n"
        f"如果你也曾是{village_name}的一员，或者你的记忆里也有这个温暖的老村，"
        f"欢迎点击下方卡片加入我们的【老村数字纪念馆】。上传您手头珍贵的老照片，留下您对老家的寄语吧！\n\n"
        f"我们在新居等你，一起让记忆重现！🌸✨\n"
        f"#雄安变迁 #回迁原住民 #{village_name} #寻根之旅 #留住乡愁"
    )

    default_douyin = (
        f"🎬【抖音/视频号 1分钟怀旧视频脚本】\n\n"
        f"【画面设计】\n"
        f"0-10秒：黑白微光滤镜，缓慢推移雄安老村落{village_name}的旧照片（黄泥路、砖瓦平房）。\n"
        f"10-30秒：平滑过渡到2026年{current_district}拔地而起的宏伟高楼、现代化公园航拍画面。\n"
        f"30-60秒：一张老水井或老树的特写，定格在村民热泪盈眶的留言板上。\n\n"
        f"【配乐建议】\n"
        f"柔和、悠长的木吉他与钢琴纯音乐，如《岁月神偷》伴奏或《送别》钢琴版，逐渐推向温馨高潮。\n\n"
        f"【旁白解说词】\n"
        f"“你还记得，那口冬暖夏凉的老水井吗？\n"
        f"你还记得，夕阳下炊烟袅袅的{village_name}吗？\n"
        f"为了雄安新区的崛起，我们微笑着挥别了祖辈居住的平房，住进了宽敞的{current_district}安置房。\n"
        f"高楼建起来了，但我们的根，还在那片黄土地上。\n"
        f"点击下方，加入属于我们{village_name}的数字家园，上传您的回忆，我们，在记忆里重逢……”"
    )

    # 1. 尝试调用 DeepSeek API 进行智能化高度共鸣的情感文案创作
    if LLM_API_KEY and LLM_API_KEY != "sk-8edae599642c4d78ae982dde988f30ce_MOCK":
        headers = {
            "Authorization": f"Bearer {LLM_API_KEY}",
            "Content-Type": "application/json"
        }
        prompt = (
            "你是一个极富情感温度的雄安本土民生情怀文案策划智能体。请结合以下老村庄的历史资料，"
            "生成两套文案：一是一篇用于分发【小红书】的情感共鸣引流爆款文案；"
            "二是一套用于拍摄【抖音/视频号】1分钟怀旧短视频的脚本（包含画面描述、配乐提示、情感解说旁白词）。\n"
            "文案要求感情细腻，温情脉脉，唤醒村民共鸣，引导他们进入‘老村数字纪念馆’留言寻根。\n\n"
            f"老村名字: {village_name}\n"
            f"原所属乡镇: {township}\n"
            f"回迁安置地: {current_district}\n"
            f"老村庄历史背景: {history}\n\n"
            "请以 JSON 格式输出，格式严格为: "
            "{\"xiaohongshu\": \"小红书正文内容...\", \"douyin\": \"抖音脚本内容...\"}"
        )
        payload = {
            "model": LLM_MODEL,
            "messages": [{"role": "user", "content": prompt}],
            "temperature": 0.7,
            "response_format": {"type": "json_object"}
        }
        try:
            res = requests.post(f"{LLM_BASE_URL}/chat/completions", json=payload, headers=headers, timeout=12)
            if res.status_code == 200:
                result = res.json()["choices"][0]["message"]["content"]
                import json
                parsed = json.loads(result)
                logger.info(f"Successfully generated custom AI marketing copy for {village_name} using DeepSeek.")
                return {
                    "xiaohongshu": parsed.get("xiaohongshu", default_xiaohongshu),
                    "douyin": parsed.get("douyin", default_douyin)
                }
            else:
                logger.error(f"DeepSeek API Error: {res.text}")
        except Exception as e:
            logger.error(f"DeepSeek API Exception: {e}")

    logger.warning("Using pre-configured high-quality templates for marketing copy.")
    return {
        "xiaohongshu": default_xiaohongshu,
        "douyin": default_douyin
    }
