<template>
	<view class="coupon-page">
		<view v-if="mode === 'center'" class="header-card">
			<text class="header-title">优惠券中心</text>
			<text class="header-desc">领券、查看我的优惠券，都在这里完成</text>
		</view>

		<view v-if="mode === 'center'" class="tabs">
			<view :class="['tab', { active: centerTab === 'available' }]" @click="switchCenterTab('available')">可领取</view>
			<view :class="['tab', { active: centerTab === 'mine' }]" @click="switchCenterTab('mine')">我的优惠券</view>
		</view>

		<view v-if="mode === 'center' && centerTab === 'mine'" class="status-tabs">
			<text v-for="item in statusTabs" :key="item.value" :class="{ active: myStatus === item.value }" @click="switchStatus(item.value)">{{ item.label }}</text>
		</view>

		<view v-if="mode === 'order'" class="order-tip">
			<text>当前订单金额：¥{{ amount }}</text>
			<text>这里只展示满足当前金额门槛的可用券，下单成功后自动核销。</text>
		</view>

		<view v-if="mode === 'order'" class="coupon-card none-card" @click="clearCoupon">
			<view class="coupon-main">
				<text class="coupon-name">不使用优惠券</text>
				<text class="coupon-meta">按原金额提交订单</text>
			</view>
			<text class="action ghost">清除选择</text>
		</view>

		<view v-if="loading" class="empty">正在加载优惠券…</view>
		<view v-else-if="!couponList.length" class="empty">{{ emptyText }}</view>

		<view v-for="item in couponList" :key="couponKey(item)" class="coupon-card">
			<view class="coupon-main">
				<view class="coupon-head">
					<text class="coupon-name">{{ couponName(item) }}</text>
					<text class="coupon-badge">{{ couponDiscount(item) }}</text>
				</view>
				<text class="coupon-meta">门槛：{{ couponThreshold(item) }}</text>
				<text class="coupon-meta">有效期：{{ couponDateRange(item) }}</text>
				<text v-if="mode === 'center' && centerTab === 'available'" class="coupon-meta">剩余库存：{{ couponStock(item) }}</text>
				<text v-if="mode === 'center' && centerTab === 'mine'" class="coupon-meta">状态：{{ myCouponStatus(item.status) }}</text>
			</view>
			<button v-if="mode === 'center' && centerTab === 'available'" class="action" @click="handleReceive(item)">立即领取</button>
			<button v-else-if="mode === 'order'" class="action" :class="{ selected: isSelected(item) }" @click="selectCoupon(item)">{{ isSelected(item) ? '已选择' : '选择' }}</button>
		</view>
	</view>
</template>

<script>
import { getCouponAvailable, getMyCoupons, getOrderAvailableCoupons, receiveCoupon } from '../api/api.js'

const ORDER_COUPON_STORAGE_KEY = 'order_selected_coupon'

