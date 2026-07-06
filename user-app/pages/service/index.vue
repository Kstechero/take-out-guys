<template>
	<view class="service-page">
		<view class="service-head">
			<view class="service-head-main">
				<image src="../../static/btn_waiter_sel.png" mode="aspectFit"></image>
				<view class="service-head-copy">
					<text class="service-title">人工客服</text>
					<text class="service-subtitle">工作时间 10:00-22:00，可处理售后、投诉和复杂问题</text>
				</view>
			</view>
			<view class="service-head-actions">
				<text class="service-status" :class="{ closed: sessionClosed }">{{ sessionStatusText }}</text>
				<text class="service-refresh" @click="manualRefresh">刷新</text>
			</view>
		</view>

		<view class="service-notice tg-card">
			<text>说明：提交消息后，管理端客服工作台会收到你的小程序人工客服请求。</text>
		</view>

		<scroll-view class="messages" scroll-y :scroll-into-view="lastMessageAnchor">
			<view
				v-for="(item, index) in messages"
				:key="item.id || item.clientId || index"
				:id="messageAnchor(index)"
				:class="['message-row', messageRole(item)]"
			>
				<image v-if="messageRole(item) === 'service'" src="../../static/btn_waiter_sel.png" mode="aspectFit"></image>
				<view class="message-bubble">
					<text class="message-name">{{ messageRole(item) === 'service' ? '客服' : '我' }}</text>
					<text class="message-content">{{ item.content || '空消息' }}</text>
					<text class="message-time">{{ formatTime(item.createTime) }}</text>
				</view>
			</view>
			<view v-if="!messages.length && !loading" class="empty-state">
				<text class="empty-title">还没有客服消息</text>
				<text class="empty-copy">发送第一条消息后，客服工作台就能看到你的会话。</text>
			</view>
			<view class="bottom-space"></view>
		</scroll-view>

		<view class="composer safe-bottom">
			<textarea
				v-model="draft"
				auto-height
				maxlength="500"
				:disabled="sending || sessionClosed"
				placeholder="输入你想咨询的问题"
				confirm-type="send"
				@confirm="submitMessage"
			/>
			<button class="send-btn" :disabled="sending || sessionClosed || !draft.trim()" @click="submitMessage">发送</button>
		</view>

		<view v-if="sessionId" class="footer-actions">
			<button class="ghost-btn" :disabled="sessionClosed || ending" @click="closeSession">
				{{ ending ? '结束中...' : '结束会话' }}
			</button>
		</view>
	</view>
</template>

<script>
import {
	createServiceSession,
	endServiceSession,
	getServiceMessageList,
	sendServiceMessage
} from '../api/api.js'

const DEFAULT_WELCOME = {
	clientId: 'welcome',
	senderType: 'admin',
	content: '你好，这里是人工客服。请描述你的问题，我们会尽快处理。',
	createTime: ''
}

