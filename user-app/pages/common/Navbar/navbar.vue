<template>
	<view class="navBar">
		<image class="brand-logo" src="../../../static/takeout-guys-logo.png" mode="aspectFit"></image>
		<!-- 个人中心 -->
		<view @click="myCenterFun" class="person-box" :style="{top:ht + 'px'}">
			<image class="test_image" src="../../../static/center.png"></image>
			<span class="person-title">个人中心</span>
		</view>
		<view class="index_bg"></view>
		<!-- <view class="leftNav" >
			<image class="logo" src="/static/logo.png"></image>
		</view> -->
	</view>
</template>

<script>
export default {
	computed: {
		// ht: function () {
		// 	let res = uni.getMenuButtonBoundingClientRect()
		// 	let num = 24
		// 	if(/iPhone.*/.test(uni.getDeviceInfo().model)){
		// 		 num = res.top * 1.6
		// 	} else {
		// 		num = res.top * 2
		// 	}
		// 	return num
		// }
		ht: function () {
			let res = uni.getMenuButtonBoundingClientRect() 
			return res.top +5
		}
	},
	methods: {
		myCenterFun () {
			uni.navigateTo({
				url: '/pages/my/my'
			})
		}
	}
}
</script>

<style src="./navbar.scss" lang="scss"></style>
