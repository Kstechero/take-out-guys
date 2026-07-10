import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({ baseURL: import.meta.env.VITE_API_BASE, timeout: 30000 })
request.interceptors.request.use(config => {
  const token = localStorage.getItem('sky_admin_token')
  if (token) config.headers.token = token
  return config
})
request.interceptors.response.use(
  response => {
    const body = response.data
    if (body?.code !== undefined && Number(body.code) !== 1) {
      ElMessage.error(body.msg || '请求失败')
      return Promise.reject(new Error(body.msg || '请求失败'))
    }
    return body
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('sky_admin_token')
      location.href = '/login'
    }
    ElMessage.error(error.response?.data?.msg || error.message || '网络异常')
    return Promise.reject(error)
  }
)
export default request
