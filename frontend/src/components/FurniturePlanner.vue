<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  CopyDocument,
  Delete,
  Download,
  Finished,
  House,
  Plus,
  RefreshRight,
  Setting,
} from '@element-plus/icons-vue'

const props = defineProps({
  property: {
    type: Object,
    required: true,
  },
  canPersist: {
    type: Boolean,
    default: false,
  },
  saving: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['save-plan'])

const FURNITURE_LIBRARY = [
  { type: 'sofa', label: '沙发', width: 22, height: 10, color: '#c58f68', shape: 'rounded' },
  { type: 'coffee', label: '茶几', width: 12, height: 7, color: '#9c7a5c', shape: 'rounded' },
  { type: 'tv', label: '电视柜', width: 20, height: 5, color: '#6f5b4a', shape: 'wide' },
  { type: 'dining', label: '餐桌', width: 18, height: 12, color: '#b1855d', shape: 'rounded' },
  { type: 'chair', label: '单椅', width: 8, height: 8, color: '#d6b08a', shape: 'circle' },
  { type: 'bed', label: '床', width: 24, height: 16, color: '#c7a186', shape: 'rounded' },
  { type: 'nightstand', label: '床头柜', width: 7, height: 7, color: '#8e6a4d', shape: 'square' },
  { type: 'desk', label: '书桌', width: 16, height: 8, color: '#b79672', shape: 'wide' },
  { type: 'wardrobe', label: '衣柜', width: 16, height: 8, color: '#8b7158', shape: 'wide' },
  { type: 'lamp', label: '落地灯', width: 6, height: 6, color: '#e7c47c', shape: 'circle' },
  { type: 'plant', label: '绿植', width: 7, height: 7, color: '#6e9b6a', shape: 'circle' },
  { type: 'rug', label: '地毯', width: 26, height: 14, color: '#d7ccb5', shape: 'rounded' },
]

const STYLE_TEMPLATES = {
  modern: {
    name: '现代简约',
    scene: 'living',
    items: [
      { type: 'rug', x: 45, y: 55, width: 34, height: 18, rotation: 0 },
      { type: 'sofa', x: 28, y: 52, width: 24, height: 11, rotation: 0 },
      { type: 'coffee', x: 45, y: 52, width: 12, height: 7, rotation: 0 },
      { type: 'tv', x: 70, y: 52, width: 22, height: 5, rotation: 0 },
      { type: 'plant', x: 78, y: 28, width: 7, height: 7, rotation: 0 },
      { type: 'lamp', x: 23, y: 30, width: 6, height: 6, rotation: 0 },
    ],
  },
  wood: {
    name: '原木风',
    scene: 'bedroom',
    items: [
      { type: 'bed', x: 52, y: 52, width: 26, height: 17, rotation: 0 },
      { type: 'nightstand', x: 35, y: 52, width: 7, height: 7, rotation: 0 },
      { type: 'nightstand', x: 69, y: 52, width: 7, height: 7, rotation: 0 },
      { type: 'wardrobe', x: 77, y: 26, width: 16, height: 8, rotation: 0 },
      { type: 'desk', x: 24, y: 30, width: 16, height: 8, rotation: 0 },
      { type: 'plant', x: 17, y: 72, width: 7, height: 7, rotation: 0 },
    ],
  },
  cream: {
    name: '奶油风',
    scene: 'living',
    items: [
      { type: 'rug', x: 48, y: 54, width: 36, height: 20, rotation: 0 },
      { type: 'sofa', x: 30, y: 54, width: 25, height: 12, rotation: 0 },
      { type: 'chair', x: 58, y: 38, width: 9, height: 9, rotation: 0 },
      { type: 'coffee', x: 47, y: 54, width: 11, height: 7, rotation: 0 },
      { type: 'lamp', x: 65, y: 26, width: 6, height: 6, rotation: 0 },
      { type: 'plant', x: 19, y: 34, width: 7, height: 7, rotation: 0 },
    ],
  },
  luxury: {
    name: '轻奢风',
    scene: 'dining',
    items: [
      { type: 'dining', x: 48, y: 50, width: 22, height: 14, rotation: 0 },
      { type: 'chair', x: 37, y: 50, width: 8, height: 8, rotation: 0 },
      { type: 'chair', x: 59, y: 50, width: 8, height: 8, rotation: 0 },
      { type: 'chair', x: 48, y: 35, width: 8, height: 8, rotation: 0 },
      { type: 'chair', x: 48, y: 65, width: 8, height: 8, rotation: 0 },
      { type: 'plant', x: 76, y: 26, width: 7, height: 7, rotation: 0 },
      { type: 'lamp', x: 21, y: 28, width: 6, height: 6, rotation: 0 },
    ],
  },
}

const roomModes = [
  { label: '客厅', value: 'living' },
  { label: '卧室', value: 'bedroom' },
  { label: '餐厅', value: 'dining' },
  { label: '书房', value: 'study' },
  { label: '全屋混搭', value: 'mixed' },
]

const FLOORPLAN_REGIONS = {
  family: [
    { key: 'bedroom', label: '主卧', x: 18, y: 10, width: 22, height: 32 },
    { key: 'living', label: '客厅', x: 18, y: 44, width: 36, height: 36 },
    { key: 'bedroom', label: '卧室', x: 71, y: 42, width: 20, height: 22 },
    { key: 'study', label: '书房', x: 74, y: 10, width: 18, height: 22 },
  ],
  compact: [
    { key: 'living', label: '客厅', x: 12, y: 10, width: 18, height: 25 },
    { key: 'dining', label: '餐厅', x: 38, y: 11, width: 14, height: 24 },
    { key: 'bedroom', label: '卧室', x: 70, y: 10, width: 21, height: 25 },
    { key: 'bedroom', label: '次卧', x: 26, y: 58, width: 18, height: 18 },
  ],
  loft: [
    { key: 'living', label: '挑空客厅', x: 16, y: 20, width: 26, height: 22 },
    { key: 'dining', label: '厨房', x: 40, y: 43, width: 12, height: 12 },
    { key: 'bedroom', label: '卧室', x: 69, y: 15, width: 18, height: 24 },
    { key: 'study', label: '工作区', x: 69, y: 58, width: 21, height: 14 },
  ],
}

const designCanvas = ref(null)
const selectedId = ref('')
const activeSource = ref('floorPlan')
const plan = ref(createDefaultPlan())
const interaction = ref(null)
const exportLoading = ref(false)

const SNAP_THRESHOLD = 2.4

function createDefaultPlan() {
  return {
    version: 1,
    source: 'floorPlan',
    style: 'modern',
    roomMode: 'living',
    items: [],
    updatedAt: new Date().toISOString(),
  }
}

function clonePlan(rawPlan) {
  return {
    version: 1,
    source: rawPlan?.source || 'floorPlan',
    style: rawPlan?.style || 'modern',
    roomMode: rawPlan?.roomMode || 'living',
    items: Array.isArray(rawPlan?.items) ? rawPlan.items.map((item) => ({ ...item })) : [],
    updatedAt: rawPlan?.updatedAt || new Date().toISOString(),
  }
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max)
}

