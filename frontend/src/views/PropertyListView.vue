<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Location, MapLocation, Search, Sort, Switch, DataAnalysis } from '@element-plus/icons-vue'
import PropertyService from '@/api/property'
import MapSearch from '@/components/MapSearch.vue'
import CompareDrawer from '@/components/CompareDrawer.vue'
import { normalizeProperty } from '@/utils/property'
import {
  clearCompare,
  getComparedIds,
  getCompareLimit,
  removeFromCompare,
  toggleCompare,
} from '@/utils/compare'
import { getFavorites } from '@/utils/favorites'

const router = useRouter()
const route = useRoute()
const properties = ref([])
const loading = ref(false)
const mapMode = ref(false)
const compareVisible = ref(false)
const searchQuery = ref(String(route.query?.q || ''))
const filterType = ref(String(route.query?.type || ''))
const sortOrder = ref('default')
const selectedTag = ref('')
const onlyFavorites = ref(false)
const compareIds = ref(getComparedIds())
const favoriteIdsState = ref(getFavorites())

const presetTags = ['近地铁', '学区房', '江景', '治愈风', '宠物友好', '高端社区']

const fetchProperties = async () => {
  loading.value = true
  try {
    const response = searchQuery.value
      ? await PropertyService.searchProperties(searchQuery.value)
      : await PropertyService.getAllProperties()

    properties.value = response.data
      .filter((item) => item.status !== 'PENDING')
      .map((item, index) => normalizeProperty(item, index))
  } catch (error) {
    console.error('Error fetching properties:', error)
    ElMessage.error('房源列表加载失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  router.replace({
    path: '/properties',
    query: {
      ...(searchQuery.value ? { q: searchQuery.value } : {}),
      ...(filterType.value ? { type: filterType.value } : {}),
    },
  })
}

const filteredProperties = computed(() => {
  let result = [...properties.value]

  if (filterType.value) {
    result = result.filter((item) => (item.type || '').includes(filterType.value))
  }

  if (selectedTag.value) {
    result = result.filter((item) => String(item.tags || '').includes(selectedTag.value))
  }

  if (onlyFavorites.value) {
    result = result.filter((item) => favoriteIdsState.value.includes(item.id))
  }

  if (sortOrder.value === 'price_asc') {
    result.sort((a, b) => Number(a.price) - Number(b.price))
  } else if (sortOrder.value === 'price_desc') {
    result.sort((a, b) => Number(b.price) - Number(a.price))
  } else if (sortOrder.value === 'newest') {
    result.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
  } else if (sortOrder.value === 'area_desc') {
    result.sort((a, b) => Number(b.area) - Number(a.area))
  }

  return result
})

const comparedProperties = computed(() =>
  properties.value.filter((property) => compareIds.value.includes(property.id))
)

const goToDetail = (id) => {
  router.push(`/property/${id}`)
}

const handleCompare = (propertyId) => {
  const result = toggleCompare(propertyId)

  if (result.reason === 'limit') {
    ElMessage.warning(`最多可同时对比 ${getCompareLimit()} 套房源`)
    return
  }

  if (result.reason === 'added') {
    ElMessage.success('已加入房源对比')
  } else if (result.reason === 'removed') {
    ElMessage.info('已从对比列表移除')
  }

  compareIds.value = getComparedIds()
}

const handleRemoveCompare = (propertyId) => {
  removeFromCompare(propertyId)
  compareIds.value = getComparedIds()
}

const handleClearCompare = () => {
  clearCompare()
  compareIds.value = []
}

watch(
  () => route.query,
  () => {
    searchQuery.value = String(route.query?.q || '')
    filterType.value = String(route.query?.type || '')
    fetchProperties()
  }
)

onMounted(fetchProperties)
</script>

