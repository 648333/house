<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  Compass,
  DataAnalysis,
  House,
  Key,
  MagicStick,
  MapLocation,
  Search,
  Star,
} from '@element-plus/icons-vue'
import PropertyService from '@/api/property'
import { getMyAppointments } from '@/api/appointment'
import { getMyRequirements } from '@/api/requirement'
import RequirementBoard from '@/components/RequirementBoard.vue'
import { getFavorites } from '@/utils/favorites'
import { normalizeProperty } from '@/utils/property'
import { getRecentlyViewedIds } from '@/utils/recentlyViewed'
import { buildPreferenceProfile, explainRecommendation } from '@/utils/recommendation'

const router = useRouter()
const properties = ref([])
const recommendedProperties = ref([])
const recentlyViewedProperties = ref([])
const matchedRequirements = ref([])
const loading = ref(false)
const recommendationLoading = ref(false)
const requirementLoading = ref(false)
const searchQuery = ref('')
const currentUser = computed(() => JSON.parse(localStorage.getItem('user') || 'null'))

const toolCards = [
  {
    title: '地图找房',
    description: '按板块、通勤和生活半径快速定位更合适的房源。',
    icon: MapLocation,
    action: () => router.push('/map'),
  },
  {
    title: '房贷测算',
    description: '浏览房源时同步估算月供、利息和预算压力。',
    icon: DataAnalysis,
    action: () => router.push('/properties'),
  },
  {
    title: '房源对比',
    description: '把候选房源放在同一视角里比较，决策更直接。',
    icon: Compass,
    action: () => router.push('/properties'),
  },
]

const fetchProperties = async () => {
  loading.value = true
  try {
    const response = await PropertyService.getAllProperties()
    const normalized = response.data
      .filter((item) => item.status === 'APPROVED')
      .map((item, index) => normalizeProperty(item, index))

    properties.value = normalized.slice(0, 8)

    const recentIds = getRecentlyViewedIds()
    recentlyViewedProperties.value = recentIds
      .map((id) => normalized.find((property) => property.id === id))
      .filter(Boolean)
      .slice(0, 4)
  } finally {
    loading.value = false
  }
}

const fetchRecommendations = async () => {
  if (!currentUser.value) {
    recommendedProperties.value = []
    return
  }

  recommendationLoading.value = true
  try {
    const [propertyResponse, recommendationResponse, appointmentResponse] = await Promise.all([
      PropertyService.getAllProperties(),
      PropertyService.getRecommendations(6, true),
      getMyAppointments(),
    ])

    const allProperties = propertyResponse.data.map((item, index) => normalizeProperty(item, index))
    const favoriteIds = getFavorites()
    const favorites = allProperties.filter((item) => favoriteIds.includes(item.id))

    const profile = buildPreferenceProfile({
      favorites,
      appointments: appointmentResponse.data,
      reviews: [],
    })

    recommendedProperties.value = recommendationResponse.data.map((item, index) => {
      const property = item.property || item
      const normalized = normalizeProperty(property, index)
      return {
        ...normalized,
        recommendationScore: item.score,
        recommendationReasons: item.reasons || [],
        reason: item.reasons?.length ? item.reasons.join(' / ') : explainRecommendation(normalized, profile),
      }
    })
  } catch (error) {
    recommendedProperties.value = []
  } finally {
    recommendationLoading.value = false
  }
}

const fetchRequirementMatches = async () => {
  if (!currentUser.value || currentUser.value.role !== 'USER') {
    matchedRequirements.value = []
    return
  }

  requirementLoading.value = true
  try {
    const response = await getMyRequirements()
    const requirementList = Array.isArray(response.data) ? response.data : []
    matchedRequirements.value = requirementList
      .filter((item) => (item.matchCount || 0) > 0)
      .slice(0, 3)
  } catch (error) {
    matchedRequirements.value = []
  } finally {
    requirementLoading.value = false
  }
}

const handleSearch = () => {
  router.push({ path: '/properties', query: { q: searchQuery.value } })
}

const openProperty = (id) => {
  router.push(`/property/${id}`)
}

onMounted(async () => {
  await Promise.all([fetchProperties(), fetchRecommendations(), fetchRequirementMatches()])
})
</script>

