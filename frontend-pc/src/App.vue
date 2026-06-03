<script setup>
import { onMounted, ref, reactive, watch, nextTick } from 'vue'
import axios from 'axios'

// OpenLayers 核心包
import Map from 'ol/Map.js'
import View from 'ol/View.js'
import Feature from 'ol/Feature.js'
import Overlay from 'ol/Overlay.js'
import { fromLonLat, toLonLat } from 'ol/proj.js'
import Point from 'ol/geom/Point.js'
import CircleGeom from 'ol/geom/Circle.js'
import { Vector as VectorLayer, Tile as TileLayer } from 'ol/layer.js'
import { Vector as VectorSource, XYZ } from 'ol/source.js'
import GeoJSON from 'ol/format/GeoJSON.js'
import { Style, Stroke, Fill, Icon, Circle as CircleStyle } from 'ol/style.js'

// 状态声明
const activeModule = ref('home') // home, showcase, projects, memory, layers, business, admin, spatial_villages
const selectedItem = ref(null)      // 当前在列表/地图点击选中的项目、老村或面要素
const searchInput = ref('')          // 模糊检索词
const loading = ref(false)
const aiLoading = ref(false)

// 双边商业与折叠抽屉状态
const showApplyModal = ref(false)
const showDemandModal = ref(false)
const showCitizenModal = ref(false)
const isPickingCoordinate = ref(false)
const sidebarCollapsed = ref(false)

const applyForm = ref({
  merchant_name: '',
  business_type: '机械租赁',
  sub_category: 'heavy_machinery',
  source_region: '雄安本地',
  contact_person: '',
  contact_phone: '',
  address: '',
  x: '',
  y: '',
  service_radius: 15.0
})

const demandForm = ref({
  project_id: '',
  material_desc: '',
  valid_period: '货到即付',
  contact_phone: ''
})

const citizenForm = ref({
  issue_type: '设施损坏',
  description: '',
  reporter_name: '热心市民',
  contact_phone: '',
  x: '',
  y: ''
})

// 监听经营类型变化自动映射子品类
watch(() => applyForm.value.business_type, (newVal) => {
  const mapping = {
    '机械租赁': 'heavy_machinery',
    '五金建材': 'hardware',
    '民生餐饮': 'ceylon_bento',
    '便利超市': 'minisuper',
    '本地劳务': 'local_labor'
  }
  applyForm.value.sub_category = mapping[newVal] || 'hardware'
})

// 数据源
const projects = ref([])
const villages = ref([])
const comments = ref([])
const merchants = ref([])
const investmentParcels = ref([])

// 商业化文旅人文景点/时空村落谱扩展状态
const selectedAttractionTag = ref('全部') // 分类筛选标签
const isAudioPlaying = ref(false)       // AI 语音播放状态
const selectedYear = ref(2021)          // 时空变迁演变轴当前年份
const hoveredVillageName = ref('')      // 鼠标悬停村落名字
const hoveredVillageStatus = ref(null)  // 鼠标悬停村落状态

// 新评论表单
const commentForm = reactive({
  author: '',
  content: ''
})

// 管理端新增/编辑表单
const editType = ref('zdgc') // zdgc, rwjg
const adminForm = reactive({
  id: '',
  name: '',
  x: '',
  y: '',
  zoom: 12,
  src: '',
  jieshao: '', // zdgc.jieshao or rwjg.jianjie
})

const showAdminModal = ref(false)
const activeAdminTab = ref('zdgc') // zdgc, merchant, rwjg, spatial-villages, investment-parcels

// 时空村落编辑表单与列表
const spatialVillages = ref([])
const spatialVillageForm = reactive({
  id: '',
  village_name: '',
  status: 0,
  plan_demolition_year: 2018,
  geom_json: '[[[116.125, 38.985], [116.135, 38.985], [116.135, 38.975], [116.125, 38.975], [116.125, 38.985]]]',
  history: ''
})

function resetSpatialVillageForm() {
  spatialVillageForm.id = ''
  spatialVillageForm.village_name = ''
  spatialVillageForm.status = 0
  spatialVillageForm.plan_demolition_year = 2018
  spatialVillageForm.geom_json = '[[[116.125, 38.985], [116.135, 38.985], [116.135, 38.975], [116.125, 38.975], [116.125, 38.985]]]'
  spatialVillageForm.history = ''
}

// 招商引资编辑表单与意向申请列表
const activeInvestmentSubTab = ref('parcels') // parcels, intents
const investmentIntents = ref([])
const investmentParcelForm = reactive({
  id: '',
  parcel_name: '',
  land_use: '商业用地',
  area_sqm: 10000,
  far: 2.0,
  geom_json: '[[[115.918, 39.050], [115.928, 39.050], [115.928, 39.056], [115.918, 39.056], [115.918, 39.050]]]',
  status: 0
})

function resetInvestmentParcelForm() {
  investmentParcelForm.id = ''
  investmentParcelForm.parcel_name = ''
  investmentParcelForm.land_use = '商业用地'
  investmentParcelForm.area_sqm = 10000
  investmentParcelForm.far = 2.0
  investmentParcelForm.geom_json = '[[[115.918, 39.050], [115.928, 39.050], [115.928, 39.056], [115.918, 39.056], [115.918, 39.050]]]'
  investmentParcelForm.status = 0
}
const merchantForm = reactive({
  id: '',
  merchantName: '',
  businessType: '机械租赁',
  contactPerson: '',
  contactPhone: '',
  address: '',
  x: '',
  y: '',
  serviceRadius: 10,
  isVip: false,
  logoUrl: '/images/merchants/m1.png',
  description: '',
  businessHours: '08:00-18:00',
  rating: 5.0,
  verified: true
})

// ToB 大基建商业服务生态中心 子标签页与空间交互状态
const activeSubTab = ref('intelligence') // intelligence, wholesale, living
const selectedRadarProject = ref(null)    // 选中的大宗供应链工地
const radarRadius = ref(3000)             // 距离滑块 3km, 5km, 10km
const selectedRadarCategory = ref('all')  // 子行业筛选
const radarMerchants = ref([])            // 雷达商户结果
const livingCircleData = ref(null)        // 工地生活圈后勤求购数据
const activeIntelligenceProject = ref(null) // 选中的情报工程
const animationFrame = ref(0)
let animationInterval = null

function startPulseAnimation() {
  if (animationInterval) clearInterval(animationInterval);
  // 移除占用高频 CPU 的 setInterval 动画渲染
  // 改为在切换 Tab 时静态渲染一次以提升性能
  if (activeModule.value === 'business') {
    if (activeSubTab.value === 'intelligence') {
      updateIntelligenceStyles();
    } else if (activeSubTab.value === 'living') {
      updateLivingCircleRipples();
    }
  }
  if (markerSource) markerSource.changed();
}

function stopPulseAnimation() {
  if (animationInterval) {
    clearInterval(animationInterval);
    animationInterval = null;
  }
}

// AI 文案生成结果
const aiResult = ref(null)

// 懒登录与身份状态
const currentUser = ref(null)
const showLoginModal = ref(false)
const loginMode = ref('login') // login, register, reset
const loginForm = reactive({
  account: '',
  password: '',
  displayName: '',
  email: '',
  newPassword: ''
})
// 暂存被拦截的动作，在登录成功后自动闭环执行
let deferredCallback = null

// OpenLayers 地图和矢量图层相关实例
let mapInstance = null
let baseOsmLayer = null
let markerSource = null
let markerLayer = null
let bufferSource = null
let bufferLayer = null
let popupOverlay = null

// 空间矢量面图层（dlgh 未来道路多图层支持）
let boundaryLayer = null  // quanjie.json
let countyLayer = null    // xianjie.json
let zonesLayer = null     // xinan.json
let roadsLayer = null     // quanjie.json secondary
let mainRoadLayer = null  // quanjie.json gold
let r1LineLayer = null    // quanjie.json dashed
let villagesLayer = null  // villages GeoJSON layer
let investmentParcelsLayer = null // investment parcels GeoJSON layer
const citizenReports = ref([])
const citizenImageFile = ref(null)
const citizenImagePreview = ref('')

// 规划图层显示隐藏控制器 Toggle Controls
const layerToggles = reactive({
  boundary: true,
  counties: false,
  zones: false,
  mainRoad: false,
  roads: false,
  r1Line: false
})

// Toast 消息浮层
const toast = reactive({
  show: false,
  message: '',
  isSuccess: false
})

function triggerToast(msg, isSuccess = false) {
  toast.message = msg;
  toast.isSuccess = isSuccess;
  toast.show = true;
  setTimeout(() => { toast.show = false; }, 4000);
}

// 拦截拦截校验 (Lazy Auth Filter)
function requireAuth(callback) {
  if (currentUser.value) {
    callback();
  } else {
    deferredCallback = callback;
    loginMode.value = 'login';
    showLoginModal.value = true;
    triggerToast('该操作属于商用保护功能，请先登录授权您的商户账号。');
  }
}

// ==================== A. 接口集成数据流 ====================

// 1. 安全状态校验
async function checkAuth() {
  try {
    const res = await axios.get('/auth/me')
    if (res.data.code === 200) {
      currentUser.value = res.data.data;
    } else {
      currentUser.value = null;
    }
  } catch (err) {
    currentUser.value = null;
  }
}

// 2. 安全登录
async function handleLogin() {
  if (!loginForm.account || !loginForm.password) return;
  loading.value = true;
  try {
    const params = new URLSearchParams();
    params.append('account', loginForm.account.trim());
    params.append('password', loginForm.password);

    const res = await axios.post('/auth/login', params, {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    });
    loading.value = false;
    if (res.data.code === 200) {
      currentUser.value = res.data.data;
      showLoginModal.value = false;
      triggerToast('商户授权登录成功！', true);
      // 执行被拦截的未完成函数
      if (deferredCallback) {
        const cb = deferredCallback;
        deferredCallback = null;
        cb();
      }
    } else {
      triggerToast(res.data.message || '登录校验失败，账号或密码错误。');
    }
  } catch (err) {
    loading.value = false;
    triggerToast('连接后台服务失败，请确保 Spring Boot 已启动。');
  }
}

// 3. 商户入驻注册
async function handleRegister() {
  if (!loginForm.account || !loginForm.password || !loginForm.displayName || !loginForm.email) return;
  loading.value = true;
  try {
    const params = new URLSearchParams();
    params.append('account', loginForm.account.trim());
    params.append('displayName', loginForm.displayName.trim());
    params.append('email', loginForm.email.trim());
    params.append('password', loginForm.password);

    const res = await axios.post('/auth/register', params, {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    });
    loading.value = false;
    if (res.data.code === 200) {
      triggerToast('商户入驻申请成功！正在切换至登录大厅...', true);
      setTimeout(() => {
        loginMode.value = 'login';
      }, 1500);
    } else {
      triggerToast(res.data.message || '入驻失败，请检查账号或邮箱是否已被占用。');
    }
  } catch (err) {
    loading.value = false;
    triggerToast('注册提交失败，网络异常。');
  }
}

// 4. 重置找回密码
async function handleResetPassword() {
  if (!loginForm.account || !loginForm.email || !loginForm.newPassword) return;
  loading.value = true;
  try {
    const params = new URLSearchParams();
    params.append('account', loginForm.account.trim());
    params.append('email', loginForm.email.trim());
    params.append('newPassword', loginForm.newPassword);

    const res = await axios.post('/auth/reset-password', params, {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    });
    loading.value = false;
    if (res.data.code === 200) {
      triggerToast('安全密码重置成功，请使用新密码登录。', true);
      setTimeout(() => {
        loginMode.value = 'login';
      }, 1500);
    } else {
      triggerToast(res.data.message || '重置失败，账号与绑定邮箱不匹配。');
    }
  } catch (err) {
    loading.value = false;
    triggerToast('密码重置失败，网络连接超时。');
  }
}

// 5. 安全退出
async function handleLogout() {
  if (!confirm('确定要安全退出当前雄安商户管理账户吗？')) return;
  try {
    await axios.post('/auth/logout')
    currentUser.value = null;
    triggerToast('已成功登出，系统还原为游客状态。', true);
    if (activeModule.value === 'admin' || activeModule.value === 'business') {
      activeModule.value = 'showcase';
    }
  } catch (err) {
    currentUser.value = null;
    activeModule.value = 'showcase';
  }
}

// 6. 加载重点工程
async function fetchProjects() {
  try {
    const res = await axios.get('/api/zdgc/list')
    if (res.data.code === 200) {
      projects.value = res.data.data || [];
    }
  } catch (err) {
    triggerToast('获取基建工程数据失败。');
  }
}

// 7. 加载人文老村
async function fetchVillages() {
  try {
    const res = await axios.get('/api/rwjg/list')
    if (res.data.code === 200) {
      const rawVillages = res.data.data || [];
      // 动态注入分类和热度分值，支持人文景点的分类标签与打卡看板功能
      villages.value = rawVillages.map((v, index) => {
        let cat = '历史遗存';
        if (v.name.includes('纪念馆') || v.name.includes('革命')) {
          cat = '红色革命';
        } else if (v.name.includes('公园') || v.name.includes('淀') || v.name.includes('生态')) {
          cat = '生态公园';
        } else if (v.name.includes('总部') || v.name.includes('广场') || v.name.includes('中心') || v.name.includes('地标')) {
          cat = '现代地标';
        }
        return {
          ...v,
          category: cat,
          hotScore: 1120 + (index * 149) % 380
        };
      });
    }
  } catch (err) {
    triggerToast('获取历史回忆老村落数据失败。');
  }
}

// 8. 获取留言
async function fetchComments(villageId) {
  try {
    let url = `/api/comment/list?villageId=${encodeURIComponent(villageId)}`;
    if (activeModule.value === 'spatial_villages') {
      url = `http://localhost:8000/api/v1/spatial/villages/${encodeURIComponent(villageId)}/comments`;
    }
    const res = await axios.get(url);
    if (activeModule.value === 'spatial_villages') {
      comments.value = res.data || [];
    } else {
      if (res.data.code === 200) {
        comments.value = res.data.data || [];
      }
    }
  } catch (err) {
    comments.value = [];
  }
}

// 9. 发表老村留言 (懒登录拦截)
function submitComment() {
  if (!selectedItem.value) return;
  if (!commentForm.author.trim() || !commentForm.content.trim()) {
    triggerToast('请输入您的昵称和留念寄语。');
    return;
  }

  requireAuth(async () => {
    loading.value = true;
    try {
      if (activeModule.value === 'spatial_villages') {
        const payload = {
          author: commentForm.author.trim(),
          content: commentForm.content.trim()
        };
        const res = await axios.post(
          `http://localhost:8000/api/v1/spatial/villages/${encodeURIComponent(selectedItem.value.id)}/comments`,
          payload
        );
        loading.value = false;
        triggerToast('回忆寄语留存成功！已成功上墙。', true);
        commentForm.content = '';
        fetchComments(selectedItem.value.id);
      } else {
        const res = await axios.post(
          `/api/comment/save?villageId=${encodeURIComponent(selectedItem.value.id)}&author=${encodeURIComponent(commentForm.author.trim())}&content=${encodeURIComponent(commentForm.content.trim())}`
        );
        loading.value = false;
        if (res.data.code === 200) {
          triggerToast('回忆寄语留存成功！已成功上墙。', true);
          commentForm.content = '';
          fetchComments(selectedItem.value.id);
        } else {
          triggerToast(res.data.message || '留言发表失败，请重试。');
        }
      }
    } catch (err) {
      loading.value = false;
      triggerToast('网络错误，留言发表失败。');
    }
  });
}

// 10. 加载 B 端商户列表 (懒登录拦截)
function loadBusinessModule() {
  requireAuth(async () => {
    activeModule.value = 'business';
    handleSubTabChange('intelligence');
  });
}

// 11. 调用 FastAPI 智能体生成 AI 怀旧宣推文案 (懒登录拦截)
function generateAICopywriter() {
  if (!selectedItem.value) return;
  requireAuth(async () => {
    aiLoading.value = true;
    aiResult.value = null;
    try {
      // 访问运行在 8000 端口的 Python FastAPI 智能体微服务
      const payload = {
        village_name: selectedItem.value.name,
        township: '雄安新区重点片区',
        current_district: '征迁安置回迁片区',
        history_content: selectedItem.value.jianjie || '华北平原历史悠久的文化名村，民风淳朴，底蕴深厚。'
      };
      const res = await axios.post('http://localhost:8000/agent/generate-marketing-copy', payload)
      aiLoading.value = false;
      if (res.data) {
        aiResult.value = res.data;
        triggerToast('AI 怀旧回忆文案创作大师，赋能生成成功！', true);
      } else {
        triggerToast('AI 智能体抓取引擎繁忙，请稍后再试。');
      }
    } catch (err) {
      aiLoading.value = false;
      triggerToast('连接 FastAPI AI Agent 服务失败，请确保 agents 端运行正常。');
    }
  });
}

// 双边商业撮合交互逻辑与折叠面板控制
function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value;
  if (mapInstance) {
    mapInstance.updateSize();
    setTimeout(() => {
      mapInstance.updateSize();
    }, 300);
  }
}

function startPickCoordinate() {
  isPickingCoordinate.value = true;
  triggerToast('📍 坐标捕获模式已开启！请直接在地图中点击目标位置进行定位选取...');
}

async function submitApply() {
  if (!applyForm.value.merchant_name.trim() || !applyForm.value.contact_phone.trim()) {
    triggerToast('❌ 请填写完整的企业名称和联系电话！');
    return;
  }
  if (!applyForm.value.x || !applyForm.value.y) {
    triggerToast('❌ 请在地图上选取坐标或手动录入！');
    return;
  }
  
  try {
    const res = await axios.post('http://localhost:8000/api/v1/tob/merchant/apply', applyForm.value);
    if (res.data) {
      triggerToast('🎉 ' + res.data.message, true);
      showApplyModal.value = false;
      // 重置表单
      applyForm.value = {
        merchant_name: '',
        business_type: '机械租赁',
        sub_category: 'heavy_machinery',
        source_region: '雄安本地',
        contact_person: '',
        contact_phone: '',
        address: '',
        x: '',
        y: '',
        service_radius: 15.0
      };
      // 刷新雷达
      if (activeModule.value === 'business') {
        fetchRadarMerchants();
      }
    }
  } catch (err) {
    console.error(err);
    triggerToast('❌ 商户入驻申请提交失败，请检查网络！');
  }
}

function openDemandModal() {
  showDemandModal.value = true;
  if (selectedRadarProject.value) {
    demandForm.value.project_id = selectedRadarProject.value.id;
  } else if (tobProjects.value.length > 0) {
    demandForm.value.project_id = tobProjects.value[0].id;
  }
}

async function submitDemand() {
  if (!demandForm.value.project_id) {
    triggerToast('❌ 请选择一个所属工程项目！');
    return;
  }
  if (!demandForm.value.material_desc.trim() || !demandForm.value.contact_phone.trim()) {
    triggerToast('❌ 请填写物资描述及联系电话！');
    return;
  }
  
  try {
    const res = await axios.post('http://localhost:8000/api/v1/tob/demand/publish', demandForm.value);
    if (res.data) {
      triggerToast('🎉 ' + res.data.message, true);
      showDemandModal.value = false;
      
      const targetProjId = demandForm.value.project_id;
      // 重置表单
      demandForm.value = {
        project_id: '',
        material_desc: '',
        valid_period: '货到即付',
        contact_phone: ''
      };
      
      // 刷新 timeline 广播
      if (activeModule.value === 'business') {
        const targetProj = tobProjects.value.find(p => p.id === targetProjId);
        if (targetProj) {
          selectedRadarProject.value = targetProj;
          fetchLivingCircleData(targetProjId);
        }
      }
    }
  } catch (err) {
    console.error(err);
    triggerToast('❌ 物资需求发布失败，请检查网络！');
  }
}

