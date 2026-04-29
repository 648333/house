<script setup>
import { computed, defineAsyncComponent, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ChatDotRound,
  Clock,
  Phone,
  Promotion,
  Star,
  StarFilled,
  Switch,
  User,
  Warning,
  Wallet,
  Refresh,
  SuccessFilled,
} from '@element-plus/icons-vue'
import PropertyService from '@/api/property'
import MortgageCalculator from '@/components/MortgageCalculator.vue'
import { createAppointment } from '@/api/appointment'
import { getChatHistory, markChatHistoryAsRead, sendMessage } from '@/api/message'
import { createReview, getPropertyReviews } from '@/api/review'
import { addToCompare, getComparedIds, isCompared, removeFromCompare } from '@/utils/compare'
import { isFavorite, toggleFavorite } from '@/utils/favorites'
import { normalizeProperty } from '@/utils/property'
import { addRecentlyViewed } from '@/utils/recentlyViewed'
import { createPaymentOrder, getPaymentOrder, payPaymentOrder } from '@/api/payment'
import { getAgentScheduleSlots } from '@/api/schedule'
import { subscribePriceAlert, unsubscribePriceAlert } from '@/api/priceAlert'
import { trackInteraction } from '@/api/interaction'
import { formatTag } from '@/utils/tagEmoji'

const PropertyModelViewer = defineAsyncComponent(() => import('@/components/PropertyModelViewer.vue'))

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const property = ref({})
const reviews = ref([])
const isCollected = ref(false)
const inCompare = ref(false)
const compareCount = ref(getComparedIds().length)
const showPhone = ref(false)
const isBookModalVisible = ref(false)
const isChatVisible = ref(false)
const isPayDialogVisible = ref(false)
const scheduleSlots = ref([])
const selectedScheduleSlot = ref(null)
const alertSubscribed = ref(false)
const activeTourSceneIndex = ref(0)
const modelPreviewEnabled = ref(false)
const chatMessage = ref('')
const chatHistory = ref([])
const payLoading = ref(false)
const payPollingTimer = ref(null)
const activeModule = ref('')

const showDescription = computed({ get: () => activeModule.value === 'description', set: (v) => { if (!v) activeModule.value = '' } })
const showTour3d = computed({ get: () => activeModule.value === 'tour3d', set: (v) => { if (!v) activeModule.value = '' } })
const showTags = computed({ get: () => activeModule.value === 'tags', set: (v) => { if (!v) activeModule.value = '' } })
const showBudget = computed({ get: () => activeModule.value === 'budget', set: (v) => { if (!v) activeModule.value = '' } })
const showSchedule = computed({ get: () => activeModule.value === 'schedule', set: (v) => { if (!v) activeModule.value = '' } })
const showRisk = computed({ get: () => activeModule.value === 'risk', set: (v) => { if (!v) activeModule.value = '' } })
const showReviews = computed({ get: () => activeModule.value === 'reviews', set: (v) => { if (!v) activeModule.value = '' } })

const bookForm = ref({
  appointmentTime: '',
  message: '',
  needDeposit: true,
  channel: 'WECHAT',
  depositAmount: '',
})

const paymentOrder = ref(null)

const newReview = ref({
  rating: 5,
  content: '',
})

const currentUser = computed(() => JSON.parse(localStorage.getItem('user') || 'null'))

const paymentChannels = [
  { label: '微信支付', value: 'WECHAT' },
  { label: '支付宝', value: 'ALIPAY' },
  { label: '银行卡', value: 'BANK_CARD' },
]

const orderStatusMeta = computed(() => {
  const status = paymentOrder.value?.status
  if (status === 'PAID') return { text: '支付成功', type: 'success' }
  if (status === 'FAILED') return { text: '支付失败', type: 'danger' }
  if (status === 'CLOSED') return { text: '订单关闭', type: 'info' }
  if (status === 'PAYING') return { text: '支付中', type: 'warning' }
  return { text: '待支付', type: 'info' }
})

const remainSeconds = computed(() => {
  if (!paymentOrder.value?.expireAt) return 0
  const diff = new Date(paymentOrder.value.expireAt).getTime() - Date.now()
  return diff > 0 ? Math.floor(diff / 1000) : 0
})