function makeItem(definition, overrides = {}) {
  const base = FURNITURE_LIBRARY.find((item) => item.type === definition.type) || FURNITURE_LIBRARY[0]
  const uid = `${definition.type}-${Date.now()}-${Math.random().toString(16).slice(2, 6)}`
  return {
    id: overrides.id || uid,
    type: definition.type,
    label: base.label,
    color: base.color,
    shape: base.shape,
    x: clamp(overrides.x ?? 50, 4, 96),
    y: clamp(overrides.y ?? 50, 4, 96),
    width: clamp(overrides.width ?? definition.width ?? base.width, 5, 40),
    height: clamp(overrides.height ?? definition.height ?? base.height, 5, 35),
    rotation: overrides.rotation ?? 0,
    room: overrides.room || 'mixed',
  }
}

function snapValue(value, anchors, threshold = SNAP_THRESHOLD) {
  let snapped = value
  let minDistance = threshold

  anchors.forEach((anchor) => {
    const distance = Math.abs(value - anchor)
    if (distance <= minDistance) {
      minDistance = distance
      snapped = anchor
    }
  })

  return snapped
}

function applySnap(item) {
  if (!item) return

  const halfWidth = item.width / 2
  const halfHeight = item.height / 2
  const horizontalAnchors = [halfWidth, 50, 100 - halfWidth]
  const verticalAnchors = [halfHeight, 50, 100 - halfHeight]

  plan.value.items
    .filter((entry) => entry.id !== item.id)
    .forEach((entry) => {
      horizontalAnchors.push(entry.x)
      verticalAnchors.push(entry.y)
      horizontalAnchors.push(entry.x - (entry.width + item.width) / 2)
      horizontalAnchors.push(entry.x + (entry.width + item.width) / 2)
      verticalAnchors.push(entry.y - (entry.height + item.height) / 2)
      verticalAnchors.push(entry.y + (entry.height + item.height) / 2)
    })

  item.x = clamp(snapValue(item.x, horizontalAnchors), halfWidth, 100 - halfWidth)
  item.y = clamp(snapValue(item.y, verticalAnchors), halfHeight, 100 - halfHeight)
}

