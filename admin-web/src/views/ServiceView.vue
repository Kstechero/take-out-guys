<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  endServiceSession,
  getServiceMessageList,
  getServiceSessionPage,
  replyServiceMessage
} from '@/api/admin'

type ServiceSession = Record<string, any>
type ServiceMessage = Record<string, any>

const loading = ref(false)
const sessions = ref<ServiceSession[]>([])
const messages = ref<ServiceMessage[]>([])
const keyword = ref('')
const activeSessionId = ref<number | null>(null)
const replyText = ref('')
const sending = ref(false)

const activeSession = computed(
  () => sessions.value.find(session => getSessionId(session) === activeSessionId.value) || null
)

async function loadSessions() {
  loading.value = true
  try {
    const response: any = await getServiceSessionPage({
      page: 1,
      pageSize: 50,
      keyword: keyword.value.trim() || undefined
    })

    sessions.value = response.data?.records || []

    if (!sessions.value.length) {
      activeSessionId.value = null
      messages.value = []
      return
    }

    const stillExists = sessions.value.some(session => getSessionId(session) === activeSessionId.value)
    if (!stillExists) {
      activeSessionId.value = getSessionId(sessions.value[0])
    }

    if (activeSessionId.value) {
      await loadMessages(activeSessionId.value)
    }
  } finally {
    loading.value = false
  }
}

async function loadMessages(sessionId: number) {
  const response: any = await getServiceMessageList({ sessionId })
  const list = response.data?.records || response.data?.list || response.data || []
  messages.value = Array.isArray(list) ? list : []
}

async function selectSession(session: ServiceSession) {
  const sessionId = getSessionId(session)
  if (!sessionId) return
  activeSessionId.value = sessionId
  await loadMessages(sessionId)
}

async function sendReply() {
  if (!activeSessionId.value || !replyText.value.trim() || sending.value) return
  sending.value = true
  try {
    await replyServiceMessage({
      sessionId: activeSessionId.value,
      content: replyText.value.trim(),
      messageType: 'text'
    })
    replyText.value = ''
    await loadMessages(activeSessionId.value)
    await loadSessions()
    ElMessage.success('回复已发送')
  } finally {
    sending.value = false
  }
}

async function refreshActiveMessages() {
  if (!activeSessionId.value) return
  await loadMessages(activeSessionId.value)
}

async function handleEndSession() {
  if (!activeSessionId.value) return
  await endServiceSession(activeSessionId.value)
  ElMessage.success('会话已结束')
  await loadSessions()
}

function getSessionId(session: ServiceSession) {
  return Number(
    session.sessionId ??
      session.id ??
      session.serviceSessionId ??
      0
  )
}

function getSessionName(session: ServiceSession) {
  return session.userName || session.nickname || session.customerName || session.consignee || `用户 #${getSessionId(session)}`
}

function getSessionPreview(session: ServiceSession) {
  return session.lastMessageContent || session.lastContent || session.latestMessage || session.content || '暂无消息'
}

function getSessionTime(session: ServiceSession) {
  return formatDateTime(session.lastMessageTime || session.updateTime || session.createTime)
}

function getSessionUnread(session: ServiceSession) {
  const unread = session.unreadCount ?? session.unread ?? 0
  return Number(unread) || 0
}

function getSessionStatus(session: ServiceSession) {
  const status = Number(session.status ?? session.sessionStatus ?? -1)
  if (status === 1) return '进行中'
  if (status === 0) return '已结束'
  return status >= 0 ? `状态 ${status}` : '未知状态'
}

function getMessageId(message: ServiceMessage, index: number) {
  return message.id || message.messageId || `${index}-${message.createTime || ''}`
}

function getMessageRole(message: ServiceMessage) {
  const raw = String(message.senderType ?? message.role ?? message.messageFrom ?? '').toLowerCase()
  if (['admin', 'service', 'staff', 'assistant'].includes(raw)) return 'service'
  if (['2', 'service'].includes(String(message.senderType))) return 'service'
  return 'user'
}

function getMessageContent(message: ServiceMessage) {
  return message.content || message.messageContent || '空消息'
}

