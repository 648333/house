<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  ChatDotRound,
  Check,
  Close,
  Delete,
  RefreshRight,
  Search,
  Star,
  User as UserIcon,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import * as echarts from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import PropertyService from '@/api/property'
import { getMyMessages, sendMessage } from '@/api/message'
import { deleteReview, getAllReviews } from '@/api/review'
import { getAdminStats } from '@/api/stats'
import { getAllUsers, updateUserStatus } from '@/api/user'
import { exportMlDataset, importMlPredictions } from '@/api/ml'
import { normalizeProperty } from '@/utils/property'

echarts.use([BarChart, LineChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const router = useRouter()
const activeTab = ref('overview')
const currentUser = ref(JSON.parse(localStorage.getItem('user') || '{}'))

const stats = ref({
  users: 0,
  enabledUsers: 0,
  agents: 0,
  admins: 0,
  pendingProperties: 0,
  approvedProperties: 0,
  soldProperties: 0,
  paidOrders: 0,
  pendingOrders: 0,
  failedOrders: 0,
  gmv: 0,
  interactionFunnel: {},
  recentActivity: {},
})

const users = ref([])
const pendingProperties = ref([])
const reviews = ref([])
const messages = ref([])

const userSearch = ref('')
const reviewSearch = ref('')
const currentChatUser = ref(null)
const replyText = ref('')
const mlDatasetSummary = ref(null)
const mlImportJson = ref('')
const mlLoading = ref(false)

const funnelChartRef = ref(null)
const serviceChartRef = ref(null)
let funnelChart = null
let serviceChart = null

const conversionSummary = computed(() => stats.value.interactionFunnel || {})
const serviceSummary = computed(() => ({
  messages: stats.value.messagesTotal || 0,
  appointments: stats.value.appointmentsTotal || 0,
  reviews: stats.value.reviewsTotal || 0,
  requirements: stats.value.requirementsTotal || 0,
  averageRating: stats.value.averageRating || 0,
}))

const fetchUsers = async () => {
  try {
    const response = await getAllUsers()
    users.value = response.data
  } catch (error) {
    ElMessage.error('获取用户列表失败')
  }
}

const fetchPendingProperties = async () => {
  try {
    const response = await PropertyService.getAllProperties()
    pendingProperties.value = response.data
      .filter((item) => item.status !== 'APPROVED')
      .map((item, index) => normalizeProperty(item, index))
  } catch (error) {
    ElMessage.error('获取待审核房源失败')
  }
}

const fetchStats = async () => {
  try {
    const response = await getAdminStats()
    stats.value = response.data
  } catch (error) {
    ElMessage.error('获取管理员统计失败')
  }
}

const fetchMessages = async () => {
  try {
    const response = await getMyMessages()
    messages.value = response.data
  } catch (error) {
    ElMessage.error('获取消息失败')
  }
}

const fetchReviews = async () => {
  try {
    const response = await getAllReviews()
    reviews.value = response.data
  } catch (error) {
    ElMessage.error('获取评论失败')
  }
}

const fetchDashboardData = async () => {
  await Promise.all([fetchUsers(), fetchPendingProperties(), fetchStats(), fetchMessages(), fetchReviews()])
  if (!currentChatUser.value && contacts.value.length > 0) {
    currentChatUser.value = contacts.value[0]
  }
}

const approveProperty = async (id) => {
  try {
    await PropertyService.updateStatus(id, 'APPROVED')
    ElMessage.success('房源已通过审核')
    await Promise.all([fetchPendingProperties(), fetchStats()])
  } catch (error) {
    ElMessage.error('审核失败')
  }
}

const rejectProperty = (id) => {
  ElMessageBox.confirm('确认驳回这套房源吗？', '提示', { type: 'warning' })
    .then(async () => {
      await PropertyService.updateStatus(id, 'REJECTED')
      ElMessage.success('已驳回该房源')
      await Promise.all([fetchPendingProperties(), fetchStats()])
    })
    .catch(() => {})
}

const toggleUserStatus = (row) => {
  const nextEnabled = !row.enabled
  const actionText = nextEnabled ? '启用' : '禁用'

  ElMessageBox.confirm(`确认要${actionText}用户 ${row.username} 吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await updateUserStatus(row.id, nextEnabled)
      row.enabled = nextEnabled
      ElMessage.success(`用户已${actionText}`)
      await fetchStats()
    })
    .catch(() => {})
}

const removeReview = (review) => {
  ElMessageBox.confirm(
    `确认删除 ${review.user?.username || '未知用户'} 对 “${review.property?.title || '未知房源'}” 的这条评论吗？`,
    '删除评论',
    { type: 'warning' },
  )
    .then(async () => {
      await deleteReview(review.id)
      ElMessage.success('评论已删除')
      await fetchReviews()
    })
    .catch(() => {})
}

const sendReply = async () => {
  if (!replyText.value.trim() || !currentChatUser.value) return

  try {
    await sendMessage({
      receiver: { id: currentChatUser.value.id },
      content: replyText.value.trim(),
    })
    replyText.value = ''
    await fetchMessages()
  } catch (error) {
    ElMessage.error('发送回复失败')
  }
}

const handleExportMlDataset = async () => {
  mlLoading.value = true
  try {
    const response = await exportMlDataset()
    const data = response.data
    mlDatasetSummary.value = {
      users: data.users?.length || 0,
      properties: data.properties?.length || 0,
      interactions: data.interactions?.length || 0,
      requirements: data.requirements?.length || 0,
      appointments: data.appointments?.length || 0,
      reviews: data.reviews?.length || 0,
    }

    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `ml-dataset-${new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-')}.json`
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('训练数据已导出')
  } catch (error) {
    ElMessage.error('导出训练数据失败')
  } finally {
    mlLoading.value = false
  }
}

const handleImportMlPredictions = async () => {
  if (!mlImportJson.value.trim()) {
    ElMessage.warning('请先粘贴预测结果 JSON')
    return
  }

  mlLoading.value = true
  try {
    const payload = JSON.parse(mlImportJson.value)
    const response = await importMlPredictions(payload)
    ElMessage.success(`预测结果已导入 ${response.data.imported} 条`)
  } catch (error) {
    ElMessage.error('导入预测结果失败，请检查 JSON 格式')
  } finally {
    mlLoading.value = false
  }
}

const filteredUsers = computed(() => {
  if (!userSearch.value) return users.value
  const keyword = userSearch.value.toLowerCase()
  return users.value.filter((user) => {
    const username = user.username?.toLowerCase() || ''
    const email = user.email?.toLowerCase() || ''
    return username.includes(keyword) || email.includes(keyword)
  })
})

const filteredReviews = computed(() => {
  if (!reviewSearch.value) return reviews.value
  const keyword = reviewSearch.value.toLowerCase()
  return reviews.value.filter((review) => {
    const username = review.user?.username?.toLowerCase() || ''
    const propertyTitle = review.property?.title?.toLowerCase() || ''
    const content = review.content?.toLowerCase() || ''
    return username.includes(keyword) || propertyTitle.includes(keyword) || content.includes(keyword)
  })
})

const contacts = computed(() => {
  const map = new Map()

  messages.value.forEach((message) => {
    const sender = message.sender
    const receiver = message.receiver
    if (!sender || !receiver) return

    const otherUser = sender.id === currentUser.value.id ? receiver : sender
    const previous = map.get(otherUser.id)
    if (!previous || new Date(message.sentAt) > new Date(previous.lastTime)) {
      map.set(otherUser.id, {
        ...otherUser,
        lastTime: message.sentAt,
        lastMessage: message.content,
      })
    }
  })

  return Array.from(map.values()).sort((a, b) => new Date(b.lastTime) - new Date(a.lastTime))
})

const chatHistory = computed(() => {
  if (!currentChatUser.value) return []
  return messages.value
    .filter((message) => {
      const senderId = message.sender?.id
      const receiverId = message.receiver?.id
      return (
        (senderId === currentUser.value.id && receiverId === currentChatUser.value.id)
        || (senderId === currentChatUser.value.id && receiverId === currentUser.value.id)
      )
    })
    .sort((a, b) => new Date(a.sentAt) - new Date(b.sentAt))
})

const reviewCount = computed(() => reviews.value.length)

const renderOverviewCharts = async () => {
  await nextTick()
  if (!funnelChartRef.value || !serviceChartRef.value) return

  if (!funnelChart) {
    funnelChart = echarts.init(funnelChartRef.value)
  }
  if (!serviceChart) {
    serviceChart = echarts.init(serviceChartRef.value)
  }

  const funnelData = [
    conversionSummary.value.views || 0,
    conversionSummary.value.favorites || 0,
    conversionSummary.value.inquiries || 0,
    conversionSummary.value.appointments || 0,
    conversionSummary.value.paidOrders || 0,
  ]

  funnelChart.setOption({
    color: ['#54a6d9', '#5ec8ff'],
    tooltip: { trigger: 'axis' },
    grid: { left: 32, right: 20, top: 24, bottom: 28 },
    xAxis: {
      type: 'category',
      data: ['浏览', '收藏', '咨询', '预约', '支付'],
      axisLabel: { color: '#6e8797' },
      axisLine: { lineStyle: { color: '#d7e7f1' } },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: '#6e8797' },
      splitLine: { lineStyle: { color: '#edf5fa' } },
    },
    series: [
      {
        name: '业务转化',
        type: 'bar',
        barMaxWidth: 38,
        data: funnelData,
        itemStyle: { borderRadius: [8, 8, 0, 0] },
      },
      {
        name: '趋势线',
        type: 'line',
        smooth: true,
        data: funnelData,
      },
    ],
  })

  serviceChart.setOption({
    color: ['#5ec8ff', '#6fc9a8', '#f0b56c', '#d97f6f'],
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: '#6e8797' } },
    series: [
      {
        name: '服务数据',
        type: 'pie',
        radius: ['40%', '66%'],
        center: ['50%', '44%'],
        data: [
          { name: '咨询消息', value: serviceSummary.value.messages },
          { name: '预约记录', value: serviceSummary.value.appointments },
          { name: '用户评论', value: serviceSummary.value.reviews },
          { name: '找房需求', value: serviceSummary.value.requirements },
        ],
        label: { color: '#5e7686' },
      },
    ],
  })
}

