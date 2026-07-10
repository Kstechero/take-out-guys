<template>
	<view class="chat-page">
		<view class="agent-head">
			<image src="../../static/takeout-guys-bot.png" mode="aspectFit"></image>
			<view>
				<text class="name">小慧 Agent</text>
				<text class="state"><i></i>GX10 · 智能服务</text>
			</view>
			<view class="head-actions">
				<text class="history-link" @click="toggleSessionPanel">{{ showSessionPanel ? '收起会话' : '最近会话' }}</text>
				<view class="recommend-link" @click="goRecommend">帮我选菜</view>
				<view class="service-link" @click="goService">转人工客服</view>
			</view>
		</view>

		<view v-if="showSessionPanel" class="session-panel">
			<view class="session-panel-head">
				<text>最近会话</text>
				<text class="new-session" @click="startNewSession">新会话</text>
			</view>
			<view v-if="sessionLoading" class="session-empty">正在加载会话...</view>
			<view v-else-if="!sessions.length" class="session-empty">还没有可切换的历史会话</view>
			<view v-else class="session-list">
				<view
					v-for="session in sessions"
					:key="session.id"
					:class="['session-item', { active: Number(session.id) === Number(sessionId) }]"
					@click="switchSession(session)"
				>
					<view class="session-meta">
						<text class="session-title">{{ session.title || session.lastMessage || ('会话 #' + session.id) }}</text>
						<text class="session-desc">{{ formatSessionTime(session) }}</text>
					</view>
					<text class="session-delete" @click.stop="removeSession(session)">删除</text>
				</view>
			</view>
		</view>

		<scroll-view class="messages" scroll-y :scroll-into-view="lastMessageId">
			<view v-for="(item, index) in messages" :key="index" :id="'message-' + index" :class="['message', item.role]">
				<image v-if="item.role === 'assistant'" src="../../static/takeout-guys-bot.png"></image>
				<view class="bubble">{{ item.content }}<text v-if="loading && index === messages.length - 1" class="cursor">▍</text></view>
			</view>
			<view class="quick-title">你可以这样问</view>
			<view class="quick-list">
				<view v-for="item in quickQuestions" :key="item" @click="send(item)">{{ item }}</view>
			</view>
			<view class="bottom-space"></view>
		</scroll-view>

		<view class="composer safe-bottom">
			<textarea
				v-model="input"
				auto-height
				maxlength="1000"
				placeholder="问菜品、订单、配送或售后"
				confirm-type="send"
				@confirm="send()"
			/>
			<button :disabled="loading || !input.trim()" @click="send()">发送</button>
		</view>
	</view>
</template>

<script>
import { aiChat, deleteAiSession, getAiSessions } from '../api/api.js'

const DEFAULT_MESSAGE = {
	role: 'assistant',
	content: '你好，我是小慧。我可以帮你解答菜品、配送、售后流程和平台规则问题；如果遇到复杂投诉，我也会建议你转人工客服。'
}
const LOCAL_CHAT_CACHE_KEY = 'ai_chat_local_cache'
const createDefaultMessages = () => [{ ...DEFAULT_MESSAGE }]

export default {
	data() {
		return {
			input: '',
			loading: false,
			sessionId: null,
			showSessionPanel: false,
			sessionLoading: false,
			messages: createDefaultMessages(),
			sessions: [],
			quickQuestions: ['我想吃辣一点，预算30元', '帮我看看最近的订单', '配送大概需要多久？']
		}
	},
	onShow() {
		this.loadSessions()
	},
	computed: {
		lastMessageId() {
			return `message-${this.messages.length - 1}`
		}
	},
	methods: {
		toggleSessionPanel() {
			this.showSessionPanel = !this.showSessionPanel
		},
		goRecommend() {
			uni.navigateTo({ url: '/pages/ai/recommend' })
		},
		goService() {
			const latestUserMessage = [...this.messages].reverse().find(item => item.role === 'user' && item.content)
			const query = latestUserMessage ? `?message=${encodeURIComponent(latestUserMessage.content)}` : ''
			uni.navigateTo({ url: `/pages/service/index${query}` })
		},
		getLocalCache() {
			return uni.getStorageSync(LOCAL_CHAT_CACHE_KEY) || {}
		},
		saveMessages(sessionId, messages) {
			if (!sessionId) return
			const cache = this.getLocalCache()
			cache[String(sessionId)] = messages
			uni.setStorageSync(LOCAL_CHAT_CACHE_KEY, cache)
		},
		loadMessages(sessionId) {
			const cache = this.getLocalCache()
			return cache[String(sessionId)] || createDefaultMessages()
		},
		clearMessages(sessionId) {
			if (!sessionId) return
			const cache = this.getLocalCache()
			delete cache[String(sessionId)]
			uni.setStorageSync(LOCAL_CHAT_CACHE_KEY, cache)
		},
		normalizeSessions(data) {
			if (Array.isArray(data)) return data
			if (Array.isArray(data?.records)) return data.records
			if (Array.isArray(data?.items)) return data.items
			return []
		},
		formatSessionTime(session) {
			return session.updateTime || session.lastTime || session.createTime || '本地缓存会话'
		},
		async loadSessions() {
			this.sessionLoading = true
			try {
				const res = await getAiSessions()
				this.sessions = this.normalizeSessions(res.data)
			} catch (error) {
				this.sessions = []
			} finally {
				this.sessionLoading = false
			}
		},
		startNewSession() {
			this.sessionId = null
			this.messages = createDefaultMessages()
			this.showSessionPanel = false
		},
		switchSession(session) {
			this.sessionId = session.id
			this.messages = this.loadMessages(session.id)
			this.showSessionPanel = false
			if (this.messages.length <= 1) {
				uni.showToast({ title: '已切换会话，本地暂无历史消息', icon: 'none' })
			}
		},
		async removeSession(session) {
			try {
				await deleteAiSession(session.id)
				this.clearMessages(session.id)
				if (Number(this.sessionId) === Number(session.id)) this.startNewSession()
				this.sessions = this.sessions.filter(item => Number(item.id) !== Number(session.id))
				uni.showToast({ title: '会话已删除', icon: 'success' })
			} catch (error) {
				uni.showToast({ title: '删除会话失败', icon: 'none' })
			}
		},
		extractReply(data) {
			return data?.content || data?.answer || data?.message || data?.reply || '暂时没有可展示的回复'
		},
		async send(text) {
			const content = (typeof text === 'string' ? text : this.input).trim()
			if (!content || this.loading) return
			this.messages.push({ role: 'user', content })
			this.input = ''
			this.loading = true
			const answer = { role: 'assistant', content: '' }
			this.messages.push(answer)
			try {
				const res = await aiChat({ sessionId: this.sessionId, message: content })
				if (res.data?.sessionId) this.sessionId = res.data.sessionId
				answer.content = this.extractReply(res.data)
				this.saveMessages(this.sessionId, this.messages)
				await this.loadSessions()
			} catch (error) {
				answer.content = error?.msg || '智能客服暂时不可用，请稍后再试。若问题较复杂，建议转人工客服。'
			} finally {
				this.loading = false
			}
		}
	}
}
</script>

