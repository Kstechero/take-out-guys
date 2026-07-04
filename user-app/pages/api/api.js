import { request } from '../../utils/request.js'

// 用户与店铺
export const userLogin = params => request({ url: '/user/user/login', method: 'POST', params })
export const userLogout = () => request({ url: '/user/user/logout', method: 'POST' })
export const getShopStatus = () => request({ url: '/user/shop/status' })

// 菜品、分类、套餐
export const getCategoryList = params => request({ url: '/user/category/list', params })
export const dishListByCategoryId = params => request({ url: '/user/dish/list', params })
export const querySetmeaList = params => request({ url: '/user/setmeal/list', params })
export const querySetmealDishById = params => request({ url: `/user/setmeal/dish/${params.id}` })

// 购物车
export const newAddShoppingCartAdd = params => request({ url: '/user/shoppingCart/add', method: 'POST', params })
export const newShoppingCartSub = params => request({ url: '/user/shoppingCart/sub', method: 'POST', params })
export const getShoppingCartList = () => request({ url: '/user/shoppingCart/list' })
export const delShoppingCart = () => request({ url: '/user/shoppingCart/clean', method: 'DELETE' })

// 订单
export const submitOrderSubmit = params => request({ url: '/user/order/submit', method: 'POST', params })
export const payOrder = params => request({ url: '/user/order/payment', method: 'PUT', params })
export const queryOrderUserPage = params => request({ url: '/user/order/historyOrders', params })
export const queryOrderDetail = id => request({ url: `/user/order/orderDetail/${id}` })
export const cancelUserOrder = id => request({ url: `/user/order/cancel/${id}`, method: 'PUT' })
export const oneOrderAgain = params => request({ url: `/user/order/repetition/${params.id}`, method: 'POST' })
export const remindOrder = id => request({ url: `/user/order/reminder/${id}` })

// 客户评价（前端已接入，后端模块待实现）
export const submitReview = params => request({ url: '/user/review', method: 'POST', params, showError: false })
export const queryUserReviews = params => request({ url: '/user/review/page', params, showError: false })

// 地址
export const queryAddressBookList = () => request({ url: '/user/addressBook/list' })
export const putAddressBookDefault = params => request({ url: '/user/addressBook/default', method: 'PUT', params })
export const addAddressBook = params => request({ url: '/user/addressBook', method: 'POST', params })
export const editAddressBook = params => request({ url: '/user/addressBook', method: 'PUT', params })
export const delAddressBook = ids => request({ url: '/user/addressBook', method: 'DELETE', params: { ids } })
export const queryAddressBookById = params => request({ url: `/user/addressBook/${params.id}` })
export const getAddressBookDefault = () => request({ url: '/user/addressBook/default' })

// 优惠券
export const getCouponAvailable = params => request({ url: '/user/coupon/available', params, showError: false })
export const receiveCoupon = couponId => request({ url: `/user/coupon/receive/${couponId}`, method: 'POST', showError: false })
export const getMyCoupons = params => request({ url: '/user/coupon/my', params, showError: false })
export const getOrderAvailableCoupons = params => request({ url: '/user/coupon/order/available', params, showError: false })

// AI Agent（已对齐用户端 Apifox 契约；部分后端能力仍待联调）
export const aiChat = params => request({ url: '/user/ai/chat', method: 'POST', params, showError: false })
export const aiChatStream = params => request({ url: '/user/ai/chat/stream', params, showError: false })
export const aiRecommend = params => request({ url: '/user/ai/recommend', method: 'POST', params, showError: false })
export const aiWriteReview = params => request({ url: '/user/ai/review/write', method: 'POST', params, showError: false })
export const getAiSessions = () => request({ url: '/user/ai/session/list', showError: false })
export const deleteAiSession = sessionId => request({ url: `/user/ai/session/${sessionId}`, method: 'DELETE' })

// 旧页面兼容别名，逐步移除
export const getList = getCategoryList
export const getDishList = dishListByCategoryId
export const addShoppingCart = newAddShoppingCartAdd
export const editHoppingCart = newAddShoppingCartAdd
export const commonDownload = params => request({ url: '/user/common/download', params })
export const openTable = () => Promise.resolve({ code: 1 })
export const getTableState = () => Promise.resolve({ code: 1 })
export const getTableOrderDishList = getShoppingCartList
export const getMoreNorm = () => Promise.resolve({ code: 1, data: [] })
export const getDishDetail = querySetmealDishById
export const addDish = newAddShoppingCartAdd
export const delDish = newShoppingCartSub
export const clearOrder = delShoppingCart
