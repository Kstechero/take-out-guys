<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  endServiceSession,
  getServiceMessageList,
  getServiceSessionPage,
  replyServiceMessage
} from '@/api/admin'

type ServiceSession = {
  id?: number
  userId?: number
  userName?: string
  source?: string
  status?: number
  lastMessage?: string
  lastMessageTime?: string
  createTime?: string
  updateTime?: string
}

type ServiceMessage = {
  id?: number
  sessionId?: number
  senderType?: string
  senderId?: number
  senderName?: string
  messageType?: string
  content?: string
  flagged?: number
  createTime?: string
}

const OPEN_STATUS = 1
const CLOSED_STATUS = 2
const POLL_INTERVAL = 5000

const loading = ref(false)
const sessions = ref<ServiceSession[]>([])
const messages = ref<ServiceMessage[]>([])
const keyword = ref('')
const statusFilter = ref<number | undefined>(OPEN_STATUS)
const activeSessionId = ref<number | null>(null)
const replyText = ref('')
const sending = ref(false)
const ending = ref(false)
const lastMessageId = ref<number | null>(null)
const messageListRef = ref<HTMLElement | null>(null)
let pollTimer: number | null = null

const activeSession = computed(
  () => sessions.value.find(session => getSessionId(session) === activeSessionId.value) || null
)

const activeSessionClosed = computed(
  () => !activeSession.value || Number(activeSession.value.status) === CLOSED_STATUS
)

const sessionCountText = computed(() => `${sessions.value.length} 个会话`)

function startPolling() {
  stopPolling()
  pollTimer = window.setInterval(async () => {
    try {
      await refreshSessions(false)
      if (activeSessionId.value) {
        await loadMessages(activeSessionId.value, true)
      }
    } catch (error) {
      console.error('Service polling failed', error)
    }
  }, POLL_INTERVAL)
}

function stopPolling() {
  if (pollTimer !== null) {
    window.clearInterval(pollTimer)
    pollTimer = null
  }
}

async function refreshSessions(showLoading = true) {
  if (showLoading) loading.value = true
  try {
    const response: any = await getServiceSessionPage({
      page: 1,
      pageSize: 50,
      status: statusFilter.value,
      keyword: keyword.value.trim() || undefined
    })

    const list = response.data?.records || response.data?.result || []
    sessions.value = Array.isArray(list) ? list : []

    if (!sessions.value.length) {
      activeSessionId.value = null
      messages.value = []
      lastMessageId.value = null
      return
    }

    const stillExists = sessions.value.some(session => getSessionId(session) === activeSessionId.value)
    if (!stillExists) {
      activeSessionId.value = getSessionId(sessions.value[0])
    }
  } finally {
    if (showLoading) loading.value = false
  }
}

async function loadMessages(sessionId: number, incremental = false) {
  const response: any = await getServiceMessageList({
    sessionId,
    lastMessageId: incremental ? (lastMessageId.value ?? undefined) : undefined
  })
  const list = Array.isArray(response.data) ? response.data : []

  if (incremental) {
    if (!list.length) return
    messages.value = messages.value.concat(list)
  } else {
    messages.value = list
  }

  const latest = messages.value[messages.value.length - 1]
  lastMessageId.value = latest?.id ? Number(latest.id) : null
  await scrollMessagesToBottom()
}

async function selectSession(session: ServiceSession) {
  const sessionId = getSessionId(session)
  if (!sessionId) return
  activeSessionId.value = sessionId
  replyText.value = ''
  messages.value = []
  lastMessageId.value = null
  await loadMessages(sessionId)
}

async function handleSearch() {
  await refreshSessions()
  if (activeSessionId.value) {
    await loadMessages(activeSessionId.value)
  }
}

async function sendReply() {
  if (!activeSessionId.value || !replyText.value.trim() || sending.value || activeSessionClosed.value) return
  sending.value = true
  try {
    await replyServiceMessage({
      sessionId: activeSessionId.value,
      content: replyText.value.trim(),
      messageType: 'text'
    })
    replyText.value = ''
    await refreshSessions(false)
    await loadMessages(activeSessionId.value)
    ElMessage.success('回复已发送')
  } finally {
    sending.value = false
  }
}

async function refreshActiveMessages() {
  if (!activeSessionId.value) return
  await refreshSessions(false)
  await loadMessages(activeSessionId.value)
}