<template>
  <div class="home">
    <section class="hero-section">
      <div class="hero-copy">
        <span class="hero-kicker">高品质居住发现平台</span>
        <h1 class="hero-title">让选房过程更清晰，也更贴近真实生活</h1>
        <p class="hero-subtitle">
          从地图浏览、在线咨询到个性化推荐与需求匹配，
          暖寓找房帮你轻松找到理想住所。
        </p>

        <div class="search-container">
          <el-input
            v-model="searchQuery"
            placeholder="搜索板块、小区、地铁或你在意的生活方式"
            class="hero-search"
            size="large"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
            <template #append>
              <el-button type="primary" class="search-btn" @click="handleSearch">开始找房</el-button>
            </template>
          </el-input>

          <div class="search-tags">
            <span @click="searchQuery = '近地铁'">近地铁</span>
            <span @click="searchQuery = '学区房'">学区房</span>
            <span @click="searchQuery = '改善型'">改善型</span>
            <span @click="searchQuery = '江景'">江景</span>
          </div>
        </div>
      </div>

      <div class="hero-visual">
        <div class="visual-copy">
          <span class="visual-badge">精准推荐</span>
          <div class="visual-title">基于你的浏览与收藏偏好，智能推荐合适房源</div>
          <p>精准匹配你的找房需求，省时省力。</p>
        </div>

        <div class="floating-chip chip-one">近地铁</div>
        <div class="floating-chip chip-two">家庭友好</div>
        <div class="floating-chip chip-three">改善住宅</div>
      </div>
    </section>

    <section class="home-section quick-access-section">
      <div class="section-top">
        <div>
          <h2>快捷入口</h2>
          <p>快速找到你需要的服务。</p>
        </div>
      </div>

      <div class="icon-grid">
        <div class="icon-item" @click="router.push('/properties?type=二手房')">
          <div class="icon-circle bg-blue"><el-icon><House /></el-icon></div>
          <span>二手房</span>
        </div>
        <div class="icon-item" @click="router.push('/properties?type=租房')">
          <div class="icon-circle bg-green"><el-icon><Key /></el-icon></div>
          <span>租房</span>
        </div>
        <div class="icon-item" @click="router.push('/map')">
          <div class="icon-circle bg-indigo"><el-icon><MapLocation /></el-icon></div>
          <span>地图看房</span>
        </div>
        <div class="icon-item" @click="router.push('/profile')">
          <div class="icon-circle bg-coral"><el-icon><Star /></el-icon></div>
          <span>我的需求</span>
        </div>
      </div>
    </section>

    <section class="home-section tools-section">
      <div class="section-header">
        <div>
          <span class="section-kicker">置业工具</span>
          <h2 class="section-title">实用工具</h2>
        </div>
      </div>

      <div class="tools-grid">
        <div v-for="tool in toolCards" :key="tool.title" class="tool-card" @click="tool.action">
          <div class="tool-icon">
            <el-icon><component :is="tool.icon" /></el-icon>
          </div>
          <div class="tool-copy">
            <h3>{{ tool.title }}</h3>
            <p>{{ tool.description }}</p>
          </div>
          <el-icon class="tool-arrow"><ArrowRight /></el-icon>
        </div>
      </div>
    </section>

    <section v-if="currentUser" class="home-section requirements-section">
      <div class="section-header">
        <div>
          <span class="section-kicker">需求匹配</span>
          <h2 class="section-title">你的需求与匹配房源</h2>
          <p class="section-subtitle">发布找房需求后，系统自动为你匹配候选房源。</p>
        </div>
        <el-button link type="primary" @click="router.push('/profile')">
          进入需求中心
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>

      <div v-if="requirementLoading" class="loading-container soft-block">
        <el-skeleton :rows="3" animated />
      </div>

      <RequirementBoard
        v-else
        :items="matchedRequirements"
        :compact="true"
        :show-hot-list="true"
        empty-description="发布一个找房需求后，这里会自动整理对应的匹配结果。"
        @open-property="openProperty"
        @open-center="router.push('/profile')"
      />
    </section>

    <section v-if="currentUser" class="home-section recommendations-section">
      <div class="section-header">
        <div>
          <span class="section-kicker">个性化推荐</span>
          <h2 class="section-title">为你精选</h2>
          <p class="section-subtitle">每条推荐都附带推荐理由，帮你快速定位心仪房源。</p>
        </div>
      </div>

      <div v-if="recommendationLoading" class="loading-container soft-block">
        <el-skeleton :rows="4" animated />
      </div>

      <div v-else class="property-stream">
        <article
          v-for="property in recommendedProperties"
          :key="property.id"
          class="stream-property stream-property--featured"
          @click="openProperty(property.id)"
        >
          <div class="image-container">
            <img :src="property.imageUrl" class="image" loading="lazy" alt="推荐房源图片" />
            <div class="status-badge smart-badge">
              <el-icon><MagicStick /></el-icon>
              精准推荐
            </div>
          </div>

          <div class="card-content">
            <h3 class="title">{{ property.title }}</h3>
            <div v-if="property.recommendationScore" class="score-chip">AI score {{ property.recommendationScore }}</div>
            <div class="reason-chip">{{ property.reason }}</div>
            <div class="price-row">
              <div class="price-main">
                <span class="currency">￥</span>
                <span class="price">{{ property.price }}</span>
                <span class="price-unit">万</span>
              </div>
              <div class="unit-price">{{ property.layout || '舒适户型' }}</div>
            </div>
          </div>
        </article>
      </div>
    </section>

    <section v-if="recentlyViewedProperties.length" class="home-section recommendations-section">
      <div class="section-header">
        <div>
          <span class="section-kicker">继续浏览</span>
          <h2 class="section-title">最近看过的房源</h2>
          <p class="section-subtitle">快速回顾你之前浏览过的房源。</p>
        </div>
      </div>

      <div class="recent-grid">
        <article
          v-for="property in recentlyViewedProperties"
          :key="property.id"
          class="recent-card"
          @click="openProperty(property.id)"
        >
          <img :src="property.imageUrl" :alt="property.title" />
          <div>
            <h3>{{ property.title }}</h3>
            <p>{{ property.address }}</p>
          </div>
          <span>￥{{ property.price }} 万</span>
        </article>
      </div>
    </section>

    <section class="home-section recommendations-section">
      <div class="section-header">
        <div>
          <span class="section-kicker">精选房源</span>
          <h2 class="section-title">热门房源</h2>
          <p class="section-subtitle">精心挑选的优质房源，助你一步到位。</p>
        </div>
        <el-button link type="primary" @click="router.push('/properties')" class="view-more">
          查看更多
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>

      <div v-if="loading" class="loading-container soft-block">
        <el-skeleton :rows="5" animated />
      </div>

      <div v-else class="listing-grid">
        <article
          v-for="property in properties"
          :key="property.id"
          class="property-card"
          @click="openProperty(property.id)"
        >
          <div class="image-container image-container--compact">
            <img :src="property.imageUrl" class="image" loading="lazy" alt="房源图片" />
            <div class="status-badge">{{ property.status === 'SOLD' ? '已成交' : '可预约' }}</div>
          </div>

          <div class="card-content">
            <h3 class="title">{{ property.title }}</h3>
            <div class="info-row">
              <span>{{ property.layout || '优质户型' }}</span>
              <span>{{ property.area || '120' }} 平方米</span>
              <span>{{ property.orientation || '南向' }}</span>
            </div>
            <div class="price-row">
              <div class="price-main">
                <span class="currency">￥</span>
                <span class="price">{{ property.price }}</span>
                <span class="price-unit">万</span>
              </div>
              <div class="unit-price">
                {{ Math.floor(Number(property.price) * 10000 / (property.area || 100)) }} 元/㎡
              </div>
            </div>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home {
  max-width: 1240px;
  margin: 0 auto;
  padding-bottom: 44px;
}

