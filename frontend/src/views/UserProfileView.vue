<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Clock, Location, Promotion, Setting, View } from '@element-plus/icons-vue'
import AuthService from '@/api/auth'
import { getMyAppointments, getAppointmentTimeline } from '@/api/appointment'
import { getChatHistory, getMyMessages, markChatHistoryAsRead, sendMessage } from '@/api/message'
import { getMyPaymentOrders, getPaymentVoucher } from '@/api/payment'
import { getMyRequirements } from '@/api/requirement'
import PropertyService from '@/api/property'
import { createSupportTicket, getMySupportTickets, getSupportCategories } from '@/api/ticket'
import { getFavorites } from '@/utils/favorites'
import { normalizeProperty } from '@/utils/property'
import { getRecentlyViewedIds } from '@/utils/recentlyViewed'

const router = useRouter()

const user = ref({})
const activeMenu = ref('favorites')
const favoriteProperties = ref([])
const appointments = ref([])
const messages = ref([])
const paymentOrders = ref([])
const recentProperties = ref([])
const requirements = ref([])
const appointmentTimelines = ref({})
const vouchers = ref({})
const tickets = ref([])
const ticketCategories = ref([])
const ticketSubmitting = ref(false)
const currentConversation = ref(null)
const chatHistory = ref([])
const replyText = ref('')
const chatLoading = ref(false)

const ticketForm = ref({
  category: 'PROPERTY_INFO',
  priority: 'MEDIUM',
  title: '',
  description: '',
  contactMobile: '',
})

const priorityOptions = [
  { value: 'LOW', label: '低' },
  { value: 'MEDIUM', label: '普通' },
  { value: 'HIGH', label: '高' },
  { value: 'URGENT', label: '紧急' },
]

const priorityLabelMap = priorityOptions.reduce((map, item) => ({ ...map, [item.value]: item.label }), {})

const unreadMessageCount = computed(() =>
  messages.value.filter((m) => m.receiver?.id === user.value?.id && !m.read).length
)

const openTicketCount = computed(() =>
  tickets.value.filter((t) => t.status === 'OPEN' || t.status === 'PROCESSING').length
)

const paidOrderCount = computed(() =>
  paymentOrders.value.filter((order) => order.status === 'PAID').length
)

const paymentOrderMap = computed(() => {
  const map = {}
  paymentOrders.value.forEach((order) => {
    if (order.appointmentId) {
      map[order.appointmentId] = order
    }
  })
  return map
})

const stats = computed(() => ({
  collections: favoriteProperties.value.length,
  appointments: appointments.value.length,
  consultations: messages.value.length,
  transactions: paymentOrders.value.length,
}))

const messageCards = computed(() => {
  const conversationMap = new Map()

  for (const message of messages.value) {
    const counterpart = message.sender?.id === user.value?.id ? message.receiver : message.sender
    const propertyId = message.property?.id
    const otherUserId = counterpart?.id

    if (!propertyId || !otherUserId) {
      continue
    }

    const key = `${propertyId}-${otherUserId}`
    const sentAt = new Date(message.sentAt || 0).getTime()
    const isUnreadIncoming = message.receiver?.id === user.value?.id && !message.read

    if (!conversationMap.has(key)) {
      conversationMap.set(key, {
        ...message,
        counterpart,
        latestSentAt: sentAt,
        unreadCount: isUnreadIncoming ? 1 : 0,
      })
      continue
    }

    const existing = conversationMap.get(key)
    if (sentAt >= existing.latestSentAt) {
      conversationMap.set(key, {
        ...message,
        counterpart,
        latestSentAt: sentAt,
        unreadCount: existing.unreadCount + (isUnreadIncoming ? 1 : 0),
      })
    } else {
      existing.unreadCount += isUnreadIncoming ? 1 : 0
      conversationMap.set(key, existing)
    }
  }

  return [...conversationMap.values()].sort((a, b) => {
    const aUnread = a.unreadCount > 0 ? 1 : 0
    const bUnread = b.unreadCount > 0 ? 1 : 0
    if (aUnread !== bUnread) return bUnread - aUnread
    return (b.latestSentAt || 0) - (a.latestSentAt || 0)
  })
})

