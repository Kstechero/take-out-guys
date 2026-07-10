<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Delete, Refresh, Search, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteReview, getReviewPage, updateReviewStatus } from '@/api/admin'

type ReviewRow = {
  id: number
  userId?: number
  userName?: string
  orderId?: number
  orderNumber?: string
  dishId?: number
  dishName?: string
  dishImage?: string
  rating?: number
  content?: string
  images?: string[]
  likeCount?: number
  status?: number
  aiGenerated?: number
  createTime?: string
  updateTime?: string
}

const loading = ref(false)
const acting = ref(false)
const keyword = ref('')
const statusFilter = ref<number | undefined>(undefined)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const rows = ref<ReviewRow[]>([])
const detailVisible = ref(false)
const activeReview = ref<ReviewRow | null>(null)

const visibleCount = computed(() => rows.value.filter(row => Number(row.status) === 1).length)
const hiddenCount = computed(() => rows.value.filter(row => Number(row.status) === 0).length)
const aiCount = computed(() => rows.value.filter(row => Number(row.aiGenerated) === 1).length)

async function refresh() {
  loading.value = true
  try {
    const response: any = await getReviewPage({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value.trim() || undefined,
      status: statusFilter.value
    })

    const payload = response.data || {}
    rows.value = Array.isArray(payload.records) ? payload.records : []
    total.value = Number(payload.total || 0)
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  page.value = 1
  await refresh()
}

function openDetail(row: ReviewRow) {
  activeReview.value = row
  detailVisible.value = true
}

async function handleToggleStatus(row: ReviewRow) {
  const nextStatus = Number(row.status) === 1 ? 0 : 1
  const actionText = nextStatus === 1 ? '恢复展示' : '隐藏评价'
  await ElMessageBox.confirm(`确认${actionText}这条评价吗？`, '评价处理', {
    type: 'warning',
    confirmButtonText: actionText,
    cancelButtonText: '取消'
  })

  acting.value = true
  try {
    await updateReviewStatus(Number(row.id), nextStatus)
    ElMessage.success(`${actionText}成功`)
    await refresh()
    if (activeReview.value?.id === row.id) {
      activeReview.value = { ...row, status: nextStatus }
    }
  } finally {
    acting.value = false
  }
}

async function handleDelete(row: ReviewRow) {
  await ElMessageBox.confirm('删除后无法恢复，确认删除这条评价吗？', '删除评价', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })

  acting.value = true
  try {
    await deleteReview(Number(row.id))
    ElMessage.success('评价已删除')
    if (activeReview.value?.id === row.id) {
      detailVisible.value = false
      activeReview.value = null
    }
    if (rows.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    await refresh()
  } finally {
    acting.value = false
  }
}

function formatDateTime(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? value.replace('T', ' ').slice(0, 19)
    : date.toLocaleString('zh-CN', { hour12: false })
}

function statusTagType(status?: number) {
  return Number(status) === 1 ? 'success' : 'info'
}

function statusText(status?: number) {
  return Number(status) === 1 ? '展示中' : '已隐藏'
}

function sourceText(aiGenerated?: number) {
  return Number(aiGenerated) === 1 ? 'AI 帮写' : '用户手写'
}

onMounted(refresh)
</script>

<template>
  <div class="reviews-page">
    <section class="page-intro">
      <div>
        <p class="eyebrow">VOICE OF CUSTOMER</p>
        <h2>评价管理</h2>
        <p>查看真实用户评价，处理违规内容，并快速定位菜品、用户和订单上下文。</p>
      </div>
      <el-button type="primary" :icon="Refresh" @click="refresh">刷新列表</el-button>
    </section>

    <section class="review-overview">
      <article class="overview-card warm">
        <span>当前页评价</span>
        <strong>{{ rows.length }}</strong>
        <small>本次查询共命中 {{ total }} 条记录</small>
      </article>
      <article class="overview-card lime">
        <span>展示中</span>
        <strong>{{ visibleCount }}</strong>
        <small>状态为正常展示的评价</small>
      </article>
      <article class="overview-card slate">
        <span>已隐藏</span>
        <strong>{{ hiddenCount }}</strong>
        <small>已被管理员处理的评价</small>
      </article>
      <article class="overview-card peach">
        <span>AI 帮写</span>
        <strong>{{ aiCount }}</strong>
        <small>当前页带 AI 标记的评价</small>
      </article>
    </section>

    <section class="card review-table-card">
      <div class="review-toolbar">
        <el-input
          v-model="keyword"
          :prefix-icon="Search"
          clearable
          placeholder="搜索评价内容、用户、菜品或订单号"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="statusFilter" clearable placeholder="全部状态">
          <el-option label="展示中" :value="1" />
          <el-option label="已隐藏" :value="0" />
        </el-select>
        <el-button @click="handleSearch">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" height="560" empty-text="暂无评价数据">
        <el-table-column label="评价内容" min-width="340">
          <template #default="{ row }">
            <div class="review-content">
              <p>{{ row.content || '-' }}</p>
              <div class="review-meta">
                <el-rate :model-value="Number(row.rating || 0)" disabled text-color="#ff6b35" />
                <el-tag size="small" effect="plain">{{ sourceText(row.aiGenerated) }}</el-tag>
                <span>{{ row.images?.length || 0 }} 张图</span>
                <span>{{ row.likeCount || 0 }} 赞</span>
              </div>
              <div v-if="row.images?.length" class="review-thumbs">
                <el-image
                  v-for="(image, index) in row.images"
                  :key="`${row.id}-${index}`"
                  :src="image"
                  :preview-src-list="row.images"
                  preview-teleported
                  fit="cover"
                />
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="用户 / 订单" min-width="180">
          <template #default="{ row }">
            <div class="info-stack">
              <strong>{{ row.userName || `用户 #${row.userId || '-'}` }}</strong>
              <span>订单号：{{ row.orderNumber || row.orderId || '-' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="关联菜品" min-width="200">
          <template #default="{ row }">
            <div class="dish-cell">
              <el-image v-if="row.dishImage" :src="row.dishImage" fit="cover" preview-teleported />
              <div class="info-stack">
                <strong>{{ row.dishName || '-' }}</strong>
                <span>菜品 ID：{{ row.dishId || '-' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" min-width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="提交时间" min-width="170">
          <template #default="{ row }">
            <span>{{ formatDateTime(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" min-width="220" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
              <el-button link @click="handleToggleStatus(row)">
                {{ Number(row.status) === 1 ? '隐藏' : '恢复' }}
              </el-button>
              <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-foot">
        <span>共 {{ total }} 条评价</span>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          layout="prev, pager, next"
          :total="total"
          @current-change="refresh"
        />
      </div>
    </section>

    <el-drawer v-model="detailVisible" title="评价详情" size="520px">
      <template v-if="activeReview">
        <div class="detail-panel">
          <div class="detail-top">
            <div class="dish-hero">
              <el-image v-if="activeReview.dishImage" :src="activeReview.dishImage" fit="cover" preview-teleported />
              <div class="info-stack">
                <strong>{{ activeReview.dishName || '-' }}</strong>
                <span>菜品 ID：{{ activeReview.dishId || '-' }}</span>
              </div>
            </div>
            <div class="detail-tags">
              <el-tag :type="statusTagType(activeReview.status)">{{ statusText(activeReview.status) }}</el-tag>
              <el-tag effect="plain">{{ sourceText(activeReview.aiGenerated) }}</el-tag>
            </div>
          </div>

          <el-descriptions :column="1" border>
            <el-descriptions-item label="评价用户">
              {{ activeReview.userName || `用户 #${activeReview.userId || '-'}` }}
            </el-descriptions-item>
            <el-descriptions-item label="关联订单">
              {{ activeReview.orderNumber || activeReview.orderId || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="评分">
              <el-rate :model-value="Number(activeReview.rating || 0)" disabled text-color="#ff6b35" />
            </el-descriptions-item>
            <el-descriptions-item label="内容">
              <div class="detail-content">{{ activeReview.content || '-' }}</div>
            </el-descriptions-item>
            <el-descriptions-item label="图片">
              <div v-if="activeReview.images?.length" class="detail-images">
                <el-image
                  v-for="(image, index) in activeReview.images"
                  :key="`${activeReview.id}-${index}`"
                  :src="image"
                  :preview-src-list="activeReview.images"
                  preview-teleported
                  fit="cover"
                />
              </div>
              <span v-else>无图片</span>
            </el-descriptions-item>
            <el-descriptions-item label="点赞数">
              {{ activeReview.likeCount || 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="提交时间">
              {{ formatDateTime(activeReview.createTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="最后处理时间">
              {{ formatDateTime(activeReview.updateTime) }}
            </el-descriptions-item>
          </el-descriptions>

          <div class="detail-actions">
            <el-button @click="handleToggleStatus(activeReview)">
              {{ Number(activeReview.status) === 1 ? '隐藏评价' : '恢复展示' }}
            </el-button>
            <el-button type="danger" :loading="acting" @click="handleDelete(activeReview)">删除评价</el-button>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.reviews-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.review-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.overview-card {
  padding: 20px 22px;
  border-radius: 22px;
  border: 1px solid rgba(237, 229, 219, 0.9);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(249, 244, 238, 0.96));
  box-shadow: 0 12px 28px rgba(16, 26, 42, 0.05);
}

.overview-card span,
.overview-card small {
  display: block;
  color: #8a8073;
}

.overview-card strong {
  display: block;
  margin: 12px 0 8px;
  font-size: 32px;
  color: #1d2430;
}

.overview-card.warm {
  background: linear-gradient(135deg, #fff7ef, #fff1e5);
}

.overview-card.lime {
  background: linear-gradient(135deg, #fffdf6, #f6f8ef);
}

.overview-card.slate {
  background: linear-gradient(135deg, #f7f8fb, #edf1f8);
}

.overview-card.peach {
  background: linear-gradient(135deg, #fff3ee, #ffe7dc);
}

.review-table-card {
  padding: 20px;
}

.review-toolbar {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) 180px 96px;
  gap: 12px;
  margin-bottom: 18px;
}

.review-content p,
.detail-content {
  margin: 0;
  line-height: 1.7;
  color: #2f3742;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.review-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  margin-top: 10px;
  color: #8d8578;
  font-size: 12px;
}

.review-thumbs,
.detail-images {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.review-thumbs :deep(.el-image),
.detail-images :deep(.el-image),
.dish-cell :deep(.el-image),
.dish-hero :deep(.el-image) {
  width: 64px;
  height: 64px;
  border-radius: 14px;
  border: 1px solid rgba(231, 224, 214, 0.9);
  overflow: hidden;
  flex-shrink: 0;
}

.dish-cell,
.dish-hero,
.detail-top,
.detail-tags,
.detail-actions {
  display: flex;
  gap: 12px;
}

.dish-cell,
.detail-top {
  align-items: center;
}

.detail-top,
.detail-actions {
  justify-content: space-between;
}

.detail-actions {
  margin-top: 20px;
}

.info-stack {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.info-stack strong,
.info-stack span {
  overflow-wrap: anywhere;
}

.info-stack strong {
  color: #1d2430;
}

.info-stack span {
  color: #8d8578;
  font-size: 12px;
}

.detail-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

@media (max-width: 1280px) {
  .review-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .review-overview,
  .review-toolbar {
    grid-template-columns: 1fr;
  }

  .detail-top,
  .detail-actions {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
