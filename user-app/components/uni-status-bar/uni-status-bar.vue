<template>
	<view :style="{ height: statusBarHeight }" class="uni-status-bar">
		<slot />
	</view>
</template>

<script>
	import { getStatusBarHeight } from '../../utils/system.js'
	export default {
		name: 'UniStatusBar',
		data() {
			return {
				statusBarHeight: getStatusBarHeight() + 'px'
			}
		}
	}
</script>

<style lang="scss" scoped>
	.uni-status-bar {
		width: 750rpx;
		height: 40rpx;
		// height: var(--status-bar-height);
	}
</style>