const fetchUserData = () => {
  const storedUser = localStorage.getItem('user')
  user.value = storedUser ? JSON.parse(storedUser) : {}
}

const fetchData = async () => {
  try {
    const [propertyResponse, appointmentResponse, messageResponse, requirementResponse, ticketResponse, categoryResponse, paymentResponse] = await Promise.all([
      PropertyService.getAllProperties(),
      getMyAppointments(),
      getMyMessages(),
      getMyRequirements(),
      getMySupportTickets(),
      getSupportCategories(),
      getMyPaymentOrders(),
    ])

    const normalized = propertyResponse.data.map((item, index) => normalizeProperty(item, index))
    const favoriteIds = getFavorites()
    favoriteProperties.value = normalized.filter((item) => favoriteIds.includes(item.id))

    const recentIds = getRecentlyViewedIds()
    recentProperties.value = recentIds
      .map((id) => normalized.find((item) => item.id === id))
      .filter(Boolean)

    appointments.value = appointmentResponse.data
    messages.value = messageResponse.data
    paymentOrders.value = Array.isArray(paymentResponse.data) ? paymentResponse.data : []
    requirements.value = Array.isArray(requirementResponse.data) ? requirementResponse.data : []
    tickets.value = Array.isArray(ticketResponse.data) ? ticketResponse.data : []
    ticketCategories.value = Array.isArray(categoryResponse.data) ? categoryResponse.data : []
  } catch (error) {
    ElMessage.error('个人中心数据加载失败')
  }
}

const scrollChatToBottom = async () => {
  await nextTick()
  const container = document.querySelector('.profile-chat-history')
  if (container) {
    container.scrollTop = container.scrollHeight
  }
}

const getCounterpart = (message) => {
  if (!message) return null
  return message.sender?.id === user.value?.id ? message.receiver : message.sender
}

const getConversationTitle = (message) => {
  const counterpart = getCounterpart(message)
  return counterpart?.username || '系统消息'
}

const canOpenConversation = (message) => Boolean(message?.property?.id && getCounterpart(message)?.id)

const openConversation = async (message) => {
  if (!canOpenConversation(message)) {
    ElMessage.info('该消息暂无可查看的聊天记录')
    return
  }

  const counterpart = getCounterpart(message)
  currentConversation.value = {
    propertyId: message.property.id,
    propertyTitle: message.property.title || '当前房源',
    otherUserId: counterpart.id,
    otherUserName: counterpart.username || '房源顾问',
  }

  chatLoading.value = true
  try {
    const response = await getChatHistory(currentConversation.value.propertyId, currentConversation.value.otherUserId)
    chatHistory.value = response.data.map((item) => ({
      ...item,
      isMe: item.sender?.id === user.value?.id,
    }))

    const hasUnreadIncoming = chatHistory.value.some((item) => item.receiver?.id === user.value?.id && !item.read)
    if (hasUnreadIncoming) {
      await markChatHistoryAsRead(currentConversation.value.propertyId, currentConversation.value.otherUserId)
      chatHistory.value = chatHistory.value.map((item) => (
        item.receiver?.id === user.value?.id ? { ...item, read: true } : item
      ))
      messages.value = messages.value.map((item) => (
        item.property?.id === currentConversation.value.propertyId
          && getCounterpart(item)?.id === currentConversation.value.otherUserId
          && item.receiver?.id === user.value?.id
          ? { ...item, read: true }
          : item
      ))
    }

    await scrollChatToBottom()
  } catch (error) {
    ElMessage.error('聊天记录加载失败')
  } finally {
    chatLoading.value = false
  }
}

