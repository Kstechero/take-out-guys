<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { Promotion, Delete, MagicStick, Document, TrendCharts } from '@element-plus/icons-vue'
import {
  getAiHealth,
  getAdminAgentMessages,
  getAdminAgentSessions,
  getBusinessData,
  getOrderOverview,
  getOrderStatistics,
  getServiceSessionPage,
  deleteAdminAgentSession,
  resumeAdminAgentChat,
  streamAdminAgentChat
} from '@/api/admin'

type Confirmation = {
  token: string
  action?: string
  summary: string
  expires_at?: string
  expiresAt?: string
  details?: Record<string, unknown>
}
type Message = { role: 'user' | 'assistant'; content: string; confirmation?: Confirmation | null }

const welcomeMessage: Message = {
  role: 'assistant',
  content: '我是管理端 AI Agent。你可以直接查询经营数据、订单状态、平台规则，返回内容将来自后端 Agent 接口。'
}

const prompts = [
  '分析今天订单趋势',
  '哪些菜品值得重点推广？',
  '新增菜品需要提供哪些信息？',
  '帮我创建一张优惠券',
  '总结待处理的客服问题'
]
const messages = ref<Message[]>([{ ...welcomeMessage }])

const input = ref('')
const sending = ref(false)
const chat = ref<HTMLElement>()
const sessionId = ref<number | null>(null)
const aiConnected = ref(false)

const context = reactive({
  turnover: undefined as number | undefined,
  validOrderCount: undefined as number | undefined,
  waitingOrders: undefined as number | undefined,
  completedOrders: undefined as number | undefined,
  toBeConfirmed: undefined as number | undefined,
  deliveryInProgress: undefined as number | undefined,
  serviceSessions: undefined as number | undefined
})

async function loadContext() {
  const [healthRes, businessRes, overviewRes, statisticsRes, serviceRes]: any[] = await Promise.all([
    getAiHealth().catch(() => null),
    getBusinessData().catch(() => null),
    getOrderOverview().catch(() => null),
    getOrderStatistics().catch(() => null),
    getServiceSessionPage({ page: 1, pageSize: 1 }).catch(() => null)
  ])

  aiConnected.value = ['ok', 'UP'].includes(healthRes?.data?.status)
  Object.assign(context, businessRes?.data || {})
  Object.assign(context, overviewRes?.data || {})
  Object.assign(context, statisticsRes?.data || {})
  context.serviceSessions = serviceRes?.data?.total
}

async function restoreConversation() {
  try {
    const sessionsResponse: any = await getAdminAgentSessions()
    const sessions = Array.isArray(sessionsResponse?.data) ? sessionsResponse.data : []
    const latestSessionId = Number(sessions[0]?.id || 0)
    if (!latestSessionId) return

    const messagesResponse: any = await getAdminAgentMessages(latestSessionId)
    const history = Array.isArray(messagesResponse?.data) ? messagesResponse.data : []
    const restored = history.filter(
      (item: any) => ['user', 'assistant'].includes(item?.role) && typeof item?.content === 'string'
    ) as Message[]

    sessionId.value = latestSessionId
    messages.value = restored.length ? restored : [{ ...welcomeMessage }]
    await nextTick()
    chat.value?.scrollTo({ top: chat.value.scrollHeight })
  } catch {
    messages.value = [{ ...welcomeMessage }]
    sessionId.value = null
  }
}

