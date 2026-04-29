<template>
  <div class="map-view">
    <div class="header">
      <h2>地图找房</h2>
      <el-button @click="$router.push('/')">返回首页</el-button>
    </div>
    <MapSearch />
  </div>
</template>

<script setup>
import MapSearch from '@/components/MapSearch.vue'
</script>

<style scoped>
.map-view {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>