function normalizeItemBounds(item) {
  item.width = clamp(item.width, 5, 45)
  item.height = clamp(item.height, 5, 35)
  item.x = clamp(item.x, item.width / 2, 100 - item.width / 2)
  item.y = clamp(item.y, item.height / 2, 100 - item.height / 2)
}

const availableSources = computed(() => {
  const items = []
  if (props.property?.floorPlanUrl) {
    items.push({ value: 'floorPlan', label: '户型平面图', url: props.property.floorPlanUrl })
  }
  if (props.property?.imageUrl) {
    items.push({ value: 'cover', label: '效果图参考', url: props.property.imageUrl })
  }
  return items
})

const activeBackground = computed(() => {
  return availableSources.value.find((item) => item.value === activeSource.value) || availableSources.value[0] || null
})

const currentFloorplanKey = computed(() => {
  const url = String(activeBackground.value?.url || '').toLowerCase()
  if (url.includes('compact-b')) return 'compact'
  if (url.includes('loft-c')) return 'loft'
  if (url.includes('family-a')) return 'family'
  return 'family'
})

const activeRegions = computed(() => activeSource.value === 'floorPlan' ? FLOORPLAN_REGIONS[currentFloorplanKey.value] || [] : [])

const primaryRegion = computed(() => {
  if (plan.value.roomMode === 'mixed') return null
  return activeRegions.value.find((region) => region.key === plan.value.roomMode) || null
})

const visibleItems = computed(() => {
  if (plan.value.roomMode === 'mixed') {
    return plan.value.items
  }
  return plan.value.items.filter((item) => (item.room || 'mixed') === plan.value.roomMode)
})

const selectedItem = computed(() => visibleItems.value.find((item) => item.id === selectedId.value) || null)

const stageTransform = computed(() => {
  if (activeSource.value !== 'floorPlan' || plan.value.roomMode === 'mixed') {
    return 'scale(1) translate(0%, 0%)'
  }

  const focusRegion = primaryRegion.value
  if (!focusRegion) {
    return 'scale(1) translate(0%, 0%)'
  }

  const scale = 1.58
  const centerX = focusRegion.x + focusRegion.width / 2
  const centerY = focusRegion.y + focusRegion.height / 2
  const translateX = (50 - centerX) / scale
  const translateY = (50 - centerY) / scale
  return `scale(${scale}) translate(${translateX}%, ${translateY}%)`
})

const stageCaption = computed(() => {
  if (plan.value.roomMode === 'mixed') {
    return '当前显示全屋视图，可直接点平面图中的房间切换局部编辑。'
  }
  if (!primaryRegion.value) {
    return '当前房间没有热区定义，仍可继续编辑家具。'
  }
  return `当前聚焦 ${primaryRegion.value.label}，仅显示该区域家具。`
})

const planSummary = computed(() => {
  const counts = plan.value.items.reduce((acc, item) => {
    acc[item.label] = (acc[item.label] || 0) + 1
    return acc
  }, {})

  return Object.entries(counts).map(([label, count]) => `${label} x${count}`).join(' / ')
})

function syncFromProperty() {
  const initial = props.property?.furnishingPlan ? clonePlan(props.property.furnishingPlan) : createDefaultPlan()
  const preferredSource = availableSources.value.some((item) => item.value === 'floorPlan')
    ? 'floorPlan'
    : availableSources.value[0]?.value || 'cover'
  activeSource.value = availableSources.value.some((item) => item.value === initial.source) && initial.source !== 'cover'
    ? initial.source
    : preferredSource
  initial.source = activeSource.value
  plan.value = initial
  selectedId.value = visibleItems.value[0]?.id || ''
}

watch(
  () => [props.property?.id, props.property?.furnishingPlan, props.property?.floorPlanUrl, props.property?.imageUrl],
  syncFromProperty,
  { immediate: true },
)

function touchPlan() {
  plan.value.updatedAt = new Date().toISOString()
  plan.value.source = activeSource.value
}

function addFurniture(type) {
  const definition = FURNITURE_LIBRARY.find((item) => item.type === type)
  if (!definition) return
  const focusRegion = primaryRegion.value
  const item = makeItem(definition, {
    x: focusRegion ? focusRegion.x + focusRegion.width / 2 : 20 + (plan.value.items.length % 5) * 12,
    y: focusRegion ? focusRegion.y + focusRegion.height / 2 : 24 + (plan.value.items.length % 4) * 12,
    room: plan.value.roomMode === 'mixed' ? 'living' : plan.value.roomMode,
  })
  plan.value.items.push(item)
  selectedId.value = item.id
  touchPlan()
}