const remainText = computed(() => {
  const total = remainSeconds.value
  const min = Math.floor(total / 60)
  const sec = total % 60
  return `${String(min).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
})

const suggestedDeposit = computed(() => {
  const priceWan = Number(property.value.price || 0)
  if (!priceWan) return 1000
  const amount = priceWan * 10000 * 0.02
  return Math.min(Math.max(amount, 1000), 50000).toFixed(2)
})

const virtualTourScenes = computed(() => {
  const tags = String(property.value.tags || '').toLowerCase()
  const title = String(property.value.title || '').toLowerCase()
  const base = property.value.imageUrl

  const oceanLike = tags.includes('江景') || tags.includes('海景') || title.includes('江') || title.includes('海')
  const villaLike = tags.includes('别墅') || tags.includes('花园') || title.includes('别墅')
  const loftLike = tags.includes('loft') || title.includes('loft')

  const scenes = [
    {
      key: 'living',
      name: '客厅主视角',
      image: base,
      depth: '270°',
      note: '优先看采光、横厅尺度和窗外视野。',
      hotspots: [
        { label: '采光面', top: '22%', left: '74%' },
        { label: '会客区', top: '60%', left: '38%' },
        { label: '观景位', top: '48%', left: '82%' },
      ],
    },
    {
      key: 'dining',
      name: '餐厨动线',
      image: loftLike
        ? 'https://images.unsplash.com/photo-1484154218962-a197022b5858?auto=format&fit=crop&w=1400&q=80'
        : 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1400&q=80',
      depth: '180°',
      note: '重点看餐厨联动、收纳和岛台尺度。',
      hotspots: [
        { label: '餐厨联动', top: '36%', left: '58%' },
        { label: '收纳面', top: '32%', left: '18%' },
      ],
    },
    {
      key: 'bedroom',
      name: '卧室静区',
      image: villaLike
        ? 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1400&q=80'
        : 'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1400&q=80',
      depth: '150°',
      note: '重点感受私密性、床位尺度和晨间光照。',
      hotspots: [
        { label: '主卧面宽', top: '50%', left: '52%' },
        { label: '衣柜位', top: '26%', left: '20%' },
      ],
    },
  ]

  if (oceanLike || villaLike) {
    scenes.push({
      key: 'view',
      name: '景观阳台',
      image: oceanLike
        ? 'https://images.unsplash.com/photo-1502005229762-cf1b2da7c5d6?auto=format&fit=crop&w=1400&q=80'
        : 'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=1400&q=80',
      depth: '320°',
      note: '这里更能判断景观价值和晚间生活氛围。',
      hotspots: [
        { label: '外摆区', top: '68%', left: '54%' },
        { label: '景观轴', top: '26%', left: '80%' },
      ],
    })
  }

  return scenes
})

const activeTourScene = computed(() => virtualTourScenes.value[activeTourSceneIndex.value] || virtualTourScenes.value[0])
const hasRealTourAssets = computed(() =>
  Boolean(
    property.value.panoramaUrl ||
    property.value.model3dUrl ||
    property.value.floorPlanUrl ||
    property.value.panoramaImages?.length
  )
)

const hasModelPreview = computed(() => Boolean(property.value.model3dUrl))

const tourSteps = computed(() => [
  { title: '入户', desc: '先确认玄关收纳和回家动线是否顺手。' },
  { title: '公区', desc: '看横厅开间、采光和多人活动尺度。' },
  { title: '静区', desc: '确认卧室隔音、卫浴位置和夜间私密性。' },
  { title: '窗外', desc: '最后看景观、楼距和噪音源。' },
])

const switchTourScene = (index) => {
  activeTourSceneIndex.value = index
  if (currentUser.value && property.value.id) {
    trackInteraction({
      propertyId: property.value.id,
      actionType: 'TOUR_SCENE_VIEW',
      source: 'property-detail',
      metadata: virtualTourScenes.value[index]?.name || '',
      weight: 1.6,
    }).catch(() => {})
  }
}

const enableModelPreview = () => {
  modelPreviewEnabled.value = true
  if (currentUser.value && property.value.id) {
    trackInteraction({
      propertyId: property.value.id,
      actionType: 'TOUR_OPEN',
      source: 'property-detail',
      metadata: 'model-preview-open',
      weight: 2.2,
    }).catch(() => {})
  }
}

const budgetReport = computed(() => {
  const totalWan = Number(property.value.price || 0)
  const total = totalWan * 10000
  const downPayment = total * 0.3
  const loan = total - downPayment
  const annualRate = 0.039
  const monthlyRate = annualRate / 12
  const months = 30 * 12
  const monthlyPayment = loan > 0
    ? loan * monthlyRate * Math.pow(1 + monthlyRate, months) / (Math.pow(1 + monthlyRate, months) - 1)
    : 0

  return {
    total,
    downPayment,
    loan,
    monthlyPayment,
  }
})

const fetchProperty = async () => {
  loading.value = true
  try {
    const response = await PropertyService.getProperty(route.params.id)
    property.value = normalizeProperty(response.data, response.data?.id)
    modelPreviewEnabled.value = false
    isCollected.value = isFavorite(response.data?.id)
    inCompare.value = isCompared(response.data?.id)
    compareCount.value = getComparedIds().length
    addRecentlyViewed(response.data?.id)
    bookForm.value.depositAmount = suggestedDeposit.value
    if (response.data?.owner?.id) {
      const slotResp = await getAgentScheduleSlots(response.data.owner.id)
      scheduleSlots.value = Array.isArray(slotResp.data) ? slotResp.data.slice(0, 12) : []
    } else {
      scheduleSlots.value = []
    }
    await fetchReviews()
    activeTourSceneIndex.value = 0
    if (currentUser.value && property.value.id) {
      trackInteraction({
        propertyId: property.value.id,
        actionType: 'TOUR_OPEN',
        source: 'property-detail',
        metadata: 'detail-open',
        weight: 1.2,
      }).catch(() => {})
    }
  } catch (error) {
    ElMessage.error('获取房源详情失败')
  } finally {
    loading.value = false
  }
}

const fetchReviews = async () => {
  try {
    const response = await getPropertyReviews(route.params.id)
    reviews.value = response.data
  } catch (error) {
    console.error('Error fetching reviews:', error)
  }
}

const ensureLogin = (tip = '请先登录') => {
  if (!currentUser.value) {
    ElMessage.warning(tip)
    router.push('/login')
    return false
  }
  return true
}

const handleCollect = () => {
  if (!ensureLogin('登录后可收藏房源')) return
  toggleFavorite(property.value.id)
  isCollected.value = isFavorite(property.value.id)
  trackInteraction({
    propertyId: property.value.id,
    actionType: 'FAVORITE',
    source: 'property-detail',
    metadata: isCollected.value ? 'favorite-on' : 'favorite-off',
    weight: isCollected.value ? 3 : 0.5,
  }).catch(() => {})
  ElMessage.success(isCollected.value ? '已加入收藏' : '已取消收藏')
}

const handleCompare = () => {
  if (inCompare.value) {
    removeFromCompare(property.value.id)
    inCompare.value = false
    compareCount.value = getComparedIds().length
    ElMessage.info('已从对比列表移除')
    return
  }

  const result = addToCompare(property.value.id)
  if (result.reason === 'limit') {
    ElMessage.warning('最多可同时对比 3 套房源')
    return
  }

  inCompare.value = true
  compareCount.value = getComparedIds().length
  trackInteraction({
    propertyId: property.value.id,
    actionType: 'COMPARE',
    source: 'property-detail',
    metadata: 'compare-add',
    weight: 2.5,
  }).catch(() => {})
  ElMessage.success('已加入房源对比')
}

const fetchChatHistoryData = async () => {
  if (!currentUser.value || !property.value.owner?.id) return

  try {
    const response = await getChatHistory(property.value.id, property.value.owner.id)
    chatHistory.value = response.data.map((message) => ({
      ...message,
      isMe: message.sender.id === currentUser.value.id,
    }))

    if (currentUser.value.id !== property.value.owner.id) {
      await markChatHistoryAsRead(property.value.id, property.value.owner.id)
      chatHistory.value = chatHistory.value.map((message) => (
        message.receiver?.id === currentUser.value.id ? { ...message, read: true } : message
      ))
    }

    await nextTick()
    const container = document.querySelector('.chat-history')
    if (container) container.scrollTop = container.scrollHeight
  } catch (error) {
    console.error('Error fetching chat history:', error)
  }
}

const openConsult = async () => {
  if (!ensureLogin('登录后可在线咨询')) return
  isChatVisible.value = true
  trackInteraction({
    propertyId: property.value.id,
    actionType: 'INQUIRY',
    source: 'property-detail',
    metadata: 'open-consult',
    weight: 3.2,
  }).catch(() => {})
  await fetchChatHistoryData()
}

const sendChatMessage = async () => {
  if (!chatMessage.value.trim()) return

  try {
    await sendMessage({
      receiver: { id: property.value.owner.id },
      content: chatMessage.value,
      property: { id: property.value.id },
    })
    chatMessage.value = ''
    await fetchChatHistoryData()
  } catch (error) {
    ElMessage.error('消息发送失败')
  }
}

const openPayDialog = async (orderId) => {
  isPayDialogVisible.value = true
  await refreshOrder(orderId)
  startPayPolling(orderId)
}

const stopPayPolling = () => {
  if (payPollingTimer.value) {
    window.clearInterval(payPollingTimer.value)
    payPollingTimer.value = null
  }
}

const startPayPolling = (orderId) => {
  stopPayPolling()
  payPollingTimer.value = window.setInterval(async () => {
    await refreshOrder(orderId)
    if (!isPayDialogVisible.value) {
      stopPayPolling()
      return
    }

    const status = paymentOrder.value?.status
    if (status === 'PAID' || status === 'FAILED' || status === 'CLOSED' || remainSeconds.value <= 0) {
      stopPayPolling()
    }
  }, 5000)
}

const refreshOrder = async (orderId = paymentOrder.value?.id) => {
  if (!orderId) return
  const response = await getPaymentOrder(orderId)
  paymentOrder.value = response.data
}

const submitBook = async () => {
  if (!ensureLogin('登录后可预约看房')) return
  if (selectedScheduleSlot.value && !bookForm.value.appointmentTime) {
    bookForm.value.appointmentTime = selectedScheduleSlot.value.startTime
  }
  if (!bookForm.value.appointmentTime) {
    ElMessage.warning('请选择预约时间')
    return
  }

  const appointmentPayload = {
    property: { id: property.value.id },
    appointmentTime: bookForm.value.appointmentTime,
    message: bookForm.value.message,
  }

  try {
    const appointmentResponse = await createAppointment(appointmentPayload)

    if (!bookForm.value.needDeposit) {
      ElMessage.success('预约申请已提交')
      isBookModalVisible.value = false
      bookForm.value = {
        appointmentTime: '',
        message: '',
        needDeposit: true,
        channel: 'WECHAT',
        depositAmount: suggestedDeposit.value,
      }
      return
    }

    const paymentResponse = await createPaymentOrder({
      propertyId: property.value.id,
      appointmentId: appointmentResponse.data?.id,
      channel: bookForm.value.channel,
      amount: bookForm.value.depositAmount,
    })

    paymentOrder.value = paymentResponse.data
    isBookModalVisible.value = false
    ElMessage.success('预约成功，请完成意向金支付')
    await openPayDialog(paymentOrder.value.id)

    bookForm.value = {
      appointmentTime: '',
      message: '',
      needDeposit: true,
      channel: 'WECHAT',
      depositAmount: suggestedDeposit.value,
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '预约或支付创建失败，请稍后重试')
  }
}

const confirmPaid = async () => {
  if (!paymentOrder.value?.id) return
  payLoading.value = true
  try {
    const response = await payPaymentOrder(paymentOrder.value.id, { success: true })
    paymentOrder.value = response.data
    if (paymentOrder.value.status === 'PAID') {
      ElMessage.success('支付成功，预约已确认')
      stopPayPolling()
    }
  } catch (error) {
    ElMessage.error('支付确认失败，请重试')
  } finally {
    payLoading.value = false
  }
}

const retryPay = async () => {
  if (!paymentOrder.value?.id) return
  try {
    await refreshOrder(paymentOrder.value.id)
    if (paymentOrder.value.status === 'FAILED' || paymentOrder.value.status === 'CLOSED') {
      const response = await createPaymentOrder({
        propertyId: property.value.id,
        channel: bookForm.value.channel,
        amount: paymentOrder.value.amount,
      })
      paymentOrder.value = response.data
      startPayPolling(paymentOrder.value.id)
      ElMessage.success('已重新生成支付订单')
      return
    }

    ElMessage.info('订单可继续支付')
  } catch (error) {
    ElMessage.error('重试失败，请稍后再试')
  }
}

const togglePriceAlert = async () => {
  if (!ensureLogin('登录后可订阅价格提醒')) return
  try {
    if (alertSubscribed.value) {
      await unsubscribePriceAlert(property.value.id)
      alertSubscribed.value = false
      ElMessage.success('已关闭价格提醒')
    } else {
      await subscribePriceAlert({ propertyId: property.value.id })
      alertSubscribed.value = true
      ElMessage.success('已开启价格提醒')
    }
  } catch (error) {
    ElMessage.error('价格提醒设置失败')
  }
}

const openFurnishingStudio = (tab = 'plan') => {
  router.push({
    path: `/property/${property.value.id}/furnishing-studio`,
    query: tab === 'model' ? { tab: 'model' } : {},
  })
}

const openAssetLink = (url, fallbackText) => {
  if (!url) {
    ElMessage.info(fallbackText)
    return
  }
  window.open(url, '_blank', 'noopener,noreferrer')
}

const exportBudgetReport = () => {
  const report = budgetReport.value
  const lines = [
    '暖寓找房 - 购房预算报告',
    `房源: ${property.value.title}`,
    `总价: ￥${report.total.toFixed(2)}`,
    `建议首付(30%): ￥${report.downPayment.toFixed(2)}`,
    `贷款金额: ￥${report.loan.toFixed(2)}`,
    `预计月供(30年): ￥${report.monthlyPayment.toFixed(2)}`,
    `地址: ${property.value.address || '-'}`,
    `生成时间: ${new Date().toLocaleString()}`,
  ]
  const html = `<!doctype html><html><head><meta charset=\"utf-8\"/><title>预算报告</title></head><body><pre style=\"font-size:16px;line-height:1.8;\">${lines.join('\n')}</pre></body></html>`
  const popup = window.open('', '_blank')
  if (!popup) {
    ElMessage.warning('请允许弹窗后重试导出')
    return
  }
  popup.document.write(html)
  popup.document.close()
  popup.focus()
  popup.print()
}

const submitReview = async () => {
  if (!ensureLogin('登录后可发表评论')) return
  if (!newReview.value.content.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  try {
    await createReview({
      property: { id: property.value.id },
      rating: newReview.value.rating,
      content: newReview.value.content,
    })
    ElMessage.success('评论发布成功')
    newReview.value = { rating: 5, content: '' }
    await fetchReviews()
  } catch (error) {
    ElMessage.error('评论发布失败')
  }
}

const reportProperty = () => {
  ElMessage.info('举报入口已预留，可先通过在线咨询提交详细问题')
}

const closePayDialog = () => {
  isPayDialogVisible.value = false
  stopPayPolling()
}

onMounted(fetchProperty)
onBeforeUnmount(stopPayPolling)
</script>

<template>
  <div v-loading="loading" class="property-detail-container">
    <template v-if="property.id">
      <el-breadcrumb separator="/" class="breadcrumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/properties' }">房源列表</el-breadcrumb-item>
        <el-breadcrumb-item>{{ property.title }}</el-breadcrumb-item>
      </el-breadcrumb>

      <!-- ===== 紧凑顶部信息 ===== -->
      <div class="compact-hero">
        <img :src="property.imageUrl" class="compact-hero-img" alt="房源主图" />
        <div class="compact-hero-info">
          <div class="compact-hero-top">
            <span class="page-kicker">房源详情</span>
            <div class="compact-hero-tags">
              <el-tag type="success" round size="small">真实房源</el-tag>
              <el-tag type="warning" round size="small">{{ property.type || '品质住宅' }}</el-tag>
              <el-tag type="info" round size="small">{{ property.status === 'SOLD' ? '已成交' : '可预约' }}</el-tag>
            </div>
          </div>
          <h1 class="compact-title">{{ property.title }}</h1>
          <div class="compact-price-row">
            <span class="compact-price">¥ {{ property.price }} 万</span>
            <span class="compact-meta">{{ property.layout || '舒适户型' }} / {{ property.area || 120 }} ㎡ / {{ property.orientation || '南向' }}</span>
          </div>
          <div class="compact-address">
            <span>{{ property.address }}</span>
            <span class="compact-extra">{{ property.decoration || '精装修' }} · {{ property.floor || '中高层' }} · {{ property.yearBuilt || '近年建成' }}</span>
          </div>
          <div class="compact-actions">
            <el-button type="primary" size="small" :icon="ChatDotRound" @click="openConsult">在线咨询</el-button>
            <el-button type="warning" size="small" plain :icon="Wallet" @click="isBookModalVisible = true">预约支付</el-button>
            <el-button size="small" plain :icon="Phone" @click="showPhone = !showPhone">{{ showPhone ? '138-0000-0000' : '获取电话' }}</el-button>
            <el-button size="small" plain @click="handleCollect">
              <el-icon><StarFilled v-if="isCollected" /><Star v-else /></el-icon>
              {{ isCollected ? '已收藏' : '收藏' }}
            </el-button>
            <el-button size="small" plain :icon="Switch" @click="handleCompare">{{ inCompare ? '移出对比' : '对比' }}</el-button>
            <el-button size="small" plain @click="togglePriceAlert">{{ alertSubscribed ? '关闭降价提醒' : '降价提醒' }}</el-button>
          </div>
        </div>
      </div>

      <!-- ===== 功能模块卡片网格 ===== -->
      <div class="module-grid">
        <div class="module-card" @click="activeModule = 'description'">
          <div class="module-icon">📝</div>
          <div class="module-label">房源描述</div>
          <div class="module-hint">查看详细信息</div>
        </div>
        <div class="module-card" @click="activeModule = 'tour3d'">
          <div class="module-icon">🏠</div>
          <div class="module-label">3D 看房</div>
          <div class="module-hint">沉浸式导览</div>
        </div>
        <div v-if="property.tags" class="module-card" @click="activeModule = 'tags'">
          <div class="module-icon">🏷️</div>
          <div class="module-label">房源亮点</div>
          <div class="module-hint">{{ property.tags.split(',').length }} 个标签</div>
        </div>
        <div class="module-card" @click="openFurnishingStudio('plan')">
          <div class="module-icon">🪑</div>
          <div class="module-label">软装工作室</div>
          <div class="module-hint">家具摆位 · 3D</div>
        </div>
        <div class="module-card" @click="activeModule = 'budget'">
          <div class="module-icon">💰</div>
          <div class="module-label">预算测算</div>
          <div class="module-hint">月供 ¥{{ Math.round(budgetReport.monthlyPayment) }}</div>
        </div>
        <div class="module-card" @click="activeModule = 'schedule'">
          <div class="module-icon">📅</div>
          <div class="module-label">看房排班</div>
          <div class="module-hint">{{ scheduleSlots.length ? scheduleSlots.length + ' 个时段' : '暂无排班' }}</div>
        </div>
        <div class="module-card" @click="activeModule = 'risk'">
          <div class="module-icon">⚠️</div>
          <div class="module-label">风险披露</div>
          <div class="module-hint">交易提示</div>
        </div>
        <div class="module-card" @click="activeModule = 'reviews'">
          <div class="module-icon">💬</div>
          <div class="module-label">住户评论</div>
          <div class="module-hint">{{ reviews.length }} 条评论</div>
        </div>
      </div>

      <!-- ===== 模块详情弹窗 ===== -->

      <!-- 房源描述 -->
      <el-dialog v-model="showDescription" title="房源描述" width="600px">
        <p class="description-text">
          {{ property.description || '房源信息完整、居住节奏舒适，适合进一步预约实地看房。' }}
        </p>
      </el-dialog>

      <!-- 3D 看房 -->
      <el-dialog v-model="showTour3d" title="3D 看房与导览" width="900px" top="4vh">
        <div v-if="hasModelPreview" class="embedded-viewer-wrap">
          <div v-if="!modelPreviewEnabled" class="model-preview-entry">
            <div>
              <div class="model-preview-title">3D 实景看房</div>
              <p>点击加载真实 3D 模型，身临其境感受空间布局。</p>
            </div>
            <div class="model-preview-actions">
              <el-button type="primary" @click="enableModelPreview">加载 3D 看房</el-button>
              <el-button plain @click="openFurnishingStudio('model')">进入完整 3D 看房页</el-button>
            </div>
          </div>
          <template v-else>
            <PropertyModelViewer
              :model-url="property.model3dUrl"
              :poster="property.imageUrl"
              :title="`${property.title || '房源'} 3D 看房`"
              :compact="true"
            />
            <div class="embedded-viewer-tip">
              <span>拖动旋转、滚轮缩放，像真正看房一样感受空间。</span>
              <el-button type="primary" plain @click="openFurnishingStudio('model')">进入完整 3D 看房页</el-button>
            </div>
          </template>
        </div>
        <div class="tour-section-label">导览场景说明</div>
        <div class="tour-panel">
          <div class="tour-stage">
            <img :src="activeTourScene.image" class="tour-image" alt="沉浸式看房场景" />
            <div class="tour-overlay">
              <span class="tour-badge">3D 看房</span>
              <div class="tour-headline">
                <strong>{{ activeTourScene.name }}</strong>
                <span>{{ activeTourScene.depth }} 视角</span>
              </div>
              <p>{{ activeTourScene.note }}</p>
            </div>
            <button
              v-for="spot in activeTourScene.hotspots"
              :key="`${activeTourScene.key}-${spot.label}`"
              class="tour-hotspot"
              :style="{ top: spot.top, left: spot.left }"
              type="button"
            >
              <span></span>
              <em>{{ spot.label }}</em>
            </button>
          </div>
          <div class="tour-sidebar">
            <div class="tour-scene-list">
              <button
                v-for="(scene, index) in virtualTourScenes"
                :key="scene.key"
                class="tour-scene-btn"
                :class="{ active: activeTourSceneIndex === index }"
                type="button"
                @click="switchTourScene(index)"
              >
                <strong>{{ scene.name }}</strong>
                <span>{{ scene.depth }} 视角</span>
              </button>
            </div>
            <div class="tour-steps">
              <div v-for="step in tourSteps" :key="step.title" class="tour-step">
                <strong>{{ step.title }}</strong>
                <span>{{ step.desc }}</span>
              </div>
            </div>
          </div>
        </div>
        <div v-if="hasRealTourAssets" class="tour-assets">
          <a v-if="property.panoramaUrl" :href="property.panoramaUrl" target="_blank" rel="noreferrer">打开 360 全景资源</a>
          <a v-if="property.model3dUrl" :href="property.model3dUrl" target="_blank" rel="noreferrer">打开 3D 模型资源</a>
          <a v-if="property.floorPlanUrl" :href="property.floorPlanUrl" target="_blank" rel="noreferrer">查看户型图资源</a>
          <span v-if="property.panoramaImages?.length">全景场景数 {{ property.panoramaImages.length }}</span>
        </div>
      </el-dialog>

      <!-- 房源亮点 -->
      <el-dialog v-model="showTags" title="房源亮点" width="500px">
        <div class="tag-list">
          <el-tag v-for="tag in (property.tags || '').split(',')" :key="tag" round size="large">{{ formatTag(tag) }}</el-tag>
        </div>
      </el-dialog>

      <!-- 预算测算 -->
      <el-dialog v-model="showBudget" title="预算测算" width="640px">
        <MortgageCalculator :total-price="Number(property.price || 0)" />
        <div class="report-actions">
          <el-button type="primary" plain @click="exportBudgetReport">导出预算报告（PDF）</el-button>
        </div>
      </el-dialog>

      <!-- 看房排班 -->
      <el-dialog v-model="showSchedule" title="看房排班" width="500px">
        <el-empty v-if="!scheduleSlots.length" description="顾问暂未配置排班，可先发起预约" />
        <div v-else class="slot-list">
          <el-radio-group v-model="selectedScheduleSlot">
            <el-radio
              v-for="slot in scheduleSlots"
              :key="slot.id"
              :label="slot"
              border
              class="slot-item"
            >
              {{ new Date(slot.startTime).toLocaleString() }}
            </el-radio>
          </el-radio-group>
        </div>
      </el-dialog>

      <!-- 风险披露 -->
      <el-dialog v-model="showRisk" title="风险披露" width="540px">
        <div class="risk-grid">
          <div class="risk-item"><strong>产权年限</strong><span>以不动产权证登记信息为准</span></div>
          <div class="risk-item"><strong>交易税费</strong><span>按当地政策和房源性质核算</span></div>
          <div class="risk-item"><strong>贷款政策</strong><span>以银行实时审批条件为准</span></div>
          <div class="risk-item"><strong>价格说明</strong><span>页面价格可能随市场与业主策略调整</span></div>
        </div>
      </el-dialog>

      <!-- 住户评论 -->
      <el-dialog v-model="showReviews" title="住户评论" width="600px">
        <div v-if="reviews.length" class="review-list">
          <div v-for="review in reviews" :key="review.id" class="review-item">
            <div class="review-header">
              <div class="review-user">
                <el-avatar :size="30" :icon="User" />
                <span>{{ review.user?.username || '匿名用户' }}</span>
              </div>
              <el-rate v-model="review.rating" disabled size="small" />
            </div>
            <p>{{ review.content }}</p>
          </div>
        </div>
        <el-empty v-else description="还没有评论，欢迎留下你的看房感受" />
        <div class="review-editor">
          <el-rate v-model="newReview.rating" />
          <el-input
            v-model="newReview.content"
            type="textarea"
            :rows="3"
            placeholder="补充空间体验、采光、社区氛围等真实感受"
          />
          <el-button type="primary" class="review-btn" @click="submitReview">发布评论</el-button>
        </div>
      </el-dialog>

      <!-- ===== 原有弹窗保留 ===== -->
      <el-dialog v-model="isBookModalVisible" title="预约看房与支付" width="460px">
        <el-form label-position="top">
          <el-form-item label="预约时间">
            <el-date-picker v-model="bookForm.appointmentTime" type="datetime" placeholder="选择日期时间" style="width: 100%" />
          </el-form-item>
          <el-form-item label="留言备注">
            <el-input v-model="bookForm.message" type="textarea" :rows="3" placeholder="可提前说明你最关心的问题" />
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="bookForm.needDeposit">本次预约同步支付意向金（推荐）</el-checkbox>
          </el-form-item>
          <template v-if="bookForm.needDeposit">
            <el-form-item label="支付渠道">
              <el-radio-group v-model="bookForm.channel">
                <el-radio-button v-for="item in paymentChannels" :key="item.value" :label="item.value">
                  {{ item.label }}
                </el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="意向金金额（元）">
              <el-input v-model="bookForm.depositAmount" type="number" placeholder="建议金额" />
              <div class="deposit-tip">建议金额：¥ {{ suggestedDeposit }}</div>
            </el-form-item>
          </template>
        </el-form>
        <template #footer>
          <el-button @click="isBookModalVisible = false">取消</el-button>
          <el-button type="primary" @click="submitBook">确认提交</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="isPayDialogVisible" title="订单支付" width="520px" @close="closePayDialog">
        <div v-if="paymentOrder" class="pay-panel">
          <div class="pay-top">
            <div><div class="pay-subject">{{ paymentOrder.subject }}</div></div>
            <el-tag :type="orderStatusMeta.type" round>{{ orderStatusMeta.text }}</el-tag>
          </div>
          <div class="pay-body">
            <div class="pay-price">¥ {{ paymentOrder.amount }}</div>
            <div class="pay-hint">请在倒计时内完成支付</div>
            <div class="pay-timer" :class="{ danger: remainSeconds <= 60 }">
              <el-icon><Clock /></el-icon>
              <span>剩余 {{ remainText }}</span>
            </div>
            <div class="pay-qr"><div class="qr-box">扫码支付</div></div>
            <div v-if="paymentOrder.failureReason" class="pay-error">{{ paymentOrder.failureReason }}</div>
          </div>
          <div class="pay-actions">
            <el-button :icon="Refresh" @click="refreshOrder()">刷新状态</el-button>
            <el-button v-if="paymentOrder.status !== 'PAID'" type="primary" :icon="SuccessFilled" :loading="payLoading" @click="confirmPaid">确认已支付</el-button>
            <el-button v-if="paymentOrder.status === 'FAILED' || paymentOrder.status === 'CLOSED'" type="warning" plain @click="retryPay">重新发起支付</el-button>
          </div>
        </div>
      </el-dialog>

      <el-dialog v-model="isChatVisible" title="在线咨询" width="520px">
        <div class="chat-header">当前咨询房源：{{ property.title }}</div>
        <div class="chat-history">
          <div v-for="message in chatHistory" :key="message.id" :class="['chat-msg', message.isMe ? 'me' : 'other']">
            <div class="msg-bubble">{{ message.content }}</div>
          </div>
          <div v-if="chatHistory.length === 0" class="empty-chat">可直接询问采光、楼层和看房时段等细节。</div>
        </div>
        <el-input v-model="chatMessage" placeholder="输入消息..." @keyup.enter="sendChatMessage">
          <template #append>
            <el-button :icon="Promotion" @click="sendChatMessage" />
          </template>
        </el-input>
      </el-dialog>
    </template>
  </div>
</template>

<style scoped>
.property-detail-container {
  max-width: 1240px;
  margin: 0 auto;
  padding-bottom: 24px;
}

.breadcrumb {
  margin-bottom: 14px;
}

.page-kicker {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(199, 140, 83, 0.22);
  color: #875a39;
  font-size: 12px;
  font-weight: 700;
}

/* ===== 紧凑顶部 ===== */
.compact-hero {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 20px;
  padding: 18px;
  border-radius: 24px;
  background: linear-gradient(150deg, rgba(255, 247, 235, 0.88), rgba(250, 238, 220, 0.94));
  border: 1px solid rgba(174, 129, 90, 0.24);
  box-shadow: 0 16px 40px rgba(146, 99, 58, 0.12);
}

.compact-hero-img {
  width: 280px;
  height: 200px;
  object-fit: cover;
  border-radius: 18px;
}

.compact-hero-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.compact-hero-top {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.compact-hero-tags {
  display: flex;
  gap: 6px;
}

.compact-title {
  margin: 0;
  font-size: 22px;
  color: #4f3324;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.compact-price-row {
  display: flex;
  align-items: baseline;
  gap: 14px;
  flex-wrap: wrap;
}

.compact-price {
  font-size: 26px;
  font-weight: 900;
  color: #a6542d;
}

.compact-meta {
  color: #8b6a54;
  font-size: 14px;
}

.compact-address {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  color: #7f5f48;
  font-size: 13px;
}

.compact-extra {
  color: #9b775d;
}

.compact-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 4px;
}

/* ===== 功能模块网格 ===== */
.module-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.module-card {
  padding: 20px 16px;
  border-radius: 20px;
  background: linear-gradient(160deg, rgba(255, 250, 242, 0.92), rgba(250, 240, 226, 0.88));
  border: 1px solid rgba(176, 129, 90, 0.16);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  text-align: center;
}

.module-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(146, 99, 58, 0.16);
  border-color: rgba(176, 129, 90, 0.3);
}

.module-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.module-label {
  font-size: 15px;
  font-weight: 700;
  color: #4f3324;
}

.module-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #9b775d;
}

