<template>
	<view class="my-center">
		<view class="my-info">
			<view class="head">
				<image class="head-image" :src="personUrl"></image>
			</view>
			<view class="phone-name">
				<view class="name">
					<text class="name-text">{{ nickName }}</text>
					<image v-if="String(gender) === '2'" class="name-type" src="../../static/girl.png"></image>
					<image v-else class="name-type" src="../../static/boy.png"></image>
				</view>
				<view class="phone">
					<text class="phone-text">{{ getPhoneNum(phoneNumber) }}</text>
				</view>
			</view>
		</view>

		<scroll-view class="container" scroll-y="true">
			<view class="ai-service-card">
				<view class="ai-service-main" @click="goAgent">
					<image src="../../static/takeout-guys-bot.png"></image>
					<view>
						<text>小慧 AI 助手</text>
						<text>问订单、配送和售后</text>
					</view>
					<text class="go">›</text>
				</view>
				<view class="ai-service-actions">
					<view class="ai-action ai-recommend" @click="goRecommend">AI 帮我选菜</view>
					<view class="ai-action ai-service" @click="goService">转人工客服</view>
				</view>
			</view>

			<view class="menu-card">
				<view class="menu-item" @click="goAddress">
					<image class="location" src="../../static/address.png"></image>
					<text class="menu-word">地址管理</text>
					<uni-icons class="to-right" type="arrowright" color="#ff4b12" size="16" />
				</view>
				<view class="menu-item" @click="goOrder">
					<image class="location" src="../../static/order.png"></image>
					<text class="menu-word">历史订单</text>
					<uni-icons class="to-right" type="arrowright" color="#ff4b12" size="16" />
				</view>
				<view class="menu-item" @click="goReview">
					<image class="location" src="../../static/edit.png"></image>
					<text class="menu-word">客户评价</text>
					<uni-icons class="to-right" type="arrowright" color="#ff4b12" size="16" />
				</view>
				<view class="menu-item" @click="goCoupon">
					<image class="location" src="../../static/money.png"></image>
					<text class="menu-word">优惠券中心</text>
					<uni-icons class="to-right" type="arrowright" color="#ff4b12" size="16" />
				</view>
				<view class="menu-item" @click="goService">
					<image class="location" src="../../static/btn_waiter_sel.png"></image>
					<text class="menu-word">人工客服</text>
					<uni-icons class="to-right" type="arrowright" color="#ff4b12" size="16" />
				</view>
			</view>

			<view class="recent-orders">
				<view v-if="recentOrdersList && recentOrdersList.length > 0" class="recent">
					<text class="order-line">最近订单</text>
				</view>
				<view class="order-lists" v-for="(item, index) in recentOrdersList" :key="index">
					<view class="date-type">
						<text class="time">{{ item.checkoutTime }}</text>
						<text class="type" :class="{ status: item.status === 2 }">{{ statusWord(item.status) }}</text>
					</view>
					<view class="food-num">
						<view class="food-num-item" v-for="(num, y) in item.orderDetailList" :key="y">
							<text class="food">{{ num.name }}</text>
							<text class="num">x{{ num.number }}</text>
						</view>
					</view>
					<view class="food-sum">
						<view>共 {{ sumOrder.number }} 件商品 实付 <text>￥{{ sumOrder.amount }}</text></view>
					</view>
					<view class="again-btn" v-if="item.status === 5">
						<button class="new-btn" type="default" @click="oneOrderFun(item.id)">再来一单</button>
					</view>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
import { queryOrderUserPage, oneOrderAgain, delShoppingCart } from '../api/api.js'
import { mapMutations } from 'vuex'
import uniIcons from '../../components/uni-icons/uni-icons.vue'

export default {
	components: {
		uniIcons
	},
	data() {
		return {
			personUrl: '../../static/btn_waiter_sel.png',
			nickName: '用户',
			gender: '0',
			phoneNumber: '18500557668',
			recentOrdersList: [],
			sumOrder: {
				amount: 0,
				number: 0
			}
		}
	},
	onLoad() {
		this.personUrl = this.$store.state.baseUserInfo && this.$store.state.baseUserInfo.avatarUrl
		this.nickName = this.$store.state.baseUserInfo && this.$store.state.baseUserInfo.nickName
		this.gender = this.$store.state.baseUserInfo && this.$store.state.baseUserInfo.gender
		this.getList()
	},
	methods: {
		...mapMutations(['setAddressBackUrl']),
		getPhoneNum(str) {
			return String(str || '').replace(/(\d{3})\d*(\d{4})/, '$1****$2')
		},
		statusWord(status) {
			switch (status) {
				case 1:
					return '待付款'
				case 2:
					return '待接单'
				case 3:
					return '已接单'
				case 4:
					return '派送中'
				case 5:
					return '已完成'
				case 6:
					return '已取消'
				case 7:
					return '已退款'
				default:
					return '处理中'
			}
		},
		getList() {
			queryOrderUserPage({ pageSize: 1, page: 1 }).then(res => {
				if (res.code !== 1) return
				const data = res.data || {}
				let number = 0
				let amount = 0
				if ((data.records || []).length > 0) {
					(data.records[0].orderDetailList || []).forEach(item => {
						number += item.number
						amount += item.amount
					})
				}
				this.sumOrder = { amount, number }
				this.recentOrdersList = data.records || []
			})
		},
		goAddress() {
			this.setAddressBackUrl('/pages/my/my')
			uni.redirectTo({ url: '/pages/address/address?form=my' })
		},
		goOrder() {
			uni.navigateTo({ url: '/pages/historyOrder/historyOrder' })
		},
		goAgent() {
			uni.navigateTo({ url: '/pages/ai/chat' })
		},
		goRecommend() {
			uni.navigateTo({ url: '/pages/ai/recommend' })
		},
		goReview() {
			uni.navigateTo({ url: '/pages/review/index' })
		},
		goCoupon() {
			uni.navigateTo({ url: '/pages/coupon/index?mode=center' })
		},
		goService() {
			uni.navigateTo({ url: '/pages/service/index' })
		},
		async oneOrderFun(id) {
			const pages = getCurrentPages()
			const routeIndex = pages.findIndex(item => item.route === 'pages/index/index')
			await delShoppingCart()
			oneOrderAgain({ id }).then(res => {
				if (res.code === 1) {
					uni.navigateBack({
						delta: routeIndex > -1 ? (pages.length - routeIndex) : 1
					})
				}
			})
		}
	}
}
</script>

