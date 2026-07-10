<template>
	<view class="review-list-page">
		<view class="hero">
			<image class="hero-image" :src="dishImageUrl" mode="aspectFill"></image>
			<view class="hero-copy">
				<text class="hero-kicker">REAL USER REVIEWS</text>
				<text class="hero-title">{{ dishName || '菜品评价' }}</text>
				<text class="hero-desc">看看大家怎么评价这道菜，再决定要不要下单。</text>
			</view>
		</view>

		<view class="summary">
			<view class="summary-card">
				<text class="summary-label">已加载均分</text>
				<text class="summary-value">{{ averageRatingText }}</text>
			</view>
			<view class="summary-card">
				<text class="summary-label">评价总数</text>
				<text class="summary-value">{{ total }}</text>
			</view>
			<view class="summary-card">
				<text class="summary-label">有图评价</text>
				<text class="summary-value">{{ imageReviewCount }}</text>
			</view>
		</view>

		<view v-if="loading && !reviews.length" class="state-card">正在加载评价...</view>
		<view v-else-if="!reviews.length" class="state-card">还没有用户评价，欢迎成为第一个分享体验的人。</view>

		<view v-else class="review-list">
			<view class="review-card" v-for="item in reviews" :key="item.id">
				<view class="review-head">
					<view class="review-user">
						<text class="review-avatar">{{ displayName(item).slice(0, 1) }}</text>
						<view class="review-user-copy">
							<text class="review-name">{{ displayName(item) }}</text>
							<text class="review-time">{{ formatTime(item.createTime) }}</text>
						</view>
					</view>
					<text class="review-rating">{{ reviewStars(item.rating) }}</text>
				</view>

				<view class="review-content">{{ item.content || '该用户没有留下文字评价。' }}</view>

				<view v-if="item.images && item.images.length" class="review-images">
					<image
						v-for="(img, index) in item.images"
						:key="`${item.id}-${index}`"
						:src="getImage(img)"
						mode="aspectFill"
						@click="previewImage(item.images, index)"
					></image>
				</view>

				<view class="review-foot">
					<text>{{ item.likeCount || 0 }} 人觉得有帮助</text>
					<button class="like-btn" :class="{ active: item.liked }" @click="toggleLike(item)">
						{{ item.liked ? '已点赞' : '点赞' }}
					</button>
				</view>
			</view>
		</view>

		<view v-if="reviews.length" class="load-state">
			<text v-if="loadingMore">正在加载更多...</text>
			<text v-else-if="finished">已经到底啦</text>
			<text v-else>上滑加载更多</text>
		</view>
	</view>
</template>

<script>
import { queryDishReviews, toggleReviewLike } from '../api/api.js'
import { baseUrl } from '../../utils/env'