.home-section {
  position: relative;
  margin-top: 34px;
  padding-top: 30px;
}

.home-section::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 1px;
  background: linear-gradient(90deg, rgba(181, 123, 78, 0), rgba(181, 123, 78, 0.24), rgba(181, 123, 78, 0));
}

.soft-block {
  border-radius: 28px;
  background: rgba(255, 251, 246, 0.72);
  border: 1px solid rgba(176, 129, 90, 0.14);
  box-shadow: 0 18px 42px rgba(118, 82, 54, 0.08);
  backdrop-filter: blur(16px);
}

.hero-section {
  display: grid;
  grid-template-columns: 1.08fr 0.92fr;
  gap: 26px;
  align-items: center;
  min-height: 520px;
  padding: 18px 0 6px;
}

.hero-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.hero-kicker,
.section-kicker {
  display: inline-flex;
  width: fit-content;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(103, 183, 255, 0.12);
  color: #4e90d0;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.hero-title {
  margin: 18px 0 14px;
  font-size: 54px;
  line-height: 1.06;
  letter-spacing: -0.04em;
  color: #243746;
}

.hero-subtitle {
  margin: 0 0 28px;
  max-width: 38rem;
  font-size: 17px;
  line-height: 1.9;
  color: #71879a;
}

.search-container {
  max-width: 620px;
}

