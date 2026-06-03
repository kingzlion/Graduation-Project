package com.example.gis.utils;

import com.example.gis.entity.Rwjg;
import com.example.gis.entity.SupplyMerchant;
import com.example.gis.entity.Zdgc;
import com.example.gis.entity.NostalgiaComment;
import com.example.gis.repository.NostalgiaCommentRepository;
import com.example.gis.repository.SupplyMerchantRepository;
import com.example.gis.service.AuthService;
import com.example.gis.service.ManagementService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AuthService authService;
    private final ManagementService managementService;
    private final SupplyMerchantRepository supplyMerchantRepository;
    private final NostalgiaCommentRepository nostalgiaCommentRepository;

    public DataInitializer(AuthService authService,
                           ManagementService managementService,
                           SupplyMerchantRepository supplyMerchantRepository,
                           NostalgiaCommentRepository nostalgiaCommentRepository) {
        this.authService = authService;
        this.managementService = managementService;
        this.supplyMerchantRepository = supplyMerchantRepository;
        this.nostalgiaCommentRepository = nostalgiaCommentRepository;
    }

    @Override
    public void run(String... args) {
        authService.ensureDefaultAdmin();
        if (managementService.listRwjg().isEmpty()) {
            seedRwjg();
        }
        if (managementService.listZdgc().isEmpty()) {
            seedZdgc();
        }
        if (supplyMerchantRepository.count() == 0) {
            seedSupplyMerchants();
        }
        if (nostalgiaCommentRepository.count() == 0) {
            seedComments();
        }
    }

    private void seedRwjg() {
        saveRwjg("1", "陈子正故居", "陈子正故居年代为1905年，位于保定市雄县昝岗镇李林庄村。 2008年10月20日陈子正故居被河北省人民政府公布为第五批省级文物保护单位。", "116.17191822200", "39.03218440900", "/images/rwjg/1.jpg");
        saveRwjg("2", "山西村砖塔", "山西村砖塔为河北省文物保护单位。 山西村砖塔为明代古建筑，位于三台镇山西村。 2008年10月20日山西村砖塔被河北省人民政府公布为第五批省级文物保护单位。", "115.82586460600", "38.97529312000", "/images/rwjg/2.jpg");
        saveRwjg("3", "白洋淀雁翎队纪念馆", "位于华北明珠、中国5A级旅游景区白洋淀的雁翎队纪念馆，近日被河北省委、省政府命名为第三批省级爱国主义基地。", "115.97927367300", "38.91565019700", "/images/rwjg/3.jpg");
        saveRwjg("4", "容城晾马台遗址", "晾马台遗址是商周时代遗址, 位于容城县城东17公里的晾马台乡晾马台村。", "115.99878125600", "39.07132793700", "/images/rwjg/4.jpg");
        saveRwjg("5", "明月禅寺", "纪念馆南侧为梯形，北侧为三角形，保存完好。", "115.99311813900", "39.07128034800", "/images/rwjg/5.jpg");
        saveRwjg("6", "安新净业禅寺", "河北省安新县三台镇净业禅寺始建于元代大德年间。", "115.84148327400", "38.96942856700", "/images/rwjg/6.jpg");
        saveRwjg("7", "白洋淀景区", "白洋淀位于河北保定境内，为华北平原最大的淡水湖。", "115.99047560400", "38.92890158700", "/images/rwjg/7.jpg");
        saveRwjg("8", "南阳遗址", "南阳遗址位于容城县城东十三公里，为周代时期遗存。", "115.99822463700", "39.04164393700", "/images/rwjg/8.jpg");
        saveRwjg("9", "荷花大观园", "白洋淀荷花大观园座落在孙犁笔下的荷花淀。", "115.99293652300", "38.92230159400", "/images/rwjg/9.jpg");
        saveRwjg("10", "安州天宁寺", "安州天宁寺始建于唐代，近几年正在着手修缮、扩建。", "115.88262702600", "38.89363296600", "/images/rwjg/10.jpg");
    }

    // ===================== ZDGC 种子数据 (FR-2.1 全生命周期) =====================

    private void seedZdgc() {
        // 已交付成果 🟢
        saveZdgc("1",  "雄安市民服务中心",     "115.9073972640", "39.0484948000", "雄安市民服务中心位于雄安新区容城东部，是雄安新区首个城建项目。", 9,
                "completed", "中建三局",        new BigDecimal("15800"), "已交付运营，入驻企业300+家",     LocalDate.of(2020,1,1),  LocalDate.of(2022,12,31), "市政配套");
        saveZdgc("2",  "雄安高铁站",           "116.1541262800", "39.0537091290", "雄安站是雄安新区开工建设的第一个重大基础设施项目。", 9,
                "completed", "中铁建工集团",    new BigDecimal("28600"), "已通车运营，日均客流量超2万人次", LocalDate.of(2019,1,1),  LocalDate.of(2023,6,30),  "交通物流");
        saveZdgc("3",  "三校工程",             "115.9383309760", "39.0287977710", "北京市援建雄安新区三所学校：北海幼儿园、史家胡同小学、北京四中。", 18,
                "completed", "北京城建集团",    new BigDecimal("12500"), "已交付使用，2023年秋季开学",      LocalDate.of(2020,6,1),  LocalDate.of(2023,8,31),  "市政配套");
        saveZdgc("4",  "容东安置区（一期）",    "115.9189995150", "39.0487613000", "容东片区是雄安新区首个大规模建成区，承担首批征迁群众安置任务。", 8,
                "completed", "中国雄安集团",    new BigDecimal("45000"), "已交付入住，安置居民3万余人",    LocalDate.of(2019,9,1),  LocalDate.of(2023,12,31), "安置房");

        // 火热在建中 🟡
        saveZdgc("5",  "雄安城际站",           "115.9366770070", "39.0053980020", "雄安城际站位于安新县小王营村庄东侧，是雄安新区[四纵两横]高铁网的关键节点。", 14,
                "under_construction", "中铁十二局", new BigDecimal("52300"), "站房主体结构完成70%，预计2026年竣工", LocalDate.of(2024,1,1),  LocalDate.of(2026,12,31), "交通物流");
        saveZdgc("6",  "雄安商务服务中心",      "115.9073972220", "39.0533926410", "雄安商务服务中心是雄安新区首个标志性城市建筑群，集会展、办公、商业于一体。", 15,
                "under_construction", "中建八局", new BigDecimal("68200"), "二期幕墙安装中，一期已投入运营",  LocalDate.of(2022,6,1),  LocalDate.of(2025,12,31), "市政配套");
        saveZdgc("7",  "一院工程（宣武医院）",   "115.9717718740", "39.0280280150", "雄安宣武医院是雄安新区第一所大型三级甲等医院，北京以交钥匙方式建设。", 16,
                "under_construction", "北京建工集团", new BigDecimal("21500"), "一期已开诊，二期主体施工中",    LocalDate.of(2021,3,1),  LocalDate.of(2025,6,30),  "市政配套");
        saveZdgc("8",  "昝岗片区基础设施建设",   "116.1705445610", "39.0430015540", "昝岗片区作为雄安站枢纽配套，重点发展高端高新产业。", 8,
                "under_construction", "中交一公局", new BigDecimal("38500"), "道路管网完成60%，220千伏变电站已投运", LocalDate.of(2023,1,1), LocalDate.of(2026,6,30), "城市绿化");
        saveZdgc("9",  "萍河左堤生态修复工程",   "115.7938758840", "38.9849427060", "萍河左堤生态修复是雄安新区环起步区生态防洪堤的重要组成部分。", 9,
                "under_construction", "中国电建",   new BigDecimal("8900"), "堤防加高培厚完成85%，生态景观带施工中", LocalDate.of(2023,6,1), LocalDate.of(2025,9,30), "城市绿化");
        saveZdgc("10", "安新组团综合开发",       "115.9263977360", "38.9224024030", "安新县城现有人口5万余人，未来将增加到10万余人，打造宜居宜业综合城区。", 10,
                "under_construction", "中国建筑",   new BigDecimal("32000"), "首批安置房主体封顶，公共设施同步建设", LocalDate.of(2023,3,1), LocalDate.of(2026,12,31), "安置房");
        saveZdgc("11", "寨里组团启动区",         "115.7874372130", "38.9551437860", "寨里组团位于起步区西南侧，规划容纳18万人，是新区重要的居住功能区。", 10,
                "under_construction", "雄安集团城发公司", new BigDecimal("28000"), "土地整理完成，基础施工启动",  LocalDate.of(2024,6,1),  LocalDate.of(2027,12,31), "安置房");
        saveZdgc("12", "寨里混凝土搅拌站项目",    "115.8323122880", "38.9880844190", "主要建设6条240型混凝土生产线，服务起步区工程建设。", 10,
                "under_construction", "河北建设集团", new BigDecimal("5600"), "设备安装调试中，预计2025年投产", LocalDate.of(2024,3,1),  LocalDate.of(2025,6,30), "市政配套");

        // 规划征迁中 🔴
        saveZdgc("13", "容西安置区（二期）",     "115.8299802320", "39.0577864240", "容西片区二期安置房项目，涵盖住宅、学校、商业配套。", 13,
                "planning", "中国雄安集团",     new BigDecimal("56000"), "规划方案已批复，征地拆迁进行中",    LocalDate.of(2025,6,1),  LocalDate.of(2028,12,31), "安置房");
        saveZdgc("14", "安州特色小镇",          "115.8255085890", "38.8717674720", "安州是雄安新区设立以来第一个要开始建设的特色小镇，以文旅康养为特色。", 12,
                "planning", "华侨城集团",       new BigDecimal("18000"), "控详规已公示，土地收储阶段",       LocalDate.of(2025,9,1),  LocalDate.of(2028,6,30), "市政配套");
        saveZdgc("15", "晾马台特色小镇",        "116.0004130990", "39.0760760620", "晾马台特色小城镇控制性详细规划已现场公示，以文化创意为产业方向。", 10,
                "planning", "华夏幸福基业",     new BigDecimal("15000"), "规划方案优化中，启动区待征迁",     LocalDate.of(2025,12,1), LocalDate.of(2029,6,30), "市政配套");
        saveZdgc("16", "留通片区征迁安置工程",   "116.0038871120", "38.9858202510", "留通片区征迁安置工作涉及3个行政村，约2000户居民。", 8,
                "planning", "雄安集团城发公司",  new BigDecimal("22000"), "征迁安置方案制定中，入户调查完成",  LocalDate.of(2025,4,1),  LocalDate.of(2027,12,31), "安置房");
        saveZdgc("17", "白洋淀生态缓冲区（北）", "115.9792736730", "38.9156501970", "白洋淀北部生态缓冲区建设，包含湿地修复、水系连通、生物多样性保护。", 10,
                "planning", "中国环境科学研究院", new BigDecimal("9500"), "可研报告编制中，生态本底调查完成",  LocalDate.of(2025,8,1),  LocalDate.of(2028,12,31), "城市绿化");
        saveZdgc("18", "昝岗高新产业园区二期",   "116.1705445610", "39.0430015540", "昝岗高新产业园区二期规划以新一代信息技术、高端装备制造为主导。", 8,
                "planning", "招商局集团",       new BigDecimal("42000"), "产业规划编制中，土地指标待落实",    LocalDate.of(2026,3,1),  LocalDate.of(2029,12,31), "市政配套");
        saveZdgc("19", "孝义河生态廊道治理",     "115.8414832740", "38.9694285670", "孝义河是白洋淀主要入淀河流之一，生态廊道治理将提升入淀水质。", 9,
                "planning", "中电建生态环境集团", new BigDecimal("7800"), "前期勘测设计阶段，已列入新区重点项目清单", LocalDate.of(2025,10,1), LocalDate.of(2027,12,31), "城市绿化");
        saveZdgc("20", "安新县城市更新（三期）",  "115.9263977360", "38.9224024030", "安新县老城区城市更新项目，涵盖管网改造、街道提升、老旧小区改造。", 10,
                "planning", "中国建筑",          new BigDecimal("12000"), "居民意见征集中，改造方案初步设计",  LocalDate.of(2025,7,1),  LocalDate.of(2027,6,30), "市政配套");

        // 额外保留几个原有点位作为填充（改造为合理名称）
        saveZdgc("21", "容西片区路网完善工程",    "115.8672972360", "39.0158623420", "容西片区次干路及支路路网系统完善，总里程约15公里。", 5,
                "under_construction", "中交路建",  new BigDecimal("8600"), "路基施工完成60%，部分路段已铺油",  LocalDate.of(2024,9,1),  LocalDate.of(2026,3,31), "交通物流");
        saveZdgc("22", "雄东片区公园绿地项目",    "115.9362064270", "39.0082480670", "雄东片区核心区公园绿地建设，含休闲广场、健身步道、景观水系。", 6,
                "completed", "北京园林集团",      new BigDecimal("4200"), "已竣工开放，日均游客约2000人次",   LocalDate.of(2022,3,1),  LocalDate.of(2024,6,30), "城市绿化");
        saveZdgc("23", "雄安智慧城市大脑二期",    "115.9556228290", "39.0078673530", "雄安城市计算中心二期扩容，提升城市物联网和AI算力支撑能力。", 8,
                "under_construction", "华为技术",  new BigDecimal("15800"), "机房主体封顶，设备陆续进场安装",   LocalDate.of(2024,6,1),  LocalDate.of(2026,6,30), "市政配套");
        saveZdgc("24", "南拒马河生态防洪工程",    "115.7667888030", "38.9625624150", "南拒马河右堤治理及生态景观提升，防洪标准达到百年一遇。", 8,
                "under_construction", "中国水利水电", new BigDecimal("9600"), "堤防加固完成80%，景观绿化同步",  LocalDate.of(2023,9,1),  LocalDate.of(2025,12,31), "城市绿化");
        saveZdgc("25", "大河村综合服务中心",      "115.8204694430", "39.0147202010", "大河村回迁安置区配套综合服务中心项目，含社区医疗、文体中心。", 9,
                "completed", "河北建工集团",      new BigDecimal("3800"), "已移交使用，服务周边3个安置社区",   LocalDate.of(2022,9,1),  LocalDate.of(2024,3,31), "市政配套");
        saveZdgc("26", "雄安自贸试验区综合服务大厅","116.0877524930", "39.0246446860", "雄安自贸试验区综合服务大厅，打造一站式跨境贸易服务平台。", 15,
                "under_construction", "中建一局",  new BigDecimal("6200"), "室内精装收尾中，智能化系统联调",   LocalDate.of(2024,1,1),  LocalDate.of(2025,9,30), "市政配套");
        saveZdgc("27", "府河湿地深度净化工程",    "115.7592643950", "38.8710060450", "府河湿地水质净化工程二期，设计净化处理规模为25万吨/天。", 6,
                "planning", "北控水务集团",       new BigDecimal("11200"), "立项审批中，环评已通过",          LocalDate.of(2025,5,1),  LocalDate.of(2027,6,30), "城市绿化");
    }

    private void saveRwjg(String id, String name, String jianjie, String x, String y, String src) {
        Rwjg rwjg = new Rwjg();
        rwjg.setId(id);
        rwjg.setName(name);
        rwjg.setJianjie(jianjie);
        rwjg.setX(x);
        rwjg.setY(y);
        rwjg.setSrc(src);
        managementService.saveRwjg(rwjg);
    }

    private void saveZdgc(String id, String name, String x, String y, String jieshao, Integer zoom,
                          String status, String contractor, BigDecimal bidAmount, String workCondition,
                          LocalDate startDate, LocalDate endDate, String category) {
        Zdgc zdgc = new Zdgc();
        zdgc.setId(id);
        zdgc.setName(name);
        zdgc.setX(x);
        zdgc.setY(y);
        zdgc.setJieshao(jieshao);
        zdgc.setZoom(zoom);
        zdgc.setSrc("/images/zdgc/" + id + ".jpg");
        // 全生命周期字段
        zdgc.setStatus(status);
        zdgc.setContractor(contractor);
        zdgc.setBidAmount(bidAmount);
        zdgc.setWorkCondition(workCondition);
        zdgc.setStartDate(startDate);
        zdgc.setEndDate(endDate);
        zdgc.setCategory(category);
        zdgc.setUpdatedAt(LocalDateTime.now());
        managementService.saveZdgc(zdgc);
    }

    // ===================== 商家种子数据 (FR-2.2/2.3) =====================

    private void seedSupplyMerchants() {
        // 已认证 VIP 商家
        saveMerchantFull("m1",  "雄安顺达五金建材",       "五金建材",     "true",  "王经理", "13811112222",
                "雄安新区容城县金源路18号",       "115.907397", "39.048494", 25.0,
                "/images/merchants/m1.png", "主营建筑五金、管件阀门、电工电料、防水材料", "07:30-19:00", 4.5);
        saveMerchantFull("m2",  "保定徐水重型机械设备租赁中心", "重型机械租赁", "true",  "李队长", "13933334444",
                "保定市徐水区大王店工业园东侧",    "115.650000", "39.020000", 50.0,
                "/images/merchants/m2.png", "提供挖掘机、装载机、推土机、起重机等全系列重型机械租赁", "07:00-20:00", 4.8);
        saveMerchantFull("m4",  "雄安聚能工程劳务派遣队",   "劳务输出",   "true",  "陈工",   "13777778888",
                "雄安新区昝岗镇雄安高铁站南侧",    "116.154126", "39.053709", 30.0,
                "/images/merchants/m4.png", "专业提供建筑工人、电焊工、钢筋工、架子工等各工种劳务派遣", "06:00-18:00", 4.2);

        // 已认证非VIP 商家
        saveMerchantFull("m3",  "定州建材钢筋批发总厂",     "大宗建材",   "false", "赵厂长", "13655556666",
                "保定定州市北环路路口",            "115.000000", "38.520000", 100.0,
                "/images/merchants/m3.png", "专业生产及批发各型号钢筋、盘螺、线材，承接大型工程供货", "08:00-18:00", 4.0);
        saveMerchantFull("m5",  "保定大森挖机租赁公司",     "重型机械租赁","false", "张老板", "13599990000",
                "保定市竞秀区朝阳南路",            "115.480000", "38.850000", 40.0,
                "/images/merchants/m5.png", "中小型挖掘机租赁，型号齐全，按天/月/年灵活计费",       "07:00-19:00", 3.8);

        // 新增商家
        saveMerchantFull("m6",  "雄安瑞丰商砼有限公司",     "大宗建材",   "false", "刘总",   "15100001111",
                "雄安新区容城县南张镇工业区",         "115.870000", "39.030000", 30.0,
                "/images/merchants/m6.png", "专业商品混凝土生产供应，C15-C60全标号，拥有4条180生产线", "06:00-22:00", 4.3);
        saveMerchantFull("m7",  "容城兴达架管扣件租赁站",   "五金建材",   "false", "周老板", "15200002222",
                "雄安新区容城县大河镇",             "115.950000", "39.060000", 20.0,
                "/images/merchants/m7.png", "钢管脚手架、扣件、顶托、模板等周转材料租赁",           "07:00-18:00", 4.1);
        saveMerchantFull("m8",  "安新县永昌建筑网片厂",    "大宗建材",   "false", "马厂长", "15300003333",
                "雄安新区安新县三台镇工业区",        "115.830000", "38.970000", 35.0,
                "/images/merchants/m8.png", "专业生产建筑钢筋网片、电焊网、护栏网，规格齐全",        "07:30-19:00", 4.4);
        saveMerchantFull("m9",  "雄安绿园园林绿化公司",     "城市绿化",   "true",  "孙经理", "15400004444",
                "雄安新区容东片区金湖北街",          "115.920000", "39.052000", 25.0,
                "/images/merchants/m9.png", "承接园林绿化设计施工、苗木供应、养护管理一体化服务",     "08:00-18:00", 4.6);
        saveMerchantFull("m10", "保定中远物流运输公司",     "交通物流",   "false", "钱总",   "15500005555",
                "保定市清苑区物流园区",             "115.550000", "38.780000", 80.0,
                "/images/merchants/m10.png", "大型工程设备运输、超限货物运输、仓储配送服务",          "00:00-24:00", 4.2);
        saveMerchantFull("m11", "安州润达建材经营部",       "五金建材",   "false", "吴老板", "15600006666",
                "雄安新区安州镇东大街",             "115.830000", "38.875000", 15.0,
                "/images/merchants/m11.png", "装饰装修材料、水电材料、卫浴洁具、瓷砖地板",            "08:00-18:30", 3.9);
        saveMerchantFull("m12", "雄安华宇钢结构工程公司",   "大宗建材",   "false", "郑总",   "15700007777",
                "雄安新区昝岗镇工业园区",            "116.160000", "39.048000", 40.0,
                "/images/merchants/m12.png", "钢结构设计、制作、安装一体化服务，持有壹级施工资质",      "07:00-19:00", 4.7);
        saveMerchantFull("m13", "容城长信工程检测公司",     "劳务输出",   "false", "杨工",   "15800008888",
                "雄安新区容城县奥威路",             "115.890000", "39.045000", 50.0,
                "/images/merchants/m13.png", "工程质量检测、材料试验、地基检测、钢结构检测",            "08:30-17:30", 4.5);
        saveMerchantFull("m14", "保定中通机电安装公司",     "劳务输出",   "false", "何总",   "15900009999",
                "保定市莲池区五四东路",             "115.500000", "38.870000", 60.0,
                "/images/merchants/m14.png", "机电设备安装、水电安装、消防工程、智能化工程施工",        "07:00-18:00", 4.0);

        // 新增待审核商家（verified = false）
        saveMerchantPending("m15", "雄安鑫达防水工程公司",     "劳务输出",     "防水补漏", "周经理", "16000001111",
                "雄安新区容东片区甘霖苑底商", "115.930000", "39.055000", 20.0);
        saveMerchantPending("m16", "安新县鸿运土石方车队",     "重型机械租赁", "土石方",   "刘队长", "16100002222",
                "雄安新区安新县建设大街",       "115.930000", "38.925000", 25.0);
        saveMerchantPending("m17", "保定鑫旺铝模科技公司",      "大宗建材",     "铝模板",   "黄总",   "16200003333",
                "保定市徐水区长城北大街",       "115.660000", "39.030000", 35.0);
        saveMerchantPending("m18", "雄安金盾安防科技公司",  "五金建材",  "安防监控", "唐经理", "16300004444",
                "雄安新区容城县板正北大街",     "115.895000", "39.050000", 15.0);
        saveMerchantPending("m19", "容城华宇玻璃幕墙厂",    "大宗建材",  "玻璃幕墙", "曹厂长", "16400005555",
                "雄安新区容城县南张镇",         "115.880000", "39.035000", 30.0);
    }

    /** 完整商家（含所有扩展字段） */
    private void saveMerchantFull(String id, String name, String type, String vip, String contact,
                                  String phone, String address, String x, String y, Double radius,
                                  String logoUrl, String desc, String hours, Double rating) {
        SupplyMerchant m = new SupplyMerchant();
        m.setId(id);
        m.setMerchantName(name);
        m.setBusinessType(type);
        m.setContactPerson(contact);
        m.setContactPhone(phone);
        m.setAddress(address);
        m.setX(x);
        m.setY(y);
        m.setServiceRadius(radius);
        m.setIsVip(Boolean.parseBoolean(vip));
        m.setLogoUrl(logoUrl);
        m.setDescription(desc);
        m.setBusinessHours(hours);
        m.setRating(rating);
        m.setVerified(true);
        m.setCreatedAt(LocalDateTime.now().minusDays((long) (Math.random() * 365)));
        m.setUpdatedAt(LocalDateTime.now());
        supplyMerchantRepository.save(m);
    }

    /** 待审核商家（简化字段） */
    private void saveMerchantPending(String id, String name, String type, String desc,
                                     String contact, String phone, String address,
                                     String x, String y, Double radius) {
        SupplyMerchant m = new SupplyMerchant();
        m.setId(id);
        m.setMerchantName(name);
        m.setBusinessType(type);
        m.setContactPerson(contact);
        m.setContactPhone(phone);
        m.setAddress(address);
        m.setX(x);
        m.setY(y);
        m.setServiceRadius(radius);
        m.setIsVip(false);
        m.setDescription(desc);
        m.setRating(0.0);
        m.setVerified(false);
        m.setCreatedAt(LocalDateTime.now());
        m.setUpdatedAt(LocalDateTime.now());
        supplyMerchantRepository.save(m);
    }

    // ===================== 留言种子数据 =====================

    private void seedComments() {
        saveComment("1", "大河村三组回迁村民", "陈子正大武术家的故里，我们庄子当年的骄傲！小毛头时候还跟老拳师一起比划过武艺，现在虽然全家住进了容东安置区，梦里还是常常回到李林庄村的土路古树下，心里热乎乎的。");
        saveComment("1", "寻根的雄安游子", "每次看到故居照片就想起太爷爷那一辈。太爷爷生前常说，保定雄县的传统武术精神是要一代代传下去的。感谢平台把我们的老村地标保留得这么好！");
        saveComment("2", "山西村老刘头", "我们山西村的砖塔可是明代的古宝贝！小时候天天在砖塔底下捉迷藏，夏天在阴凉地里下象棋。搬家那会儿全村老少都特意去塔前拍了合影，真是一辈子的牵挂。");
        saveComment("2", "白洋淀晚霞", "看到老家照片瞬间红了眼眶。小时候塔边那棵大洋槐树太茂盛了，不知道现在保存得如何。衷心祝愿所有的老乡亲在容东新家一切顺利、阖家安康！");
    }

    private void saveComment(String villageId, String author, String content) {
        NostalgiaComment comment = new NostalgiaComment();
        comment.setId(UUID.randomUUID().toString());
        comment.setVillageId(villageId);
        comment.setAuthor(author);
        comment.setContent(content);
        comment.setCreatedAt(LocalDateTime.now().toString());
        nostalgiaCommentRepository.save(comment);
    }
}
