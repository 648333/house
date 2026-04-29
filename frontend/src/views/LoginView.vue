<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import AuthService from '@/api/auth'

const router = useRouter()
const route = useRoute()
const emit = defineEmits(['login-success'])
const formRef = ref(null)
const loading = ref(false)
const focusField = ref('')
const selectedRole = ref('USER')

const roleOptions = [
  { value: 'USER', label: '普通用户' },
  { value: 'AGENT', label: '经纪人' },
  { value: 'ADMIN', label: '管理员' },
]

const roleNameMap = {
  USER: '普通用户',
  AGENT: '经纪人',
  ADMIN: '管理员',
}

const form = ref({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const cloudMood = computed(() => {
  if (focusField.value === 'password') return 'shy'
  if (focusField.value === 'username') return 'curious'
  return 'idle'
})

const handleLogin = async () => {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const response = await AuthService.login(form.value)

    if (response.role !== selectedRole.value) {
      selectedRole.value = response.role
      ElMessage.info(`已按账号真实身份登录：${roleNameMap[response.role] || response.role}`)
    }

    emit('login-success')
    ElMessage.success('登录成功')

    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    if (redirect && redirect.startsWith('/') && redirect !== '/login') {
      router.push(redirect)
      return
    }

    if (response.role === 'ADMIN') {
      router.push('/admin')
    } else if (response.role === 'AGENT') {
      router.push('/agent')
    } else {
      router.push('/')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (route.query.reason === 'auth') {
    ElMessage.info('登录后将自动返回你刚才访问的页面')
  } else if (route.query.reason === 'expired') {
    ElMessage.info('登录状态已过期，请重新登录')
  }
})
</script>

<template>
  <div class="login-page">
    <div class="ambient ambient-one"></div>
    <div class="ambient ambient-two"></div>

    <div class="login-shell">
      <section class="showcase-panel">
        <div class="showcase-copy">
          <span class="showcase-kicker">暖屿找房</span>
          <h1>让找房这件事，既温柔也专业。</h1>
          <p>
            登录后即可浏览房源、在线咨询、预约看房，享受一站式找房服务。
          </p>
        </div>

        <div class="showcase-grid">
          <div class="showcase-card">
            <strong>清晰角色入口</strong>
            <span>不同身份从对应界面进入，减少误操作。</span>
          </div>
          <div class="showcase-card">
            <strong>更柔和的体验</strong>
            <span>页面层次清晰，操作简单直观。</span>
          </div>
        </div>

        <div class="cloud-stage" :class="cloudMood">
          <div class="cloud">
            <div class="cloud-core"></div>
            <div class="cloud-puff puff-1"></div>
            <div class="cloud-puff puff-2"></div>
            <div class="cloud-puff puff-3"></div>
            <div class="cloud-puff puff-4"></div>

            <div class="cloud-face">
              <div class="eyes">
                <span class="eye"></span>
                <span class="eye"></span>
              </div>
              <div class="blush-wrap">
                <span class="blush"></span>
                <span class="blush"></span>
              </div>
              <span class="mouth"></span>
            </div>
          </div>

          <div class="cloud-note">
            <strong>柔和、清晰、可信赖</strong>
            <span>从合适的身份入口开始，体验会自然很多。</span>
          </div>
        </div>
      </section>

      <el-card class="login-card" shadow="hover">
        <div class="auth-header">
          <span class="auth-kicker">Sign In</span>
          <h2>暖屿找房统一入口</h2>
          <p>选择身份登录，开始你的找房之旅。</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="handleLogin">
          <el-form-item prop="username">
            <template #label>
              <div class="field-label">
                <span>账号登录</span>
                <span class="field-tip">{{ roleNameMap[selectedRole] }}</span>
              </div>
            </template>

            <el-input
              v-model="form.username"
              :prefix-icon="User"
              placeholder="用户名"
              @focus="focusField = 'username'"
              @blur="focusField = ''"
            >
              <template #prepend>
                <el-select v-model="selectedRole" class="role-select" popper-class="login-role-popper">
                  <el-option
                    v-for="role in roleOptions"
                    :key="role.value"
                    :label="role.label"
                    :value="role.value"
                  />
                </el-select>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="登录密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              :prefix-icon="Lock"
              placeholder="密码"
              show-password
              @focus="focusField = 'password'"
              @blur="focusField = ''"
            />
          </el-form-item>

          <div class="form-subnote">
            登录后将根据身份自动进入对应页面。
          </div>

          <el-button type="primary" class="submit-btn" :loading="loading" @click="handleLogin">
            进入{{ roleNameMap[selectedRole] }}端
          </el-button>
        </el-form>

        <div class="auth-footer">
          <span>新账号注册</span>
          <el-link type="primary" @click="router.push('/register')">创建账户</el-link>
          <span class="footer-sep">|</span>
          <el-link type="primary" @click="router.push('/forgot-password')">找回密码</el-link>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  position: relative;
  min-height: calc(100vh - 180px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 18px;
  overflow: hidden;
  background:
    linear-gradient(180deg, #ecfbff 0%, #e0f7fa 58%, #fdf7f4 100%);
}

.ambient {
  position: absolute;
  width: 28rem;
  height: 28rem;
  border-radius: 50%;
  filter: blur(24px);
  pointer-events: none;
}

.ambient-one {
  top: -10rem;
  left: -8rem;
  background: radial-gradient(circle, rgba(255, 214, 226, 0.58), rgba(255, 214, 226, 0));
}

.ambient-two {
  right: -10rem;
  bottom: -10rem;
  background: radial-gradient(circle, rgba(141, 216, 255, 0.5), rgba(141, 216, 255, 0));
}

.login-shell {
  position: relative;
  width: min(1180px, 100%);
  display: grid;
  grid-template-columns: 1.08fr 0.92fr;
  gap: 24px;
  align-items: stretch;
}

.showcase-panel,
.login-card {
  position: relative;
  z-index: 1;
  border-radius: 36px;
  border: 1px solid rgba(255, 255, 255, 0.74);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(20px);
  box-shadow: 0 26px 64px rgba(103, 144, 184, 0.15);
}

.showcase-panel {
  padding: 34px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
}

.showcase-kicker,
.auth-kicker {
  display: inline-flex;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(79, 195, 247, 0.12);
  color: #4fc3f7;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.showcase-copy h1 {
  margin: 18px 0 14px;
  font-size: 46px;
  line-height: 1.1;
  color: #2f4555;
}

.showcase-copy p {
  margin: 0;
  max-width: 32rem;
  color: #758793;
  line-height: 1.9;
  font-size: 15px;
}

.showcase-grid {
  margin-top: 26px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.showcase-card {
  padding: 18px;
  border-radius: 24px;
  background: rgba(247, 251, 255, 0.88);
}

.showcase-card strong {
  display: block;
  color: #33495a;
  font-size: 15px;
}

.showcase-card span {
  display: block;
  margin-top: 8px;
  color: #8195a5;
  line-height: 1.7;
  font-size: 13px;
}

.login-card {
  padding: 10px;
}

.auth-header {
  text-align: center;
  margin-bottom: 22px;
}

.auth-header h2 {
  margin: 16px 0 10px;
  color: #314655;
  font-size: 30px;
}

.auth-header p {
  margin: 0;
  color: #748693;
  line-height: 1.8;
}

:deep(.el-form-item__label) {
  color: #3a4f5d;
  font-weight: 700;
}

.field-label {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.field-tip {
  color: #8da0ac;
  font-size: 12px;
  font-weight: 600;
}

:deep(.el-input-group__prepend) {
  background: #eef3f6;
  border: none;
  box-shadow: none;
  padding-left: 10px;
  border-radius: 16px 0 0 16px;
}

:deep(.el-input-group__prepend .el-select__wrapper) {
  min-width: 126px;
  background: transparent !important;
  box-shadow: none !important;
  border-radius: 12px !important;
}

:deep(.el-input__wrapper) {
  background: #eef3f6 !important;
  box-shadow: none !important;
  border-radius: 16px;
  border: 1px solid transparent;
  min-height: 48px;
}

:deep(.el-input__wrapper.is-focus) {
  background: #ffffff !important;
  border-color: #4fc3f7;
  box-shadow: 0 0 0 1px #4fc3f7 !important;
}

.role-select {
  width: 126px;
}

.form-subnote {
  margin: 2px 0 14px;
  color: #8b9ca8;
  font-size: 13px;
  line-height: 1.7;
}

.submit-btn {
  width: 100%;
  height: 52px;
  margin-top: 8px;
  border-radius: 18px;
  border: none;
  background: linear-gradient(135deg, #4fc3f7, #65cdf6) !important;
  color: white;
  font-weight: 700;
  letter-spacing: 0.08em;
  box-shadow: 0 16px 28px rgba(79, 195, 247, 0.24);
}

.auth-footer {
  margin-top: 20px;
  text-align: center;
  color: #7c8d98;
}

.footer-sep {
  margin: 0 8px;
  color: #9db0bc;
}

.cloud-stage {
  margin-top: 28px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.cloud {
  position: relative;
  width: 176px;
  height: 112px;
  animation: cloudFloat 4s ease-in-out infinite;
}

.cloud-core,
.cloud-puff {
  position: absolute;
  background: #ffffff;
  border-radius: 999px;
}

.cloud-core {
  inset: 30px 10px 0;
  box-shadow: 0 14px 34px rgba(79, 195, 247, 0.18);
}

.puff-1 { width: 64px; height: 64px; left: 8px; top: 34px; }
.puff-2 { width: 84px; height: 84px; left: 34px; top: 6px; }
.puff-3 { width: 70px; height: 70px; right: 18px; top: 16px; }
.puff-4 { width: 52px; height: 52px; right: 4px; top: 42px; }

.cloud-face {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -22%);
  width: 88px;
  text-align: center;
  z-index: 2;
}

.eyes {
  display: flex;
  justify-content: center;
  gap: 22px;
}

.eye {
  width: 9px;
  height: 9px;
  background: #424242;
  border-radius: 50%;
  transition: all 0.25s ease;
}

.blush-wrap {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
  padding: 0 4px;
}

.blush {
  width: 14px;
  height: 10px;
  background: #ffebee;
  border-radius: 50%;
}

.mouth {
  display: inline-block;
  margin-top: 8px;
  width: 14px;
  height: 7px;
  border-bottom: 3px solid #424242;
  border-radius: 0 0 14px 14px;
}

.cloud-note {
  margin-top: 22px;
  padding: 14px 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.68);
  text-align: center;
  box-shadow: 0 14px 32px rgba(121, 161, 193, 0.12);
}

.cloud-note strong {
  display: block;
  color: #32485a;
  font-size: 14px;
}

.cloud-note span {
  display: block;
  margin-top: 6px;
  color: #7c8e9b;
  line-height: 1.7;
  font-size: 13px;
}

.curious .eye {
  width: 11px;
  height: 11px;
}

.shy .eye {
  width: 10px;
  height: 2px;
  border-radius: 999px;
  margin-top: 4px;
}

.shy .blush {
  background: #ffd5de;
  transform: scale(1.2);
}

@keyframes cloudFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-12px); }
}

@media (max-width: 980px) {
  .login-shell {
    grid-template-columns: 1fr;
  }

  .showcase-copy h1 {
    font-size: 38px;
  }
}

@media (max-width: 640px) {
  .showcase-panel,
  .login-card {
    border-radius: 28px;
  }

  .showcase-panel {
    padding: 24px;
  }

  .showcase-grid {
    grid-template-columns: 1fr;
  }
}
</style>