function applyStyle(styleKey) {
  const template = STYLE_TEMPLATES[styleKey]
  if (!template) return
  plan.value.style = styleKey
  plan.value.roomMode = template.scene
  plan.value.items = template.items.map((item) => makeItem(item, { ...item, room: template.scene }))
  selectedId.value = plan.value.items[0]?.id || ''
  touchPlan()
  ElMessage.success(`已生成 ${template.name} 初稿，可继续拖动微调`)
}

function resetPlan() {
  plan.value.items = []
  selectedId.value = ''
  touchPlan()
}

function duplicateSelected() {
  if (!selectedItem.value) return
  const duplicated = makeItem(selectedItem.value, {
    x: clamp(selectedItem.value.x + 6, 4, 96),
    y: clamp(selectedItem.value.y + 6, 4, 96),
    width: selectedItem.value.width,
    height: selectedItem.value.height,
    rotation: selectedItem.value.rotation,
    room: selectedItem.value.room,
  })
  plan.value.items.push(duplicated)
  selectedId.value = duplicated.id
  touchPlan()
}

function removeSelected() {
  if (!selectedItem.value) return
  plan.value.items = plan.value.items.filter((item) => item.id !== selectedItem.value.id)
  selectedId.value = plan.value.items[0]?.id || ''
  touchPlan()
}

function nudgeSelected(field, delta, min, max) {
  if (!selectedItem.value) return
  selectedItem.value[field] = clamp(Number(selectedItem.value[field]) + delta, min, max)
  applySnap(selectedItem.value)
  touchPlan()
}

function startInteraction(event, itemId, mode) {
  if (!designCanvas.value) return
  const item = plan.value.items.find((entry) => entry.id === itemId)
  if (!item) return
  const rect = designCanvas.value.getBoundingClientRect()
  selectedId.value = itemId
  const centerX = rect.left + (item.x / 100) * rect.width
  const centerY = rect.top + (item.y / 100) * rect.height

  interaction.value = {
    itemId,
    mode,
    rect,
    startMouseX: event.clientX,
    startMouseY: event.clientY,
    startX: item.x,
    startY: item.y,
    startWidth: item.width,
    startHeight: item.height,
    startRotation: item.rotation,
    startAngle: Math.atan2(event.clientY - centerY, event.clientX - centerX) * 180 / Math.PI,
  }
}

function handleWindowMove(event) {
  if (!interaction.value) return
  const item = plan.value.items.find((entry) => entry.id === interaction.value.itemId)
  if (!item) return

  const dxPct = ((event.clientX - interaction.value.startMouseX) / interaction.value.rect.width) * 100
  const dyPct = ((event.clientY - interaction.value.startMouseY) / interaction.value.rect.height) * 100

  if (interaction.value.mode === 'move') {
    item.x = clamp(interaction.value.startX + dxPct, item.width / 2, 100 - item.width / 2)
    item.y = clamp(interaction.value.startY + dyPct, item.height / 2, 100 - item.height / 2)
    applySnap(item)
  }

  if (interaction.value.mode === 'resize') {
    item.width = clamp(interaction.value.startWidth + dxPct, 5, 45)
    item.height = clamp(interaction.value.startHeight + dyPct, 5, 35)
    normalizeItemBounds(item)
    applySnap(item)
  }

  if (interaction.value.mode === 'rotate') {
    const centerX = interaction.value.rect.left + (item.x / 100) * interaction.value.rect.width
    const centerY = interaction.value.rect.top + (item.y / 100) * interaction.value.rect.height
    const currentAngle = Math.atan2(event.clientY - centerY, event.clientX - centerX) * 180 / Math.PI
    item.rotation = Math.round(interaction.value.startRotation + (currentAngle - interaction.value.startAngle))
  }

  touchPlan()
}

function stopInteraction() {
  interaction.value = null
}

function savePlan() {
  emit('save-plan', JSON.stringify(plan.value))
}

function focusRegion(regionKey) {
  plan.value.roomMode = regionKey
  const nextVisible = plan.value.items.filter((item) => (item.room || 'mixed') === regionKey)
  selectedId.value = nextVisible[0]?.id || ''
  touchPlan()
}

async function exportPlanImage() {
  if (!activeBackground.value?.url) {
    ElMessage.warning('当前没有可导出的底图')
    return
  }

  exportLoading.value = true
  try {
    const image = await loadImage(activeBackground.value.url)
    const width = image.naturalWidth || image.width || 1600
    const height = image.naturalHeight || image.height || 900
    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const ctx = canvas.getContext('2d')
    if (!ctx) throw new Error('canvas context unavailable')

    ctx.drawImage(image, 0, 0, width, height)
    plan.value.items.forEach((item) => drawFurniture(ctx, item, width, height))

    const link = document.createElement('a')
    const safeTitle = String(props.property?.title || 'furnishing-plan').replace(/[\\/:*?"<>|]/g, '-')
    link.download = `${safeTitle}-soft-furnishing.png`
    link.href = canvas.toDataURL('image/png')
    link.click()
    ElMessage.success('已导出当前方案图片')
  } catch (error) {
    ElMessage.error('导出失败，请确认底图资源允许跨域访问')
  } finally {
    exportLoading.value = false
  }
}

function loadImage(url) {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.crossOrigin = 'anonymous'
    image.onload = () => resolve(image)
    image.onerror = reject
    image.src = url
  })
}