async function send(text = input.value) {
  if (!text.trim() || sending.value) return

  const prompt = text.trim()
  messages.value.push({ role: 'user', content: prompt })
  input.value = ''
  sending.value = true

  const answer: Message = { role: 'assistant', content: '', confirmation: null }
  messages.value.push(answer)
  await nextTick()
  chat.value?.scrollTo({ top: chat.value.scrollHeight })

  try {
    const response = await streamAdminAgentChat({
      sessionId: sessionId.value,
      message: prompt,
      context: {}
    })

    if (!response.ok || !response.body) {
      throw new Error('流式响应不可用')
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    const handleEvent = (block: string) => {
      let event = 'message'
      const dataLines: string[] = []

      block.split(/\r?\n/).forEach(line => {
        if (line.startsWith('event:')) event = line.slice(6).trim()
        if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
      })

      if (!dataLines.length) return

      const raw = dataLines.join('\n')
      let data: any = raw

      try {
        data = JSON.parse(raw)
      } catch {
        data = raw
      }

      if (event === 'meta') {
        aiConnected.value = true
        if (typeof data?.sessionId === 'number') sessionId.value = data.sessionId
      }

      if (event === 'delta') {
        answer.content += typeof data === 'string' ? data : data?.content || ''
      }

      if (event === 'confirmation') {
        answer.confirmation = data
        if (!answer.content) answer.content = data?.summary || '该操作需要确认。'
      }

      if (event === 'error') {
        throw new Error(data?.message || 'AI 服务异常')
      }
    }

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')

      let boundary = buffer.indexOf('\n\n')
      while (boundary >= 0) {
        handleEvent(buffer.slice(0, boundary))
        buffer = buffer.slice(boundary + 2)
        boundary = buffer.indexOf('\n\n')
      }
    }

    if (buffer.trim()) handleEvent(buffer)
    if (!answer.content) answer.content = '模型已响应，但没有返回可展示的正文内容。'
  } catch (error: any) {
    aiConnected.value = false
    answer.content = `AI 服务调用失败：${error?.message || '请检查后端和模型配置'}`
  } finally {
    sending.value = false
    await nextTick()
    chat.value?.scrollTo({ top: chat.value.scrollHeight, behavior: 'smooth' })
  }
}

function canEditConfirmation(confirmation: Confirmation) {
  return Boolean(
    confirmation.action && ['set_shop_status', 'update_order', 'manage_coupon'].includes(confirmation.action)
  )
}

async function editConfirmation(message: Message) {
  if (!message.confirmation || !canEditConfirmation(message.confirmation)) return
  const details = message.confirmation.details || {}
  const auditReason = window.prompt('请输入新的审计理由', String(details.audit_reason || ''))
  if (!auditReason) return
  const editedArguments: Record<string, unknown> = { audit_reason: auditReason }
  if (message.confirmation.action === 'set_shop_status') {
    const status = window.prompt('新的门店状态（OPEN/CLOSED）', String(details.new_value || 'OPEN'))
    if (!status) return
    editedArguments.status = status.toUpperCase()
  } else {
    const action = window.prompt(
      message.confirmation.action === 'update_order'
        ? '新的订单动作（confirm/deliver/complete）'
        : '新的优惠券动作（activate/deactivate）'
    )
    if (!action) return
    editedArguments.action = action.toLowerCase()
  }
  await resolveConfirmation(message, 'edit', editedArguments)
}

async function resolveConfirmation(
  message: Message,
  decision: 'approve' | 'reject' | 'edit',
  editedArguments: Record<string, unknown> | null = null
) {
  if (!message.confirmation || !sessionId.value || sending.value) return
  sending.value = true
  try {
    const response: any = await resumeAdminAgentChat({
      sessionId: sessionId.value,
      confirmationToken: message.confirmation.token,
      decision,
      editedArguments
    })
    message.content = response?.data?.content || response?.data?.answer || '确认流程已处理。'
    message.confirmation = response?.data?.confirmation || null
  } catch (error: any) {
    message.content = `确认操作失败：${error?.message || '请重新发起操作'}`
  } finally {
    sending.value = false
  }
}

async function clearMessages() {
  const currentSessionId = sessionId.value
  messages.value = [{ ...welcomeMessage }]
  input.value = ''
  sessionId.value = null
  if (currentSessionId) {
    await deleteAdminAgentSession(currentSessionId).catch(() => undefined)
  }
}

function formatCurrency(value?: number) {
  return typeof value === 'number' ? `¥${value.toLocaleString('zh-CN', { maximumFractionDigits: 2 })}` : '--'
}

function formatCount(value?: number, suffix = '') {
  return typeof value === 'number' ? `${value}${suffix}` : '--'
}

function formatConfirmationDetails(confirmation: Confirmation) {
  if (!confirmation.details) return ''
  return JSON.stringify(confirmation.details, null, 2)
}

onMounted(() => {
  void Promise.all([loadContext(), restoreConversation()])
})
</script>