async function submitCitizenReport() {
  if (!citizenForm.value.description.trim()) {
    triggerToast('❌ 请填写上报描述内容！');
    return;
  }
  const formData = new FormData();
  formData.append('description', citizenForm.value.description);
  formData.append('issueType', citizenForm.value.issue_type);
  formData.append('reporterName', citizenForm.value.reporter_name);
  formData.append('x', citizenForm.value.x);
  formData.append('y', citizenForm.value.y);
  if (citizenImageFile.value) {
    formData.append('image', citizenImageFile.value);
  }
  try {
    const res = await axios.post('/api/citizen/report', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    if (res.data.code === 200) {
      triggerToast('📸 上报成功，感谢支持！', true);
      showCitizenModal.value = false;
      citizenForm.value.description = '';
      citizenImageFile.value = null;
      citizenImagePreview.value = '';
      loadCitizenReports();
    } else {
      triggerToast(res.data.message || '上报失败');
    }
  } catch (err) {
    console.error(err);
    triggerToast('❌ 随手拍上报失败，请检查网络！');
  }
}
function onCitizenImageChange(e) {
  const file = e.target.files[0];
  if (file) {
    citizenImageFile.value = file;
    const reader = new FileReader();
    reader.onload = (ev) => { citizenImagePreview.value = ev.target.result; };
    reader.readAsDataURL(file);
  }
}
async function loadCitizenReports() {
  try {
    const res = await axios.get('/api/citizen/list');
    if (res.data.code === 200) {
      citizenReports.value = res.data.data || [];
    }
  } catch(e) {}
}
function openCitizenModal() {
  if (mapInstance) {
    const view = mapInstance.getView();
    const center = toLonLat(view.getCenter());
    citizenForm.value.x = center[0].toFixed(6);
    citizenForm.value.y = center[1].toFixed(6);
  }
  showCitizenModal.value = true;
}

const showInvestmentIntentModal = ref(false)
const investmentIntentForm = ref({
  parcel_id: '',
  company_name: '',
  contact_person: '',
  contact_phone: '',
  intent_desc: ''
})

async function fetchInvestmentParcels() {
  try {
    const res = await axios.get('/api/v1/investment/parcels');
    if (res.data && res.data.features) {
      investmentParcels.value = res.data.features;
      if (investmentParcelsLayer) {
        const source = new VectorSource({
          features: new GeoJSON().readFeatures(res.data, {
            featureProjection: 'EPSG:3857'
          })
        });
        investmentParcelsLayer.setSource(source);
      }
    }
  } catch (err) {
    console.error(err);
    triggerToast('❌ 获取招商引资地块数据失败');
  }
}

async function submitInvestmentIntent() {
  if (!investmentIntentForm.value.company_name.trim() || !investmentIntentForm.value.contact_phone.trim()) {
    triggerToast('❌ 请填写公司名称和联系电话！');
    return;
  }
  try {
    const res = await axios.post('/api/v1/investment/intent', investmentIntentForm.value);
    if (res.data) {
      triggerToast('🎉 ' + res.data.message, true);
      showInvestmentIntentModal.value = false;
      investmentIntentForm.value = {
        parcel_id: '',
        company_name: '',
        contact_person: '',
        contact_phone: '',
        intent_desc: ''
      };
    }
  } catch (err) {
    console.error(err);
    triggerToast('❌ 投资意向提交失败，请检查网络！');
  }
}

async function checkinCulturalSpot(spot) {
  try {
    const res = await axios.post(`http://localhost:8000/api/v1/cultural/checkin?spot_id=${spot.id}`);
    if (res.data) {
      triggerToast(`🎉 ${res.data.message} 恭喜获得：${res.data.coupon}`, true);
    }
  } catch (err) {
    console.error(err);
    triggerToast('❌ 打卡失败，请重试！');
  }
}

// 12. 数据管理 (懒登录拦截，触发全屏 Modal)
function openAdminConsole() {
  requireAuth(() => {
    showAdminModal.value = true;
    activeAdminTab.value = 'zdgc';
    resetAdminForm();
    resetMerchantForm();
    loadCitizenReportsFromAgent(); // 默认在打开控制台时拉取市民工单
    loadSpatialVillages();
    fetchInvestmentParcels();
    loadInvestmentIntents();
  });
}

// ==========================================
// AI Ingestion and Geocoding SandBox Methods
// ==========================================
const aiCrawlLoading = ref(false)
const aiCrawlResults = ref([])
const customAddressToGeocode = ref('')
const geocodedResult = ref(null)
const selectedCitizenReport = ref(null)

async function runAiCrawl() {
  aiCrawlLoading.value = true;
  aiCrawlResults.value = [];
  try {
    const res = await axios.post('http://localhost:8000/agent/crawl');
    if (res.data) {
      aiCrawlResults.value = res.data;
      triggerToast(`🎉 AI 抓取成功！共捕获到 ${res.data.length} 条工程数据线索。`, true);
    }
  } catch (e) {
    console.error(e);
    triggerToast('❌ AI 抓取失败，请检查 Python FastAPI 服务是否正常运行！');
  } finally {
    aiCrawlLoading.value = false;
  }
}

async function geocodeCustomAddress() {
  if (!customAddressToGeocode.value.trim()) {
    triggerToast('请输入待解析的中文文本地址！');
    return;
  }
  try {
    const res = await axios.get('http://localhost:8000/agent/geocode', {
      params: { address: customAddressToGeocode.value.trim() }
    });
    if (res.data) {
      geocodedResult.value = res.data;
      triggerToast('🎉 空间解析成功！脱敏坐标已计算完毕。', true);
    }
  } catch (e) {
    console.error(e);
    triggerToast('❌ 空间解析失败，请确保地址包含有效的地标。');
  }
}

function fillGeocodedCoordsToForm() {
  if (!geocodedResult.value) return;
  adminForm.id = 'ai_' + Date.now().toString().slice(-6);
  adminForm.name = geocodedResult.value.address;
  adminForm.x = String(geocodedResult.value.longitude);
  adminForm.y = String(geocodedResult.value.latitude);
  adminForm.jieshao = `该地块由 AI 空间解析模块自动转换。原始输入地址：${geocodedResult.value.address}`;
  activeAdminTab.value = 'zdgc';
  triggerToast('坐标与地名已填入重点工程表单中。');
}

function fillCrawlLeadToForm(lead) {
  adminForm.id = 'ai_' + Date.now().toString().slice(-6);
  adminForm.name = lead.name;
  adminForm.x = String(lead.x);
  adminForm.y = String(lead.y);
  adminForm.jieshao = lead.jieshao;
  adminForm.src = lead.src || '/images/zdgc/10.jpg';
  adminForm.zoom = lead.zoom || 13;
  editType.value = 'zdgc';
  activeAdminTab.value = 'zdgc';
  triggerToast('线索已加载到编辑器。请补充其余内容后点击保存！');
}

async function approveAndPublishLead(lead) {
  try {
    loading.value = true;
    const payload = {
      id: 'ai_' + Date.now().toString().slice(-6),
      name: lead.name,
      x: String(lead.x),
      y: String(lead.y),
      zoom: lead.zoom || 13,
      src: lead.src || '/images/zdgc/10.jpg',
      jieshao: lead.jieshao
    };
    const res = await axios.post('/manage/zdgc', payload);
    if (res.data.code === 200) {
      triggerToast(`🎉 【${lead.name}】已成功审核上架至 H2 数据库！`, true);
      fetchProjects();
      aiCrawlResults.value = aiCrawlResults.value.filter(item => item.name !== lead.name);
    } else {
      triggerToast(res.data.message || '保存失败。');
    }
  } catch (e) {
    console.error(e);
    triggerToast('❌ 发布失败，请检查 Java 后端是否正常运行。');
  } finally {
    loading.value = false;
  }
}

async function loadCitizenReportsFromAgent() {
  try {
    const res = await axios.get('http://localhost:8000/api/v1/citizen/reports');
    citizenReports.value = res.data || [];
  } catch (e) {
    console.error(e);
  }
}

async function updateReportStatus(reportId, newStatus) {
  try {
    const res = await axios.post(`http://localhost:8000/api/v1/citizen/reports/${reportId}/status`, {
      status: newStatus
    });
    triggerToast(res.data.message || '状态更新成功！', true);
    await loadCitizenReportsFromAgent();
  } catch (e) {
    console.error(e);
    triggerToast('❌ 更新状态失败。');
  }
}

// ==========================================
// 时空老村庄 & 招商引资地块 CRUD 后台管理逻辑
// ==========================================
async function loadSpatialVillages() {
  try {
    const res = await axios.get('/api/v1/spatial/villages');
    if (res.data && res.data.features) {
      spatialVillages.value = res.data.features;
    }
  } catch (err) {
    console.error(err);
    triggerToast('获取时空老村落数据失败。');
  }
}

async function saveSpatialVillageRecord() {
  if (!spatialVillageForm.id || !spatialVillageForm.village_name || !spatialVillageForm.geom_json) {
    triggerToast('请填写完整 ID、名称和空间多边形坐标值。');
    return;
  }
  loading.value = true;
  try {
    const payload = {
      id: spatialVillageForm.id.trim(),
      village_name: spatialVillageForm.village_name.trim(),
      status: Number(spatialVillageForm.status),
      plan_demolition_year: spatialVillageForm.plan_demolition_year ? Number(spatialVillageForm.plan_demolition_year) : null,
      geom_json: spatialVillageForm.geom_json.trim(),
      history: spatialVillageForm.history.trim()
    };
    const res = await axios.post('/api/v1/spatial/villages', payload);
    loading.value = false;
    if (res.data) {
      triggerToast('村落空间数据保存成功！', true);
      loadSpatialVillages();
      resetSpatialVillageForm();
      if (villagesLayer) {
        villagesLayer.getSource().refresh();
      }
    }
  } catch (err) {
    loading.value = false;
    console.error(err);
    triggerToast('时空村落数据保存失败。');
  }
}

function editSpatialVillage(village) {
  spatialVillageForm.id = village.properties.id;
  spatialVillageForm.village_name = village.properties.village_name;
  spatialVillageForm.status = village.properties.status;
  spatialVillageForm.plan_demolition_year = village.properties.plan_demolition_year;
  spatialVillageForm.geom_json = JSON.stringify(village.geometry.coordinates);
  spatialVillageForm.history = village.properties.history || '';
}

async function deleteSpatialVillageRecord(villageId) {
  if (!confirm(`确定要物理删除老村落 [${villageId}] 吗？此操作不可逆！`)) return;
  loading.value = true;
  try {
    const res = await axios.delete(`/api/v1/spatial/villages/${encodeURIComponent(villageId)}`);
    loading.value = false;
    if (res.data) {
      triggerToast('时空村落数据已安全移除。', true);
      loadSpatialVillages();
      if (villagesLayer) {
        villagesLayer.getSource().refresh();
      }
    }
  } catch (err) {
    loading.value = false;
    console.error(err);
    triggerToast('删除时空老村落数据失败。');
  }
}

function locateVillageOnMap(village) {
  if (!mapInstance) return;
  const coords = village.geometry.coordinates[0][0];
  if (coords) {
    const center = fromLonLat([parseFloat(coords[0]), parseFloat(coords[1])]);
    mapInstance.getView().animate({
      center: center,
      zoom: 14,
      duration: 800
    });
    showAdminModal.value = false;
    triggerToast(`📍 正在定位村落：[${village.properties.village_name}]`);
  }
}

async function saveInvestmentParcelRecord() {
  if (!investmentParcelForm.id || !investmentParcelForm.parcel_name || !investmentParcelForm.geom_json) {
    triggerToast('请填写完整 ID、名称和空间多边形坐标值。');
    return;
  }
  loading.value = true;
  try {
    const payload = {
      id: investmentParcelForm.id.trim(),
      parcel_name: investmentParcelForm.parcel_name.trim(),
      land_use: investmentParcelForm.land_use.trim(),
      area_sqm: Number(investmentParcelForm.area_sqm),
      far: Number(investmentParcelForm.far),
      geom_json: investmentParcelForm.geom_json.trim(),
      status: Number(investmentParcelForm.status)
    };
    const res = await axios.post('/api/v1/investment/parcels', payload);
    loading.value = false;
    if (res.data) {
      triggerToast('招商地块数据保存成功！', true);
      fetchInvestmentParcels();
      resetInvestmentParcelForm();
      if (investmentParcelsLayer) {
        investmentParcelsLayer.getSource().refresh();
      }
    }
  } catch (err) {
    loading.value = false;
    console.error(err);
    triggerToast('招商地块数据保存失败。');
  }
}

function editInvestmentParcel(parcel) {
  investmentParcelForm.id = parcel.properties.id;
  investmentParcelForm.parcel_name = parcel.properties.parcel_name;
  investmentParcelForm.land_use = parcel.properties.land_use;
  investmentParcelForm.area_sqm = parcel.properties.area_sqm;
  investmentParcelForm.far = parcel.properties.far;
  investmentParcelForm.geom_json = JSON.stringify(parcel.geometry.coordinates);
  investmentParcelForm.status = parcel.properties.status;
}

async function deleteInvestmentParcelRecord(parcelId) {
  if (!confirm(`确定要物理删除招商地块 [${parcelId}] 吗？此操作不可逆！`)) return;
  loading.value = true;
  try {
    const res = await axios.delete(`/api/v1/investment/parcels/${encodeURIComponent(parcelId)}`);
    loading.value = false;
    if (res.data) {
      triggerToast('招商地块已安全移除。', true);
      fetchInvestmentParcels();
      if (investmentParcelsLayer) {
        investmentParcelsLayer.getSource().refresh();
      }
    }
  } catch (err) {
    loading.value = false;
    console.error(err);
    triggerToast('删除招商地块失败。');
  }
}

function locateParcelOnMap(parcel) {
  if (!mapInstance) return;
  const coords = parcel.geometry.coordinates[0][0];
  if (coords) {
    const center = fromLonLat([parseFloat(coords[0]), parseFloat(coords[1])]);
    mapInstance.getView().animate({
      center: center,
      zoom: 14,
      duration: 800
    });
    showAdminModal.value = false;
    triggerToast(`📍 正在定位招商地块：[${parcel.properties.parcel_name}]`);
  }
}

async function loadInvestmentIntents() {
  try {
    const res = await axios.get('/api/v1/investment/intents');
    investmentIntents.value = res.data || [];
  } catch (err) {
    console.error(err);
  }
}

async function deleteInvestmentIntentRecord(intentId) {
  if (!confirm('确定要删除这条投资意向记录吗？')) return;
  loading.value = true;
  try {
    const res = await axios.delete(`/api/v1/investment/intents/${intentId}`);
    loading.value = false;
    if (res.data) {
      triggerToast('投资意向记录已删除。', true);
      loadInvestmentIntents();
    }
  } catch (err) {
    loading.value = false;
    console.error(err);
    triggerToast('删除意向记录失败。');
  }
}

function locateReportOnMap(report) {
  if (!mapInstance || !popupOverlay) return;
  const coords = fromLonLat([parseFloat(report.x), parseFloat(report.y)]);
  
  const popupTitle = document.getElementById('popup-title');
  const popupText = document.getElementById('popup-text');
  if (popupTitle && popupText) {
    const statusLabel = report.status === 0 ? '🔴 待处理' : (report.status === 1 ? '🟡 处理中' : '🟢 已解决');
    popupTitle.innerHTML = `⚠️ 市民城管随手拍`;
    popupText.innerHTML = `
      <div class="space-y-1 text-[11px] leading-relaxed text-gray-300">
        <div class="font-extrabold text-white text-xs">问题: ${report.issue_type}</div>
        <div><strong>详情:</strong> ${report.description}</div>
        <div><strong>上报人:</strong> ${report.reporter_name} (${report.contact_phone || '无'})</div>
        <div><strong>状态:</strong> ${statusLabel}</div>
        <div class="text-[9px] text-gray-500 mt-1">${report.created_at.substring(0, 16).replace('T', ' ')}</div>
      </div>
    `;
    popupOverlay.setPosition(coords);
  }
  
  mapInstance.getView().animate({
    center: coords,
    zoom: 16,
    duration: 1200
  });
  
  showAdminModal.value = false;
  triggerToast(`📍 正在定位市民随手拍事件：[${report.issue_type}]`);
}


// 13. 保存数据 (重点工程 / 人文景观)
async function saveAdminRecord() {
  if (!adminForm.id || !adminForm.name || !adminForm.x || !adminForm.y) {
    triggerToast('请完整填写 ID、名称、经度 X 和纬度 Y。');
    return;
  }
  loading.value = true;
  try {
    if (editType.value === 'zdgc') {
      const payload = {
        id: adminForm.id.trim(),
        name: adminForm.name.trim(),
        x: adminForm.x.trim(),
        y: adminForm.y.trim(),
        zoom: Number(adminForm.zoom),
        src: adminForm.src.trim() || '/images/zdgc/10.jpg',
        jieshao: adminForm.jieshao.trim()
      };
      const res = await axios.post('/manage/zdgc', payload)
      loading.value = false;
      if (res.data.code === 200) {
        triggerToast('重点工程记录已成功保存入库！', true);
        fetchProjects();
        resetAdminForm();
      } else {
        triggerToast(res.data.message || '保存失败。');
      }
    } else {
      const payload = {
        id: adminForm.id.trim(),
        name: adminForm.name.trim(),
        x: adminForm.x.trim(),
        y: adminForm.y.trim(),
        src: adminForm.src.trim() || '/images/rwjg/1.jpg',
        jianjie: adminForm.jieshao.trim()
      };
      const res = await axios.post('/manage/rwjg', payload)
      loading.value = false;
      if (res.data.code === 200) {
        triggerToast('人文老村回忆记录已成功保存入库！', true);
        fetchVillages();
        resetAdminForm();
      } else {
        triggerToast(res.data.message || '保存失败。');
      }
    }
  } catch (err) {
    loading.value = false;
    triggerToast('数据持久化提交失败，网络异常。');
  }
}

// 14. 删除数据 (重点工程 / 人文景观)
async function deleteAdminRecord(type, id) {
  if (!confirm(`确定要物理删除记录 [${id}] 吗？该操作不可逆！`)) return;
  loading.value = true;
  try {
    if (type === 'zdgc') {
      const res = await axios.delete(`/manage/zdgc/${encodeURIComponent(id)}`)
      loading.value = false;
      if (res.data.code === 200) {
        triggerToast('重点工程记录已安全移除。', true);
        fetchProjects();
      } else {
        triggerToast(res.data.message || '物理删除失败。');
      }
    } else {
      const res = await axios.delete(`/manage/rwjg/${encodeURIComponent(id)}`)
      loading.value = false;
      if (res.data.code === 200) {
        triggerToast('人文回忆老村落记录已安全移除。', true);
        fetchVillages();
      } else {
        triggerToast(res.data.message || '物理删除失败。');
      }
    }
  } catch (err) {
    loading.value = false;
    triggerToast('移除记录请求失败，网络异常。');
  }
}

function resetAdminForm() {
  adminForm.id = '';
  adminForm.name = '';
  adminForm.x = '';
  adminForm.y = '';
  adminForm.zoom = 12;
  adminForm.src = '';
  adminForm.jieshao = '';
}

function fillAdminForm(item, type) {
  editType.value = type;
  adminForm.id = item.id;
  adminForm.name = item.name;
  adminForm.x = item.x;
  adminForm.y = item.y;
  adminForm.zoom = item.zoom || 12;
  adminForm.src = item.src;
  adminForm.jieshao = type === 'zdgc' ? item.jieshao : item.jianjie;
}

// ========= 15. 商户管理 (SupplyMerchant CRUD) =========
function resetMerchantForm() {
  merchantForm.id = '';
  merchantForm.merchantName = '';
  merchantForm.businessType = '机械租赁';
  merchantForm.contactPerson = '';
  merchantForm.contactPhone = '';
  merchantForm.address = '';
  merchantForm.x = '';
  merchantForm.y = '';
  merchantForm.serviceRadius = 10;
  merchantForm.isVip = false;
  merchantForm.logoUrl = '/images/merchants/m1.png';
  merchantForm.description = '';
  merchantForm.businessHours = '08:00-18:00';
  merchantForm.rating = 5.0;
  merchantForm.verified = true;
}

function fillMerchantForm(item) {
  merchantForm.id = item.id;
  merchantForm.merchantName = item.merchantName;
  merchantForm.businessType = item.businessType;
  merchantForm.contactPerson = item.contactPerson;
  merchantForm.contactPhone = item.contactPhone;
  merchantForm.address = item.address;
  merchantForm.x = item.x;
  merchantForm.y = item.y;
  merchantForm.serviceRadius = item.serviceRadius || 10;
  merchantForm.isVip = !!item.isVip;
  merchantForm.logoUrl = item.logoUrl || '/images/merchants/m1.png';
  merchantForm.description = item.description || '';
  merchantForm.businessHours = item.businessHours || '08:00-18:00';
  merchantForm.rating = item.rating || 5.0;
  merchantForm.verified = !!item.verified;
}

async function saveMerchantRecord() {
  if (!merchantForm.id || !merchantForm.merchantName || !merchantForm.x || !merchantForm.y) {
    triggerToast('请完整填写 商户ID、名称、经度 X 和纬度 Y。');
    return;
  }
  loading.value = true;
  try {
    const payload = {
      id: merchantForm.id.trim(),
      merchantName: merchantForm.merchantName.trim(),
      businessType: merchantForm.businessType.trim(),
      contactPerson: merchantForm.contactPerson.trim(),
      contactPhone: merchantForm.contactPhone.trim(),
      address: merchantForm.address.trim(),
      x: merchantForm.x.trim(),
      y: merchantForm.y.trim(),
      serviceRadius: Number(merchantForm.serviceRadius),
      isVip: Boolean(merchantForm.isVip),
      logoUrl: merchantForm.logoUrl.trim(),
      description: merchantForm.description.trim(),
      businessHours: merchantForm.businessHours.trim(),
      rating: Number(merchantForm.rating),
      verified: Boolean(merchantForm.verified)
    };
    const res = await axios.post('/manage/merchant/save', payload);
    loading.value = false;
    if (res.data.code === 200) {
      triggerToast('商户记录已成功保存入库！', true);
      // 重新加载商户列表
      const listRes = await axios.get('/api/merchant/list');
      if (listRes.data.code === 200) {
        merchants.value = listRes.data.data || [];
        if (activeModule.value === 'business') {
          renderMerchantCirclesOnMap();
        }
      }
      resetMerchantForm();
    } else {
      triggerToast(res.data.message || '保存商户失败。');
    }
  } catch (err) {
    loading.value = false;
    triggerToast('数据持久化提交失败，网络异常。');
  }
}

async function deleteMerchantRecord(id) {
  if (!confirm(`确定要物理删除商户 [${id}] 吗？该操作不可逆！`)) return;
  loading.value = true;
  try {
    const res = await axios.delete(`/manage/merchant/${encodeURIComponent(id)}`);
    loading.value = false;
    if (res.data.code === 200) {
      triggerToast('商户记录已安全移除。', true);
      // 重新加载商户列表
      const listRes = await axios.get('/api/merchant/list');
      if (listRes.data.code === 200) {
        merchants.value = listRes.data.data || [];
        if (activeModule.value === 'business') {
          renderMerchantCirclesOnMap();
        }
      }
    } else {
      triggerToast(res.data.message || '物理删除失败。');
    }
  } catch (err) {
    loading.value = false;
    triggerToast('移除记录请求失败，网络异常。');
  }
}

async function toggleMerchantVip(id, isVip) {
  try {
    const params = new URLSearchParams();
    params.append('id', id);
    params.append('isVip', String(isVip));
    const res = await axios.post('/manage/merchant/set-vip', params, {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    });
    if (res.data.code === 200) {
      triggerToast(isVip ? '商户 VIP 特权已开启！' : '商户 VIP 特权已取消。', true);
      // 重新加载商户列表
      const listRes = await axios.get('/api/merchant/list');
      if (listRes.data.code === 200) {
        merchants.value = listRes.data.data || [];
        if (activeModule.value === 'business') {
          renderMerchantCirclesOnMap();
        }
      }
    }
  } catch (err) {
    triggerToast('设置 VIP 失败，网络异常。');
  }
}

async function toggleMerchantVerify(id, approved) {
  try {
    const params = new URLSearchParams();
    params.append('id', id);
    params.append('approved', String(approved));
    const res = await axios.post('/manage/merchant/audit', params, {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    });
    if (res.data.code === 200) {
      triggerToast(approved ? '商户资质审核已通过！' : '商户资质已驳回。', true);
      // 重新加载商户列表
      const listRes = await axios.get('/api/merchant/list');
      if (listRes.data.code === 200) {
        merchants.value = listRes.data.data || [];
        if (activeModule.value === 'business') {
          renderMerchantCirclesOnMap();
        }
      }
    }
  } catch (err) {
    triggerToast('审核操作失败，网络异常。');
  }
}

// ==================== B. OpenLayers 地图及矢量渲染核心 ====================

// 1. 初始化 OpenLayers
function initOpenLayers() {
  // A. 基础瓦片地图 (OSM)
  baseOsmLayer = new TileLayer({
    title: "OSM Standard",
    source: new XYZ({
      url: "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
      crossOrigin: "anonymous"
    })
  });

  // B. 定制 Marker 打点源和图层
  markerSource = new VectorSource({ wrapX: false });
  markerLayer = new VectorLayer({
    source: markerSource,
    zIndex: 99
  });

  bufferSource = new VectorSource({ wrapX: false });
  bufferLayer = new VectorLayer({
    source: bufferSource,
    zIndex: 90
  });

  investmentParcelsLayer = new VectorLayer({
    source: new VectorSource(),
    zIndex: 85,
    style: new Style({
      fill: new Fill({ color: 'rgba(255, 170, 0, 0.3)' }),
      stroke: new Stroke({ color: '#ffaa00', width: 2 })
    }),
    visible: false
  });

  // C. 实例化 Popup Overlay 容器
  const popupContainer = document.getElementById('map-popup');
  popupOverlay = new Overlay({
    element: popupContainer,
    autoPan: {
      animation: { duration: 250 }
    }
  });

  // D. 统一构建地图对象
  mapInstance = new Map({
    target: 'map',
    layers: [baseOsmLayer, investmentParcelsLayer, bufferLayer, markerLayer],
    overlays: [popupOverlay],
    view: new View({
      projection: 'EPSG:3857',
      center: fromLonLat([116.1174, 38.9751]), // 雄安容城县核心
      zoom: 11
    })
  });

  // E. 监听地图 Marker 与多边形村庄点击
  mapInstance.on('singleclick', function (evt) {
    if (activeModule.value === 'citizen_report') {
      const coords = toLonLat(evt.coordinate);
      citizenForm.value.x = coords[0].toFixed(6);
      citizenForm.value.y = coords[1].toFixed(6);
      showCitizenModal.value = true;
      return;
    }
    if (isPickingCoordinate.value) {
      const coords = toLonLat(evt.coordinate);
      applyForm.value.x = coords[0].toFixed(6);
      applyForm.value.y = coords[1].toFixed(6);
      isPickingCoordinate.value = false;
      triggerToast(`📍 坐标采集成功！经度: ${applyForm.value.x}, 纬度: ${applyForm.value.y}`, true);
      return;
    }
    const hit = mapInstance.forEachFeatureAtPixel(evt.pixel, function (f, layer) {
      return { feature: f, layer: layer };
    });
    if (hit && hit.feature) {
      const f = hit.feature;
      const l = hit.layer;
      if (f.get('data')) {
        const data = f.get('data');
        const coords = f.getGeometry().getCoordinates();
        handleMarkerSelect(data, coords);
      } else if (l === villagesLayer || f.get('village_name')) {
        handleVillagePolygonSelect(f);
      } else if (l === investmentParcelsLayer || f.get('parcel_name')) {
        const data = f.getProperties();
        selectedItem.value = data;
        investmentIntentForm.value.parcel_id = data.id;
      }
    } else {
      popupOverlay.setPosition(undefined);
    }
  });

  // 监听鼠标悬浮多边形面要素，高亮与显示气泡
  mapInstance.on('pointermove', function (evt) {
    if (evt.dragging) return;
    const pixel = mapInstance.getEventPixel(evt.originalEvent);
    let foundFeat = null;
    mapInstance.forEachFeatureAtPixel(pixel, function (f, layer) {
      if (layer === villagesLayer || f.get('village_name')) {
        foundFeat = f;
        return true;
      }
    });

    if (foundFeat) {
      mapInstance.getTargetElement().style.cursor = 'pointer';
      hoveredVillageName.value = foundFeat.get('village_name');
      hoveredVillageStatus.value = foundFeat.get('status');
    } else {
      mapInstance.getTargetElement().style.cursor = '';
      hoveredVillageName.value = '';
      hoveredVillageStatus.value = null;
    }
  });

  // F. 预加载 6 个空间规划图层 (未来道路 dlgh 核心支持)
  initSpatialLayers();
}

// 2. 预载空间规划图层 (矢量文件 quanjie, xianjie, xinan, xioan)
function initSpatialLayers() {
  // A. 规划区界 (xioan.json)
  countyLayer = new VectorLayer({
    title: "三县边界",
    visible: layerToggles.counties,
    source: new VectorSource({
      url: "/geojson/xianjie.json",
      format: new GeoJSON()
    }),
    style: new Style({
      stroke: new Stroke({ color: '#8e44ad', width: 2 }),
      fill: new Fill({ color: 'rgba(142, 68, 173, 0.03)' })
    })
  });

  // B. 规划总边界 (quanjie.json)
  boundaryLayer = new VectorLayer({
    title: "规划总边界",
    visible: layerToggles.boundary,
    source: new VectorSource({
      url: "/geojson/quanjie.json",
      format: new GeoJSON()
    }),
    style: new Style({
      stroke: new Stroke({ color: '#00ecff', width: 3 }),
      fill: new Fill({ color: 'rgba(0, 236, 255, 0.05)' })
    })
  });

  // C. 拆迁与建设区 (xinan.json)
  zonesLayer = new VectorLayer({
    title: "拆迁与建设区",
    visible: layerToggles.zones,
    source: new VectorSource({
      url: "/geojson/xinan.json",
      format: new GeoJSON()
    }),
    style: new Style({
      stroke: new Stroke({ color: '#e74c3c', width: 2.5 }),
      fill: new Fill({ color: 'rgba(231, 76, 60, 0.08)' })
    })
  });

  // D. 主干路 (quanjie.json gold)
  mainRoadLayer = new VectorLayer({
    title: "主干路",
    visible: layerToggles.mainRoad,
    source: new VectorSource({
      url: "/geojson/quanjie.json",
      format: new GeoJSON()
    }),
    style: new Style({
      stroke: new Stroke({ color: '#f39c12', width: 3.5 })
    })
  });

  // E. 道路 (quanjie.json styled blue)
  roadsLayer = new VectorLayer({
    title: "道路",
    visible: layerToggles.roads,
    source: new VectorSource({
      url: "/geojson/quanjie.json",
      format: new GeoJSON()
    }),
    style: new Style({
      stroke: new Stroke({ color: '#3498db', width: 2 })
    })
  });

  // F. R1快线 (quanjie.json dashed orange)
  r1LineLayer = new VectorLayer({
    title: "R1快线",
    visible: layerToggles.r1Line,
    source: new VectorSource({
      url: "/geojson/quanjie.json",
      format: new GeoJSON()
    }),
    style: new Style({
      stroke: new Stroke({
        color: '#e67e22',
        width: 4,
        lineDash: [8, 8]
      })
    })
  });

  // G. 雄安印记·时空村落谱 矢量多边形图层
  villagesLayer = new VectorLayer({
    title: "时空村落谱",
    visible: activeModule.value === 'spatial_villages',
    source: new VectorSource({
      url: "http://localhost:8000/api/v1/spatial/villages",
      format: new GeoJSON()
    }),
    style: function (feature) {
      const status = feature.get('status');
      const demoYear = feature.get('plan_demolition_year');
      
      // 时空滑块年份轴演化逻辑：
      // 大于所选滑动轴年份，说明在选定年份中还未拆除，隐藏（达到视觉上的村庄随年份消失效果）
      if (status !== 2 && demoYear && demoYear > selectedYear.value) {
        return null;
      }
      
      let fillColor = 'rgba(16, 185, 129, 0.35)'; // 绿色：特色保留
      let strokeColor = '#10B981';
      
      if (status === 0 || (demoYear && demoYear <= selectedYear.value)) {
        fillColor = 'rgba(239, 68, 68, 0.3)'; // 红色：已拆除
        strokeColor = '#EF4444';
      } else if (status === 1) {
        fillColor = 'rgba(245, 158, 11, 0.35)'; // 黄色：征迁拆除中
        strokeColor = '#F59E0B';
      }
      
      return new Style({
        fill: new Fill({ color: fillColor }),
        stroke: new Stroke({ color: strokeColor, width: 2 })
      });
    }
  });

  // 按顺序插入到底层瓦片之上，Marker图层之下
  mapInstance.getLayers().insertAt(1, countyLayer);
  mapInstance.getLayers().insertAt(2, boundaryLayer);
  mapInstance.getLayers().insertAt(3, zonesLayer);
  mapInstance.getLayers().insertAt(4, mainRoadLayer);
  mapInstance.getLayers().insertAt(5, roadsLayer);
  mapInstance.getLayers().insertAt(6, r1LineLayer);
  mapInstance.getLayers().insertAt(7, villagesLayer);
}

// 监听 Toggle 变量，动态控制图层显示隐藏
watch(() => layerToggles.boundary, (val) => boundaryLayer.setVisible(val))
watch(() => layerToggles.counties, (val) => countyLayer.setVisible(val))
watch(() => layerToggles.zones, (val) => zonesLayer.setVisible(val))
watch(() => layerToggles.mainRoad, (val) => mainRoadLayer.setVisible(val))
watch(() => layerToggles.roads, (val) => roadsLayer.setVisible(val))
watch(() => layerToggles.r1Line, (val) => r1LineLayer.setVisible(val))

// 监听时空演化轴和模块切换，重绘村庄多边形样式与可见性
watch(selectedYear, () => {
  if (villagesLayer) villagesLayer.getSource().changed();
});
watch(activeModule, (val) => {
  if (villagesLayer) {
    villagesLayer.setVisible(val === 'spatial_villages');
  }
  if (investmentParcelsLayer) {
    investmentParcelsLayer.setVisible(val === 'investment');
  }
  if (val === 'investment') {
    fetchInvestmentParcels();
  }
  if (val === 'citizen_report') {
    loadCitizenReports();
  }
  isAudioPlaying.value = false;
});

// 3. 点击列表或地图打点
function handleMarkerSelect(item, customCoords = null) {
  if (activeModule.value === 'business') {
    if (activeSubTab.value === 'intelligence') {
      activeIntelligenceProject.value = item;
    } else if (activeSubTab.value === 'wholesale') {
      selectedRadarProject.value = item;
      fetchRadarMerchants();
    } else if (activeSubTab.value === 'living') {
      fetchLivingCircleData(item.id);
    }
    
    const coords = customCoords || fromLonLat([parseFloat(item.x), parseFloat(item.y)]);
    mapInstance.getView().animate({
      center: coords,
      zoom: 13.5,
      duration: 1200
    });

    // 1. Double-sided Click-Linkage: Renders popup info window for selected B-end project
    const popupTitle = document.getElementById('popup-title');
    const popupText = document.getElementById('popup-text');
    if (popupTitle && popupText) {
      popupTitle.innerHTML = item.name;
      
      const stages = ['规划许可/即将开工', '主体施工', '人防/消防验收', '综合验收/完工'];
      const stageLabel = stages[item.approval_stage] || '未定';
      
      popupText.innerHTML = `
        <b>建设阶段:</b> ${stageLabel}<br>
        <b>常驻工人:</b> ${item.worker_scale_est || 500}人<br>
        <b>投资规模:</b> ¥ ${(item.bid_amount / 10000).toFixed(1)} 亿元<br>
        <hr style="border-color:#333;margin:8px 0">
        ${item.jieshao || '大基建商业配套中心重点项目'}
      `;
      popupOverlay.setPosition(coords);
    }

    // 2. Double-sided Click-Linkage: Automatically scroll corresponding sidebar card into viewport
    nextTick(() => {
      const el = document.getElementById(`tob-card-${item.id}`);
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      }
    });

    return;
  }

  selectedItem.value = item;
  isAudioPlaying.value = false;
  
  // A. 时空飞行定位 (Fly-to)
  const coords = customCoords || fromLonLat([parseFloat(item.x), parseFloat(item.y)]);
  mapInstance.getView().animate({
    center: coords,
    zoom: 12.8,
    duration: 1200
  });

  // B. Popup 气泡呈现
  const popupTitle = document.getElementById('popup-title');
  const popupText = document.getElementById('popup-text');
  popupTitle.innerHTML = item.name;

  // FR-2.1: 工程弹窗展示全生命周期信息
  if (activeModule.value === 'projects') {
    const statusLabels = {
      'planning': '🟥 规划征迁中',
      'under_construction': '🟨 火热在建中',
      'completed': '🟩 已交付成果'
    };
    const statusHtml = item.status ? `<span style="font-weight:bold;font-size:14px">${statusLabels[item.status] || item.status}</span><br>` : '';
    popupText.innerHTML = `
      ${statusHtml}
      <b>总包单位:</b> ${item.contractor || '-'}<br>
      <b>中标金额:</b> ${item.bidAmount ? item.bidAmount + ' 万元' : '-'}<br>
      <b>开工日期:</b> ${item.startDate || '-'}<br>
      <b>竣工日期:</b> ${item.endDate || '-'}<br>
      <b>当前工况:</b> ${item.workCondition || '-'}<br>
      <hr style="border-color:#333;margin:8px 0">
      ${item.jieshao || ''}
    `;
  } else {
    popupText.innerHTML = item.jieshao || item.jianjie || '点击查看详情';
  }
  
  popupOverlay.setPosition(coords);

  // C. 如果是老村/人文景点模式，拉取它的情感留言板数据
  if (activeModule.value === 'memory') {
    comments.value = [];
    commentForm.content = '';
    fetchComments(item.id);
  }
}