const resizeOverviewCharts = () => {
  funnelChart?.resize()
  serviceChart?.resize()
}

onMounted(async () => {
  await fetchDashboardData()
  await renderOverviewCharts()
  window.addEventListener('resize', resizeOverviewCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeOverviewCharts)
  funnelChart?.dispose()
  serviceChart?.dispose()
})

watch(stats, renderOverviewCharts, { deep: true })
</script>

<template>
  <div class="admin-dashboard-container">
    <div class="dashboard-header">
      <div>
        <span class="page-kicker">管理员后台</span>
        <h2>平台数据管理中心</h2>
        <p>审核房源、管理用户、查看运营数据和消息。</p>
      </div>
      <div class="header-actions">
        <el-button :icon="RefreshRight" plain @click="fetchDashboardData">刷新数据</el-button>
        <el-tag type="success" round>系统运行正常</el-tag>
      </div>
    </div>

    <el-row :gutter="18" class="overview-cards">
      <el-col :md="6" :xs="12">
        <el-card class="overview-card">
          <span class="label">用户总数</span>
          <strong>{{ stats.users }}</strong>
        </el-card>
      </el-col>
      <el-col :md="6" :xs="12">
        <el-card class="overview-card">
          <span class="label">启用用户</span>
          <strong>{{ stats.enabledUsers }}</strong>
        </el-card>
      </el-col>
      <el-col :md="6" :xs="12">
        <el-card class="overview-card">
          <span class="label">待审核房源</span>
          <strong>{{ stats.pendingProperties }}</strong>
        </el-card>
      </el-col>
      <el-col :md="6" :xs="12">
        <el-card class="overview-card">
          <span class="label">评论总数</span>
          <strong>{{ reviewCount }}</strong>
        </el-card>
      </el-col>
    </el-row>

    <div class="data-highlight-grid">
      <el-card class="soft-card">
        <span class="mini-kicker">转化漏斗</span>
        <div class="highlight-metric">{{ conversionSummary.views || 0 }}</div>
        <p>浏览量</p>
        <div class="highlight-meta">
          <span>收藏 {{ conversionSummary.favorites || 0 }}</span>
          <span>咨询 {{ conversionSummary.inquiries || 0 }}</span>
          <span>预约 {{ conversionSummary.appointments || 0 }}</span>
        </div>
      </el-card>
      <el-card class="soft-card">
        <span class="mini-kicker">交易概览</span>
        <div class="highlight-metric">{{ stats.paidOrders || 0 }}</div>
        <p>已完成支付</p>
        <div class="highlight-meta">
          <span>进行中 {{ stats.pendingOrders || 0 }}</span>
          <span>异常 {{ stats.failedOrders || 0 }}</span>
          <span>GMV ¥ {{ stats.gmv || 0 }}</span>
        </div>
      </el-card>
      <el-card class="soft-card">
        <span class="mini-kicker">服务质量</span>
        <div class="highlight-metric">{{ serviceSummary.averageRating }}</div>
        <p>平均评分</p>
        <div class="highlight-meta">
          <span>消息 {{ serviceSummary.messages }}</span>
          <span>评论 {{ serviceSummary.reviews }}</span>
          <span>需求 {{ serviceSummary.requirements }}</span>
        </div>
      </el-card>
    </div>

    <el-tabs v-model="activeTab" class="admin-tabs">
      <el-tab-pane label="总览" name="overview">
        <div class="insight-grid">
          <el-card class="soft-card">
            <h3>房源状态</h3>
            <p>已通过审核 {{ stats.approvedProperties }} 套，已成交 {{ stats.soldProperties }} 套。</p>
          </el-card>
          <el-card class="soft-card">
            <h3>角色分布</h3>
            <p>管理员 {{ stats.admins }} 人，经纪人 {{ stats.agents }} 人，普通用户 {{ stats.users - stats.admins - stats.agents }} 人。</p>
          </el-card>
          <el-card class="soft-card">
            <h3>数据分析摘要</h3>
            <p>启用率 {{ stats.activationRate || 0 }}%，成交率 {{ stats.saleRate || 0 }}%，近 7 天咨询 {{ stats.recentActivity?.messages7d || 0 }} 条，预约 {{ stats.recentActivity?.appointments7d || 0 }} 次。</p>
          </el-card>
        </div>

        <div class="overview-chart-grid">
          <el-card class="soft-card chart-card">
            <div class="chart-copy">
              <span class="mini-kicker">可视化漏斗</span>
              <h3>浏览到支付的业务转化路径</h3>
              <p>浏览、收藏、咨询、预约和支付各环节数据，帮助优化运营策略。</p>
            </div>
            <div ref="funnelChartRef" class="overview-chart"></div>
          </el-card>

          <el-card class="soft-card chart-card">
            <div class="chart-copy">
              <span class="mini-kicker">服务分布</span>
              <h3>咨询、预约、评论与需求结构</h3>
              <p>咨询、预约、评论与需求的结构化分析，持续提升服务质量。</p>
            </div>
            <div ref="serviceChartRef" class="overview-chart"></div>
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane label="ML 训练" name="ml">
        <div class="ml-grid">
          <el-card class="soft-card">
            <h3>训练数据导出</h3>
            <p>导出用户、房源、交互、需求、预约、评论数据，给离线推荐模型训练使用。</p>
            <el-button type="primary" :loading="mlLoading" @click="handleExportMlDataset">导出训练数据</el-button>

            <div v-if="mlDatasetSummary" class="ml-summary">
              <span>用户 {{ mlDatasetSummary.users }}</span>
              <span>房源 {{ mlDatasetSummary.properties }}</span>
              <span>交互 {{ mlDatasetSummary.interactions }}</span>
              <span>需求 {{ mlDatasetSummary.requirements }}</span>
              <span>预约 {{ mlDatasetSummary.appointments }}</span>
              <span>评论 {{ mlDatasetSummary.reviews }}</span>
            </div>
          </el-card>

          <el-card class="soft-card">
            <h3>预测结果导入</h3>
            <p>把训练脚本产出的 `predictions.json` 粘贴到这里，系统会回写模型分数。</p>
            <el-input
              v-model="mlImportJson"
              type="textarea"
              :rows="12"
              placeholder='{"modelVersion":"lgbm_v1","modelName":"lightgbm-ranker","predictions":[]}'
            />
            <div class="ml-actions">
              <el-button type="success" :loading="mlLoading" @click="handleImportMlPredictions">导入预测结果</el-button>
            </div>
          </el-card>

          <el-card class="soft-card ml-card-wide">
            <h3>命令行训练流程</h3>
            <div class="ml-code">
              <pre>pip install -r scripts/ml/requirements.txt</pre>
              <pre>$env:ML_API_BASE="http://localhost:8080"</pre>
              <pre>$env:ML_API_TOKEN="管理员JWT"</pre>
              <pre>$env:ML_MODEL_VERSION="lgbm_v1"</pre>
              <pre>python scripts/ml/train_ranker.py</pre>
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane label="房源审核" name="audit">
        <el-table :data="pendingProperties" style="width: 100%">
          <el-table-column label="房源" min-width="260">
            <template #default="scope">
              <div class="property-cell">
                <img :src="scope.row.imageUrl" class="thumb" alt="房源图片" />
                <div>
                  <div class="title">{{ scope.row.title }}</div>
                  <div class="sub">{{ scope.row.address }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="提交人" width="140">
            <template #default="scope">
              {{ scope.row.owner ? scope.row.owner.username : '未知用户' }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="scope">
              <el-tag :type="scope.row.status === 'PENDING' ? 'warning' : (scope.row.status === 'REJECTED' ? 'danger' : 'info')">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220">
            <template #default="scope">
              <el-button size="small" type="success" :icon="Check" @click="approveProperty(scope.row.id)">通过</el-button>
              <el-button size="small" type="danger" :icon="Close" @click="rejectProperty(scope.row.id)">驳回</el-button>
              <el-button size="small" plain @click="router.push(`/property/${scope.row.id}`)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="用户管理" name="users">
        <div class="toolbar">
          <el-input v-model="userSearch" placeholder="搜索用户名或邮箱" style="width: 280px" :prefix-icon="Search" />
        </div>
        <el-table :data="filteredUsers" style="width: 100%">
          <el-table-column label="用户" min-width="220">
            <template #default="scope">
              <div class="user-cell">
                <el-avatar :icon="UserIcon" />
                <div>
                  <div class="title">{{ scope.row.username }}</div>
                  <div class="sub">{{ scope.row.email }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="角色" width="120">
            <template #default="scope">
              <el-tag :type="scope.row.role === 'ADMIN' ? 'danger' : (scope.row.role === 'AGENT' ? 'warning' : 'info')">
                {{ scope.row.role }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="scope">
              <el-tag :type="scope.row.enabled ? 'success' : 'danger'" round>
                {{ scope.row.enabled ? '正常' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="scope">
              <el-button size="small" :type="scope.row.enabled ? 'danger' : 'success'" @click="toggleUserStatus(scope.row)">
                {{ scope.row.enabled ? '禁用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="评论管理" name="reviews">
        <div class="toolbar toolbar-split">
          <el-input
            v-model="reviewSearch"
            placeholder="搜索评论内容、用户名或房源"
            style="width: 320px"
            :prefix-icon="Search"
          />
          <el-button :icon="RefreshRight" plain @click="fetchReviews">刷新评论</el-button>
        </div>
        <el-table :data="filteredReviews" style="width: 100%">
          <el-table-column label="评论用户" width="140">
            <template #default="scope">
              {{ scope.row.user?.username || '未知用户' }}
            </template>
          </el-table-column>
          <el-table-column label="房源" min-width="220">
            <template #default="scope">
              <span class="table-link" @click="router.push(`/property/${scope.row.property?.id}`)">
                {{ scope.row.property?.title || '未知房源' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="评分" width="100">
            <template #default="scope">
              <div class="rating-cell">
                <el-icon><Star /></el-icon>
                <span>{{ scope.row.rating || 0 }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="评论内容" min-width="280">
            <template #default="scope">
              <div class="review-content">{{ scope.row.content || '暂无内容' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="180">
            <template #default="scope">
              {{ scope.row.createdAt ? new Date(scope.row.createdAt).toLocaleString() : '未知时间' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110">
            <template #default="scope">
              <el-button size="small" type="danger" :icon="Delete" plain @click="removeReview(scope.row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="消息中心" name="messages">
        <div class="message-dashboard">
          <div class="contact-sidebar">
            <div class="sidebar-title">站内会话</div>
            <div
              v-for="contact in contacts"
              :key="contact.id"
              class="contact-item"
              :class="{ active: currentChatUser && currentChatUser.id === contact.id }"
              @click="currentChatUser = contact"
            >
              <el-avatar>{{ contact.username?.charAt(0)?.toUpperCase() || '?' }}</el-avatar>
              <div class="contact-main">
                <div class="name-row">
                  <span>{{ contact.username }}</span>
                  <span>{{ new Date(contact.lastTime).toLocaleDateString() }}</span>
                </div>
                <div class="msg-preview">{{ contact.lastMessage }}</div>
              </div>
            </div>
            <el-empty v-if="contacts.length === 0" description="暂时没有消息会话" :image-size="70" />
          </div>

          <div v-if="currentChatUser" class="chat-area">
            <div class="chat-header">
              <div class="chat-title">{{ currentChatUser.username }}</div>
              <div class="chat-subtitle">{{ currentChatUser.email }}</div>
            </div>

            <div class="chat-body">
              <div
                v-for="message in chatHistory"
                :key="message.id"
                :class="['bubble-row', message.sender?.id === currentUser.id ? 'mine' : 'other']"
              >
                <div class="bubble">
                  <div class="bubble-text">{{ message.content }}</div>
                  <div class="bubble-time">{{ new Date(message.sentAt).toLocaleString() }}</div>
                </div>
              </div>
              <el-empty v-if="chatHistory.length === 0" description="还没有和这个用户的聊天记录" :image-size="70" />
            </div>

            <div class="chat-send">
              <el-input v-model="replyText" placeholder="输入管理员回复内容..." @keyup.enter="sendReply">
                <template #append>
                  <el-button @click="sendReply">发送</el-button>
                </template>
              </el-input>
            </div>
          </div>

          <div v-else class="chat-empty">
            <el-icon size="52"><ChatDotRound /></el-icon>
            <p>从左侧选择一个会话开始回复消息。</p>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.admin-dashboard-container {
  padding: 24px;
  max-width: 1280px;
  margin: 0 auto;
  min-height: 100vh;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 16px;
  margin-bottom: 24px;
}

.page-kicker {
  display: inline-flex;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(94, 200, 255, 0.12);
  color: #54a6d9;
  font-size: 12px;
  font-weight: 700;
}

.dashboard-header h2 {
  margin: 14px 0 8px;
  font-size: 30px;
  color: #334b5c;
}

.dashboard-header p {
  margin: 0;
  color: #7b8f9d;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.overview-cards {
  margin-bottom: 20px;
}

.overview-card {
  border-radius: 24px;
}

.overview-card .label {
  display: block;
  color: #8ca0ac;
  margin-bottom: 10px;
}

.overview-card strong {
  font-size: 30px;
  color: #385264;
}

.soft-card {
  border-radius: 22px;
}

.soft-card h3 {
  margin-top: 0;
  color: #355064;
}

.soft-card p {
  color: #728896;
  line-height: 1.8;
}

.data-highlight-grid {
  margin-bottom: 20px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.mini-kicker {
  display: inline-flex;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(94, 200, 255, 0.12);
  color: #54a6d9;
  font-size: 12px;
  font-weight: 700;
}

.highlight-metric {
  margin: 12px 0 8px;
  font-size: 34px;
  font-weight: 900;
  color: #385264;
}

.highlight-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  color: #7b8f9d;
}

.admin-tabs {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 24px;
  padding: 16px;
}

.insight-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.overview-chart-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.chart-card :deep(.el-card__body) {
  display: grid;
  grid-template-columns: minmax(220px, 0.9fr) minmax(280px, 1.1fr);
  gap: 16px;
  align-items: center;
}

.chart-copy h3 {
  margin: 10px 0 8px;
  color: #355064;
}

.overview-chart {
  min-height: 280px;
  width: 100%;
}

.ml-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.ml-card-wide {
  grid-column: 1 / -1;
}

.ml-summary {
  margin-top: 14px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.ml-summary span {
  padding: 8px 12px;
  border-radius: 999px;
  background: #eef8ff;
  color: #4b7894;
  font-size: 13px;
}

.ml-actions {
  margin-top: 14px;
}

.ml-code {
  display: grid;
  gap: 10px;
}

.ml-code pre {
  margin: 0;
  padding: 12px 14px;
  border-radius: 14px;
  background: #f7fbff;
  color: #47606e;
  overflow-x: auto;
}

.toolbar {
  margin-bottom: 16px;
}

.toolbar-split {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.property-cell,
.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.thumb {
  width: 76px;
  height: 58px;
  object-fit: cover;
  border-radius: 12px;
}

.title {
  font-weight: 800;
  color: #355064;
}

.sub {
  color: #8ca0ac;
  font-size: 13px;
}

.table-link {
  color: #3b9fd6;
  cursor: pointer;
  font-weight: 600;
}

.rating-cell {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #f6a623;
  font-weight: 700;
}

.review-content {
  color: #4f6270;
  line-height: 1.6;
}

.message-dashboard {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 18px;
  min-height: 520px;
}

.contact-sidebar,
.chat-area,
.chat-empty {
  background: #fbfdff;
  border-radius: 22px;
  padding: 16px;
}

.sidebar-title {
  font-weight: 800;
  color: #445968;
  margin-bottom: 12px;
}

.contact-item {
  display: flex;
  gap: 10px;
  padding: 10px;
  border-radius: 16px;
  cursor: pointer;
}

.contact-item:hover,
.contact-item.active {
  background: #eef8ff;
}

.contact-main {
  flex: 1;
  min-width: 0;
}

.name-row {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 13px;
  color: #455a64;
}

.msg-preview {
  margin-top: 4px;
  color: #8aa0ad;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-header {
  padding-bottom: 12px;
  border-bottom: 1px solid #eef3f6;
}

.chat-title {
  font-weight: 800;
  color: #324b5c;
}

.chat-subtitle {
  margin-top: 4px;
  color: #8aa0ad;
  font-size: 12px;
}

.chat-body {
  min-height: 360px;
  max-height: 360px;
  overflow-y: auto;
  padding: 16px 0;
}

.bubble-row {
  display: flex;
  margin-bottom: 12px;
}

.bubble-row.mine {
  justify-content: flex-end;
}

.bubble {
  max-width: 72%;
  padding: 12px 15px;
  border-radius: 16px;
  background: #ffffff;
  color: #47606e;
}

.bubble-row.mine .bubble {
  background: #5ec8ff;
  color: white;
}

.bubble-time {
  margin-top: 6px;
  font-size: 11px;
  opacity: 0.75;
}

.chat-send {
  margin-top: 12px;
}

.chat-empty {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #8ba0ac;
}

@media (max-width: 900px) {
  .dashboard-header,
  .data-highlight-grid,
  .insight-grid,
  .overview-chart-grid,
  .ml-grid,
  .message-dashboard {
    grid-template-columns: 1fr;
  }

  .dashboard-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .chart-card :deep(.el-card__body) {
    grid-template-columns: 1fr;
  }
}
</style>
