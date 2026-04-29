<script setup>
import { computed, defineAsyncComponent, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, MagicStick, PictureRounded, VideoCamera } from '@element-plus/icons-vue'
import PropertyService from '@/api/property'
import FurniturePlanner from '@/components/FurniturePlanner.vue'
import { normalizeProperty } from '@/utils/property'

const PropertyModelViewer = defineAsyncComponent(() => import('@/components/PropertyModelViewer.vue'))

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const saving = ref(false)
const property = ref({})
const activeTab = ref(route.query.tab === 'model' ? 'model' : 'plan')

const currentUser = computed(() => JSON.parse(localStorage.getItem('user') || 'null'))
const canPersistFurnishingPlan = computed(() =>
  Boolean(currentUser.value?.id && property.value?.owner?.id && currentUser.value.id === property.value.owner.id),
)

const modelCredits = computed(() => {
  const url = String(property.value.model3dUrl || '')
  if (url.includes('house-interiors.glb')) {
    return {
      label: 'house interiors',
      creator: 'Gabriele Romagnoli',
      license: 'CC BY 3.0',
      source: 'https://poly.pizza/m/9UGOIbCy8C',
    }
  }
  if (url.includes('apartment-2.glb')) {
    return {
      label: 'Apartment 2',
      creator: 'Gabriele Romagnoli',
      license: 'CC BY 3.0',
      source: 'https://poly.pizza/m/dtgO5dwwtkk',
    }
  }
  if (url.includes('kenney-house.glb')) {
    return {
      label: 'House',
      creator: 'Kenney',
      license: 'CC0 1.0',
      source: 'https://poly.pizza/m/7VSVwAg2T3',
    }
  }
  return null
})

const fetchProperty = async () => {
  loading.value = true
  try {
    const response = await PropertyService.getProperty(route.params.id)
    property.value = normalizeProperty(response.data, response.data?.id)
  } catch (error) {
    ElMessage.error('获取房源信息失败')
    router.push('/properties')
  } finally {
    loading.value = false
  }
}

const saveFurnishingPlan = async (serializedPlan) => {
  if (!currentUser.value) {
    ElMessage.warning('登录后可保存软装方案')
    router.push('/login')
    return
  }

  if (!canPersistFurnishingPlan.value) {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(serializedPlan).catch(() => {})
    }
    ElMessage.info('当前账号不是房源发布者，可先预览方案后联系发布者保存')
    return
  }

  saving.value = true
  try {
    const payload = {
      ...property.value,
      furnishingPlan: serializedPlan,
      panoramaImages: JSON.stringify(property.value.panoramaImages || []),
    }
    const response = await PropertyService.updateProperty(property.value.id, payload)
    property.value = normalizeProperty(response.data, response.data?.id)
    ElMessage.success('软装方案已保存')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '软装方案保存失败')
  } finally {
    saving.value = false
  }
}

const setTab = (tab) => {
  activeTab.value = tab
  router.replace({
    path: route.path,
    query: tab === 'model' ? { tab: 'model' } : {},
  })
}

watch(
  () => route.query.tab,
  (tab) => {
    activeTab.value = tab === 'model' ? 'model' : 'plan'
  },
)

onMounted(fetchProperty)
</script>

