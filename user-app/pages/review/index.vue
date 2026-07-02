<template>
	<view class="review-page">
		<view class="hero"><text>VOICE OF CUSTOMER</text><text>分享你的用餐体验</text></view>
		<view v-if="!orders.length" class="empty">暂无可评价的已完成订单</view>
		<view v-for="order in orders" :key="order.id" class="review-card">
			<view class="order-head"><text>订单 {{order.number}}</text><text>¥{{order.amount}}</text></view>
			<view class="dishes">{{dishNames(order)}}</view>
			<view class="stars"><text v-for="star in 5" :key="star" :class="{active:star<=form(order.id).score}" @click="form(order.id).score=star">★</text></view>
			<textarea v-model="form(order.id).content" maxlength="300" placeholder="菜品口味、包装和配送体验怎么样？" />
			<view class="actions"><button class="ai" @click="writeWithAi(order)">AI 帮写</button><button class="submit" @click="submit(order)">提交评价</button></view>
		</view>
	</view>
</template>

<script>
import { aiWriteReview, queryOrderUserPage, submitReview } from '../api/api.js'
export default {
	data(){return{orders:[],forms:{}}},
	onLoad(){this.load()},
	methods:{
		async load(){try{const res=await queryOrderUserPage({page:1,pageSize:20,status:5});this.orders=(res.data.records||[]).filter(Boolean)}catch(e){this.orders=[]}},
		form(id){if(!this.forms[id])this.forms[id]={score:5,content:''};return this.forms[id]},
		dishNames(order){return (order.orderDetailList||[]).map(item=>item.name).join('、')},
		async writeWithAi(order){try{const res=await aiWriteReview({orderId:order.id,action:'write',draft:this.form(order.id).content,instruction:'生成自然真实的用餐评价'});this.form(order.id).content=res.data.content}catch(e){uni.showToast({title:'AI 评价接口尚未实现',icon:'none'})}},
		async submit(order){const value=this.form(order.id);if(!value.content.trim()){uni.showToast({title:'请填写评价内容',icon:'none'});return}try{await submitReview({orderId:order.id,score:value.score,content:value.content});uni.showToast({title:'评价成功'});this.orders=this.orders.filter(item=>item.id!==order.id)}catch(e){uni.showToast({title:'评价后端接口尚未实现',icon:'none'})}}
	}
}
</script>

<style lang="scss" scoped>
.review-page{min-height:100vh;padding:24rpx;background:#f6f5f1;box-sizing:border-box}.hero{margin:-24rpx -24rpx 28rpx;padding:42rpx 30rpx;background:#101a2a;color:#fff;display:flex;flex-direction:column}.hero text:first-child{color:#ff5a22;font-size:18rpx;letter-spacing:3rpx}.hero text:last-child{font-size:36rpx;font-weight:700;margin-top:10rpx}.empty{padding:100rpx 20rpx;text-align:center;color:#929894}.review-card{margin-bottom:20rpx;padding:24rpx;background:#fff;border-radius:24rpx;box-shadow:0 8rpx 24rpx rgba(16,26,42,.05)}.order-head{display:flex;justify-content:space-between;font-size:26rpx;font-weight:700}.order-head text:last-child{color:#ff4b12}.dishes{margin:12rpx 0;color:#737c76;font-size:22rpx}.stars text{margin-right:10rpx;color:#d9d9d4;font-size:40rpx}.stars .active{color:#ff4b12}.review-card textarea{width:100%;height:130rpx;margin-top:18rpx;padding:18rpx;background:#f5f5f1;border-radius:18rpx;box-sizing:border-box;font-size:24rpx}.actions{display:flex;gap:14rpx;margin-top:16rpx}.actions button{flex:1;margin:0;border-radius:18rpx;font-size:23rpx}.ai{background:#fff1eb!important;color:#dc410e!important}.submit{background:#ff4b12!important;color:#fff!important}
</style>