function drawFurniture(ctx, item, canvasWidth, canvasHeight) {
  const centerX = (item.x / 100) * canvasWidth
  const centerY = (item.y / 100) * canvasHeight
  const drawWidth = (item.width / 100) * canvasWidth
  const drawHeight = (item.height / 100) * canvasHeight
  const radius = Math.min(drawWidth, drawHeight) * 0.18

  ctx.save()
  ctx.translate(centerX, centerY)
  ctx.rotate((item.rotation * Math.PI) / 180)
  ctx.fillStyle = item.color
  ctx.strokeStyle = 'rgba(255,255,255,0.94)'
  ctx.lineWidth = Math.max(2, Math.min(canvasWidth, canvasHeight) * 0.003)
  ctx.shadowColor = 'rgba(73, 47, 28, 0.18)'
  ctx.shadowBlur = 18
  ctx.shadowOffsetY = 10

  if (item.shape === 'circle') {
    ctx.beginPath()
    ctx.ellipse(0, 0, drawWidth / 2, drawHeight / 2, 0, 0, Math.PI * 2)
    ctx.fill()
    ctx.stroke()
  } else {
    roundRectPath(ctx, -drawWidth / 2, -drawHeight / 2, drawWidth, drawHeight, radius)
    ctx.fill()
    ctx.stroke()
  }

  ctx.shadowColor = 'transparent'
  ctx.fillStyle = 'rgba(255, 249, 244, 0.98)'
  ctx.font = `${Math.max(16, Math.min(drawWidth, drawHeight) * 0.24)}px sans-serif`
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(item.label, 0, 0, drawWidth * 0.82)
  ctx.restore()
}

function roundRectPath(ctx, x, y, width, height, radius) {
  const r = Math.min(radius, width / 2, height / 2)
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.arcTo(x + width, y, x + width, y + height, r)
  ctx.arcTo(x + width, y + height, x, y + height, r)
  ctx.arcTo(x, y + height, x, y, r)
  ctx.arcTo(x, y, x + width, y, r)
  ctx.closePath()
}

function itemStyle(item) {
  return {
    left: `${item.x}%`,
    top: `${item.y}%`,
    width: `${item.width}%`,
    height: `${item.height}%`,
    transform: `translate(-50%, -50%) rotate(${item.rotation}deg)`,
    background: item.color,
  }
}

function itemShapeClass(item) {
  return {
    'shape-circle': item.shape === 'circle',
    'shape-wide': item.shape === 'wide',
    'shape-rounded': item.shape === 'rounded',
    'is-selected': selectedId.value === item.id,
  }
}

watch(activeSource, touchPlan)
watch(
  () => plan.value.roomMode,
  () => {
    if (plan.value.roomMode === 'mixed') {
      selectedId.value = plan.value.items[0]?.id || ''
    } else if (selectedItem.value == null) {
      selectedId.value = visibleItems.value[0]?.id || ''
    }
    touchPlan()
  },
)
watch(
  () => selectedItem.value && [selectedItem.value.x, selectedItem.value.y, selectedItem.value.width, selectedItem.value.height],
  () => {
    if (!selectedItem.value) return
    normalizeItemBounds(selectedItem.value)
  },
)

onMounted(() => {
  window.addEventListener('mousemove', handleWindowMove)
  window.addEventListener('mouseup', stopInteraction)
})

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', handleWindowMove)
  window.removeEventListener('mouseup', stopInteraction)
})
</script>