// 选中多边形时空村落要素
function handleVillagePolygonSelect(feature) {
  const props = feature.getProperties();
  const id = props.id;
  const name = props.village_name;
  const history = props.history;
  const status = props.status;
  const year = props.plan_demolition_year;
  isAudioPlaying.value = false;

  selectedItem.value = {
    id: id,
    name: name,
    jianjie: history || `华北平原历史悠久的文化古村落。时空谱显示，本村于 ${year} 年开始大规模征拆变迁，留存着珍贵的乡土回忆。`,
    status: status,
    plan_demolition_year: year
  };

  // Popup 气泡
  const popupTitle = document.getElementById('popup-title');
  const popupText = document.getElementById('popup-text');
  popupTitle.innerHTML = name;

  const statusLabels = {
    0: '🟥 已拆迁腾退',
    1: '🟨 大规模拆迁中',
    2: '🟩 特色保留村落'
  };

  popupText.innerHTML = `
    <b>村庄状态:</b> ${statusLabels[status] || '暂无'}<br>
    <b>变迁年份:</b> ${year ? year + ' 年' : '保留村'}<br>
    <hr style="border-color:#333;margin:8px 0">
    ${selectedItem.value.jianjie}
  `;

  // 计算多边形质心
  const extent = feature.getGeometry().getExtent();
  const center = [(extent[0] + extent[2]) / 2, (extent[1] + extent[3]) / 2];

  mapInstance.getView().animate({
    center: center,
    zoom: 13.2,
    duration: 1000
  });

  popupOverlay.setPosition(center);

  // 联动回忆留言板
  comments.value = [];
  commentForm.content = '';
  fetchComments(id);
}

// 一键定位时空村落
function zoomToVillage(villageId) {
  if (!villagesLayer) return;
  const features = villagesLayer.getSource().getFeatures();
  const feat = features.find(f => f.get('id') === villageId);
  if (feat) {
    handleVillagePolygonSelect(feat);
  } else {
    // 异步安全降级处理：手动构建模拟飞行参数，防止要素未就绪发生报错
    const coordsMap = {
      'v_dawang': [116.130, 38.980],
      'v_xiaowang': [116.110, 38.965],
      'v_nanzhang': [116.090, 38.975],
      'v_huyue': [116.145, 38.960],
      'v_pingwang': [116.160, 38.980]
    };
    const c = coordsMap[villageId];
    if (c) {
      mapInstance.getView().animate({
        center: fromLonLat(c),
        zoom: 13.5,
        duration: 1000
      });
      const names = {
        'v_dawang': '大王镇旧址',
        'v_xiaowang': '小王庄村',
        'v_nanzhang': '南张庄村',
        'v_huyue': '胡阅村',
        'v_pingwang': '平王群落'
      };
      const histories = {
        'v_dawang': '大王镇是雄安新区征迁拆迁的“第一镇”。2018年开始整体搬迁，原址现已拔地而起建设为容东片区核心商业街。',
        'v_xiaowang': '小王庄村位于容城县东侧，于2020年完成整体征拆腾退。该区域已蜕变成为绿油油的“千年秀林”核心林区。',
        'v_nanzhang': '南张庄村在2021年雄安大规模安置区建设中进行腾退。如今原址上建立起了宏伟的容西片区社区。',
        'v_huyue': '胡阅村目前正处于紧张的拆迁流转与过渡补偿交房阶段。村内的百年老树已被原地妥善保护。',
        'v_pingwang': '平王群落属于雄安特色保护保留的村庄群，将作为非物质文化遗产风貌区予以永久保存。'
      };
      selectedItem.value = {
        id: villageId,
        name: names[villageId],
        jianjie: histories[villageId],
        status: villageId === 'v_pingwang' ? 2 : (villageId === 'v_huyue' ? 1 : 0),
        plan_demolition_year: villageId === 'v_pingwang' ? 2030 : (villageId === 'v_dawang' ? 2018 : (villageId === 'v_xiaowang' ? 2020 : 2021))
      };
      fetchComments(villageId);
    }
  }
}

// 4. 重绘 Marker 点 (按模块清空和重载)
function renderMarkersForModule() {
  if (!markerSource) return;
  markerSource.clear();
  popupOverlay.setPosition(undefined);
  selectedItem.value = null;

  let items = [];
  let iconPath = '';
  let scale = 1.0;

  if (activeModule.value === 'projects') {
    items = projects.value;
    iconPath = '/dist/img/markerred.png'; // 重点工程采用红色水滴
  } else if (activeModule.value === 'memory') {
    items = villages.value;
    // 动态应用分类 Tag 筛选
    if (selectedAttractionTag.value !== '全部') {
      items = items.filter(v => v.category === selectedAttractionTag.value);
    }
    iconPath = '/dist/img/ceshi.png';    // 人文景点采用蓝色地标
  }

  items.forEach(item => {
    if (!item.x || !item.y) return;
    const feat = new Feature({
      geometry: new Point(fromLonLat([parseFloat(item.x), parseFloat(item.y)])),
      data: item
    });

    // FR-2.1: 工程 Marker 根据状态使用红/黄/绿三色
    if (activeModule.value === 'projects' && item.status) {
      const statusColor = {
        'planning': '#EF4444',
        'under_construction': '#F59E0B',
        'completed': '#10B981'
      };
      const color = statusColor[item.status] || '#EF4444';
      feat.setStyle(new Style({
        image: new CircleStyle({
          radius: 10,
          fill: new Fill({ color: color }),
          stroke: new Stroke({ color: '#fff', width: 3 })
        })
      }));
    } else {
      feat.setStyle(new Style({
        image: new Icon({
          src: iconPath,
          anchor: [0.5, 1],
          scale: scale
        })
      }));
    }

    markerSource.addFeature(feat);
  });
}

// 5. B端供应商空间服务半径圈打点渲染
function renderMerchantCirclesOnMap() {
  if (!markerSource) return;
  markerSource.clear();
  popupOverlay.setPosition(undefined);
  selectedItem.value = null;

  merchants.value.forEach(item => {
    if (!item.x || !item.y) return;
    const coords = fromLonLat([parseFloat(item.x), parseFloat(item.y)]);
    const feat = new Feature({
      geometry: new Point(coords),
      data: {
        name: item.merchantName,
        jieshao: `业务: ${item.businessType} | 电话: ${item.contactPhone}<br>服务半径: ${item.serviceRadius}公里`
      }
    });

    feat.setStyle(new Style({
      image: new Icon({
        src: '/dist/img/markerred.png',
        anchor: [0.5, 1],
        scale: 0.95
      })
    }));

    markerSource.addFeature(feat);
  });
}

// 监听模块改变，更新打点层
watch(activeModule, () => {
  aiResult.value = null;
  if (bufferSource) bufferSource.clear();
  stopPulseAnimation();
  // 非图层/商机/管理模块，绘制对应的点
  if (activeModule.value === 'projects' || activeModule.value === 'memory') {
    renderMarkersForModule();
  } else if (activeModule.value === 'business') {
    // Left for SubTab handler
  } else {
    markerSource.clear();
    popupOverlay.setPosition(undefined);
  }
});

// ==================== C. 检索逻辑 ====================
function handleGlobalSearch() {
  if (!searchInput.value.trim()) return;
  const q = searchInput.value.trim().toLowerCase();
  
  // 在工程中找
  let found = projects.value.find(p => p.name.toLowerCase().includes(q));
  if (found) {
    activeModule.value = 'projects';
    nextTick(() => { handleMarkerSelect(found); });
    return;
  }

  // 在老村里找
  found = villages.value.find(v => v.name.toLowerCase().includes(q));
  if (found) {
    activeModule.value = 'memory';
    nextTick(() => { handleMarkerSelect(found); });
    return;
  }

  // 利用百度/第三方 Geocoding 模拟模糊飞图
  triggerToast(`“${q}”未在本地数据库打点中检索到，尝试进行全网地理空间检索飞行。`, true);
  // 提供默认飞图
  mapInstance.getView().animate({
    center: fromLonLat([116.13, 38.98]),
    zoom: 13,
    duration: 1500
  });
}

// 一键复制 AI 文案
function copyText(txt) {
  navigator.clipboard.writeText(txt).then(() => {
    triggerToast('AI 时空宣推文案已成功复制到剪贴板！', true);
  }).catch(() => {
    triggerToast('复制失败，请手动选择复制。');
  });
}

// ==================== D. 大基建商业服务生态中心 (ToB Core Logic) ====================

const intelligenceStageFilter = ref('') // '', '0', '1', '2', '3'
const intelligenceInvestmentFilter = ref('') // '', 'billion', 'hundred_million'
const tobProjects = ref([])

// 1. 切换 Sub-Tab 处理器
async function handleSubTabChange(tab) {
  activeSubTab.value = tab;
  
  // 清除现有的 Buffer 圈和 Marker 点
  if (bufferSource) bufferSource.clear();
  if (markerSource) markerSource.clear();
  popupOverlay.setPosition(undefined);
  stopPulseAnimation();

  if (tab === 'intelligence') {
    // 加载工程列表并渲染
    await loadTobProjects();
    renderIntelligenceProjects();
    startPulseAnimation(); // tab 1 uses blinking pulse animation for stage 0
  } else if (tab === 'wholesale') {
    await loadTobProjects();
    if (tobProjects.value.length > 0) {
      if (!selectedRadarProject.value) {
        selectedRadarProject.value = tobProjects.value[0];
      }
      await fetchRadarMerchants();
    }
  } else if (tab === 'living') {
    await loadTobProjects();
    if (tobProjects.value.length > 0) {
      const projId = selectedRadarProject.value ? selectedRadarProject.value.id : tobProjects.value[0].id;
      await fetchLivingCircleData(projId);
    }
  }
}

// 2. 加载 ToB 重点工程
async function loadTobProjects() {
  loading.value = true;
  try {
    let url = 'http://localhost:8000/api/v1/tob/projects/intelligence';
    const params = {};
    if (intelligenceStageFilter.value !== '') {
      params.approval_stage = parseInt(intelligenceStageFilter.value);
    }
    if (intelligenceInvestmentFilter.value !== '') {
      params.total_investment = intelligenceInvestmentFilter.value;
    }
    const res = await axios.get(url, { params });
    loading.value = false;
    if (res.data && res.data.features) {
      tobProjects.value = res.data.features.map(feat => {
        const props = feat.properties;
        const coords = feat.geometry.coordinates;
        props.x = coords[0];
        props.y = coords[1];
        return props;
      });
      
      // Auto-select defaults
      if (tobProjects.value.length > 0) {
        if (!selectedRadarProject.value) selectedRadarProject.value = tobProjects.value[0];
        if (!activeIntelligenceProject.value) activeIntelligenceProject.value = tobProjects.value[0];
      }
    }
  } catch (err) {
    loading.value = false;
    triggerToast('拉取大基建工程情报失败。');
  }
}

// 3. 渲染 Sub-Tab 1: 工程情报点位
function renderIntelligenceProjects() {
  if (!markerSource) return;
  markerSource.clear();
  
  tobProjects.value.forEach(p => {
    if (!p.x || !p.y) return;
    const coords = fromLonLat([parseFloat(p.x), parseFloat(p.y)]);
    const feat = new Feature({
      geometry: new Point(coords),
      data: p
    });

    markerSource.addFeature(feat);
  });
  
  updateIntelligenceStyles();
}

function updateIntelligenceStyles() {
  if (!markerSource) return;
  
  markerSource.getFeatures().forEach(feat => {
    const p = feat.get('data');
    if (!p) return;
    
    const stage = p.approval_stage;
    let color = '#EF4444'; // 阶段 0: 橙红/黄 (规划许可)
    if (stage === 1) color = '#3B82F6'; // 阶段 1: 蓝色/紫色 (主体施工)
    if (stage === 2 || stage === 3) color = '#10B981'; // 阶段 2/3: 绿色 (完工交付)

    const baseStyle = new Style({
      image: new CircleStyle({
        radius: 9,
        fill: new Fill({ color: color }),
        stroke: new Stroke({ color: '#ffffff', width: 2 })
      })
    });
    
    const styles = [baseStyle];
    
    if (stage === 0) {
      const radius = 9 + (animationFrame.value % 10) * 1.5;
      const opacity = 1 - (animationFrame.value % 10) / 10;
      styles.push(new Style({
        image: new CircleStyle({
          radius: radius,
          stroke: new Stroke({ color: `rgba(239, 68, 68, ${opacity})`, width: 2 })
        })
      }));
    }
    
    feat.setStyle(styles);
  });
}

// 4. 渲染 Sub-Tab 2: 大宗供应链 Buffer & 供应商 Marker
async function fetchRadarMerchants() {
  if (!selectedRadarProject.value) return;
  loading.value = true;
  try {
    const projId = selectedRadarProject.value.id;
    const res = await axios.get('http://localhost:8000/api/v1/tob/spatial/radar', {
      params: {
        project_id: projId,
        radius_meters: radarRadius.value,
        sub_category: selectedRadarCategory.value
      }
    });
    loading.value = false;
    if (res.data) {
      radarMerchants.value = res.data.merchants || [];
      renderRadarBufferAndMarkers();
    }
  } catch (err) {
    loading.value = false;
    triggerToast('拉取供应链雷达商户失败。');
  }
}

function renderRadarBufferAndMarkers() {
  if (!markerSource || !bufferSource || !selectedRadarProject.value) return;
  
  markerSource.clear();
  bufferSource.clear();
  
  const projCenter = fromLonLat([parseFloat(selectedRadarProject.value.x), parseFloat(selectedRadarProject.value.y)]);
  const projFeat = new Feature({
    geometry: new Point(projCenter),
    data: selectedRadarProject.value
  });
  
  projFeat.setStyle(new Style({
    image: new CircleStyle({
      radius: 12,
      fill: new Fill({ color: '#00ECFF' }),
      stroke: new Stroke({ color: '#FFFFFF', width: 3 })
    })
  }));
  markerSource.addFeature(projFeat);
  
  const circleGeom = new CircleGeom(projCenter, radarRadius.value);
  const bufferFeat = new Feature(circleGeom);
  bufferFeat.setStyle(new Style({
    fill: new Fill({ color: 'rgba(0, 236, 255, 0.06)' }),
    stroke: new Stroke({ color: 'rgba(0, 236, 255, 0.45)', width: 2, lineDash: [6, 4] })
  }));
  bufferSource.addFeature(bufferFeat);
  
  radarMerchants.value.forEach(m => {
    if (!m.x || !m.y) return;
    const mCoords = fromLonLat([parseFloat(m.x), parseFloat(m.y)]);
    const feat = new Feature({
      geometry: new Point(mCoords),
      data: {
        name: m.merchantName,
        jieshao: `业务: ${m.businessType} | 联系人: ${m.contactPerson}<br>电话: ${m.contactPhone}<br>腹地: ${m.source_region || '雄安本地'}`
      }
    });
    
    const color = m.isVip ? '#F59E0B' : '#3B82F6';
    const strokeColor = m.isVip ? '#FFFFFF' : '#93C5FD';
    const radius = m.isVip ? 9 : 7;
    const strokeWidth = m.isVip ? 3 : 1.5;
    
    feat.setStyle(new Style({
      image: new CircleStyle({
        radius: radius,
        fill: new Fill({ color: color }),
        stroke: new Stroke({ color: strokeColor, width: strokeWidth })
      })
    }));
    markerSource.addFeature(feat);
  });
}

function flyToRegion(region) {
  let center = [116.1174, 38.9751];
  let zoom = 12.8;
  if (region === 'baoding') {
    center = [115.8000, 38.9000];
    zoom = 13.5;
    triggerToast('🚀 正在跨区域平滑飞越至【保定机械仓】制造大后方...');
  } else if (region === 'dingzhou') {
    center = [115.0000, 38.4000];
    zoom = 13.5;
    triggerToast('🚀 正在跨区域平滑飞越至【定州建材带】供应大后方...');
  }
  mapInstance.getView().animate({
    center: fromLonLat(center),
    zoom: zoom,
    duration: 1500
  });
}

async function fetchLivingCircleData(projectId) {
  loading.value = true;
  try {
    const res = await axios.get('http://localhost:8000/api/v1/tob/projects/living-circle', {
      params: { project_id: projectId }
    });
    loading.value = false;
    if (res.data) {
      livingCircleData.value = res.data;
      
      const proj = tobProjects.value.find(p => p.id === projectId);
      if (proj) {
        selectedRadarProject.value = proj;
      }
      
      renderLivingCircleGateAndRipple();
      startPulseAnimation();
    }
  } catch (err) {
    loading.value = false;
    triggerToast('拉取工地生活圈信息失败。');
  }
}

function renderLivingCircleGateAndRipple() {
  if (!markerSource || !bufferSource || !livingCircleData.value) return;
  
  markerSource.clear();
  bufferSource.clear();
  
  const gateCoords = fromLonLat(livingCircleData.value.gate_geom);
  
  const gateFeat = new Feature({
    geometry: new Point(gateCoords),
    data: {
      name: '工地主大门 (门禁及物流主通道)',
      jieshao: `🏢 重点工程: ${livingCircleData.value.project_name}<br>🧑‍🏭 常驻工人预估: ${livingCircleData.value.worker_scale_est} 人<br>🍲 周边餐饮配套: ${livingCircleData.value.catering_saturation === 0 ? '❌ 极度空缺' : '✅ 基本饱和'}`
    }
  });
  
  gateFeat.setStyle(new Style({
    image: new CircleStyle({
      radius: 11,
      fill: new Fill({ color: '#EC4899' }),
      stroke: new Stroke({ color: '#FFFFFF', width: 3 })
    })
  }));
  markerSource.addFeature(gateFeat);
  
  updateLivingCircleRipples();
}

function updateLivingCircleRipples() {
  if (!bufferSource || !livingCircleData.value) return;
  bufferSource.clear();
  
  const gateCoords = fromLonLat(livingCircleData.value.gate_geom);
  
  for (let i = 0; i < 3; i++) {
    const ringFrame = (animationFrame.value + i * 3.3) % 10;
    const currentRadius = 30 + ringFrame * 17;
    const ringOpacity = 0.5 * (1 - ringFrame / 10);
    
    const circleGeom = new CircleGeom(gateCoords, currentRadius);
    const rippleFeat = new Feature(circleGeom);
    
    rippleFeat.setStyle(new Style({
      fill: new Fill({ color: `rgba(244, 63, 94, ${ringOpacity * 0.04})` }),
      stroke: new Stroke({ color: `rgba(244, 63, 94, ${ringOpacity})`, width: 1.5 })
    }));
    bufferSource.addFeature(rippleFeat);
  }
}

// 挂载
onMounted(async () => {
  // 1. 安全校验
  await checkAuth();
  // 2. 拉取工程与回忆
  await fetchProjects();
  await fetchVillages();
  
  // 3. 延时 150ms 挂载地图，确保 DOM 容器 100% 渲染且跨平台节点就绪
  setTimeout(() => {
    initOpenLayers();
    // 初始化时展示区位展示
    activeModule.value = 'showcase';
  }, 150);
});
</script>