.hero-search :deep(.el-input__wrapper) {
  min-height: 60px;
}

.hero-search :deep(.el-input-group__append) {
  padding: 0;
  border-radius: 18px;
  overflow: hidden;
}

.search-btn {
  height: 52px;
  padding: 0 24px;
  border-radius: 16px !important;
}

.search-tags {
  margin-top: 14px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.search-tags span {
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 251, 245, 0.82);
  color: #5d7488;
  border: 1px solid rgba(177, 128, 91, 0.1);
  cursor: pointer;
}

.hero-visual {
  position: relative;
  min-height: 420px;
  border-radius: 36px;
  background:
    radial-gradient(circle at top left, rgba(144, 209, 255, 0.34), rgba(144, 209, 255, 0) 42%),
    linear-gradient(145deg, rgba(238, 249, 255, 0.94) 0%, rgba(255, 247, 242, 0.98) 100%);
  overflow: hidden;
  box-shadow:
    inset 0 0 0 1px rgba(164, 126, 95, 0.12),
    0 26px 58px rgba(113, 73, 42, 0.1);
}

.hero-visual::before {
  content: '';
  position: absolute;
  inset: 18px;
  border-radius: 28px;
  border: 1px solid rgba(255, 255, 255, 0.42);
  pointer-events: none;
}

.visual-copy {
  position: absolute;
  right: 28px;
  bottom: 28px;
  width: min(340px, calc(100% - 56px));
  padding: 24px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 20px 40px rgba(102, 134, 173, 0.12);
  backdrop-filter: blur(14px);
}

.visual-badge {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 217, 226, 0.55);
  color: #d17090;
  font-size: 12px;
  font-weight: 700;
}

.visual-title {
  margin-top: 18px;
  font-size: 24px;
  font-weight: 800;
  line-height: 1.4;
  color: #29465d;
}

.visual-copy p {
  margin: 10px 0 0;
  color: #7a8f9f;
  line-height: 1.8;
}

.floating-chip {
  position: absolute;
  padding: 10px 16px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.76);
  color: #557085;
  box-shadow: 0 14px 30px rgba(112, 145, 186, 0.12);
}

.chip-one { top: 54px; left: 34px; }
.chip-two { top: 146px; right: 50px; }
.chip-three { left: 70px; bottom: 126px; }

.quick-access-section {
  padding-bottom: 4px;
}

.section-top h2,
.section-title {
  margin: 10px 0 8px;
  color: #253847;
}

.section-top p,
.section-subtitle {
  margin: 0;
  color: #7890a2;
}

.icon-grid {
  margin-top: 22px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.icon-item {
  padding: 18px 14px;
  display: grid;
  justify-items: center;
  gap: 12px;
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(255, 251, 245, 0.74), rgba(255, 247, 237, 0.38));
  border: 1px solid rgba(177, 128, 91, 0.12);
  cursor: pointer;
  transition: transform 0.25s ease, background 0.25s ease, border-color 0.25s ease;
}

.icon-item:hover,
.tool-card:hover,
.recent-card:hover,
.property-card:hover,
.stream-property:hover {
  transform: translateY(-4px);
}

.icon-item:hover {
  background: linear-gradient(180deg, rgba(255, 250, 242, 0.92), rgba(255, 243, 230, 0.54));
  border-color: rgba(180, 125, 86, 0.22);
}

.icon-circle {
  width: 66px;
  height: 66px;
  display: grid;
  place-items: center;
  border-radius: 22px;
  color: white;
  font-size: 28px;
}