const sendReply = async () => {
  if (!currentConversation.value || !replyText.value.trim()) return

  try {
    await sendMessage({
      receiver: { id: currentConversation.value.otherUserId },
      property: { id: currentConversation.value.propertyId },
      content: replyText.value.trim(),
    })

    replyText.value = ''
    const messageResponse = await getMyMessages()
    messages.value = messageResponse.data
    await openConversation({
      property: { id: currentConversation.value.propertyId, title: currentConversation.value.propertyTitle },
      sender: { id: currentConversation.value.otherUserId, username: currentConversation.value.otherUserName },
      receiver: { id: user.value?.id },
    })
  } catch (error) {
    ElMessage.error('发送消息失败')
  }
}

const loadAppointmentTimeline = async (appointmentId) => {
  try {
    const response = await getAppointmentTimeline(appointmentId)
    appointmentTimelines.value = {
      ...appointmentTimelines.value,
      [appointmentId]: response.data,
    }
  } catch (error) {
    ElMessage.error('预约进度加载失败')
  }
}

const orderStatusType = (status) => {
  if (status === 'PAID') return 'success'
  if (status === 'FAILED' || status === 'CLOSED') return 'danger'
  if (status === 'PAYING' || status === 'CREATED') return 'warning'
  return ''
}

const orderStatusLabel = (status) => {
  if (status === 'PAID') return '已支付'
  if (status === 'FAILED') return '支付失败'
  if (status === 'CLOSED') return '已关闭'
  if (status === 'PAYING') return '支付中'
  if (status === 'CREATED') return '待支付'
  return status || '未知状态'
}

const appointmentStatusLabel = (status) => {
  if (status === 'APPROVED') return '顾问已确认'
  if (status === 'COMPLETED') return '已完成线下看房'
  if (status === 'REJECTED') return '预约被拒绝'
  if (status === 'CANCELLED') return '已取消'
  return '待确认'
}

const buildTradeSteps = (order) => {
  const paid = order?.status === 'PAID'
  const approved = order?.appointmentStatus === 'APPROVED' || order?.appointmentStatus === 'COMPLETED'
  const completed = order?.appointmentStatus === 'COMPLETED'

  return [
    { title: '提交预约', finished: Boolean(order?.appointmentId) },
    { title: '创建订单', finished: Boolean(order?.id) },
    { title: '支付意向金', finished: paid },
    { title: '顾问确认', finished: approved },
    { title: '线下签约', finished: completed },
  ]
}

const loadVoucher = async (orderId) => {
  if (vouchers.value[orderId]) return

  try {
    const response = await getPaymentVoucher(orderId)
    vouchers.value = {
      ...vouchers.value,
      [orderId]: response.data,
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '存证信息加载失败')
  }
}

const submitTicket = async () => {
  const payload = {
    category: ticketForm.value.category || 'OTHER',
    priority: ticketForm.value.priority || 'MEDIUM',
    title: ticketForm.value.title?.trim(),
    description: ticketForm.value.description?.trim(),
    contactMobile: ticketForm.value.contactMobile?.trim(),
  }

  if (!payload.title || !payload.description) {
    ElMessage.warning('请补充工单标题和问题描述')
    return
  }

  try {
    ticketSubmitting.value = true
    await createSupportTicket(payload)
    ElMessage.success('工单提交成功')
    ticketForm.value = {
      category: 'PROPERTY_INFO',
      priority: 'MEDIUM',
      title: '',
      description: '',
      contactMobile: '',
    }
    await fetchData()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.response?.data || '工单提交失败')
  } finally {
    ticketSubmitting.value = false
  }
}

const handleLogout = () => {
  AuthService.logout()
  router.push('/login')
}

onMounted(() => {
  fetchUserData()
  fetchData()
})

