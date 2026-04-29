<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { BarChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import * as echarts from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { getAgentStats } from '@/api/stats'
import PropertyService from '@/api/property'
import { getAgentAppointments, updateAppointmentStatus } from '@/api/appointment'
import { createScheduleSlot, getMyScheduleSlots, updateScheduleAvailability } from '@/api/schedule'
import { getAllSupportTickets, updateSupportTicketStatus } from '@/api/ticket'
import { normalizeProperty } from '@/utils/property'

const router = useRouter()

echarts.use([BarChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const activeTab = ref('properties')
const agentStats = ref({
  onSale: 0,
  sold: 0,
  pending: 0,
  views: 0,
  consults: 0,
  payments: { total: 0, paid: 0, paying: 0, failed: 0, paidAmount: 0, conversionRate: 0 },
  messages: { total: 0, unreadIncoming: 0, distinctClients: 0 },
  service: { requirementsAssigned: 0, openRequirements: 0, averageRating: 0, reviews: 0 },
  recentActivity: { messages7d: 0, appointments7d: 0, paidOrders7d: 0 },
})
const properties = ref([])
const appointments = ref([])
const slots = ref([])
const tickets = ref([])
const pendingAppointmentCount = ref(0)
const processingTicketCount = ref(0)
const ticketChartRef = ref(null)
let ticketChart = null

const ticketFilter = ref({
  status: 'ALL',
  priority: 'ALL',
  keyword: '',
})

const statusOptions = [
  { value: 'ALL', label: '全部状态' },
  { value: 'OPEN', label: '待处理' },
  { value: 'PROCESSING', label: '处理中' },
  { value: 'RESOLVED', label: '已解决' },
  { value: 'CLOSED', label: '已关闭' },
]

const priorityOptions = [
  { value: 'ALL', label: '全部优先级' },
  { value: 'URGENT', label: '紧急' },
  { value: 'HIGH', label: '高' },
  { value: 'MEDIUM', label: '普通' },
  { value: 'LOW', label: '低' },
]

const priorityLabelMap = priorityOptions.reduce((map, item) => ({ ...map, [item.value]: item.label }), {})
const statusLabelMap = statusOptions.reduce((map, item) => ({ ...map, [item.value]: item.label }), {})

const slotForm = ref({
  startTime: '',
  endTime: '',
  note: '',
})

const ticketStats = computed(() => ({
  total: tickets.value.length,
  open: tickets.value.filter((item) => item.status === 'OPEN').length,
  processing: tickets.value.filter((item) => item.status === 'PROCESSING').length,
  urgent: tickets.value.filter((item) => item.priority === 'URGENT' || item.priority === 'HIGH').length,
}))

const ticketChartData = computed(() => ({
  status: statusOptions
    .filter((item) => item.value !== 'ALL')
    .map((item) => ({
      name: item.label,
      value: tickets.value.filter((ticket) => ticket.status === item.value).length,
    })),
  priority: priorityOptions
    .filter((item) => item.value !== 'ALL')
    .map((item) => ({
      name: item.label,
      value: tickets.value.filter((ticket) => (ticket.priority || 'MEDIUM') === item.value).length,
    })),
}))

const filteredTickets = computed(() => {
  const keyword = ticketFilter.value.keyword.trim().toLowerCase()

  return tickets.value.filter((ticket) => {
    const statusMatched = ticketFilter.value.status === 'ALL' || ticket.status === ticketFilter.value.status
    const priorityMatched = ticketFilter.value.priority === 'ALL' || ticket.priority === ticketFilter.value.priority
    const keywordMatched = !keyword || [
      ticket.title,
      ticket.description,
      ticket.user?.username,
      ticket.contactMobile,
    ].some((value) => String(value || '').toLowerCase().includes(keyword))

    return statusMatched && priorityMatched && keywordMatched
  })
})

const ticketPriorityType = (priority) => {
  if (priority === 'URGENT') return 'danger'
  if (priority === 'HIGH') return 'warning'
  if (priority === 'LOW') return 'info'
  return ''
}

const ticketStatusType = (status) => {
  if (status === 'RESOLVED') return 'success'
  if (status === 'CLOSED') return 'info'
  if (status === 'PROCESSING') return 'warning'
  return ''
}

const ticketAgeHours = (ticket) => {
  if (!ticket.createdAt) return 0
  const createdAt = new Date(ticket.createdAt).getTime()
  if (Number.isNaN(createdAt)) return 0
  return Math.max(0, Math.round(((Date.now() - createdAt) / 3_600_000) * 100) / 100)
}

const ticketSlaLimit = (ticket) => {
  if (ticket.priority === 'URGENT') return 4
  if (ticket.priority === 'HIGH') return 12
  if (ticket.priority === 'LOW') return 72
  return 24
}

const ticketSlaProgress = (ticket) => {
  if (ticket.status === 'RESOLVED' || ticket.status === 'CLOSED') return 100
  return Math.min(100, Math.round((ticketAgeHours(ticket) / ticketSlaLimit(ticket)) * 100))
}

const ticketSlaStatus = (ticket) => {
  const progress = ticketSlaProgress(ticket)
  if (ticket.status === 'RESOLVED' || ticket.status === 'CLOSED') return 'success'
  if (progress >= 90) return 'exception'
  if (progress >= 70) return 'warning'
  return 'success'
}

const ticketRiskLabel = (ticket) => {
  if (ticket.status === 'RESOLVED' || ticket.status === 'CLOSED') return '已闭环'
  const progress = ticketSlaProgress(ticket)
  if (progress >= 90) return '即将超时'
  if (progress >= 70) return '需要关注'
  return '正常'
}

const fetchAgentPerformance = async () => {
  try {
    const response = await getAgentStats()
    agentStats.value = {
      ...agentStats.value,
      ...response.data,
    }
  } catch (error) {
    ElMessage.error('经营分析加载失败')
  }
}

const renderTicketChart = async () => {
  if (activeTab.value !== 'tickets') return
  await nextTick()
  if (!ticketChartRef.value) return

  if (!ticketChart) {
    ticketChart = echarts.init(ticketChartRef.value)
  }

  ticketChart.setOption({
    color: ['#c7824f', '#d95f4f', '#e0b175', '#7f624d', '#9c6038'],
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: '#7f624d' } },
    grid: { left: 36, right: 20, top: 28, bottom: 58 },
    xAxis: {
      type: 'category',
      data: ticketChartData.value.status.map((item) => item.name),
      axisLabel: { color: '#7f624d' },
      axisLine: { lineStyle: { color: '#e3c7ae' } },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: '#7f624d' },
      splitLine: { lineStyle: { color: '#f0dfcf' } },
    },
    series: [
      {
        name: '状态分布',
        type: 'bar',
        barMaxWidth: 34,
        data: ticketChartData.value.status.map((item) => item.value),
        itemStyle: { borderRadius: [6, 6, 0, 0] },
      },
      {
        name: '优先级分布',
        type: 'pie',
        radius: ['32%', '52%'],
        center: ['78%', '42%'],
        data: ticketChartData.value.priority,
        label: { color: '#7f624d' },
      },
    ],
  })
}