<template>
  <div class="agent-layout">
    <aside class="agent-side card">
      <div class="agent-identity">
        <div class="agent-avatar">
          <img src="/takeout-guys-bot.png" alt="AI" />
          <i></i>
        </div>
        <div>
          <h3>Takeout Agent</h3>
          <span>管理端智能运营助手</span>
        </div>
      </div>

      <p class="side-label">快捷能力</p>
      <button @click="send(prompts[0])"><el-icon><TrendCharts /></el-icon>经营数据分析</button>
      <button @click="send(prompts[1])"><el-icon><MagicStick /></el-icon>营销建议生成</button>
      <button @click="send('请检索当前平台规则和可用工具')"><el-icon><Document /></el-icon>平台规则检索</button>

      <div class="knowledge">
        <span>当前状态</span>
        <b>{{ aiConnected ? 'AI 接口已连通' : '等待接口连通' }}</b>
        <small>侧边上下文已改为真实接口数据</small>
      </div>

      <button class="clear" @click="clearMessages">
        <el-icon><Delete /></el-icon>
        清空对话
      </button>
    </aside>

    <section class="chat-panel card">
      <div class="chat-top">
        <div>
          <p class="eyebrow">AGENT CONSOLE</p>
          <h2>智能运营对话</h2>
        </div>
        <span :class="['model-tag', { pending: !aiConnected }]">
          <i></i>
          {{ aiConnected ? 'AI 服务已连接' : '等待首次成功连接' }}
        </span>
      </div>

      <div ref="chat" class="messages">
        <div v-for="(message, index) in messages" :key="index" :class="['message', message.role]">
          <div class="bubble">
            <small>{{ message.role === 'assistant' ? 'Agent' : '你' }}</small>
            <p>{{ message.content }}<span v-if="sending && index === messages.length - 1" class="cursor"></span></p>
            <div v-if="message.confirmation" class="confirmation-card">
              <small>高风险操作 · {{ message.confirmation.expires_at || message.confirmation.expiresAt }}</small>
              <pre v-if="message.confirmation.details">{{ formatConfirmationDetails(message.confirmation) }}</pre>
              <div>
                <button :disabled="sending" @click="resolveConfirmation(message, 'reject')">取消</button>
                <button
                  v-if="canEditConfirmation(message.confirmation)"
                  :disabled="sending"
                  @click="editConfirmation(message)"
                >编辑后重审</button>
                <button class="approve" :disabled="sending" @click="resolveConfirmation(message, 'approve')">确认执行</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="prompt-row">
        <button v-for="prompt in prompts" :key="prompt" @click="send(prompt)">{{ prompt }}</button>
      </div>

      <div class="composer">
        <textarea
          v-model="input"
          placeholder="输入经营问题、订单问题或联调指令"
          @keydown.enter.exact.prevent="send()"
        ></textarea>
        <button :disabled="sending" @click="send()">
          <el-icon><Promotion /></el-icon>
        </button>
        <small>Enter 发送，Agent 输出应与后端 SSE 流式接口保持一致。</small>
      </div>
    </section>

    <aside class="agent-insight">
      <article class="card">
        <p class="eyebrow">LIVE CONTEXT</p>
        <h3>实时上下文</h3>
        <dl>
          <div>
            <dt>营业额</dt>
            <dd>{{ formatCurrency(context.turnover) }}</dd>
          </div>
          <div>
            <dt>有效订单</dt>
            <dd>{{ formatCount(context.validOrderCount, '单') }}</dd>
          </div>
          <div>
            <dt>待接单</dt>
            <dd>{{ formatCount(context.waitingOrders, '单') }}</dd>
          </div>
          <div>
            <dt>待确认</dt>
            <dd>{{ formatCount(context.toBeConfirmed, '单') }}</dd>
          </div>
          <div>
            <dt>派送中</dt>
            <dd>{{ formatCount(context.deliveryInProgress, '单') }}</dd>
          </div>
          <div>
            <dt>客服会话</dt>
            <dd>{{ formatCount(context.serviceSessions, '个') }}</dd>
          </div>
        </dl>
      </article>

      <article class="card guard">
        <span>✓</span>
        <div>
          <b>前端已移除固定假数据</b>
          <small>当前右侧指标全部来自工作台、订单和客服接口。</small>
        </div>
      </article>
    </aside>
  </div>
</template>