<style lang="scss" scoped>
.chat-page{height:100%;background:#f7f6f2;display:flex;flex-direction:column}
.agent-head{min-height:126rpx;padding:20rpx 28rpx;background:#101a2a;color:#fff;display:flex;align-items:center;box-sizing:border-box}
.agent-head image{width:78rpx;height:78rpx;border-radius:22rpx;margin-right:18rpx}
.agent-head>view:nth-child(2){display:flex;flex-direction:column;flex:1}
.name{font-size:29rpx;font-weight:700}
.state{font-size:20rpx;color:#8f9baa}
.state i{display:inline-block;width:12rpx;height:12rpx;border-radius:50%;background:#58d68d;margin-right:8rpx}
.head-actions{display:flex;flex-direction:column;align-items:flex-end;gap:10rpx}
.history-link{font-size:21rpx;color:#cfd6df}
.recommend-link,.service-link{padding:10rpx 18rpx;border-radius:22rpx;font-size:21rpx}
.recommend-link{background:#ff4b12}
.service-link{background:rgba(255,255,255,.12);border:1rpx solid rgba(255,255,255,.18)}
.session-panel{padding:18rpx 24rpx;background:#fff;border-bottom:1rpx solid #ecebe7}
.session-panel-head{display:flex;justify-content:space-between;align-items:center;font-size:24rpx;font-weight:700;color:#101a2a}
.new-session{color:#ff4b12;font-weight:600}
.session-empty{padding:26rpx 0 10rpx;color:#8b938e;font-size:22rpx}
.session-list{display:flex;flex-direction:column;gap:14rpx;margin-top:18rpx}
.session-item{display:flex;align-items:center;justify-content:space-between;padding:18rpx 20rpx;background:#f6f6f2;border-radius:20rpx}
.session-item.active{background:#fff1eb;border:1rpx solid #ffd2c4}
.session-meta{display:flex;flex:1;flex-direction:column;overflow:hidden}
.session-title{font-size:24rpx;color:#182232;white-space:nowrap;text-overflow:ellipsis;overflow:hidden}
.session-desc{margin-top:6rpx;font-size:20rpx;color:#8d948f}
.session-delete{margin-left:16rpx;color:#d94b1e;font-size:22rpx}
.messages{flex:1;height:0;padding:28rpx 24rpx;box-sizing:border-box}
.message{display:flex;align-items:flex-start;margin-bottom:25rpx}
.message image{width:58rpx;height:58rpx;border-radius:17rpx;margin-right:12rpx}
.message.user{justify-content:flex-end}
.bubble{max-width:72%;padding:20rpx 23rpx;background:#fff;border-radius:8rpx 26rpx 26rpx 26rpx;box-shadow:0 7rpx 22rpx rgba(16,26,42,.06);font-size:26rpx;line-height:1.65}
.user .bubble{background:#ff4b12;color:#fff;border-radius:26rpx 8rpx 26rpx 26rpx}
.cursor{color:#ff4b12}
.quick-title{font-size:21rpx;color:#969d99;margin:35rpx 0 12rpx 70rpx}
.quick-list{margin-left:70rpx}
.quick-list view{display:inline-block;padding:11rpx 17rpx;margin:0 8rpx 10rpx 0;background:#fff;border:1rpx solid #e7e8e4;border-radius:24rpx;color:#5f6963;font-size:21rpx}
.bottom-space{height:30rpx}
.composer{display:flex;align-items:flex-end;padding:18rpx 22rpx calc(18rpx + env(safe-area-inset-bottom));background:#fff;border-top:1rpx solid #e8e8e5}
.composer textarea{flex:1;min-height:42rpx;max-height:150rpx;padding:17rpx 20rpx;background:#f3f4f0;border-radius:24rpx;font-size:25rpx;line-height:1.45}
.composer button{width:112rpx;height:72rpx;line-height:72rpx;margin:0 0 0 14rpx;padding:0;background:#ff4b12;color:#fff;border-radius:24rpx;font-size:24rpx}
.composer button[disabled]{opacity:.45}
</style>