<template>
  <div class="property-list-container">
    <section class="filter-bar">
      <div class="filter-copy">
        <span class="page-kicker">房源浏览</span>
        <h1>全部房源</h1>
        <p>搜索、筛选和对比，轻松找到理想住所。</p>
      </div>

      <div class="search-row">
        <el-input
          v-model="searchQuery"
          placeholder="搜索板块、小区、地铁或关键词"
          class="list-search-input"
          @keyup.enter="handleSearch"
          clearable
          @clear="handleSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button plain :icon="MapLocation" @click="mapMode = !mapMode">
          {{ mapMode ? '返回列表' : '地图模式' }}
        </el-button>
      </div>

      <div class="filter-row">
        <el-select
          v-model="filterType"
          placeholder="房源类型"
          class="filter-select"
          clearable
          size="large"
          @change="handleSearch"
        >
          <el-option label="二手房" value="二手房" />
          <el-option label="租房" value="租房" />
          <el-option label="新房" value="新房" />
        </el-select>

        <el-dropdown trigger="click" @command="(command) => (sortOrder = command)">
          <el-button class="filter-sort-btn" size="large">
            排序方式
            <el-icon class="el-icon--right"><Sort /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="default">综合排序</el-dropdown-item>
              <el-dropdown-item command="newest">最新发布</el-dropdown-item>
              <el-dropdown-item command="price_asc">价格从低到高</el-dropdown-item>
              <el-dropdown-item command="price_desc">价格从高到低</el-dropdown-item>
              <el-dropdown-item command="area_desc">面积优先</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <el-switch
          v-model="onlyFavorites"
          inline-prompt
          active-text="收藏"
          inactive-text="全部"
        />

        <div class="result-count">共 {{ filteredProperties.length }} 套房源</div>
      </div>

      <div class="tag-row">
        <span
          v-for="tag in presetTags"
          :key="tag"
          :class="['tag-chip', { active: selectedTag === tag }]"
          @click="selectedTag = selectedTag === tag ? '' : tag"
        >
          {{ tag }}
        </span>
      </div>
    </section>

    <div v-if="mapMode" class="map-shell">
      <MapSearch />
    </div>

    <div v-else>
      <div v-if="loading" class="loading-container surface-panel">
        <el-skeleton :rows="5" animated />
      </div>

      <div v-else-if="filteredProperties.length === 0" class="empty-state surface-panel">
        <el-empty description="暂时没有符合条件的房源" />
      </div>

      <div v-else class="property-list">
        <el-card
          v-for="property in filteredProperties"
          :key="property.id"
          class="list-card"
          shadow="hover"
          :body-style="{ padding: '0px', display: 'flex' }"
          @click="goToDetail(property.id)"
        >
          <div class="list-image-container">
            <img :src="property.imageUrl" class="list-image" loading="lazy" alt="房源图片" />
            <div class="list-tag">{{ property.status === 'SOLD' ? '已成交' : '可预约' }}</div>
          </div>

          <div class="list-content">
            <div class="list-header">
              <div>
                <h3 class="list-title">{{ property.title }}</h3>
                <div class="list-address">
                  <el-icon><Location /></el-icon>
                  {{ property.address }}
                </div>
              </div>

              <div class="list-price-block">
                <span class="currency">¥</span>
                <span class="amount">{{ property.price }}</span>
                <span class="amount-unit">万</span>
              </div>
            </div>

            <div class="list-info-row">
              <div class="info-group">
                <span class="info-value">{{ property.layout || '舒适户型' }}</span>
                <span class="info-label">户型</span>
              </div>
              <div class="info-group">
                <span class="info-value">{{ property.area || '120' }} 平方米</span>
                <span class="info-label">面积</span>
              </div>
              <div class="info-group">
                <span class="info-value">{{ property.orientation || '南向' }}</span>
                <span class="info-label">朝向</span>
              </div>
              <div class="info-group">
                <span class="info-value">{{ property.yearBuilt || '近年' }}</span>
                <span class="info-label">建成年代</span>
              </div>
            </div>

            <div class="list-footer">
              <div class="list-tags" v-if="property.tags">
                <el-tag
                  v-for="tag in property.tags.split(',').slice(0, 3)"
                  :key="tag"
                  size="small"
                  type="info"
                  effect="light"
                  class="custom-tag"
                >
                  {{ tag }}
                </el-tag>
              </div>

              <div class="footer-actions" @click.stop>
                <span class="unit-price">
                  {{ Math.floor(Number(property.price) * 10000 / (property.area || 100)) }} 元/㎡
                </span>
                <el-button
                  plain
                  :icon="Switch"
                  :type="compareIds.includes(property.id) ? 'primary' : 'default'"
                  @click="handleCompare(property.id)"
                >
                  {{ compareIds.includes(property.id) ? '已加入对比' : '加入对比' }}
                </el-button>
                <el-button plain :icon="DataAnalysis" @click="goToDetail(property.id)">查看测算</el-button>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <div v-if="comparedProperties.length" class="compare-bar premium-float">
      <div class="compare-copy">
        <strong>房源对比</strong>
        <span>已选择 {{ comparedProperties.length }}/{{ getCompareLimit() }} 套候选房源</span>
      </div>
      <div class="compare-actions">
        <el-button text @click="handleClearCompare">清空</el-button>
        <el-button type="primary" @click="compareVisible = true">查看对比</el-button>
      </div>
    </div>

    <CompareDrawer
      v-model:visible="compareVisible"
      :properties="comparedProperties"
      @remove="handleRemoveCompare"
      @clear="handleClearCompare"
      @open="goToDetail"
    />
  </div>
</template>

