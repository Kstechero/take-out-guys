<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { getBusinessData, getDishOverview, getDishPage, getOrderOverview, getOrderPage } from '@/api/admin'

type BusinessData = {
  turnover?: number
  validOrderCount?: number
  newUsers?: number
  orderCompletionRate?: number
  unitPrice?: number
}

type OrderOverview = {
  allOrders?: number
  waitingOrders?: number
  completedOrders?: number
  cancelledOrders?: number
  deliveredOrders?: number
}

type DishOverview = {
  sold?: number
  discontinued?: number
}

type OrderRecord = {
  id: number
  number?: string
  orderDishes?: string
  amount?: number
  status?: number
  userName?: string
  orderTime?: string
}

type DishRecord = {
  id: number
  name?: string
  price?: number
  categoryName?: string
  status?: number
  updateTime?: string
}

const loading = ref(false)
const todayLabel = new Intl.DateTimeFormat('zh-CN', { dateStyle: 'full' }).format(new Date())

const business = reactive<BusinessData>({})
const orderOverview = reactive<OrderOverview>({})
const dishOverview = reactive<DishOverview>({})
const orders = ref<OrderRecord[]>([])
const dishes = ref<DishRecord[]>([])

const metrics = computed(() => [
  { label: '营业额', value: formatCurrency(business.turnover), hint: '来自经营总览接口', tone: 'lime' },
  { label: '有效订单', value: formatCount(business.validOrderCount, '单'), hint: '当前统计口径', tone: 'orange' },
  { label: '新增用户', value: formatCount(business.newUsers, '人'), hint: '今日新增', tone: 'blue' },
  { label: '订单完成率', value: formatRate(business.orderCompletionRate), hint: '后端实时计算', tone: 'purple' }
])

const orderSummary = computed(() => [
  { label: '全部订单', value: formatCount(orderOverview.allOrders, '单') },
  { label: '待接单', value: formatCount(orderOverview.waitingOrders, '单') },
  { label: '派送中', value: formatCount(orderOverview.deliveredOrders, '单') },
  { label: '已完成', value: formatCount(orderOverview.completedOrders, '单') },
  { label: '已取消', value: formatCount(orderOverview.cancelledOrders, '单') },
  { label: '在售菜品', value: formatCount(dishOverview.sold, '份') }
])

async function loadDashboard() {
  loading.value = true
  try {
    const [businessRes, orderRes, dishRes, orderPageRes, dishPageRes]: any[] = await Promise.all([
      getBusinessData(),
      getOrderOverview(),
      getDishOverview(),
      getOrderPage({ page: 1, pageSize: 6 }),
      getDishPage({ page: 1, pageSize: 6 })
    ])

    Object.assign(business, businessRes.data || {})
    Object.assign(orderOverview, orderRes.data || {})
    Object.assign(dishOverview, dishRes.data || {})
    orders.value = orderPageRes.data?.records || []
    dishes.value = dishPageRes.data?.records || []
  } finally {
    loading.value = false
  }
}

function formatCurrency(value?: number) {
  return typeof value === 'number' ? `¥${value.toLocaleString('zh-CN', { maximumFractionDigits: 2 })}` : '--'
}

function formatCount(value?: number, suffix = '') {
  return typeof value === 'number' ? `${value}${suffix}` : '--'
}

function formatRate(value?: number) {
  return typeof value === 'number' ? `${(value * 100).toFixed(1)}%` : '--'
}

function formatDateTime(value?: string) {
  if (!value) return '暂无时间'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false })
}

function orderStatusName(status?: number) {
  const map: Record<number, string> = {
    1: '待付款',
    2: '待接单',
    3: '已接单',
    4: '派送中',
    5: '已完成',
    6: '已取消'
  }
  return status ? map[status] || `状态 ${status}` : '未知状态'
}

function dishStatusName(status?: number) {
  if (status === 1) return '起售'
  if (status === 0) return '停售'
  return '未知状态'
}

onMounted(loadDashboard)
</script>

