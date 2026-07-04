import navBar from '../common/Navbar/navbar.vue'
import uniIcons from '../../components/uni-icons/uni-icons.vue'
import 
	{ 
		openTable, 
		getTableState, 
		getList, 
		clearOrder, 
		getMoreNorm,
		getDishDetail, 
		getDishList, 
		addDish, 
		delDish, 
		getTableOrderDishList, 
		// 瑞吉外卖相关的接口
		userLogin, 
		getCategoryList, 
		dishListByCategoryId, 
		commonDownload,
		// 加菜
		addShoppingCart,
		// 查询套餐列表的接口
		querySetmeaList,
		// 获取购物车集合
		getShoppingCartList,
		// 新的购物车添加逻辑接口
		newAddShoppingCartAdd,
		// 新的购物车减少接口
		newShoppingCartSub,
		// 清空购物车
		delShoppingCart,
		// 此接口为首页查询套餐详情展示的接口
		querySetmealDishById
	} from '../api/api.js'
import initWebScoket from '../../utils/webscoket'
import {mapState, mapMutations, mapActions} from 'vuex'
import { baseUrl } from '../../utils/env'
export default {
	data () {
		return {
			title: 'Hello',
			// 去结算部分
			openOrderCartList: false,
			// 存放左侧滚动区域菜品分类数组
			typeListData: [],
			dishListData: [],
			// 存放右侧对应菜品每个菜名称的数组
			dishListItems: [],
			dishDetailes: {},
			openDetailPop: false,
			openMoreNormPop: false,
			moreNormDataes: null,
			tableInfo:null,
			moreNormDishdata: {},
			moreNormdata: [],
			// 套餐中查询到的菜品名称
			dishMealData: [],
			openTablePeoPleNumber: 1,
			orderData: 0,
			// 选中左侧菜品的索引
			typeIndex: 0,
			// 控制菜品详情显示
			openTablePop: false,
			// 规格有关的数组
			flavorDataes: [],
			// 加入购物车数量
			orderDishNumber: 0,
			// 菜品金额
			orderDishPrice: 0,
			selectedFlavorInCart: false,
			params: {
				shopId: 'f3deb',
				storeId: '1282344676983062530',
				tableId: '1282346960773238786'
			 },
			 // 添加一个右侧number更新以后重新刷新接口的id --- 这个id来自左侧菜品分类的id
			 rightIdAndType: {}
		}
	},
	computed: {
		// 购物车信息列表
		orderListDataes: function () {
			return this.orderListData()
			// return this.orderListData().dishList
		},
		loaddingSt: function () {
			return this.lodding()
		},
		orderAndUserInfo: function () {
			let orderData = []
			Array.isArray(this.orderListDataes) && this.orderListDataes.filter(Boolean).forEach((n,i) => {
				let userData = {}
				userData.nickName = n.name ?? ''
				userData.avatarUrl = n.image ?? ''
				userData.dishList = [n]
				const num = orderData.findIndex(o => o.nickName == userData.nickName)
				if (num != -1) {
					orderData[num].dishList.push(n)
				} else {
					orderData.push(userData)
				}
			})
			return orderData
		},
		ht: function () {
			return uni.getMenuButtonBoundingClientRect().top + uni.getMenuButtonBoundingClientRect().height + 7
		}
	},
	components: { navBar, uniIcons },
	onLoad (options) {
		uni.onNetworkStatusChange(function(res) {
			if (res.isConnected == false) {
				uni.navigateTo({url: '/pages/nonet/index'})
			} 
		})
		if (options) {
			if (!options.status && !options.formOrder) {
				this.getData()
			}
		}
		// 有sessionId免授权
		// this.sessionId() && this.init()
	},
	onShow () {
		// 有sessionId免授权
		this.sessionId() && this.init()
	},
	methods: {
		...mapMutations(['setShopInfo', 'initdishListMut', 'setStoreInfo', 'setBaseUserInfo', 'setLodding', 'setSessionId']),
		...mapState(['shopInfo', 'orderListData', 'baseUserInfo', 'lodding', 'sessionId']),
		goAgent () {
			uni.navigateTo({ url: '/pages/ai/chat' })
		},
		loginSync () {
			return new Promise((resolve, reject) => {
				uni.login({
					// provider: 'weixin',
					success: (loginRes) => {
						if (loginRes.errMsg === 'login:ok') {
							resolve(loginRes.code)
						}
					}
				})
			})
		},
		getData () {
			let res = wx.getMenuButtonBoundingClientRect()
			let _this = this
			this.selectHeight = res.height
			uni.showModal({
				title: '温馨提示',
				content: '亲，授权微信登录后才能正点餐！',
				showCancel: false,
				async success(res) {
					if (res.confirm) {
						const jsCode = await _this.loginSync()
							// 授权
							uni.getUserProfile({
								desc: '登录',
								success: function (userInfo) {
									_this.setBaseUserInfo(userInfo.rawData)
									const params = { code: jsCode }
									userLogin(params).then(success => {
										if (success.code === 1) {
											_this.setSessionId(success.data.token)
											_this.init()
										}
									}).catch(err => {
									})
								},
								fail: function (err) {
									
								}
							})
						
						
						// uni.getUserProfile({
						// 	desc: '登录',
						// 	success(userInfo) {
						// 		_this.setBaseUserInfo(userInfo.rawData)
						// 		const params = {
						// 			phone: userInfo.signature,
						// 			avatar: userInfo.userInfo.avatarUrl,
						// 			name: userInfo.userInfo.nickName,
						// 			sex: userInfo.userInfo.gender
						// 		}
						// 		// 咱们自己的的接口
						// 		userLogin(params).then(success => {
						// 			if (success.code === 1) {
						// 				success.map && _this.setSessionId(success.map.sessionId)
						// 				_this.init()
						// 			}
						// 		})
						// 	},
						// 	fail: function (err) {
						// 	}
						// })
					}
				}
			})
		}, 
		
		async init () {
			// 获取菜品和套餐分类接口
			getCategoryList().then(res => {
				if (res && res.code === 1) {
					this.typeListData = [ ...res.data ]
					if (res.data.length > 0){
						this.getDishListDataes(res.data[this.typeIndex || 0])
					}
				}
			})
			// 调用一次购物车集合---初始化
			this.getTableOrderDishListes()
		},
		// 开桌操作 开桌后初始化websocket结束点餐信息
		// async openTableHandle () {
		// 	openTable({tableId: this.params.tableId, seatNumber: this.openTablePeoPleNumber}).then(res => {
		// 		this.openTablePop = false
		// 		// initWebScoket(this.params)
		// 		this.getTableOrderDishListes()
		// 		this.computOrderInfo()
		// 	}).catch(err => {
		// 	})
		// },
		// 获取菜品列表
		async getDishListDataes (params, index) {
      console.log('=-=-=-=-=-=-=getDishListDataes-=-params=-',params)
			this.rightIdAndType = {}
			this.rightIdAndType = {
				id: params.id,
				type: params.type
			}
			const param = {categoryId: params.id,type: params.type, page: 1, pageSize: 1000,status:1}
			if (params.type === 1) {
				await dishListByCategoryId(param).then(res => {
					if (res && res.code === 1) {
						// 添加一个字段去实时更新加入购物车number数量 ----- newCardNumber
						this.dishListData = res.data && res.data.map((obj) => ({ ...obj, type: 1, newCardNumber: 0 }))
					}
				}).catch(err => {
				})
			} else {
				await querySetmeaList(param).then(success => {
					if (success && success.code === 1) {
						// dishListItems被转换数组---原始this.dishListData
						this.dishListData = success.data && success.data.map((obj) => ({ ...obj, type: 2, newCardNumber: 0 }))
					}
				}).catch(err => {
				})
			}
			if (index !== undefined) this.typeIndex = index
			this.setOrderNum()
		},
		// 重新拼装image
		getNewImage (image) {
			if (!image) return '../../static/takeout-guys-bot.png'
			if (/^https?:\/\//.test(image)) return image
			return `${baseUrl}${image}`
		},
		// 获取购物车订单列表
		async getTableOrderDishListes () {
			// 调用获取购物车集合接口
			await getShoppingCartList({}).then(res => {
				if (res.code === 1) {
					this.initdishListMut(Array.isArray(res.data) ? res.data.filter(Boolean) : [])
					this.computOrderInfo()
					if (Array.isArray(this.dishListData) && this.dishListData.length) this.setOrderNum()
					if (this.openMoreNormPop) this.syncSelectedFlavorQuantity()
				}
			}).catch(err => {
			})
		},
		// 去订单页面
		goOrder () {
			uni.navigateTo({url: '/pages/order/index'})
		},
		// 加菜 - 添加菜品
		async addDishAction (item, form) {
      console.log('this.flavorDataes',this.flavorDataes)
      console.log('-=-=-=addDishAction-=-=-')
			// 实时更新obj.newCardNumber新添加的字段----加入购物车数量number
			// item.newCardNumber++
			if(this.orderListDataes && !this.orderListDataes.some(n => n.id == item.dishId) && this.flavorDataes.length > 0) {
				item.flavorRemark = JSON.stringify(this.flavorDataes)
			}
			// const wxUserInfo = this.baseUserInfo()
			// 有sort字段是菜品
			const dishFlavor = form === '购物车'
				? (item.dishFlavor || '')
				: (this.openMoreNormPop ? this.flavorDataes.filter(Boolean).join(',') : '')
			let params = {
				id: form === '购物车' ? item.id : null,
				amount: item.price,
				dishFlavor,
				number: 1 || item.dishNumber,
				name: item.name,
				image: item.image
			}
			if (item.type === 1 || (form === '购物车' && item.dishId != null)) {
				params = {
					...params,
					dishId: form === '购物车' ? item.dishId : item.id
				}
			} else {
				params = {
					...params,
					setmealId: form === '购物车' ? item.setmealId : item.id
				}
			}
			newAddShoppingCartAdd(params).then(res => {
				if (res.code === 1) {
					// this.computOrderInfo()
					// this.setOrderNum()
					// 菜品详情弹框隐藏---暂时这么处理，去更新购物车状态
					this.openDetailPop = false
					this.openMoreNormPop = false
					this.flavorDataes.splice(0, this.flavorDataes.length)
					// 调用一次购物车集合---初始化
					this.getTableOrderDishListes()
					// 重新调取刷新右侧具体菜品列表
					this.getDishListDataes(this.rightIdAndType)
				}
			}).catch(err => {
			})
		},
		// 减菜 - 添加菜品
		async redDishAction (item, form) {
			// 实时更新obj.newCardNumber新添加的字段----加入购物车数量number
			// if (item.newCardNumber === 0) {
			// 	item.newCardNumber = 0
			// } else {
			// 	item.newCardNumber--
			// }
			let params = {
				id: form === '购物车' ? item.id : null,
				dishFlavor: form === '购物车'
					? (item.dishFlavor || '')
					: (this.openMoreNormPop ? this.flavorDataes.filter(Boolean).join(',') : '')
			}
			if (item.type === 1 || (form === '购物车' && item.dishId != null)) {
				params = {
					...params,
					dishId: form === '购物车' ? item.dishId : item.id
				}
			} else {
				params = {
					...params,
					setmealId: form === '购物车' ? item.setmealId : item.id
				}
			}
			await newShoppingCartSub(params).then(res => {
				if (res.code === 1) {
					// this.computOrderInfo()
					// this.setOrderNum()
					// 调用一次购物车集合---初始化
					this.getTableOrderDishListes()
					// 重新调取刷新右侧具体菜品列表
					this.getDishListDataes(this.rightIdAndType)
				}
			}).catch(err => {
			})
		},
		async addDishFromBrowse (item) {
			const isSetmeal = item.type === 2
			const rows = Array.isArray(this.orderListDataes)
				? this.orderListDataes.filter(row => row && (isSetmeal ? String(row.setmealId) === String(item.id) : String(row.dishId) === String(item.id)) && !(row.dishFlavor || ''))
				: []
			if (rows.length > 0) {
				await this.addDishAction(rows[0], '购物车')
			} else {
				await this.addDishAction(item, '普通')
			}
		},
		async redDishFromBrowse (item) {
			const isSetmeal = item.type === 2
			const rows = Array.isArray(this.orderListDataes)
				? this.orderListDataes.filter(row => row && (isSetmeal ? String(row.setmealId) === String(item.id) : String(row.dishId) === String(item.id)))
				: []
			if (rows.length === 1 || (!(item.flavors && item.flavors.length) && rows.length > 0)) {
				await this.redDishAction(rows[0], '购物车')
			} else if (rows.length > 1) {
				this.openOrderCartList = true
				uni.showToast({ title: '请在购物车中选择要减少的口味', icon: 'none' })
			}
		},
		// 清空购物车
		clearCardOrder () {
			delShoppingCart().then(res => {
				// this.computOrderInfo()
				// this.setOrderNum()
				this.openOrderCartList = false
				// 调用一次购物车集合---初始化
				this.getTableOrderDishListes()
				// 重新调取刷新右侧具体菜品列表
				this.getDishListDataes(this.rightIdAndType)
			}).catch(err => {
			})
		},
		// 打开菜品牌详情
		openDetailHandle (item) {
			this.dishDetailes = item
			if (item.type === 2) {
				querySetmealDishById({ id: item.id }).then(res => {
					console.log(res)
					if (res.code === 1) {
						this.openDetailPop = true
						this.dishMealData = res.data
					}
				}).catch(err => {
				})
				// 老接口
				// getDishDetail({setmealId:item.dishId}).then(res => {
				// 	this.openDetailPop = true
				// 	this.dishMealData= res.data
				// }).catch(err => {
				// })
			} else {
				this.openDetailPop = true
			}
		},
		// 多规格数据处理
		moreNormDataesHandle (item) {
      this.flavorDataes.splice(0)
			this.moreNormDishdata = item
			this.openMoreNormPop = true
			this.moreNormdata = item.flavors.map(obj => ({ ...obj, value: JSON.parse(obj.value) }))
			// 每个口味组默认不选择，用户可以按需选择。
			this.flavorDataes.splice(0, this.flavorDataes.length, ...this.moreNormdata.map(() => ''))
			this.syncSelectedFlavorQuantity()
      // this.moreNormdata = item.flavors === null ? [] : item.flavors
			// getMoreNorm({dishId: item.dishId}).then(res => {
			// 	this.openMoreNormPop = true
			// 	this.moreNormdata = res.data
			// }).catch(err => {
			// })
		},
		// 选规格 处理一行只能选择一种 
		checkMoreNormPop (groupIndex, item) {
			// 同组最多选择一项；再次点击当前项即可取消。
			const nextValue = this.flavorDataes[groupIndex] === item ? '' : item
			this.flavorDataes.splice(groupIndex, 1, nextValue)
			this.syncSelectedFlavorQuantity()
		},
		syncSelectedFlavorQuantity () {
			if (!this.moreNormDishdata || !this.moreNormDishdata.id) return
			const flavor = this.flavorDataes.filter(Boolean).join(',')
			const cartItem = Array.isArray(this.orderListDataes)
				? this.orderListDataes.find(item => item && String(item.dishId) === String(this.moreNormDishdata.id) && (item.dishFlavor || '') === flavor)
				: null
			this.selectedFlavorInCart = Boolean(cartItem)
			// 新规格尚未加入购物车时，选择器默认展示 1 份。
			this.moreNormDishdata.dishNumber = cartItem ? cartItem.number : 1
		},
		// 关闭选规格弹窗
		closeMoreNorm (moreNormDishdata) {
			this.flavorDataes.splice(0, this.flavorDataes.length)
			this.openMoreNormPop = false
		},
		// // 设置开桌人数
		// setOpenTableNumber (st) {
		// 	if (st == 'add') {
		// 		this.openTablePeoPleNumber+=1
		// 	} else {
		// 		this.openTablePeoPleNumber =  this.openTablePeoPleNumber > 1 ? this.openTablePeoPleNumber-1 : 1
		// 	}
		// },
		// 订单里和总订单价格计算
		computOrderInfo () {
			let oriData = this.orderListDataes
			this.orderDishNumber = this.orderDishPrice = 0
			oriData.map((n, i) => {
				this.orderDishNumber += n.number
				// this.orderDishPrice += n.number * n.price
				this.orderDishPrice += n.number * n.amount
			})
		},
		// 处理点餐数量 - 更新菜品已点餐数量
		setOrderNum () {
			let ODate = this.dishListData
			let CData = this.orderListDataes
			ODate && ODate.map((obj, index) => {
				obj.dishNumber = 0
			CData && CData.forEach((tg, ind) => {
					if ((obj.type === 2 && String(obj.id) === String(tg.setmealId)) || (obj.type !== 2 && String(obj.id) === String(tg.dishId))) {
						// 浏览页展示该菜品所有口味记录的总数量。
						obj.dishNumber += Number(tg.number || 0)
						// 对新添加的实时更新newCardNumber字段进行赋值
						// obj.newCardNumber = tg.number
					}
				})
			})
			
			// this.dishListData && this.dishListData.map((obj, index) => {
			// 	obj.dishNumber = 0
			// 	this.orderListDataes && this.orderListDataes.forEach((tg, ind) => {
			// 		if (obj.id === tg.dishId) {
			// 			obj.dishNumber = tg.number
			// 		}
			// 	})
			// })
			if (this.dishListItems.length == 0) {
				this.dishListItems = ODate
			} else {
				this.dishListItems.splice(0, this.dishListItems.length, ...ODate)
			}
      console.log('-=-=-=-setOrderNum-=-=',this.dishListItems)
		},
	}
}
