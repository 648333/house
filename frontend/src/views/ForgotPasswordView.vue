<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthService from '@/api/auth'

const router = useRouter()
const sending = ref(false)
const resetting = ref(false)
const sent = ref(false)
const debugCode = ref('')

const form = ref({
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: '',
})

const sendCode = async () => {
  if (!form.value.email) {
    ElMessage.warning('请输入邮箱')
    return
  }

  sending.value = true
  try {
    const response = await AuthService.forgotPassword(form.value.email)
    sent.value = true
    debugCode.value = response?.data?.code || ''
    ElMessage.success('验证码已生成，请继续重置密码')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '发送验证码失败')
  } finally {
    sending.value = false
  }
}

const resetPassword = async () => {
  if (!form.value.email || !form.value.code || !form.value.newPassword) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (form.value.newPassword !== form.value.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }

  resetting.value = true
  try {
    await AuthService.resetPassword({
      email: form.value.email,
      code: form.value.code,
      newPassword: form.value.newPassword,
    })
    ElMessage.success('密码重置成功，请登录')
    router.push('/login')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '重置密码失败')
  } finally {
    resetting.value = false
  }
}
</script>

<template>
  <div class="forgot-page">
    <el-card class="forgot-card">
      <h2>找回密码</h2>
      <p class="desc">输入注册邮箱，获取验证码后设置新密码。</p>

      <el-form label-position="top">
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入注册邮箱" />
        </el-form-item>

        <div class="row">
          <el-form-item label="验证码" class="grow">
            <el-input v-model="form.code" placeholder="6位验证码" />
          </el-form-item>
          <el-button :loading="sending" @click="sendCode">
            {{ sent ? '重新发送' : '发送验证码' }}
          </el-button>
        </div>

        <el-alert
          v-if="debugCode"
          type="warning"
          :closable="false"
          title="演示模式验证码"
          :description="`当前验证码: ${debugCode}`"
          style="margin-bottom: 12px"
        />

        <el-form-item label="新密码">
          <el-input v-model="form.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>

        <el-form-item label="确认新密码">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>

        <div class="actions">
          <el-button @click="router.push('/login')">返回登录</el-button>
          <el-button type="primary" :loading="resetting" @click="resetPassword">重置密码</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.forgot-page {
  max-width: 560px;
  margin: 0 auto;
  padding: 24px 16px;
}

.forgot-card {
  border-radius: 18px;
}

h2 {
  margin: 0;
}

.desc {
  margin: 8px 0 16px;
  color: #6d6d6d;
}

.row {
  display: flex;
  gap: 10px;
  align-items: end;
}

.grow {
  flex: 1;
}

.actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>