<template>
  <div class="dashboard" v-loading="loading">
    <section class="welcome">
      <div>
        <p class="eyebrow">BUSINESS SNAPSHOT</p>
        <h2>经营总览</h2>
        <p>当前页面只展示后端接口返回的数据，方便直接联调。</p>
      </div>
      <div class="welcome-actions">
        <span class="soft-tag">{{ todayLabel }}</span>
        <el-button type="primary" @click="loadDashboard">刷新数据</el-button>
      </div>
    </section>

    <section class="metric-grid">
      <article v-for="metric in metrics" :key="metric.label" :class="['metric-card', metric.tone]">
        <div class="metric-head">
          <span>{{ metric.label }}</span>
          <i></i>
        </div>
        <strong>{{ metric.value }}</strong>
        <small>{{ metric.hint }}</small>
      </article>
    </section>

    <section class="content-grid">
      <article class="card">
        <div class="card-title">
          <div>
            <p class="eyebrow">ORDER OVERVIEW</p>
            <h3>订单与菜品概况</h3>
          </div>
          <span class="soft-tag">工作台概览接口</span>
        </div>
        <div class="overview-list">
          <div v-for="item in orderSummary" :key="item.label" class="overview-item">
            <span>{{ item.label }}</span>
            <b>{{ item.value }}</b>
          </div>
        </div>
        <div class="overview-list compact">
          <div class="overview-item">
            <span>平均客单价</span>
            <b>{{ formatCurrency(business.unitPrice) }}</b>
          </div>
          <div class="overview-item">
            <span>停售菜品</span>
            <b>{{ formatCount(dishOverview.discontinued, '份') }}</b>
          </div>
        </div>
      </article>

      <article class="card ai-brief">
        <div class="spark">AI</div>
        <p class="eyebrow">INTEGRATION READY</p>
        <h3>联调说明</h3>
        <p>本页不再内置趋势假图和演示摘要，所有指标均来自后端工作台与列表接口。</p>
        <ul>
          <li>
            <span>最近订单</span>
            <b>{{ orders.length }} 条</b>
          </li>
          <li>
            <span>菜品列表</span>
            <b>{{ dishes.length }} 条</b>
          </li>
          <li>
            <span>订单完成率</span>
            <b>{{ formatRate(business.orderCompletionRate) }}</b>
          </li>
        </ul>
        <router-link to="/agent">前往 AI Agent 工作台</router-link>
      </article>

      <article class="card recent">
        <div class="card-title">
          <div>
            <p class="eyebrow">LATEST ORDERS</p>
            <h3>最新订单</h3>
          </div>
          <router-link to="/orders">查看全部</router-link>
        </div>
        <table v-if="orders.length">
          <tbody>
            <tr v-for="order in orders" :key="order.id">
              <td>
                <b>{{ order.orderDishes || '订单明细待返回' }}</b>
                <small>#{{ order.number || order.id }} · {{ order.userName || '匿名用户' }}</small>
              </td>
              <td>{{ formatCurrency(order.amount) }}</td>
              <td>
                <span class="status" :class="orderStatusName(order.status)">{{ orderStatusName(order.status) }}</span>
              </td>
              <td>{{ formatDateTime(order.orderTime) }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="panel-empty">暂无订单数据</div>
      </article>

      <article class="card dish-rank">
        <div class="card-title">
          <div>
            <p class="eyebrow">DISH LIST</p>
            <h3>在售菜品</h3>
          </div>
          <router-link to="/business">查看菜品</router-link>
        </div>
        <div v-if="dishes.length">
          <div v-for="dish in dishes" :key="dish.id" class="rank rank-row">
            <div>
              <b>{{ dish.name || `菜品 #${dish.id}` }}</b>
              <span class="rank-meta">{{ dish.categoryName || '未分类' }} · {{ dishStatusName(dish.status) }}</span>
            </div>
            <strong>{{ formatCurrency(dish.price) }}</strong>
          </div>
        </div>
        <div v-else class="panel-empty">暂无菜品数据</div>
      </article>
    </section>
  </div>
</template>
