<template>
	<view class="my-center">
		<!-- 头像展示部分 -->
		<view class="my_info">
			<!-- 头像部分 -->
			<view class="head">
				<image class="head_image" :src="psersonUrl"></image>
			</view>
			<!-- 姓名及手机号 -->
			<view class="phone_name">
				<!-- 姓名 -->
				<view class="name">
					<text class="name_text">{{ nickName }}</text>
					<image v-if="String(gender) === '2'" class="name_type" src="../../static/girl.png"></image>
					<image v-else class="name_type" src="../../static/boy.png"></image>
				</view>
				<!-- 电话号 -->
				<view class="phone">
					<text class="phone_text">{{ getPhoneNum(phoneNumber) }}</text>
				</view>
			</view>
		</view>
		
		<scroll-view class="container" scroll-y="true">
			<view class="ai-service-card">
				<view class="ai-service-main" @click="goAgent"><image src="../../static/takeout-guys-bot.png"></image><view><text>小慧 AI 助手</text><text>问订单、配送和售后</text></view><text class="go">›</text></view>
				<view class="ai-recommend" @click="goRecommend">AI 帮我选菜</view>
			</view>
			<!-- 地址和历史订单 -->
			<view class="address_order">
				<!-- 地址管理 -->
				<view class="address" @click="goAddress">
					<image class="location" src="../../static/address.png"></image>
					<text class="address_word">地址管理</text>
					<uni-icons class="to_right" type="arrowright" color="#ff4b12" size="16" />
				</view>
				<!-- 历史订单 -->
				<view class="order" @click="goOrder">
					<image class="location" src="../../static/order.png"></image>
					<text class="order_word">历史订单</text>
					<uni-icons class="to_right" type="arrowright" color="#ff4b12" size="16" />
				</view>
				<view class="order" @click="goReview">
					<image class="location" src="../../static/edit.png"></image>
					<text class="order_word">客户评价</text>
					<uni-icons class="to_right" type="arrowright" color="#ff4b12" size="16" />
				</view>
				<view class="order" @click="goCoupon">
					<image class="location" src="../../static/money.png"></image>
					<text class="order_word">优惠券中心</text>
					<uni-icons class="to_right" type="arrowright" color="#ff4b12" size="16" />
				</view>
			</view>
			
			<!-- 最近订单 -->
			<view class="recent_orders">
				<!-- 最近订单title -->
				<view class="recent" v-if="recentOrdersList && recentOrdersList.length>0">
					<text class="order_line">最近订单</text>
				</view>
				<!-- 最近订单列表 -->
				<view class="order_lists" v-for="(item, index) in recentOrdersList" :key="index">
					<!-- 时间和支付状态 -->
					<view class="date_type">
						<!-- 时间 -->
						<text class="time">{{ item.checkoutTime }}</text>
						<!-- 支付状态 -->
						<text class="type" :class="{'status': item.status==2}">{{ statusWord(item.status) }}</text>
					</view>
					<!-- 点菜的内容 -->
					<view class="food_num">
						<view class="food_num_item" v-for="(num, y) in item.orderDetailList" :key="y">
							<text class="food">{{ num.name }}</text>
							<text class="num">x{{ num.number }}</text>
						</view>
					</view>
					<view class="food_sum">
						<view>共{{sumOrder.number}}件商品,实付<text>{{'￥' + sumOrder.amount}}</text></view>
					</view>
					<view class="againBtn" v-if="item.status === 5">
						<button class="new_btn" type="default" @click="oneOrderFun(item.id)">再来一单</button>
					</view>
				</view>
			</view>