async function handleEndSession() {
  if (!activeSessionId.value || activeSessionClosed.value || ending.value) return
  await ElMessageBox.confirm('结束后该会话将不能继续回复，是否继续？', '结束会话', {
    type: 'warning',
    confirmButtonText: '结束会话',
    cancelButtonText: '取消'
  })

  ending.value = true
  try {
    await endServiceSession(activeSessionId.value)
    ElMessage.success('会话已结束')
    await refreshSessions(false)
    if (activeSessionId.value) {
      await loadMessages(activeSessionId.value)
    }
  } finally {
    ending.value = false
  }
}

function getSessionId(session: ServiceSession) {
  return Number(session.id || 0)
}

function getSessionName(session: ServiceSession) {
  return session.userName || `用户 #${getSessionId(session)}`
}

function getSessionPreview(session: ServiceSession) {
  return session.lastMessage || '暂无消息'
}

function getSessionTime(session: ServiceSession) {
  return formatDateTime(session.lastMessageTime || session.updateTime || session.createTime)
}

function getSessionStatus(session: ServiceSession) {
  const status = Number(session.status ?? -1)
  if (status === OPEN_STATUS) return '进行中'
  if (status === CLOSED_STATUS) return '已结束'
  return status >= 0 ? `状态 ${status}` : '未知状态'
}

function getSessionTagType(session: ServiceSession) {
  return Number(session.status) === OPEN_STATUS ? 'success' : 'info'
}

function getSessionSource(session: ServiceSession) {
  if (session.source === 'miniapp') return 'UniApp'
  return session.source || '未知来源'
}

function getMessageId(message: ServiceMessage, index: number) {
  return message.id || `${index}-${message.createTime || ''}`
}

function getMessageRole(message: ServiceMessage) {
  return String(message.senderType || '').toLowerCase() === 'admin' ? 'service' : 'user'
}

function getMessageSender(message: ServiceMessage) {
  if (getMessageRole(message) === 'service') return message.senderName || '客服'
  return message.senderName || getSessionName(activeSession.value || {})
}

function getMessageContent(message: ServiceMessage) {
  return message.content || '空消息'
}

function getMessageTime(message: ServiceMessage) {
  return formatDateTime(message.createTime)
}

function isFlaggedMessage(message: ServiceMessage) {
  return Number(message.flagged || 0) > 0
}

function formatDateTime(value?: string) {
  if (!value) return '暂无时间'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value.replace('T', ' ').slice(0, 19) : date.toLocaleString('zh-CN', { hour12: false })
}

async function scrollMessagesToBottom() {
  await nextTick()
  const container = messageListRef.value
  if (!container) return
  container.scrollTop = container.scrollHeight
}

watch(activeSessionId, async sessionId => {
  if (!sessionId) {
    messages.value = []
    lastMessageId.value = null
    return
  }
  await loadMessages(sessionId)
})