function getMessageTime(message: ServiceMessage) {
  return formatDateTime(message.createTime || message.sendTime || message.messageTime)
}

function formatDateTime(value?: string) {
  if (!value) return '暂无时间'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false })
}

function activeUserInitial() {
  return activeSession.value ? getSessionName(activeSession.value).slice(0, 1) : '?'
}

onMounted(loadSessions)
</script>

<template>
  <div class="service-page card" v-loading="loading">
    <aside>
      <div class="service-title">
        <div>
          <p class="eyebrow">HUMAN SERVICE</p>
          <h2>客服工作台</h2>
        </div>
        <span>{{ sessions.length }} 个会话</span>
      </div>

      <div class="service-search">
        <el-input v-model="keyword" placeholder="搜索用户或会话" @keyup.enter="loadSessions" />
        <el-button type="primary" @click="loadSessions">查询</el-button>
      </div>

      <div v-if="sessions.length">
        <div
          v-for="session in sessions"
          :key="getSessionId(session)"
          class="session"
          :class="{ active: getSessionId(session) === activeSessionId }"
          @click="selectSession(session)"
        >
          <div class="avatar">{{ getSessionName(session).slice(0, 1) }}</div>
          <div>
            <b>{{ getSessionName(session) }}</b>
            <p>{{ getSessionPreview(session) }}</p>
          </div>
          <small>{{ getSessionTime(session) }}</small>
          <i v-if="getSessionUnread(session)">{{ getSessionUnread(session) }}</i>
        </div>
      </div>
      <div v-else class="panel-empty service-empty-list">暂无客服会话</div>
    </aside>

    <section class="service-chat">
      <div v-if="activeSession" class="customer-head">
        <div class="avatar">{{ activeUserInitial() }}</div>
        <div>
          <b>{{ getSessionName(activeSession) }}</b>
          <small>{{ getSessionStatus(activeSession) }} · {{ getSessionTime(activeSession) }}</small>
        </div>
        <el-button @click="refreshActiveMessages">刷新消息</el-button>
        <el-button type="primary" @click="handleEndSession">结束会话</el-button>
      </div>

      <div v-if="activeSession" class="service-messages">
        <div v-if="messages.length">
          <div
            v-for="(message, index) in messages"
            :key="getMessageId(message, index)"
            :class="['service-message', getMessageRole(message)]"
          >
            <div class="service-bubble">
              <small>{{ getMessageRole(message) === 'service' ? '客服' : getSessionName(activeSession) }}</small>
              <p>{{ getMessageContent(message) }}</p>
              <time>{{ getMessageTime(message) }}</time>
            </div>
          </div>
        </div>
        <div v-else class="empty-conversation">
          <span>💬</span>
          <h3>暂无消息记录</h3>
          <p>会话已接通，但后端暂未返回消息内容。</p>
        </div>
      </div>

      <div v-else class="empty-conversation">
        <span>💬</span>
        <h3>选择会话开始处理</h3>
        <p>左侧列表将直接展示后端返回的客服会话。</p>
      </div>

      <div class="reply">
        <el-input
          v-model="replyText"
          placeholder="输入回复内容"
          :disabled="!activeSession || sending"
          @keyup.enter="sendReply"
        />
        <el-button type="primary" :disabled="!activeSession || sending" @click="sendReply">发送</el-button>
      </div>
    </section>

    <aside class="customer-info">
      <p class="eyebrow">SESSION DETAIL</p>
      <h3>会话信息</h3>
      <template v-if="activeSession">
        <dl>
          <dt>会话 ID</dt>
          <dd>{{ getSessionId(activeSession) }}</dd>

          <dt>用户昵称</dt>
          <dd>{{ getSessionName(activeSession) }}</dd>

          <dt>会话状态</dt>
          <dd>{{ getSessionStatus(activeSession) }}</dd>

          <dt>最近消息</dt>
          <dd>{{ getSessionPreview(activeSession) }}</dd>

          <dt>更新时间</dt>
          <dd>{{ getSessionTime(activeSession) }}</dd>
        </dl>
      </template>
      <div v-else class="panel-empty side-empty">暂无选中会话</div>
    </aside>
  </div>
</template>