export default {
	data() {
		return {
			dishId: null,
			dishName: '',
			dishImage: '',
			page: 1,
			pageSize: 10,
			total: 0,
			reviews: [],
			loading: false,
			loadingMore: false,
			finished: false
		}
	},
	computed: {
		dishImageUrl() {
			return this.getImage(this.dishImage)
		},
		averageRatingText() {
			if (!this.reviews.length) return '暂无'
			const sum = this.reviews.reduce((total, item) => total + Number(item.rating || 0), 0)
			return (sum / this.reviews.length).toFixed(1)
		},
		imageReviewCount() {
			return this.reviews.filter(item => Array.isArray(item.images) && item.images.length > 0).length
		}
	},
	onLoad(options) {
		this.dishId = Number(options.dishId || 0) || null
		this.dishName = decodeURIComponent(options.name || '')
		this.dishImage = decodeURIComponent(options.image || '')
		this.loadReviews()
	},
	onPullDownRefresh() {
		this.reloadReviews()
	},
	onReachBottom() {
		this.loadMore()
	},
	methods: {
		getImage(image) {
			if (!image) return '../../static/takeout-guys-bot.png'
			if (/^https?:\/\//.test(image)) return image
			return `${baseUrl}${image}`
		},
		displayName(item) {
			return item.userName || `用户#${item.userId || ''}`
		},
		formatTime(value) {
			if (!value) return '刚刚'
			return String(value).replace('T', ' ').slice(0, 16)
		},
		reviewStars(rating) {
			const count = Math.max(0, Math.min(5, Number(rating || 0)))
			return '★'.repeat(count) + '☆'.repeat(5 - count)
		},
		previewImage(list, current) {
			const urls = (list || []).map(this.getImage)
			if (!urls.length) return
			uni.previewImage({
				urls,
				current: urls[current] || urls[0]
			})
		},
		async loadReviews(reset = true) {
			if (!this.dishId) {
				this.loading = false
				this.loadingMore = false
				this.finished = true
				return
			}
			if (reset) {
				this.loading = true
				this.page = 1
				this.finished = false
			} else {
				this.loadingMore = true
			}
			try {
				const res = await queryDishReviews(this.dishId, { page: this.page, pageSize: this.pageSize })
				const payload = res && res.data ? res.data : {}
				const records = Array.isArray(payload.records) ? payload.records : []
				this.total = Number(payload.total || 0)
				this.reviews = reset ? records : this.reviews.concat(records)
				this.finished = this.reviews.length >= this.total || records.length < this.pageSize
			} catch (error) {
				if (reset) this.reviews = []
			} finally {
				this.loading = false
				this.loadingMore = false
				uni.stopPullDownRefresh()
			}
		},
		async reloadReviews() {
			await this.loadReviews(true)
		},
		async loadMore() {
			if (this.loading || this.loadingMore || this.finished) return
			this.page += 1
			await this.loadReviews(false)
		},
		async toggleLike(item) {
			if (!item || !item.id) return
			const liked = Boolean(item.liked)
			const delta = liked ? -1 : 1
			item.liked = !liked
			item.likeCount = Math.max(0, Number(item.likeCount || 0) + delta)
			try {
				await toggleReviewLike(item.id)
			} catch (error) {
				item.liked = liked
				item.likeCount = Math.max(0, Number(item.likeCount || 0) - delta)
				uni.showToast({ title: (error && error.msg) || '点赞失败，请稍后再试', icon: 'none' })
			}
		}
	}
}
</script>

<style lang="scss" scoped>
.review-list-page{min-height:100vh;padding:24rpx;background:#f6f5f1;box-sizing:border-box}
.hero{display:flex;align-items:center;padding:28rpx;background:#101a2a;border-radius:28rpx;color:#fff;box-shadow:0 18rpx 40rpx rgba(16,26,42,.14)}
.hero-image{width:124rpx;height:124rpx;border-radius:24rpx;margin-right:22rpx;background:#fff2eb}
.hero-copy{display:flex;flex:1;flex-direction:column;min-width:0}
.hero-kicker{font-size:18rpx;letter-spacing:2rpx;color:#ff7a4b}
.hero-title{margin-top:8rpx;font-size:34rpx;font-weight:700}
.hero-desc{margin-top:8rpx;font-size:22rpx;line-height:1.6;color:#c7cfda}
.summary{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:16rpx;margin:22rpx 0}
.summary-card{padding:24rpx 20rpx;background:#fff;border-radius:24rpx;box-shadow:0 8rpx 24rpx rgba(16,26,42,.05)}
.summary-label{display:block;font-size:22rpx;color:#88918b}
.summary-value{display:block;margin-top:12rpx;font-size:34rpx;font-weight:700;color:#182232}
.state-card{margin-top:16rpx;padding:90rpx 28rpx;background:#fff;border-radius:28rpx;text-align:center;font-size:24rpx;line-height:1.7;color:#89928d}
.review-list{display:flex;flex-direction:column;gap:18rpx}
.review-card{padding:26rpx;background:#fff;border-radius:26rpx;box-shadow:0 8rpx 24rpx rgba(16,26,42,.05)}
.review-head,.review-foot{display:flex;align-items:center;justify-content:space-between;gap:16rpx}
.review-user{display:flex;align-items:center;min-width:0}
.review-avatar{display:flex;align-items:center;justify-content:center;width:60rpx;height:60rpx;border-radius:50%;background:#fff1eb;color:#dc4311;font-size:24rpx;font-weight:700;margin-right:16rpx;flex-shrink:0}
.review-user-copy{display:flex;flex-direction:column;min-width:0}
.review-name{font-size:26rpx;font-weight:700;color:#182232}
.review-time{margin-top:6rpx;font-size:20rpx;color:#929a95}
.review-rating{font-size:22rpx;letter-spacing:2rpx;color:#ff4b12;flex-shrink:0}
.review-content{margin-top:18rpx;font-size:25rpx;line-height:1.7;color:#49514c;word-break:break-all}
.review-images{display:flex;flex-wrap:wrap;gap:12rpx;margin-top:18rpx}
.review-images image{width:184rpx;height:184rpx;border-radius:18rpx;background:#f1f1ed}
.review-foot{margin-top:18rpx;font-size:22rpx;color:#8e968f}
.like-btn{margin:0;padding:0 24rpx;height:60rpx;line-height:60rpx;border-radius:18rpx;background:#fff1eb;color:#dc4311;font-size:22rpx}
.like-btn.active{background:#ff4b12;color:#fff}
.load-state{padding:28rpx 0 10rpx;text-align:center;font-size:22rpx;color:#949c97}
</style>
