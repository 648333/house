<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { FullScreen, RefreshRight, VideoCamera } from '@element-plus/icons-vue'
import '@google/model-viewer'

const props = defineProps({
  modelUrl: {
    type: String,
    required: true,
  },
  poster: {
    type: String,
    default: '',
  },
  title: {
    type: String,
    default: '3D 看房',
  },
  compact: {
    type: Boolean,
    default: false,
  },
})

const modelViewerRef = ref(null)
const loading = ref(true)
const progress = ref(0)
const hasError = ref(false)
const isFullscreen = ref(false)
const reloadKey = ref(0)

const shellClass = computed(() => ({
  'is-compact': props.compact,
  'is-fullscreen': isFullscreen.value,
}))

const progressText = computed(() => `${Math.round(progress.value * 100)}%`)

const hotspotList = computed(() => [
  { slot: 'hotspot-1', label: '客厅主视角', position: '0m 1.4m 1.8m', normal: '0m 0m 1m' },
  { slot: 'hotspot-2', label: '餐厨动线', position: '-1.4m 1.2m 0.2m', normal: '1m 0m 0m' },
  { slot: 'hotspot-3', label: '采光面', position: '1.5m 1.7m -0.8m', normal: '-1m 0m 0m' },
])

const resetState = () => {
  loading.value = true
  progress.value = 0
  hasError.value = false
  reloadKey.value += 1
}

const handleLoad = () => {
  loading.value = false
  progress.value = 1
  hasError.value = false
}

const handleProgress = (event) => {
  const value = Number(event?.detail?.totalProgress || 0)
  progress.value = Number.isFinite(value) ? value : 0
}

const handleError = () => {
  loading.value = false
  hasError.value = true
}

const resetCamera = () => {
  const viewer = modelViewerRef.value
  if (!viewer) return
  viewer.cameraOrbit = '35deg 72deg 105%'
  viewer.fieldOfView = '30deg'
  viewer.jumpCameraToGoal()
}

const toggleFullscreen = async () => {
  const shell = modelViewerRef.value?.closest('.model-viewer-shell')
  if (!shell) return

  try {
    if (!document.fullscreenElement) {
      await shell.requestFullscreen()
      isFullscreen.value = true
    } else {
      await document.exitFullscreen()
      isFullscreen.value = false
    }
  } catch (error) {
    ElMessage.info('当前环境暂不支持全屏预览')
  }
}

const openModelInNewTab = () => {
  if (!props.modelUrl) return
  window.open(props.modelUrl, '_blank', 'noopener,noreferrer')
}

watch(
  () => props.modelUrl,
  () => {
    resetState()
  },
)

onMounted(() => {
  document.addEventListener('fullscreenchange', () => {
    isFullscreen.value = Boolean(document.fullscreenElement)
  })
})
</script>

<template>
  <div class="model-viewer-shell" :class="shellClass">
    <div class="viewer-toolbar">
      <div>
        <div class="viewer-kicker">Real 3D Viewer</div>
        <strong>{{ title }}</strong>
      </div>

      <div class="viewer-actions">
        <el-button plain :icon="RefreshRight" @click="resetCamera">重置视角</el-button>
        <el-button plain :icon="FullScreen" @click="toggleFullscreen">全屏</el-button>
        <el-button type="primary" plain :icon="VideoCamera" @click="openModelInNewTab">打开模型文件</el-button>
      </div>
    </div>

    <div class="viewer-stage">
      <div v-if="loading" class="viewer-overlay">
        <div class="loading-card">
          <div class="loading-title">3D 模型加载中</div>
          <el-progress :percentage="Math.round(progress * 100)" :stroke-width="10" />
          <span>当前进度 {{ progressText }}</span>
        </div>
      </div>

      <div v-else-if="hasError" class="viewer-overlay">
        <div class="loading-card">
          <div class="loading-title">3D 模型加载失败</div>
          <span>请检查模型路径、格式或浏览器 WebGL 支持。</span>
          <div class="error-actions">
            <el-button type="primary" @click="resetState">重新加载</el-button>
            <el-button plain @click="openModelInNewTab">查看模型地址</el-button>
          </div>
        </div>
      </div>

      <model-viewer
        :key="reloadKey"
        ref="modelViewerRef"
        class="model-viewer"
        :src="modelUrl"
        :poster="poster || undefined"
        :alt="title"
        camera-controls
        auto-rotate
        ar
        ar-modes="webxr scene-viewer quick-look"
        shadow-intensity="1.1"
        shadow-softness="0.9"
        exposure="1.15"
        environment-image="neutral"
        interaction-prompt="auto"
        touch-action="pan-y"
        camera-orbit="35deg 72deg 105%"
        min-camera-orbit="auto auto 65%"
        max-camera-orbit="auto auto 180%"
        field-of-view="30deg"
        @load="handleLoad"
        @progress="handleProgress"
        @error="handleError"
      >
        <button
          v-for="hotspot in hotspotList"
          :key="hotspot.slot"
          :slot="hotspot.slot"
          class="hotspot"
          :data-position="hotspot.position"
          :data-normal="hotspot.normal"
        >
          {{ hotspot.label }}
        </button>

        <button slot="ar-button" class="ar-entry">
          手机 AR 看房
        </button>
      </model-viewer>
    </div>

    <div class="viewer-footnote">
      <span>支持拖拽旋转、滚轮缩放、双指缩放和手机 AR 预览。</span>
      <span>如果当前房源没有专属模型，系统会自动使用户型匹配的默认 `glb` 模型。</span>
    </div>
  </div>
