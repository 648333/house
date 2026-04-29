<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Message, User } from '@element-plus/icons-vue'
import AuthService from '@/api/auth'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const role = ref('USER')
const focusField = ref('')

const form = ref({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
})

const cloudMood = computed(() => {
  if (focusField.value === 'password' || focusField.value === 'confirmPassword') return 'shy'
  if (focusField.value) return 'curious'
  return 'idle'
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度应为 3 到 20 位', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 40, message: '密码长度应为 6 到 40 位', trigger: 'blur' },
  ],
  confirmPassword: [{ required: true, message: '请再次输入密码', trigger: 'blur' }],
}

const handleRegister = async () => {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (form.value.password !== form.value.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }

  loading.value = true
  try {
    await AuthService.register({
      username: form.value.username,
      email: form.value.email,
      password: form.value.password,
      role: role.value,
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '注册失败，请稍后再试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <div class="register-shell">
      <el-card class="register-card" shadow="hover">
        <div class="auth-header">
          <span class="auth-kicker">加入暖屿</span>
          <h2>创建你的专属找房账号</h2>
          <p>注册后可以收藏房源、预约看房、在线咨询，也能收到更贴近偏好的推荐。</p>
        </div>

        <el-radio-group v-model="role" class="role-group">
          <el-radio-button value="USER">普通用户</el-radio-button>
          <el-radio-button value="AGENT">经纪人 / 房东</el-radio-button>
        </el-radio-group>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="handleRegister">
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              :prefix-icon="User"
              placeholder="请输入用户名"
              @focus="focusField = 'username'"
              @blur="focusField = ''"
            />
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <el-input
              v-model="form.email"
              :prefix-icon="Message"
              placeholder="请输入邮箱"
              @focus="focusField = 'email'"
              @blur="focusField = ''"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              :prefix-icon="Lock"
              placeholder="请输入密码"
              show-password
              @focus="focusField = 'password'"
              @blur="focusField = ''"
            />
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              :prefix-icon="Lock"
              placeholder="请再次输入密码"
              show-password
              @focus="focusField = 'confirmPassword'"
              @blur="focusField = ''"
            />
          </el-form-item>

          <el-button type="primary" class="submit-btn" :loading="loading" @click="handleRegister">
            立即注册
          </el-button>
        </el-form>

        <div class="auth-footer">
          已有账号？
          <el-link type="primary" @click="router.push('/login')">去登录</el-link>
        </div>
      </el-card>

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
        <p class="cloud-text">填好资料，我们就一起认真开始找房。</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  min-height: calc(100vh - 180px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
  background:
    radial-gradient(circle at top left, #ffebee 0, transparent 28%),
    radial-gradient(circle at bottom right, rgba(79, 195, 247, 0.18) 0, transparent 24%),
    #e0f7fa;
}

.register-shell {
  display: flex;
  align-items: center;
  gap: 40px;
}

.register-card {
  width: 470px;
  max-width: 100%;
  border-radius: 32px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.92);
}

.auth-header {
  text-align: center;
  margin-bottom: 18px;
}

.auth-kicker {
  display: inline-flex;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(79, 195, 247, 0.12);
  color: #4fc3f7;
  font-size: 12px;
  font-weight: 700;
}

.auth-header h2 {
  margin: 14px 0 8px;
  color: #424242;
  font-size: 30px;
}

.auth-header p {
  margin: 0;
  color: #6f7c80;
  line-height: 1.7;
}

.role-group {
  display: flex;
  margin-bottom: 20px;
}

.role-group :deep(.el-radio-button) {
  flex: 1;
}

.role-group :deep(.el-radio-button__inner) {
  width: 100%;
  background: #eeeeee;
  border-color: #eeeeee;
  color: #424242;
}

.role-group :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: #4fc3f7;
  border-color: #4fc3f7;
  color: white;
  box-shadow: none;
}

:deep(.el-form-item__label) {
  color: #424242;
  font-weight: 600;
}

:deep(.el-input__wrapper) {
  background: #eeeeee !important;
  box-shadow: none !important;
  border-radius: 14px;
  border: 1px solid transparent;
}

:deep(.el-input__wrapper.is-focus) {
  background: #ffffff !important;
  border-color: #4fc3f7;
  box-shadow: 0 0 0 1px #4fc3f7 !important;
}

.submit-btn {
  width: 100%;
  height: 48px;
  margin-top: 8px;
  border-radius: 16px;
  border: none;
  background: #4fc3f7 !important;
  color: white;
  font-weight: 700;
}

.auth-footer {
  margin-top: 18px;
  text-align: center;
  color: #757575;
}

.cloud-stage {
  width: 240px;
  min-height: 260px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.cloud {
  position: relative;
  width: 170px;
  height: 108px;
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
  box-shadow: 0 12px 30px rgba(79, 195, 247, 0.18);
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

.cloud-text {
  margin-top: 20px;
  color: #424242;
  text-align: center;
  line-height: 1.7;
  background: rgba(255, 255, 255, 0.65);
  padding: 10px 14px;
  border-radius: 18px;
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

@media (max-width: 900px) {
  .register-shell {
    flex-direction: column-reverse;
    gap: 18px;
  }

  .cloud-stage {
    width: 100%;
    min-height: 180px;
  }
}
</style>
