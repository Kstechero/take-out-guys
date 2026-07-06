<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getOrderReport, getTop10Report, getTurnoverReport, getUserReport } from '@/api/admin'

type TurnoverReport = {
  dateList?: string
  turnoverList?: string
}

type OrderReport = {
  dateList?: string
  orderCountList?: string
  validOrderCountList?: string
  totalOrderCount?: number
  validOrderCount?: number
  orderCompletionRate?: number
}

type UserReport = {
  dateList?: string
  newUserList?: string
  totalUserList?: string
}

type Top10Report = {
  nameList?: string
  numberList?: string
}

const pad = (n: number) => String(n).padStart(2, '0')
const formatDate = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

const end = new Date()
const begin = new Date(Date.now() - 6 * 86400000)
const dates = ref<[string, string]>([formatDate(begin), formatDate(end)])
const loading = ref(false)

const turnover = ref<TurnoverReport>({})
const orders = ref<OrderReport>({})
const users = ref<UserReport>({})
const top10 = ref<Top10Report>({})

const turnoverRows = computed(() => zipSeries(turnover.value.dateList, turnover.value.turnoverList))
const orderRows = computed(() =>
  zipSeries(orders.value.dateList, orders.value.orderCountList, orders.value.validOrderCountList).map(item => ({
    date: item.date,
    total: item.values[0] || '--',
    valid: item.values[1] || '--'
  }))
)
const userRows = computed(() =>
  zipSeries(users.value.dateList, users.value.newUserList, users.value.totalUserList).map(item => ({
    date: item.date,
    added: item.values[0] || '--',
    total: item.values[1] || '--'
  }))
)
const topRows = computed(() => {
  const names = splitCsv(top10.value.nameList)
  const counts = splitCsv(top10.value.numberList)
  return names.map((name, index) => ({ name, count: counts[index] || '--' }))
})

async function load() {
  loading.value = true
  try {
    const [turnoverRes, orderRes, userRes, topRes]: any[] = await Promise.all([
      getTurnoverReport(...dates.value),
      getOrderReport(...dates.value),
      getUserReport(...dates.value),
      getTop10Report(...dates.value)
    ])

    turnover.value = turnoverRes.data || {}
    orders.value = orderRes.data || {}
    users.value = userRes.data || {}
    top10.value = topRes.data || {}
  } finally {
    loading.value = false
  }
}

function splitCsv(value?: string) {
  return (value || '')
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
}

function zipSeries(dateList?: string, ...series: Array<string | undefined>) {
  const datesArray = splitCsv(dateList)
  const seriesList = series.map(splitCsv)
  return datesArray.map((date, index) => ({
    date,
    values: seriesList.map(list => list[index] || '--')
  }))
}

function formatRate(value?: number) {
  return typeof value === 'number' ? `${(value * 100).toFixed(1)}%` : '--'
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <section class="page-intro">
      <div>
        <p class="eyebrow">BUSINESS ANALYTICS</p>
        <h2>数据统计</h2>
        <p>报表页仅渲染接口返回结果，便于直接校验后端统计口径。</p>
      </div>
      <div class="report-actions">
        <el-date-picker v-model="dates" type="daterange" value-format="YYYY-MM-DD" />
        <el-button type="primary" @click="load">查询</el-button>
      </div>
    </section>

    <section class="report-grid report-grid-wide">
      <article class="card report-card">
        <p class="eyebrow">TURNOVER</p>
        <h3>营业额趋势</h3>
        <strong>{{ turnoverRows.length ? `${turnoverRows.length} 个统计点` : '暂无数据' }}</strong>
        <div v-if="turnoverRows.length" class="data-list">
          <div v-for="row in turnoverRows" :key="row.date" class="data-row">
            <span>{{ row.date }}</span>
            <b>{{ row.values[0] }}</b>
          </div>
        </div>
        <div v-else class="panel-empty">后端返回为空</div>
      </article>

      <article class="card report-card">
        <p class="eyebrow">ORDERS</p>
        <h3>订单统计</h3>
        <strong>{{ orders.totalOrderCount ?? 0 }} 单</strong>
        <small>有效订单 {{ orders.validOrderCount ?? 0 }} · 完成率 {{ formatRate(orders.orderCompletionRate) }}</small>
        <div v-if="orderRows.length" class="data-list">
          <div v-for="row in orderRows" :key="row.date" class="data-row">
            <span>{{ row.date }}</span>
            <b>总 {{ row.total }} / 有效 {{ row.valid }}</b>
          </div>
        </div>
        <div v-else class="panel-empty">后端返回为空</div>
      </article>

      <article class="card report-card">
        <p class="eyebrow">USERS</p>
        <h3>用户增长</h3>
        <strong>{{ userRows.length ? `${userRows.length} 个统计点` : '暂无数据' }}</strong>
        <div v-if="userRows.length" class="data-list">
          <div v-for="row in userRows" :key="row.date" class="data-row">
            <span>{{ row.date }}</span>
            <b>新增 {{ row.added }} / 累计 {{ row.total }}</b>
          </div>
        </div>
        <div v-else class="panel-empty">后端返回为空</div>
      </article>

      <article class="card report-card">
        <p class="eyebrow">TOP 10</p>
        <h3>销量排名</h3>
        <strong>{{ topRows.length ? `${topRows.length} 个菜品` : '暂无数据' }}</strong>
        <div v-if="topRows.length" class="data-list">
          <div v-for="(row, index) in topRows" :key="`${row.name}-${index}`" class="data-row">
            <span>{{ index + 1 }}. {{ row.name }}</span>
            <b>{{ row.count }}</b>
          </div>
        </div>
        <div v-else class="panel-empty">后端返回为空</div>
      </article>
    </section>
  </div>
</template>