watch(activeMenu, async (value) => {
  if (value !== 'messages' || currentConversation.value || messageCards.value.length === 0) {
    return
  }

  const firstUnread = messageCards.value.find(
    (message) => message.receiver?.id === user.value?.id && !message.read && canOpenConversation(message)
  )
  const fallbackMessage = messageCards.value.find((message) => canOpenConversation(message))
  const targetMessage = firstUnread || fallbackMessage

  if (targetMessage) {
    await openConversation(targetMessage)
  }
})
</script>

<template>
  <div class="user-profile">
    <section class="hero-card">
      <div class="user-info">
        <el-avatar :size="70" class="hero-avatar">{{ (user.username || 'U').charAt(0).toUpperCase() }}</el-avatar>
        <div class="user-details">
          <span class="profile-kicker">个人中心</span>
          <h2>{{ user.username || '用户' }}</h2>
          <p>{{ user.email || '未设置邮箱' }}</p>
        </div>
      </div>
      <el-button :icon="Setting" plain @click="router.push('/profile/settings')">账号设置</el-button>
    </section>

    <el-row :gutter="18" class="stats-grid">
      <el-col :xs="12" :sm="6"><div class="stat-item"><span class="stat-num">{{ stats.collections }}</span><span class="stat-label">收藏房源</span></div></el-col>
      <el-col :xs="12" :sm="6"><div class="stat-item"><span class="stat-num">{{ stats.appointments }}</span><span class="stat-label">预约记录</span></div></el-col>
      <el-col :xs="12" :sm="6"><div class="stat-item"><span class="stat-num">{{ stats.consultations }}</span><span class="stat-label">咨询消息</span></div></el-col>
      <el-col :xs="12" :sm="6"><div class="stat-item"><span class="stat-num">{{ paidOrderCount }}</span><span class="stat-label">已完成交易款项</span></div></el-col>
    </el-row>

    <el-card class="function-list" shadow="never">
      <el-tabs v-model="activeMenu">
        <el-tab-pane name="favorites" label="收藏房源">
          <div class="card-list">
            <el-empty v-if="favoriteProperties.length === 0" description="暂无收藏房源" />
            <el-card v-for="property in favoriteProperties" :key="property.id" class="list-item" shadow="hover" @click="router.push(`/property/${property.id}`)">
              <div class="item-header"><span class="property-name">{{ property.title }}</span><span class="item-price">¥ {{ property.price }} 万</span></div>
              <div class="item-content"><p><el-icon><Location /></el-icon>{{ property.address }}</p></div>
            </el-card>
          </div>
        </el-tab-pane>

        <el-tab-pane name="appointments" label="预约记录">
          <div class="card-list">
            <el-empty v-if="appointments.length === 0" description="暂无预约记录" />
            <el-card v-for="appointment in appointments" :key="appointment.id" class="list-item" shadow="hover">
              <div class="item-header">
                <span class="property-name">{{ appointment.property?.title || '看房预约' }}</span>
                <el-tag size="small">{{ appointmentStatusLabel(appointment.status) }}</el-tag>
              </div>
              <div class="item-content">
                <p><el-icon><Clock /></el-icon>{{ new Date(appointment.appointmentTime).toLocaleString() }}</p>
                <p>{{ appointment.message || '等待确认中' }}</p>
                <div v-if="paymentOrderMap[appointment.id]" class="inline-order">
                  <span>关联交易单：{{ orderStatusLabel(paymentOrderMap[appointment.id].status) }}</span>
                  <span>金额 ¥ {{ paymentOrderMap[appointment.id].amount }}</span>
                </div>
                <el-button size="small" type="primary" plain @click="loadAppointmentTimeline(appointment.id)">查看预约进度</el-button>
                <el-timeline v-if="appointmentTimelines[appointment.id]" class="timeline">
                  <el-timeline-item
                    v-for="item in appointmentTimelines[appointment.id]"
                    :key="`${appointment.id}-${item.code}-${item.time}`"
                    :timestamp="item.time ? new Date(item.time).toLocaleString() : ''"
                  >
                    {{ item.label }}
                  </el-timeline-item>
                </el-timeline>
              </div>
            </el-card>
          </div>
        </el-tab-pane>

        <el-tab-pane name="transactions" label="交易管理">
          <div class="trade-summary">
            <div class="trade-metric">
              <strong>{{ paymentOrders.length }}</strong>
              <span>交易订单</span>
            </div>
            <div class="trade-metric">
              <strong>{{ paidOrderCount }}</strong>
              <span>已支付订单</span>
            </div>
            <div class="trade-metric">
              <strong>{{ paymentOrders.filter((item) => item.status === 'PAYING' || item.status === 'CREATED').length }}</strong>
              <span>进行中</span>
            </div>
          </div>

          <div class="card-list">
            <el-empty v-if="paymentOrders.length === 0" description="暂无交易订单" />
            <el-card v-for="order in paymentOrders" :key="order.id" class="list-item trade-card" shadow="hover">
              <div class="item-header">
                <div>
                  <div class="property-name">{{ order.propertyTitle || order.subject }}</div>
                  <div class="trade-order-no">订单号：{{ order.outTradeNo }}</div>
                </div>
                <el-tag :type="orderStatusType(order.status)">{{ orderStatusLabel(order.status) }}</el-tag>
              </div>

              <div class="trade-grid">
                <div><span class="muted-label">金额</span><strong>¥ {{ order.amount }}</strong></div>
                <div><span class="muted-label">渠道</span><strong>{{ order.channel }}</strong></div>
                <div><span class="muted-label">创建时间</span><strong>{{ order.createdAt ? new Date(order.createdAt).toLocaleString() : '-' }}</strong></div>
                <div><span class="muted-label">支付时间</span><strong>{{ order.paidAt ? new Date(order.paidAt).toLocaleString() : '-' }}</strong></div>
              </div>

              <div class="trade-steps">
                <div
                  v-for="step in buildTradeSteps(order)"
                  :key="`${order.id}-${step.title}`"
                  class="trade-step"
                  :class="{ finished: step.finished }"
                >
                  {{ step.title }}
                </div>
              </div>

              <div class="trade-note">
                <span>当前预约状态：{{ appointmentStatusLabel(order.appointmentStatus) }}</span>
                <span v-if="order.thirdPartyTradeNo">流水号：{{ order.thirdPartyTradeNo }}</span>
                <span v-else-if="order.failureReason">异常原因：{{ order.failureReason }}</span>
              </div>

              <div class="trade-actions">
                <el-button v-if="order.propertyId" size="small" plain @click="router.push(`/property/${order.propertyId}`)">查看房源</el-button>
                <el-button v-if="order.voucherAvailable" size="small" type="primary" plain @click="loadVoucher(order.id)">查看交易存证</el-button>
              </div>

              <div v-if="vouchers[order.id]" class="voucher-card">
                <div class="voucher-title">交易存证</div>
                <p>凭证编号：{{ vouchers[order.id].voucherNo }}</p>
                <p>客户姓名：{{ vouchers[order.id].customer }}</p>
                <p>房源标题：{{ vouchers[order.id].propertyTitle }}</p>
                <p>支付金额：¥ {{ vouchers[order.id].amount }}</p>
                <p>支付时间：{{ vouchers[order.id].paidAt ? new Date(vouchers[order.id].paidAt).toLocaleString() : '-' }}</p>
                <p>{{ vouchers[order.id].summary }}</p>
              </div>
            </el-card>
          </div>
        </el-tab-pane>

        <el-tab-pane name="tickets">
          <template #label>
            <span>客服工单</span>
            <el-badge v-if="openTicketCount > 0" :value="openTicketCount" class="tab-badge" />
          </template>
          <el-card class="list-item" shadow="never">
            <el-form label-position="top">
              <el-form-item label="问题类型">
                <el-select v-model="ticketForm.category">
                  <el-option v-for="item in ticketCategories" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="紧急程度">
                <el-select v-model="ticketForm.priority">
                  <el-option v-for="item in priorityOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="标题"><el-input v-model="ticketForm.title" /></el-form-item>
              <el-form-item label="问题描述"><el-input v-model="ticketForm.description" type="textarea" :rows="3" /></el-form-item>
              <el-form-item label="联系电话"><el-input v-model="ticketForm.contactMobile" /></el-form-item>
              <el-button type="primary" :loading="ticketSubmitting" @click="submitTicket">提交工单</el-button>
            </el-form>
          </el-card>

          <div class="card-list">
            <el-empty v-if="tickets.length === 0" description="暂无工单记录" />
            <el-card v-for="ticket in tickets" :key="ticket.id" class="list-item" shadow="hover">
              <div class="item-header">
                <span class="property-name">{{ ticket.title }}</span>
                <div class="ticket-tags">
                  <el-tag type="warning">{{ priorityLabelMap[ticket.priority] || '普通' }}</el-tag>
                  <el-tag>{{ ticket.status }}</el-tag>
                </div>
              </div>
              <div class="item-content">
                <p>{{ ticket.description }}</p>
                <p v-if="ticket.handlerNote">处理说明：{{ ticket.handlerNote }}</p>
                <p v-if="ticket.resolvedAt">解决时间：{{ new Date(ticket.resolvedAt).toLocaleString() }}</p>
                <p>{{ new Date(ticket.createdAt).toLocaleString() }}</p>
              </div>
            </el-card>
          </div>
        </el-tab-pane>

        <el-tab-pane name="requirements" label="找房需求">
          <div class="card-list">
            <el-empty v-if="requirements.length === 0" description="暂无需求" />
            <el-card v-for="req in requirements" :key="req.id" class="list-item" shadow="hover">
              <div class="item-header"><span class="property-name">{{ req.title }}</span><el-tag>{{ req.status }}</el-tag></div>
              <div class="item-content"><p>{{ req.note }}</p></div>
            </el-card>
          </div>
        </el-tab-pane>

        <el-tab-pane name="recent" label="最近浏览">
          <div class="card-list">
            <el-empty v-if="recentProperties.length === 0" description="暂无最近浏览" />
            <el-card v-for="property in recentProperties" :key="property.id" class="list-item" shadow="hover" @click="router.push(`/property/${property.id}`)">
              <div class="item-header"><span class="property-name">{{ property.title }}</span><span class="item-price">¥ {{ property.price }} 万</span></div>
              <div class="item-content"><p><el-icon><View /></el-icon>{{ property.layout }} / {{ property.area }}㎡</p></div>
            </el-card>
          </div>
        </el-tab-pane>

        <el-tab-pane name="messages">
          <template #label>
            <span>咨询消息</span>
            <el-badge v-if="unreadMessageCount > 0" :value="unreadMessageCount" class="tab-badge" />
          </template>
          <div class="message-summary">
            <div class="trade-metric">
              <strong>{{ messages.length }}</strong>
              <span>会话消息总量</span>
            </div>
            <div class="trade-metric">
              <strong>{{ unreadMessageCount }}</strong>
              <span>未读消息</span>
            </div>
          </div>
          <div class="message-layout">
            <div class="card-list message-list">
              <el-empty v-if="messages.length === 0" description="暂无消息" />
              <el-card
                v-for="message in messageCards"
                :key="message.id"
                class="list-item message-card"
                shadow="hover"
                :class="{
                  clickable: canOpenConversation(message),
                  active: currentConversation
                    && currentConversation.propertyId === message.property?.id
                    && currentConversation.otherUserId === getCounterpart(message)?.id,
                }"
                @click="openConversation(message)"
              >
                <div class="item-header">
                  <div class="message-card-head">
                    <span class="property-name">{{ getConversationTitle(message) }}</span>
                    <small v-if="message.property?.title" class="message-property">{{ message.property.title }}</small>
                  </div>
                  <el-tag v-if="message.unreadCount > 0" type="danger" size="small">
                    未读 {{ message.unreadCount }}
                  </el-tag>
                </div>
                <div class="item-content">
                  <p>{{ message.content }}</p>
                  <p>{{ new Date(message.sentAt).toLocaleString() }}</p>
                </div>
                <div v-if="canOpenConversation(message)" class="message-action-hint">
                  点击进入聊天框
                </div>
              </el-card>
            </div>

            <div class="profile-chat-panel">
              <div v-if="currentConversation" class="profile-chat-shell">
                <div class="profile-chat-top">
                  <div>
                    <div class="profile-chat-title">{{ currentConversation.otherUserName }}</div>
                    <div class="profile-chat-subtitle">当前房源：{{ currentConversation.propertyTitle }}</div>
                  </div>
                  <el-button text @click="router.push(`/property/${currentConversation.propertyId}`)">查看房源</el-button>
                </div>

                <div v-loading="chatLoading" class="profile-chat-history">
                  <div v-if="chatHistory.length === 0" class="profile-chat-empty">
                    暂无聊天记录，可以直接发送消息。
                  </div>
                  <div
                    v-for="message in chatHistory"
                    :key="message.id"
                    class="profile-chat-row"
                    :class="{ me: message.isMe }"
                  >
                    <div class="profile-chat-bubble">
                      <div>{{ message.content }}</div>
                      <small>{{ new Date(message.sentAt).toLocaleString() }}</small>
                    </div>
                  </div>
                </div>

                <el-input
                  v-model="replyText"
                  type="textarea"
                  :rows="3"
                  resize="none"
                  placeholder="输入回复内容..."
                />
                <div class="profile-chat-actions">
                  <el-button type="primary" :icon="Promotion" @click="sendReply">发送回复</el-button>
                </div>
              </div>
              <div v-else class="profile-chat-placeholder">
                <el-icon><ChatDotRound /></el-icon>
                <p>点击左侧未读或历史消息，即可进入聊天框查看并回复。</p>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>

      <div class="logout-wrap">
        <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.user-profile {
  max-width: 1180px;
  margin: 0 auto;
}