<!-- 			<view class="quit" @click="quitClick">退出登录</view>
 -->		</scroll-view>
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
	data () {
		return {
			psersonUrl: '../../static/btn_waiter_sel.png',
			nickName: '林之迷',
			gender: '0',
			phoneNumber: '18500557668',
			recentOrdersList: [],
			sumOrder:{
				amount:0,
				number:0
			}
		}
	},
	onLoad () {
		this.psersonUrl = this.$store.state.baseUserInfo && this.$store.state.baseUserInfo.avatarUrl
		this.nickName = this.$store.state.baseUserInfo && this.$store.state.baseUserInfo.nickName
		this.gender = this.$store.state.baseUserInfo && this.$store.state.baseUserInfo.gender
		this.getList()
	},
	methods: {
    ...mapMutations(['setAddressBackUrl']),
		getPhoneNum (str) {
			return String(str || '').replace(/(\d{3})\d*(\d{4})/, '$1****$2')
		},
		statusWord (status) {
			switch (status) {
				case 1:
				return '待付款'
				case 2: return '待接单'
				case 3: return '已接单'
				case 4: return '派送中'
				case 5: return '已完成'
				case 6: return '已取消'
				case 7: return '已退款'
			}
		},
		getList () {
			const params = {
				pageSize: 1,
				page: 1
			}
			queryOrderUserPage(params).then(res => {
				if (res.code === 1) {
					const data = res.data
					let number = 0
					let amount = 0
					if(data.records.length > 0){
						(data.records[0].orderDetailList || []).forEach(item=>{
							number += item.number
							amount += item.amount
						})
						this.sumOrder = {
							amount: amount,
							number:number
						}
					}
					this.recentOrdersList = data.records
					console.log('-=-this.recentOrdersList-=-',this.recentOrdersList)
				}
			})
		},
		goAddress () {
      this.setAddressBackUrl('/pages/my/my')
			// TODO
			uni.redirectTo({
				url: '/pages/address/address?form=' + 'my'
			})
		},
		goOrder () {
			console.log('-=-=goOrder-=-=-')
			// TODO
			uni.navigateTo({
				url: '/pages/historyOrder/historyOrder'
			})
		},
		goAgent () { uni.navigateTo({ url: '/pages/ai/chat' }) },
		goRecommend () { uni.navigateTo({ url: '/pages/ai/recommend' }) },
		goReview () { uni.navigateTo({ url: '/pages/review/index' }) },
		goCoupon () { uni.navigateTo({ url: '/pages/coupon/index?mode=center' }) },
		async oneOrderFun (id) {
			let pages = getCurrentPages()
			let routeIndex = pages.findIndex(item=>item.route==='pages/index/index')
			// 先清空购物车
      await delShoppingCart()
			oneOrderAgain({ id }).then(res => {
				if (res.code === 1) {
					uni.navigateBack({
						delta: routeIndex>-1?(pages.length-routeIndex):1
					})
					// uni.redirectTo({
					// 	url: '/pages/index/index?formOrder=' + 'oneMoreOrder'
					// })
				}
			})
		},
	}
}
</script>