export default {
	data() {
		return {
			mode: 'center',
			centerTab: 'available',
			myStatus: '',
			amount: 0,
			loading: false,
			couponList: [],
			selectedCouponId: null,
			statusTabs: [
				{ label: '全部', value: '' },
				{ label: '未使用', value: 0 },
				{ label: '已使用', value: 1 },
				{ label: '已过期', value: 2 }
			]
		}
	},
	computed: {
		emptyText() {
			if (this.mode === 'order') return '当前订单暂无可用优惠券'
			return this.centerTab === 'available' ? '暂时没有可领取的优惠券' : '你还没有优惠券'
		}
	},
	onLoad(options) {
		this.mode = options.mode || 'center'
		this.amount = Number(options.amount || 0)
		const selectedCoupon = uni.getStorageSync(ORDER_COUPON_STORAGE_KEY) || null
		this.selectedCouponId = selectedCoupon && selectedCoupon.id ? Number(selectedCoupon.id) : null
		this.loadCoupons()
	},
	methods: {
		switchCenterTab(tab) {
			this.centerTab = tab
			this.loadCoupons()
		},
		switchStatus(status) {
			this.myStatus = status
			this.loadCoupons()
		},
		extractList(data) {
			if (Array.isArray(data)) return data
			if (Array.isArray(data?.records)) return data.records
			if (Array.isArray(data?.items)) return data.items
			return []
		},
		couponKey(item) {
			return item.id || item.couponId || item.userCouponId || item.name
		},
		couponName(item) {
			return item.name || item.couponName || '优惠券'
		},
		couponDiscount(item) {
			if (item.discountAmount != null) return `减${item.discountAmount}元`
			if (item.discountRate != null) return `${Number(item.discountRate) * 10}折`
			return '限时优惠'
		},
		couponThreshold(item) {
			const minimum = item.minimumAmount ?? item.thresholdAmount
			if (minimum != null) return `满${minimum}元可用`
			return '无门槛'
		},
		couponStock(item) {
			if (item.remainingCount != null) return item.remainingCount
			if (item.remainingStock != null) return item.remainingStock
			if (item.stock != null) return item.stock
			return '未知'
		},
		couponDateRange(item) {
			const start = item.validFrom || item.startTime || item.receiveTime || item.createTime || '即刻'
			const end = item.validUntil || item.endTime || item.expireTime || '长期有效'
			return `${start} - ${end}`
		},
		myCouponStatus(status) {
			switch (Number(status)) {
				case 0: return '未使用'
				case 1: return '已使用'
				case 2: return '已过期'
				default: return '未知状态'
			}
		},
		isSelected(item) {
			return Number(this.selectedCouponId) === Number(this.couponKey(item))
		},
		async loadCoupons() {
			this.loading = true
			try {
				let res
				if (this.mode === 'order') {
					res = await getOrderAvailableCoupons({ amount: this.amount })
				} else if (this.centerTab === 'mine') {
					const params = {}
					if (this.myStatus !== '') params.status = this.myStatus
					res = await getMyCoupons(params)
				} else {
					res = await getCouponAvailable({ page: 1, pageSize: 20 })
				}
				this.couponList = this.extractList(res.data)
			} catch (error) {
				this.couponList = []
			} finally {
				this.loading = false
			}
		},
		async handleReceive(item) {
			try {
				await receiveCoupon(this.couponKey(item))
				uni.showToast({ title: '领取成功', icon: 'success' })
				this.loadCoupons()
			} catch (error) {
				uni.showToast({ title: '领取失败，请稍后再试', icon: 'none' })
			}
		},
		selectCoupon(item) {
			const stored = {
				id: this.couponKey(item),
				name: this.couponName(item),
				discountAmount: item.discountAmount,
				discountRate: item.discountRate,
				minimumAmount: item.minimumAmount ?? item.thresholdAmount,
				startTime: item.validFrom || item.startTime,
				endTime: item.validUntil || item.endTime
			}
			uni.setStorageSync(ORDER_COUPON_STORAGE_KEY, stored)
			this.selectedCouponId = Number(stored.id)
			uni.showToast({ title: '已选择优惠券', icon: 'success' })
			setTimeout(() => uni.navigateBack(), 300)
		},
		clearCoupon() {
			uni.removeStorageSync(ORDER_COUPON_STORAGE_KEY)
			this.selectedCouponId = null
			uni.showToast({ title: '已清除优惠券', icon: 'success' })
			setTimeout(() => uni.navigateBack(), 300)
		}
	}
}
</script>

<style lang="scss" scoped>
.coupon-page{min-height:100vh;padding:24rpx;background:#f6f5f1;box-sizing:border-box}.header-card{padding:34rpx 30rpx;background:#101a2a;border-radius:28rpx;color:#fff}.header-title{font-size:34rpx;font-weight:700}.header-desc{display:block;margin-top:10rpx;font-size:22rpx;color:#a9b3c0}.tabs{display:flex;gap:16rpx;margin:24rpx 0}.tab{flex:1;padding:22rpx 0;background:#fff;border-radius:22rpx;text-align:center;font-size:26rpx;color:#5d6762}.tab.active{background:#ff4b12;color:#fff;font-weight:600}.status-tabs{display:flex;gap:12rpx;flex-wrap:wrap;margin-bottom:20rpx}.status-tabs text{padding:10rpx 18rpx;background:#fff;border-radius:20rpx;font-size:22rpx;color:#5d6762}.status-tabs .active{background:#fff1eb;color:#d94b1e}.order-tip{padding:24rpx;background:#fff5df;border-radius:22rpx;color:#986912;display:flex;flex-direction:column;font-size:23rpx;line-height:1.6}.order-tip text:last-child{margin-top:8rpx}.coupon-card{display:flex;align-items:center;justify-content:space-between;margin-top:18rpx;padding:26rpx;background:#fff;border-radius:24rpx;box-shadow:0 8rpx 24rpx rgba(16,26,42,.05)}.none-card{border:1rpx dashed #d7ddd7}.coupon-main{display:flex;flex:1;flex-direction:column;padding-right:20rpx}.coupon-head{display:flex;align-items:center;justify-content:space-between;gap:16rpx}.coupon-name{font-size:28rpx;font-weight:700;color:#182232}.coupon-badge{font-size:24rpx;color:#ff4b12;font-weight:700}.coupon-meta{margin-top:8rpx;font-size:22rpx;color:#7d8782;line-height:1.5}.action{margin:0;padding:0 24rpx;height:68rpx;line-height:68rpx;background:#ff4b12;color:#fff;border-radius:20rpx;font-size:24rpx}.action.selected{background:#101a2a}.action.ghost{color:#ff4b12;background:#fff1eb}.empty{padding:120rpx 20rpx;text-align:center;color:#8b938e;font-size:24rpx}
</style>