onMounted(async () => {
  await refreshSessions()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<template>
  <div class="service-page card" v-loading="loading">
    <aside class="session-list">
      <div class="service-title">
        <div>
          <p class="eyebrow">HUMAN SERVICE</p>
          <h2>客服工作台</h2>
        </div>
        <span>{{ sessionCountText }}</span>
      </div>

      <div class="service-search">
        <el-input v-model="keyword" clearable placeholder="搜索用户或消息关键词" @keyup.enter="handleSearch" />
        <el-select v-model="statusFilter" clearable placeholder="全部状态">
          <el-option label="进行中" :value="1" />
          <el-option label="已结束" :value="2" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>

      <div v-if="sessions.length" class="session-items">
        <button
          v-for="session in sessions"
          :key="getSessionId(session)"
          type="button"
          class="session"
          :class="{ active: getSessionId(session) === activeSessionId }"
          @click="selectSession(session)"
        >
          <div class="avatar">{{ getSessionName(session).slice(0, 1) }}</div>
          <div class="session-meta">
            <div class="session-headline">
              <b>{{ getSessionName(session) }}</b>
              <el-tag size="small" :type="getSessionTagType(session)">{{ getSessionStatus(session) }}</el-tag>
            </div>
            <p>{{ getSessionPreview(session) }}</p>
            <small>{{ getSessionSource(session) }} · {{ getSessionTime(session) }}</small>
          </div>
        </button>
      </div>
      <div v-else class="panel-empty service-empty-list">暂无客服会话</div>
    </aside>

    <section class="service-chat">
      <div v-if="activeSession" class="customer-head">
        <div class="customer-main">
          <div class="avatar">{{ getSessionName(activeSession).slice(0, 1) }}</div>
          <div>
            <b>{{ getSessionName(activeSession) }}</b>
            <small>{{ getSessionStatus(activeSession) }} · {{ getSessionTime(activeSession) }}</small>
          </div>
        </div>
        <div class="customer-actions">
          <el-button @click="refreshActiveMessages">刷新消息</el-button>
          <el-button type="primary" :disabled="activeSessionClosed || ending" @click="handleEndSession">
            {{ ending ? '结束中...' : '结束会话' }}
          </el-button>
        </div>
      </div>

      <div v-if="activeSession" ref="messageListRef" class="service-messages">
        <div v-if="messages.length">
          <div
            v-for="(message, index) in messages"
            :key="getMessageId(message, index)"
            :class="['service-message', getMessageRole(message)]"
          >
            <div class="service-bubble">
              <small>{{ getMessageSender(message) }}</small>
              <p>{{ getMessageContent(message) }}</p>
              <div class="message-foot">
                <time>{{ getMessageTime(message) }}</time>
                <span v-if="isFlaggedMessage(message)" class="flagged-tag">敏感词拦截</span>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="empty-conversation">
          <h3>暂无消息记录</h3>
          <p>当前会话还没有聊天内容。</p>
        </div>
      </div>

      <div v-else class="empty-conversation is-large">
        <h3>选择会话开始处理</h3>
        <p>左侧会展示 UniApp 用户提交的人工客服会话。</p>
      </div>

      <div class="reply">
        <el-input
          v-model="replyText"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          resize="none"
          placeholder="输入回复内容"
          :disabled="!activeSession || activeSessionClosed || sending"
          @keyup.ctrl.enter="sendReply"
        />
        <el-button type="primary" :disabled="!activeSession || activeSessionClosed || sending" @click="sendReply">
          {{ sending ? '发送中...' : '发送' }}
        </el-button>
      </div>
    </section>

    <aside class="customer-info">
      <p class="eyebrow">SESSION DETAIL</p>
      <h3>会话信息</h3>
      <template v-if="activeSession">
        <dl>
          <dt>会话 ID</dt>
          <dd>{{ getSessionId(activeSession) }}</dd>

          <dt>用户 ID</dt>
          <dd>{{ activeSession.userId || '-' }}</dd>

          <dt>用户昵称</dt>
          <dd>{{ getSessionName(activeSession) }}</dd>

          <dt>来源</dt>
          <dd>{{ getSessionSource(activeSession) }}</dd>

          <dt>会话状态</dt>
          <dd>{{ getSessionStatus(activeSession) }}</dd>

          <dt>最近消息</dt>
          <dd>{{ getSessionPreview(activeSession) }}</dd>

          <dt>创建时间</dt>
          <dd>{{ formatDateTime(activeSession.createTime) }}</dd>

          <dt>更新时间</dt>
          <dd>{{ getSessionTime(activeSession) }}</dd>
        </dl>
      </template>
      <div v-else class="panel-empty side-empty">暂无选中会话</div>
    </aside>
  </div>
</template>

<style scoped>
.service-page {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr) 260px;
  gap: 20px;
  min-height: calc(100vh - 160px);
}

.session-list,
.service-chat,
.customer-info {
  min-height: 0;
}

.session-list,
.customer-info {
  padding: 24px;
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(247, 245, 239, 0.96));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.75);
}

.service-chat {
  display: flex;
  flex-direction: column;
  min-width: 0;
  border-radius: 28px;
  background: linear-gradient(180deg, #fffaf3 0%, #f5efe5 100%);
  border: 1px solid rgba(185, 137, 91, 0.16);
  overflow: hidden;
}

.service-title,
.customer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.18em;
  color: #8f826a;
}

.service-title h2,
.customer-info h3 {
  margin: 0;
}

.service-title span {
  color: #8a7d68;
  font-size: 13px;
}

.service-search {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 18px;
}

.service-search :deep(.el-input),
.service-search :deep(.el-select),
.service-search .el-button {
  width: 100%;
}

.service-search .el-button {
  min-height: 42px;
}