/* ===== 弹窗内部样式 ===== */
.description-text {
  margin: 0;
  line-height: 1.9;
  color: #6f5542;
}

.tag-list {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.embedded-viewer-wrap {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 18px;
}

.model-preview-entry {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
  padding: 20px;
  border-radius: 22px;
  background: linear-gradient(145deg, rgba(255, 249, 241, 0.96), rgba(250, 238, 223, 0.92));
  border: 1px solid rgba(177, 136, 96, 0.18);
}

.model-preview-title {
  font-size: 18px;
  font-weight: 800;
  color: #5a3926;
}

.model-preview-entry p {
  margin: 8px 0 0;
  color: #7b5a43;
  line-height: 1.7;
}

.model-preview-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.embedded-viewer-tip {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(194, 150, 107, 0.12);
  color: #755641;
}

.tour-section-label {
  margin: 2px 0 14px;
  display: inline-flex;
  width: fit-content;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(194, 150, 107, 0.12);
  color: #8a5d3d;
  font-size: 12px;
  font-weight: 700;
}

.tour-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(220px, 0.7fr);
  gap: 18px;
}

.tour-stage {
  position: relative;
  min-height: 320px;
  border-radius: 22px;
  overflow: hidden;
  background: #1f140e;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.16);
}

.tour-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scale(1.02);
}

