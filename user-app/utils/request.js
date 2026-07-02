import { baseUrl } from './env'

/**
 * Takeout Guys 小程序统一请求封装。
 * 后端统一返回 { code, msg, data }，用户 JWT 使用 authentication 请求头。
 */
export function request({ url = '', params = {}, method = 'GET', showError = true }) {
	const token = uni.getStorageSync('user_token') || ''
	return new Promise((resolve, reject) => {
		uni.request({
			url: baseUrl + url,
			data: params,
			method,
			header: {
				Accept: 'application/json',
				'Content-Type': 'application/json',
				authentication: token
			},
			timeout: 30000,
			success: res => {
				const body = res.data || {}
				if (res.statusCode === 401) {
					uni.removeStorageSync('user_token')
					showError && uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
					reject(body)
					return
				}
				if (Number(body.code) === 1) resolve(body)
				else {
					showError && uni.showToast({ title: body.msg || '请求失败', icon: 'none' })
					reject(body)
				}
			},
			fail: error => {
				showError && uni.showToast({ title: '无法连接服务器', icon: 'none' })
				reject(error)
			}
		})
	})
}