.hero-card,
.function-list,
.stats-grid {
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(160, 120, 82, 0.16);
  box-shadow: 0 20px 50px rgba(122, 86, 56, 0.12);
  border-radius: 24px;
}

.hero-card {
  padding: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 18px;
}

.hero-avatar {
  background: linear-gradient(135deg, #c7824f, #e0b175);
  color: white;
  font-size: 26px;
  font-weight: 800;
}

.profile-kicker {
  display: inline-flex;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(195, 130, 79, 0.12);
  color: #a4673f;
  font-size: 12px;
  font-weight: 700;
}

.user-details h2 {
  margin: 12px 0 6px;
  color: #5b3d2b;
}

.user-details p {
  margin: 0;
  color: #8d705b;
}

.stats-grid {
  margin: 0 0 20px !important;
  padding: 18px;
}

.stat-item {
  height: 100%;
  padding: 16px;
  text-align: center;
  border-radius: 16px;
  background: rgba(255, 248, 238, 0.9);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-num {
  font-size: 28px;
  font-weight: 900;
  color: #6b4631;
}

.stat-label {
  color: #8b6d57;
}

.function-list {
  padding: 18px;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.list-item {
  border-radius: 18px;
}

.item-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.property-name {
  font-weight: 700;
  color: #5b3d2b;
}

.item-price {
  color: #b46a3a;
  font-weight: 700;
}

.item-content {
  color: #7f624d;
  line-height: 1.7;
}

.item-content p {
  margin: 5px 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.inline-order {
  margin: 10px 0;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(195, 130, 79, 0.08);
}

.trade-summary,
.message-summary {
  margin-bottom: 16px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.trade-metric {
  padding: 16px;
  border-radius: 16px;
  background: linear-gradient(145deg, rgba(255, 247, 238, 0.96), rgba(250, 237, 222, 0.92));
  border: 1px solid rgba(160, 120, 82, 0.14);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.trade-metric strong {
  font-size: 26px;
  color: #6b4631;
}

.trade-metric span {
  color: #8b6d57;
}

.trade-card {
  border: 1px solid rgba(160, 120, 82, 0.14);
}

.message-layout {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(320px, 1.05fr);
  gap: 16px;
  align-items: start;
}

.message-list {
  max-height: 720px;
  overflow-y: auto;
  padding-right: 4px;
}

.message-card {
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.message-card.clickable {
  cursor: pointer;
}

.message-card.clickable:hover,
.message-card.active {
  transform: translateY(-1px);
  border-color: rgba(180, 106, 58, 0.38);
  box-shadow: 0 12px 28px rgba(122, 86, 56, 0.12);
}

.message-card-head {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-property,
.message-action-hint {
  color: #8e715a;
}

.message-action-hint {
  margin-top: 8px;
  font-size: 12px;
}

.profile-chat-panel {
  min-height: 420px;
  border-radius: 20px;
  border: 1px solid rgba(160, 120, 82, 0.16);
  background: rgba(255, 251, 245, 0.82);
  overflow: hidden;
}

.profile-chat-shell {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px;
}

.profile-chat-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.profile-chat-title {
  font-size: 18px;
  font-weight: 800;
  color: #5b3d2b;
}

.profile-chat-subtitle {
  margin-top: 4px;
  color: #8e715a;
  font-size: 13px;
}

.profile-chat-history {
  min-height: 300px;
  max-height: 420px;
  overflow-y: auto;
  padding: 14px;
  border-radius: 16px;
  background: rgba(195, 130, 79, 0.08);
}

.profile-chat-row {
  display: flex;
  margin-bottom: 12px;
}

.profile-chat-row.me {
  justify-content: flex-end;
}

.profile-chat-bubble {
  max-width: 75%;
  padding: 10px 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.94);
  color: #6f5542;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.profile-chat-row.me .profile-chat-bubble {
  background: linear-gradient(135deg, #b46a3a, #d59a63);
  color: #fff;
}

.profile-chat-bubble small {
  opacity: 0.78;
  font-size: 12px;
}

.profile-chat-actions {
  display: flex;
  justify-content: flex-end;
}

.profile-chat-empty,
.profile-chat-placeholder {
  min-height: 300px;
  display: grid;
  place-items: center;
  text-align: center;
  color: #8e715a;
}

.profile-chat-placeholder {
  padding: 24px;
}

.profile-chat-placeholder .el-icon {
  font-size: 40px;
  margin-bottom: 12px;
  color: #b46a3a;
}

.trade-order-no,
.muted-label {
  color: #8e715a;
  font-size: 13px;
}

.trade-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin: 16px 0;
}

.trade-grid div {
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(255, 248, 238, 0.88);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.trade-steps {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.trade-step {
  padding: 11px 10px;
  border-radius: 14px;
  text-align: center;
  color: #876954;
  background: rgba(219, 197, 177, 0.42);
}

.trade-step.finished {
  color: #fffaf3;
  background: linear-gradient(135deg, #b47043, #d59a63);
}

.trade-note,
.trade-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.trade-note {
  color: #7f624d;
  margin-bottom: 12px;
}

.voucher-card {
  margin-top: 14px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 251, 245, 0.96);
  border: 1px dashed rgba(160, 120, 82, 0.34);
  color: #755a46;
}

.voucher-title {
  margin-bottom: 10px;
  font-weight: 800;
  color: #5b3d2b;
}

.timeline {
  margin-top: 8px;
}

.tab-badge {
  margin-left: 8px;
}

.ticket-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.logout-wrap {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 760px) {
  .hero-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .trade-summary,
  .message-summary,
  .trade-grid,
  .trade-steps,
  .message-layout {
    grid-template-columns: 1fr;
  }

  .profile-chat-panel {
    min-height: 320px;
  }
}
</style>