.service-search :deep(.el-input__wrapper),
.service-search :deep(.el-select__wrapper) {
  min-height: 42px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 0 0 1px rgba(201, 188, 166, 0.65) inset;
}

.service-search :deep(.el-input__inner),
.service-search :deep(.el-select__placeholder),
.service-search :deep(.el-select__selected-item),
.service-search :deep(.el-input__inner::placeholder) {
  color: #3f372f;
}

.session-items {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.session {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  width: 100%;
  padding: 14px;
  border: 1px solid rgba(201, 188, 166, 0.55);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  text-align: left;
  cursor: pointer;
  transition: 0.2s ease;
}

.session:hover,
.session.active {
  border-color: rgba(255, 107, 53, 0.48);
  box-shadow: 0 16px 30px rgba(255, 107, 53, 0.1);
  transform: translateY(-1px);
}

.avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: linear-gradient(135deg, #ff8a5b, #ff5b2e);
  color: #fff;
  font-weight: 700;
  flex-shrink: 0;
}

.session-meta,
.session-meta p,
.session-meta small {
  min-width: 0;
}

.session-headline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.session-headline b,
.session-meta p,
.session-meta small,
.service-bubble p,
.customer-info dd {
  overflow-wrap: anywhere;
}

.session-meta p {
  margin: 8px 0 6px;
  color: #5f5a52;
}

.session-meta small {
  color: #9a8f7d;
}

.customer-head {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(185, 137, 91, 0.16);
  background: rgba(255, 255, 255, 0.78);
}

.customer-main,
.customer-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.customer-main small {
  display: block;
  margin-top: 4px;
  color: #8f826a;
}

.service-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background:
    radial-gradient(circle at top left, rgba(255, 173, 120, 0.16), transparent 28%),
    radial-gradient(circle at bottom right, rgba(255, 92, 46, 0.1), transparent 24%);
}

.service-message {
  display: flex;
  margin-bottom: 18px;
}

.service-message.user {
  justify-content: flex-start;
}

.service-message.service {
  justify-content: flex-end;
}

.service-bubble {
  max-width: 78%;
  padding: 14px 16px;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 12px 24px rgba(72, 52, 29, 0.08);
}

.service-message.service .service-bubble {
  background: linear-gradient(135deg, #ff6b35, #ff8b5f);
  color: #fff;
}

.service-bubble small {
  display: block;
  margin-bottom: 8px;
  color: #8f826a;
  font-size: 12px;
}

.service-message.service .service-bubble small,
.service-message.service .message-foot,
.service-message.service time {
  color: rgba(255, 255, 255, 0.8);
}

.service-bubble p {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.65;
}

.message-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
  color: #8f826a;
  font-size: 12px;
}

.flagged-tag {
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(216, 67, 21, 0.12);
  color: #d84315;
}

.reply {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 96px;
  gap: 12px;
  padding: 18px 24px 24px;
  border-top: 1px solid rgba(185, 137, 91, 0.16);
  background: rgba(255, 255, 255, 0.9);
}

.empty-conversation,
.panel-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 180px;
  padding: 24px;
  border-radius: 18px;
  color: #8f826a;
  text-align: center;
  background: rgba(255, 255, 255, 0.7);
}

.empty-conversation {
  flex-direction: column;
  flex: 1;
  margin: 24px;
}

.empty-conversation.is-large {
  min-height: 420px;
}

.empty-conversation h3 {
  margin: 0 0 8px;
  color: #42392f;
}

.empty-conversation p {
  margin: 0;
}

.customer-info dl {
  margin: 20px 0 0;
}

.customer-info dt {
  margin-top: 14px;
  color: #8f826a;
  font-size: 13px;
}

.customer-info dd {
  margin: 6px 0 0;
  color: #332d27;
}

@media (max-width: 1280px) {
  .service-page {
    grid-template-columns: 280px minmax(0, 1fr);
  }

  .customer-info {
    grid-column: 1 / -1;
  }
}

@media (max-width: 900px) {
  .service-page {
    grid-template-columns: 1fr;
  }

  .service-search,
  .reply {
    grid-template-columns: 1fr;
  }

  .customer-head,
  .customer-actions,
  .customer-main {
    align-items: flex-start;
    flex-direction: column;
  }

  .service-bubble {
    max-width: 100%;
  }
}
</style>
