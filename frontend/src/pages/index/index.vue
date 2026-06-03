<template>
  <view class="container">
    <!-- 顶部双模切换选项卡 (C端怀旧 vs B端商机) -->
    <view class="top-nav-bar">
      <view 
        class="nav-tab" 
        :class="'nav-tab-active-' + activeTab"
      >
        <view 
          class="tab-item" 
          :class="{ 'text-active': activeTab === 'c' }" 
          @click="switchTab('c')"
        >
          <text class="tab-icon">🌸</text>
          <text>老村记忆</text>
        </view>
        <view 
          class="tab-item" 
          :class="{ 'text-active': activeTab === 'b' }" 
          @click="switchTab('b')"
        >
          <text class="tab-icon">⚡</text>
          <text>商机雷达</text>
        </view>
        <view 
          class="tab-item" 
          :class="{ 'text-active': activeTab === 'i' }" 
          @click="switchTab('i')"
        >
          <text class="tab-icon">🤝</text>
          <text>数字金地</text>
        </view>
      </view>
    </view>

    <!-- B端专用的细分工种筛选滑块 -->
    <view class="filter-bar" v-if="activeTab === 'b'">
      <scroll-view scroll-x class="filter-scroll" show-scrollbar="false">
        <view 
          class="filter-item" 
          :class="{ 'filter-item-active': selectedCategory === '全部' }"
          @click="selectCategory('全部')"
        >全部</view>
        <view 
          v-for="cat in categories" 
          :key="cat" 
          class="filter-item" 
          :class="{ 'filter-item-active': selectedCategory === cat }"
          @click="selectCategory(cat)"
        >{{ cat }}</view>
      </scroll-view>
    </view>

    <!-- C端专用的时空影像透视轴滑块 (复用顶部同一高宽度槽位) -->
    <view class="timeline-bar" v-if="activeTab === 'c'">
      <view class="timeline-container">
        <view class="timeline-meta">
          <text class="timeline-title">⏳ 雄安时空变迁透视</text>
          <text class="timeline-year-badge">{{ selectedYear }} 年</text>
        </view>
        <slider 
          class="timeline-slider" 
          min="0" 
          max="100" 
          :value="sliderValue" 
          active-color="#e67e22" 
          block-color="#e67e22" 
          block-size="16"
          @changing="onSliderInput"
          @change="onSliderInput"
        />
        <view class="timeline-legend">
          <text class="legend-text">2018 村落原貌</text>
          <text class="legend-text">2026 建设新城</text>
        </view>
      </view>
    </view>

    <!-- WebGIS 地图核心组件：条件编译实现 H5 使用开源 Leaflet，小程序使用原生微信 map -->
    <!-- #ifdef MP-WEIXIN -->
    <map
      id="map"
      class="map"
      :longitude="center.longitude"
      :latitude="center.latitude"
      :scale="scale"
      :markers="filteredMarkers"
      :polygons="filteredPolygons"
      @markertap="onMarkerTap"
      @poitap="onPoiTap"
      @tap="onMapTap"
      show-location
    ></map>
    <!-- #endif -->

    <!-- #ifdef H5 -->
    <view id="h5-map" class="map"></view>
    <!-- H5 专用的高级 WebGIS 图层控制浮面 (毛玻璃微光设计) -->
    <view class="layer-control-panel">
      <view class="panel-header">
        <text class="panel-title">🗺️ 图层智能控制</text>
      </view>
      <view class="panel-body">
        <view class="layer-item" :class="{ 'layer-item-active': showBorder }" @click="toggleLayer('border')">
          <text class="layer-icon">🌐</text>
          <text class="layer-name">规划总边界</text>
        </view>
        <view class="layer-item" :class="{ 'layer-item-active': showZones }" @click="toggleLayer('zones')">
          <text class="layer-icon">🧱</text>
          <text class="layer-name">拆迁与建设区</text>
        </view>
      </view>
    </view>
    <!-- #endif -->
    
    <!-- 地图中央定位准心 (随手拍专用) -->
    <view class="map-crosshair" v-if="activeTab === 'c' && !selectedItem">
      <text class="crosshair-icon">+</text>
      <view class="crosshair-text">移动地图选点</view>
    </view>

    <!-- 底部弹出式精致详情卡片 -->
    <view 
      class="detail-card" 
      v-if="selectedItem" 
      :class="{ 'card-show': selectedItem, 'theme-c': activeTab === 'c', 'theme-b': activeTab === 'b' }"
    >
      <!-- 卡片顶部抓条与关闭按钮 -->
      <view class="card-drag-indicator"></view>
      <view class="card-header">
        <view class="card-title-group">
          <text class="card-badge" :class="activeTab === 'c' ? 'badge-c' : (activeTab === 'b' ? 'badge-b' : 'badge-i')">
            {{ activeTab === 'c' ? (selectedItem.type === 'village' ? (selectedItem.status === 0 ? '已拆迁' : (selectedItem.status === 1 ? '征迁中' : '保留村')) : '乡土记忆') : (activeTab === 'b' ? (selectedItem.category || '基建商机') : '招商地块') }}
          </text>
          <text class="card-title">{{ selectedItem.name || selectedItem.villageName || selectedItem.parcel_name || selectedItem.village_name }}</text>
        </view>
        <view class="close-btn" @click="closeCard">✕</view>
      </view>

      <scroll-view scroll-y class="card-content">
        <!-- ========================================== -->
        <!-- C端：老村庄情感内容展示 (带相册轮播) -->
        <!-- ========================================== -->
        <block v-if="activeTab === 'c'">
          <!-- 老照片轮播画廊 -->
          <swiper class="gallery-swiper" indicator-dots autoplay circular indicator-active-color="#e67e22">
            <swiper-item v-for="(img, idx) in getVillagePhotos(selectedItem)" :key="idx">
              <image :src="getImageUrl(img)" mode="aspectFill" class="gallery-img"></image>
            </swiper-item>
          </swiper>

          <view class="village-meta-grid">
            <view class="meta-item" v-if="selectedItem.type === 'village'">
              <text class="meta-label">当前状态</text>
              <text class="meta-val" :style="selectedItem.status === 0 ? 'color: #ef4444;' : (selectedItem.status === 1 ? 'color: #f59e0b;' : 'color: #10b981;')">
                {{ selectedItem.status === 0 ? '已拆除' : (selectedItem.status === 1 ? '征迁中' : '特色保留') }}
              </text>
            </view>
            <view class="meta-item" v-else>
              <text class="meta-label">所属原乡镇</text>
              <text class="meta-val">{{ selectedItem.township || '容城镇' }}</text>
            </view>
            <view class="meta-item">
              <text class="meta-label">{{ selectedItem.type === 'village' ? '变迁年份' : '拆迁年份' }}</text>
              <text class="meta-val highlight-amber">{{ selectedItem.plan_demolition_year || selectedItem.demolishedYear || '保留' }}{{ (selectedItem.plan_demolition_year || selectedItem.demolishedYear) ? '年' : '' }}</text>
            </view>
            <view class="meta-item" v-if="selectedItem.type === 'village'">
              <text class="meta-label">数据类型</text>
              <text class="meta-val" style="color: #10b981;">面矢量要素</text>
            </view>
            <view class="meta-item" v-else>
              <text class="meta-label">回迁安置地</text>
              <text class="meta-val">{{ selectedItem.currentDistrict || '容东片区' }}</text>
            </view>
          </view>

          <view class="section-title">✨ 老村庄旧事与回忆</view>
          <view class="village-history-card">
            <text class="quote-icon-left">“</text>
            <text class="history-desc">{{ selectedItem.jianjie || selectedItem.historyContent || selectedItem.history || '无历史背景资料' }}</text>
            <text class="quote-icon-right">”</text>
          </view>

          <!-- AI 怀旧文案大师引流工具 (毛玻璃微光设计) -->
          <view class="ai-marketing-box">
            <button class="ai-gen-btn" @click="generateAICopy" :loading="isGeneratingCopy">
              ✨ 唤醒老家记忆：生成 AI 视频/宣推文案
            </button>
            <button class="checkin-btn mt-2" @click="checkinSpot(selectedItem)">
              📍 立即打卡领红包
            </button>
          </view>

          <!-- C端回忆留言板 -->
          <view class="comment-section">
            <view class="comment-header">
              <text class="comment-title">💬 乡亲回忆留言板</text>
              <text class="comment-count">共 {{ comments.length }} 条发言</text>
            </view>

            <!-- 发表回忆框 -->
            <view class="comment-form-box">
              <input 
                class="comment-input-name" 
                v-model="newCommentAuthor" 
                placeholder="您的称呼 (例如: 大河村回迁村民)" 
              />
              <textarea 
                class="comment-input-content" 
                v-model="newCommentContent" 
                placeholder="说句心里话，聊聊我们老村子当年的故事..." 
                auto-height
              ></textarea>
              <button class="comment-submit-btn" @click="submitComment">
                📝 发布我的老家回忆
              </button>
            </view>

            <!-- 留言列表 -->
            <view class="comment-list" v-if="comments.length > 0">
              <view class="comment-card" v-for="c in comments" :key="c.id">
                <view class="comment-top">
                  <text class="comment-author">👤 {{ c.author || '匿名乡亲' }}</text>
                  <text class="comment-date">{{ formatCommentDate(c.createdAt) }}</text>
                </view>
                <text class="comment-text">{{ c.content }}</text>
              </view>
            </view>
            <view class="no-comment-placeholder" v-else>
              <text class="no-comment-icon">🍃</text>
              <text class="no-comment-txt">还没有乡亲留言，来写下第一条对老家的回忆吧！</text>
            </view>
          </view>
        </block>

        <!-- ========================================== -->
        <!-- B端：工地详情及临近供应链自动撮合 -->
        <!-- ========================================== -->
        <block v-if="activeTab === 'b'">
          <image :src="getImageUrl(selectedItem.src)" mode="aspectFill" class="card-banner"></image>
          
          <view class="project-info-box">
            <view class="info-row">
              <text class="info-label">工程地址：</text>
              <text class="info-value">{{ selectedItem.address || '雄安核心区' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">建设简介：</text>
              <text class="info-value">{{ selectedItem.jieshao }}</text>
            </view>
          </view>

          <!-- 核心功能：周边 30 公里供应链高价值撮合雷达 -->
          <view class="matching-section">
            <view class="matching-header">
              <text class="matching-title">🗺️ 周边 30公里 供应链配套推荐</text>
              <text class="matching-count">发现 {{ nearbyMerchants.length }} 个商户</text>
            </view>

            <view class="merchant-list" v-if="nearbyMerchants.length > 0">
              <view 
                class="merchant-item-card" 
                v-for="mer in nearbyMerchants" 
                :key="mer.id"
                :class="{ 'merchant-vip': mer.isVip }"
              >
                <view class="merchant-top">
                  <view class="merchant-name-group">
                    <text class="merchant-name">{{ mer.merchantName }}</text>
                    <text class="vip-tag" v-if="mer.isVip">VIP 优质商户</text>
                  </view>
                  <text class="merchant-type">{{ mer.businessType }}</text>
                </view>

                <view class="merchant-body">
                  <view class="merchant-info-item">
                    <text class="mer-label">联系人：</text>
                    <text class="mer-val">{{ mer.contactPerson }}</text>
                  </view>
                  <view class="merchant-info-item">
                    <text class="mer-label">服务半径：</text>
                    <text class="mer-val highlight-blue">{{ mer.serviceRadius }} 公里</text>
                  </view>
                  <view class="merchant-info-item">
                    <text class="mer-label">店铺位置：</text>
                    <text class="mer-val truncate-text">{{ mer.address }}</text>
                  </view>
                </view>

                <view class="merchant-actions">
                  <button class="call-btn" @click="callMerchant(mer)">
                    📞 一键电话联络商机
                  </button>
                </view>
              </view>
            </view>

            <!-- 暂无商户入驻的引流展示 -->
            <view class="no-merchant-box" v-else>
              <text class="no-mer-icon">🏢</text>
              <text class="no-mer-txt">当前工地周边暂无认证建材商或设备租赁队</text>
              <button class="join-btn" @click="joinPlatform">抢先入驻，获取商机订单</button>
            </view>
          </view>
        </block>

        <!-- ========================================== -->
        <!-- B端：招商引资 (数字金地) 详情 -->
        <!-- ========================================== -->
        <block v-if="activeTab === 'i'">
          <view class="project-info-box">
            <view class="info-row">
              <text class="info-label">地块名称：</text>
              <text class="info-value highlight-amber">{{ selectedItem.parcel_name }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">土地用途：</text>
              <text class="info-value">{{ selectedItem.land_use }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">面积：</text>
              <text class="info-value">{{ selectedItem.area_sqm }} 平方米</text>
            </view>
            <view class="info-row">
              <text class="info-label">容积率 (FAR)：</text>
              <text class="info-value">{{ selectedItem.far }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">状态：</text>
              <text class="info-value text-green-500">待出让</text>
            </view>
          </view>
          <view class="merchant-actions" style="margin-top: 10px;">
            <button class="call-btn" style="background: linear-gradient(135deg, #f39c12, #e67e22);" @click="showIntentModal = true">
              📝 提交投资意向
            </button>
          </view>
        </block>
      </scroll-view>
    </view>

    <!-- AI 怀旧文案弹窗 (毛玻璃拟态微光设计) -->
    <view class="ai-modal-mask" v-if="showAIModal" @click="closeAIModal">
      <view class="ai-modal-card" @click.stop>
        <view class="ai-modal-header">
          <text class="ai-modal-title">✨ AI 时空记忆宣推包</text>
          <view class="ai-modal-close" @click="closeAIModal">✕</view>
        </view>
        <scroll-view scroll-y class="ai-modal-body">
          <view class="ai-section">
            <view class="ai-section-header">
              <text class="ai-section-title">📖 小红书情感共鸣推文</text>
              <button class="copy-small-btn" @click="copyText(aiCopyResult.xiaohongshu)">📋 复制</button>
            </view>
            <view class="ai-text-box">
              <text class="ai-text-content">{{ aiCopyResult.xiaohongshu }}</text>
            </view>
          </view>

          <view class="ai-section">
            <view class="ai-section-header">
              <text class="ai-section-title">🎬 抖音/视频号 短视频脚本</text>
              <button class="copy-small-btn" @click="copyText(aiCopyResult.douyin)">📋 复制</button>
            </view>
            <view class="ai-text-box">
              <text class="ai-text-content">{{ aiCopyResult.douyin }}</text>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>
    <!-- 悬浮的 全民随手拍 按钮 (FAB) -->
    <view class="fab-btn" v-if="activeTab === 'c'" @click="openCitizenModal">
      <text class="fab-icon">📷</text>
    </view>

    <!-- 全民随手拍 弹窗（含图片上传） -->
    <view class="ai-modal-mask" v-if="showCitizenModal" @click="closeCitizenModal">
      <view class="ai-modal-card" @click.stop style="height: auto; max-height: 80vh;">
        <view class="ai-modal-header">
          <text class="ai-modal-title">📷 全民随手拍</text>
          <view class="ai-modal-close" @click="closeCitizenModal">✕</view>
        </view>
        <view class="ai-modal-body p-4">
          <view class="form-group">
            <text class="form-label">上报类型：</text>
            <picker :value="citizenIssueIndex" :range="issueTypes" @change="onIssueTypeChange" class="picker-box">
              <view class="picker-text">{{ issueTypes[citizenIssueIndex] }}</view>
            </picker>
          </view>
          <view class="form-group">
            <text class="form-label">拍照上传：</text>
            <view class="upload-area" @click="chooseImage">
              <image v-if="citizenImage.tempFilePath" :src="citizenImage.tempFilePath" mode="aspectFill" class="upload-preview"></image>
              <view v-else class="upload-placeholder">
                <text class="upload-icon">📸</text>
                <text class="upload-text">点击拍照或选择相册</text>
              </view>
            </view>
          </view>
          <view class="form-group">
            <text class="form-label">详细描述：</text>
            <textarea class="form-textarea" v-model="citizenForm.description" placeholder="请描述具体情况..."></textarea>
          </view>
          <button class="call-btn mt-4" style="background: linear-gradient(135deg, #1abc9c, #16a085);" @click="submitCitizenReport">
            提交上报
          </button>
        </view>
      </view>
    </view>

    <!-- 提交投资意向 弹窗 -->
    <view class="ai-modal-mask" v-if="showIntentModal" @click="closeIntentModal">
      <view class="ai-modal-card" @click.stop style="height: auto; max-height: 80vh;">
        <view class="ai-modal-header" style="border-bottom-color: rgba(243,156,18,0.2);">
          <text class="ai-modal-title" style="color: #f39c12;">🤝 提交投资意向</text>
          <view class="ai-modal-close" @click="closeIntentModal">✕</view>
        </view>
        <view class="ai-modal-body p-4">
          <view class="form-group">
            <text class="form-label">企业名称：</text>
            <input class="form-input" v-model="intentForm.company_name" placeholder="请输入贵司全称" />
          </view>
          <view class="form-group">
            <text class="form-label">联系电话：</text>
            <input class="form-input" v-model="intentForm.contact_phone" placeholder="请输入联系人电话" type="number" />
          </view>
          <button class="call-btn mt-4" style="background: linear-gradient(135deg, #f39c12, #e67e22);" @click="submitInvestmentIntent">
            确认提交
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { request, fastRequest } from '../../utils/request';

// 雄安新区中心坐标
const center = ref({
  longitude: 115.932145,
  latitude: 38.956795
});
const scale = ref(11);

const activeTab = ref('c'); // 'c' = C端情怀怀旧, 'b' = B端基建商机
const selectedCategory = ref('全部');
const categories = ['城市绿化', '市政配套', '交通物流', '安置房'];

// 数据源
const rawZdgc = ref([]);
const rawRwjg = ref([]);
const selectedItem = ref(null);
const nearbyMerchants = ref([]);

// 新增数据与状态
const rawParcels = ref([]);
const rawVillages = ref([]);
const showCitizenModal = ref(false);
const showIntentModal = ref(false);
const issueTypes = ['设施损坏', '环境脏乱', '美丽风景分享'];
const citizenIssueIndex = ref(0);
const citizenImage = ref({ tempFilePath: null, file: null });const citizenForm = ref({
  issue_type: '设施损坏',
  description: '',
  reporter_name: '热心市民',
  contact_phone: '',
  x: '',
  y: ''
});
const intentForm = ref({
  parcel_id: '',
  company_name: '',
  contact_person: '',
  contact_phone: '',
  intent_desc: ''
});

const filteredPolygons = computed(() => {
  if (activeTab.value === 'i') {
    return rawParcels.value.map((item, index) => {
      let points = [];
      try {
        points = item.geom.coordinates[0][0].map(coord => ({
          longitude: coord[0],
          latitude: coord[1]
        }));
      } catch(e) {}
      
      return {
        points: points,
        strokeWidth: 2,
        strokeColor: '#f39c12',
        fillColor: '#f39c124D',
        zIndex: 10
      };
    });
  }
  return [];
});

// C端评论留言数据
const comments = ref([]);
const newCommentAuthor = ref('');
const newCommentContent = ref('');

// 时空影像透视轴数据
const sliderValue = ref(0);
const selectedYear = computed(() => Math.round(2018 + (sliderValue.value / 100) * 8));

// 图层智能控制开关状态
const showBorder = ref(false);
const showZones = ref(false);

// Leaflet 地图与图层变量 (仅在 H5 环境下定义)
let leafletMap = null;
let leafletMarkersLayer = null;
let leafletOsmLayer = null;
let leafletSatelliteLayer = null;
let leafletBorderLayer = null;
let leafletZonesLayer = null;

const onSliderInput = (e) => {
  sliderValue.value = e.detail.value;
  if (leafletSatelliteLayer && activeTab.value === 'c') {
    leafletSatelliteLayer.setOpacity(sliderValue.value / 100);
  }
};

const toggleLayer = async (type) => {
  if (type === 'border') {
    showBorder.value = !showBorder.value;
    if (showBorder.value) {
      if (!leafletBorderLayer) {
        try {
          const res = await fetch('/static/geojson/quanjie.json');
          const data = await res.json();
          leafletBorderLayer = window.L.geoJSON(data, {
            style: {
              color: '#3498db',
              weight: 3,
              opacity: 0.8,
              fillOpacity: 0.05,
              fillColor: '#3498db'
            }
          });
        } catch (e) {
          console.error('Failed to load border geojson', e);
        }
      }
      if (leafletBorderLayer && leafletMap) {
        leafletBorderLayer.addTo(leafletMap);
      }
    } else {
      if (leafletBorderLayer && leafletMap) {
        leafletMap.removeLayer(leafletBorderLayer);
      }
    }
  } else if (type === 'zones') {
    showZones.value = !showZones.value;
    if (showZones.value) {
      if (!leafletZonesLayer) {
        try {
          const res = await fetch('/static/geojson/xinan.json');
          const data = await res.json();
          leafletZonesLayer = window.L.geoJSON(data, {
            style: function(feature) {
              const name = feature.properties.NL_NAME_3 || feature.properties.NAME_3 || '';
              if (name.includes('容城')) {
                // 容城县：重点建设与已落成新城区 (Rongdong/Rongxi) -> Emerald Green
                return {
                  color: '#2ecc71',
                  weight: 2,
                  opacity: 0.8,
                  fillOpacity: 0.35,
                  fillColor: '#2ecc71'
                };
              } else if (name.includes('雄县') || name.includes('Xiong')) {
                // 雄县：待拆迁与在建规划枢纽 (Zangang) -> Amber Gold
                return {
                  color: '#f1c40f',
                  weight: 2,
                  opacity: 0.8,
                  fillOpacity: 0.35,
                  fillColor: '#f1c40f'
                };
              } else {
                // 安新县：生态保全与退耕还淀区 (Wetland Reserve) -> Terracotta Red
                return {
                  color: '#e74c3c',
                  weight: 2,
                  opacity: 0.8,
                  fillOpacity: 0.35,
                  fillColor: '#e74c3c'
                };
              }
            },
            onEachFeature: function(feature, layer) {
              const name = feature.properties.NL_NAME_3 || feature.properties.NAME_3 || '';
              let popupContent = '';
              if (name.includes('容城')) {
                popupContent = '<div style="font-family: sans-serif; padding: 4px; min-width: 180px;">' +
                               '<b style="color: #2ecc71; font-size: 14px; display: block; margin-bottom: 6px;">🏢 重点建设与已落成城区 (容城县)</b>' +
                               '<span style="font-size: 12px; color: #555; line-height: 1.5; display: block;">' +
                               '主要规划：容东安置区、容西安置区、雄安市民服务中心、雄安商务中心。<br/>' +
                               '<b style="color: #2c3e50; margin-top: 4px; display: block;">状态：绝大部分老村落已拆迁腾退，高品质现代化绿色宜居新城全面落成起步！</b>' +
                               '</span></div>';
              } else if (name.includes('雄县') || name.includes('Xiong')) {
                popupContent = '<div style="font-family: sans-serif; padding: 4px; min-width: 180px;">' +
                               '<b style="color: #e67e22; font-size: 14px; display: block; margin-bottom: 6px;">🧱 待拆迁与规划在建区 (雄县)</b>' +
                               '<span style="font-size: 12px; color: #555; line-height: 1.5; display: block;">' +
                               '主要规划：昝岗高端产业片区、雄安站综合交通枢纽、临港物流园区。<br/>' +
                               '<b style="color: #2c3e50; margin-top: 4px; display: block;">状态：征地拆迁腾退已处于收尾期，世界级高铁客运枢纽与周边核心路网正处于火热建设状态！</b>' +
                               '</span></div>';
              } else {
                popupContent = '<div style="font-family: sans-serif; padding: 4px; min-width: 180px;">' +
                               '<b style="color: #e74c3c; font-size: 14px; display: block; margin-bottom: 6px;">🍃 生态保全与退耕还淀区 (安新县)</b>' +
                               '<span style="font-size: 12px; color: #555; line-height: 1.5; display: block;">' +
                               '主要规划：白洋淀生态保护核心区、千年秀林碳汇森林带、白洋淀湿地公园。<br/>' +
                               '<b style="color: #2c3e50; margin-top: 4px; display: block;">状态：已全面完成淀区内低洼敏感老村落的环保大拆迁移民，坚决退耕还林还湿地，死守华北明珠生态红线！</b>' +
                               '</span></div>';
              }
              layer.bindPopup(popupContent);
            }
          });
        } catch (e) {
          console.error('Failed to load zones geojson', e);
        }
      }
      if (leafletZonesLayer && leafletMap) {
        leafletZonesLayer.addTo(leafletMap);
      }
    } else {
      if (leafletZonesLayer && leafletMap) {
        leafletMap.removeLayer(leafletZonesLayer);
      }
    }
  }
};

// 获取服务器完整图片路径
const getImageUrl = (path) => {
  if (!path) return '';
  if (path.startsWith('http')) return path;
  return `http://localhost:8080${path}`;
};

// 解析 C 端照片
const getVillagePhotos = (item) => {
  if (!item) return [];
  if (item.photoGallery) {
    try {
      return JSON.parse(item.photoGallery);
    } catch (e) {
      // JSON 解析失败则返回默认数组
    }
  }
  return [item.src || '/images/rwjg/1.jpg'];
};

const fetchData = async () => {
  try {
    const zdgcRes = await request({ url: '/zdgc/list' });
    const rwjgRes = await request({ url: '/rwjg/list' });
    
    if (zdgcRes.code === 200 && zdgcRes.data) {
      rawZdgc.value = zdgcRes.data;
    }
    if (rwjgRes.code === 200 && rwjgRes.data) {
      rawRwjg.value = rwjgRes.data;
    }
    
    // Fetch Investment Parcels
    try {
      const parcelsRes = await fastRequest({ url: '/investment/parcels' });
      if (parcelsRes && parcelsRes.features) {
        rawParcels.value = parcelsRes.features.map(f => ({
          ...f.properties,
          id: f.properties.id,
          geom: f.geometry
        }));
      }
    } catch(err) {
      console.log('Parcels fail', err);
    }
    
    // Fetch Spatial Villages
    try {
      const villagesRes = await fastRequest({ url: '/spatial/villages' });
      if (villagesRes && villagesRes.features) {
        rawVillages.value = villagesRes.features;
      }
    } catch(err) {
      console.log('Villages fail', err);
    }
    
    // #ifdef H5
    if (window.L) {
      updateLeafletMarkers(window.L);
      updateLeafletPolygons(window.L);
    }
    // #endif
  } catch (error) {
    console.error('获取数据失败', error);
  }
};

// 动态计算在地图上展示的 Marker 数据源
const filteredMarkers = computed(() => {
  if (activeTab.value === 'c') {
    return rawRwjg.value.map(item => ({
      id: parseInt(item.id) + 10000,
      latitude: parseFloat(item.y),
      longitude: parseFloat(item.x),
      x: item.x,
      y: item.y,
      name: item.name,
      iconPath: '/static/marker-rwjg.png',
      width: 38,
      height: 38,
      type: 'rwjg',
      ...item
    }));
  } else if (activeTab.value === 'b') {
    let projects = rawZdgc.value;
    if (selectedCategory.value !== '全部') {
      projects = projects.filter(p => p.category === selectedCategory.value);
    }
    return projects.map(item => ({
      id: parseInt(item.id),
      latitude: parseFloat(item.y),
      longitude: parseFloat(item.x),
      x: item.x,
      y: item.y,
      name: item.name,
      iconPath: '/static/marker-zdgc.png',
      width: 36,
      height: 36,
      type: 'zdgc',
      ...item
    }));
  } else if (activeTab.value === 'i') {
    // 招商引资为了小程序能点击，计算中心点放置不可见/专用 Marker
    return rawParcels.value.map(item => {
      return {
        id: parseInt(item.id) + 20000,
        latitude: parseFloat(item.centroid_y),
        longitude: parseFloat(item.centroid_x),
        name: item.parcel_name,
        iconPath: '/static/marker-zdgc.png', // 暂时复用或用透明/特殊图标
        width: 32,
        height: 32,
        type: 'parcel',
        ...item
      };
    });
  }
  return [];
});

const switchTab = (tab) => {
  activeTab.value = tab;
  selectedItem.value = null;
  nearbyMerchants.value = [];
  scale.value = tab === 'c' ? 11.5 : 11;
  
  // 同步切换地图底图图层显示
  if (leafletSatelliteLayer) {
    if (tab === 'b' || tab === 'i') {
      // B端：保持为纯矢量路网底图，突出工地科技蓝Marker
      leafletSatelliteLayer.setOpacity(0);
    } else {
      // C端：还原为时空轴指定的变迁透明度
      leafletSatelliteLayer.setOpacity(sliderValue.value / 100);
    }
  }
  // #ifdef H5
  if (window.L) {
    updateLeafletPolygons(window.L);
  }
  // #endif
};

const selectCategory = (cat) => {
  selectedCategory.value = cat;
  selectedItem.value = null;
  nearbyMerchants.value = [];
};

const onMarkerTap = async (e) => {
  const markerId = e.detail.markerId || e.markerId;
  let item = null;
  
  if (activeTab.value === 'c') {
    item = rawRwjg.value.find(m => (parseInt(m.id) + 10000) === markerId);
  } else if (activeTab.value === 'b') {
    item = rawZdgc.value.find(m => parseInt(m.id) === markerId);
  } else if (activeTab.value === 'i') {
    item = rawParcels.value.find(m => (parseInt(m.id) + 20000) === markerId);
  }
  
  if (item) {
    selectedItem.value = item;
    center.value = {
      longitude: parseFloat(item.x || item.centroid_x),
      latitude: parseFloat(item.y || item.centroid_y)
    };
    
    // 如果是 C 端老村庄，加载情感回忆留言板数据
    if (activeTab.value === 'c') {
      fetchComments(item.id);
    }
    
    // 如果是 B 端工地，自动触发空间搜索，匹配周边 30 公里的供应链配套
    if (activeTab.value === 'b') {
      try {
        const matchingRes = await request({
          url: `/merchant/nearby?longitude=${item.x}&latitude=${item.y}&distance=30`
        });
        if (matchingRes.code === 200 && matchingRes.data) {
          nearbyMerchants.value = matchingRes.data;
        } else {
          nearbyMerchants.value = [];
        }
      } catch (err) {
        console.error('获取周边供应链失败', err);
        nearbyMerchants.value = [];
      }
    }
  }
};

// ==========================================
// H5 环境：动态异步加载 Leaflet 地图引擎 (无 Key 解决方案)
// ==========================================
// #ifdef H5
const loadLeaflet = () => {
  return new Promise((resolve) => {
    if (window.L) {
      resolve(window.L);
      return;
    }
    // 动态添加 CSS 样式表 (采用极速国内 BootCDN 节点)
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = 'https://cdn.bootcdn.net/ajax/libs/leaflet/1.9.4/leaflet.min.css';
    document.head.appendChild(link);

    // 动态添加 JavaScript 核心库
    const script = document.createElement('script');
    script.src = 'https://cdn.bootcdn.net/ajax/libs/leaflet/1.9.4/leaflet.min.js';
    script.onload = () => {
      resolve(window.L);
    };
    document.head.appendChild(script);
  });
};

const initLeafletMap = async () => {
  const L = await loadLeaflet();
  
  // 初始化 Leaflet 地图
  leafletMap = L.map('h5-map', {
    zoomControl: false,
    attributionControl: false,
    tap: false // 禁用默认 tap 拦截，解决移动端与响应式开发触控下拖拽失效的问题
  }).setView([center.value.latitude, center.value.longitude], scale.value);

  // 1. 底层：OpenStreetMap 标准瓦片 (与 B 端 PC 一致)
  leafletOsmLayer = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    subdomains: "abc",
    maxZoom: 18
  }).addTo(leafletMap);  // 2. 顶层：使用国内高德地图高清卫星实景图层 (2026 建设新城卫星实景 - 确保卫星影像秒级呈现)
  leafletSatelliteLayer = L.tileLayer('https://webst{s}.is.autonavi.com/appmaptile?style=6&x={x}&y={y}&z={z}', {
    subdomains: ["01", "02", "03", "04"],
    maxZoom: 18,
    opacity: sliderValue.value / 100
  }).addTo(leafletMap);

  leafletMarkersLayer = L.layerGroup().addTo(leafletMap);
  leafletPolygonsLayer = L.layerGroup().addTo(leafletMap);

  // 绑定首批渲染的打点
  updateLeafletMarkers(L);
  updateLeafletPolygons(L);
};

let leafletPolygonsLayer = null;

const getPolygonCentroid = (geometry) => {
  try {
    if (geometry && geometry.type === 'Polygon' && geometry.coordinates && geometry.coordinates[0]) {
      const coords = geometry.coordinates[0];
      let sumLng = 0;
      let sumLat = 0;
      coords.forEach(pt => {
        sumLng += pt[0];
        sumLat += pt[1];
      });
      return {
        longitude: sumLng / coords.length,
        latitude: sumLat / coords.length
      };
    }
  } catch (e) {}
  return null;
};

const onVillagePolygonTap = (item) => {
  selectedItem.value = {
    id: item.properties.id,
    name: item.properties.village_name,
    status: item.properties.status,
    plan_demolition_year: item.properties.plan_demolition_year,
    history: item.properties.history,
    type: 'village'
  };
  const centroid = getPolygonCentroid(item.geometry);
  if (centroid) {
    center.value = centroid;
  }
  fetchComments(item.properties.id);
};

const updateLeafletPolygons = (L) => {
  if (!leafletMap || !leafletPolygonsLayer) return;
  leafletPolygonsLayer.clearLayers();
  
  if (activeTab.value === 'i') {
    rawParcels.value.forEach(item => {
      try {
        const polygon = L.geoJSON(item.geom, {
          style: {
            color: '#f39c12',
            weight: 2,
            opacity: 1,
            fillOpacity: 0.3,
            fillColor: '#f39c12'
          }
        });
        polygon.on('click', () => {
          onMarkerTap({ detail: { markerId: parseInt(item.id) + 20000 }});
        });
        leafletPolygonsLayer.addLayer(polygon);
      } catch (e) {}
    });
  } else if (activeTab.value === 'c') {
    rawVillages.value.forEach(item => {
      try {
        const status = item.properties.status;
        const demoYear = item.properties.plan_demolition_year;
        
        // 动态展示逻辑（与PC后台时空滑块一致）
        // 大于所选年份说明还没拆除，显示为未拆除（特色保留/暂未拆迁）
        let isActuallyDemolished = false;
        let isActuallyDemolishing = false;
        
        if (status === 0) {
          if (!demoYear || demoYear <= selectedYear.value) {
            isActuallyDemolished = true;
          }
        } else if (status === 1) {
          if (!demoYear || demoYear <= selectedYear.value) {
            isActuallyDemolishing = true;
          }
        }
        
        let fillColor = 'rgba(16, 185, 129, 0.2)'; // 绿色：特色保留/未拆除
        let strokeColor = '#10B981';
        
        if (isActuallyDemolished) {
          fillColor = 'rgba(239, 68, 68, 0.2)'; // 红色：已拆除
          strokeColor = '#EF4444';
        } else if (isActuallyDemolishing) {
          fillColor = 'rgba(245, 158, 11, 0.25)'; // 黄色：征迁拆迁中
          strokeColor = '#F59E0B';
        }
        
        const polygon = L.geoJSON(item.geometry, {
          style: {
            color: strokeColor,
            weight: 2,
            opacity: 0.8,
            fillOpacity: 0.2,
            fillColor: fillColor
          }
        });
        polygon.on('click', () => {
          onVillagePolygonTap(item);
        });
        leafletPolygonsLayer.addLayer(polygon);
      } catch (e) {
        console.error('Error drawing village polygon', e);
      }
    });
  }
};

const updateLeafletMarkers = (L) => {
  if (!leafletMap || !leafletMarkersLayer) return;
  leafletMarkersLayer.clearLayers();

  // 自定义 C端 温暖色老村庄打点 Marker 图标 (高阶 SVG 悬浮光效)
  const rwjgIcon = L.divIcon({
    html: `
      <div class="premium-gis-marker nostalgia-marker">
        <div class="pulse-wave-ring"></div>
        <div class="pulse-wave-ring-delayed"></div>
        <svg class="marker-svg" viewBox="0 0 40 46">
          <defs>
            <filter id="shadow-c" x="-20%" y="-20%" width="140%" height="140%">
              <feDropShadow dx="0" dy="3" stdDeviation="2.5" flood-color="#e67e22" flood-opacity="0.45"/>
            </filter>
            <linearGradient id="grad-c" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#ffbe76"/>
              <stop offset="100%" stop-color="#e67e22"/>
            </linearGradient>
          </defs>
          <path d="M20 2C11 2 4 9 4 18c0 11 16 25 16 25s16-14 16-25C36 9 29 2 20 2z" fill="url(#grad-c)" filter="url(#shadow-c)"/>
          <circle cx="20" cy="18" r="9" fill="#ffffff" fill-opacity="0.95"/>
          <!-- 传统屋顶/老村门楼线条 SVG -->
          <path d="M14 20v-4.5l6-3.5 6 3.5v4.5M17 20v-3h6v3" fill="none" stroke="#d35400" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>
    `,
    className: 'custom-leaflet-icon-container',
    iconSize: [40, 46],
    iconAnchor: [20, 45]
  });

  // 自定义 B端 科技蓝工地打点 Marker 图标 (高阶 SVG 雷达光效)
  const zdgcIcon = L.divIcon({
    html: `
      <div class="premium-gis-marker construction-marker">
        <div class="pulse-wave-ring"></div>
        <div class="pulse-wave-ring-delayed"></div>
        <svg class="marker-svg" viewBox="0 0 40 46">
          <defs>
            <filter id="shadow-b" x="-20%" y="-20%" width="140%" height="140%">
              <feDropShadow dx="0" dy="3" stdDeviation="2.5" flood-color="#2980b9" flood-opacity="0.5"/>
            </filter>
            <linearGradient id="grad-b" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#00ecff"/>
              <stop offset="100%" stop-color="#2980b9"/>
            </linearGradient>
          </defs>
          <path d="M20 2C11 2 4 9 4 18c0 11 16 25 16 25s16-14 16-25C36 9 29 2 20 2z" fill="url(#grad-b)" filter="url(#shadow-b)"/>
          <circle cx="20" cy="18" r="9" fill="#ffffff" fill-opacity="0.95"/>
          <!-- 科技塔吊/工地吊车线条 SVG -->
          <path d="M14 22h12 M20 22v-9 M20 13h5 M20 13l-5-3 M15 22l5-9" fill="none" stroke="#2980b9" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>
    `,
    className: 'custom-leaflet-icon-container',
    iconSize: [40, 46],
    iconAnchor: [20, 45]
  });

  filteredMarkers.value.forEach(item => {
    const icon = item.type === 'rwjg' ? rwjgIcon : zdgcIcon;
    // 如果是 parcel 类型，在 H5 使用多边形点击，不显示 marker
    if (item.type === 'parcel') return;
    
    const marker = L.marker([parseFloat(item.latitude || item.y), parseFloat(item.longitude || item.x)], { icon: icon });
    marker.on('click', () => {
      onMarkerTap({
        detail: {
          markerId: item.id
        }
      });
    });
    leafletMarkersLayer.addLayer(marker);
  });
};

// 监听打点和中心的改变，同步更新 H5 开源地图
watch(filteredMarkers, () => {
  if (window.L) {
    updateLeafletMarkers(window.L);
  }
}, { deep: true });

watch(selectedYear, () => {
  if (window.L) {
    updateLeafletPolygons(window.L);
  }
});

watch(center, (newVal) => {
  if (leafletMap) {
    leafletMap.setView([newVal.latitude, newVal.longitude], scale.value);
  }
});
// #endif

const callMerchant = (mer) => {
  if (!mer.contactPhone) return;
  uni.makePhoneCall({
    phoneNumber: mer.contactPhone
  });
};

const joinPlatform = () => {
  uni.showModal({
    title: '商家入驻申请',
    content: '欢迎保定、定州及雄安当地的五金、建材、机械租赁、劳务派遣团队入驻时空情报平台，请联系官方客服: 138-xxxx-xxxx。',
    showCancel: false
  });
};

const fetchComments = async (villageId) => {
  try {
    if (selectedItem.value && selectedItem.value.type === 'village') {
      const res = await fastRequest({
        url: `/spatial/villages/${encodeURIComponent(villageId)}/comments`
      });
      if (res) {
        comments.value = res;
      } else {
        comments.value = [];
      }
    } else {
      const res = await request({
        url: `/comment/list?villageId=${villageId}`
      });
      if (res.code === 200 && res.data) {
        comments.value = res.data;
      } else {
        comments.value = [];
      }
    }
  } catch (err) {
    console.error('获取留言列表失败', err);
    comments.value = [];
  }
};

const submitComment = async () => {
  if (!newCommentContent.value.trim()) {
    uni.showToast({
      title: '请输入回忆内容',
      icon: 'none'
    });
    return;
  }
  const author = newCommentAuthor.value.trim() || '匿名乡亲';
  
  if (selectedItem.value && selectedItem.value.type === 'village') {
    const payload = {
      author: author,
      content: newCommentContent.value.trim()
    };
    try {
      const res = await fastRequest({
        url: `/spatial/villages/${encodeURIComponent(selectedItem.value.id)}/comments`,
        method: 'POST',
        data: payload
      });
      if (res) {
        uni.showToast({
          title: '回忆发表成功',
          icon: 'success'
        });
        comments.value.unshift(res);
        newCommentContent.value = '';
        newCommentAuthor.value = '';
      }
    } catch (err) {
      console.error('提交留言失败', err);
      uni.showToast({
        title: '发布失败，请检查网络',
        icon: 'none'
      });
    }
  } else {
    const payload = {
      villageId: selectedItem.value.id.toString(),
      author: author,
      content: newCommentContent.value.trim()
    };
    try {
      const res = await request({
        url: '/comment/save',
        method: 'POST',
        data: payload
      });
      if (res.code === 200 && res.data) {
        uni.showToast({
          title: '回忆发表成功',
          icon: 'success'
        });
        comments.value.unshift(res.data);
        newCommentContent.value = '';
        newCommentAuthor.value = '';
      } else {
        uni.showToast({
          title: res.message || '发布失败',
          icon: 'none'
        });
      }
    } catch (err) {
      console.error('提交留言失败', err);
      uni.showToast({
        title: '发布失败，请检查网络',
        icon: 'none'
      });
    }
  }
};

// ======================
// 新增 API 交互逻辑
// ======================
const onIssueTypeChange = (e) => {
  citizenIssueIndex.value = e.detail.value;
  citizenForm.value.issue_type = issueTypes[citizenIssueIndex.value];
};

const openCitizenModal = () => {
  citizenForm.value.x = center.value.longitude.toFixed(6);
  citizenForm.value.y = center.value.latitude.toFixed(6);
  showCitizenModal.value = true;
};
const closeCitizenModal = () => showCitizenModal.value = false;

const closeIntentModal = () => showIntentModal.value = false;

const chooseImage = () => {
  uni.showActionSheet({
    itemList: ["拍照", "从相册选择"],
    success: (res) => {
      const source = res.tapIndex === 0 ? "camera" : "album";
      uni.chooseImage({
        count: 1,
        sourceType: [source],
        sizeType: ["compressed"],
        success: (r) => {
          citizenImage.value.tempFilePath = r.tempFilePaths[0];
          citizenImage.value.file = r.tempFiles[0];
        }
      });
    }
  });
};

const submitCitizenReport = async () => {
  if (!citizenForm.value.description.trim()) {
    return uni.showToast({ title: "请填写详细描述", icon: "none" });
  }
  uni.showLoading({ title: "上传中..." });
  try {
    const res = await new Promise((resolve, reject) => {
      uni.uploadFile({
        url: "http://localhost:8080/api/citizen/report",
        filePath: citizenImage.value.tempFilePath,
        name: "image",
        formData: {
          description: citizenForm.value.description,
          issueType: citizenForm.value.issue_type,
          reporterName: citizenForm.value.reporter_name,
          x: citizenForm.value.x,
          y: citizenForm.value.y
        },
        success: (r) => resolve(r),
        fail: (e) => reject(e)
      });
    });
    uni.hideLoading();
    const data = typeof res.data === "string" ? JSON.parse(res.data) : res.data;
    if (data.code === 200) {
      uni.showToast({ title: "上报成功，感谢支持！", icon: "success" });
      showCitizenModal.value = false;
      citizenForm.value.description = "";
      citizenImage.value = { tempFilePath: null, file: null };
    } else {
      uni.showToast({ title: data.message || "上报失败", icon: "none" });
    }
  } catch(e) {
    uni.hideLoading();
    uni.showToast({ title: "网络异常，请稍后重试", icon: "none" });
  }
};

const submitInvestmentIntent = async () => {
  if (!intentForm.value.company_name.trim() || !intentForm.value.contact_phone.trim()) {
    return uni.showToast({ title: '请填写公司名称和联系电话', icon: 'none' });
  }
  intentForm.value.parcel_id = selectedItem.value.id.toString();
  try {
    const res = await fastRequest({
      url: '/investment/intent',
      method: 'POST',
      data: intentForm.value
    });
    uni.showToast({ title: '意向已提交！', icon: 'success' });
    showIntentModal.value = false;
    intentForm.value = { parcel_id: '', company_name: '', contact_person: '', contact_phone: '', intent_desc: '' };
  } catch(e) {}
};

const mockLocation = ref(true); // 默认开启模拟以便测试

const getDistance = (lat1, lng1, lat2, lng2) => {
  const radLat1 = lat1 * Math.PI / 180.0;
  const radLat2 = lat2 * Math.PI / 180.0;
  const a = radLat1 - radLat2;
  const b = lng1 * Math.PI / 180.0 - lng2 * Math.PI / 180.0;
  let s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) +
    Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
  s = s * 6378.137;
  s = Math.round(s * 10000) / 10;
  return s; // 返回米
};

const checkinSpot = async (spot) => {
  uni.showLoading({ title: '定位中...' });
  
  uni.getLocation({
    type: 'gcj02',
    success: async (res) => {
      uni.hideLoading();
      // 开发阶段可开启 mockLocation 测试，模拟就在景点旁边
      const userLat = mockLocation.value ? parseFloat(spot.y || spot.centroid_y) + 0.001 : res.latitude;
      const userLng = mockLocation.value ? parseFloat(spot.x || spot.centroid_x) + 0.001 : res.longitude;
      
      const distance = getDistance(userLat, userLng, parseFloat(spot.y || spot.centroid_y), parseFloat(spot.x || spot.centroid_x));
      
      if (!mockLocation.value && distance > 500) {
        uni.showModal({
          title: '打卡失败',
          content: `您距离景点 ${distance.toFixed(0)} 米，请前往现场打卡！\n(测试阶段可开启模拟定位)`,
          showCancel: false
        });
        return;
      }

      try {
        const result = await fastRequest({
          url: `/cultural/checkin?spot_id=${spot.id}`,
          method: 'POST'
        });
        if (result) {
          uni.vibrateShort(); // 打卡成功震动反馈
          uni.showModal({
            title: '打卡成功 🎉',
            content: `恭喜您点亮新地标！\n当前距离: ${distance.toFixed(0)}米\n获得奖励: ${result.coupon}`,
            showCancel: false
          });
        }
      } catch(e) {}
    },
    fail: () => {
      uni.hideLoading();
      uni.showToast({ title: '无法获取位置权限', icon: 'none' });
    }
  });
};

// === 小程序原生多边形事件拦截 ===
const onPoiTap = (e) => {
  // POI Tap event fallback
};

const onMapTap = (e) => {
  // 地图点击，如果需要取点，可以更新 center 供随手拍使用
  center.value = {
    longitude: e.detail.longitude,
    latitude: e.detail.latitude
  };
};

const formatCommentDate = (dateStr) => {
  if (!dateStr) return '';
  try {
    const parts = dateStr.split('T');
    if (parts.length > 0) {
      const date = parts[0];
      const time = parts[1] ? parts[1].substring(0, 5) : '';
      return `${date} ${time}`;
    }
  } catch (e) {}
  return dateStr;
};

const closeCard = () => {
  selectedItem.value = null;
  nearbyMerchants.value = [];
  comments.value = [];
};

// AI 宣推文案变量与函数
const showAIModal = ref(false);
const isGeneratingCopy = ref(false);
const aiCopyResult = ref({ xiaohongshu: '', douyin: '' });

const generateAICopy = () => {
  if (!selectedItem.value) return;
  isGeneratingCopy.value = true;
  const payload = {
    village_name: selectedItem.value.name || selectedItem.value.villageName,
    township: selectedItem.value.township || "容城镇",
    current_district: selectedItem.value.currentDistrict || "容东片区",
    history: selectedItem.value.jianjie || selectedItem.value.historyContent || ""
  };
  
  uni.request({
    url: 'http://127.0.0.1:8000/agent/generate-marketing-copy',
    method: 'POST',
    data: payload,
    success: (res) => {
      if (res.statusCode === 200 && res.data) {
        aiCopyResult.value = res.data;
        showAIModal.value = true;
      } else {
        uni.showToast({
          title: 'AI 繁忙，请稍后再试',
          icon: 'none'
        });
      }
    },
    fail: (err) => {
      console.error('AI generate copy failed', err);
      uni.showToast({
        title: '网络错误，请检查 AI 服务',
        icon: 'none'
      });
    },
    complete: () => {
      isGeneratingCopy.value = false;
    }
  });
};

const closeAIModal = () => {
  showAIModal.value = false;
};

const copyText = (text) => {
  uni.setClipboardData({
    data: text,
    success: () => {
      uni.showToast({
        title: '复制成功，快去分享吧！',
        icon: 'success'
      });
    }
  });
};

onMounted(() => {
  fetchData();
  
  // #ifdef H5
  setTimeout(() => {
    initLeafletMap();
  }, 150);
  // #endif
});
</script>

<style>
/* ========================================== */
/* 现代轻量化 GIS 微信小程序设计规范 (深浅对比) */
/* ========================================== */

.container {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}

.map {
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
}

/* ========================================== */
/* 顶部双模切换胶囊 (Glassmorphism 玻璃微光) */
/* ========================================== */

.top-nav-bar {
  position: absolute;
  top: 40rpx;
  left: 5%;
  width: 90%;
  z-index: 1000;
  display: flex;
  justify-content: center;
}

.nav-tab {
  display: flex;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border-radius: 50rpx;
  padding: 8rpx;
  box-shadow: 0 10rpx 30rpx rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.5);
  position: relative;
  width: 100%;
  transition: all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.tab-item {
  flex: 1;
  text-align: center;
  font-size: 28rpx;
  font-weight: 600;
  color: #666;
  padding: 16rpx 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
  transition: color 0.3s;
}

.tab-icon {
  margin-right: 8rpx;
  font-size: 32rpx;
}

/* 动效激活滑块底色 (C端Amber暖色 vs B端Blue科技色) */
.nav-tab-active-c::after {
  content: "";
  position: absolute;
  top: 8rpx;
  left: 8rpx;
  width: calc(50% - 8rpx);
  height: calc(100% - 16rpx);
  background: linear-gradient(135deg, #f39c12, #e67e22);
  border-radius: 40rpx;
  z-index: 1;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.nav-tab-active-b::after {
  content: "";
  position: absolute;
  top: 8rpx;
  left: calc(50%);
  width: calc(50% - 8rpx);
  height: calc(100% - 16rpx);
  background: linear-gradient(135deg, #2980b9, #3498db);
  border-radius: 40rpx;
  z-index: 1;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.text-active {
  color: #ffffff !important;
}

/* ========================================== */
/* B端工种横向筛选条 */
/* ========================================== */

.filter-bar {
  position: absolute;
  top: 170rpx;
  left: 0;
  width: 100%;
  z-index: 999;
  padding: 10rpx 30rpx;
  box-sizing: border-box;
}

.filter-scroll {
  white-space: nowrap;
  width: 100%;
}

.filter-item {
  display: inline-block;
  padding: 12rpx 30rpx;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 30rpx;
  margin-right: 16rpx;
  font-size: 26rpx;
  font-weight: 500;
  color: #555;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.6);
}

.filter-item-active {
  background: #2980b9 !important;
  color: #ffffff !important;
  border-color: #2980b9 !important;
  box-shadow: 0 6rpx 16rpx rgba(41, 128, 185, 0.3);
}

/* ========================================== */
/* C端时空影像透视横向轴 */
/* ========================================== */
.timeline-bar {
  position: absolute;
  top: 170rpx;
  left: 5%;
  width: 90%;
  z-index: 999;
  box-sizing: border-box;
}

.timeline-container {
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px);
  border-radius: 36rpx;
  padding: 16rpx 28rpx;
  box-shadow: 0 10rpx 30rpx rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.timeline-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6rpx;
}

.timeline-title {
  font-size: 24rpx;
  font-weight: 700;
  color: #845c47;
}

.timeline-year-badge {
  font-size: 24rpx;
  font-weight: 800;
  color: #e67e22;
  background: #fdf2e9;
  padding: 2rpx 14rpx;
  border-radius: 20rpx;
}

.timeline-slider {
  margin: 10rpx 0;
  padding: 0;
}

.timeline-legend {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.legend-text {
  font-size: 20rpx;
  font-weight: 600;
  color: #a88;
}

/* ========================================== */
/* 高级 WebGIS 图层控制面板 (Glassmorphism) */
/* ========================================== */
.layer-control-panel {
  position: absolute;
  top: 310rpx;
  right: 5%;
  width: 260rpx;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px);
  border-radius: 28rpx;
  padding: 16rpx 20rpx;
  box-shadow: 0 10rpx 30rpx rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.5);
  z-index: 999;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.panel-header {
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  padding-bottom: 10rpx;
  margin-bottom: 6rpx;
}

.panel-title {
  font-size: 22rpx;
  font-weight: 700;
  color: #2c3e50;
}

.panel-body {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.layer-item {
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 10rpx 16rpx;
  background: rgba(248, 249, 250, 0.8);
  border-radius: 16rpx;
  border: 1px solid #e9ecef;
  transition: all 0.25s;
}

.layer-icon {
  font-size: 24rpx;
}

.layer-name {
  font-size: 22rpx;
  font-weight: 600;
  color: #495057;
}

.layer-item-active {
  background: linear-gradient(135deg, #2c3e50, #34495e) !important;
  border-color: #2c3e50 !important;
}

.layer-item-active .layer-name {
  color: #ffffff !important;
}

/* ========================================== */
/* 底部精致滑滑卡片 (Bottom Slider Details) */
/* ========================================== */

.detail-card {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  background: #ffffff;
  border-radius: 48rpx 48rpx 0 0;
  padding: 24rpx 40rpx 40rpx 40rpx;
  box-sizing: border-box;
  transform: translateY(100%);
  transition: transform 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
  box-shadow: 0 -15rpx 40rpx rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  max-height: 70vh;
  z-index: 1001;
}

.card-show {
  transform: translateY(0);
}

.card-drag-indicator {
  width: 80rpx;
  height: 8rpx;
  background-color: #e0e0e0;
  border-radius: 4rpx;
  align-self: center;
  margin-bottom: 20rpx;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24rpx;
}

.card-title-group {
  display: flex;
  flex-direction: column;
}

.card-badge {
  font-size: 20rpx;
  font-weight: 700;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
  align-self: flex-start;
  margin-bottom: 8rpx;
  text-transform: uppercase;
}

.badge-c {
  background-color: #fdf2e9;
  color: #d35400;
}

.badge-b {
  background-color: #ebf5fb;
  color: #2980b9;
}

.card-title {
  font-size: 40rpx;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.3;
}

.close-btn {
  font-size: 36rpx;
  color: #bbb;
  padding: 10rpx;
  line-height: 1;
}

.card-content {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 40rpx;
}

/* ========================================== */
/* C端样式设计：暖意浓浓的乡土历史相册 */
/* ========================================== */

.gallery-swiper {
  width: 100%;
  height: 380rpx;
  border-radius: 24rpx;
  overflow: hidden;
  margin-bottom: 30rpx;
  box-shadow: 0 8rpx 20rpx rgba(0, 0, 0, 0.05);
}

.gallery-img {
  width: 100%;
  height: 100%;
}

.village-meta-grid {
  display: flex;
  background-color: #fcfcfc;
  border-radius: 20rpx;
  padding: 24rpx 10rpx;
  margin-bottom: 30rpx;
  border: 1px solid #f0f0f0;
}

.meta-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  border-right: 1px solid #eee;
}

.meta-item:last-child {
  border-right: none;
}

.meta-label {
  font-size: 22rpx;
  color: #888;
  margin-bottom: 6rpx;
}

.meta-val {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
}

.highlight-amber {
  color: #d35400 !important;
}

.section-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #2c3e50;
  margin-bottom: 16rpx;
  margin-top: 10rpx;
}

.village-history-card {
  position: relative;
  background-color: #fef9e7;
  padding: 30rpx 40rpx;
  border-radius: 24rpx;
  border-left: 8rpx solid #f1c40f;
}

.quote-icon-left {
  position: absolute;
  top: 10rpx;
  left: 15rpx;
  font-size: 50rpx;
  color: #f39c12;
  opacity: 0.2;
}

.quote-icon-right {
  position: absolute;
  bottom: -20rpx;
  right: 15rpx;
  font-size: 50rpx;
  color: #f39c12;
  opacity: 0.2;
}

.history-desc {
  font-size: 28rpx;
  color: #5d4037;
  line-height: 1.7;
}

/* ========================================== */
/* B端样式设计：现代高阶商机撮合雷达 */
/* ========================================== */

.card-banner {
  width: 100%;
  height: 280rpx;
  border-radius: 20rpx;
  margin-bottom: 24rpx;
}

.project-info-box {
  background-color: #f8f9fa;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 32rpx;
}

.info-row {
  display: flex;
  margin-bottom: 12rpx;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-label {
  font-size: 26rpx;
  font-weight: 600;
  color: #555;
  width: 140rpx;
  flex-shrink: 0;
}

.info-value {
  font-size: 26rpx;
  color: #333;
  line-height: 1.5;
}

/* 智能匹配供应链区 */
.matching-section {
  border-top: 2px dashed #e8e8e8;
  padding-top: 30rpx;
}

.matching-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.matching-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #2c3e50;
}

.matching-count {
  font-size: 22rpx;
  color: #7f8c8d;
  background: #f2f4f4;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
}

.merchant-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.merchant-item-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 24rpx;
  padding: 24rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.02);
  transition: all 0.3s;
}

.merchant-vip {
  border: 1px solid #d4e6f1 !important;
  background-color: #fcfefe !important;
  box-shadow: 0 6rpx 16rpx rgba(41, 128, 185, 0.05);
}

.merchant-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.merchant-name-group {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.merchant-name {
  font-size: 30rpx;
  font-weight: 700;
  color: #2c3e50;
}

.vip-tag {
  font-size: 18rpx;
  font-weight: 700;
  color: #2980b9;
  background-color: #eaf2f8;
  padding: 2rpx 10rpx;
  border-radius: 6rpx;
}

.merchant-type {
  font-size: 22rpx;
  font-weight: 600;
  color: #7f8c8d;
  background: #f2f4f4;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
}

.merchant-body {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  margin-bottom: 16rpx;
}

.merchant-info-item {
  display: flex;
  font-size: 24rpx;
}

.mer-label {
  color: #7f8c8d;
  width: 130rpx;
}

.mer-val {
  color: #34495e;
}

.highlight-blue {
  color: #2980b9 !important;
  font-weight: 600;
}

.truncate-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.merchant-actions {
  width: 100%;
}

.call-btn {
  background: linear-gradient(135deg, #2980b9, #3498db);
  color: #ffffff;
  font-size: 24rpx;
  font-weight: 600;
  border-radius: 16rpx;
  border: none;
  padding: 12rpx 0;
  box-shadow: 0 4rpx 10rpx rgba(41, 128, 185, 0.2);
}

.call-btn:active {
  opacity: 0.9;
}

/* 暂无商户样式 */
.no-merchant-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 50rpx 30rpx;
  background-color: #fcfcfc;
  border-radius: 24rpx;
  border: 1px dashed #dcdde1;
}

.no-mer-icon {
  font-size: 60rpx;
  margin-bottom: 16rpx;
  opacity: 0.7;
}

.no-mer-txt {
  font-size: 24rpx;
  color: #7f8c8d;
  text-align: center;
  margin-bottom: 24rpx;
}

.join-btn {
  background-color: #ffffff;
  color: #2980b9;
  border: 1px solid #2980b9;
  font-size: 24rpx;
  font-weight: 600;
  border-radius: 16rpx;
  padding: 10rpx 30rpx;
}

.join-btn:active {
  background-color: #f4f6f7;
}

/* ========================================== */
/* C端评论区留言板专属样式 */
/* ========================================== */
.comment-section {
  border-top: 2px dashed #ebd1b9;
  padding-top: 30rpx;
  margin-top: 40rpx;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.comment-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #845c47;
}

.comment-count {
  font-size: 22rpx;
  color: #e67e22;
  background: #fdf2e9;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
}

/* 留言表单样式 */
.comment-form-box {
  background-color: #fdfcf7;
  border: 1px solid #ebd1b9;
  border-radius: 24rpx;
  padding: 20rpx;
  margin-bottom: 30rpx;
}

.comment-input-name {
  font-size: 26rpx;
  border-bottom: 1px solid #ebd1b9;
  padding: 10rpx 0;
  margin-bottom: 16rpx;
  color: #333;
}

.comment-input-content {
  font-size: 26rpx;
  min-height: 100rpx;
  width: 100%;
  color: #444;
  line-height: 1.5;
  margin-bottom: 16rpx;
}

.comment-submit-btn {
  background: linear-gradient(135deg, #e67e22, #d35400);
  color: #ffffff;
  font-size: 24rpx;
  font-weight: 600;
  border-radius: 16rpx;
  border: none;
  padding: 12rpx 0;
  box-shadow: 0 4rpx 10rpx rgba(230, 126, 34, 0.2);
}

.comment-submit-btn:active {
  opacity: 0.9;
}

/* 评论卡片样式 */
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.comment-card {
  background-color: #fcfcfc;
  border: 1px solid #f0f0f0;
  border-radius: 20rpx;
  padding: 20rpx;
}

.comment-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10rpx;
}

.comment-author {
  font-size: 24rpx;
  font-weight: 700;
  color: #845c47;
}

.comment-date {
  font-size: 20rpx;
  color: #999;
}

.comment-text {
  font-size: 26rpx;
  color: #4a4a4a;
  line-height: 1.6;
}

/* 暂无留言样式 */
.no-comment-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40rpx 0;
}

.no-comment-icon {
  font-size: 50rpx;
  margin-bottom: 10rpx;
  opacity: 0.6;
}

.no-comment-txt {
  font-size: 24rpx;
  color: #888;
  text-align: center;
}

/* ========================================== */
/* 地图脉冲发光定位打点 (Glowing Pulse Marker CSS) */
/* ========================================== */
.custom-leaflet-icon-container {
  background: transparent !important;
  border: none !important;
}

.premium-gis-marker {
  position: relative;
  width: 40px;
  height: 46px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.marker-svg {
  width: 100%;
  height: 100%;
  z-index: 10;
  transition: transform 0.25s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.premium-gis-marker:hover .marker-svg {
  transform: scale(1.15) translateY(-2px);
}

.pulse-wave-ring {
  position: absolute;
  bottom: 0px; /* 对齐在 teardrop 针尖处 */
  left: 20px;
  width: 32px;
  height: 32px;
  margin-left: -16px;
  margin-top: -16px;
  border-radius: 50%;
  animation: wave-ripple 2.5s infinite cubic-bezier(0.1, 0.8, 0.3, 1);
  opacity: 0;
  z-index: 1;
  pointer-events: none;
}

.pulse-wave-ring-delayed {
  position: absolute;
  bottom: 0px;
  left: 20px;
  width: 32px;
  height: 32px;
  margin-left: -16px;
  margin-top: -16px;
  border-radius: 50%;
  animation: wave-ripple 2.5s infinite cubic-bezier(0.1, 0.8, 0.3, 1);
  animation-delay: 1.25s;
  opacity: 0;
  z-index: 1;
  pointer-events: none;
}

.nostalgia-marker .pulse-wave-ring,
.nostalgia-marker .pulse-wave-ring-delayed {
  background-color: rgba(230, 126, 34, 0.3);
  border: 1px solid rgba(230, 126, 34, 0.5);
}

.construction-marker .pulse-wave-ring,
.construction-marker .pulse-wave-ring-delayed {
  background-color: rgba(41, 128, 185, 0.3);
  border: 1px solid rgba(41, 128, 185, 0.5);
}

@keyframes wave-ripple {
  0% {
    transform: scale(0.1) rotateX(75deg); /* 三维倾斜透视，使光圈贴合水平地面展开 */
    opacity: 0.95;
  }
  80% {
    opacity: 0.45;
  }
  100% {
    transform: scale(1.6) rotateX(75deg);
    opacity: 0;
  }
}

/* ========================================== */
/* AI 怀旧文案与拟态模态框样式 */
/* ========================================== */

.ai-marketing-box {
  margin-top: 24rpx;
  width: 100%;
}

.ai-gen-btn {
  background: linear-gradient(135deg, #8e44ad, #9b59b6);
  color: #ffffff;
  font-size: 26rpx;
  font-weight: 700;
  border-radius: 20rpx;
  border: none;
  padding: 16rpx 0;
  box-shadow: 0 6rpx 16rpx rgba(142, 68, 173, 0.25);
  transition: all 0.3s;
}

.ai-gen-btn:active {
  transform: translateY(2rpx);
  opacity: 0.95;
}

/* 拟态拟物理毛玻璃弹窗 */
.ai-modal-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-modal-card {
  width: 85%;
  max-height: 75vh;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  border-radius: 40rpx;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 20rpx 50rpx rgba(0, 0, 0, 0.15);
  padding: 30rpx 40rpx;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  animation: slideUpModal 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes slideUpModal {
  0% { transform: translateY(30%); opacity: 0; }
  100% { transform: translateY(0); opacity: 1; }
}

.ai-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  padding-bottom: 20rpx;
  margin-bottom: 24rpx;
}

.ai-modal-title {
  font-size: 32rpx;
  font-weight: 800;
  color: #2c3e50;
  background: linear-gradient(135deg, #8e44ad, #e67e22);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.ai-modal-close {
  font-size: 36rpx;
  color: #bbb;
  padding: 10rpx;
  line-height: 1;
}

.ai-modal-body {
  flex: 1;
  overflow-y: auto;
}

.ai-section {
  margin-bottom: 30rpx;
}

.ai-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}

.ai-section-title {
  font-size: 26rpx;
  font-weight: 700;
  color: #34495e;
}

.copy-small-btn {
  margin: 0;
  background-color: #fcfcfc;
  color: #8e44ad;
  border: 1px solid #ebd6f0;
  font-size: 20rpx;
  font-weight: 700;
  border-radius: 12rpx;
  padding: 4rpx 16rpx;
  line-height: 1.5;
}

.copy-small-btn:active {
  background-color: #f4ecf7;
}

.ai-text-box {
  background: rgba(248, 249, 250, 0.7);
  border: 1px solid rgba(0, 0, 0, 0.03);
  border-radius: 20rpx;
  padding: 20rpx;
  box-sizing: border-box;
}

.ai-text-content {
  font-size: 24rpx;
  color: #444;
  line-height: 1.6;
  white-space: pre-wrap;
}

/* ========================================== */
/* 新增表单与互动组件 CSS */
/* ========================================== */

.checkin-btn {
  background: linear-gradient(135deg, #e74c3c, #c0392b);
  color: #ffffff;
  font-size: 26rpx;
  font-weight: 700;
  border-radius: 20rpx;
  border: none;
  padding: 16rpx 0;
  box-shadow: 0 6rpx 16rpx rgba(231, 76, 60, 0.25);
  transition: all 0.3s;
}
.checkin-btn:active {
  transform: translateY(2rpx);
  opacity: 0.95;
}

.fab-btn {
  position: fixed;
  right: 30rpx;
  bottom: 250rpx;
  width: 100rpx;
  height: 100rpx;
  background: linear-gradient(135deg, #1abc9c, #16a085);
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 10rpx 30rpx rgba(26, 188, 156, 0.4);
  z-index: 1000;
  transition: all 0.3s;
}
.fab-btn:active {
  transform: scale(0.92);
}
.fab-icon {
  font-size: 44rpx;
  color: #fff;
}

.form-group {
  margin-bottom: 24rpx;
}
.form-label {
  display: block;
  font-size: 26rpx;
  font-weight: 700;
  color: #2c3e50;
  margin-bottom: 12rpx;
}
.form-input, .picker-box {
  width: 100%;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 16rpx;
  padding: 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}
.form-textarea {
  width: 100%;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 16rpx;
  padding: 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;
  height: 200rpx;
}
.picker-text {
  color: #34495e;
}
.p-4 {
  padding: 30rpx;
}
.mt-2 {
  margin-top: 16rpx;
}
.mt-4 {
  margin-top: 32rpx;
}

/* 准心 UI */
.map-crosshair {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 100;
  margin-top: -30rpx; /* 微调抵消 tab 栏的高度偏差 */
}
.crosshair-icon {
  color: #1abc9c;
  font-size: 60rpx;
  font-weight: 200;
  line-height: 1;
  text-shadow: 0 0 10rpx rgba(255,255,255,0.8);
}
.crosshair-text {
  font-size: 20rpx;
  color: #fff;
  background: rgba(26, 188, 156, 0.9);
  padding: 4rpx 12rpx;
  border-radius: 20rpx;
  margin-top: 4rpx;
  box-shadow: 0 4rpx 10rpx rgba(0,0,0,0.1);
}


/* 随手拍 - 图片上传样式 */
.upload-area {
  width: 100%;
  height: 160px;
  border: 2px dashed rgba(255,255,255,0.15);
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255,255,255,0.03);
  cursor: pointer;
}
.upload-preview {
  width: 100%;
  height: 100%;
}
.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.upload-icon {
  font-size: 36px;
}
.upload-text {
  font-size: 13px;
  color: rgba(255,255,255,0.4);
}</style>