<template>
  <div class="planner-shell">
    <section class="planner-toolbar">
      <div class="toolbar-main">
        <div class="toolbar-kicker">平面编辑工作台</div>
        <div class="toolbar-head">
          <h2>大画布摆场编辑</h2>
          <p>先选底图，再套风格，然后直接拖动家具。当前方案会自动吸附到边缘、中心线和附近家具。</p>
        </div>
      </div>

      <div class="toolbar-actions">
        <el-button plain :icon="RefreshRight" @click="resetPlan">清空方案</el-button>
        <el-button plain :icon="Download" :loading="exportLoading" @click="exportPlanImage">导出图片</el-button>
        <el-button type="primary" :icon="Finished" :loading="saving" @click="savePlan">
          {{ canPersist ? '保存方案' : '导出方案' }}
        </el-button>
      </div>
    </section>

    <section class="planner-controlbar">
      <div class="control-group">
        <div class="group-label">
          <el-icon><House /></el-icon>
          <span>编辑底图</span>
        </div>
        <el-radio-group v-model="activeSource">
          <el-radio-button v-for="source in availableSources" :key="source.value" :label="source.value">
            {{ source.label }}
          </el-radio-button>
        </el-radio-group>
      </div>

      <div class="control-group">
        <div class="group-label">
          <el-icon><Setting /></el-icon>
          <span>空间模式</span>
        </div>
        <el-radio-group v-model="plan.roomMode">
          <el-radio-button v-for="room in roomModes" :key="room.value" :label="room.value">
            {{ room.label }}
          </el-radio-button>
        </el-radio-group>
      </div>

      <div class="control-summary">
        <strong>{{ STYLE_TEMPLATES[plan.style]?.name || '自定义方案' }}</strong>
        <span>{{ planSummary || '还没有加入家具，可从下方素材区点击添加。' }}</span>
        <em>{{ stageCaption }}</em>
      </div>
    </section>

    <section class="planner-stage-card">
      <div v-if="activeBackground" ref="designCanvas" class="planner-stage">
        <div class="stage-viewport" :style="{ transform: stageTransform }">
          <img :src="activeBackground.url" class="stage-background" alt="编辑底图" />
          <div class="stage-mask"></div>
          <div
            v-if="primaryRegion"
            class="region-spotlight"
            :style="{
              left: `${primaryRegion.x}%`,
              top: `${primaryRegion.y}%`,
              width: `${primaryRegion.width}%`,
              height: `${primaryRegion.height}%`,
            }"
          ></div>

          <button
            v-for="region in activeRegions"
            :key="`${region.key}-${region.label}`"
            type="button"
            class="region-hotspot"
            :class="{ 'is-active': plan.roomMode === region.key }"
            :style="{
              left: `${region.x}%`,
              top: `${region.y}%`,
              width: `${region.width}%`,
              height: `${region.height}%`,
            }"
            @click.stop="focusRegion(region.key)"
          >
            <span>{{ region.label }}</span>
          </button>

          <button
            v-for="item in visibleItems"
            :key="item.id"
            type="button"
            class="furniture-item"
            :class="itemShapeClass(item)"
            :style="itemStyle(item)"
            @mousedown.left.stop="startInteraction($event, item.id, 'move')"
            @click.stop="selectedId = item.id"
          >
            <span class="item-label">{{ item.label }}</span>
            <span class="item-handle rotate" @mousedown.left.stop="startInteraction($event, item.id, 'rotate')"></span>
            <span class="item-handle resize" @mousedown.left.stop="startInteraction($event, item.id, 'resize')"></span>
          </button>
        </div>

        <div v-if="activeRegions.length" class="region-toolbar">
          <button
            v-for="region in activeRegions"
            :key="`pill-${region.key}-${region.label}`"
            type="button"
            class="region-pill"
            :class="{ 'is-active': plan.roomMode === region.key }"
            @click="focusRegion(region.key)"
          >
            {{ region.label }}
          </button>
          <button
            type="button"
            class="region-pill"
            :class="{ 'is-active': plan.roomMode === 'mixed' }"
            @click="focusRegion('mixed')"
          >
            全屋
          </button>
        </div>
      </div>
      <el-empty v-else description="当前房源还没有可编辑的平面图或效果图资源" />
    </section>

    <section class="planner-style-strip">
      <div class="section-title">
        <el-icon><Setting /></el-icon>
        <span>一键风格草案</span>
      </div>
      <div class="style-grid">
        <button
          v-for="(style, key) in STYLE_TEMPLATES"
          :key="key"
          class="style-card"
          type="button"
          @click="applyStyle(key)"
        >
          <strong>{{ style.name }}</strong>
          <span>{{ style.scene === 'bedroom' ? '卧室模板' : style.scene === 'dining' ? '餐厅模板' : '客厅模板' }}</span>
        </button>
      </div>
    </section>

    <section class="planner-bottom">
      <div class="bottom-panel">
        <div class="section-title">
          <el-icon><Plus /></el-icon>
          <span>常用家具</span>
        </div>
        <div class="library-grid">
          <button
            v-for="item in FURNITURE_LIBRARY"
            :key="item.type"
            class="library-item"
            type="button"
            @click="addFurniture(item.type)"
          >
            <span class="library-dot" :style="{ background: item.color }"></span>
            <strong>{{ item.label }}</strong>
          </button>
        </div>
      </div>

      <div class="bottom-panel">
        <div class="section-title">
          <el-icon><Setting /></el-icon>
          <span>{{ selectedItem ? '选中家具' : '方案说明' }}</span>
        </div>

        <template v-if="selectedItem">
          <div class="selected-head">
            <strong>{{ selectedItem.label }}</strong>
            <span>{{ selectedItem.type }}</span>
          </div>

          <el-form label-position="top">
            <el-form-item label="水平位置">
              <el-slider v-model="selectedItem.x" :min="selectedItem.width / 2" :max="100 - selectedItem.width / 2" @change="touchPlan" />
            </el-form-item>
            <el-form-item label="垂直位置">
              <el-slider v-model="selectedItem.y" :min="selectedItem.height / 2" :max="100 - selectedItem.height / 2" @change="touchPlan" />
            </el-form-item>
            <el-form-item label="宽度">
              <el-slider v-model="selectedItem.width" :min="5" :max="45" @change="touchPlan" />
            </el-form-item>
            <el-form-item label="高度">
              <el-slider v-model="selectedItem.height" :min="5" :max="35" @change="touchPlan" />
            </el-form-item>
            <el-form-item label="旋转角度">
              <el-slider v-model="selectedItem.rotation" :min="-180" :max="180" @change="touchPlan" />
            </el-form-item>
          </el-form>

          <div class="inspector-actions">
            <el-button plain @click="nudgeSelected('x', -1, selectedItem.width / 2, 100 - selectedItem.width / 2)">左移</el-button>
            <el-button plain @click="nudgeSelected('x', 1, selectedItem.width / 2, 100 - selectedItem.width / 2)">右移</el-button>
            <el-button plain @click="nudgeSelected('y', -1, selectedItem.height / 2, 100 - selectedItem.height / 2)">上移</el-button>
            <el-button plain @click="nudgeSelected('y', 1, selectedItem.height / 2, 100 - selectedItem.height / 2)">下移</el-button>
            <el-button plain :icon="CopyDocument" @click="duplicateSelected">复制</el-button>
            <el-button type="danger" plain :icon="Delete" @click="removeSelected">删除</el-button>
          </div>
        </template>

        <template v-else>
          <div class="plan-copy">
            <p>点击画布中的家具后，这里会出现精细调节区。当前工作室更适合先在平面图里定位置，再去 3D 看房里确认整体感觉。</p>
            <p>如果你想快速出方案，先点上面的一键风格，再做局部微调会更快。</p>
          </div>
        </template>
      </div>
    </section>
  </div>