<style lang="scss" scoped>
.ai-service-card{margin:24rpx;background:#101a2a;border-radius:28rpx;color:#fff;overflow:hidden;box-shadow:0 16rpx 34rpx rgba(16,26,42,.15)}
.ai-service-main{display:flex;align-items:center;padding:22rpx}
.ai-service-main image{width:74rpx;height:74rpx;border-radius:19rpx;margin-right:16rpx}
.ai-service-main>view{display:flex;flex:1;flex-direction:column}
.ai-service-main>view text:first-child{font-size:28rpx;font-weight:700}
.ai-service-main>view text:last-child{font-size:21rpx;color:#8e9aa8}
.ai-service-main .go{font-size:50rpx;color:#ff5a22}
.ai-service-actions{display:flex}
.ai-action{flex:1;padding:15rpx 22rpx;text-align:center;font-size:23rpx;font-weight:600}
.ai-recommend{background:#ff4b12}
.ai-service{background:rgba(255,255,255,.12)}
.my-center{background:#f6f6f6;height:100%}
.my-info{height:172rpx;width:750rpx;background:#101a2a;display:flex}
.head{width:172rpx;height:172rpx;margin:auto;text-align:center}
.head-image{width:116rpx;height:116rpx;line-height:172rpx;vertical-align:top;margin:20rpx auto;border-radius:50%;background-color:#fff}
.phone-name{flex:1;margin:auto}
.name-text{font-size:32rpx;font-weight:550;text-align:left;color:#fff;height:44rpx;line-height:44rpx;margin-right:12rpx}
.name-type{width:32rpx;height:32rpx;vertical-align:middle;margin-bottom:6rpx}
.phone-text{height:40rpx;font-size:28rpx;font-weight:400;text-align:left;color:#d6dde6;line-height:40rpx}
.container{margin-top:20rpx;height:calc(100% - 194rpx)}
.menu-card{width:710rpx;border-radius:16rpx;background-color:#fff;margin:20rpx auto 0}
.menu-item{line-height:100rpx;position:relative;border-top:1px dashed #ebebeb;margin-left:30rpx;margin-right:20rpx}
.menu-item:first-child{border-top:none}
.location{width:34rpx;height:36rpx;margin-right:8rpx;vertical-align:middle;margin-bottom:4rpx}
.menu-word{font-size:28rpx;font-weight:400;text-align:center;color:#333;line-height:40rpx}
.to-right{width:30rpx;height:30rpx;display:flex;align-items:center;justify-content:center;position:absolute;top:50%;right:0;transform:translateY(-50%)}
.recent-orders{width:710rpx;border-radius:16rpx;background-color:#fff;margin:20rpx auto}
.recent{height:120rpx;padding:0 16rpx 0 22rpx}
.order-line{font-size:32rpx;font-weight:550;text-align:left;color:#333;line-height:120rpx;display:block;width:100%;border-bottom:1px solid #efefef;padding-left:6rpx}
.date-type{margin:0 16rpx 0 28rpx;border-bottom:1px dashed #efefef;height:100rpx}
.time{display:inline-block;font-size:28rpx;color:#333;height:100rpx;line-height:100rpx}
.type{display:inline-block;font-size:28rpx;color:#666;height:100rpx;line-height:100rpx;float:right;padding-right:14rpx}
.status{color:#ff4b12}
.food-num{margin:0 30rpx;padding-bottom:32rpx}
.food-num-item{margin-top:20rpx;height:40rpx;line-height:40rpx}
.food-num-item:first-child{margin-top:30rpx}
.food,.num{font-size:28rpx;color:#666}
.num{float:right}
.food-sum{display:flex;justify-content:flex-end;margin-right:34rpx;padding-bottom:40rpx;height:40rpx;font-size:28rpx;color:#666;line-height:40rpx}
.food-sum text{font-weight:600;color:#333}
.again-btn{padding-bottom:32rpx;margin-right:20rpx;height:72rpx}
.new-btn{float:right;width:248rpx;height:72rpx;line-height:68rpx;border:1px solid #e5e4e4;background-color:#fff;border-radius:38rpx;font-size:28rpx;font-weight:500;color:#333}
</style>