const resizeTicketChart = () => {
  ticketChart?.resize()
}

const fetchMyProperties = async () => {
  try {
    const response = await PropertyService.getMyProperties()
    properties.value = response.data.map((item, index) => normalizeProperty(item, index))
  } catch (error) {
    ElMessage.error('房源加载失败')
  }
}

const fetchAppointments = async () => {
  try {
    const response = await getAgentAppointments()
    appointments.value = response.data
    pendingAppointmentCount.value = appointments.value.filter((item) => item.status === 'PENDING').length
  } catch (error) {
    ElMessage.error('预约记录加载失败')
  }
}

const fetchSlots = async () => {
  try {
    const response = await getMyScheduleSlots()
    slots.value = response.data
  } catch (error) {
    ElMessage.error('排班加载失败')
  }
}

const fetchTickets = async () => {
  try {
    const response = await getAllSupportTickets()
    tickets.value = response.data
    processingTicketCount.value = tickets.value.filter((t) => t.status === 'OPEN' || t.status === 'PROCESSING').length
  } catch (error) {
    ElMessage.error('工单加载失败')
  }
}

const updateAppointment = async (id, status) => {
  try {
    await updateAppointmentStatus(id, status)
    ElMessage.success('预约状态已更新')
    await fetchAppointments()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const markPropertySold = (id) => {
  ElMessageBox.confirm('确认将这套房源标记为已成交吗？', '提示', { type: 'warning' })
    .then(async () => {
      await PropertyService.updateStatus(id, 'SOLD')
      ElMessage.success('房源状态已更新')
      await fetchMyProperties()
    })
    .catch(() => {})
}

const submitSlot = async () => {
  if (!slotForm.value.startTime || !slotForm.value.endTime) {
    ElMessage.warning('请填写开始和结束时间')
    return
  }
  try {
    await createScheduleSlot(slotForm.value)
    ElMessage.success('排班已新增')
    slotForm.value = { startTime: '', endTime: '', note: '' }
    await fetchSlots()
  } catch (error) {
    ElMessage.error(error?.response?.data || '新增失败')
  }
}

const toggleSlot = async (slot) => {
  try {
    await updateScheduleAvailability(slot.id, !slot.available)
    ElMessage.success('排班状态已更新')
    await fetchSlots()
  } catch (error) {
    ElMessage.error('更新失败')
  }
}

const updateTicket = async (ticket, status) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入本次处理说明，用户会在个人中心看到这段反馈。', '工单处理', {
      inputValue: ticket.handlerNote || '',
      confirmButtonText: '提交',
      cancelButtonText: '取消',
      inputType: 'textarea',
    })
    await updateSupportTicketStatus(ticket.id, status, value)
    ElMessage.success('工单状态已更新')
    await fetchTickets()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || error.response?.data || '更新失败')
    }
  }
}

