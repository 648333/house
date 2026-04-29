<template>
  <div class="map-wrapper">
    <div class="map-tip">
      <div class="tip-title">通勤圈找房</div>
      <div class="tip-sub">可按通勤中心与通勤时长筛选房源，点击标记查看详情。</div>
      <div class="controls">
        <el-input-number v-model="commute.centerLat" :precision="4" :step="0.01" size="small" controls-position="right" />
        <el-input-number v-model="commute.centerLng" :precision="4" :step="0.01" size="small" controls-position="right" />
        <el-select v-model="commute.minutes" size="small" style="width: 120px">
          <el-option :value="20" label="20分钟" />
          <el-option :value="30" label="30分钟" />
          <el-option :value="45" label="45分钟" />
          <el-option :value="60" label="60分钟" />
        </el-select>
        <el-button size="small" type="primary" @click="applyCommuteFilter">应用筛选</el-button>
        <el-button size="small" plain @click="loadProperties">重置</el-button>
      </div>
    </div>
    <div id="map" class="map-box"></div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import PropertyService from '@/api/property'
import { normalizeProperty } from '@/utils/property'

import icon from 'leaflet/dist/images/marker-icon.png'
import iconShadow from 'leaflet/dist/images/marker-shadow.png'

const fallbackPoints = [
  [31.2304, 121.4737],
  [31.2197, 121.4451],
  [31.2051, 121.4683],
  [31.2429, 121.5012],
  [31.2145, 121.5208],
]

const router = useRouter()
const map = ref(null)
const markers = ref([])
const commute = ref({
  centerLat: 31.2304,
  centerLng: 121.4737,
  minutes: 30,
})

const defaultIcon = L.icon({
  iconUrl: icon,
  shadowUrl: iconShadow,
  iconSize: [25, 41],
  iconAnchor: [12, 41],
})

L.Marker.prototype.options.icon = defaultIcon

const clearMarkers = () => {
  markers.value.forEach((m) => m.remove())
  markers.value = []
}

const drawMarkers = (properties) => {
  clearMarkers()
  properties.forEach((property, index) => {
    const point =
      property.latitude && property.longitude
        ? [property.latitude, property.longitude]
        : fallbackPoints[index % fallbackPoints.length]

    const marker = L.marker(point)
      .addTo(map.value)
      .bindPopup(`
        <div style="text-align:center; min-width: 150px;">
          <img src="${property.imageUrl}" style="width: 140px; height: 86px; object-fit: cover; border-radius: 10px; margin-bottom: 8px;" />
          <div style="font-weight: 700; margin-bottom: 6px; color: #6b4a35;">${property.title}</div>
          <div style="color: #b86a3b; font-weight: bold;">¥${property.price}万</div>
          <button id="view-btn-${property.id}" style="margin-top: 8px; padding: 6px 12px; background: #b46a3a; color: white; border: none; border-radius: 999px; cursor: pointer;">查看详情</button>
        </div>
      `)

    marker.on('popupopen', () => {
      const button = document.getElementById(`view-btn-${property.id}`)
      if (button) {
        button.onclick = () => router.push(`/property/${property.id}`)
      }
    })

    markers.value.push(marker)
  })
}

const loadProperties = async () => {
  const response = await PropertyService.getAllProperties()
  const properties = response.data.map((item, index) => normalizeProperty(item, index))
  drawMarkers(properties)
}

const applyCommuteFilter = async () => {
  const response = await PropertyService.getCommuteFilteredProperties({
    centerLat: commute.value.centerLat,
    centerLng: commute.value.centerLng,
    minutes: commute.value.minutes,
  })
  const properties = response.data.map((item, index) => normalizeProperty(item, index))
  drawMarkers(properties)
  if (properties.length) {
    const first = properties[0]
    map.value.setView([first.latitude || commute.value.centerLat, first.longitude || commute.value.centerLng], 12)
  }
}

onMounted(async () => {
  map.value = L.map('map').setView([31.2304, 121.4737], 12)

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors',
  }).addTo(map.value)

  try {
    await loadProperties()
  } catch (error) {
    console.error('Failed to load properties for map:', error)
  }
})
</script>

<style scoped>
.map-wrapper {
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 10px 26px rgba(176, 115, 63, 0.12);
  background: white;
}

.map-tip {
  padding: 14px 18px;
  background: linear-gradient(135deg, #fff5e8, #fffaf2);
}

.tip-title {
  color: #714a32;
  font-weight: 700;
}

.tip-sub {
  color: #8a6b53;
  margin-top: 4px;
  font-size: 14px;
}

.controls {
  margin-top: 10px;
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.map-box {
  height: 560px;
  width: 100%;
}
</style>
