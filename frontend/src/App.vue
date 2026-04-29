<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { Compass, Plus, UserFilled } from '@element-plus/icons-vue'
import AuthService from '@/api/auth'

const router = useRouter()
const route = useRoute()
const user = ref(null)

const isLoggedIn = computed(() => Boolean(user.value))
const currentPath = computed(() => route.path)

const syncUser = () => {
  const storedUser = localStorage.getItem('user')
  user.value = storedUser ? JSON.parse(storedUser) : null
}

const go = (path) => {
  if (path !== route.path) {
    router.push(path)
  }
}

const handleMenuSelect = (index) => {
  if (index === 'logout') {
    AuthService.logout()
    user.value = null
    router.push('/login')
    return
  }

  go(index)
}

onMounted(() => {
  syncUser()
  window.addEventListener('storage', syncUser)
})

onBeforeUnmount(() => {
  window.removeEventListener('storage', syncUser)
})
</script>

<template>
  <div class="app-shell">
    <div class="ambient-orb orb-a"></div>
    <div class="ambient-orb orb-b"></div>
    <div class="ambient-grid"></div>

    <el-container>
      <el-header class="header">
        <div class="header-content">
          <div class="logo" @click="go('/')">
            <div class="logo-badge">
              <el-icon class="logo-icon"><Compass /></el-icon>
            </div>
            <div>
              <div class="logo-title">暖寓找房</div>
            </div>
          </div>

          <el-menu
            :default-active="currentPath"
            class="nav-menu"
            mode="horizontal"
            :ellipsis="false"
            @select="handleMenuSelect"
          >
            <el-menu-item index="/">首页</el-menu-item>
            <el-menu-item index="/properties">精选房源</el-menu-item>
            <el-menu-item index="/map">地图找房</el-menu-item>
            <div class="nav-spacer" />

            <template v-if="!isLoggedIn">
              <el-menu-item index="/login">登录</el-menu-item>
              <el-menu-item index="/register">注册</el-menu-item>
            </template>

            <template v-else>
              <el-menu-item v-if="user?.role !== 'ADMIN'" index="/property/new">
                <el-icon><Plus /></el-icon>
                发布房源
              </el-menu-item>
              <el-sub-menu index="user-menu">
                <template #title>
                  <el-avatar :size="34" class="user-avatar">
                    <el-icon><UserFilled /></el-icon>
                  </el-avatar>
                  <span class="user-name">{{ user?.username }}</span>
                </template>
                <el-menu-item v-if="user?.role === 'ADMIN'" index="/admin">管理后台</el-menu-item>
                <el-menu-item v-if="user?.role === 'AGENT'" index="/agent">经纪人后台</el-menu-item>
                <el-menu-item v-if="user?.role !== 'ADMIN'" index="/profile">个人中心</el-menu-item>
                <el-menu-item index="logout">退出登录</el-menu-item>
              </el-sub-menu>
            </template>
          </el-menu>
        </div>
      </el-header>

      <el-main class="main-content">
        <RouterView @login-success="syncUser" />
      </el-main>

      <el-footer class="footer">
        <div class="footer-line">暖寓找房</div>
      </el-footer>
    </el-container>
  </div>
</template>

<style>
.app-shell {
  position: relative;
  min-height: 100vh;
  overflow: clip;
}

.ambient-orb {
  position: fixed;
  width: 34rem;
  height: 34rem;
  border-radius: 50%;
  filter: blur(34px);
  z-index: -2;
  opacity: 0.6;
  pointer-events: none;
}

.orb-a {
  left: -12rem;
  top: -8rem;
  background: radial-gradient(circle, rgba(235, 166, 98, 0.28), rgba(235, 166, 98, 0));
}

.orb-b {
  right: -10rem;
  bottom: -8rem;
  background: radial-gradient(circle, rgba(196, 128, 82, 0.26), rgba(196, 128, 82, 0));
}

.ambient-grid {
  position: fixed;
  inset: 0;
  z-index: -3;
  background-image:
    linear-gradient(rgba(149, 109, 72, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(149, 109, 72, 0.08) 1px, transparent 1px);
  background-size: 34px 34px;
  mask-image: radial-gradient(circle at 50% 40%, black 30%, transparent 75%);
  pointer-events: none;
}

.header {
  position: sticky;
  top: 0;
  z-index: 20;
  height: 84px;
  padding: 0;
  background: rgba(255, 246, 233, 0.86);
  backdrop-filter: blur(18px);
  border-bottom: 1px solid rgba(173, 129, 88, 0.22);
}

.header-content {
  height: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  margin-right: 28px;
}

.logo-badge {
  width: 52px;
  height: 52px;
  display: grid;
  place-items: center;
  border-radius: 16px;
  background: linear-gradient(145deg, rgba(189, 114, 63, 0.95), rgba(215, 161, 94, 0.95));
  box-shadow: 0 14px 26px rgba(153, 94, 53, 0.28);
}

.logo-icon {
  font-size: 24px;
  color: white;
}

.logo-title {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 0.05em;
  color: #5e3f2b;
}

.nav-menu {
  flex: 1;
  border-bottom: none !important;
  background: transparent !important;
}

.nav-menu.el-menu--horizontal > .el-menu-item,
.nav-menu.el-menu--horizontal > .el-sub-menu .el-sub-menu__title {
  height: 52px;
  border-radius: 999px;
  margin: 0 5px;
  color: #6a4b36;
  border-bottom: none !important;
}

.nav-menu.el-menu--horizontal > .el-menu-item:hover,
.nav-menu.el-menu--horizontal > .el-sub-menu .el-sub-menu__title:hover {
  color: #4d3424;
  background: rgba(211, 158, 104, 0.22);
}

.nav-menu.el-menu--horizontal > .el-menu-item.is-active {
  background: linear-gradient(135deg, rgba(182, 113, 63, 0.42), rgba(214, 162, 95, 0.38));
  color: #533726 !important;
}

.nav-spacer {
  flex: 1;
}

.user-avatar {
  margin-right: 8px;
  background: linear-gradient(135deg, #be7441, #d89f5c);
  color: white;
}

.user-name {
  font-weight: 700;
}

.main-content {
  min-height: calc(100vh - 156px);
  padding: 28px 22px 20px !important;
}

.footer {
  padding: 18px 0 28px !important;
  text-align: center;
  color: #9d795d;
}

.footer-line {
  font-size: 14px;
  font-weight: 700;
  color: #6b4b35;
}
</style>