<style lang="scss" scoped>
	.ai-service-card{margin:24rpx;background:#101a2a;border-radius:28rpx;color:#fff;overflow:hidden;box-shadow:0 16rpx 34rpx rgba(16,26,42,.15)}
	.ai-service-main{display:flex;align-items:center;padding:22rpx}.ai-service-main image{width:74rpx;height:74rpx;border-radius:19rpx;margin-right:16rpx}.ai-service-main>view{display:flex;flex:1;flex-direction:column}.ai-service-main>view text:first-child{font-size:28rpx;font-weight:700}.ai-service-main>view text:last-child{font-size:21rpx;color:#8e9aa8}.ai-service-main .go{font-size:50rpx;color:#ff5a22}.ai-recommend{padding:15rpx 22rpx;background:#ff4b12;text-align:center;font-size:23rpx;font-weight:600}
	.my-center {
		background: #f6f6f6;
		height: 100%;
		.my_info {
			height: 172rpx;
			width: 750rpx;
			background: #101a2a;
			display: flex;
			// 头像
			.head {
				width: 172rpx;
				height: 172rpx;
				margin: auto;
				text-align: center;
				.head_image {
					width: 116rpx;
					height: 116rpx;
					line-height: 172rpx;
					vertical-align: top;
					margin: 20rpx auto;
					border-radius: 50%;
					background-color: #fff;
				}
			}
			// 姓名电话号
			.phone_name {
				flex: 1;
				margin: auto;
				.name {
					.name_text {
						font-size: 32rpx;
						opacity: 1;
						font-family: PingFangSC, PingFangSC-Medium;
						font-weight: 550;
						text-align: left;
						color: #333333;
						height: 44rpx;
						line-height: 44rpx;
						margin-right: 12rpx;
					}
					
					.name_type {
						width: 32rpx;
						height: 32rpx;
						vertical-align: middle;
						margin-bottom: 6rpx;
					}
				}
				.phone {
					.phone_text {
						height: 40rpx;
						opacity: 1;
						font-size: 28rpx;
						font-family: PingFangSC, PingFangSC-Regular;
						font-weight: 400;
						text-align: left;
						color: #333333;
						line-height: 40rpx;
					}
				}
			}
		}
		
		.container{
			margin-top: 20rpx;
			height: calc(100% - 194rpx);
			// 地址及订单
			.address_order {
				width: 710rpx;
				height: 400rpx;
				border-radius: 16rpx;
				background-color: #fff;
				margin:20rpx auto;
				margin-top: 0;
				// 地址
				.address {
					line-height: 100rpx;
					position: relative;
					.location {
						width: 34rpx;
						height: 36rpx;
						margin-right: 8rpx;
						vertical-align: middle;
						margin-bottom: 4rpx;
						padding-left: 30rpx;
					}
					.address_word {
						opacity: 1;
						font-size: 28rpx;
						font-family: PingFangSC, PingFangSC-Regular;
						font-weight: 400;
						text-align: center;
						color: #333333;
						line-height: 40rpx;
					}
					.to_right {
						width: 30rpx;
						height: 30rpx;
						display: flex;
						align-items: center;
						justify-content: center;
						position: absolute;
						top: 50%;
						right: 20rpx;
						transform: translateY(-50%);
					}
				}
				// 订单
				.order {
					line-height: 100rpx;
					position: relative;
					border-top: 1px dashed #ebebeb;
					margin-left: 30rpx;
					margin-right: 20rpx;
					.location {
						width: 34rpx;
						height: 36rpx;
						margin-right: 8rpx;
						vertical-align: middle;
						margin-bottom: 4rpx;
					}
					.order_word {
						opacity: 1;
						font-size: 28rpx;
						font-family: PingFangSC, PingFangSC-Regular;
						font-weight: 400;
						text-align: center;
						color: #333333;
						line-height: 40rpx;
					}
					.to_right {
						width: 30rpx;
						height: 30rpx;
						display: flex;
						align-items: center;
						justify-content: center;
						position: absolute;
						top: 50%;
						right: 0;
						transform: translateY(-50%);
					}
				}
			}

			// 最近订单
			.recent_orders {
				width: 710rpx;
				border-radius: 16rpx;
				background-color: #fff;
				margin:20rpx auto;
				.recent {
					height: 120rpx;
					padding: 0 16rpx 0 22rpx;
					.order_line {
						opacity: 1;
						font-size: 32rpx;
						font-family: PingFangSC, PingFangSC-Medium;
						font-weight: 550;
						text-align: left;
						color: #333333;
						line-height: 120rpx;
						letter-spacing: 0px;
						display: block;
						width: 100%;
						border-bottom: 1px solid #efefef;
						padding-left: 6rpx;
					}
				}
				
				// 最近订单列表
				.order_lists {
					.date_type {
						margin: 0 16rpx 0 28rpx;
						border-bottom: 1px dashed #efefef;
						height: 100rpx;
						.time {
							
							display: inline-block;
							opacity: 1;
							font-size: 28rpx;
							font-family: PingFangSC, PingFangSC-Regular;
							font-weight: 400;
							text-align: left;
							color: #333333;
							height: 100rpx;
							line-height: 100rpx;
							letter-spacing: 0px;
						}
						.type {
							display: inline-block;
							opacity: 1;
							font-size: 28rpx;
							font-family: PingFangSC, PingFangSC-Regular;
							font-weight: 400;
							text-align: left;
							color: #666666;
							height: 100rpx;
							line-height: 100rpx;
							letter-spacing: 0px;
							float: right;
							padding-right: 14rpx;
						}
						.status{
							color: #ff4b12;
						}
					}
					.food_num {
						margin: 0 30rpx;
						padding-bottom: 32rpx;
						.food_num_item {
							margin-top: 20rpx;
							height: 40rpx;
							line-height: 40rpx;
							.food, .num {
								// height: 20px;
								opacity: 1;
								font-size: 28rpx;
								font-family: PingFangSC, PingFangSC-Regular;
								font-weight: 400;
								text-align: left;
								color: #666666;
								// line-height: 20px;
								letter-spacing: 0px;
							}
							.num {
								float: right;
							}
							&:first-child {
								margin-top: 30rpx;
							}
						}
					}
					.food_sum{
						display: flex;
						justify-content: flex-end;
						margin-right: 34rpx;
						padding-bottom: 40rpx;
						height: 40rpx;
						opacity: 1;
						font-size: 28rpx;
						font-family: PingFangSC, PingFangSC-Regular;
						font-weight: 400;
						text-align: left;
						color: #666666;
						line-height: 40rpx;
						text{
							font-family: PingFangSC, PingFangSC-Medium;
							color: #333333;
						}
					}
					// 按钮部分
					.againBtn {
						// margin: right;
						padding-bottom: 32rpx;
						margin-right: 20rpx;
						height: 72rpx;
						.new_btn {
							float: right;
							width: 248rpx;
							height: 72rpx;
							line-height: 68rpx;
							border: 1px solid #e5e4e4;
							background-color: #fff;
							border-radius: 38rpx;
							font-size: 28rpx;
							font-family: PingFangSC, PingFangSC-Medium;
							font-weight: 500;
							color: #333333;
						}
					}
				}
			}
			.quit{
				    width: 710rpx;
					height: 100rpx;
					opacity: 1;
					background: #ffffff;
					border-radius: 12rpx;
					margin: 20rpx auto;
					font-size: 30rpx;
					font-family: PingFangSC, PingFangSC-Medium;
					font-weight: 500;
					text-align: center;
					color: #333333;
					line-height: 100rpx;
			}
		}
		}
	
</style>