.bg-blue { background: linear-gradient(135deg, #58b7ff, #81d0ff); }
.bg-green { background: linear-gradient(135deg, #7ecfba, #9fe3cd); }
.bg-indigo { background: linear-gradient(135deg, #7d98ff, #a5b4ff); }
.bg-coral { background: linear-gradient(135deg, #ff9d8c, #ffc09c); }

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 16px;
  margin-bottom: 22px;
}

.tools-grid {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.tool-card {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr) auto;
  align-items: center;
  gap: 18px;
  padding: 20px 0;
  cursor: pointer;
  border-bottom: 1px solid rgba(176, 129, 90, 0.12);
  transition: transform 0.25s ease;
}

.tool-card:last-child,
.recent-card:last-child,
.property-card:last-child {
  border-bottom: none;
}

.tool-icon {
  width: 58px;
  height: 58px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  background: linear-gradient(135deg, #ebf7ff, #fff3f7);
  color: #5690c7;
  font-size: 26px;
}

.tool-copy {
  min-width: 0;
}

.tool-card h3 {
  margin: 0 0 6px;
  color: #2a3f4e;
}

.tool-card p {
  margin: 0;
  color: #708595;
  line-height: 1.8;
}

.tool-arrow {
  color: #a36a47;
  font-size: 20px;
}

.loading-container {
  padding: 24px;
}

.property-stream {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 22px;
}

.stream-property {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
  cursor: pointer;
  transition: transform 0.25s ease;
}

.listing-grid,
.recent-grid {
  display: flex;
  flex-direction: column;
}

.property-card {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 18px;
  align-items: stretch;
  padding: 0 0 22px;
  cursor: pointer;
  border-bottom: 1px solid rgba(176, 129, 90, 0.14);
  transition: transform 0.25s ease;
}

.image-container {
  position: relative;
  height: 230px;
  border-radius: 28px;
  overflow: hidden;
}

.image-container--compact {
  height: 184px;
}

.image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.status-badge {
  position: absolute;
  top: 14px;
  left: 14px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.86);
  color: #476a83;
  font-size: 12px;
  font-weight: 700;
}

.smart-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #338f74;
}

.card-content {
  padding: 4px 0 0;
}

.title {
  margin: 0 0 12px;
  color: #2a3f4e;
  font-size: 18px;
  font-weight: 800;
}

.reason-chip {
  margin-bottom: 16px;
  padding: 10px 12px;
  border-radius: 16px;
  background: #f5fbf7;
  color: #4e846f;
  font-size: 13px;
  line-height: 1.6;
}

.score-chip {
  width: fit-content;
  margin-bottom: 10px;
  padding: 6px 10px;
  border-radius: 999px;
  background: #f5f9ff;
  color: #4e90d0;
  font-size: 12px;
  font-weight: 700;
}

.info-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 14px;
  color: #6d8395;
  font-size: 13px;
}

.info-row span {
  padding: 6px 10px;
  border-radius: 999px;
  background: #f6faff;
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 16px;
  border-top: 1px solid rgba(143, 177, 206, 0.14);
  padding-top: 14px;
}

.price-main {
  color: #f07fa1;
}

.currency,
.price-unit {
  font-size: 16px;
  font-weight: 700;
}

.price {
  font-size: 28px;
  font-weight: 900;
}

.unit-price {
  padding: 6px 12px;
  border-radius: 999px;
  background: #f5f9ff;
  color: #6b8092;
  font-size: 12px;
  white-space: nowrap;
}

.recent-card {
  padding: 18px 0;
  display: grid;
  grid-template-columns: 148px 1fr auto;
  gap: 16px;
  align-items: center;
  cursor: pointer;
  border-bottom: 1px solid rgba(176, 129, 90, 0.12);
  transition: transform 0.25s ease;
}

.recent-card img {
  width: 148px;
  height: 94px;
  object-fit: cover;
  border-radius: 18px;
}

.recent-card h3 {
  margin: 0 0 8px;
  color: #2a3f4e;
}

.recent-card p {
  margin: 0;
  color: #7a8f9f;
}

.recent-card span {
  color: #ef7a9b;
  font-weight: 800;
  white-space: nowrap;
}

@media (max-width: 1024px) {
  .hero-section {
    grid-template-columns: 1fr;
  }

  .property-stream {
    grid-template-columns: 1fr;
  }

  .icon-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .property-card {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .hero-title {
    font-size: 40px;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .icon-grid {
    grid-template-columns: 1fr;
  }

  .tool-card {
    grid-template-columns: 48px minmax(0, 1fr);
  }

  .tool-arrow {
    display: none;
  }

  .recent-card {
    grid-template-columns: 1fr;
  }

  .recent-card img {
    width: 100%;
    height: 180px;
  }

  .price-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
