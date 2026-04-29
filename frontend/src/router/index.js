import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/HomeView.vue')
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue')
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/RegisterView.vue')
    },
    {
      path: '/forgot-password',
      name: 'forgot-password',
      component: () => import('../views/ForgotPasswordView.vue')
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('../views/AdminDashboard.vue'),
      meta: { requiresAuth: true, role: 'ADMIN' }
    },
    {
      path: '/agent',
      name: 'agent',
      component: () => import('../views/AgentDashboard.vue'),
      meta: { requiresAuth: true, role: 'AGENT' }
    },
    {
      path: '/properties',
      name: 'properties',
      component: () => import('../views/PropertyListView.vue')
    },
    {
      path: '/map',
      name: 'map',
      component: () => import('../views/MapView.vue')
    },
    {
      path: '/property/new',
      name: 'property-create',
      component: () => import('../views/PropertyFormView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/property/:id/edit',
      name: 'property-edit',
      component: () => import('../views/PropertyFormView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/property/:id',
      name: 'property-detail',
      component: () => import('../views/PropertyDetailView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/property/:id/furnishing-studio',
      name: 'property-furnishing-studio',
      component: () => import('../views/PropertyFurnishingStudioView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/UserProfileView.vue'),
      alias: ['/me', '/user/profile', '/agent/profile'],
      meta: { requiresAuth: true }
    },
    {
      path: '/profile/settings',
      name: 'profile-settings',
      component: () => import('../views/ProfileSettingsView.vue'),
      meta: { requiresAuth: true }
    }
  ]
})

router.beforeEach((to, from, next) => {
  const loggedIn = localStorage.getItem('user')
  const authRequired = Boolean(to.meta.requiresAuth || to.meta.role)

  if (authRequired && !loggedIn) {
    ElMessage.warning('请先登录后查看详细内容')
    next({
      path: '/login',
      query: {
        redirect: to.fullPath,
        reason: 'auth',
      },
    })
    return
  }

  if (to.meta.role && loggedIn) {
    const user = JSON.parse(loggedIn)
    if (user.role !== to.meta.role) {
      next('/')
      return
    }
  }

  next()
})

export default router
