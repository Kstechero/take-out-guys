	// import uniNavBar from '@/uni_modules/uni-nav-bar/components/uni-nav-bar/uni-nav-bar.vue'
	import { 
		openTable, 
		getTableState, 
		getList,payOrder,
		clearOrder, 
		getDishList, 
		addDish, 
		delDish, 
		getTableOrderDishList,
		// 提交订单
		submitOrderSubmit,
		// 查询默认地址
		getAddressBookDefault,
		getOrderAvailableCoupons
	} from '../api/api.js'
	import initWebScoket from '../../utils/webscoket'
import {mapState, mapMutations, mapActions} from 'vuex'
import { baseUrl } from '../../utils/env'
import { getPlatform } from '../../utils/system.js'
import uniIcons from '../../components/uni-icons/uni-icons.vue'

	const ORDER_COUPON_STORAGE_KEY = 'order_selected_coupon'
	
	export default {
		data () {
			return {
				platform: 'ios',
				submittingOrder: false,
				orderDishPrice: 0,
				openPayType: false,
				psersonUrl: '../../static/btn_waiter_sel.png',
				nickName: '',
				gender: '0',
				phoneNumber: '',
				address: '',
				remark: '',
				arrivalTime: '',
				addressBookId: '',
				// 加入购物车数量
				orderDishNumber: 0,
				selectedCoupon: null,
				availableCouponCount: 0,
			}
		},
		computed: {
			tableInfo: function () {
				return this.shopInfo()
			},
			orderListDataes: function () {
				return this.orderListData()
				// return this.orderListData().dishList
			},
			originalPayableAmount () {
				return Number((this.orderDishPrice + 6).toFixed(2))
			},
			couponDiscountAmount () {
				if (!this.selectedCoupon || this.selectedCoupon.discountAmount == null) return 0
				return Math.min(Number(this.selectedCoupon.discountAmount) || 0, this.originalPayableAmount)
			},
			payableAmount () {
				return Number(Math.max(0, this.originalPayableAmount - this.couponDiscountAmount).toFixed(2))
			},
			couponHint () {
				if (this.selectedCoupon) return '已选择优惠券'
				if (this.availableCouponCount > 0) return `${this.availableCouponCount} 张可用`
				return '暂无可用优惠券'
			},
			couponValueText () {
				if (!this.selectedCoupon) return '去看看'
				return `${this.selectedCoupon.name || '已选优惠券'} · ${this.couponDiscountText(this.selectedCoupon)}`
			}
		},
		components: { uniIcons },
		onLoad (options) {
			this.initPlatform()
			this.psersonUrl = this.$store.state.baseUserInfo && this.$store.state.baseUserInfo.avatarUrl
			this.nickName = this.$store.state.baseUserInfo && this.$store.state.baseUserInfo.nickName
			this.gender = this.$store.state.baseUserInfo && this.$store.state.baseUserInfo.gender
			this.init()
			// 获取一小时以后的时间
			this.getHarfAnOur()
			// 存在options说明换地址了
			if (options && options.address) {
				this.addressBookId = ''
				const newAddress = JSON.parse(options.address)
				this.address = newAddress.provinceName + newAddress.cityName + newAddress.districtName + newAddress.detail
				this.phoneNumber = newAddress.phone
				this.nickName = newAddress.consignee
				this.gender = newAddress.sex
				this.addressBookId = newAddress.id
			}
			
			// 默认地址查询
			this.getAddressBookDefault()
		},
		onShow () {
			this.syncSelectedCoupon()
			this.loadOrderCoupons()
		},
		methods: {
			...mapState(['shopInfo', 'orderListData']),
      ...mapMutations(['setAddressBackUrl']),
			init () {
				this.computOrderInfo()
			},
			initPlatform(){
				this.platform = getPlatform()
			},
			// 获取一小时以后的时间
			getHarfAnOur () {
				const date = new Date()
				date.setTime(date.getTime() + 3600000)
				let hours = date.getHours()
				let minutes = date.getMinutes()
				if (hours < 10) hours = '0' + hours
				if (minutes < 10) minutes = '0' + minutes
				this.arrivalTime = hours + ':' + minutes
			},
			// 默认地址查询
			getAddressBookDefault () {
				getAddressBookDefault().then(res => {
					if (res.code === 1) {
						this.addressBookId = ''
						this.address = res.data.provinceName + res.data.cityName + res.data.districtName + res.data.detail
						this.phoneNumber = res.data.phone
						this.nickName = res.data.consignee
						this.gender = res.data.sex
						this.addressBookId = res.data.id
					}
				})
			},
			// 去地址页面
			goAddress () {
        this.setAddressBackUrl('/pages/order/index')
				uni.redirectTo({
					url: '/pages/address/address'
				})
			},
			// 重新拼装image
			getNewImage (image) {
				if (!image) return '../../static/takeout-guys-bot.png'
				if (/^https?:\/\//.test(image)) return image
				return `${baseUrl}${image}`
			},
			// 订单里和总订单价格计算
			computOrderInfo () {
				let oriData = this.orderListDataes || []
				this.orderDishNumber = this.orderDishPrice = 0
				this.orderDishPrice = 0
				oriData.map((n,i) => {
					// this.orderDishPrice += n.number * n.price
					this.orderDishPrice += n.number * n.amount
					this.orderDishNumber += n.number
				})
				this.loadOrderCoupons()
			},
			syncSelectedCoupon () {
				this.selectedCoupon = uni.getStorageSync(ORDER_COUPON_STORAGE_KEY) || null
			},
			extractCouponList (data) {
				if (Array.isArray(data)) return data
				if (Array.isArray(data?.records)) return data.records
				if (Array.isArray(data?.items)) return data.items
				return []
			},
			couponDiscountText (coupon) {
				if (!coupon) return ''
				if (coupon.discountAmount != null) return `减${coupon.discountAmount}元`
				if (coupon.discountRate != null) return `${Number(coupon.discountRate) * 10}折`
				return '优惠券'
			},
			loadOrderCoupons () {
				if (!this.payableAmount) return
				getOrderAvailableCoupons({ amount: this.originalPayableAmount }).then(res => {
					this.availableCouponCount = this.extractCouponList(res.data).length
				}).catch(() => {
					this.availableCouponCount = 0
				})
			},
			goCouponPage () {
				uni.navigateTo({
					url: `/pages/coupon/index?mode=order&amount=${this.originalPayableAmount}`
				})
			},
			// 返回上一级
			goback () {
				uni.navigateBack()
			},
			closeMask () {
				this.openPayType = false
			},
			// 支付下单
			async payOrderHandle () {
				if (this.submittingOrder) return
				if (!this.address) {
					uni.showToast({
						title: '请选择收货地址',
						icon: 'none',
					})
					return false
				}
				if (!this.orderDishNumber) {
					uni.showToast({
						title: '购物车为空，暂不能下单',
						icon: 'none',
					})
					return false
				}
				const params = {
					payMethod: 1,
					addressBookId: this.addressBookId,
					remark: this.remark,
					deliveryStatus: 1,
					tablewareStatus: 1,
					tablewareNumber: 0,
					packAmount: 0,
					amount: this.originalPayableAmount,
					couponId: this.selectedCoupon && this.selectedCoupon.id ? this.selectedCoupon.id : undefined
				}
				this.submittingOrder = true
				uni.showLoading({
					title: '提交中...',
					mask: true
				})
				try {
					const submitRes = await submitOrderSubmit(params)
					if (submitRes.code === 1) {
						await payOrder({ orderNumber: submitRes.data.orderNumber, payMethod: 1 })
						uni.removeStorageSync(ORDER_COUPON_STORAGE_KEY)
						uni.redirectTo({
							url: '/pages/order/success'
						})
						return
					}
					uni.showToast({
						title: submitRes.msg || '下单失败',
						icon: 'none',
					})
				} catch (error) {
					uni.showToast({
						title: (error && error.msg) || '支付失败，请稍后重试',
						icon: 'none',
					})
				} finally {
					this.submittingOrder = false
					uni.hideLoading()
				}
			}
			
			// async payOrderHandle () {
			// 	uni.login({success: (res) => {
			// 		if(res.errMsg == 'login:ok'){
			// 			const params = {tableId: this.shopInfo().tableId, jsCode: res.code}
			// 			payOrder(params).then(async res => {
			// 				if (res.code == 400){
			// 					this.openPayType = true
			// 					return false
			// 				} else {
			// 					const params = JSON.parse(res.data)
			// 					uni.requestPayment({
			// 						timeStamp: params.timeStamp,
			// 						nonceStr: params.nonceStr,
			// 						package: params.package,
			// 						signType: 'MD5',
			// 						paySign: params.paySign,
			// 						success: (resc) => {
			// 						  console.log('支付成功')
			// 						  uni.navigateTo({url: '/pages/order/success'})
			// 						  console.log(resc)
			// 						},
			// 						fail: (err) => {
			// 							uni.showToast({
			// 								title: '取消支付',
			// 								icon: 'none',
			// 							})
			// 							console.log(err)
			// 						}
			// 					})
			// 				}
			// 			}).catch(err => {
			// 				uni.showToast({
			// 					title: res.message,
			// 					icon: 'none',
			// 				})
			// 			})
			// 		} else {
			// 			uni.showToast({
			// 				title: '出错了， 请稍后再试！',
			// 				icon: 'none',
			// 			})
			// 		}
			// 	}})
			// 	console.log('支付下单')
			// }
		}
	}
