<script setup>
import { computed } from 'vue'
import { ArrowRight, Clock, User } from '@element-plus/icons-vue'

const props = defineProps({
  items: {
    type: Array,
    default: () => [],
  },
  emptyTitle: {
    type: String,
    default: '需求匹配区已准备好',
  },
  emptyDescription: {
    type: String,
    default: '发布找房需求后，这里会自动生成匹配卡片与候选房源。',
  },
  compact: {
    type: Boolean,
    default: false,
  },
  showCloseAction: {
    type: Boolean,
    default: false,
  },
  showBookAction: {
    type: Boolean,
    default: false,
  },
  showOpenAction: {
    type: Boolean,
    default: true,
  },
  showHotList: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['open-property', 'book-property', 'close-requirement', 'open-center'])

const hasMeaningfulContent = (item) => {
  if (!item) return false
  return Boolean(
    item.title ||
    item.preferredArea ||
    item.propertyType ||
    item.layoutPreference ||
    item.note ||
    item.minBudget ||
    item.maxBudget ||
    item.matchCount ||
    item.matchedProperties?.length
  )
}

const visibleItems = computed(() => props.items.filter(hasMeaningfulContent))

const hotItems = computed(() =>
  [...visibleItems.value]
    .sort((left, right) => (right.matchCount || 0) - (left.matchCount || 0))
    .slice(0, 3)
)

const formatBudget = (item) => {
  if (!item?.minBudget && !item?.maxBudget) return '预算待沟通'
  return `${item.minBudget || 0}-${item.maxBudget || 0} 万`
}

const statusText = (status) => {
  if (status === 'OPEN') return '待匹配'
  if (status === 'MATCHED') return '已匹配'
  if (status === 'FOLLOWING') return '跟进中'
  if (status === 'CLOSED') return '已结束'
  return '进行中'
}
</script>

<template>
  <div v-if="visibleItems.length" class="board-layout">
    <div class="board-grid">
      <article
        v-for="item in visibleItems"
        :key="item.id"
        :class="['requirement-card', { compact }]"
      >
        <header class="requirement-top">
          <div class="title-wrap">
            <span class="area-chip">{{ item.preferredArea || '找房需求' }}</span>
            <h3>{{ item.title || '待补充需求主题' }}</h3>
            <p class="meta-line">
              <span><el-icon><Clock /></el-icon>{{ statusText(item.status) }}</span>
              <span v-if="item.assignedAgent"><el-icon><User /></el-icon>{{ item.assignedAgent.username }}</span>
            </p>
          </div>
          <div class="match-chip">{{ item.matchCount || 0 }} 套匹配</div>
        </header>

        <div class="bubble-group">
          <span v-if="item.propertyType" class="bubble">{{ item.propertyType }}</span>
          <span v-if="item.layoutPreference" class="bubble">{{ item.layoutPreference }}</span>
          <span class="bubble">{{ formatBudget(item) }}</span>
        </div>

        <p v-if="item.note" class="requirement-note">{{ item.note }}</p>

        <div v-if="item.matchedProperties?.length" class="match-list">
          <div
            v-for="property in item.matchedProperties.slice(0, compact ? 2 : 3)"
            :key="property.id"
            class="match-item"
          >
            <img :src="property.imageUrl" :alt="property.title" />
            <div class="match-copy">
              <strong>{{ property.title }}</strong>
              <span>{{ property.price }} 万 · {{ property.area }} 平</span>
              <div class="match-actions">
                <el-button
                  v-if="showOpenAction"
                  link
                  type="primary"
                  @click="emit('open-property', property.id)"
                >
                  查看详情
                </el-button>
                <el-button
                  v-if="showBookAction"
                  link
                  type="success"
                  @click="emit('book-property', item, property)"
                >
                  预约带看
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="empty-match">
          <span>匹配结果整理中，稍后会优先展示更贴近条件的房源。</span>
        </div>

        <footer v-if="showCloseAction || $slots.footer" class="card-footer">
          <slot name="footer" :item="item" />
          <el-button
            v-if="showCloseAction && item.status !== 'CLOSED'"
            plain
            @click="emit('close-requirement', item.id)"
          >
            结束需求
          </el-button>
        </footer>
      </article>
    </div>

    <aside v-if="showHotList && hotItems.length" class="hot-list">
      <div class="hot-head">
        <span class="area-chip">热门需求</span>
        <h3>当前更容易形成匹配的方向</h3>
      </div>
      <div
        v-for="item in hotItems"
        :key="`hot-${item.id}`"
        class="hot-item"
      >
        <div>
          <strong>{{ item.title || '需求主题待完善' }}</strong>
          <p>{{ item.preferredArea || '区域待定' }} · {{ item.layoutPreference || '户型待定' }}</p>
        </div>
        <span>{{ item.matchCount || 0 }} 套</span>
      </div>
    </aside>
  </div>

  <div v-else class="empty-panel">
    <div>
      <h3>{{ emptyTitle }}</h3>
      <p>{{ emptyDescription }}</p>
    </div>
    <el-button type="primary" @click="emit('open-center')">
      进入需求中心
      <el-icon><ArrowRight /></el-icon>
    </el-button>
  </div>
</template>

<style scoped>
.board-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 18px;
}

.board-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 18px;
}

.requirement-card,
.hot-list,
.empty-panel {
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(140, 176, 208, 0.16);
  box-shadow: 0 20px 44px rgba(108, 137, 170, 0.12);
  backdrop-filter: blur(16px);
}

.requirement-card {
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.requirement-card.compact {
  min-height: 100%;
}

.requirement-top {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.title-wrap h3,
.hot-head h3,
.empty-panel h3 {
  margin: 10px 0 6px;
  color: #30475a;
}

.meta-line {
  margin: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #7e91a2;
  font-size: 13px;
}

.meta-line span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.area-chip,
.match-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.area-chip {
  background: rgba(79, 195, 247, 0.12);
  color: #458fc2;
}

.match-chip {
  background: linear-gradient(135deg, #edf8ff, #fff2f6);
  color: #4a8fc3;
  white-space: nowrap;
}

.bubble-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.bubble {
  padding: 8px 12px;
  border-radius: 999px;
  background: #f3f8fd;
  color: #698095;
  font-size: 13px;
}

.requirement-note {
  margin: 0;
  color: #7d90a0;
  line-height: 1.6;
}

.match-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.match-item {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  padding: 10px;
  border-radius: 20px;
  background: #f8fbff;
}

.match-item img {
  width: 88px;
  height: 72px;
  object-fit: cover;
  border-radius: 16px;
}

.match-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.match-copy strong {
  color: #334a5c;
}

.match-copy span,
.hot-item p,
.empty-panel p {
  color: #8094a6;
}

.match-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.empty-match {
  padding: 14px 16px;
  border-radius: 18px;
  background: #f6f9fc;
  color: #8598aa;
  font-size: 13px;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.hot-list {
  padding: 22px;
}

.hot-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid rgba(126, 151, 173, 0.12);
}

.hot-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.hot-item strong {
  color: #32495b;
}

.hot-item p {
  margin: 6px 0 0;
  font-size: 13px;
}

.hot-item > span {
  color: #4e8dbf;
  font-weight: 700;
}

.empty-panel {
  padding: 28px;
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
}

@media (max-width: 1080px) {
  .board-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .requirement-card,
  .hot-list,
  .empty-panel {
    border-radius: 24px;
  }

  .empty-panel {
    flex-direction: column;
    align-items: flex-start;
  }

  .match-item {
    grid-template-columns: 1fr;
  }

  .match-item img {
    width: 100%;
    height: 160px;
  }
}
</style>