.tour-stage::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(28, 18, 11, 0.15), rgba(28, 18, 11, 0.55));
}

.tour-overlay {
  position: absolute;
  left: 20px;
  right: 20px;
  bottom: 20px;
  z-index: 2;
  color: #fff9f4;
}

.tour-badge {
  display: inline-flex;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(8px);
  font-size: 12px;
  font-weight: 700;
}

.tour-headline {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.tour-headline strong {
  font-size: 20px;
}

.tour-overlay p {
  margin: 8px 0 0;
  max-width: 32rem;
  line-height: 1.7;
  font-size: 13px;
}

.tour-hotspot {
  position: absolute;
  z-index: 2;
  transform: translate(-50%, -50%);
  background: transparent;
  border: none;
  color: #fff;
  cursor: default;
}

.tour-hotspot span {
  display: block;
  width: 14px;
  height: 14px;
  border-radius: 999px;
  background: #f4c27b;
  box-shadow: 0 0 0 6px rgba(244, 194, 123, 0.22);
}

.tour-hotspot em {
  display: inline-flex;
  margin-top: 8px;
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(24, 16, 11, 0.62);
  font-style: normal;
  font-size: 11px;
  white-space: nowrap;
}

.tour-sidebar {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tour-assets {
  margin-top: 16px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.tour-assets a,
.tour-assets span {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(194, 150, 107, 0.14);
  color: #6f503a;
  text-decoration: none;
  font-size: 13px;
}

.tour-scene-list,
.tour-steps {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tour-scene-btn,
.tour-step {
  padding: 10px;
  border-radius: 12px;
  border: 1px solid rgba(176, 129, 90, 0.16);
  background: rgba(255, 248, 239, 0.92);
}

.tour-scene-btn {
  text-align: left;
  cursor: pointer;
}

.tour-scene-btn.active {
  background: linear-gradient(145deg, rgba(210, 166, 118, 0.26), rgba(234, 196, 142, 0.18));
  border-color: rgba(176, 129, 90, 0.32);
}

.tour-scene-btn strong,
.tour-step strong {
  display: block;
  color: #5c3d2c;
  font-size: 13px;
}

.tour-scene-btn span,
.tour-step span {
  display: block;
  margin-top: 4px;
  color: #7d6049;
  line-height: 1.5;
  font-size: 12px;
}

.risk-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.risk-item {
  border-radius: 12px;
  padding: 12px;
  background: rgba(211, 170, 128, 0.15);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.risk-item strong {
  color: #69452e;
}

.risk-item span {
  color: #7f5f48;
  font-size: 13px;
}

.review-item {
  padding: 14px 0;
  border-bottom: 1px solid rgba(171, 132, 97, 0.2);
}

.review-item:last-child {
  border-bottom: none;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.review-user {
  display: flex;
  align-items: center;
  gap: 8px;
}

.review-editor {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-btn {
  align-self: flex-start;
}

.report-actions {
  margin-top: 12px;
}

.slot-list :deep(.el-radio-group) {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.slot-item {
  margin-right: 0;
}

.deposit-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #946f52;
}

.pay-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pay-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.pay-subject {
  font-size: 16px;
  font-weight: 700;
  color: #4f3324;
}

.pay-body {
  border-radius: 18px;
  padding: 16px;
  background: rgba(212, 171, 125, 0.16);
  border: 1px solid rgba(180, 135, 95, 0.26);
}

.pay-price {
  font-size: 34px;
  font-weight: 900;
  color: #a6542d;
}

.pay-hint {
  margin-top: 8px;
  color: #7f5d46;
  font-size: 13px;
}

.pay-timer {
  margin-top: 12px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(204, 160, 115, 0.24);
  color: #76553c;
}

.pay-timer.danger {
  background: rgba(205, 109, 72, 0.2);
  color: #93492b;
}

.pay-qr {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

.qr-box {
  height: 112px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: repeating-linear-gradient(45deg, rgba(203, 158, 111, 0.32), rgba(203, 158, 111, 0.32) 8px, rgba(183, 136, 94, 0.22) 8px, rgba(183, 136, 94, 0.22) 16px);
  color: #6f4f38;
  font-weight: 700;
}

.pay-error {
  margin-top: 12px;
  color: #ffb9ca;
}

.pay-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.chat-header {
  margin-bottom: 10px;
  color: #7d5d45;
}

.chat-history {
  min-height: 260px;
  max-height: 320px;
  overflow-y: auto;
  padding: 16px;
  margin-bottom: 12px;
  border-radius: 16px;
  background: rgba(210, 168, 122, 0.14);
}

.chat-msg {
  display: flex;
  margin-bottom: 12px;
}

.chat-msg.me {
  justify-content: flex-end;
}

.msg-bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 14px;
  background: rgba(255, 250, 242, 0.95);
}

.chat-msg.me .msg-bubble {
  background: linear-gradient(135deg, #b46a3a, #d6a25f);
  color: white;
}

.empty-chat {
  color: #8c6b53;
}

@media (max-width: 980px) {
  .compact-hero {
    grid-template-columns: 1fr;
  }

  .compact-hero-img {
    width: 100%;
    height: 180px;
  }

  .module-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .tour-panel {
    grid-template-columns: 1fr;
  }

  .risk-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .module-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .compact-actions {
    flex-direction: column;
  }
}
</style>
