<template>
	<view class="recommend-page">
		<view class="hero"><text class="eyebrow">AI FOOD DISCOVERY</text><text class="title">不用翻菜单，告诉我你想吃什么</text><text class="desc">推荐只会来自当前真实可售菜品</text></view>
		<view class="search-card tg-card"><textarea v-model="query" maxlength="300" placeholder="例如：想吃辣一点，一个人，预算30元以内"/><view class="chips"><text v-for="item in presets" :key="item" @click="query=item">{{item}}</text></view><button :disabled="loading || !query.trim()" @click="recommend">{{loading?'正在挑选…':'让 AI 帮我选'}}</button></view>
		<view v-if="notice" class="notice">{{notice}}</view>
		<view v-if="items.length" class="result-title">为你找到 {{items.length}} 道菜</view>
		<view v-for="item in items" :key="item.dishId" class="dish-card tg-card"><image :src="item.image" mode="aspectFill"/><view class="dish-info"><view class="dish-name"><text>{{item.name}}</text><text>¥{{item.price}}</text></view><text class="category">{{item.categoryName}}</text><text class="reason">{{item.reason}}</text><button @click="add(item)">加入购物车</button></view></view>
	</view>
</template>

<script>
import { aiRecommend, newAddShoppingCartAdd } from '../api/api.js'
export default {
	data(){return{query:'',loading:false,items:[],notice:'',presets:['一人食 · 30元内','清淡低脂','无辣不欢','适合两个人分享']}},
	methods:{
		buildRecommendPayload() {
			const payload = { requirement: this.query.trim() }
			const budgetMatch = payload.requirement.match(/(\d+(?:\.\d+)?)\s*元/)
			const peopleMatch = payload.requirement.match(/([1-9]\d*)\s*(?:人|位)/)
			if (budgetMatch) payload.budget = Number(budgetMatch[1])
			if (peopleMatch) payload.peopleCount = Number(peopleMatch[1])
			return payload
		},
		normalizeItems(data) {
			if (Array.isArray(data)) return data
			if (Array.isArray(data?.items)) return data.items
			if (Array.isArray(data?.records)) return data.records
			return []
		},
		async recommend(){this.loading=true;this.notice='';try{const res=await aiRecommend(this.buildRecommendPayload());this.items=this.normalizeItems(res.data)}catch(e){this.items=[];this.notice='AI 推荐接口暂未完成后端联调，请先实现 /user/ai/recommend。'}finally{this.loading=false}},
		async add(item){await newAddShoppingCartAdd({dishId:item.dishId,number:1,amount:item.price,name:item.name,image:item.image});uni.showToast({title:'已加入购物车',icon:'success'})}
	}
}
</script>

<style lang="scss" scoped>
.recommend-page{min-height:100%;padding:0 24rpx 50rpx;background:#f7f6f2}.hero{margin:0 -24rpx;padding:46rpx 30rpx 92rpx;background:linear-gradient(145deg,#ff3f08,#ff6b30);color:#fff;display:flex;flex-direction:column}.eyebrow{font-size:18rpx;letter-spacing:3rpx;opacity:.7}.title{font-size:38rpx;font-weight:800;line-height:1.35;margin:13rpx 0}.desc{font-size:23rpx;opacity:.74}.search-card{margin-top:-58rpx;padding:24rpx}.search-card textarea{width:100%;height:125rpx;padding:18rpx;background:#f6f6f2;border-radius:20rpx;box-sizing:border-box;font-size:25rpx}.chips{margin:15rpx 0}.chips text{display:inline-block;margin:0 8rpx 8rpx 0;padding:8rpx 14rpx;background:#fff1ea;color:#d9410e;border-radius:18rpx;font-size:20rpx}.search-card button,.dish-info button{background:#101a2a;color:#fff;border-radius:20rpx;font-size:25rpx}.notice{margin:22rpx 0;padding:20rpx;background:#fff5df;color:#986912;border-radius:18rpx;font-size:23rpx}.result-title{font-size:28rpx;font-weight:700;margin:30rpx 0 16rpx}.dish-card{display:flex;padding:18rpx;margin-bottom:18rpx}.dish-card>image{width:190rpx;height:190rpx;border-radius:21rpx;margin-right:20rpx}.dish-info{flex:1;display:flex;flex-direction:column}.dish-name{display:flex;justify-content:space-between;font-size:28rpx;font-weight:700}.dish-name text:last-child{color:#ff4b12}.category{font-size:20rpx;color:#929a95;margin:5rpx 0}.reason{font-size:22rpx;color:#5f6963;line-height:1.5;flex:1}.dish-info button{height:52rpx;line-height:52rpx;font-size:21rpx;margin:8rpx 0 0}
</style>