<template>
  <div v-loading="loading" class="studio-page">
    <section class="studio-hero">
      <div class="hero-copy">
        <div class="hero-kicker">Furnishing Studio</div>
        <h1>{{ property.title || '房源软装工作室' }}</h1>
        <p>
          在平面图上规划家具布局，再切换到 3D 视角确认空间效果。
        </p>
        <div class="hero-meta">
          <span>{{ property.layout || '户型待补充' }}</span>
          <span>{{ property.area || '--' }}㎡</span>
          <span>{{ property.orientation || '朝向待补充' }}</span>
        </div>
      </div>

      <div class="hero-actions">
        <el-button plain round :icon="ArrowLeft" @click="router.push(`/property/${route.params.id}`)">返回详情页</el-button>
        <el-button round :icon="PictureRounded" @click="setTab('plan')">平面编辑</el-button>
        <el-button type="warning" round :icon="VideoCamera" @click="setTab('model')">3D 看房</el-button>
      </div>
    </section>

    <section class="studio-switcher">
      <button type="button" :class="['switch-pill', activeTab === 'plan' && 'active']" @click="setTab('plan')">
        平面摆场
      </button>
      <button type="button" :class="['switch-pill', activeTab === 'model' && 'active']" @click="setTab('model')">
        3D 看房
      </button>
    </section>

    <section v-if="activeTab === 'plan'" class="studio-panel">
      <div class="studio-tipbar">
        <div class="tip-item">
          <el-icon><MagicStick /></el-icon>
          <span>先在平面图里拖动家具确认布局，再切到 3D 看房做空间感校验。</span>
        </div>
        <div class="tip-item">
          <el-icon><PictureRounded /></el-icon>
          <span>支持自定义平面图，录入平面图 URL 后即可使用。</span>
        </div>
      </div>

      <FurniturePlanner
        :property="property"
        :can-persist="canPersistFurnishingPlan"
        :saving="saving"
        @save-plan="saveFurnishingPlan"
      />
    </section>

    <section v-else class="studio-panel">
      <div class="model-hero">
        <div>
          <div class="model-kicker">3D 看房</div>
          <h2>拖动旋转、缩放，像真正看房一样检查空间体量</h2>
          <p>支持真实 3D 模型，拖动旋转查看每个角落的空间细节。</p>
        </div>
        <div class="model-note">
          <span>鼠标拖动旋转</span>
          <span>滚轮缩放</span>
          <span>自动缓慢旋转</span>
        </div>
      </div>

      <PropertyModelViewer
        v-if="property.model3dUrl"
        :model-url="property.model3dUrl"
        :poster="property.imageUrl"
        :title="`${property.title || '房源'} 3D 看房`"
      />
      <el-empty v-else description="当前房源还没有 3D 模型资源" />

      <div v-if="modelCredits" class="credit-card">
        <strong>3D 模型素材信息</strong>
        <span>{{ modelCredits.label }} / {{ modelCredits.creator }} / {{ modelCredits.license }}</span>
        <a :href="modelCredits.source" target="_blank" rel="noreferrer">查看素材来源</a>
      </div>
    </section>
  </div>
</template>

<style scoped>
.studio-page {
  max-width: 1480px;
  margin: 0 auto;
  padding: 28px 24px 40px;
}

.studio-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  gap: 20px;
  padding: 30px 32px;
  border-radius: 34px;
  background:
    radial-gradient(circle at top right, rgba(235, 192, 132, 0.22), transparent 24%),
    linear-gradient(140deg, #fffaf3, #f5eadb);
  border: 1px solid rgba(177, 136, 96, 0.22);
  box-shadow: 0 26px 60px rgba(114, 80, 49, 0.12);
}

.hero-kicker,
.model-kicker {
  display: inline-flex;
  padding: 7px 14px;
  border-radius: 999px;
  background: rgba(196, 144, 91, 0.16);
  color: #9b6842;
  font-size: 12px;
  font-weight: 700;
}

.hero-copy h1 {
  margin: 16px 0 12px;
  font-size: 42px;
  line-height: 1.1;
  color: #4d3325;
}

.hero-copy p,
.model-hero p {
  margin: 0;
  color: #7a5c45;
  line-height: 1.8;
}

.hero-meta,
.model-note {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-meta span,
.model-note span {
  padding: 9px 14px;
  border-radius: 999px;
  background: rgba(183, 142, 103, 0.14);
  color: #6b4a34;
  font-size: 13px;
}

.hero-actions {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 12px;
}

.studio-switcher {
  margin: 20px 0 18px;
  display: inline-flex;
  gap: 10px;
  padding: 8px;
  border-radius: 999px;
  background: rgba(255, 251, 245, 0.96);
  border: 1px solid rgba(177, 136, 96, 0.18);
}

.switch-pill {
  border: none;
  padding: 12px 22px;
  border-radius: 999px;
  background: transparent;
  color: #7c5d45;
  font-weight: 700;
  cursor: pointer;
}

.switch-pill.active {
  background: linear-gradient(135deg, #b96e3e, #d19c61);
  color: #fffaf4;
}

.studio-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.studio-tipbar {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.tip-item,
.credit-card,
.model-hero {
  padding: 18px 20px;
  border-radius: 24px;
  background: rgba(255, 252, 247, 0.96);
  border: 1px solid rgba(180, 140, 100, 0.18);
  box-shadow: 0 18px 36px rgba(114, 80, 49, 0.08);
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #765742;
}

.model-hero {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
}

.model-hero h2 {
  margin: 14px 0 10px;
  color: #4f3424;
  font-size: 32px;
}

.credit-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #6e5039;
}

.credit-card a {
  color: #b06d3d;
  text-decoration: none;
  font-weight: 700;
}

@media (max-width: 980px) {
  .studio-hero,
  .studio-tipbar,
  .model-hero {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: stretch;
  }

  .hero-copy h1 {
    font-size: 34px;
  }
}
</style>