<style scoped>
.property-list-container {
  max-width: 1180px;
  margin: 0 auto;
  padding-bottom: 112px;
}

.surface-panel,
.filter-bar {
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(143, 177, 206, 0.16);
  box-shadow: 0 22px 58px rgba(103, 130, 166, 0.14);
  backdrop-filter: blur(18px);
}

.filter-bar {
  position: sticky;
  top: 92px;
  z-index: 10;
  padding: 26px;
  border-radius: 32px;
  margin-bottom: 26px;
}

.page-kicker {
  display: inline-flex;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(103, 183, 255, 0.12);
  color: #4e90d0;
  font-size: 13px;
  font-weight: 700;
}

.filter-copy h1 {
  margin: 14px 0 8px;
  font-size: 34px;
  color: #243847;
}

.filter-copy p {
  margin: 0 0 20px;
  color: #7890a2;
}

.search-row,
.filter-row {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.search-row {
  margin-bottom: 14px;
}

.list-search-input {
  flex: 1;
  min-width: 240px;
}

.filter-select {
  width: 160px;
}

.result-count {
  margin-left: auto;
  padding: 8px 14px;
  border-radius: 999px;
  background: #f4f8ff;
  color: #688196;
  font-size: 13px;
}

.tag-row {
  margin-top: 14px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.tag-chip {
  padding: 8px 14px;
  border-radius: 999px;
  background: #f5f8fd;
  color: #617c92;
  cursor: pointer;
  transition: all 0.24s ease;
}

.tag-chip.active {
  background: linear-gradient(135deg, #e8f5ff, #fff1f6);
  color: #4d86b9;
}

.map-shell {
  overflow: hidden;
  border-radius: 30px;
}

.loading-container,
.empty-state {
  padding: 30px;
  border-radius: 30px;
}

.property-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.list-card {
  overflow: hidden;
  border-radius: 30px;
  cursor: pointer;
  transition: transform 0.24s ease, box-shadow 0.24s ease;
}

.list-card:hover {
  transform: translateY(-3px);
}

.list-image-container {
  width: 290px;
  min-height: 210px;
  margin: 18px;
  border-radius: 24px;
  overflow: hidden;
  position: relative;
  flex-shrink: 0;
}

.list-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.list-tag {
  position: absolute;
  top: 14px;
  left: 14px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  color: #48677f;
  font-size: 12px;
  font-weight: 700;
}

.list-content {
  flex: 1;
  padding: 24px 24px 24px 4px;
  display: flex;
  flex-direction: column;
}

.list-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
}

.list-title {
  margin: 0;
  font-size: 24px;
  color: #273c4c;
}

.list-price-block {
  color: #ee7c9f;
  font-weight: 900;
  white-space: nowrap;
}

.currency,
.amount-unit {
  font-size: 16px;
}

.amount {
  font-size: 34px;
}

.list-address {
  margin-top: 10px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #74889b;
}

.list-info-row {
  margin-top: 20px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.info-group {
  padding: 14px;
  border-radius: 18px;
  background: #f7fbff;
}

.info-value {
  display: block;
  color: #294050;
  font-size: 15px;
  font-weight: 800;
}

.info-label {
  display: block;
  margin-top: 6px;
  color: #8a9cac;
  font-size: 12px;
}

.list-footer {
  margin-top: auto;
  padding-top: 18px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  border-top: 1px solid rgba(143, 177, 206, 0.14);
}

.list-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.custom-tag {
  border: none;
  background: #eef6ff;
  color: #5685b4;
}

.footer-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.unit-price {
  padding: 8px 14px;
  border-radius: 999px;
  background: #f6f8fc;
  color: #71869a;
  font-size: 12px;
}

.premium-float {
  position: fixed;
  left: 50%;
  bottom: 20px;
  transform: translateX(-50%);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  min-width: min(720px, calc(100% - 24px));
  padding: 16px 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(143, 177, 206, 0.14);
  box-shadow: 0 16px 36px rgba(103, 132, 166, 0.16);
  backdrop-filter: blur(14px);
}

.compare-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #667f93;
}

.compare-actions {
  display: flex;
  gap: 8px;
}

@media (max-width: 900px) {
  .list-card :deep(.el-card__body) {
    flex-direction: column !important;
  }

  .list-image-container {
    width: calc(100% - 36px);
    height: 220px;
  }

  .list-content {
    padding: 0 22px 22px;
  }

  .list-info-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .premium-float {
    min-width: calc(100% - 24px);
  }
}

@media (max-width: 640px) {
  .filter-bar {
    padding: 20px;
  }

  .filter-copy h1 {
    font-size: 28px;
  }

  .list-info-row {
    grid-template-columns: 1fr;
  }

  .premium-float,
  .list-footer {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
