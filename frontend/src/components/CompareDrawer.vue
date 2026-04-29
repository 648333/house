<script setup>
import { computed } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  properties: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:visible', 'remove', 'clear', 'open'])

const rows = computed(() => [
  { label: '总价', field: 'price', suffix: '万' },
  { label: '户型', field: 'layout' },
  { label: '面积', field: 'area', suffix: '㎡' },
  { label: '朝向', field: 'orientation' },
  { label: '装修', field: 'decoration' },
  { label: '楼层', field: 'floor' },
  { label: '类型', field: 'type' },
  { label: '标签', field: 'tags' },
])

const close = () => emit('update:visible', false)
</script>

<template>
  <el-drawer
    :model-value="visible"
    title="房源对比"
    size="860px"
    @close="close"
    @update:model-value="emit('update:visible', $event)"
  >
    <div v-if="properties.length === 0" class="empty-wrap">
      <el-empty description="将感兴趣的房源加入对比，便于快速判断差异" />
    </div>

    <template v-else>
      <div class="drawer-toolbar">
        <span>当前已加入 {{ properties.length }} 套房源</span>
        <el-button text type="danger" @click="emit('clear')">清空对比</el-button>
      </div>

      <div class="compare-grid">
        <div class="label-column">
          <div class="header-cell">对比项</div>
          <div v-for="row in rows" :key="row.label" class="data-cell label-cell">
            {{ row.label }}
          </div>
        </div>

        <div v-for="property in properties" :key="property.id" class="property-column">
          <div class="header-cell property-head">
            <img :src="property.imageUrl" :alt="property.title" />
            <div class="head-copy">
              <div class="title">{{ property.title }}</div>
              <div class="actions">
                <el-button text type="primary" @click="emit('open', property.id)">查看详情</el-button>
                <el-button text type="danger" @click="emit('remove', property.id)">移除</el-button>
              </div>
            </div>
          </div>

          <div v-for="row in rows" :key="`${property.id}-${row.label}`" class="data-cell">
            {{ property[row.field] || '暂无信息' }}{{ property[row.field] ? row.suffix || '' : '' }}
          </div>
        </div>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped>
.empty-wrap {
  padding: 24px 0;
}

.drawer-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
  color: #677d90;
}

.compare-grid {
  display: grid;
  grid-template-columns: 140px repeat(auto-fit, minmax(210px, 1fr));
  gap: 14px;
  align-items: start;
}

.label-column,
.property-column {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.header-cell,
.data-cell {
  padding: 14px;
  border-radius: 20px;
  background: #f8fbff;
  color: #33495c;
}

.header-cell {
  min-height: 142px;
}

.label-cell {
  color: #8094a6;
  font-weight: 700;
}

.property-head {
  display: grid;
  grid-template-columns: 92px 1fr;
  gap: 12px;
}

.property-head img {
  width: 92px;
  height: 92px;
  object-fit: cover;
  border-radius: 16px;
}

.title {
  font-weight: 800;
  line-height: 1.5;
}

.actions {
  margin-top: 10px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

@media (max-width: 900px) {
  .compare-grid {
    grid-template-columns: 1fr;
  }

  .label-column {
    display: none;
  }
}
</style>
