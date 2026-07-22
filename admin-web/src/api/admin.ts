import request from '@/utils/request'

export const getBusinessData = () => request.get('/workspace/businessData')
export const getOrderOverview = () => request.get('/workspace/overviewOrders')
export const getDishOverview = () => request.get('/workspace/overviewDishes')
export const getOrderPage = (params: any) => request.get('/order/conditionSearch', { params })
export const getOrderStatistics = () => request.get('/order/statistics')
export const getDishPage = (params: any) => request.get('/dish/page', { params })
export const getEmployeePage = (params: any) => request.get('/employee/page', { params })
export const getCategoryPage = (params: any) => request.get('/category/page', { params })
export const getSetmealPage = (params: any) => request.get('/setmeal/page', { params })
export const getCategoryList = (type: number) => request.get('/category/list', { params: { type } })
export const getOrderDetail = (id: number) => request.get(`/order/details/${id}`)
export const getResourceDetail = (kind: 'dish'|'setmeal'|'employee', id: number) => request.get(`/${kind}/${id}`)
export const updateResource = (kind: 'dish'|'setmeal'|'employee'|'category', data: any) => request.put(`/${kind}`, data)
export const createCategory = (data: any) => request.post('/category', data)
export const createResource = (kind: 'dish'|'employee'|'category', data: any) => request.post(`/${kind}`, data)
export const toggleResource = (kind: 'dish'|'setmeal'|'employee'|'category', id: number, status: number) => request.post(`/${kind}/status/${status}`, null, { params: { id } })
export const deleteResource = (kind: 'dish'|'setmeal'|'category', id: number) => request.delete(`/${kind}`, { params: kind === 'category' ? { id } : { ids: id } })
export const confirmOrder = (id: number) => request.put('/order/confirm', { id })
export const rejectOrder = (id: number, rejectionReason: string) => request.put('/order/rejection', { id, rejectionReason })
export const cancelOrder = (id: number, cancelReason: string) => request.put('/order/cancel', { id, cancelReason })
export const deliverOrder = (id: number) => request.put(`/order/delivery/${id}`)
export const completeOrder = (id: number) => request.put(`/order/complete/${id}`)
export const getShopStatus = () => request.get('/shop/status')
export const setShopStatus = (status: number) => request.put(`/shop/${status}`)
export const getAiHealth = () => request.get('/ai/health')
export const getAdminAgentSessions = () => request.get('/ai/session/list')
export const getAdminAgentMessages = (sessionId: number) => request.get(`/ai/session/${sessionId}/messages`)
export const deleteAdminAgentSession = (sessionId: number) => request.delete(`/ai/session/${sessionId}`)

export interface AdminAgentChatPayload {
  sessionId: number | null
  message: string
  context?: Record<string, unknown>
}

export const streamAdminAgentChat = (data: AdminAgentChatPayload) =>
  fetch(`${import.meta.env.VITE_API_BASE}/ai/chat/stream`, {
    method: 'POST',
    headers: {
      token: localStorage.getItem('sky_admin_token') || '',
      Accept: 'text/event-stream',
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(data)
  })
export const resumeAdminAgentChat = (data: {
  sessionId: number
  confirmationToken: string
  decision: 'approve' | 'reject' | 'edit'
  editedArguments?: Record<string, unknown> | null
}) => request.post('/ai/chat/resume', data)
export const getTurnoverReport = (begin: string, end: string) => request.get('/report/turnoverStatistics', { params: { begin, end } })
export const getOrderReport = (begin: string, end: string) => request.get('/report/ordersStatistics', { params: { begin, end } })
export const getUserReport = (begin: string, end: string) => request.get('/report/userStatistics', { params: { begin, end } })
export const getTop10Report = (begin: string, end: string) => request.get('/report/top10', { params: { begin, end } })
export const getServiceSessionPage = (params: { page: number; pageSize: number; status?: number; keyword?: string }) =>
  request.get('/service/session/page', { params })
export const getServiceMessageList = (params: { sessionId: number; lastMessageId?: number }) =>
  request.get('/service/message/list', { params })
export const replyServiceMessage = (data: { sessionId: number; content: string; messageType?: string }) =>
  request.post('/service/message/reply', data)
export const endServiceSession = (sessionId: number) =>
  request.post('/service/session/end', { sessionId })

export interface ReviewPageParams {
  page: number
  pageSize: number
  keyword?: string
  status?: number
}

export const getReviewPage = (params: ReviewPageParams) =>
  request.get('/review/page', { params })

export const updateReviewStatus = (id: number, status: number) =>
  request.put(`/review/${id}/status/${status}`)

export const deleteReview = (id: number) =>
  request.delete(`/review/${id}`)

export interface CouponPayload {
  name: string
  type: number
  discountAmount: number
  minimumAmount: number
  totalCount: number
  perUserLimit: number
  validFrom: string
  validUntil: string
  status: number
  description?: string
}

export const getCouponPage = (params: { page: number; pageSize: number; name?: string; status?: number }) => request.get('/coupon/page', { params })
export const createCoupon = (data: CouponPayload) => request.post('/coupon', data)
export const updateCoupon = (id: number, data: CouponPayload) => request.put(`/coupon/${id}`, data)
export const deleteCoupon = (id: number) => request.delete(`/coupon/${id}`)

export interface SensitiveWordPayload {
  word: string
  level?: number
  replacement?: string
  status?: number
}

export const getSensitiveWordPage = (params: { page: number; pageSize: number; word?: string; status?: number }) =>
  request.get('/sensitive-word/page', { params })
export const createSensitiveWord = (data: SensitiveWordPayload) => request.post('/sensitive-word', data)
export const updateSensitiveWord = (id: number, data: SensitiveWordPayload) => request.put(`/sensitive-word/${id}`, data)
export const deleteSensitiveWords = (ids: number[]) => request.delete('/sensitive-word/batch', { params: { ids: ids.join(',') } })