export default {
	data() {
		return {
			loading: false,
			sending: false,
			ending: false,
			draft: '',
			sessionId: null,
			sessionClosed: false,
			messages: [],
			lastMessageId: null,
			pollTimer: null
		}
	},
	computed: {
		lastMessageAnchor() {
			return this.messageAnchor(this.messages.length - 1)
		},
		sessionStatusText() {
			return this.sessionClosed ? '会话已结束' : '会话进行中'
		}
	},
	onLoad(options) {
		if (options && options.message) this.draft = decodeURIComponent(options.message)
	},
	onShow() {
		this.bootstrap()
	},
	onHide() {
		this.stopPolling()
	},
	onUnload() {
		this.stopPolling()
	},
	methods: {
		messageAnchor(index) {
			return `service-message-${Math.max(index, 0)}`
		},
		messageRole(message) {
			return String((message && message.senderType) || '').toLowerCase() === 'user' ? 'user' : 'service'
		},
		formatTime(value) {
			if (!value) return '刚刚'
			return String(value).replace('T', ' ').slice(0, 16)
		},
		getServerMessageId(message) {
			const parsed = Number(message && message.id)
			return Number.isFinite(parsed) && parsed > 0 ? parsed : null
		},
		resetState() {
			this.sessionId = null
			this.sessionClosed = false
			this.messages = []
			this.lastMessageId = null
		},
		normalizeMessages(list) {
			if (!Array.isArray(list)) return []
			if (!list.length) return [DEFAULT_WELCOME]
			return list
		},
		applySession(session) {
			this.sessionId = Number((session && session.id) || 0) || null
			this.sessionClosed = Number((session && session.status) || 0) === 2
		},
		async ensureSession() {
			console.log('[service] ensureSession:start', { sessionId: this.sessionId, sessionClosed: this.sessionClosed })
			const res = await createServiceSession({ source: 'miniapp' })
			if (!(res.data && res.data.id)) {
				console.log('[service] ensureSession:invalid', res)
				throw new Error('create session failed')
			}
			this.applySession(res.data)
			console.log('[service] ensureSession:success', { sessionId: this.sessionId, sessionClosed: this.sessionClosed, session: res.data })
			return this.sessionId
		},
		async bootstrap() {
			this.loading = true
			console.log('[service] bootstrap:start')
			try {
				await this.ensureSession()
				await this.loadMessages()
				this.startPolling()
			} catch (error) {
				console.log('[service] bootstrap:error', error)
				this.stopPolling()
				this.resetState()
			} finally {
				this.loading = false
				console.log('[service] bootstrap:end', { sessionId: this.sessionId, sessionClosed: this.sessionClosed, lastMessageId: this.lastMessageId, messageCount: this.messages.length })
			}
		},
		async loadMessages(lastMessageId) {
			if (!this.sessionId) {
				await this.ensureSession()
			}
			const params = { sessionId: this.sessionId }
			if (lastMessageId !== null && lastMessageId !== undefined && lastMessageId !== '') {
				params.lastMessageId = lastMessageId
			}
			console.log('[service] loadMessages:request', params)
			const res = await getServiceMessageList(params)
			const list = Array.isArray(res.data) ? res.data : []
			console.log('[service] loadMessages:response', {
				requestLastMessageId: lastMessageId,
				responseCount: list.length,
				firstMessage: list[0],
				lastMessage: list[list.length - 1]
			})

			if (lastMessageId) {
				if (!list.length) return
				this.messages = this.messages.concat(list)
			} else {
				this.messages = this.normalizeMessages(list)
			}

			const latest = this.messages[this.messages.length - 1]
			this.lastMessageId = this.getServerMessageId(latest)
			console.log('[service] loadMessages:applied', {
				sessionId: this.sessionId,
				lastMessageId: this.lastMessageId,
				messageCount: this.messages.length
			})
		},
		startPolling() {
			this.stopPolling()
			if (!this.sessionId) {
				console.log('[service] polling:skip', { sessionId: this.sessionId, sessionClosed: this.sessionClosed })
				return
			}
			console.log('[service] polling:start', { sessionId: this.sessionId, sessionClosed: this.sessionClosed, lastMessageId: this.lastMessageId })
			this.pollTimer = setInterval(async () => {
				try {
					console.log('[service] polling:tick', { sessionId: this.sessionId, sessionClosed: this.sessionClosed, lastMessageId: this.lastMessageId })
					await this.loadMessages(this.lastMessageId)
				} catch (error) {
					console.log('[service] polling:error', error)
				}
			}, 5000)
		},
		stopPolling() {
			if (this.pollTimer) {
				clearInterval(this.pollTimer)
				this.pollTimer = null
				console.log('[service] polling:stop')
			}
		},
		async manualRefresh() {
			try {
				await this.loadMessages()
				uni.showToast({ title: '已刷新', icon: 'success' })
			} catch (error) {
				uni.showToast({ title: (error && error.msg) || '刷新失败', icon: 'none' })
			}
		},
		async submitMessage() {
			const content = this.draft.trim()
			if (!content || this.sending || this.sessionClosed) return
			this.sending = true
			try {
				if (!this.sessionId) {
					await this.ensureSession()
				}
				await sendServiceMessage({
					sessionId: this.sessionId,
					content,
					messageType: 'text'
				})
				this.draft = ''
				await this.loadMessages()
				this.startPolling()
			} catch (error) {
				uni.showToast({ title: (error && error.msg) || '发送失败', icon: 'none' })
			} finally {
				this.sending = false
			}
		},
		async closeSession() {
			if (!this.sessionId || this.sessionClosed || this.ending) return
			this.ending = true
			try {
				await endServiceSession(this.sessionId)
				this.sessionClosed = true
				this.stopPolling()
				uni.showToast({ title: '会话已结束', icon: 'success' })
			} catch (error) {
				uni.showToast({ title: (error && error.msg) || '结束失败', icon: 'none' })
			} finally {
				this.ending = false
			}
		}
	}
}
</script>

