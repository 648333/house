<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCurrentUserProfile, updateCurrentUserProfile } from '@/api/user'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = ref({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
}

const fetchProfile = async () => {
  try {
    const response = await getCurrentUserProfile()
    form.value.username = response.data.username
    form.value.email = response.data.email
  } catch (error) {
    ElMessage.error('获取账号信息失败')
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (form.value.password && form.value.password !== form.value.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }

  loading.value = true
  try {
    const response = await updateCurrentUserProfile({
      username: form.value.username,
      email: form.value.email,
      password: form.value.password || undefined,
    })

    const currentUser = JSON.parse(localStorage.getItem('user') || '{}')
    localStorage.setItem(
      'user',
      JSON.stringify({
        ...currentUser,
        username: response.data.username,
        email: response.data.email,
      })
    )

    ElMessage.success('账号信息已更新')
    router.push('/profile')
  } catch (error) {
    ElMessage.error(error.response?.data || '保存失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchProfile()
})
</script>

<template>
  <div class="settings-page">
    <el-card class="settings-card">
      <div class="header">
        <div>
          <h2>账号设置</h2>
          <p>可以修改用户名、邮箱和登录密码。</p>
        </div>
        <el-button round @click="router.push('/profile')">返回</el-button>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="form.password" type="password" placeholder="留空则不修改密码" show-password />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入新密码" show-password />
        </el-form-item>

        <el-button type="primary" class="save-btn" :loading="loading" @click="handleSubmit">保存修改</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.settings-page {
  max-width: 760px;
  margin: 0 auto;
  padding: 24px 16px;
}

.settings-card {
  border-radius: 24px;
  border: none;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.header h2 {
  margin: 0 0 8px;
  color: #34495e;
}

.header p {
  margin: 0;
  color: #8aa0ad;
}

.save-btn {
  width: 100%;
  margin-top: 8px;
  border-radius: 14px;
}
</style>
