<template>
	<view class="chat-page">
		<view class="agent-head">
			<image src="../../static/takeout-guys-bot.png" mode="aspectFit"></image>
			<view><text class="name">小慧 Agent</text><text class="state"><i></i>GX10 · 智能服务</text></view>
			<view class="recommend-link" @click="goRecommend">帮我选菜</view>
		</view>

		<scroll-view class="messages" scroll-y :scroll-into-view="lastMessageId">
			<view v-for="(item,index) in messages" :key="index" :id="'message-'+index" :class="['message', item.role]">
				<image v-if="item.role==='assistant'" src="../../static/takeout-guys-bot.png"></image>
				<view class="bubble">{{item.content}}<text v-if="loading && index===messages.length-1" class="cursor">▍</text></view>
			</view>
			<view class="quick-title">你可以这样问</view>
			<view class="quick-list"><view v-for="item in quickQuestions" :key="item" @click="send(item)">{{item}}</view></view>
			<view class="bottom-space"></view>
		</scroll-view>

		<view class="composer safe-bottom">
			<textarea v-model="input" auto-height maxlength="1000" placeholder="问菜品、订单、配送或售后…" confirm-type="send" @confirm="send()"/>
			<button :disabled="loading || !input.trim()" @click="send()">发送</button>
		</view>
	</view>
</template>

<script>
import { aiChat } from '../api/api.js'
export default {
	data() {
		return {
			input: '', loading: false, sessionId: null,
			messages: [{ role: 'assistant', content: '你好，我是小慧 👋 我可以帮你推荐菜品、查询订单，也可以解答配送和售后问题。' }],
			quickQuestions: ['我想吃辣一点，预算30元', '帮我看看最近的订单', '配送大概需要多久？']
		}
	},
	computed: { lastMessageId() { return `message-${this.messages.length - 1}` } },
	methods: {
		goRecommend() { uni.navigateTo({ url: '/pages/ai/recommend' }) },
		async send(text) {
			const content = (typeof text === 'string' ? text : this.input).trim()
			if (!content || this.loading) return
			this.messages.push({ role: 'user', content }); this.input = ''; this.loading = true
			const answer = { role: 'assistant', content: '' }; this.messages.push(answer)
			try {
				const res = await aiChat({ sessionId: this.sessionId, message: content, enableTools: true })
				this.sessionId = res.data.sessionId
				answer.content = res.data.content || '暂时没有可展示的回复'
			} catch (error) {
				answer.content = '用户端 AI 接口正在接入中。后端实现 /user/ai/chat 后即可在这里直接对话。'
			} finally { this.loading = false }
		}
	}
}
</script>

<style lang="scss" scoped>
.chat-page{height:100%;background:#f7f6f2;display:flex;flex-direction:column}.agent-head{height:126rpx;padding:20rpx 28rpx;background:#101a2a;color:#fff;display:flex;align-items:center;box-sizing:border-box}.agent-head image{width:78rpx;height:78rpx;border-radius:22rpx;margin-right:18rpx}.agent-head>view:nth-child(2){display:flex;flex-direction:column;flex:1}.name{font-size:29rpx;font-weight:700}.state{font-size:20rpx;color:#8f9baa}.state i{display:inline-block;width:12rpx;height:12rpx;border-radius:50%;background:#58d68d;margin-right:8rpx}.recommend-link{padding:10rpx 18rpx;background:#ff4b12;border-radius:22rpx;font-size:21rpx}.messages{flex:1;height:0;padding:28rpx 24rpx;box-sizing:border-box}.message{display:flex;align-items:flex-start;margin-bottom:25rpx}.message image{width:58rpx;height:58rpx;border-radius:17rpx;margin-right:12rpx}.message.user{justify-content:flex-end}.bubble{max-width:72%;padding:20rpx 23rpx;background:#fff;border-radius:8rpx 26rpx 26rpx 26rpx;box-shadow:0 7rpx 22rpx rgba(16,26,42,.06);font-size:26rpx;line-height:1.65}.user .bubble{background:#ff4b12;color:#fff;border-radius:26rpx 8rpx 26rpx 26rpx}.cursor{color:#ff4b12}.quick-title{font-size:21rpx;color:#969d99;margin:35rpx 0 12rpx 70rpx}.quick-list{margin-left:70rpx}.quick-list view{display:inline-block;padding:11rpx 17rpx;margin:0 8rpx 10rpx 0;background:#fff;border:1rpx solid #e7e8e4;border-radius:24rpx;color:#5f6963;font-size:21rpx}.bottom-space{height:30rpx}.composer{display:flex;align-items:flex-end;padding:18rpx 22rpx calc(18rpx + env(safe-area-inset-bottom));background:#fff;border-top:1rpx solid #e8e8e5}.composer textarea{flex:1;min-height:42rpx;max-height:150rpx;padding:17rpx 20rpx;background:#f3f4f0;border-radius:24rpx;font-size:25rpx;line-height:1.45}.composer button{width:112rpx;height:72rpx;line-height:72rpx;margin:0 0 0 14rpx;padding:0;background:#ff4b12;color:#fff;border-radius:24rpx;font-size:24rpx}.composer button[disabled]{opacity:.45}
</style>
