import { createStore } from 'vuex'

const store = createStore({
	state: {
		storeInfo: {}, // 店铺请求的id信息
		shopInfo:'',  // 店铺详细信息
		orderListData:[] ,// 购物车列表信息
		baseUserInfo: '', // 存储获取的用户微信的信息（用户名、头像）
		lodding: false,
		sessionId: uni.getStorageSync('user_token') || '',
    addressBackUrl: '',
    dishTypeIndex: 0
	},
	mutations: {
		setStoreInfo(state, provider){
			state.storeInfo = provider;
		},
		setShopInfo(state, provider){
			state.shopInfo = provider;
		},
		initdishListMut(state, provider){
			state.orderListData = provider;
		},
		setBaseUserInfo(state, provider){
			state.baseUserInfo = JSON.parse(provider);
		},
		setLodding(state, provider){
			console.log(5656, provider)
			state.lodding = provider;
		},
		setSessionId(state, provider) {
			state.sessionId = provider
			if (provider) uni.setStorageSync('user_token', provider)
			else uni.removeStorageSync('user_token')
		},
		setAddressBackUrl(state, provider) {
			state.addressBackUrl = provider
		},
    setDishTypeIndex(state, provider) {
    	state.dishTypeIndex = provider
    }
	},
	actions: {
		
	}
})

export default store