</template>

<style scoped>
.planner-shell {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.planner-toolbar,
.planner-controlbar,
.planner-stage-card,
.planner-style-strip,
.bottom-panel {
  border-radius: 28px;
  border: 1px solid rgba(177, 136, 96, 0.18);
  background: rgba(255, 252, 247, 0.96);
  box-shadow: 0 18px 36px rgba(114, 80, 49, 0.08);
}

.planner-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
  padding: 24px 26px;
}

.toolbar-kicker {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(196, 144, 91, 0.16);
  color: #9b6842;
  font-size: 12px;
  font-weight: 700;
}

.toolbar-head h2 {
  margin: 14px 0 8px;
  color: #4f3424;
  font-size: 30px;
}

.toolbar-head p {
  margin: 0;
  color: #7b5d47;
  line-height: 1.7;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.planner-controlbar {
  display: grid;
  grid-template-columns: 1fr 1fr minmax(260px, 0.8fr);
  gap: 16px;
  padding: 18px 20px;
}

.control-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.group-label,
.section-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #654631;
  font-weight: 700;
}

.control-summary {
  padding: 14px 16px;
  border-radius: 20px;
  background: linear-gradient(135deg, #fff6eb, #f8efe4);
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
}

.control-summary strong {
  color: #5d3d29;
}

.control-summary span {
  color: #87654a;
  font-size: 13px;
  line-height: 1.6;
}

.control-summary em {
  color: #a17b59;
  font-size: 12px;
  font-style: normal;
  line-height: 1.5;
}

.planner-stage-card {
  padding: 18px;
}

.planner-stage {
  position: relative;
  min-height: 720px;
  border-radius: 24px;
  overflow: hidden;
  background: #f7efe4;
}

.stage-viewport {
  position: absolute;
  inset: 0;
  transform-origin: center center;
  transition: transform 0.35s ease;
}

.stage-background,
.stage-mask {
  position: absolute;
  inset: 0;
}

.stage-background {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #f7efe4;
}

.stage-mask {
  background:
    linear-gradient(0deg, rgba(57, 39, 28, 0.04), rgba(57, 39, 28, 0.04)),
    linear-gradient(90deg, rgba(255, 255, 255, 0.2) 1px, transparent 1px),
    linear-gradient(rgba(255, 255, 255, 0.2) 1px, transparent 1px);
  background-size: auto, 48px 48px, 48px 48px;
  pointer-events: none;
}

.region-spotlight {
  position: absolute;
  z-index: 1;
  border: 2px solid rgba(255, 220, 168, 0.92);
  background: rgba(255, 240, 218, 0.04);
  box-shadow: 0 0 0 9999px rgba(18, 22, 27, 0.34);
  pointer-events: none;
}

.region-hotspot {
  position: absolute;
  z-index: 1;
  border: 1px dashed rgba(255, 243, 230, 0.6);
  background: rgba(244, 203, 145, 0.08);
  color: #fff7ef;
  transform: translate(0, 0);
  transition: background 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.region-hotspot span {
  position: absolute;
  left: 10px;
  top: 10px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(24, 28, 34, 0.72);
  font-size: 12px;
  font-weight: 700;
}

.region-hotspot:hover,
.region-hotspot.is-active {
  border-color: rgba(255, 213, 151, 0.94);
  background: rgba(244, 203, 145, 0.16);
  box-shadow: inset 0 0 0 1px rgba(255, 220, 162, 0.4);
}

.region-toolbar {
  position: absolute;
  left: 20px;
  right: 20px;
  bottom: 20px;
  z-index: 3;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: center;
}

.region-pill {
  border: 1px solid rgba(255, 232, 205, 0.28);
  background: rgba(22, 28, 36, 0.68);
  color: #fff7ec;
  border-radius: 999px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 700;
  backdrop-filter: blur(10px);
  transition: transform 0.18s ease, background 0.18s ease, border-color 0.18s ease;
}

.region-pill:hover,
.region-pill.is-active {
  background: rgba(190, 125, 72, 0.92);
  border-color: rgba(255, 223, 189, 0.72);
  transform: translateY(-1px);
}

.furniture-item {
  position: absolute;
  z-index: 2;
  border: 2px solid rgba(255, 255, 255, 0.78);
  box-shadow: 0 14px 24px rgba(74, 48, 31, 0.18);
  color: #fffaf5;
  display: grid;
  place-items: center;
  cursor: move;
  user-select: none;
}

.shape-rounded {
  border-radius: 18px;
}

.shape-circle {
  border-radius: 999px;
}

.shape-wide {
  border-radius: 12px;
}

.is-selected {
  outline: 3px solid rgba(245, 204, 116, 0.86);
}

.item-label {
  padding: 0 8px;
  font-size: 13px;
  font-weight: 700;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.18);
}

.item-handle {
  position: absolute;
  width: 14px;
  height: 14px;
  border-radius: 999px;
  background: #fff;
  border: 2px solid #b87a4d;
}

.item-handle.rotate {
  right: -7px;
  top: -7px;
  cursor: alias;
}

.item-handle.resize {
  right: -7px;
  bottom: -7px;
  cursor: nwse-resize;
}

.planner-style-strip {
  padding: 18px 20px;
}

.style-grid,
.library-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.style-card,
.library-item {
  border: 1px solid rgba(182, 142, 104, 0.2);
  background: linear-gradient(145deg, #fffdf9, #f7efe4);
  border-radius: 18px;
  padding: 14px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.style-card:hover,
.library-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 20px rgba(132, 95, 63, 0.1);
}

.style-card strong,
.library-item strong {
  display: block;
  color: #583a28;
}

.style-card span {
  display: block;
  margin-top: 6px;
  color: #85644b;
  font-size: 13px;
}

.planner-bottom {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 18px;
}

.bottom-panel {
  padding: 18px 20px;
}

.library-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.library-dot {
  width: 14px;
  height: 14px;
  border-radius: 999px;
  flex: none;
}

.selected-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin: 14px 0 10px;
  color: #6b4a34;
}

.selected-head span {
  color: #8b6a50;
  font-size: 13px;
}

.inspector-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.plan-copy {
  margin-top: 14px;
  padding: 14px 16px;
  border-radius: 18px;
  background: linear-gradient(135deg, #fff7ed, #f7efe4);
  color: #7a5c45;
  line-height: 1.8;
}

.plan-copy p {
  margin: 0;
}

.plan-copy p + p {
  margin-top: 10px;
}

@media (max-width: 1100px) {
  .planner-controlbar,
  .planner-bottom {
    grid-template-columns: 1fr;
  }

  .style-grid,
  .library-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .planner-stage {
    min-height: 580px;
  }
}

@media (max-width: 760px) {
  .planner-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .planner-stage {
    min-height: 420px;
  }

  .style-grid,
  .library-grid,
  .inspector-actions {
    grid-template-columns: 1fr;
  }
}
</style>