onMounted(async () => {
  await Promise.all([fetchMyProperties(), fetchAppointments(), fetchSlots(), fetchTickets(), fetchAgentPerformance()])
  await renderTicketChart()
  window.addEventListener('resize', resizeTicketChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeTicketChart)
  ticketChart?.dispose()
})

watch([tickets, activeTab], renderTicketChart, { deep: true })
</script>

<template>
  <div class="agent-dashboard-container">
    <div class="dashboard-header">
      <div>
        <span class="page-kicker">经纪人工作台</span>
        <h2>房源管理、预约处理、排班与客服工单一体化</h2>
      </div>
      <div class="header-actions">
        <el-button plain @click="router.push('/profile')">个人中心</el-button>
        <el-button type="primary" @click="router.push('/property/new')">发布房源</el-button>
      </div>
    </div>

    <div class="analytics-overview">
      <div class="analytics-metric">
        <span>线上咨询</span>
        <strong>{{ agentStats.messages.total }}</strong>
        <small>未读 {{ agentStats.messages.unreadIncoming }} 条</small>
      </div>
      <div class="analytics-metric">
        <span>交易转化</span>
        <strong>{{ agentStats.payments.paid }}</strong>
        <small>转化率 {{ agentStats.payments.conversionRate }}%</small>
      </div>
      <div class="analytics-metric">
        <span>成交金额</span>
        <strong>¥ {{ agentStats.payments.paidAmount }}</strong>
        <small>近 7 天支付 {{ agentStats.recentActivity.paidOrders7d }} 笔</small>
      </div>
      <div class="analytics-metric">
        <span>服务口碑</span>
        <strong>{{ agentStats.service.averageRating }}</strong>
        <small>{{ agentStats.service.reviews }} 条评价</small>
      </div>
    </div>

    <div class="funnel-strip">
      <div class="funnel-step"><strong>{{ agentStats.views }}</strong><span>浏览</span></div>
      <div class="funnel-step"><strong>{{ agentStats.interactionFunnel?.favorites || 0 }}</strong><span>收藏</span></div>
      <div class="funnel-step"><strong>{{ agentStats.consults }}</strong><span>咨询</span></div>
      <div class="funnel-step"><strong>{{ agentStats.interactionFunnel?.appointments || 0 }}</strong><span>预约</span></div>
      <div class="funnel-step"><strong>{{ agentStats.payments.paid }}</strong><span>支付</span></div>
    </div>

    <el-tabs v-model="activeTab" class="dashboard-tabs">
      <el-tab-pane name="properties" label="我的房源">
        <el-table :data="properties" style="width: 100%">
          <el-table-column prop="title" label="标题" min-width="240" />
          <el-table-column prop="address" label="地址" min-width="260" />
          <el-table-column prop="price" label="价格" width="120">
            <template #default="scope">¥ {{ scope.row.price }} 万</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="120" />
          <el-table-column label="操作" width="260">
            <template #default="scope">
              <el-button size="small" type="primary" plain @click="router.push(`/property/${scope.row.id}/edit`)">编辑</el-button>
              <el-button size="small" plain @click="router.push(`/property/${scope.row.id}`)">查看</el-button>
              <el-button size="small" type="danger" plain @click="markPropertySold(scope.row.id)">成交</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane name="appointments">
        <template #label>
          <span>预约管理</span>
          <el-badge v-if="pendingAppointmentCount > 0" :value="pendingAppointmentCount" class="tab-badge" />
        </template>
        <el-card v-for="item in appointments" :key="item.id" class="soft-card">
          <div class="row-top">
            <strong>{{ item.property?.title }}</strong>
            <el-tag>{{ item.status }}</el-tag>
          </div>
          <p>客户：{{ item.user?.username }} / {{ item.user?.email }}</p>
          <p>预约时间：{{ new Date(item.appointmentTime).toLocaleString() }}</p>
          <p>留言：{{ item.message || '-' }}</p>
          <div v-if="item.status === 'PENDING'" class="actions">
            <el-button size="small" type="success" @click="updateAppointment(item.id, 'APPROVED')">同意</el-button>
            <el-button size="small" type="danger" @click="updateAppointment(item.id, 'REJECTED')">拒绝</el-button>
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane name="schedule" label="排班日历">
        <el-card class="soft-card">
          <el-form inline>
            <el-form-item label="开始时间"><el-date-picker v-model="slotForm.startTime" type="datetime" /></el-form-item>
            <el-form-item label="结束时间"><el-date-picker v-model="slotForm.endTime" type="datetime" /></el-form-item>
            <el-form-item label="备注"><el-input v-model="slotForm.note" /></el-form-item>
            <el-form-item><el-button type="primary" @click="submitSlot">新增排班</el-button></el-form-item>
          </el-form>
        </el-card>

        <el-table :data="slots" style="margin-top: 12px; width: 100%">
          <el-table-column label="开始" min-width="180"><template #default="scope">{{ new Date(scope.row.startTime).toLocaleString() }}</template></el-table-column>
          <el-table-column label="结束" min-width="180"><template #default="scope">{{ new Date(scope.row.endTime).toLocaleString() }}</template></el-table-column>
          <el-table-column prop="note" label="备注" />
          <el-table-column label="状态" width="120"><template #default="scope"><el-tag :type="scope.row.available ? 'success' : 'info'">{{ scope.row.available ? '可预约' : '关闭' }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="120"><template #default="scope"><el-button size="small" plain @click="toggleSlot(scope.row)">{{ scope.row.available ? '关闭' : '开启' }}</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane name="tickets">
        <template #label>
          <span>客服工单</span>
          <el-badge v-if="processingTicketCount > 0" :value="processingTicketCount" class="tab-badge" />
        </template>
        <el-row :gutter="12" class="ticket-stats">
          <el-col :xs="12" :sm="6"><div class="ticket-stat"><strong>{{ ticketStats.total }}</strong><span>全部工单</span></div></el-col>
          <el-col :xs="12" :sm="6"><div class="ticket-stat"><strong>{{ ticketStats.open }}</strong><span>待处理</span></div></el-col>
          <el-col :xs="12" :sm="6"><div class="ticket-stat"><strong>{{ ticketStats.processing }}</strong><span>处理中</span></div></el-col>
          <el-col :xs="12" :sm="6"><div class="ticket-stat danger"><strong>{{ ticketStats.urgent }}</strong><span>高优先级</span></div></el-col>
        </el-row>

        <el-card class="analytics-card" shadow="never">
          <div class="analytics-copy">
            <span class="panel-kicker">实时分析</span>
            <h3>工单状态与优先级分布</h3>
            <p>结合处理状态和紧急程度，优先把高风险问题推进到闭环。</p>
          </div>
          <div ref="ticketChartRef" class="ticket-chart"></div>
        </el-card>

        <el-card class="soft-card" shadow="never">
          <el-form inline class="ticket-filter">
            <el-form-item label="状态">
              <el-select v-model="ticketFilter.status" style="width: 140px">
                <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="优先级">
              <el-select v-model="ticketFilter.priority" style="width: 140px">
                <el-option v-for="item in priorityOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="搜索">
              <el-input v-model="ticketFilter.keyword" clearable placeholder="标题、用户、电话" style="width: 220px" />
            </el-form-item>
          </el-form>
        </el-card>

        <el-empty v-if="filteredTickets.length === 0" description="暂无匹配工单" />
        <el-card v-for="ticket in filteredTickets" :key="ticket.id" class="soft-card">
          <div class="row-top">
            <strong>{{ ticket.title }}</strong>
            <div class="ticket-tags">
              <el-tag :type="ticketPriorityType(ticket.priority)">{{ priorityLabelMap[ticket.priority] || '普通' }}</el-tag>
              <el-tag :type="ticketStatusType(ticket.status)">{{ statusLabelMap[ticket.status] || ticket.status }}</el-tag>
            </div>
          </div>
          <p>用户：{{ ticket.user?.username }}</p>
          <p>类型：{{ ticket.category }}</p>
          <p>描述：{{ ticket.description }}</p>
          <p>联系方式：{{ ticket.contactMobile || '-' }}</p>
          <p v-if="ticket.handlerNote">处理说明：{{ ticket.handlerNote }}</p>
          <p>更新时间：{{ ticket.updatedAt ? new Date(ticket.updatedAt).toLocaleString() : '-' }}</p>
          <p v-if="ticket.resolvedAt">解决时间：{{ new Date(ticket.resolvedAt).toLocaleString() }}</p>
          <div class="sla-row">
            <span>SLA 风险：{{ ticketRiskLabel(ticket) }} · {{ ticketAgeHours(ticket) }} 小时 / {{ ticketSlaLimit(ticket) }} 小时</span>
            <el-progress :percentage="ticketSlaProgress(ticket)" :status="ticketSlaStatus(ticket)" />
          </div>
          <div class="actions">
            <el-button size="small" plain @click="updateTicket(ticket, 'PROCESSING')">处理中</el-button>
            <el-button size="small" type="success" plain @click="updateTicket(ticket, 'RESOLVED')">已解决</el-button>
            <el-button size="small" type="info" plain @click="updateTicket(ticket, 'CLOSED')">关闭</el-button>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.agent-dashboard-container {
  max-width: 1240px;
  margin: 0 auto;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.page-kicker {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(190, 128, 77, 0.15);
  color: #9c6038;
  font-size: 12px;
  font-weight: 700;
}

.dashboard-header h2 {
  margin: 10px 0 0;
  color: #5f3f2b;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.analytics-overview {
  margin-bottom: 14px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.analytics-metric {
  padding: 18px;
  border-radius: 18px;
  background: linear-gradient(145deg, rgba(255, 248, 238, 0.96), rgba(250, 235, 217, 0.92));
  border: 1px solid rgba(160, 120, 82, 0.14);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.analytics-metric span,
.analytics-metric small {
  color: #8d6e57;
}

.analytics-metric strong {
  font-size: 28px;
  color: #5f3f2b;
}

.funnel-strip {
  margin-bottom: 16px;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.funnel-step {
  padding: 14px;
  border-radius: 16px;
  background: rgba(255, 251, 246, 0.92);
  border: 1px solid rgba(160, 120, 82, 0.14);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.funnel-step strong {
  font-size: 24px;
  color: #9c6038;
}

.funnel-step span {
  color: #8d6e57;
}

.soft-card {
  border-radius: 18px;
  margin-bottom: 12px;
}

.row-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}


.tab-badge {
  margin-left: 8px;
}

.ticket-stats {
  margin-bottom: 12px;
}

.ticket-stat {
  padding: 14px;
  border-radius: 12px;
  background: rgba(255, 248, 238, 0.92);
  border: 1px solid rgba(160, 120, 82, 0.16);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ticket-stat strong {
  color: #6b4631;
  font-size: 24px;
}

.ticket-stat span {
  color: #8b6d57;
}

.ticket-stat.danger strong {
  color: #b43f33;
}

.analytics-card {
  border-radius: 18px;
  margin-bottom: 12px;
}

.analytics-card :deep(.el-card__body) {
  display: grid;
  grid-template-columns: minmax(220px, 0.8fr) minmax(320px, 1.6fr);
  gap: 16px;
  align-items: center;
}

.analytics-copy h3 {
  margin: 10px 0 8px;
  color: #5f3f2b;
}

.analytics-copy p {
  margin: 0;
  color: #8b6d57;
  line-height: 1.7;
}

.panel-kicker {
  display: inline-flex;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(190, 128, 77, 0.15);
  color: #9c6038;
  font-size: 12px;
  font-weight: 700;
}

.ticket-chart {
  min-height: 280px;
  width: 100%;
}

.ticket-filter {
  row-gap: 8px;
}

.ticket-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.sla-row {
  margin-top: 10px;
  padding: 12px;
  border-radius: 12px;
  background: rgba(255, 248, 238, 0.9);
}

.sla-row span {
  display: block;
  margin-bottom: 8px;
  color: #7f624d;
  font-size: 13px;
}

@media (max-width: 900px) {
  .dashboard-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .analytics-overview,
  .funnel-strip {
    grid-template-columns: 1fr 1fr;
  }

  .analytics-card :deep(.el-card__body) {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .analytics-overview,
  .funnel-strip {
    grid-template-columns: 1fr;
  }
}
</style>
