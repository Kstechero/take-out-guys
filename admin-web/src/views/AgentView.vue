<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { Promotion, Delete, MagicStick, Document, TrendCharts } from '@element-plus/icons-vue'
type Message = { role: 'user' | 'assistant', content: string }
const messages = ref<Message[]>([{ role: 'assistant', content: '晚上好，我是小慧。可以帮你分析经营数据、定位异常订单、生成运营建议，也可以检索平台业务规则。今天想先看什么？' }])
const input = ref(''); const sending = ref(false); const chat = ref<HTMLElement>()
const aiConnected = ref(false)
const sessionId = ref<number | null>(null)
const prompts = ['分析今天订单趋势', '哪些菜品值得重点推广？', '总结待处理的客服问题']
async function send(text = input.value) {
  if (!text.trim() || sending.value) return
  const prompt = text.trim()
  messages.value.push({ role: 'user', content: prompt }); input.value = ''; sending.value = true
  const answer: Message = { role: 'assistant', content: '' }; messages.value.push(answer); await nextTick(); chat.value?.scrollTo({ top: chat.value.scrollHeight })
  try {
    const response = await fetch(`${import.meta.env.VITE_API_BASE}/ai/chat/stream`, { method: 'POST', headers: { token: localStorage.getItem('sky_admin_token') || '', Accept: 'text/event-stream', 'Content-Type': 'application/json' }, body: JSON.stringify({ sessionId: sessionId.value, message: prompt, context: {} }) })
    if (!response.ok || !response.body) throw new Error('stream unavailable')
    const reader = response.body.getReader(); const decoder = new TextDecoder(); let buffer = ''
    const handleEvent = (block: string) => {
      let event = 'message'; const dataLines: string[] = []
      block.split(/\r?\n/).forEach(line => { if (line.startsWith('event:')) event = line.slice(6).trim(); if (line.startsWith('data:')) dataLines.push(line.slice(5).trim()) })
      if (!dataLines.length) return
      const raw = dataLines.join('\n'); let data: any = raw
      try { data = JSON.parse(raw) } catch { /* plain text SSE */ }
      if (event === 'meta') {
        aiConnected.value = true
        if (data && typeof data.sessionId === 'number') sessionId.value = data.sessionId
      }
      if (event === 'delta') answer.content += typeof data === 'string' ? data : (data.content || '')
      if (event === 'error') throw new Error(data.message || 'AI 服务异常')
    }
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')
      let boundary: number
      while ((boundary = buffer.indexOf('\n\n')) >= 0) { const block = buffer.slice(0, boundary); buffer = buffer.slice(boundary + 2); handleEvent(block) }
    }
    if (buffer.trim()) handleEvent(buffer)
    if (!answer.content) answer.content = '模型已响应，但没有返回可展示的正文。请适当提高 max_tokens 后重试。'
  } catch (error: any) { aiConnected.value = false; answer.content = `AI 服务调用失败：${error.message || '请检查后端和模型密钥配置'}` }
  finally { sending.value = false; await nextTick(); chat.value?.scrollTo({ top: chat.value.scrollHeight, behavior: 'smooth' }) }
}
</script>
<template>
  <div class="agent-layout">
    <aside class="agent-side card"><div class="agent-identity"><div class="agent-avatar"><img src="/takeout-guys-bot.png" alt="AI"/><i></i></div><div><h3>Takeout Agent</h3><span>智能运营助理 · 在线</span></div></div><p class="side-label">快捷能力</p><button><el-icon><TrendCharts /></el-icon>经营数据分析</button><button><el-icon><MagicStick /></el-icon>营销方案生成</button><button><el-icon><Document /></el-icon>平台规则检索</button><div class="knowledge"><span>知识库</span><b>12 篇文档已同步</b><small>上次更新：今天 16:40</small></div><button class="clear" @click="messages.splice(1); sessionId = null"><el-icon><Delete /></el-icon>清空对话</button></aside>
    <section class="chat-panel card"><div class="chat-top"><div><p class="eyebrow">AGENT CONSOLE</p><h2>智能运营对话</h2></div><span :class="['model-tag', { pending: !aiConnected }]"><i></i>GX10 · ornith · {{ aiConnected ? '已连接' : '等待首次连接' }}</span></div><div ref="chat" class="messages"><div v-for="(m,i) in messages" :key="i" :class="['message', m.role]"><div class="bubble"><small>{{ m.role === 'assistant' ? '小慧' : '你' }}</small><p>{{ m.content }}<span v-if="sending && i === messages.length-1" class="cursor"></span></p></div></div></div><div class="prompt-row"><button v-for="p in prompts" :key="p" @click="send(p)">{{ p }}</button></div><div class="composer"><textarea v-model="input" placeholder="询问经营数据、订单情况或运营建议…" @keydown.enter.exact.prevent="send()"></textarea><button :disabled="sending" @click="send()"><el-icon><Promotion /></el-icon></button><small>Enter 发送 · Agent 只会访问当前管理员有权查看的数据</small></div></section>
    <aside class="agent-insight"><article class="card"><p class="eyebrow">LIVE CONTEXT</p><h3>实时上下文</h3><dl><div><dt>今日订单</dt><dd>326</dd></div><div><dt>营业额</dt><dd>¥18,630</dd></div><div><dt>异常订单</dt><dd class="warn">2</dd></div><div><dt>客服排队</dt><dd>3</dd></div></dl></article><article class="card guard"><span>✓</span><div><b>安全护栏已开启</b><small>订单归属与操作权限自动校验</small></div></article></aside>
  </div>
</template>
