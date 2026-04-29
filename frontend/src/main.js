import { createApp } from 'vue'
import { createPinia } from 'pinia'
import axios from 'axios'
import { ElMessage } from 'element-plus'

import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/theme.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

axios.interceptors.request.use((config) => {
  const rawUser = localStorage.getItem('user')
  if (!rawUser) return config

  try {
    const user = JSON.parse(rawUser)
    if (user?.accessToken && !config.headers?.Authorization) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${user.accessToken}`
    }
  } catch (error) {
    localStorage.removeItem('user')
  }

  return config
})

axios.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status
    const requestUrl = error?.config?.url || ''
    const isLoginApi = requestUrl.includes('/auth/login')

    if (status === 401 && !isLoginApi) {
      localStorage.removeItem('user')
      const redirect = router.currentRoute.value?.fullPath || '/'
      const atLogin = router.currentRoute.value?.path === '/login'

      if (!atLogin) {
        ElMessage.warning('登录状态已失效，请重新登录')
        router.push({ path: '/login', query: { redirect, reason: 'expired' } })
      }
    }

    return Promise.reject(error)
  }
)

app.mount('#app')
