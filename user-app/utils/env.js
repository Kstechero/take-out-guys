// 微信小程序不能请求 localhost；开发机当前局域网地址为 192.168.31.107。
// IP 变化后请运行 ipconfig 并同步修改；生产环境必须改为已备案的 HTTPS 域名。
const DEFAULT_API_BASE_URL = 'http://192.168.31.107:8080'

export function getApiBaseUrl() {
	const runtimeUrl = uni.getStorageSync('api_base_url')
	return String(runtimeUrl || DEFAULT_API_BASE_URL).replace(/\/$/, '')
}

// Backward-compatible export for pages that only display the configured address.
export const baseUrl = DEFAULT_API_BASE_URL

export const brand = {
	name: 'Takeout Guys AI',
	primary: '#ff4b12',
	dark: '#101a2a'
}