<template>
  <div class="relative w-full h-full font-sans select-none overflow-hidden bg-darkBg text-textMain">
    <!-- 1. 全屏唯一 OpenLayers 地图底座 -->
    <div id="map" class="w-full h-full"></div>

    <!-- 时空村落谱 悬停浮动轻量提示卡 (Premium Glassmorphism Overlay) -->
    <div v-if="activeModule === 'spatial_villages' && hoveredVillageName" 
         class="absolute top-24 left-5 px-4 py-2.5 bg-glassBg border border-glassBorder rounded-xl shadow-glass backdrop-blur-xl z-40 text-xs font-bold flex items-center gap-2">
      <i class="fa-solid fa-tree-city text-emerald-400"></i>
      <span class="text-white">{{ hoveredVillageName }}</span>
      <span class="text-[10px] px-1.5 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/25">
        {{ hoveredVillageStatus === 0 ? '已拆迁' : (hoveredVillageStatus === 1 ? '拆迁中' : '特色保留') }}
      </span>
    </div>

    <!-- 时空村落谱 底部年份演化滑动轴 (Timeline Slider) -->
    <div v-if="activeModule === 'spatial_villages'" 
         class="absolute bottom-10 left-1/2 transform -translate-x-1/2 w-1/3 p-4 bg-glassBg border border-glassBorder rounded-2xl shadow-glass backdrop-blur-2xl z-40 flex flex-col gap-2 transition-all duration-300">
      <div class="flex justify-between items-center px-1">
        <span class="text-[11px] font-bold text-emerald-400 flex items-center gap-1.5">
          <i class="fa-solid fa-clock-rotate-left"></i>时空变迁历史演变轴
        </span>
        <span class="text-xs font-extrabold text-white bg-emerald-500/20 px-2 py-0.5 rounded border border-emerald-500/30">{{ selectedYear }} 年</span>
      </div>
      <input type="range" min="2017" max="2026" v-model="selectedYear" 
             class="w-full h-1 bg-gray-700 rounded-lg appearance-none cursor-pointer accent-emerald-500">
      <div class="flex justify-between text-[9px] text-gray-500 font-bold mt-1 px-1">
        <span>2017</span>
        <span>2019</span>
        <span>2021</span>
        <span>2023</span>
        <span>2025</span>
        <span>2026</span>
      </div>
    </div>

    <!-- 2. 全局弹出提示 Toast -->
    <Transition name="fade-slide">
      <div v-if="toast.show" 
           class="absolute top-24 left-1/2 transform -translate-x-1/2 z-50 flex items-center gap-3 px-6 py-4 rounded-xl border backdrop-blur-xl shadow-glass transition-all duration-300"
           :class="toast.isSuccess ? 'bg-emerald-950/85 border-emerald-500/35 text-emerald-300' : 'bg-rose-950/85 border-rose-500/35 text-rose-300'">
        <i class="fa-solid" :class="toast.isSuccess ? 'fa-circle-check text-emerald-400' : 'fa-circle-exclamation text-rose-400'"></i>
        <span class="text-sm font-medium">{{ toast.message }}</span>
      </div>
    </Transition>

    <!-- 3. 顶部磨砂玻璃头部 (Top Translucent Header) -->
    <header class="absolute top-5 left-5 right-5 h-16 flex items-center justify-between px-6 bg-glassBg border border-glassBorder rounded-2xl shadow-glass backdrop-blur-2xl z-40">
      <div class="flex items-center gap-3">
        <div class="w-9 h-9 rounded-lg bg-gradient-to-br from-neonCyan to-neonPurple flex items-center justify-center shadow-cyan-glow">
          <i class="fa-solid fa-earth-asia text-white text-base"></i>
        </div>
        <span class="font-extrabold text-lg tracking-wider text-transparent bg-clip-text bg-gradient-to-r from-white to-gray-400 font-sans">XAGIS HUB 决策中心</span>
      </div>

      <!-- 高精度搜索 -->
      <div class="flex items-center w-1/3 bg-black/40 border border-glassBorder rounded-full px-4 py-1.5 focus-within:border-neonCyan transition-all">
        <input type="text" v-model="searchInput" @keyup.enter="handleGlobalSearch" placeholder="请输入要检索的项目名称/老家村庄..." 
               class="w-full bg-transparent text-sm text-white placeholder-gray-500 outline-none">
        <button @click="handleGlobalSearch" class="text-gray-400 hover:text-neonCyan transition-colors">
          <i class="fa-solid fa-magnifying-glass"></i>
        </button>
      </div>

      <!-- 右侧商户区 -->
      <div class="flex items-center gap-4">
        <!-- ToB 商机行动入口 -->
        <div class="flex items-center gap-2 mr-1">
          <button @click="showApplyModal = true" 
                  class="flex items-center gap-2 px-3 py-1.5 bg-gradient-to-r from-emerald-500/10 to-teal-500/5 hover:from-emerald-500/20 hover:to-teal-500/10 border border-emerald-500/40 hover:border-emerald-400 hover:shadow-[0_0_10px_rgba(16,185,129,0.3)] text-emerald-400 rounded-xl text-xs font-bold transition-all hover:scale-[1.03] active:scale-95 cursor-pointer">
            <span>🧱 供应商入驻</span>
          </button>
          <button @click="openDemandModal" 
                  class="flex items-center gap-2 px-3 py-1.5 bg-gradient-to-r from-amber-500/10 to-orange-500/5 hover:from-amber-500/20 hover:to-orange-500/10 border border-amber-500/40 hover:border-amber-400 hover:shadow-[0_0_10px_rgba(245,158,11,0.3)] text-amber-400 rounded-xl text-xs font-bold transition-all hover:scale-[1.03] active:scale-95 cursor-pointer">
            <span>📢 发布物资求购</span>
          </button>
        </div>
        <div v-if="currentUser" class="flex items-center gap-3">
          <div class="flex flex-col text-right">
            <span class="text-xs text-textMuted uppercase font-semibold tracking-wider">商户通道</span>
            <span class="text-sm font-bold text-neonCyan">{{ currentUser.displayName || currentUser.account }}</span>
          </div>
          <button v-if="currentUser.user_role === 3 || currentUser.account === 'admin'" 
                  @click="openAdminConsole" 
                  title="进入管理后台"
                  class="flex items-center justify-center w-9 h-9 rounded-xl bg-neonRed/10 border border-neonRed/35 hover:bg-neonRed/25 hover:scale-105 active:scale-95 text-neonRed transition-all mr-1 shadow-red-glow">
            <i class="fa-solid fa-sliders text-sm"></i>
          </button>
          <button @click="handleLogout" class="flex items-center gap-2 px-4 py-2 bg-rose-500/10 border border-rose-500/20 rounded-xl hover:bg-rose-500/25 active:scale-95 transition-all text-xs font-bold text-neonRed">
            <i class="fa-solid fa-sign-out-alt"></i>
            <span>退出登录</span>
          </button>
        </div>
        <button v-else @click="showLoginModal = true" class="flex items-center gap-2 px-6 py-2.5 bg-gradient-to-r from-neonCyan to-blue-500 rounded-xl font-bold text-xs text-white shadow-cyan-glow hover:scale-105 active:scale-95 transition-all">
          <i class="fa-solid fa-user-shield"></i>
          <span>商户登录 / 入驻</span>
        </button>
      </div>
    </header>

    <!-- 4. 左侧悬浮导航胶囊 (Left Floating Menu Capsule) -->
    <nav class="absolute top-28 left-5 w-64 flex flex-col p-3 bg-glassBg border border-glassBorder rounded-2xl shadow-glass backdrop-blur-2xl z-40">
      <!-- 宏观底座 -->
      <div class="text-[10px] text-textMuted font-bold uppercase tracking-widest px-3 mb-1.5 flex items-center gap-1.5 text-blue-400">
        <i class="fa-solid fa-server text-[8px]"></i> 宏观底座
      </div>
      <button @click="activeModule = 'showcase'" :class="activeModule === 'showcase' ? 'bg-gradient-to-r from-neonCyan/10 to-blue-500/5 text-neonCyan border-l-2 border-neonCyan font-bold' : 'text-gray-400 hover:text-white'"
              class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm text-left transition-all mb-1">
        <i class="fa-solid fa-layer-group w-5 text-center"></i>
        <span>规划一张图</span>
      </button>

      <!-- 微弱分割线 -->
      <div class="h-px bg-white/5 my-2"></div>

      <!-- C端民生情怀 -->
      <div class="text-[10px] text-textMuted font-bold uppercase tracking-widest px-3 mb-1.5 flex items-center gap-1.5 text-emerald-400">
        <i class="fa-solid fa-heart text-[8px]"></i> C端民生情怀
      </div>
      <button @click="activeModule = 'citizen_report'" :class="activeModule === 'citizen_report' ? 'bg-gradient-to-r from-neonCyan/10 to-teal-500/5 text-neonCyan border-l-2 border-neonCyan font-bold' : 'text-gray-400 hover:text-white'"
              class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm text-left transition-all mb-1">
        <i class="fa-solid fa-camera text-center w-5"></i>
        <span>全民随手拍</span>
      </button>
      <button @click="activeModule = 'spatial_villages'" :class="activeModule === 'spatial_villages' ? 'bg-gradient-to-r from-emerald-600/10 to-teal-500/5 text-emerald-400 border-l-2 border-emerald-500 font-bold' : 'text-gray-400 hover:text-white'"
              class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm text-left transition-all mb-1">
        <i class="fa-solid fa-tree-city w-5 text-center"></i>
        <span>时空村落谱</span>
      </button>
      <button @click="activeModule = 'memory'" :class="activeModule === 'memory' ? 'bg-gradient-to-r from-neonCyan/10 to-teal-500/5 text-neonCyan border-l-2 border-neonCyan font-bold' : 'text-gray-400 hover:text-white'"
              class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm text-left transition-all mb-1">
        <i class="fa-solid fa-camera-retro w-5 text-center"></i>
        <span>人文景点</span>
      </button>

      <!-- 微弱分割线 -->
      <div class="h-px bg-white/5 my-2"></div>

      <!-- B端基建商机 -->
      <div class="text-[10px] text-textMuted font-bold uppercase tracking-widest px-3 mb-1.5 flex items-center gap-1.5 text-amber-500">
        <i class="fa-solid fa-briefcase text-[8px]"></i> B端基建商机
      </div>
      <button @click="activeModule = 'investment'" :class="activeModule === 'investment' ? 'bg-gradient-to-r from-neonGold/10 to-amber-500/5 text-neonGold border-l-2 border-neonGold font-bold' : 'text-gray-400 hover:text-white'"
              class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm text-left transition-all mb-1">
        <i class="fa-solid fa-handshake w-5 text-center"></i>
        <span>招商引资</span>
      </button>
      <button @click="activeModule = 'projects'" :class="activeModule === 'projects' ? 'bg-gradient-to-r from-neonGold/10 to-amber-500/5 text-neonGold border-l-2 border-neonGold font-bold' : 'text-gray-400 hover:text-white'"
              class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm text-left transition-all mb-1">
        <i class="fa-solid fa-tower-observation w-5 text-center"></i>
        <span>重点工程</span>
      </button>
      <button @click="loadBusinessModule" :class="activeModule === 'business' ? 'bg-gradient-to-r from-neonCyan/10 to-blue-500/5 text-neonCyan border-l-2 border-neonCyan font-bold' : 'text-gray-400 hover:text-white'"
              class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm text-left transition-all mb-1">
        <i class="fa-solid fa-truck-ramp-box w-5 text-center text-teal-400"></i>
        <span>商机供应链</span>
      </button>
    </nav>

    <!-- 5. 右侧抽屉式详情展示面板 (Right Slide-out Drawer Panel) -->
    <aside :class="['absolute top-28 right-5 bottom-5 w-[420px] flex flex-col bg-glassBg border border-glassBorder rounded-2xl shadow-glass backdrop-blur-2xl z-40 transition-all duration-300', sidebarCollapsed ? 'translate-x-[calc(100%+20px)]' : 'translate-x-0']">
      
      <!-- Drawer Collapse Toggle Button (Sticks out to the left) -->
      <button @click="toggleSidebar" 
              class="absolute top-1/2 -translate-y-1/2 -left-8 w-8 h-20 bg-glassBg border-y border-l border-glassBorder rounded-l-xl flex items-center justify-center text-gray-400 hover:text-white transition-all backdrop-blur-2xl shadow-[-4px_0_12px_rgba(0,0,0,0.3)] z-50 cursor-pointer">
        <i :class="['fa-solid', sidebarCollapsed ? 'fa-chevron-left' : 'fa-chevron-right', 'text-[10px]']"></i>
      </button>

      <!-- Overflow-hidden wrapper to ensure inner content is clean -->
      <div class="w-full h-full flex flex-col overflow-hidden relative rounded-2xl">
      
      <!-- ==================== 5.0 全局时空数据总览 Home Dashboard ==================== -->
      <div v-if="activeModule === 'home'" class="flex-1 flex flex-col p-6 overflow-y-auto">
        <h3 class="text-lg font-extrabold mb-4 border-b border-white/5 pb-2 text-neonCyan flex items-center gap-2">
          <i class="fa-solid fa-chart-line"></i> 📊 全局时空数据总览
        </h3>
        
        <!-- Welcome Title -->
        <div class="mb-5 p-4 bg-white/5 rounded-xl border border-white/5 text-left">
          <h4 class="text-xs font-bold text-white mb-1">XAGIS HUB 决策大屏</h4>
          <p class="text-[11px] text-textMuted leading-relaxed">
            欢迎进入雄安时空变迁与建设情报平台。当前大屏宏观承化了雄安新区设立以来的空间地理轨迹、村落谱系演演变、大基建工程进度及产业供应链大数据。
          </p>
        </div>

        <!-- Tech Dashboard Cards -->
        <div class="space-y-4">
          <!-- Card 1 -->
          <div class="p-4 bg-gradient-to-br from-amber-500/10 to-transparent border border-amber-500/20 rounded-xl relative overflow-hidden group hover:border-amber-500/40 transition-all text-left">
            <div class="absolute right-3 top-3 opacity-10 text-amber-500 text-3xl group-hover:scale-110 transition-transform">
              <i class="fa-solid fa-tower-observation"></i>
            </div>
            <div class="text-[9px] text-amber-400 font-bold uppercase tracking-wider mb-1">基建情报</div>
            <div class="text-2xl font-black text-white mb-1 flex items-baseline gap-1">
              <span>27</span>
              <span class="text-xs font-normal text-textMuted">个</span>
            </div>
            <div class="text-[11px] text-textMuted">📊 雄安今日活跃在建特大工程</div>
          </div>

          <!-- Card 2 -->
          <div class="p-4 bg-gradient-to-br from-emerald-500/10 to-transparent border border-emerald-500/20 rounded-xl relative overflow-hidden group hover:border-emerald-500/40 transition-all text-left">
            <div class="absolute right-3 top-3 opacity-10 text-emerald-500 text-3xl group-hover:scale-110 transition-transform">
              <i class="fa-solid fa-tree-city"></i>
            </div>
            <div class="text-[9px] text-emerald-400 font-bold uppercase tracking-wider mb-1">乡土回忆</div>
            <div class="text-2xl font-black text-white mb-1 flex items-baseline gap-1">
              <span>142</span>
              <span class="text-xs font-normal text-textMuted">个</span>
            </div>
            <div class="text-[11px] text-textMuted">📦 累计收录已消失的老村庄谱系</div>
          </div>

          <!-- Card 3 -->
          <div class="p-4 bg-gradient-to-br from-neonCyan/10 to-transparent border border-neonCyan/20 rounded-xl relative overflow-hidden group hover:border-neonCyan/40 transition-all text-left">
            <div class="absolute right-3 top-3 opacity-10 text-neonCyan text-3xl group-hover:scale-110 transition-transform">
              <i class="fa-solid fa-truck-ramp-box"></i>
            </div>
            <div class="text-[9px] text-neonCyan font-bold uppercase tracking-wider mb-1">商机撮合</div>
            <div class="text-2xl font-black text-white mb-1 flex items-baseline gap-1">
              <span>538</span>
              <span class="text-xs font-normal text-textMuted">家</span>
            </div>
            <div class="text-[11px] text-textMuted">🚚 腹地联动保定/定州核心供应链企业</div>
          </div>

          <!-- Card 4 -->
          <div class="p-4 bg-gradient-to-br from-pink-500/10 to-transparent border border-pink-500/20 rounded-xl relative overflow-hidden group hover:border-pink-500/40 transition-all text-left">
            <div class="absolute right-3 top-3 opacity-10 text-pink-500 text-3xl group-hover:scale-110 transition-transform">
              <i class="fa-solid fa-comments"></i>
            </div>
            <div class="text-[9px] text-pink-400 font-bold uppercase tracking-wider mb-1">寻根留言</div>
            <div class="text-2xl font-black text-white mb-1 flex items-baseline gap-1">
              <span>1.2k</span>
              <span class="text-xs font-normal text-textMuted">次</span>
            </div>
            <div class="text-[11px] text-textMuted">💬 今日老乡寻根留言/思念墙打卡</div>
          </div>
        </div>

        <!-- Visual Prompt -->
        <div class="mt-6 p-4 bg-white/5 rounded-xl border border-white/5 flex gap-3 items-center text-left">
          <div class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse shrink-0"></div>
          <div class="text-[11px] text-textMuted leading-relaxed">
            👈 请点击左侧功能菜单，深度探索具体业务专题情报。
          </div>
        </div>
      </div>
      
      <!-- ==================== 5.1 规划一张图 Showcase ==================== -->
      <div v-if="activeModule === 'showcase'" class="flex-1 flex flex-col p-6 overflow-y-auto">
        <h3 class="text-lg font-extrabold mb-4 border-b border-white/5 pb-2 text-neonCyan flex items-center gap-2">
          <i class="fa-solid fa-map"></i> 🗺️ 规划一张图控制中心
        </h3>
        
        <p class="text-xs leading-loose text-gray-300 mb-4">
          雄安新区设立于2017年，地处北京、天津、保定腹地。本模块深度融合了宏观区位时空演变概况与本地高保真 ArcGIS 空间规划矢量面、线图层，为您呈现数字化雄安时空底座。
        </p>
        
        <div class="p-3.5 bg-white/2 border border-glassBorder rounded-xl mb-4 text-left">
          <h4 class="text-[10px] font-bold text-textMuted uppercase mb-1">开发状况与时空变迁</h4>
          <p class="text-[11px] text-gray-400 leading-relaxed">截至2026年，起步区、容东片区、昝岗片区等已由规划蓝图走向大规模实景交付。老旧村落全面腾退拆迁，绿色生态秀林与城际轨道相继落成。</p>
        </div>

        <div class="h-px bg-white/5 my-3"></div>

        <!-- 【下半部分】：直接嵌入图层控制 Toggle 滑动开关 -->
        <h4 class="text-xs font-bold text-neonCyan uppercase mb-3 flex items-center gap-1.5">
          <i class="fa-solid fa-sliders"></i> 规划专题图层叠加控制
        </h4>
        
        <div class="space-y-3 text-left">
          <!-- 1. 规划总边界 -->
          <div class="flex items-center justify-between p-3 rounded-xl border border-glassBorder bg-white/2">
            <div class="flex flex-col">
              <span class="text-xs font-bold text-white">🌐 规划总边界</span>
              <span class="text-[9px] text-textMuted mt-0.5">雄安新区整体划定物理边界</span>
            </div>
            <label class="relative inline-flex items-center cursor-pointer">
              <input type="checkbox" v-model="layerToggles.boundary" class="sr-only peer">
              <div class="w-9 h-5 bg-gray-800 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-gray-400 after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-neonCyan peer-checked:after:bg-black"></div>
            </label>
          </div>

          <!-- 2. 三县区划边界 -->
          <div class="flex items-center justify-between p-3 rounded-xl border border-glassBorder bg-white/2">
            <div class="flex flex-col">
              <span class="text-xs font-bold text-white">🏘️ 三县行政边界</span>
              <span class="text-[9px] text-textMuted mt-0.5">雄县/容城/安新行政分界</span>
            </div>
            <label class="relative inline-flex items-center cursor-pointer">
              <input type="checkbox" v-model="layerToggles.counties" class="sr-only peer">
              <div class="w-9 h-5 bg-gray-800 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-gray-400 after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-neonPurple peer-checked:after:bg-black"></div>
            </label>
          </div>

          <!-- 3. 拆迁与建设区 -->
          <div class="flex items-center justify-between p-3 rounded-xl border border-glassBorder bg-white/2">
            <div class="flex flex-col">
              <span class="text-xs font-bold text-white">🚧 西南拆迁建设区</span>
              <span class="text-[9px] text-textMuted mt-0.5">时空拆迁变迁与腾退面数据</span>
            </div>
            <label class="relative inline-flex items-center cursor-pointer">
              <input type="checkbox" v-model="layerToggles.zones" class="sr-only peer">
              <div class="w-9 h-5 bg-gray-800 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-gray-400 after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-neonRed peer-checked:after:bg-black"></div>
            </label>
          </div>

          <!-- 4. 主干路 -->
          <div class="flex items-center justify-between p-3 rounded-xl border border-glassBorder bg-white/2">
            <div class="flex flex-col">
              <span class="text-xs font-bold text-white">🛣️ 未来主干路网</span>
              <span class="text-[9px] text-textMuted mt-0.5">起步区核心主干路网规划</span>
            </div>
            <label class="relative inline-flex items-center cursor-pointer">
              <input type="checkbox" v-model="layerToggles.mainRoad" class="sr-only peer">
              <div class="w-9 h-5 bg-gray-800 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-gray-400 after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-neonGold peer-checked:after:bg-black"></div>
            </label>
          </div>

          <!-- 5. 道路 -->
          <div class="flex items-center justify-between p-3 rounded-xl border border-glassBorder bg-white/2">
            <div class="flex flex-col">
              <span class="text-xs font-bold text-white">🗺️ 普通次干道路</span>
              <span class="text-[9px] text-textMuted mt-0.5">次干路与普通集散网</span>
            </div>
            <label class="relative inline-flex items-center cursor-pointer">
              <input type="checkbox" v-model="layerToggles.roads" class="sr-only peer">
              <div class="w-9 h-5 bg-gray-800 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-gray-400 after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-blue-500 peer-checked:after:bg-black"></div>
            </label>
          </div>

          <!-- 6. R1快线 -->
          <div class="flex items-center justify-between p-3 rounded-xl border border-glassBorder bg-white/2">
            <div class="flex flex-col">
              <span class="text-xs font-bold text-white">🚄 雄安R1快线</span>
              <span class="text-[9px] text-textMuted mt-0.5">连通大兴机场的雄安城际轨道</span>
            </div>
            <label class="relative inline-flex items-center cursor-pointer">
              <input type="checkbox" v-model="layerToggles.r1Line" class="sr-only peer">
              <div class="w-9 h-5 bg-gray-800 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-gray-400 after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-orange-500 peer-checked:after:bg-black"></div>
            </label>
          </div>
        </div>
      </div>

      <!-- ==================== 5.X 全民随手拍 Citizen Report ==================== -->
      <div v-if="activeModule === 'citizen_report'" class="flex-1 flex flex-col overflow-hidden">
        <div class="p-6 border-b border-white/5 flex justify-between items-center">
          <div>
            <h3 class="text-lg font-extrabold text-neonCyan flex items-center gap-2">
              <i class="fa-solid fa-camera"></i> 全民随手拍
            </h3>
            <p class="text-xs text-textMuted mt-1">点击地图位置上报，或在下方浏览已上报的随手拍。</p>
          </div>
          <button @click="openCitizenModal"
                  class="px-4 py-2 bg-neonCyan/10 border border-neonCyan/30 rounded-lg text-xs font-bold text-neonCyan hover:bg-neonCyan/20 transition-all">
            <i class="fa-solid fa-plus mr-1"></i> 新增上报
          </button>
        </div>
        <div class="flex-1 overflow-y-auto p-4 space-y-3" v-if="citizenReports.length">
          <div v-for="r in citizenReports" :key="r.id"
               class="p-3 rounded-xl border border-glassBorder bg-white/2 flex gap-3">
            <img v-if="r.imageUrl" :src="r.imageUrl" class="w-20 h-20 object-cover rounded-lg shrink-0">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <span class="text-[10px] px-1.5 py-0.5 rounded bg-neonCyan/10 text-neonCyan">{{ r.issueType }}</span>
                <span class="text-[10px] text-gray-500">{{ r.reporterName }}</span>
              </div>
              <p class="text-xs text-gray-300 line-clamp-2">{{ r.description }}</p>
              <p class="text-[10px] text-gray-600 mt-1">{{ r.createdAt }}</p>
            </div>
          </div>
        </div>
        <div v-else class="flex-1 p-6 flex flex-col items-center justify-center text-center">
          <i class="fa-solid fa-map-location-dot text-4xl text-gray-600 mb-4 animate-pulse"></i>
          <p class="text-gray-400 text-sm">请在左侧地图中点击，或点击上方按钮开始上报。</p>
        </div>
      </div>

      <!-- ==================== 5.Y 招商引资地块 Investment ==================== -->
      <div v-if="activeModule === 'investment'" class="flex-1 flex flex-col overflow-hidden">
        <div class="p-6 border-b border-white/5">
          <h3 class="text-lg font-extrabold text-neonGold flex items-center gap-2">
            <i class="fa-solid fa-handshake"></i> 招商引资“数字金地”
          </h3>
          <p class="text-xs text-textMuted mt-1">点击地图上高亮的橙色地标多边形，查看地块容积率与控规要求。</p>
        </div>
        
        <div class="flex-1 overflow-y-auto p-6" v-if="selectedItem && selectedItem.parcel_name">
          <h4 class="text-lg font-bold text-white mb-2">{{ selectedItem.parcel_name }}</h4>
          <div class="space-y-2 text-sm text-gray-300 mb-6">
            <p><b>土地用途:</b> {{ selectedItem.land_use }}</p>
            <p><b>面积:</b> {{ selectedItem.area_sqm }} 平方米</p>
            <p><b>容积率 (FAR):</b> {{ selectedItem.far }}</p>
            <p><b>状态:</b> <span class="text-green-400">待出让</span></p>
          </div>
          <button @click="showInvestmentIntentModal = true"
                  class="w-full bg-gradient-to-r from-orange-500 to-amber-500 hover:from-orange-600 hover:to-amber-600 text-white font-bold py-3 rounded-lg shadow-lg">
            <i class="fa-solid fa-paper-plane mr-2"></i> 提交投资意向
          </button>
        </div>
        <div v-else class="flex-1 p-6 flex flex-col items-center justify-center text-center">
          <p class="text-gray-400 text-sm">请在地图上点击招商地块查看详情。</p>
        </div>
      </div>

      <!-- ==================== 5.2 重点工程模块 Projects ==================== -->
      <div v-if="activeModule === 'projects'" class="flex-1 flex flex-col overflow-hidden">
        <div class="p-6 border-b border-white/5">
          <h3 class="text-lg font-extrabold text-neonGold flex items-center gap-2">
            <i class="fa-solid fa-screwdriver-wrench"></i>重点基建工程目录
          </h3>
          <p class="text-xs text-textMuted mt-1">点击工程条目可定位地图并呼出气泡，拉取项目详情。</p>
        </div>
        <div class="flex-1 overflow-y-auto p-4 space-y-3">
          <div v-for="item in projects" :key="item.id"
               @click="handleMarkerSelect(item)"
               class="p-4 rounded-xl border border-glassBorder bg-white/2 hover:bg-white/5 cursor-pointer active:scale-98 transition-all flex items-start gap-4"
               :class="selectedItem?.id === item.id ? 'border-neonGold bg-neonGold/5' : ''">
            <img :src="item.src || '/images/zdgc/10.jpg'" class="w-16 h-12 object-cover rounded-lg border border-glassBorder">
            <div class="flex-1 min-w-0">
              <h4 class="text-sm font-bold text-white truncate flex items-center gap-2">
                {{ item.name }}
                <!-- FR-2.1: 状态标记 -->
                <span v-if="item.status === 'planning'" class="text-[10px] px-1.5 py-0.5 rounded-full bg-red-500/10 text-red-400 border border-red-500/20">规划</span>
                <span v-else-if="item.status === 'under_construction'" class="text-[10px] px-1.5 py-0.5 rounded-full bg-amber-500/10 text-amber-400 border border-amber-500/20">在建</span>
                <span v-else-if="item.status === 'completed'" class="text-[10px] px-1.5 py-0.5 rounded-full bg-green-500/10 text-green-400 border border-green-500/20">已交付</span>
              </h4>
              <p class="text-xs text-gray-400 line-clamp-2 mt-1">{{ item.jieshao }}</p>
              <p v-if="item.contractor" class="text-[10px] text-gray-500 mt-1">🏗️ {{ item.contractor }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- ==================== 5.3 乡村变迁/人文景点模块 Memory ==================== -->
      <div v-if="activeModule === 'memory'" class="flex-1 flex flex-col overflow-hidden">
        <div class="p-6 border-b border-white/5 pb-4">
          <h3 class="text-lg font-extrabold text-neonCyan flex items-center gap-2">
            <i class="fa-solid fa-camera-retro"></i>人文景点博览馆
          </h3>
          <p class="text-xs text-textMuted mt-1">雄安及周边（保定、白洋淀）核心红色文化、生态旅游与历史遗迹。</p>
        </div>

        <!-- 增加前端“分类筛选”标签 Tag Buttons -->
        <div class="flex flex-wrap gap-1.5 px-6 py-3 border-b border-white/5 bg-white/2">
          <button v-for="tag in ['全部', '红色革命', '生态公园', '历史遗存', '现代地标']" :key="tag"
                  @click="selectedAttractionTag = tag"
                  :class="selectedAttractionTag === tag ? 'bg-neonCyan/20 text-neonCyan border-neonCyan' : 'bg-white/3 text-gray-400 border-glassBorder hover:text-white'"
                  class="px-2.5 py-1 rounded-full text-[10px] font-bold border transition-all active:scale-95">
            {{ tag }}
          </button>
        </div>
        
        <div v-if="!selectedItem" class="flex-1 flex flex-col items-center justify-center p-6 text-center">
          <i class="fa-solid fa-camera-retro text-3xl text-gray-600 mb-3"></i>
          <p class="text-sm text-textMuted">请点击地图上的【蓝色地标 Marker】<br>或在下方列表中选择景点进行游览。</p>
          <div class="w-full mt-6 space-y-2 overflow-y-auto max-h-[250px] px-2">
            <div v-for="v in villages.filter(item => selectedAttractionTag === '全部' || item.category === selectedAttractionTag)" :key="v.id" @click="handleMarkerSelect(v)" 
                 class="p-3 text-xs font-bold bg-white/2 border border-glassBorder rounded-xl hover:bg-white/8 cursor-pointer text-left truncate text-gray-300 flex justify-between items-center transition-all">
              <span>🏡 {{ v.name }}</span>
              <span class="text-[9px] px-1.5 py-0.5 rounded bg-neonCyan/10 text-neonCyan border border-neonCyan/25">{{ v.category }}</span>
            </div>
          </div>
        </div>

        <div v-else class="flex-1 overflow-y-auto flex flex-col p-6">
          <div class="relative w-full h-44 rounded-xl overflow-hidden border border-glassBorder mb-4">
            <img :src="selectedItem.src || '/images/rwjg/1.jpg'" class="w-full h-full object-cover">
            <div class="absolute bottom-0 inset-x-0 bg-gradient-to-t from-black/80 to-transparent p-3">
              <span class="text-xs text-neonCyan font-bold uppercase tracking-wider">{{ selectedItem.category }}</span>
              <h4 class="text-base font-extrabold text-white">{{ selectedItem.name }}</h4>
            </div>
          </div>

          <h5 class="text-xs font-bold text-textMuted uppercase mb-1">景点简介</h5>
          <p class="text-xs text-gray-300 leading-relaxed mb-4">{{ selectedItem.jianjie }}</p>

          <!-- 商业化核心亮点扩展 UI -->
          <div class="p-4 bg-white/3 border border-glassBorder rounded-xl mb-4 space-y-4">
            
            <!-- ② 立即打卡按钮 -->
            <button @click="checkinCulturalSpot(selectedItem)" 
                    class="w-full bg-gradient-to-r from-red-500 to-pink-500 hover:from-red-600 hover:to-pink-600 text-white font-bold py-2.5 rounded-lg flex items-center justify-center gap-2 transition-all shadow-[0_0_15px_rgba(239,68,68,0.4)] hover:shadow-[0_0_25px_rgba(239,68,68,0.6)] mb-2">
              <i class="fa-solid fa-location-dot"></i> 📍 立即打卡领红包
            </button>

            <!-- ① 打卡热度图 -->
            <div class="flex flex-col gap-1.5">
              <div class="flex justify-between items-center text-xs font-bold">
                <span class="text-neonCyan flex items-center gap-1">🔥 景区打卡热度</span>
                <span class="text-white">{{ selectedItem.hotScore || 1240 }} 人已打卡</span>
              </div>
              <div class="w-full h-2 bg-gray-800 rounded-full overflow-hidden">
                <div class="h-full bg-gradient-to-r from-neonCyan to-blue-500 rounded-full transition-all duration-500" 
                     :style="{ width: (((selectedItem.hotScore || 1240) - 800) / 10) + '%' }"></div>
              </div>
            </div>

            <!-- ② 一键路线规划 -->
            <div class="flex gap-2">
              <a :href="`https://uri.amap.com/marker?position=${selectedItem.y},${selectedItem.x}&name=${encodeURIComponent(selectedItem.name)}`" 
                 target="_blank"
                 class="flex-1 py-2 bg-gradient-to-r from-neonCyan/20 to-blue-500/20 border border-neonCyan/40 hover:bg-neonCyan/30 active:scale-95 text-center rounded-lg text-xs font-bold text-neonCyan transition-all flex items-center justify-center gap-1.5">
                <i class="fa-solid fa-map-location-dot"></i>
                <span>一键路线规划 (高德)</span>
              </a>
            </div>

            <!-- ③ AI 语音导览播放器 -->
            <div class="flex items-center justify-between p-3 rounded-lg bg-black/40 border border-glassBorder">
              <div class="flex items-center gap-2.5">
                <button @click="isAudioPlaying = !isAudioPlaying" 
                        class="w-8 h-8 rounded-full bg-neonCyan flex items-center justify-center text-black hover:scale-105 active:scale-95 transition-all focus:outline-none">
                  <i class="fa-solid" :class="isAudioPlaying ? 'fa-pause' : 'fa-play'"></i>
                </button>
                <div class="flex flex-col">
                  <span class="text-xs font-bold text-white">🎧 智能 AI 语音导览</span>
                  <span class="text-[9px] text-textMuted mt-0.5">{{ isAudioPlaying ? '语音导览正在播放中' : '收听官方双语语音导览' }}</span>
                </div>
              </div>
              <!-- 动态音波频率小动画 -->
              <div v-if="isAudioPlaying" class="flex items-end gap-0.5 h-3.5">
                <div class="w-0.5 bg-neonCyan animate-pulse h-3"></div>
                <div class="w-0.5 bg-neonCyan animate-pulse h-4.5" style="animation-delay: 0.15s"></div>
                <div class="w-0.5 bg-neonCyan animate-pulse h-2.5" style="animation-delay: 0.3s"></div>
                <div class="w-0.5 bg-neonCyan animate-pulse h-3.5" style="animation-delay: 0.45s"></div>
              </div>
            </div>
          </div>

          <!-- 极光紫 AI 宣推文案按钮 -->
          <button @click="generateAICopywriter" 
                  class="w-full py-3 bg-gradient-to-r from-violet-600 to-indigo-600 border border-violet-500/30 rounded-xl font-bold text-xs shadow-purple-glow hover:scale-[1.02] active:scale-98 transition-all flex items-center justify-center gap-2 mb-6">
            <i class="fa-solid fa-wand-magic-sparkles"></i>
            <span>✨ 唤醒老家记忆：生成 AI 视频/宣推文案</span>
          </button>

          <!-- 留言回忆板 -->
          <div class="border-t border-white/5 pt-4">
            <h5 class="text-xs font-bold text-neonCyan uppercase mb-3 flex items-center justify-between">
              <span>💬 游客留言交流墙 ({{ comments.length }})</span>
            </h5>

            <!-- 留言提交表单 -->
            <div class="space-y-2 mb-4">
              <input type="text" v-model="commentForm.author" placeholder="您的回迁昵称 / 游客昵称 (如: 雁翎队后代)" 
                     class="w-full text-xs bg-black/40 border border-glassBorder rounded-lg px-3 py-2 text-white outline-none focus:border-neonCyan">
              <textarea v-model="commentForm.content" rows="2" placeholder="分享您关于该人文景点的游览感悟或留念寄语吧..." 
                        class="w-full text-xs bg-black/40 border border-glassBorder rounded-lg px-3 py-2 text-white outline-none focus:border-neonCyan resize-none"></textarea>
              <button @click="submitComment" :disabled="loading" class="w-full py-2 bg-white/5 border border-glassBorder hover:bg-white/10 rounded-lg text-xs font-bold transition-all">
                {{ loading ? '留言提交中...' : '寄托思念上墙' }}
              </button>
            </div>

            <!-- 留言列表 -->
            <div class="space-y-3">
              <div v-for="c in comments" :key="c.id" class="p-3 rounded-lg bg-white/2 border border-glassBorder">
                <div class="flex items-center justify-between mb-1">
                  <span class="text-xs font-bold text-neonCyan">👤 {{ c.author }}</span>
                  <span class="text-[9px] text-gray-500">{{ c.createdAt ? c.createdAt.split('T')[0] : '刚刚' }}</span>
                </div>
                <p class="text-xs text-gray-300 leading-normal">{{ c.content }}</p>
              </div>
              <div v-if="comments.length === 0" class="text-center text-[10px] text-textMuted py-4">还没有人留言。快来发表您的第一条景点感言吧！</div>
            </div>
          </div>
        </div>
      </div>

      <!-- ==================== 5.5 时空村落谱模块 Spatial Villages ==================== -->
      <div v-if="activeModule === 'spatial_villages'" class="flex-1 flex flex-col overflow-hidden">
        <div class="p-6 border-b border-white/5 pb-4">
          <h3 class="text-lg font-extrabold text-emerald-400 flex items-center gap-2">
            <i class="fa-solid fa-tree-city"></i>时空老村庄博物馆
          </h3>
          <p class="text-xs text-textMuted mt-1">点击地图上的【村庄多边形面要素】以查阅老村庄历史变迁与思念留言墙。</p>
        </div>

        <div v-if="!selectedItem" class="flex-1 flex flex-col items-center justify-center p-6 text-center">
          <i class="fa-solid fa-map-location text-3xl text-emerald-500/40 mb-3 animate-pulse"></i>
          <p class="text-sm text-textMuted leading-relaxed">请在地图上点击任意【彩色村庄面】<br>或在下方列表中选择回忆对象。</p>
          <div class="w-full mt-6 space-y-2 overflow-y-auto max-h-[250px] px-2">
            <div @click="zoomToVillage('v_dawang')" class="p-3 text-xs font-bold bg-white/2 border border-glassBorder rounded-xl hover:bg-emerald-950/20 hover:border-emerald-500/30 cursor-pointer text-left flex justify-between items-center transition-all">
              <span>🏡 大王镇旧址 (已拆迁)</span>
              <span class="text-[9px] px-1.5 py-0.5 rounded bg-rose-500/10 text-rose-400 border border-rose-500/25">2018年</span>
            </div>
            <div @click="zoomToVillage('v_xiaowang')" class="p-3 text-xs font-bold bg-white/2 border border-glassBorder rounded-xl hover:bg-emerald-950/20 hover:border-emerald-500/30 cursor-pointer text-left flex justify-between items-center transition-all">
              <span>🏡 小王庄村 (已拆迁)</span>
              <span class="text-[9px] px-1.5 py-0.5 rounded bg-rose-500/10 text-rose-400 border border-rose-500/25">2020年</span>
            </div>
            <div @click="zoomToVillage('v_nanzhang')" class="p-3 text-xs font-bold bg-white/2 border border-glassBorder rounded-xl hover:bg-emerald-950/20 hover:border-emerald-500/30 cursor-pointer text-left flex justify-between items-center transition-all">
              <span>🏡 南张庄村 (已拆迁)</span>
              <span class="text-[9px] px-1.5 py-0.5 rounded bg-rose-500/10 text-rose-400 border border-rose-500/25">2021年</span>
            </div>
            <div @click="zoomToVillage('v_huyue')" class="p-3 text-xs font-bold bg-white/2 border border-glassBorder rounded-xl hover:bg-emerald-950/20 hover:border-emerald-500/30 cursor-pointer text-left flex justify-between items-center transition-all">
              <span>🏡 胡阅村 (拆迁中)</span>
              <span class="text-[9px] px-1.5 py-0.5 rounded bg-amber-500/10 text-amber-400 border border-amber-500/25">2024年</span>
            </div>
            <div @click="zoomToVillage('v_pingwang')" class="p-3 text-xs font-bold bg-white/2 border border-glassBorder rounded-xl hover:bg-emerald-950/20 hover:border-emerald-500/30 cursor-pointer text-left flex justify-between items-center transition-all">
              <span>🏡 平王群落 (特色保留)</span>
              <span class="text-[9px] px-1.5 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/25">保留</span>
            </div>
          </div>
        </div>

        <div v-else class="flex-1 overflow-y-auto flex flex-col p-6">
          <div class="relative w-full h-44 rounded-xl overflow-hidden border border-glassBorder mb-4">
            <img :src="selectedItem.src || '/images/rwjg/1.jpg'" class="w-full h-full object-cover">
            <div class="absolute bottom-0 inset-x-0 bg-gradient-to-t from-black/80 to-transparent p-3">
              <span class="text-xs text-emerald-400 font-bold uppercase tracking-wider">老家村落变迁史</span>
              <h4 class="text-base font-extrabold text-white">{{ selectedItem.name }}</h4>
            </div>
          </div>

          <div class="flex items-center justify-between mb-4">
            <h5 class="text-xs font-bold text-textMuted uppercase">村落沿革</h5>
            <span class="text-[10px] px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-300 border border-emerald-500/20 font-bold">
              {{ selectedItem.plan_demolition_year && selectedItem.plan_demolition_year < 2030 ? selectedItem.plan_demolition_year + ' 年变迁' : '特色保留' }}
            </span>
          </div>
          <p class="text-xs text-gray-300 leading-relaxed mb-4">{{ selectedItem.jianjie }}</p>

          <!-- 极光紫 AI 宣推文案按钮 -->
          <button @click="generateAICopywriter" 
                  class="w-full py-3 bg-gradient-to-r from-violet-600 to-indigo-600 border border-violet-500/30 rounded-xl font-bold text-xs shadow-purple-glow hover:scale-[1.02] active:scale-98 transition-all flex items-center justify-center gap-2 mb-6">
            <i class="fa-solid fa-wand-magic-sparkles"></i>
            <span>✨ 唤醒村庄记忆：生成 AI 时空视频脚本</span>
          </button>

          <!-- 留言回忆板 -->
          <div class="border-t border-white/5 pt-4">
            <h5 class="text-xs font-bold text-emerald-400 uppercase mb-3 flex items-center justify-between">
              <span>💬 情感回忆交流墙 ({{ comments.length }})</span>
              <span class="text-[10px] text-textMuted font-normal">留下对村落的老家回忆</span>
            </h5>

            <!-- 留言提交表单 -->
            <div class="space-y-2 mb-4">
              <input type="text" v-model="commentForm.author" placeholder="您的回迁昵称 (如: 容东大姐)" 
                     class="w-full text-xs bg-black/40 border border-glassBorder rounded-lg px-3 py-2 text-white outline-none focus:border-emerald-500">
              <textarea v-model="commentForm.content" rows="2" placeholder="写写那些回迁后依然魂牵梦绕的回忆细节吧..." 
                        class="w-full text-xs bg-black/40 border border-glassBorder rounded-lg px-3 py-2 text-white outline-none focus:border-emerald-500 resize-none"></textarea>
              <button @click="submitComment" :disabled="loading" class="w-full py-2 bg-emerald-500/10 border border-emerald-500/30 hover:bg-emerald-500/20 text-emerald-400 rounded-lg text-xs font-bold transition-all">
                {{ loading ? '留言提交中...' : '寄托思念上墙' }}
              </button>
            </div>

            <!-- 留言列表 -->
            <div class="space-y-3">
              <div v-for="c in comments" :key="c.id" class="p-3 rounded-lg bg-white/2 border border-glassBorder">
                <div class="flex items-center justify-between mb-1">
                  <span class="text-xs font-bold text-emerald-400">👤 {{ c.author }}</span>
                  <span class="text-[9px] text-gray-500">{{ c.createdAt ? c.createdAt.split('T')[0] : '刚刚' }}</span>
                </div>
                <p class="text-xs text-gray-300 leading-normal">{{ c.content }}</p>
              </div>
              <div v-if="comments.length === 0" class="text-center text-[10px] text-textMuted py-4">还没有人留言。快来抢占老村第一面留言回忆墙吧！</div>
            </div>
          </div>
        </div>
      </div>



      <!-- ==================== 5.5 商机供应链模块 Business (Protected) ==================== -->
      <!-- ==================== 5.5 商机供应链模块 Business (Protected) ==================== -->
      <div v-if="activeModule === 'business'" class="flex-1 flex flex-col overflow-hidden">
        <div class="p-5 border-b border-white/5 bg-panelBg/80">
          <h3 class="text-md font-extrabold text-teal-400 flex items-center gap-2">
            <i class="fa-solid fa-industry"></i> 大基建商业服务生态中心
          </h3>
          <p class="text-[11px] text-textMuted mt-1">智慧基建配套与全域供应链商机深度解密</p>
          
          <!-- Sub-Tabs Switcher -->
          <div class="flex mt-4 bg-black/40 rounded-xl p-1 border border-white/5">
            <button @click="handleSubTabChange('intelligence')" 
                    :class="activeSubTab === 'intelligence' ? 'bg-teal-500/20 text-teal-300 font-bold border border-teal-500/30' : 'text-gray-400 hover:text-white'"
                    class="flex-1 text-center py-2 text-xs rounded-lg transition-all">
              工程情报
            </button>
            <button @click="handleSubTabChange('wholesale')" 
                    :class="activeSubTab === 'wholesale' ? 'bg-teal-500/20 text-teal-300 font-bold border border-teal-500/30' : 'text-gray-400 hover:text-white'"
                    class="flex-1 text-center py-2 text-xs rounded-lg transition-all">
              大宗供应链
            </button>
            <button @click="handleSubTabChange('living')" 
                    :class="activeSubTab === 'living' ? 'bg-teal-500/20 text-teal-300 font-bold border border-teal-500/30' : 'text-gray-400 hover:text-white'"
                    class="flex-1 text-center py-2 text-xs rounded-lg transition-all">
              工地生活圈
            </button>
          </div>
        </div>

        <!-- Tab 1 Container: Engineering Intelligence -->
        <div v-if="activeSubTab === 'intelligence'" class="flex-1 overflow-y-auto p-4 space-y-4">
          <!-- Filters -->
          <div class="p-3.5 rounded-xl border border-glassBorder bg-white/2 flex flex-col gap-3">
            <div class="flex items-center justify-between text-xs text-textMain">
              <span><i class="fa-solid fa-filter text-teal-400 mr-1"></i> 条件筛选</span>
            </div>
            
            <div class="grid grid-cols-2 gap-2">
              <div class="flex flex-col gap-1.5 text-left">
                <span class="text-[10px] text-textMuted font-semibold">工程审批/建设阶段</span>
                <select v-model="intelligenceStageFilter" @change="handleSubTabChange('intelligence')"
                        class="bg-black/60 border border-glassBorder rounded-lg px-2.5 py-1.5 text-xs text-white outline-none focus:border-teal-500 transition-colors">
                  <option value="">全部阶段</option>
                  <option value="0">即将开工/规划许可</option>
                  <option value="1">主体施工</option>
                  <option value="2">人防/消防验收</option>
                  <option value="3">综合验收/完工</option>
                </select>
              </div>
              
              <div class="flex flex-col gap-1.5 text-left">
                <span class="text-[10px] text-textMuted font-semibold">投资规模</span>
                <select v-model="intelligenceInvestmentFilter" @change="handleSubTabChange('intelligence')"
                        class="bg-black/60 border border-glassBorder rounded-lg px-2.5 py-1.5 text-xs text-white outline-none focus:border-teal-500 transition-colors">
                  <option value="">全部规模</option>
                  <option value="billion">百亿级特大项目</option>
                  <option value="hundred_million">十亿级骨干项目</option>
                </select>
              </div>
            </div>
          </div>

          <!-- Project List -->
          <div class="space-y-2">
            <div class="text-[11px] font-bold text-textMuted flex items-center justify-between px-1">
              <span>匹配到工程情报 ({{ tobProjects.length }} 个)</span>
              <span class="text-teal-400 text-[10px]"><i class="fa-solid fa-wave-square animate-pulse"></i> 实时感知中</span>
            </div>

            <div v-for="p in tobProjects" :key="p.id"
                 :id="'tob-card-' + p.id"
                 @click="handleMarkerSelect(p)"
                 :class="activeIntelligenceProject?.id === p.id ? 'border-teal-500 bg-teal-500/5 shadow-teal-glow' : 'border-glassBorder bg-white/2 hover:bg-white/5'"
                 class="p-4 rounded-xl border flex flex-col gap-2.5 cursor-pointer transition-all">
              <div class="flex items-center justify-between">
                <span class="text-[10px] px-2 py-0.5 rounded-full font-bold border"
                      :class="p.approval_stage === 0 ? 'bg-red-500/10 text-red-400 border-red-500/20' :
                              p.approval_stage === 1 ? 'bg-amber-500/10 text-amber-300 border-amber-500/20' :
                              p.approval_stage === 2 ? 'bg-blue-500/10 text-blue-300 border-blue-500/20' :
                              'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'">
                  {{ p.approval_stage === 0 ? '🔴 规划许可' : 
                     p.approval_stage === 1 ? '🟡 主体施工' :
                     p.approval_stage === 2 ? '🔵 消防验收' : '🟢 综合验收' }}
                </span>
                <span class="text-[11px] font-bold text-teal-400">¥ {{ (p.bid_amount / 10000).toFixed(1) }} 亿元</span>
              </div>
              <h4 class="text-sm font-extrabold text-white text-left">{{ p.name }}</h4>
              
              <!-- Timeline component for selected project -->
              <div v-if="activeIntelligenceProject?.id === p.id" class="mt-3 pt-3 border-t border-white/5 text-left">
                <span class="text-[10px] text-textMuted font-bold uppercase tracking-wider block mb-2">
                  <i class="fa-solid fa-road-barrier mr-1 text-teal-400"></i> 工程审批/建设生命周期时间轴
                </span>
                <div class="relative pl-4 border-l border-white/10 space-y-3.5 my-2">
                  <div class="relative">
                    <div class="absolute -left-[20.5px] top-1 w-2.5 h-2.5 rounded-full border border-black transition-all"
                         :class="p.approval_stage === 0 ? 'bg-[#2ECC71] shadow-[0_0_8px_#2ECC71]' : 'bg-gray-800'"></div>
                    <span class="text-xs block font-bold transition-all" :class="p.approval_stage === 0 ? 'text-[#2ECC71] font-extrabold animate-pulse' : 'text-gray-500 font-normal'">规划许可 & 即将开工</span>
                    <span class="text-[10px] block transition-all" :class="p.approval_stage === 0 ? 'text-white' : 'text-gray-600'">大宗原材料及重型机具抓紧进场匹配</span>
                  </div>
                  <div class="relative">
                    <div class="absolute -left-[20.5px] top-1 w-2.5 h-2.5 rounded-full border border-black transition-all"
                         :class="p.approval_stage === 1 ? 'bg-[#2ECC71] shadow-[0_0_8px_#2ECC71]' : 'bg-gray-800'"></div>
                    <span class="text-xs block font-bold transition-all" :class="p.approval_stage === 1 ? 'text-[#2ECC71] font-extrabold animate-pulse' : 'text-gray-500 font-normal'">主体结构施工</span>
                    <span class="text-[10px] block transition-all" :class="p.approval_stage === 1 ? 'text-white' : 'text-gray-600'">工地生活配套圈餐饮超市消费极度旺盛期</span>
                  </div>
                  <div class="relative">
                    <div class="absolute -left-[20.5px] top-1 w-2.5 h-2.5 rounded-full border border-black transition-all"
                         :class="p.approval_stage === 2 ? 'bg-[#2ECC71] shadow-[0_0_8px_#2ECC71]' : 'bg-gray-800'"></div>
                    <span class="text-xs block font-bold transition-all" :class="p.approval_stage === 2 ? 'text-[#2ECC71] font-extrabold animate-pulse' : 'text-gray-500 font-normal'">人防与消防验收</span>
                    <span class="text-[10px] block transition-all" :class="p.approval_stage === 2 ? 'text-white' : 'text-gray-600'">设备调试及装饰安装等供应链后期保障</span>
                  </div>
                  <div class="relative">
                    <div class="absolute -left-[20.5px] top-1 w-2.5 h-2.5 rounded-full border border-black transition-all"
                         :class="p.approval_stage === 3 ? 'bg-[#2ECC71] shadow-[0_0_8px_#2ECC71]' : 'bg-gray-800'"></div>
                    <span class="text-xs block font-bold transition-all" :class="p.approval_stage === 3 ? 'text-[#2ECC71] font-extrabold animate-pulse' : 'text-gray-500 font-normal'">综合验收与交付</span>
                    <span class="text-[10px] block transition-all" :class="p.approval_stage === 3 ? 'text-white' : 'text-gray-600'">项目部退场、生活圈迁移至下一片区</span>
                  </div>
                </div>
                
                <div class="mt-3 grid grid-cols-2 gap-2 text-[10px]">
                  <div class="bg-black/40 rounded-lg p-2 border border-white/5">
                    <span class="text-textMuted block">常驻工人数</span>
                    <span class="text-white font-extrabold text-xs">{{ p.worker_scale_est }} 人</span>
                  </div>
                  <div class="bg-black/40 rounded-lg p-2 border border-white/5">
                    <span class="text-textMuted block">餐饮配套饱和度</span>
                    <span class="font-extrabold text-xs" :class="p.catering_saturation === 0 ? 'text-red-400' : 'text-emerald-400'">
                      {{ p.catering_saturation === 0 ? '❌ 极度空缺' : '✅ 基本饱和' }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Tab 2 Container: Wholesale Supply Chain -->
        <div v-if="activeSubTab === 'wholesale'" class="flex-1 overflow-y-auto p-4 space-y-4">
          <!-- Controls Panel -->
          <div class="p-4 rounded-xl border border-glassBorder bg-white/2 flex flex-col gap-3.5 text-left">
            <div class="flex flex-col gap-1.5">
              <span class="text-[11px] text-teal-400 font-bold uppercase"><i class="fa-solid fa-compass-drafting mr-1"></i> 工程核心锚点</span>
              <select v-model="selectedRadarProject" @change="fetchRadarMerchants"
                      class="bg-black/60 border border-glassBorder rounded-lg px-2.5 py-1.5 text-xs text-white outline-none focus:border-teal-500 transition-colors w-full">
                <option v-for="p in tobProjects" :key="p.id" :value="p">{{ p.name }}</option>
              </select>
            </div>

            <!-- Buffer Radius Slider -->
            <div class="flex flex-col gap-1.5">
              <div class="flex items-center justify-between text-xs">
                <span class="text-[11px] text-teal-400 font-bold uppercase"><i class="fa-solid fa-circle-notch mr-1"></i> 供应链雷达检索半径</span>
                <span class="text-white font-bold">{{ (radarRadius / 1000).toFixed(1) }} km</span>
              </div>
              <input type="range" min="1000" max="15000" step="500" v-model="radarRadius" @input="fetchRadarMerchants"
                     class="w-full h-1 bg-gray-800 rounded-lg appearance-none cursor-pointer accent-teal-400">
              <div class="flex items-center justify-between text-[9px] text-textMuted">
                <span>1.0 km</span>
                <span>5.0 km</span>
                <span>10.0 km</span>
                <span>15.0 km</span>
              </div>
            </div>

            <!-- Categories Select -->
            <div class="flex flex-col gap-1.5">
              <span class="text-[11px] text-teal-400 font-bold uppercase"><i class="fa-solid fa-tags mr-1"></i> 商户供应分类</span>
              <div class="grid grid-cols-3 gap-1">
                <button v-for="cat in [
                  { id: 'all', name: '全部' },
                  { id: 'heavy_machinery', name: '重型机械' },
                  { id: 'hardware', name: '五金建材' },
                  { id: 'ceylon_bento', name: '工地快餐' },
                  { id: 'minisuper', name: '平价超市' },
                  { id: 'local_labor', name: '劳务输出' }
                ]" :key="cat.id" 
                        @click="selectedRadarCategory = cat.id; fetchRadarMerchants()"
                        :class="selectedRadarCategory === cat.id ? 'bg-teal-500/20 text-teal-300 font-bold border-teal-500/30' : 'bg-black/40 text-gray-400 hover:text-white border-white/5'"
                        class="text-[10px] text-center py-1.5 rounded-lg border transition-all truncate text-left pl-1">
                  {{ cat.name }}
                </button>
              </div>
            </div>

            <!-- Coordinated Flyovers -->
            <div class="flex flex-col gap-1.5 pt-2 border-t border-white/5">
              <span class="text-[11px] text-teal-400 font-bold uppercase"><i class="fa-solid fa-plane-departure mr-1"></i> 跨区域供应链协作飞越</span>
              <div class="grid grid-cols-2 gap-2">
                <button @click="flyToRegion('baoding')" 
                        class="bg-blue-500/10 text-blue-300 hover:bg-blue-500/20 border border-blue-500/20 text-[11px] py-2 rounded-lg font-bold transition-all flex items-center justify-center gap-1.5">
                  <i class="fa-solid fa-warehouse text-xs"></i> 保定机械仓
                </button>
                <button @click="flyToRegion('dingzhou')" 
                        class="bg-emerald-500/10 text-emerald-300 hover:bg-emerald-500/20 border border-emerald-500/20 text-[11px] py-2 rounded-lg font-bold transition-all flex items-center justify-center gap-1.5">
                  <i class="fa-solid fa-trowel-bricks text-xs"></i> 定州建材带
                </button>
              </div>
            </div>
          </div>

          <!-- Radar Results -->
          <div class="space-y-2">
            <div class="text-[11px] font-bold text-textMuted flex items-center justify-between px-1">
              <span>雷达扫描到服务供应商 ({{ radarMerchants.length }} 个)</span>
            </div>

            <div v-for="m in radarMerchants" :key="m.id"
                 class="p-4 rounded-xl border border-glassBorder bg-white/2 flex flex-col gap-2 text-left">
              <div class="flex items-center justify-between">
                <span class="text-[10px] px-2 py-0.5 rounded-full font-bold"
                      :class="m.isVip ? 'bg-amber-500/10 text-amber-300 border border-amber-500/20 shadow-gold-glow' : 'bg-gray-500/10 text-gray-400 border border-gray-500/10'">
                  {{ m.isVip ? '💎 VIP订阅' : '普通商户' }}
                </span>
                <span class="text-[10px] text-teal-400 font-bold">
                  {{ m.sub_category === 'heavy_machinery' ? '🏗️ 重型机械' :
                     m.sub_category === 'hardware' ? '🛠️ 五金建材' :
                     m.sub_category === 'ceylon_bento' ? '🍱 工地快餐' :
                     m.sub_category === 'minisuper' ? '🛒 平价超市' : '🧑‍🤝‍🧑 劳务输出' }}
                </span>
              </div>
              <div class="flex items-start justify-between">
                <h4 class="text-sm font-extrabold text-white mt-1">{{ m.merchantName }}</h4>
                <span class="text-[10px] text-teal-400 font-bold"><i class="fa-solid fa-location-arrow"></i> {{ (m.distance_meters / 1000).toFixed(2) }} km</span>
              </div>
              <p class="text-xs text-gray-400"><i class="fa-solid fa-map-location-dot mr-1 text-teal-500"></i>{{ m.address }}</p>
              <div class="flex items-center justify-between text-[10px] text-textMuted pt-1">
                <span>📡 覆盖能力: {{ m.serviceRadius || '-' }} km</span>
                <span>🌾 货源腹地: {{ m.source_region || '雄安本地' }}</span>
              </div>
              <div class="flex items-center justify-between mt-2 pt-2 border-t border-white/5">
                <span class="text-xs text-textMuted">📞 联系人: {{ m.contactPerson }}</span>
                <a :href="'tel:' + m.contactPhone" class="text-xs font-bold text-neonCyan bg-neonCyan/10 px-3 py-1 rounded-lg border border-neonCyan/20 hover:bg-neonCyan/20 transition-all">
                  拨打电话: {{ m.contactPhone }}
                </a>
              </div>
            </div>
          </div>
        </div>

        <!-- Tab 3 Container: 工地生活圈 Living Circle -->
        <div v-if="activeSubTab === 'living'" class="flex-1 overflow-y-auto p-4 space-y-4">
          <!-- Controls Panel -->
          <div class="p-4 rounded-xl border border-glassBorder bg-white/2 flex flex-col gap-3.5 text-left">
            <div class="flex flex-col gap-1.5">
              <span class="text-[11px] text-teal-400 font-bold uppercase"><i class="fa-solid fa-tree-city mr-1"></i> 工地配套研究锚点</span>
              <select v-model="selectedRadarProject" @change="fetchLivingCircleData(selectedRadarProject.id)"
                      class="bg-black/60 border border-glassBorder rounded-lg px-2.5 py-1.5 text-xs text-white outline-none focus:border-teal-500 transition-colors w-full">
                <option v-for="p in tobProjects" :key="p.id" :value="p">{{ p.name }}</option>
              </select>
            </div>

            <!-- Consumption Intel Cards -->
            <div v-if="livingCircleData" class="space-y-2">
              <div class="text-[11px] text-teal-400 font-bold uppercase border-b border-white/5 pb-1"><i class="fa-solid fa-chart-line mr-1"></i> 工地生活消费情报</div>
              
              <div class="grid grid-cols-2 gap-2 text-left">
                <div class="bg-black/40 rounded-xl p-3 border border-white/5">
                  <span class="text-[10px] text-textMuted block">常驻工人规模</span>
                  <span class="text-white font-extrabold text-md">{{ livingCircleData.worker_scale_est }} 人</span>
                </div>
                <div class="bg-black/40 rounded-xl p-3 border border-white/5">
                  <span class="text-[10px] text-textMuted block">周边配套饱和度</span>
                  <span class="font-extrabold text-[10px] block mt-0.5" :class="livingCircleData.catering_saturation === 0 ? 'text-red-400' : 'text-emerald-400'">
                    {{ livingCircleData.catering_saturation === 0 ? '极度空缺 - 进场黄金期' : '基本饱和 - 竞争激烈' }}
                  </span>
                </div>
              </div>

              <div class="bg-rose-500/10 border border-rose-500/20 rounded-xl p-3.5 flex items-start gap-2.5">
                <i class="fa-solid fa-circle-exclamation text-rose-400 mt-0.5 text-sm"></i>
                <div class="flex flex-col text-left">
                  <span class="text-xs font-bold text-rose-300">消费人流热点时段</span>
                  <span class="text-[10px] text-rose-400 mt-1 leading-relaxed">
                    大门周边 200 米已渲染玫瑰红“高密度消费引力波”。预计中午 11:20 - 12:40 将涌现大量就餐人潮，是快餐盒饭配送、便利零售的绝对引力场！
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- Purchase Broadcast Logistical Wall -->
          <div class="space-y-2">
            <div class="text-[11px] font-bold text-textMuted flex items-center justify-between px-1">
              <span>项目后勤求购/灵工包广播墙 (滚动中)</span>
              <span class="text-rose-400 text-[10px] flex items-center gap-1">
                <span class="relative flex h-2 w-2">
                  <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-rose-400 opacity-75"></span>
                  <span class="relative inline-flex rounded-full h-2 w-2 bg-rose-500"></span>
                </span>
                LIVE
              </span>
            </div>

            <!-- Custom scrolling container simulating broadcast marquee -->
            <div class="border border-glassBorder bg-black/60 rounded-2xl overflow-hidden max-h-[280px]">
              <div class="p-3 space-y-2.5 overflow-y-auto max-h-[275px]">
                <div v-for="b in livingCircleData?.broadcasts" :key="b.id"
                     class="p-3 bg-white/2 border border-white/5 rounded-xl flex flex-col gap-1.5 text-left text-xs transition-all hover:bg-white/5">
                  <div class="flex items-center justify-between">
                    <span class="text-[9px] font-extrabold px-1.5 py-0.5 rounded"
                          :class="b.type === 'catering' ? 'bg-orange-500/10 text-orange-400 border border-orange-500/20' :
                                  b.type === 'materials' ? 'bg-blue-500/10 text-blue-400 border border-blue-500/20' :
                                  b.type === 'labor' ? 'bg-purple-500/10 text-purple-400 border border-purple-500/20' :
                                  'bg-teal-500/10 text-teal-400 border border-teal-500/20'">
                      {{ b.type === 'catering' ? '🍱 快餐求购' :
                         b.type === 'materials' ? '🦺 后勤物资' :
                         b.type === 'labor' ? '🏗️ 灵工众包' : '🛒 超市货架' }}
                    </span>
                    <span class="text-[10px] text-textMuted">{{ b.time }}</span>
                  </div>
                  <p class="text-white font-medium leading-relaxed text-[11px]">{{ b.content }}</p>
                  <div class="flex justify-end pt-1 border-t border-white/5">
                    <button @click="triggerToast('项目采购通道已激活，建议联系拨打项目部电话。', true)"
                            class="text-[9px] font-bold text-teal-400 hover:text-teal-300">
                      立即联系响应 <i class="fa-solid fa-arrow-right-long ml-0.5"></i>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>


      </div>
    </aside>

    <!-- 6. OpenLayers HTML Popup Container -->
    <div id="map-popup" class="ol-popup">
      <div class="popup-head flex items-center justify-between pb-1 border-b border-white/10 mb-2">
        <div id="popup-title" class="font-extrabold text-sm text-neonCyan"></div>
        <a href="#" id="popup-closer" class="ol-popup-closer"></a>
      </div>
      <div id="popup-text" class="text-xs text-gray-300 leading-relaxed max-w-[280px] break-words"></div>
    </div>

    <!-- 7. AI 文案大师毛玻璃展示浮层 (AI Marketing Copywriter Overlay) -->
    <div v-if="aiResult" class="absolute inset-0 bg-black/60 backdrop-blur-md flex items-center justify-center z-50 p-6">
      <div class="w-full max-w-2xl bg-panelBg border border-glassBorder rounded-3xl p-8 shadow-glass flex flex-col gap-6 max-h-[85vh] overflow-y-auto relative animate-scaleUp">
        
        <button @click="aiResult = null" class="absolute top-6 right-6 text-gray-400 hover:text-white transition-colors text-xl">
          <i class="fa-solid fa-times"></i>
        </button>

        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-violet-600 to-indigo-600 flex items-center justify-center shadow-purple-glow">
            <i class="fa-solid fa-wand-magic-sparkles text-white"></i>
          </div>
          <div class="flex flex-col text-left">
            <span class="text-xs text-textMuted uppercase font-semibold">FastAPI AI Agent 集群合作成果</span>
            <h3 class="text-lg font-extrabold text-transparent bg-clip-text bg-gradient-to-r from-violet-400 to-indigo-400">
              “{{ selectedItem?.name }}” - AI 情感回忆宣推大师
            </h3>
          </div>
        </div>

        <p class="text-xs text-textMuted text-left border-b border-white/5 pb-2">
          基于 DeepSeek LLM 高级情感分析算法，为您的毕业设计老村落一键全自动生成商用引流方案。
        </p>

        <!-- 1. 小红书推文 -->
        <div class="flex flex-col gap-2 text-left">
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold text-neonCyan flex items-center gap-1.5">
              <i class="fa-solid fa-book-open"></i> 📖 小红书爆款怀旧正文
            </span>
            <button @click="copyText(aiResult.xiaohongshu_copy)" class="text-[10px] font-bold text-violet-400 bg-violet-500/10 px-3 py-1 rounded-lg border border-violet-500/20 hover:bg-violet-500/20 transition-all">
              一键复制正文
            </button>
          </div>
          <div class="p-4 rounded-xl bg-black/50 border border-glassBorder text-xs text-gray-300 leading-loose whitespace-pre-wrap max-h-[160px] overflow-y-auto">
            {{ aiResult.xiaohongshu_copy }}
          </div>
        </div>

        <!-- 2. 抖音脚本 -->
        <div class="flex flex-col gap-2 text-left">
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold text-neonGold flex items-center gap-1.5">
              <i class="fa-solid fa-video"></i> 🎬 抖音/短视频旁白与分镜脚本
            </span>
            <button @click="copyText(aiResult.tiktok_script)" class="text-[10px] font-bold text-violet-400 bg-violet-500/10 px-3 py-1 rounded-lg border border-violet-500/20 hover:bg-violet-500/20 transition-all">
              一键复制脚本
            </button>
          </div>
          <div class="p-4 rounded-xl bg-black/50 border border-glassBorder text-xs text-gray-300 leading-loose whitespace-pre-wrap max-h-[160px] overflow-y-auto font-mono">
            {{ aiResult.tiktok_script }}
          </div>
        </div>
      </div>
    </div>

    <!-- AI 生成加载中 Mask -->
    <div v-if="aiLoading" class="absolute inset-0 bg-black/75 backdrop-blur-sm flex flex-col items-center justify-center z-50">
      <div class="w-16 h-16 rounded-2xl bg-gradient-to-br from-violet-600 to-indigo-600 flex items-center justify-center shadow-purple-glow animate-spin mb-4">
        <i class="fa-solid fa-wand-magic-sparkles text-white text-xl"></i>
      </div>
      <p class="text-sm font-extrabold text-violet-300">DeepSeek AI 时空回忆文案大师正在研磨字句...</p>
      <p class="text-xs text-textMuted mt-1">大模型正融合该村庄沿革、回迁片区、华北地貌进行高感性创作...</p>
    </div>

    <!-- 8. Lazy Auth 全局登录/注册/重置模态框 (Glassmorphic Auth Modal) -->
    <Transition name="fade">
      <div v-if="showLoginModal" class="absolute inset-0 bg-black/50 backdrop-blur-md flex items-center justify-center z-50 p-6">
        <div class="w-[400px] bg-panelBg border border-glassBorder rounded-3xl p-8 shadow-glass flex flex-col gap-6 relative animate-scaleUp">
          
          <button @click="showLoginModal = false; deferredCallback = null" class="absolute top-6 right-6 text-gray-400 hover:text-white transition-colors text-lg">
            <i class="fa-solid fa-times"></i>
          </button>

          <div class="flex items-center gap-3">
            <div class="w-9 h-9 rounded-lg bg-gradient-to-br from-neonCyan to-blue-500 flex items-center justify-center shadow-cyan-glow">
              <i class="fa-solid fa-user-lock text-white"></i>
            </div>
            <div class="flex flex-col text-left">
              <span class="text-[10px] text-textMuted uppercase font-bold tracking-wider">安全授权中心</span>
              <h3 class="text-base font-extrabold text-white">雄安时空商户登录</h3>
            </div>
          </div>

          <!-- Tab 切换 -->
          <div class="flex p-1 bg-black/40 border border-glassBorder rounded-xl">
            <button @click="loginMode = 'login'" :class="loginMode === 'login' ? 'bg-gradient-to-r from-neonCyan/10 to-blue-500/5 text-neonCyan border border-neonCyan/20' : 'text-gray-400'"
                    class="flex-1 py-1.5 rounded-lg text-xs font-bold transition-all">
              登录
            </button>
            <button @click="loginMode = 'register'" :class="loginMode === 'register' ? 'bg-gradient-to-r from-neonCyan/10 to-blue-500/5 text-neonCyan border border-neonCyan/20' : 'text-gray-400'"
                    class="flex-1 py-1.5 rounded-lg text-xs font-bold transition-all">
              商户注册
            </button>
            <button @click="loginMode = 'reset'" :class="loginMode === 'reset' ? 'bg-gradient-to-r from-neonCyan/10 to-blue-500/5 text-neonCyan border border-neonCyan/20' : 'text-gray-400'"
                    class="flex-1 py-1.5 rounded-lg text-xs font-bold transition-all">
              找回密码
            </button>
          </div>

          <!-- A. 登录 Form -->
          <form v-if="loginMode === 'login'" @submit.prevent="handleLogin" class="space-y-4 text-left">
            <div class="flex flex-col gap-1.5">
              <label class="text-[10px] text-textMuted uppercase font-bold tracking-wider">商户管理账号</label>
              <input type="text" v-model="loginForm.account" placeholder="请输入您的专属账号 (默认: admin)" required
                     class="w-full text-xs bg-black/40 border border-glassBorder rounded-lg px-3 py-2.5 text-white outline-none focus:border-neonCyan">
            </div>

            <div class="flex flex-col gap-1.5">
              <label class="text-[10px] text-textMuted uppercase font-bold tracking-wider">系统登录密码</label>
              <input type="password" v-model="loginForm.password" placeholder="请输入密码 (默认: admin123)" required
                     class="w-full text-xs bg-black/40 border border-glassBorder rounded-lg px-3 py-2.5 text-white outline-none focus:border-neonCyan">
            </div>

            <button type="submit" :disabled="loading" 
                    class="w-full py-3 bg-gradient-to-r from-neonCyan to-blue-600 rounded-xl font-bold text-xs text-white shadow-cyan-glow hover:scale-105 active:scale-95 transition-all mt-4">
              {{ loading ? '安全验证授权中...' : '商户安全登录' }}
            </button>
          </form>

          <!-- B. 注册 Form -->
          <form v-if="loginMode === 'register'" @submit.prevent="handleRegister" class="space-y-4 text-left">
            <div class="flex flex-col gap-1.5">
              <label class="text-[10px] text-textMuted uppercase font-bold tracking-wider">专属管理账号 (英文)</label>
              <input type="text" v-model="loginForm.account" placeholder="用于直接登录的专属英文账号" required
                     class="w-full text-xs bg-black/40 border border-glassBorder rounded-lg px-3 py-2.5 text-white outline-none focus:border-neonCyan">
            </div>

            <div class="flex flex-col gap-1.5">
              <label class="text-[10px] text-textMuted uppercase font-bold tracking-wider">真实姓名 / 供应商厂名</label>
              <input type="text" v-model="loginForm.displayName" placeholder="例如: 徐水挖机租赁工程队" required
                     class="w-full text-xs bg-black/40 border border-glassBorder rounded-lg px-3 py-2.5 text-white outline-none focus:border-neonCyan">
            </div>

            <div class="flex flex-col gap-1.5">
              <label class="text-[10px] text-textMuted uppercase font-bold tracking-wider">商用邮箱地址</label>
              <input type="email" v-model="loginForm.email" placeholder="输入合规的电子邮箱地址" required
                     class="w-full text-xs bg-black/40 border border-glassBorder rounded-lg px-3 py-2.5 text-white outline-none focus:border-neonCyan">
            </div>

            <div class="flex flex-col gap-1.5">
              <label class="text-[10px] text-textMuted uppercase font-bold tracking-wider">设置管理密码</label>
              <input type="password" v-model="loginForm.password" placeholder="设置您的登录密码" required
                     class="w-full text-xs bg-black/40 border border-glassBorder rounded-lg px-3 py-2.5 text-white outline-none focus:border-neonCyan">
            </div>

            <button type="submit" :disabled="loading" 
                    class="w-full py-3 bg-gradient-to-r from-neonCyan to-blue-600 rounded-xl font-bold text-xs text-white shadow-cyan-glow hover:scale-105 active:scale-95 transition-all mt-4">
              {{ loading ? '提交入驻信息...' : '商户入驻注册' }}
            </button>
          </form>

          <!-- C. 重置密码 Form -->
          <form v-if="loginMode === 'reset'" @submit.prevent="handleResetPassword" class="space-y-4 text-left">
            <div class="flex flex-col gap-1.5">
              <label class="text-[10px] text-textMuted uppercase font-bold tracking-wider">管理账号</label>
              <input type="text" v-model="loginForm.account" placeholder="请输入绑定的系统账号" required
                     class="w-full text-xs bg-black/40 border border-glassBorder rounded-lg px-3 py-2.5 text-white outline-none focus:border-neonCyan">
            </div>

            <div class="flex flex-col gap-1.5">
              <label class="text-[10px] text-textMuted uppercase font-bold tracking-wider">注册邮箱</label>
              <input type="email" v-model="loginForm.email" placeholder="必须与您入驻时填写的邮箱一致" required
                     class="w-full text-xs bg-black/40 border border-glassBorder rounded-lg px-3 py-2.5 text-white outline-none focus:border-neonCyan">
            </div>

            <div class="flex flex-col gap-1.5">
              <label class="text-[10px] text-textMuted uppercase font-bold tracking-wider">新安全登录密码</label>
              <input type="password" v-model="loginForm.newPassword" placeholder="设置新安全登录密码" required
                     class="w-full text-xs bg-black/40 border border-glassBorder rounded-lg px-3 py-2.5 text-white outline-none focus:border-neonCyan">
            </div>

            <button type="submit" :disabled="loading" 
                    class="w-full py-3 bg-gradient-to-r from-neonCyan to-blue-600 rounded-xl font-bold text-xs text-white shadow-cyan-glow hover:scale-105 active:scale-95 transition-all mt-4">
              {{ loading ? '递交安全验证中...' : '提交密码重置' }}
            </button>
          </form>
        </div>
      </div>
    </Transition>

    <!-- ToB 供应商自主入驻模态框 -->
    <Transition name="fade">
      <div v-if="showApplyModal" class="absolute inset-0 bg-black/60 backdrop-blur-md flex items-center justify-center z-50 p-6">
        <div class="w-[500px] bg-panelBg border border-glassBorder rounded-3xl p-8 shadow-glass flex flex-col gap-5 relative animate-scaleUp text-left">
          
          <button @click="showApplyModal = false" class="absolute top-6 right-6 text-gray-400 hover:text-white transition-colors text-lg cursor-pointer">
            <i class="fa-solid fa-times"></i>
          </button>

          <div class="flex items-center gap-3">
            <div class="w-9 h-9 rounded-lg bg-gradient-to-br from-emerald-500 to-teal-600 flex items-center justify-center shadow-emerald-glow">
              <i class="fa-solid fa-hotel text-white"></i>
            </div>
            <div class="flex flex-col text-left">
              <span class="text-[10px] text-emerald-400 uppercase font-bold tracking-wider">双边商业入驻</span>
              <h3 class="text-base font-extrabold text-white">🧱 优质服务商自主入驻申请</h3>
            </div>
          </div>

          <form @submit.prevent="submitApply" class="space-y-4">
            <div class="grid grid-cols-2 gap-4">
              <div class="flex flex-col gap-1.5">
                <label class="text-[10px] text-textMuted uppercase font-bold tracking-wider">企业商户名称</label>
                <input type="text" v-model="applyForm.merchant_name" placeholder="请输入企业名称" required
                       class="w-full text-xs bg-black/40 border border-glassBorder rounded-lg px-3 py-2.5 text-white outline-none focus:border-emerald-500">
              </div>

              <div class="flex flex-col gap-1.5">
                <label class="text-[10px] text-textMuted uppercase font-bold tracking-wider">主营行业分类</label>
                <select v-model="applyForm.business_type" required
                        class="w-full text-xs bg-black/40 border border-glassBorder rounded-lg px-3 py-2.5 text-white outline-none focus:border-emerald-500">
                  <option value="机械租赁" class="bg-panelBg">🚜 机械租赁</option>
                  <option value="五金建材" class="bg-panelBg">🧱 五金建材</option>
                  <option value="民生餐饮" class="bg-panelBg">🍱 民生餐饮</option>
                  <option value="便利超市" class="bg-panelBg">🛒 便利超市</option>
                  <option value="本地劳务" class="bg-panelBg">👷 本地劳务</option>
                </select>
              </div>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div class="flex flex-col gap-1.5">
                <label class="text-[10px] text-textMuted uppercase font-bold tracking-wider">所属供应腹地</label>
                <select v-model="applyForm.source_region" required
                        class="w-full text-xs bg-black/40 border border-glassBorder rounded-lg px-3 py-2.5 text-white outline-none focus:border-emerald-500">
                  <option value="雄安本地" class="bg-panelBg">📍 雄安本地</option>
                  <option value="保定仓" class="bg-panelBg">📦 保定仓</option>
                  <option value="定州基地" class="bg-panelBg">🚛 定州基地</option>
                </select>
              </div>

              <div class="flex flex-col gap-1.5">
                <label class="text-[10px] text-textMuted uppercase font-bold tracking-wider">负责人联系电话</label>
                <input type="text" v-model="applyForm.contact_phone" placeholder="手机或固话" required
                       class="w-full text-xs bg-black/40 border border-glassBorder rounded-lg px-3 py-2.5 text-white outline-none focus:border-emerald-500">
              </div>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div class="flex flex-col gap-1.5">
                <label class="text-[10px] text-textMuted uppercase font-bold tracking-wider">企业联系人</label>
                <input type="text" v-model="applyForm.contact_person" placeholder="张经理 / 李工" required
                       class="w-full text-xs bg-black/40 border border-glassBorder rounded-lg px-3 py-2.5 text-white outline-none focus:border-emerald-500">
              </div>
              <div class="flex flex-col gap-1.5">
                <label class="text-[10px] text-textMuted uppercase font-bold tracking-wider">服务覆盖半径 (km)</label>
                <input type="number" v-model="applyForm.service_radius" placeholder="15" required min="1" max="500"
                       class="w-full text-xs bg-black/40 border border-glassBorder rounded-lg px-3 py-2.5 text-white outline-none focus:border-emerald-500">
              </div>
            </div>

            <div class="flex flex-col gap-1.5">
              <label class="text-[10px] text-textMuted uppercase font-bold tracking-wider">详细经营地址</label>
              <input type="text" v-model="applyForm.address" placeholder="详细物理地址（如：雄安新区容城县核心工业园2号）" required
                     class="w-full text-xs bg-black/40 border border-glassBorder rounded-lg px-3 py-2.5 text-white outline-none focus:border-emerald-500">
            </div>

            <!-- Coordinate Capture Input -->
            <div class="flex flex-col gap-1.5">
              <label class="text-[10px] text-textMuted uppercase font-bold tracking-wider">空间地理位置坐标 (GCJ-02/WGS-84)</label>
              <div class="flex gap-2">
                <input type="text" v-model="applyForm.x" placeholder="经度 (点击地图选点)" readonly required
                       class="w-full text-xs bg-black/40 border border-glassBorder rounded-lg px-3 py-2.5 text-white outline-none">
                <input type="text" v-model="applyForm.y" placeholder="纬度 (点击地图选点)" readonly required
                       class="w-full text-xs bg-black/40 border border-glassBorder rounded-lg px-3 py-2.5 text-white outline-none">
                <button type="button" @click="startPickCoordinate"
                        :class="[isPickingCoordinate ? 'bg-amber-500/25 border-amber-500 text-amber-300' : 'bg-emerald-500/10 border-emerald-500/35 text-emerald-400']"
                        class="px-4 py-2 border rounded-xl hover:bg-emerald-500/20 active:scale-95 transition-all text-xs font-bold shrink-0 cursor-pointer">
                  <i class="fa-solid fa-map-marker-alt mr-1"></i>
                  {{ isPickingCoordinate ? '地图选点中...' : '地图选点' }}
                </button>
              </div>
              <p class="text-[10px] text-textMuted mt-1">💡 提示：点击“地图选点”后，直接在主大图任意位置鼠标左键点击，将精确捕捉该位置的坐标数值。</p>
            </div>

            <button type="submit"
                    class="w-full py-3 bg-gradient-to-r from-emerald-500 to-teal-600 rounded-xl font-bold text-xs text-white shadow-emerald-glow hover:scale-105 active:scale-95 transition-all mt-4 cursor-pointer">
              提交入驻申请 (认证发声)
            </button>
          </form>
        </div>
      </div>
    </Transition>

    <!-- 全民随手拍 Modal -->
    <Transition name="fade">
      <div v-if="showCitizenModal" class="absolute inset-0 bg-black/60 backdrop-blur-md flex items-center justify-center z-50 p-6">
        <div class="bg-glassBg border border-glassBorder rounded-2xl shadow-[0_0_40px_rgba(0,0,0,0.5)] w-full max-w-md overflow-hidden animate-fade-in-up">
          <div class="bg-gradient-to-r from-neonCyan/20 to-transparent p-4 border-b border-glassBorder flex justify-between items-center">
            <h3 class="text-lg font-bold text-white"><i class="fa-solid fa-camera mr-2 text-neonCyan"></i>全民随手拍</h3>
            <button @click="showCitizenModal = false" class="text-gray-400 hover:text-white"><i class="fa-solid fa-xmark"></i></button>
          </div>
          <div class="p-5 space-y-4">
            <div>
              <label class="block text-xs text-gray-400 mb-1">上报类型</label>
              <select v-model="citizenForm.issue_type" class="w-full bg-white/5 border border-glassBorder rounded px-3 py-2 text-sm text-white outline-none focus:border-neonCyan">
                <option value="设施损坏">设施损坏</option>
                <option value="环境脏乱">环境脏乱</option>
                <option value="美丽风景">美丽风景分享</option>
              </select>
            </div>
            <div>
              <label class="block text-xs text-gray-400 mb-1">坐标位置 (已自动捕获)</label>
              <div class="flex gap-2">
                <input type="text" v-model="citizenForm.x" readonly class="w-1/2 bg-white/5 border border-glassBorder rounded px-3 py-2 text-sm text-gray-400 cursor-not-allowed">
                <input type="text" v-model="citizenForm.y" readonly class="w-1/2 bg-white/5 border border-glassBorder rounded px-3 py-2 text-sm text-gray-400 cursor-not-allowed">
              </div>
            </div>
            <div>
              <label class="block text-xs text-gray-400 mb-1">详细描述 *</label>
              <textarea v-model="citizenForm.description" rows="3" class="w-full bg-white/5 border border-glassBorder rounded px-3 py-2 text-sm text-white outline-none focus:border-neonCyan" placeholder="请描述具体情况..."></textarea>
            </div>
            <div>
              <label class="block text-xs text-gray-400 mb-1">上传图片（可选）</label>
              <div @click="document.getElementById('citizenImageInput').click()" class="w-full h-32 border-2 border-dashed border-white/10 rounded-lg flex flex-col items-center justify-center cursor-pointer hover:border-neonCyan/50 transition-colors bg-white/3">
                <template v-if="citizenImagePreview">
                  <img :src="citizenImagePreview" class="w-full h-full object-cover rounded-lg">
                </template>
                <template v-else>
                  <i class="fa-solid fa-camera text-2xl text-gray-500 mb-2"></i>
                  <span class="text-xs text-gray-500">点击上传照片</span>
                </template>
              </div>
              <input id="citizenImageInput" type="file" accept="image/*" @change="onCitizenImageChange" class="hidden">
            </div>
            <button @click="submitCitizenReport" class="w-full bg-neonCyan hover:bg-cyan-400 text-black font-bold py-2.5 rounded shadow-[0_0_15px_rgba(0,229,255,0.4)]">
              提交上报
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 投资意向 Modal -->
    <Transition name="fade">
      <div v-if="showInvestmentIntentModal" class="absolute inset-0 bg-black/60 backdrop-blur-md flex items-center justify-center z-50 p-6">
        <div class="bg-glassBg border border-glassBorder rounded-2xl shadow-[0_0_40px_rgba(0,0,0,0.5)] w-full max-w-md overflow-hidden animate-fade-in-up">
          <div class="bg-gradient-to-r from-neonGold/20 to-transparent p-4 border-b border-glassBorder flex justify-between items-center">
            <h3 class="text-lg font-bold text-white"><i class="fa-solid fa-handshake mr-2 text-neonGold"></i>提交投资意向</h3>
            <button @click="showInvestmentIntentModal = false" class="text-gray-400 hover:text-white"><i class="fa-solid fa-xmark"></i></button>
          </div>
          <div class="p-5 space-y-4">
            <div>
              <label class="block text-xs text-gray-400 mb-1">目标地块</label>
              <input type="text" :value="selectedItem?.parcel_name" readonly class="w-full bg-white/5 border border-glassBorder rounded px-3 py-2 text-sm text-gray-300 cursor-not-allowed">
            </div>
            <div>
              <label class="block text-xs text-gray-400 mb-1">企业名称 *</label>
              <input type="text" v-model="investmentIntentForm.company_name" class="w-full bg-white/5 border border-glassBorder rounded px-3 py-2 text-sm text-white outline-none focus:border-neonGold" placeholder="请输入贵司全称">
            </div>
            <div>
              <label class="block text-xs text-gray-400 mb-1">联系电话 *</label>
              <input type="text" v-model="investmentIntentForm.contact_phone" class="w-full bg-white/5 border border-glassBorder rounded px-3 py-2 text-sm text-white outline-none focus:border-neonGold" placeholder="请输入联系人电话">
            </div>
            <button @click="submitInvestmentIntent" class="w-full bg-neonGold hover:bg-amber-400 text-black font-bold py-2.5 rounded shadow-[0_0_15px_rgba(255,215,0,0.4)]">
              确认提交
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- ToB 物资求购发布模态框 -->
    <Transition name="fade">
      <div v-if="showDemandModal" class="absolute inset-0 bg-black/60 backdrop-blur-md flex items-center justify-center z-50 p-6">
        <div class="w-[450px] bg-panelBg border border-glassBorder rounded-3xl p-8 shadow-glass flex flex-col gap-5 relative animate-scaleUp text-left">
          
          <button @click="showDemandModal = false" class="absolute top-6 right-6 text-gray-400 hover:text-white transition-colors text-lg cursor-pointer">
            <i class="fa-solid fa-times"></i>
          </button>

          <div class="flex items-center gap-3">
            <div class="w-9 h-9 rounded-lg bg-gradient-to-br from-amber-500 to-orange-600 flex items-center justify-center shadow-amber-glow">
              <i class="fa-solid fa-bullhorn text-white"></i>
            </div>
            <div class="flex flex-col text-left">
              <span class="text-[10px] text-amber-400 uppercase font-bold tracking-wider">总包供求配对</span>
              <h3 class="text-base font-extrabold text-white">📢 发布总包物资求购广播</h3>
            </div>
          </div>

          <form @submit.prevent="submitDemand" class="space-y-4">
            <div class="flex flex-col gap-1.5">
              <label class="text-[10px] text-textMuted uppercase font-bold tracking-wider">所属工程项目 (急需采购方)</label>
              <select v-model="demandForm.project_id" required
                      class="w-full text-xs bg-black/40 border border-glassBorder rounded-lg px-3 py-2.5 text-white outline-none focus:border-amber-500">
                <option value="" disabled class="bg-panelBg">请选择求购的重点工程</option>
                <option v-for="p in tobProjects" :key="p.id" :value="p.id" class="bg-panelBg">
                  🏗️ {{ p.name }}
                </option>
              </select>
            </div>

            <div class="flex flex-col gap-1.5">
              <label class="text-[10px] text-textMuted uppercase font-bold tracking-wider">急需采购物资描述</label>
              <textarea v-model="demandForm.material_desc" rows="3" placeholder="例如：二期施工段紧急需要高强度防刺安全鞋300双、国标安全帽200顶，能保定送货或本地速送的商家联系！" required
                        class="w-full text-xs bg-black/40 border border-glassBorder rounded-lg px-3 py-2.5 text-white outline-none focus:border-amber-500 resize-none"></textarea>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div class="flex flex-col gap-1.5">
                <label class="text-[10px] text-textMuted uppercase font-bold tracking-wider">结算支付与账期要求</label>
                <select v-model="demandForm.valid_period" required
                        class="w-full text-xs bg-black/40 border border-glassBorder rounded-lg px-3 py-2.5 text-white outline-none focus:border-amber-500">
                  <option value="货到即付" class="bg-panelBg">💵 货到即付</option>
                  <option value="月结30天" class="bg-panelBg">🗓️ 月结30天</option>
                  <option value="月结60天" class="bg-panelBg">🗓️ 月结60天</option>
                  <option value="工程尾款付清" class="bg-panelBg">🧱 完工后结算</option>
                </select>
              </div>

              <div class="flex flex-col gap-1.5">
                <label class="text-[10px] text-textMuted uppercase font-bold tracking-wider">项目部采购负责人电话</label>
                <input type="text" v-model="demandForm.contact_phone" placeholder="采购经理联系电话" required
                       class="w-full text-xs bg-black/40 border border-glassBorder rounded-lg px-3 py-2.5 text-white outline-none focus:border-amber-500">
              </div>
            </div>

            <button type="submit"
                    class="w-full py-3 bg-gradient-to-r from-amber-500 to-orange-600 rounded-xl font-bold text-xs text-white shadow-amber-glow hover:scale-105 active:scale-95 transition-all mt-4 cursor-pointer">
              广播并发布物资求购
            </button>
          </form>
        </div>
      </div>
    </Transition>

    <!-- 9. 全屏高级数据管理后台模态框 (Full-screen Premium Admin Modal) -->
    <Transition name="fade">
      <div v-if="showAdminModal" class="absolute inset-0 bg-black/85 backdrop-blur-xl flex items-center justify-center z-50 p-8">
        <div class="w-full h-full max-w-7xl bg-glassBg border border-glassBorder rounded-3xl p-8 shadow-glass flex flex-col gap-6 overflow-hidden relative animate-scaleUp">
          
          <!-- 关闭按钮 -->
          <button @click="showAdminModal = false" class="absolute top-6 right-6 text-gray-400 hover:text-white transition-colors text-2xl">
            <i class="fa-solid fa-xmark"></i>
          </button>

          <!-- 头部标题 -->
          <div class="flex items-center gap-3 border-b border-white/10 pb-4">
            <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-neonRed to-rose-600 flex items-center justify-center shadow-red-glow animate-pulse">
              <i class="fa-solid fa-sliders text-white text-lg"></i>
            </div>
            <div class="flex flex-col text-left">
              <span class="text-[10px] text-textMuted uppercase font-bold tracking-widest">时空数据控制枢纽</span>
              <h3 class="text-xl font-extrabold text-transparent bg-clip-text bg-gradient-to-r from-neonRed to-rose-400 font-sans">
                XAGIS HUB - 雄安实空高级数据控制后台
              </h3>
            </div>
          </div>

          <!-- 功能选项 Tab 切换 -->
          <div class="flex gap-2 p-1 bg-black/40 border border-glassBorder rounded-2xl w-fit">
            <button @click="activeAdminTab = 'zdgc'; resetAdminForm()" 
                    :class="activeAdminTab === 'zdgc' ? 'bg-gradient-to-r from-neonRed/20 to-rose-500/10 text-neonRed border border-neonRed/30' : 'text-gray-400 hover:text-white'"
                    class="px-5 py-2.5 rounded-xl text-xs font-bold transition-all flex items-center gap-2">
              <i class="fa-solid fa-tower-observation"></i>
              重点基建工程
            </button>
            <button @click="activeAdminTab = 'merchant'; resetMerchantForm()" 
                    :class="activeAdminTab === 'merchant' ? 'bg-gradient-to-r from-neonRed/20 to-rose-500/10 text-neonRed border border-neonRed/30' : 'text-gray-400 hover:text-white'"
                    class="px-5 py-2.5 rounded-xl text-xs font-bold transition-all flex items-center gap-2">
              <i class="fa-solid fa-truck-ramp-box"></i>
              商用供应链商户
            </button>
            <button @click="activeAdminTab = 'rwjg'; resetAdminForm()" 
                    :class="activeAdminTab === 'rwjg' ? 'bg-gradient-to-r from-neonRed/20 to-rose-500/10 text-neonRed border border-neonRed/30' : 'text-gray-400 hover:text-white'"
                    class="px-5 py-2.5 rounded-xl text-xs font-bold transition-all flex items-center gap-2">
              <i class="fa-solid fa-camera-retro"></i>
              人文景点
            </button>
            <button @click="activeAdminTab = 'spatial-villages'; loadSpatialVillages(); resetSpatialVillageForm()" 
                    :class="activeAdminTab === 'spatial-villages' ? 'bg-gradient-to-r from-neonRed/20 to-rose-500/10 text-neonRed border border-neonRed/30' : 'text-gray-400 hover:text-white'"
                    class="px-5 py-2.5 rounded-xl text-xs font-bold transition-all flex items-center gap-2">
              <i class="fa-solid fa-tree-city"></i>
              时空村落谱
            </button>
            <button @click="activeAdminTab = 'investment-parcels'; fetchInvestmentParcels(); loadInvestmentIntents(); resetInvestmentParcelForm()" 
                    :class="activeAdminTab === 'investment-parcels' ? 'bg-gradient-to-r from-neonRed/20 to-rose-500/10 text-neonRed border border-neonRed/30' : 'text-gray-400 hover:text-white'"
                    class="px-5 py-2.5 rounded-xl text-xs font-bold transition-all flex items-center gap-2">
              <i class="fa-solid fa-handshake"></i>
              招商引资
            </button>
            <button @click="activeAdminTab = 'ai-agent'" 
                    :class="activeAdminTab === 'ai-agent' ? 'bg-gradient-to-r from-neonRed/20 to-rose-500/10 text-neonRed border border-neonRed/30' : 'text-gray-400 hover:text-white'"
                    class="px-5 py-2.5 rounded-xl text-xs font-bold transition-all flex items-center gap-2">
              <i class="fa-solid fa-robot text-neonRed animate-pulse"></i>
              AI 抓取与空间解析
            </button>
            <button @click="activeAdminTab = 'citizen-reports'; loadCitizenReports()" 
                    :class="activeAdminTab === 'citizen-reports' ? 'bg-gradient-to-r from-neonRed/20 to-rose-500/10 text-neonRed border border-neonRed/30' : 'text-gray-400 hover:text-white'"
                    class="px-5 py-2.5 rounded-xl text-xs font-bold transition-all flex items-center gap-2">
              <i class="fa-solid fa-bullhorn"></i>
              市民城管随手拍
            </button>
          </div>

          <!-- 双栏布局：左侧表单编辑器，右侧现有记录一览 -->
          <div class="flex-1 flex gap-6 overflow-hidden">
            
            <!-- 左栏：新增/编辑表单 (Glass Card) -->
            <div class="w-2/5 bg-white/2 border border-glassBorder rounded-2xl p-6 overflow-y-auto flex flex-col gap-4">
              <div class="flex justify-between items-center border-b border-white/5 pb-2">
                <h4 class="text-sm font-extrabold text-neonRed flex items-center gap-2">
                  <i class="fa-solid fa-pen-to-square"></i> 数据点属性编辑器
                </h4>
                <span class="text-[9px] px-2 py-0.5 rounded bg-neonRed/10 border border-neonRed/25 text-neonRed font-bold">
                  {{ (activeAdminTab === 'zdgc' || activeAdminTab === 'rwjg') && adminForm.id ? '编辑模式' : (activeAdminTab === 'merchant' && merchantForm.id ? '编辑模式' : (activeAdminTab === 'spatial-villages' && spatialVillageForm.id ? '编辑模式' : (activeAdminTab === 'investment-parcels' && investmentParcelForm.id ? '编辑模式' : (activeAdminTab === 'ai-agent' || activeAdminTab === 'citizen-reports' ? '监控模式' : '新增模式')))) }}
                </span>
              </div>

              <!-- 时空老村庄表单 -->
              <div v-if="activeAdminTab === 'spatial-villages'" class="space-y-4 text-left">
                <div class="flex flex-col gap-1.5">
                  <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">村落 ID (如 v_dawang)</label>
                  <input type="text" v-model="spatialVillageForm.id" placeholder="如: v_dawang" :disabled="!!spatialVillageForm.id"
                         class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed disabled:opacity-50">
                </div>
                <div class="flex flex-col gap-1.5">
                  <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">村落名称</label>
                  <input type="text" v-model="spatialVillageForm.village_name" placeholder="请输入村庄名称"
                         class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed">
                </div>
                <div class="grid grid-cols-2 gap-4">
                  <div class="flex flex-col gap-1.5">
                    <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">村落状态</label>
                    <select v-model="spatialVillageForm.status"
                            class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed">
                      <option :value="0" class="bg-darkBg">已拆迁腾退</option>
                      <option :value="1" class="bg-darkBg">大规模拆迁中</option>
                      <option :value="2" class="bg-darkBg">特色保留村落</option>
                    </select>
                  </div>
                  <div class="flex flex-col gap-1.5">
                    <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">预计/实际变迁年份</label>
                    <input type="number" v-model="spatialVillageForm.plan_demolition_year" placeholder="如: 2018"
                           class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed">
                  </div>
                </div>
                <div class="flex flex-col gap-1.5">
                  <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">空间边界 (GeoJSON Polygon Coordinates 数组)</label>
                  <textarea v-model="spatialVillageForm.geom_json" rows="3" placeholder="[[[lng, lat], [lng, lat], ...]]]"
                            class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed font-mono"></textarea>
                </div>
                <div class="flex flex-col gap-1.5">
                  <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">村落历史沿革简介</label>
                  <textarea v-model="spatialVillageForm.history" rows="4" placeholder="请输入村落的历史沿革介绍..."
                            class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed resize-none"></textarea>
                </div>
                <div class="flex gap-3 pt-2">
                  <button @click="resetSpatialVillageForm" class="flex-1 py-3 bg-white/5 border border-glassBorder hover:bg-white/10 rounded-xl text-xs font-bold transition-all">
                    重置
                  </button>
                  <button @click="saveSpatialVillageRecord" :disabled="loading" 
                          class="flex-1 py-3 bg-gradient-to-r from-neonRed to-rose-600 border border-neonRed/20 hover:scale-[1.02] active:scale-98 text-white rounded-xl text-xs font-extrabold shadow-red-glow transition-all">
                    {{ loading ? '存盘中...' : '确认保存村落' }}
                  </button>
                </div>
              </div>

              <!-- 招商引资地块表单 -->
              <div v-if="activeAdminTab === 'investment-parcels'" class="space-y-4 text-left">
                <div class="flex p-0.5 bg-black/40 border border-white/5 rounded-xl mb-2">
                  <button @click="activeInvestmentSubTab = 'parcels'"
                          :class="activeInvestmentSubTab === 'parcels' ? 'bg-white/10 text-neonRed font-extrabold' : 'text-gray-400 hover:text-white'"
                          class="flex-1 py-2 rounded-lg text-xs transition-all">
                    地块管理
                  </button>
                  <button @click="activeInvestmentSubTab = 'intents'"
                          :class="activeInvestmentSubTab === 'intents' ? 'bg-white/10 text-neonRed font-extrabold' : 'text-gray-400 hover:text-white'"
                          class="flex-1 py-2 rounded-lg text-xs transition-all">
                    意向申请 ({{ investmentIntents.length }})
                  </button>
                </div>

                <div v-if="activeInvestmentSubTab === 'parcels'" class="space-y-4">
                  <div class="flex flex-col gap-1.5">
                    <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">地块 ID (如 parcel_001)</label>
                    <input type="text" v-model="investmentParcelForm.id" placeholder="如: parcel_001" :disabled="!!investmentParcelForm.id"
                           class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed disabled:opacity-50">
                  </div>
                  <div class="flex flex-col gap-1.5">
                    <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">地块名称</label>
                    <input type="text" v-model="investmentParcelForm.parcel_name" placeholder="请输入地块名称"
                           class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed">
                  </div>
                  <div class="grid grid-cols-2 gap-4">
                    <div class="flex flex-col gap-1.5">
                      <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">地块状态</label>
                      <select v-model="investmentParcelForm.status"
                              class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed">
                        <option :value="0" class="bg-darkBg">待出让</option>
                        <option :value="1" class="bg-darkBg">洽谈中</option>
                        <option :value="2" class="bg-darkBg">已出让</option>
                      </select>
                    </div>
                    <div class="flex flex-col gap-1.5">
                      <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">规划用途</label>
                      <input type="text" v-model="investmentParcelForm.land_use" placeholder="如: 商业用地"
                             class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed">
                    </div>
                  </div>
                  <div class="grid grid-cols-2 gap-4">
                    <div class="flex flex-col gap-1.5">
                      <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">占地面积 (㎡)</label>
                      <input type="number" step="any" v-model="investmentParcelForm.area_sqm" placeholder="面积"
                             class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed">
                    </div>
                    <div class="flex flex-col gap-1.5">
                      <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">规划容积率</label>
                      <input type="number" step="any" v-model="investmentParcelForm.far" placeholder="容积率"
                             class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed">
                    </div>
                  </div>
                  <div class="flex flex-col gap-1.5">
                    <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">空间边界 (GeoJSON Polygon Coordinates 数组)</label>
                    <textarea v-model="investmentParcelForm.geom_json" rows="3" placeholder="[[[lng, lat], [lng, lat], ...]]]"
                              class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed font-mono"></textarea>
                  </div>
                  <div class="flex gap-3 pt-2">
                    <button @click="resetInvestmentParcelForm" class="flex-1 py-3 bg-white/5 border border-glassBorder hover:bg-white/10 rounded-xl text-xs font-bold transition-all">
                      重置
                    </button>
                    <button @click="saveInvestmentParcelRecord" :disabled="loading" 
                            class="flex-1 py-3 bg-gradient-to-r from-neonRed to-rose-600 border border-neonRed/20 hover:scale-[1.02] active:scale-98 text-white rounded-xl text-xs font-extrabold shadow-red-glow transition-all">
                      {{ loading ? '存盘中...' : '确认保存地块' }}
                    </button>
                  </div>
                </div>
                <div v-else class="p-6 bg-black/30 border border-glassBorder rounded-2xl text-center flex flex-col items-center justify-center py-12 gap-2 text-textMuted">
                  <i class="fa-solid fa-handshake text-3xl mb-2 text-rose-500"></i>
                  <span class="text-xs">请在右侧列表切换到“意向申请审核”子页签以管理与查看所有提交的企业投资意向。</span>
                </div>
              </div>

              <!-- A. 重点工程 / 人文老村 表单 -->
              <div v-if="activeAdminTab === 'zdgc' || activeAdminTab === 'rwjg'" class="space-y-4 text-left">
                <div class="flex flex-col gap-1.5">
                  <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">ID (主键)</label>
                  <input type="text" v-model="adminForm.id" placeholder="如: 10" 
                         :disabled="!!adminForm.id"
                         class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed disabled:opacity-50">
                </div>

                <div class="flex flex-col gap-1.5">
                  <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">项目/景点名称</label>
                  <input type="text" v-model="adminForm.name" placeholder="请输入名称" 
                         class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed">
                </div>

                <div class="grid grid-cols-2 gap-4">
                  <div class="flex flex-col gap-1.5">
                    <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">经度 X</label>
                    <input type="text" v-model="adminForm.x" placeholder="如: 116.1274" 
                           class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed">
                  </div>
                  <div class="flex flex-col gap-1.5">
                    <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">纬度 Y</label>
                    <input type="text" v-model="adminForm.y" placeholder="如: 38.9851" 
                           class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed">
                  </div>
                </div>

                <div v-if="activeAdminTab === 'zdgc'" class="flex flex-col gap-1.5">
                  <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">默认缩放 (Zoom)</label>
                  <input type="number" v-model="adminForm.zoom" placeholder="如: 12" 
                         class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed">
                </div>

                <div class="flex flex-col gap-1.5">
                  <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">图片路径 (URL)</label>
                  <input type="text" v-model="adminForm.src" placeholder="如: /images/zdgc/1.jpg" 
                         class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed">
                </div>

                <div class="flex flex-col gap-1.5">
                  <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">详细属性/历史沿革</label>
                  <textarea v-model="adminForm.jieshao" rows="5" placeholder="请输入详细的地理空间实体介绍..." 
                            class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed resize-none"></textarea>
                </div>

                <div class="flex gap-3 pt-2">
                  <button @click="resetAdminForm" class="flex-1 py-3 bg-white/5 border border-glassBorder hover:bg-white/10 rounded-xl text-xs font-bold transition-all">
                    清空重置
                  </button>
                  <button @click="editType = activeAdminTab; saveAdminRecord()" :disabled="loading" 
                          class="flex-1 py-3 bg-gradient-to-r from-neonRed to-rose-600 border border-neonRed/20 hover:scale-[1.02] active:scale-98 text-white rounded-xl text-xs font-extrabold shadow-red-glow transition-all">
                    {{ loading ? '存盘提交中...' : '保存记录至 H2' }}
                  </button>
                </div>
              </div>

              <!-- B. 供应链商户 表单 -->
              <div v-if="activeAdminTab === 'merchant'" class="space-y-4 text-left">
                <div class="flex flex-col gap-1.5">
                  <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">商户ID (主键)</label>
                  <input type="text" v-model="merchantForm.id" placeholder="如: merchant_1" 
                         :disabled="!!merchantForm.id"
                         class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed disabled:opacity-50">
                </div>

                <div class="flex flex-col gap-1.5">
                  <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">供应商厂名</label>
                  <input type="text" v-model="merchantForm.merchantName" placeholder="如: 雄安鑫旺建材制造厂" 
                         class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed">
                </div>

                <div class="grid grid-cols-2 gap-4">
                  <div class="flex flex-col gap-1.5">
                    <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">主营业务类型</label>
                    <select v-model="merchantForm.businessType" 
                            class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed cursor-pointer">
                      <option value="机械租赁">🏗️ 机械租赁</option>
                      <option value="劳务分包">👷 劳务分包</option>
                      <option value="五金管件">🔩 五金管件</option>
                      <option value="水泥钢筋">🧱 水泥钢筋</option>
                      <option value="周转材料">🛠️ 周转材料</option>
                      <option value="绿化种植">🌿 绿化种植</option>
                      <option value="物流配送">🚚 物流配送</option>
                    </select>
                  </div>
                  <div class="flex flex-col gap-1.5">
                    <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">服务半径 (km)</label>
                    <input type="number" v-model="merchantForm.serviceRadius" placeholder="如: 25" 
                           class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed">
                  </div>
                </div>

                <div class="grid grid-cols-2 gap-4">
                  <div class="flex flex-col gap-1.5">
                    <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">经度 X</label>
                    <input type="text" v-model="merchantForm.x" placeholder="如: 116.1154" 
                           class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed">
                  </div>
                  <div class="flex flex-col gap-1.5">
                    <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">纬度 Y</label>
                    <input type="text" v-model="merchantForm.y" placeholder="如: 38.9741" 
                           class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed">
                  </div>
                </div>

                <div class="grid grid-cols-2 gap-4">
                  <div class="flex flex-col gap-1.5">
                    <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">联系人</label>
                    <input type="text" v-model="merchantForm.contactPerson" placeholder="王经理" 
                           class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed">
                  </div>
                  <div class="flex flex-col gap-1.5">
                    <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">联系电话</label>
                    <input type="text" v-model="merchantForm.contactPhone" placeholder="13900000000" 
                           class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed">
                  </div>
                </div>

                <div class="flex flex-col gap-1.5">
                  <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">商户物理地址</label>
                  <input type="text" v-model="merchantForm.address" placeholder="如: 容城县核心工业园区12号" 
                         class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed">
                </div>

                <div class="flex flex-col gap-1.5">
                  <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">商户简介与供应链能力描述</label>
                  <textarea v-model="merchantForm.description" rows="3" placeholder="描述您的建筑原材料供给或设备出租优势..." 
                            class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed resize-none"></textarea>
                </div>

                <!-- Toggle Controls (VIP & Verify status) -->
                <div class="grid grid-cols-2 gap-4 p-3 bg-black/30 border border-glassBorder rounded-xl">
                  <div class="flex items-center justify-between">
                    <span class="text-xs font-bold text-white">VIP 商家资格</span>
                    <label class="relative inline-flex items-center cursor-pointer">
                      <input type="checkbox" v-model="merchantForm.isVip" class="sr-only peer">
                      <div class="w-9 h-5 bg-gray-800 rounded-full peer peer-checked:after:translate-x-full peer-checked:bg-amber-500 after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-gray-400 after:rounded-full after:h-4 after:w-4 after:transition-all animate-all"></div>
                    </label>
                  </div>
                  <div class="flex items-center justify-between">
                    <span class="text-xs font-bold text-white">审核认证资质</span>
                    <label class="relative inline-flex items-center cursor-pointer">
                      <input type="checkbox" v-model="merchantForm.verified" class="sr-only peer">
                      <div class="w-9 h-5 bg-gray-800 rounded-full peer peer-checked:after:translate-x-full peer-checked:bg-emerald-500 after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-gray-400 after:rounded-full after:h-4 after:w-4 after:transition-all animate-all"></div>
                    </label>
                  </div>
                </div>

                <div class="flex gap-3 pt-2">
                  <button @click="resetMerchantForm" class="flex-1 py-3 bg-white/5 border border-glassBorder hover:bg-white/10 rounded-xl text-xs font-bold transition-all">
                    清空重置
                  </button>
                  <button @click="saveMerchantRecord" :disabled="loading" 
                          class="flex-1 py-3 bg-gradient-to-r from-neonRed to-rose-600 border border-neonRed/20 hover:scale-[1.02] active:scale-98 text-white rounded-xl text-xs font-extrabold shadow-red-glow transition-all">
                    {{ loading ? '存盘提交中...' : '保存商户记录' }}
                  </button>
                </div>
              </div>

              <!-- C. 在线地理编码 (Geocoding) 沙盒 -->
              <div v-if="activeAdminTab === 'ai-agent'" class="space-y-4 text-left">
                <div class="p-4 bg-white/5 border border-glassBorder rounded-xl text-xs leading-relaxed text-gray-400">
                  <span class="text-white font-extrabold flex items-center gap-1.5 mb-1.5">
                    <i class="fa-solid fa-circle-info text-neonRed animate-pulse"></i> 什么是 Geocoding 地理编码？
                  </span>
                  AI 智能体抓取新闻后，只有文本地址（如“容西组团”），无法直接在地图打点。本模块调用 Geocoding API 进行空间解析，将中文地址转化为经纬度，并加入合规偏差。
                </div>

                <div class="flex flex-col gap-1.5 mt-2">
                  <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">要解析的文本地址</label>
                  <input type="text" v-model="customAddressToGeocode" placeholder="例如: 雄安新区雄安市民服务中心" 
                         class="w-full text-xs bg-black/40 border border-glassBorder rounded-xl px-4 py-2.5 text-white outline-none focus:border-neonRed">
                </div>

                <button @click="geocodeCustomAddress"
                        class="w-full py-3 bg-white/5 border border-glassBorder hover:bg-white/10 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-1.5">
                  <i class="fa-solid fa-magnifying-glass-location text-neonRed"></i>
                  唤醒地名解析 Agent
                </button>

                <!-- 解析结果面板 -->
                <div v-if="geocodedResult" class="p-4 bg-black/50 border border-glassBorder rounded-xl space-y-3">
                  <div class="text-xs font-bold text-white border-b border-white/5 pb-1">Geocoding 运行报告</div>
                  <div class="grid grid-cols-2 gap-y-2 text-xs">
                    <div class="text-textMuted">输入地名：</div>
                    <div class="text-white font-semibold truncate">{{ geocodedResult.address }}</div>
                    <div class="text-textMuted">解析经度 X：</div>
                    <div class="text-neonCyan font-mono font-semibold">{{ geocodedResult.longitude }}</div>
                    <div class="text-textMuted">解析纬度 Y：</div>
                    <div class="text-neonCyan font-mono font-semibold">{{ geocodedResult.latitude }}</div>
                    <div class="text-textMuted">坐标系：</div>
                    <div class="text-emerald-400 font-semibold">{{ geocodedResult.coordinate_system }}</div>
                    <div class="text-textMuted">安全脱敏：</div>
                    <div class="text-amber-400 font-semibold">已应用 (随机偏置)</div>
                  </div>
                  <button @click="fillGeocodedCoordsToForm"
                          class="w-full py-2.5 bg-gradient-to-r from-neonRed/20 to-rose-600/20 border border-neonRed/30 hover:scale-[1.02] active:scale-98 text-neonRed rounded-xl text-xs font-extrabold transition-all flex items-center justify-center gap-1">
                    <i class="fa-solid fa-copy"></i> 将此坐标填充至新增表单
                  </button>
                </div>
              </div>

              <!-- D. 市民城管工单详情 -->
              <div v-if="activeAdminTab === 'citizen-reports'" class="space-y-4 text-left">
                <div v-if="selectedCitizenReport" class="space-y-4">
                  <div class="p-4 bg-black/40 border border-glassBorder rounded-xl space-y-3">
                    <div class="text-xs font-bold text-white border-b border-white/5 pb-1.5 flex justify-between items-center">
                      <span>工单详情 [ID: {{ selectedCitizenReport.id }}]</span>
                      <span :class="selectedCitizenReport.status === 0 ? 'text-red-400 bg-red-400/10 border-red-400/20' : (selectedCitizenReport.status === 1 ? 'text-amber-400 bg-amber-400/10 border-amber-400/20' : 'text-emerald-400 bg-emerald-400/10 border-emerald-400/20')"
                            class="px-2 py-0.5 rounded border text-[9px] font-extrabold">
                        {{ selectedCitizenReport.status === 0 ? '待处理' : (selectedCitizenReport.status === 1 ? '处理中' : '已解决') }}
                      </span>
                    </div>

                    <div class="text-xs space-y-2 text-gray-300">
                      <div><strong class="text-textMuted">问题类型:</strong> {{ selectedCitizenReport.issue_type }}</div>
                      <div><strong class="text-textMuted">事件描述:</strong> {{ selectedCitizenReport.description }}</div>
                      <div><strong class="text-textMuted">上报市民:</strong> {{ selectedCitizenReport.reporter_name }}</div>
                      <div><strong class="text-textMuted">联系电话:</strong> {{ selectedCitizenReport.contact_phone || '无' }}</div>
                      <div><strong class="text-textMuted">上报坐标:</strong> {{ selectedCitizenReport.x }}, {{ selectedCitizenReport.y }}</div>
                      <div><strong class="text-textMuted">上报时间:</strong> {{ selectedCitizenReport.created_at.substring(0, 16).replace('T', ' ') }}</div>
                    </div>
                  </div>

                  <!-- 工单处置操作 -->
                  <div class="space-y-2">
                    <label class="text-[10px] text-textMuted uppercase font-extrabold tracking-wider">工单流转处置</label>
                    <div class="grid grid-cols-3 gap-2">
                      <button @click="updateReportStatus(selectedCitizenReport.id, 0); selectedCitizenReport.status = 0"
                              :class="selectedCitizenReport.status === 0 ? 'bg-red-500/20 text-red-400 border-red-500/40' : 'bg-white/3 text-gray-400 border-white/5'"
                              class="py-2.5 border rounded-xl text-xs font-bold transition-all">
                        待处理
                      </button>
                      <button @click="updateReportStatus(selectedCitizenReport.id, 1); selectedCitizenReport.status = 1"
                              :class="selectedCitizenReport.status === 1 ? 'bg-amber-500/20 text-amber-400 border-amber-500/40' : 'bg-white/3 text-gray-400 border-white/5'"
                              class="py-2.5 border rounded-xl text-xs font-bold transition-all">
                        处理中
                      </button>
                      <button @click="updateReportStatus(selectedCitizenReport.id, 2); selectedCitizenReport.status = 2"
                              :class="selectedCitizenReport.status === 2 ? 'bg-emerald-500/20 text-emerald-400 border-emerald-500/40' : 'bg-white/3 text-gray-400 border-white/5'"
                              class="py-2.5 border rounded-xl text-xs font-bold transition-all">
                        已解决
                      </button>
                    </div>
                  </div>

                  <button @click="locateReportOnMap(selectedCitizenReport)"
                          class="w-full py-3 bg-gradient-to-r from-neonRed to-rose-600 hover:scale-[1.02] active:scale-98 text-white rounded-xl text-xs font-extrabold shadow-red-glow transition-all flex items-center justify-center gap-1.5">
                    <i class="fa-solid fa-map-location-dot"></i>
                    在 WebGIS 地图上平滑飞行定位
                  </button>
                </div>
                <div v-else class="py-12 text-center text-xs text-textMuted border border-dashed border-glassBorder rounded-xl">
                  请在右侧列表中选择一条市民反馈工单进行处理与定位。
                </div>
              </div>

            </div>

            <!-- 右栏：现有实体记录列表 (Glass Card) -->
            <div class="flex-1 bg-white/2 border border-glassBorder rounded-2xl p-6 overflow-hidden flex flex-col gap-4">
              <div class="flex justify-between items-center border-b border-white/5 pb-2">
                <h4 class="text-sm font-extrabold text-white flex items-center gap-2">
                  <i class="fa-solid fa-list-ul"></i> 
                  {{ activeAdminTab === 'ai-agent' ? 'AI 抓取待审核线索' : (activeAdminTab === 'citizen-reports' ? '市民城管反馈工单' : (activeAdminTab === 'spatial-villages' ? '时空老村落列表' : (activeAdminTab === 'investment-parcels' ? (activeInvestmentSubTab === 'parcels' ? '招商引资地块库' : '商机投资意向申请') : '数据库现有记录一览'))) }} 
                  ({{ activeAdminTab === 'zdgc' ? projects.length : (activeAdminTab === 'merchant' ? merchants.length : (activeAdminTab === 'rwjg' ? villages.value.length : (activeAdminTab === 'ai-agent' ? aiCrawlResults.length : (activeAdminTab === 'citizen-reports' ? citizenReports.length : (activeAdminTab === 'spatial-villages' ? spatialVillages.length : (activeInvestmentSubTab === 'parcels' ? investmentParcels.length : investmentIntents.length)))))) }})
                </h4>
                <span class="text-xs text-textMuted">
                  {{ activeAdminTab === 'ai-agent' ? '审查并一键发布 AI 抓取出的工地情报' : (activeAdminTab === 'citizen-reports' ? '查看市民上报详情并在地图上交互定位' : (activeAdminTab === 'spatial-villages' ? '物理删除、在地图上定位或重新编辑老村落面要素' : (activeAdminTab === 'investment-parcels' ? '地块数据更新及意向投资申请列表管理' : '点击操作对数据进行实时持久化更新'))) }}
                </span>
              </div>

              <!-- 数据列表滚动容器 -->
              <div class="flex-1 overflow-y-auto space-y-3 pr-2 border-none">
                
                <!-- 时空老村落列表 -->
                <div v-if="activeAdminTab === 'spatial-villages'" class="space-y-2">
                  <div v-if="spatialVillages.length === 0" class="py-12 text-center text-xs text-textMuted border border-dashed border-glassBorder rounded-xl">
                    暂无村落空间数据
                  </div>
                  <div v-else v-for="v in spatialVillages" :key="v.properties.id" 
                       class="flex items-center justify-between p-4 rounded-xl bg-black/40 border border-glassBorder hover:bg-white/5 transition-all text-xs text-gray-300 gap-4">
                    <div class="flex items-center gap-3 min-w-0 text-left">
                      <i class="fa-solid fa-tree-city text-emerald-400 text-sm flex-shrink-0 w-8 h-8 rounded-lg border border-glassBorder flex items-center justify-center bg-emerald-500/10"></i>
                      <div class="flex flex-col min-w-0">
                        <span class="font-bold text-white truncate text-sm">{{ v.properties.village_name }}</span>
                        <span class="text-[10px] text-textMuted mt-1">ID: {{ v.properties.id }} | 变迁年份: {{ v.properties.plan_demolition_year || '保留' }} | 状态: {{ v.properties.status === 0 ? '已拆迁' : (v.properties.status === 1 ? '拆迁中' : '保留') }}</span>
                      </div>
                    </div>
                    <div class="flex gap-2 flex-shrink-0">
                      <button @click="editSpatialVillage(v)" class="px-3 py-1.5 bg-blue-500/10 hover:bg-blue-500/25 active:scale-95 text-blue-400 border border-blue-500/35 rounded-lg text-xs font-bold transition-all">
                        编辑
                      </button>
                      <button @click="locateVillageOnMap(v)" class="px-3 py-1.5 bg-emerald-500/10 hover:bg-emerald-500/25 active:scale-95 text-emerald-400 border border-emerald-500/35 rounded-lg text-xs font-bold transition-all">
                        定位
                      </button>
                      <button @click="deleteSpatialVillageRecord(v.properties.id)" class="px-3 py-1.5 bg-rose-500/10 hover:bg-rose-500/25 active:scale-95 text-rose-400 border border-rose-500/35 rounded-lg text-xs font-bold transition-all">
                        删除
                      </button>
                    </div>
                  </div>
                </div>

                <!-- 招商引资列表 -->
                <div v-if="activeAdminTab === 'investment-parcels'" class="space-y-2">
                  <!-- 地块库列表 -->
                  <div v-if="activeInvestmentSubTab === 'parcels'" class="space-y-2">
                    <div v-if="investmentParcels.length === 0" class="py-12 text-center text-xs text-textMuted border border-dashed border-glassBorder rounded-xl">
                      暂无招商引资地块数据
                    </div>
                    <div v-else v-for="p in investmentParcels" :key="p.properties.id" 
                         class="flex items-center justify-between p-4 rounded-xl bg-black/40 border border-glassBorder hover:bg-white/5 transition-all text-xs text-gray-300 gap-4">
                      <div class="flex items-center gap-3 min-w-0 text-left">
                        <i class="fa-solid fa-handshake text-amber-400 text-sm flex-shrink-0 w-8 h-8 rounded-lg border border-glassBorder flex items-center justify-center bg-amber-500/10"></i>
                        <div class="flex flex-col min-w-0">
                          <span class="font-bold text-white truncate text-sm">{{ p.properties.parcel_name }}</span>
                          <span class="text-[10px] text-textMuted mt-1">ID: {{ p.properties.id }} | 用途: {{ p.properties.land_use }} | 面积: {{ p.properties.area_sqm }}㎡ | 状态: {{ p.properties.status === 0 ? '待出让' : (p.properties.status === 1 ? '洽谈中' : '已出让') }}</span>
                        </div>
                      </div>
                      <div class="flex gap-2 flex-shrink-0">
                        <button @click="editInvestmentParcel(p)" class="px-3 py-1.5 bg-blue-500/10 hover:bg-blue-500/25 active:scale-95 text-blue-400 border border-blue-500/35 rounded-lg text-xs font-bold transition-all">
                          编辑
                        </button>
                        <button @click="locateParcelOnMap(p)" class="px-3 py-1.5 bg-amber-500/10 hover:bg-amber-500/25 active:scale-95 text-amber-400 border border-amber-500/35 rounded-lg text-xs font-bold transition-all">
                          定位
                        </button>
                        <button @click="deleteInvestmentParcelRecord(p.properties.id)" class="px-3 py-1.5 bg-rose-500/10 hover:bg-rose-500/25 active:scale-95 text-rose-400 border border-rose-500/35 rounded-lg text-xs font-bold transition-all">
                          删除
                        </button>
                      </div>
                    </div>
                  </div>

                  <!-- 意向申请列表 -->
                  <div v-else class="space-y-2">
                    <div v-if="investmentIntents.length === 0" class="py-12 text-center text-xs text-textMuted border border-dashed border-glassBorder rounded-xl">
                      暂无意向申请记录
                    </div>
                    <div v-else v-for="intent in investmentIntents" :key="intent.id" 
                         class="p-4 rounded-xl border border-glassBorder bg-black/40 hover:bg-white/5 transition-all text-left space-y-2 text-xs">
                      <div class="flex justify-between items-start gap-4">
                        <div class="min-w-0">
                          <h5 class="font-extrabold text-white text-sm truncate">{{ intent.company_name }}</h5>
                          <p class="text-[10px] text-textMuted mt-0.5">意向地块: <span class="text-neonRed font-bold">{{ intent.parcel_name }}</span></p>
                        </div>
                        <button @click="deleteInvestmentIntentRecord(intent.id)" class="px-2.5 py-1 text-[10px] font-bold bg-rose-500/10 hover:bg-rose-500/25 text-rose-400 border border-rose-500/35 rounded-lg transition-all flex-shrink-0">
                          删除意向
                        </button>
                      </div>
                      <p class="text-gray-300 text-xs leading-relaxed bg-black/20 p-2.5 rounded-lg border border-white/5 whitespace-pre-wrap">
                        {{ intent.intent_desc }}
                      </p>
                      <div class="flex justify-between items-center text-[10px] text-textMuted pt-1">
                        <span>联系人: {{ intent.contact_person }} | 电话: {{ intent.contact_phone }}</span>
                        <span>时间: {{ new Date(intent.created_at).toLocaleString() }}</span>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- A. 重点工程列表 -->
                <div v-if="activeAdminTab === 'zdgc'" class="space-y-2">
                  <div v-for="item in projects" :key="item.id" 
                       class="flex items-center justify-between p-4 rounded-xl bg-black/40 border border-glassBorder hover:bg-white/5 transition-all text-xs text-gray-300 gap-4">
                    <div class="flex items-center gap-3 min-w-0">
                      <img :src="item.src || '/images/zdgc/10.jpg'" class="w-10 h-10 object-cover rounded-lg border border-glassBorder flex-shrink-0">
                      <div class="flex flex-col text-left min-w-0">
                        <span class="font-bold text-white truncate text-sm">{{ item.name }}</span>
                        <span class="text-[10px] text-textMuted mt-1">ID: {{ item.id }} | 经纬度: {{ item.x }}, {{ item.y }}</span>
                      </div>
                    </div>
                    <div class="flex gap-2 flex-shrink-0">
                      <button @click="fillAdminForm(item, 'zdgc')" class="px-3 py-1.5 bg-blue-500/10 hover:bg-blue-500/25 active:scale-95 text-blue-400 border border-blue-500/35 rounded-lg text-xs font-bold transition-all">
                        <i class="fa-solid fa-edit mr-1"></i>编辑
                      </button>
                      <button @click="deleteAdminRecord('zdgc', item.id)" class="px-3 py-1.5 bg-rose-500/10 hover:bg-rose-500/25 active:scale-95 text-rose-400 border border-rose-500/35 rounded-lg text-xs font-bold transition-all">
                        <i class="fa-solid fa-trash-can mr-1"></i>删除
                      </button>
                    </div>
                  </div>
                </div>

                <!-- B. 人文景点老村列表 -->
                <div v-if="activeAdminTab === 'rwjg'" class="space-y-2">
                  <div v-for="item in villages" :key="item.id" 
                       class="flex items-center justify-between p-4 rounded-xl bg-black/40 border border-glassBorder hover:bg-white/5 transition-all text-xs text-gray-300 gap-4">
                    <div class="flex items-center gap-3 min-w-0">
                      <img :src="item.src || '/images/rwjg/1.jpg'" class="w-10 h-10 object-cover rounded-lg border border-glassBorder flex-shrink-0">
                      <div class="flex flex-col text-left min-w-0">
                        <span class="font-bold text-white truncate text-sm">{{ item.name }}</span>
                        <span class="text-[10px] text-textMuted mt-1">ID: {{ item.id }} | 经纬度: {{ item.x }}, {{ item.y }}</span>
                      </div>
                    </div>
                    <div class="flex gap-2 flex-shrink-0">
                      <button @click="fillAdminForm(item, 'rwjg')" class="px-3 py-1.5 bg-blue-500/10 hover:bg-blue-500/25 active:scale-95 text-blue-400 border border-blue-500/35 rounded-lg text-xs font-bold transition-all">
                        <i class="fa-solid fa-edit mr-1"></i>编辑
                      </button>
                      <button @click="deleteAdminRecord('rwjg', item.id)" class="px-3 py-1.5 bg-rose-500/10 hover:bg-rose-500/25 active:scale-95 text-rose-400 border border-rose-500/35 rounded-lg text-xs font-bold transition-all">
                        <i class="fa-solid fa-trash-can mr-1"></i>删除
                      </button>
                    </div>
                  </div>
                </div>

                <!-- C. 供应链商户列表 -->
                <div v-if="activeAdminTab === 'merchant'" class="space-y-2">
                  <div v-for="item in merchants" :key="item.id" 
                       class="flex items-center justify-between p-4 rounded-xl bg-black/40 border border-glassBorder hover:bg-white/5 transition-all text-xs text-gray-300 gap-4">
                    <div class="flex items-center gap-3 min-w-0">
                      <img :src="item.logoUrl || '/images/merchants/m1.png'" class="w-10 h-10 object-cover rounded-lg border border-glassBorder flex-shrink-0">
                      <div class="flex flex-col text-left min-w-0">
                        <div class="flex items-center gap-2">
                          <span class="font-bold text-white truncate text-sm">{{ item.merchantName }}</span>
                          <span v-if="item.isVip" class="text-[9px] px-1.5 py-0.5 rounded bg-amber-500/15 border border-amber-500/30 text-amber-400 font-extrabold flex items-center gap-0.5 shadow-gold-glow animate-pulse">
                            <i class="fa-solid fa-gem text-[8px]"></i>VIP
                          </span>
                          <span v-if="item.verified" class="text-[9px] px-1.5 py-0.5 rounded bg-emerald-500/15 border border-emerald-500/30 text-emerald-400 font-bold">已认证</span>
                        </div>
                        <span class="text-[10px] text-textMuted mt-1">ID: {{ item.id }} | 业务: {{ item.businessType }} | 电话: {{ item.contactPhone }}</span>
                      </div>
                    </div>
                    <div class="flex gap-2 flex-shrink-0">
                      <!-- VIP 快捷切换 -->
                      <button @click="toggleMerchantVip(item.id, !item.isVip)" 
                              :class="item.isVip ? 'text-amber-500 bg-amber-500/10 border-amber-500/20' : 'text-gray-400 bg-white/3 border-white/10'"
                              class="px-2 py-1.5 border hover:bg-white/5 rounded-lg text-xs font-semibold transition-all">
                        VIP
                      </button>
                      <!-- 审核快捷切换 -->
                      <button @click="toggleMerchantVerify(item.id, !item.verified)" 
                              :class="item.verified ? 'text-emerald-500 bg-emerald-500/10 border-emerald-500/20' : 'text-rose-500 bg-rose-500/10 border-rose-500/20'"
                              class="px-2 py-1.5 border hover:bg-white/5 rounded-lg text-xs font-semibold transition-all">
                        {{ item.verified ? '资质通过' : '审核拒绝' }}
                      </button>
                      <button @click="fillMerchantForm(item)" class="px-2.5 py-1.5 bg-blue-500/10 hover:bg-blue-500/25 active:scale-95 text-blue-400 border border-blue-500/35 rounded-lg text-xs font-bold transition-all">
                        编辑
                      </button>
                      <button @click="deleteMerchantRecord(item.id)" class="px-2.5 py-1.5 bg-rose-500/10 hover:bg-rose-500/25 active:scale-95 text-rose-400 border border-rose-500/35 rounded-lg text-xs font-bold transition-all">
                        删除
                      </button>
                    </div>
                  </div>
                </div>

                <!-- D. AI 抓取待审核列表 -->
                <div v-if="activeAdminTab === 'ai-agent'" class="space-y-4">
                  <!-- AI 控制头栏 -->
                  <div class="p-5 bg-black/40 border border-glassBorder rounded-xl flex items-center justify-between gap-4">
                    <div class="text-left">
                      <div class="font-extrabold text-sm text-white flex items-center gap-1.5">
                        <i class="fa-solid fa-gears text-neonRed"></i> AI 招投标数据情报官 Agent
                      </div>
                      <p class="text-[10px] text-textMuted mt-1">智能分析官方招标网站，并自动执行地名解析与脱敏打点流水线</p>
                    </div>
                    <button @click="runAiCrawl" :disabled="aiCrawlLoading"
                            class="px-5 py-2.5 bg-gradient-to-r from-neonRed to-rose-600 border border-neonRed/20 hover:scale-[1.02] active:scale-98 disabled:opacity-50 text-white rounded-xl text-xs font-extrabold shadow-red-glow transition-all flex items-center gap-2">
                      <i v-if="aiCrawlLoading" class="fa-solid fa-spinner animate-spin"></i>
                      <i v-else class="fa-solid fa-robot"></i>
                      {{ aiCrawlLoading ? '正在分析招标公告...' : '立即唤醒 AI 抓取' }}
                    </button>
                  </div>

                  <!-- 结果列表 -->
                  <div v-if="aiCrawlResults.length === 0" class="py-16 text-center text-xs text-textMuted border border-dashed border-glassBorder rounded-xl">
                    <i class="fa-solid fa-inbox text-2xl mb-2 text-glassBorder block"></i>
                    暂无采集线索，请点击右上方按钮唤醒 AI 智能体抓取最新招投标数据
                  </div>
                  <div v-else class="space-y-2.5">
                    <div v-for="item in aiCrawlResults" :key="item.name"
                         class="p-4 rounded-xl bg-black/40 border border-glassBorder hover:bg-white/5 transition-all text-xs text-gray-300 flex flex-col gap-3 text-left">
                      <div class="flex justify-between items-start gap-4">
                        <div class="min-w-0">
                          <div class="flex items-center gap-2">
                            <span class="font-extrabold text-white text-sm truncate max-w-[320px]">{{ item.name }}</span>
                            <span class="px-1.5 py-0.5 rounded bg-neonRed/10 border border-neonRed/20 text-neonRed text-[9px] font-bold">{{ item.category }}</span>
                          </div>
                          <div class="text-[10px] text-textMuted mt-1">地址：{{ item.address }} | 建议脱敏坐标：{{ item.x }}, {{ item.y }}</div>
                        </div>
                        <div class="flex gap-2">
                          <button @click="fillCrawlLeadToForm(item)" class="px-3 py-1.5 bg-blue-500/10 hover:bg-blue-500/25 active:scale-95 text-blue-400 border border-blue-500/35 rounded-lg text-xs font-bold transition-all">
                            导入编辑
                          </button>
                          <button @click="approveAndPublishLead(item)" class="px-3 py-1.5 bg-emerald-500/10 hover:bg-emerald-500/25 active:scale-95 text-emerald-400 border border-emerald-500/35 rounded-lg text-xs font-bold transition-all">
                            一键上架
                          </button>
                        </div>
                      </div>
                      <p class="text-textMuted text-[11px] leading-relaxed bg-black/20 p-2.5 rounded-lg border border-white/5">{{ item.jieshao }}</p>
                    </div>
                  </div>
                </div>

                <!-- E. 市民城管随手拍列表 -->
                <div v-if="activeAdminTab === 'citizen-reports'" class="space-y-2">
                  <div v-if="citizenReports.length === 0" class="py-16 text-center text-xs text-textMuted border border-dashed border-glassBorder rounded-xl">
                    <i class="fa-solid fa-clipboard-list text-2xl mb-2 text-glassBorder block"></i>
                    暂无市民上报的城管投诉工单
                  </div>
                  <div v-else v-for="item in citizenReports" :key="item.id" @click="selectedCitizenReport = item"
                       :class="selectedCitizenReport && selectedCitizenReport.id === item.id ? 'border-neonRed bg-neonRed/5' : 'border-glassBorder bg-black/40 hover:bg-white/5'"
                       class="flex items-center justify-between p-4 rounded-xl border transition-all text-xs text-gray-300 gap-4 cursor-pointer">
                    <div class="flex items-center gap-3 min-w-0 text-left">
                      <div :class="item.status === 0 ? 'bg-red-500/20 text-red-400 border-red-500/30' : (item.status === 1 ? 'bg-amber-500/20 text-amber-400 border-amber-500/30' : 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30')"
                           class="w-10 h-10 rounded-lg border flex flex-col items-center justify-center font-extrabold flex-shrink-0">
                        <i :class="item.status === 0 ? 'fa-solid fa-circle-exclamation' : (item.status === 1 ? 'fa-solid fa-clock-rotate-left' : 'fa-solid fa-circle-check')" class="text-sm"></i>
                        <span class="text-[8px] mt-0.5">{{ item.status === 0 ? '待处' : (item.status === 1 ? '处理' : '解决') }}</span>
                      </div>
                      <div class="flex flex-col min-w-0">
                        <div class="flex items-center gap-2">
                          <span class="font-extrabold text-white text-sm">{{ item.issue_type }}</span>
                          <span class="text-[10px] text-textMuted">{{ item.created_at.substring(0, 16).replace('T', ' ') }}</span>
                        </div>
                        <p class="text-textMuted text-xs mt-1 truncate max-w-[420px]">{{ item.description }}</p>
                      </div>
                    </div>
                    
                    <div class="flex gap-2 flex-shrink-0">
                      <button @click.stop="locateReportOnMap(item)" class="px-3 py-1.5 bg-neonRed/10 hover:bg-neonRed/20 text-neonRed border border-neonRed/20 rounded-lg text-xs font-bold transition-all flex items-center gap-1">
                        <i class="fa-solid fa-location-crosshairs"></i>地图定位
                      </button>
                    </div>
                  </div>
                </div>

              </div>
            </div>

          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
/* 渐入渐出动画 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

/* 渐入滑入动画 */
.fade-slide-enter-active, .fade-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translate(-50%, -20px);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: translate(-50%, -20px);
}

/* 缩放缩放动画 */
@keyframes scaleUp {
  0% { transform: scale(0.92); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}
.animate-scaleUp {
  animation: scaleUp 0.3s cubic-bezier(0.25, 0.8, 0.25, 1) forwards;
}
</style>
