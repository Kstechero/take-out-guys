/**
 * 微信基础库 3.7.0+ 推荐使用拆分后的设备与窗口 API。
 * 避免旧版聚合系统信息 API 在 HarmonyOS 等平台产生废弃警告。
 */
export function getStatusBarHeight() {
	if (typeof uni.getWindowInfo !== 'function') return 0
	return Number(uni.getWindowInfo().statusBarHeight) || 0
}

export function getPlatform() {
	if (typeof uni.getDeviceInfo !== 'function') return ''
	return uni.getDeviceInfo().platform || ''
}