<style lang="scss" scoped>
.service-page{height:100%;background:#f7f6f2;display:flex;flex-direction:column}
.service-head{display:flex;align-items:flex-start;justify-content:space-between;padding:24rpx;background:#101a2a;color:#fff}
.service-head-main{display:flex;align-items:center;flex:1;min-width:0}
.service-head-main image{width:74rpx;height:74rpx;border-radius:18rpx;margin-right:18rpx}
.service-head-copy{display:flex;flex-direction:column;min-width:0}
.service-title{font-size:30rpx;font-weight:700}
.service-subtitle{margin-top:8rpx;font-size:21rpx;line-height:1.5;color:#c8d1dc}
.service-head-actions{display:flex;flex-direction:column;align-items:flex-end;margin-left:16rpx}
.service-status{padding:8rpx 16rpx;background:rgba(88,214,141,.18);border:1rpx solid rgba(88,214,141,.35);border-radius:999rpx;font-size:20rpx;color:#8ff0b3}
.service-status.closed{background:rgba(255,255,255,.08);border-color:rgba(255,255,255,.15);color:#d5dae1}
.service-refresh{margin-top:14rpx;font-size:22rpx;color:#ffb095}
.service-notice{margin:20rpx 24rpx 0;padding:22rpx 24rpx;font-size:23rpx;line-height:1.6;color:#5c6672}
.messages{flex:1;height:0;padding:24rpx;box-sizing:border-box}
.message-row{display:flex;align-items:flex-start;margin-bottom:24rpx}
.message-row.service image{width:56rpx;height:56rpx;border-radius:16rpx;margin-right:12rpx}
.message-row.user{justify-content:flex-end}
.message-bubble{display:flex;flex-direction:column;max-width:76%;padding:18rpx 22rpx;background:#fff;border-radius:10rpx 24rpx 24rpx 24rpx;box-shadow:0 10rpx 24rpx rgba(16,26,42,.06)}
.message-row.user .message-bubble{background:#ff4b12;color:#fff;border-radius:24rpx 10rpx 24rpx 24rpx}
.message-name{font-size:20rpx;font-weight:600;color:#8d948f}
.message-row.user .message-name{color:rgba(255,255,255,.78)}
.message-content{margin-top:8rpx;font-size:26rpx;line-height:1.6;word-break:break-all}
.message-time{margin-top:10rpx;font-size:19rpx;color:#9ca49e}
.message-row.user .message-time{color:rgba(255,255,255,.75)}
.empty-state{margin-top:70rpx;padding:36rpx 28rpx;text-align:center;background:#fff;border-radius:28rpx;color:#7d867f}
.empty-title{display:block;font-size:28rpx;font-weight:700;color:#162131}
.empty-copy{display:block;margin-top:10rpx;font-size:23rpx;line-height:1.6}
.bottom-space{height:36rpx}
.composer{display:flex;align-items:flex-end;padding:18rpx 22rpx calc(18rpx + env(safe-area-inset-bottom));background:#fff;border-top:1rpx solid #ebe8e2}
.composer textarea{flex:1;min-height:44rpx;max-height:180rpx;padding:16rpx 20rpx;background:#f2f3ef;border-radius:24rpx;font-size:25rpx;line-height:1.5}
.send-btn{width:116rpx;height:74rpx;line-height:74rpx;margin-left:14rpx;padding:0;background:#ff4b12;color:#fff;border-radius:24rpx;font-size:24rpx}
.send-btn[disabled]{opacity:.45}
.footer-actions{padding:0 24rpx 22rpx;background:#fff}
.ghost-btn{height:76rpx;line-height:76rpx;background:#fff;color:#101a2a;border:1rpx solid #d8dbe0;border-radius:24rpx;font-size:24rpx}
.ghost-btn[disabled]{opacity:.5}
</style>