</template>

<style scoped>
.model-viewer-shell {
  border-radius: 28px;
  overflow: hidden;
  border: 1px solid rgba(177, 136, 96, 0.18);
  background:
    radial-gradient(circle at top right, rgba(235, 192, 132, 0.18), transparent 24%),
    linear-gradient(160deg, #fff9f2, #efe1cf);
  box-shadow: 0 20px 40px rgba(114, 80, 49, 0.1);
}

.model-viewer-shell.is-fullscreen {
  border-radius: 0;
}

.model-viewer-shell.is-compact .viewer-stage,
.model-viewer-shell.is-compact .model-viewer {
  min-height: 420px;
  height: 420px;
}

.viewer-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 18px 20px;
  border-bottom: 1px solid rgba(177, 136, 96, 0.14);
  background: rgba(255, 252, 247, 0.94);
}

.viewer-kicker {
  display: inline-flex;
  margin-bottom: 8px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(196, 144, 91, 0.16);
  color: #9b6842;
  font-size: 12px;
  font-weight: 700;
}

.viewer-toolbar strong {
  color: #5a3a27;
}

.viewer-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.viewer-stage {
  position: relative;
  min-height: 720px;
  height: 720px;
}

.model-viewer {
  width: 100%;
  height: 100%;
  background:
    radial-gradient(circle at top right, rgba(235, 192, 132, 0.14), transparent 20%),
    linear-gradient(160deg, #fff9f2, #efe1cf);
}

.viewer-overlay {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: grid;
  place-items: center;
  background: rgba(250, 242, 231, 0.74);
  backdrop-filter: blur(8px);
}

.loading-card {
  min-width: min(420px, calc(100% - 32px));
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  border-radius: 24px;
  background: rgba(255, 252, 247, 0.96);
  border: 1px solid rgba(177, 136, 96, 0.18);
  box-shadow: 0 20px 36px rgba(114, 80, 49, 0.12);
  color: #6b4a34;
}

.loading-title {
  font-size: 18px;
  font-weight: 800;
  color: #5a3926;
}

.error-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.hotspot {
  border: none;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(26, 25, 21, 0.72);
  color: #fffaf5;
  font-size: 12px;
  font-weight: 700;
  backdrop-filter: blur(8px);
}

.ar-entry {
  position: absolute;
  right: 18px;
  bottom: 18px;
  border: none;
  padding: 12px 16px;
  border-radius: 999px;
  background: linear-gradient(135deg, #b96e3e, #d19c61);
  color: #fffaf4;
  font-weight: 700;
  box-shadow: 0 14px 24px rgba(114, 80, 49, 0.18);
}

.viewer-footnote {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  padding: 14px 20px 18px;
  color: #7c5d45;
  font-size: 13px;
  background: rgba(255, 252, 247, 0.9);
}

@media (max-width: 760px) {
  .viewer-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .viewer-stage,
  .model-viewer {
    min-height: 440px;
    height: 440px;
  }

  .viewer-footnote {
    flex-direction: column;
  }
}
</style>
