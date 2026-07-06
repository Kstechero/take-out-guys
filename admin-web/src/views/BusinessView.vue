<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Search, Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  cancelOrder,
  completeOrder,
  confirmOrder,
  createResource,
  createSensitiveWord,
  deleteResource,
  deleteSensitiveWords,
  deliverOrder,
  getCategoryList,
  getCategoryPage,
  getDishPage,
  getEmployeePage,
  getOrderDetail,
  getOrderPage,
  getOrderStatistics,
  getResourceDetail,
  getSensitiveWordPage,
  getSetmealPage,
  rejectOrder,
  toggleResource,
  updateResource,
  updateSensitiveWord
} from '@/api/admin'

const route = useRoute()
const keyword = ref('')
const loading = ref(false)
const saving = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const dialog = ref(false)
const detailDialog = ref(false)
const editing = ref(false)
const categories = ref<any[]>([])
const orderDetail = ref<any>({})
const orderStatus = ref<number | undefined>(undefined)
const orderStats = reactive({ toBeConfirmed: 0, confirmed: 0, deliveryInProgress: 0 })
const flavors = ref<Array<{ name: string; options: string[] }>>([])
const flavorPresets: Record<string, string[]> = {
  甜味: ['无糖', '少糖', '半糖', '多糖', '全糖'],
  温度: ['热饮', '常温', '去冰', '少冰', '多冰'],
  忌口: ['不要葱', '不要蒜', '不要香菜', '不要辣'],
  辣度: ['不辣', '微辣', '中辣', '重辣']
}
const uploadHeaders = computed(() => ({ token: localStorage.getItem('sky_admin_token') || '' }))

const emptyForm = () => ({
  id: null,
  name: '',
  username: '',
  phone: '',
  sex: '1',
  idNumber: '',
  type: 1,
  sort: 1,
  categoryId: null,
  price: 0,
  image: '',
  description: '',
  status: 1,
  word: '',
  level: 1,
  replacement: '***'
})
const form = reactive<any>(emptyForm())

const configs: Record<string, any> = {
  order: { eyebrow: 'ORDER FLOW', desc: '查看订单详情并按业务状态处理订单', action: '刷新订单', columns: ['订单号', '订单内容', '用户', '金额', '状态', '下单时间', '操作'] },
  dish: { eyebrow: 'MENU STUDIO', desc: '维护菜品、所属分类、图片和口味选项', action: '新增菜品', columns: ['菜品', '分类', '价格', '状态', '更新时间', '操作'] },
  category: { eyebrow: 'CATEGORY SYSTEM', desc: '维护菜品分类和套餐分类', action: '新增分类', columns: ['分类名称', '类型', '排序', '状态', '更新时间', '操作'] },
  setmeal: { eyebrow: 'SETMEAL STUDIO', desc: '维护套餐组合与销售状态', action: '新增套餐', columns: ['套餐名称', '分类', '价格', '状态', '更新时间', '操作'] },
  employee: { eyebrow: 'TEAM MANAGEMENT', desc: '新增员工并管理账号资料和状态', action: '新增员工', columns: ['员工姓名', '账号', '手机号', '性别', '状态', '创建时间', '操作'] },
  review: { eyebrow: 'VOICE OF CUSTOMER', desc: '评价管理后端接口暂未开放，当前不再展示写死假数据', action: '等待接口', columns: ['评价内容', '用户', '关联菜品', '评分', '状态', '时间'] },
  sensitive: { eyebrow: 'CONTENT SAFETY', desc: '动态维护内容安全词库，已接通后端敏感词接口', action: '新增敏感词', columns: ['敏感词', '等级', '替换词', '命中次数', '状态', '更新时间', '操作'] }
}

const kind = computed(() => (route.meta.kind as string) || 'order')
const config = computed(() => configs[kind.value])
const supported = computed(() => ['order', 'dish', 'category', 'setmeal', 'employee', 'sensitive'].includes(kind.value))
const title = computed(() => `${editing.value ? '修改' : '新增'}${({ dish: '菜品', category: '分类', employee: '员工', sensitive: '敏感词' } as any)[kind.value] || '信息'}`)

const statusName = (status: number) => ['', '待付款', '待接单', '已接单', '派送中', '已完成', '已取消', '已退款'][status] || String(status)
function cell(row: any, index: number) {
  const maps: any = {
    order: [row.number, row.orderDishes || '-', row.userName || row.phone || '-', `¥ ${row.amount ?? 0}`, statusName(row.status), row.orderTime],
    dish: [row.name, row.categoryName || '-', `¥ ${row.price ?? 0}`, row.status === 1 ? '启售' : '停售', row.updateTime],
    category: [row.name, row.type === 1 ? '菜品分类' : '套餐分类', row.sort, row.status === 1 ? '启用' : '禁用', row.updateTime],
    setmeal: [row.name, row.categoryName || '-', `¥ ${row.price ?? 0}`, row.status === 1 ? '启售' : '停售', row.updateTime],
    employee: [row.name, row.username, row.phone, row.sex === '1' ? '男' : '女', row.status === 1 ? '启用' : '禁用', row.createTime],
    sensitive: [row.word, `L${row.level ?? 1}`, row.replacement || '***', row.hitCount ?? 0, row.status === 1 ? '启用' : '禁用', row.updateTime]
  }
  return maps[kind.value]?.[index] ?? '-'
}

async function refresh() {
  loading.value = true
  try {
    const params: any = { page: page.value, pageSize: 10, name: keyword.value || undefined }
    let res: any
    if (kind.value === 'order') {
      delete params.name
      params.number = keyword.value || undefined
      params.status = orderStatus.value
      res = await getOrderPage(params)
      const stats: any = await getOrderStatistics()
      Object.assign(orderStats, stats.data || {})
    } else if (kind.value === 'dish') {
      res = await getDishPage(params)
    } else if (kind.value === 'employee') {
      res = await getEmployeePage(params)
    } else if (kind.value === 'category') {
      res = await getCategoryPage(params)
    } else if (kind.value === 'setmeal') {
      res = await getSetmealPage(params)
    } else if (kind.value === 'sensitive') {
      res = await getSensitiveWordPage({ page: page.value, pageSize: 10, word: keyword.value || undefined })
    } else {
      rows.value = []
      total.value = 0
      return
    }
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

async function loadDishCategories() {
  const res: any = await getCategoryList(1)
  categories.value = res.data || []
}

function resetForm() {
  Object.assign(form, emptyForm())
  flavors.value = []
}

async function primaryAction() {
  if (kind.value === 'order') return refresh()
  if (kind.value === 'review') return ElMessage.info('评价管理后端接口暂未提供，当前页已移除假数据展示')
  if (!['dish', 'category', 'employee', 'sensitive'].includes(kind.value)) return
  resetForm()
  editing.value = false
  if (kind.value === 'dish') await loadDishCategories()
  dialog.value = true
}

async function edit(row: any) {
  resetForm()
  editing.value = true
  let data = row
  if (['dish', 'employee'].includes(kind.value)) data = (await getResourceDetail(kind.value as any, row.id) as any).data
  Object.assign(form, data)
  if (kind.value === 'dish') {
    await loadDishCategories()
    flavors.value = (data.flavors || []).map((item: any) => {
      let options: string[] = []
      try {
        options = JSON.parse(item.value || '[]')
      } catch {
        options = []
      }
      return { name: item.name || '', options }
    })
  }
  dialog.value = true
}

function validate() {
  if (kind.value === 'sensitive') {
    if (!form.word?.trim()) throw new Error('敏感词不能为空')
    return
  }
  if (!form.name?.trim()) throw new Error('名称不能为空')
  if (kind.value === 'category' && !form.sort) throw new Error('请输入排序值')
  if (kind.value === 'dish') {
    if (!form.categoryId) throw new Error('请选择菜品分类')
    if (!form.price || Number(form.price) <= 0) throw new Error('菜品价格必须大于 0')
    if (!form.image?.trim()) throw new Error('请上传图片或填写图片地址')
    if (flavors.value.some(f => !f.name.trim() || !f.options.length)) throw new Error('每组口味必须填写名称和至少一个选项')
  }
  if (kind.value === 'employee') {
    if (!form.username?.trim()) throw new Error('账号不能为空')
    if (!/^1\d{10}$/.test(form.phone || '')) throw new Error('请输入正确的 11 位手机号')
    if (!/^\d{17}[\dXx]$/.test(form.idNumber || '')) throw new Error('请输入正确的身份证号')
  }
}

async function save() {
  try {
    validate()
  } catch (e: any) {
    return ElMessage.warning(e.message)
  }
  saving.value = true
  try {
    if (kind.value === 'sensitive') {
      const payload = {
        word: form.word.trim(),
        level: Number(form.level || 1),
        replacement: (form.replacement || '***').trim(),
        status: Number(form.status ?? 1)
      }
      if (editing.value) await updateSensitiveWord(Number(form.id), payload)
      else await createSensitiveWord(payload)
    } else {
      const payload: any = { ...form }
      if (kind.value === 'dish') payload.flavors = flavors.value.map(f => ({ name: f.name.trim(), value: JSON.stringify(f.options.filter(Boolean)) }))
      if (editing.value) await updateResource(kind.value as any, payload)
      else await createResource(kind.value as any, payload)
    }
    ElMessage.success(editing.value ? '修改成功' : '新增成功')
    dialog.value = false
    await refresh()
  } finally {
    saving.value = false
  }
}

function addFlavor() {
  flavors.value.push({ name: '', options: [] })
}
function availableFlavorNames(index: number) {
  const selected = new Set(flavors.value.filter((_, i) => i !== index).map(item => item.name))
  return Object.keys(flavorPresets).filter(name => !selected.has(name))
}
function presetOptions(name: string) {
  return flavorPresets[name] || []
}
function flavorNameChanged(flavor: { name: string; options: string[] }) {
  if (flavorPresets[flavor.name]) flavor.options = [...flavorPresets[flavor.name]]
  else flavor.options = []
}
function uploadSuccess(response: any) {
  if (Number(response.code) === 1) form.image = response.data
  else ElMessage.error(response.msg || '上传失败')
}
async function toggle(row: any) {
  if (kind.value === 'sensitive') {
    await updateSensitiveWord(row.id, { word: row.word, level: row.level, replacement: row.replacement, status: row.status === 1 ? 0 : 1 })
  } else {
    await toggleResource(kind.value as any, row.id, row.status === 1 ? 0 : 1)
  }
  ElMessage.success('状态已更新')
  refresh()
}
async function remove(row: any) {
  await ElMessageBox.confirm(`确定删除“${row.name || row.word}”吗？`, '删除确认', { type: 'warning' })
  if (kind.value === 'sensitive') await deleteSensitiveWords([row.id])
  else await deleteResource(kind.value as any, row.id)
  ElMessage.success('删除成功')
  refresh()
}

async function showOrder(row: any) {
  const res: any = await getOrderDetail(row.id)
  orderDetail.value = res.data || {}
  detailDialog.value = true
}
async function orderAction(row: any, action: string) {
  const labels: Record<string, string> = { confirm: '确认接单', delivery: '开始派送', complete: '确认完成' }
  if (labels[action]) await ElMessageBox.confirm(`确定要${labels[action]}吗？`, '订单操作', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '返回' })
  if (action === 'confirm') await confirmOrder(row.id)
  if (action === 'delivery') await deliverOrder(row.id)
  if (action === 'complete') await completeOrder(row.id)
  if (action === 'reject' || action === 'cancel') {
    const { value } = await ElMessageBox.prompt('请输入操作原因', '操作确认', { inputValidator: value => !!value?.trim() || '原因不能为空' })
    action === 'reject' ? await rejectOrder(row.id, value) : await cancelOrder(row.id, value)
  }
  ElMessage.success('订单状态已更新')
  detailDialog.value = false
  await refresh()
}

function changeOrderStatus(status?: number) {
  orderStatus.value = status
  page.value = 1
  refresh()
}

watch(kind, () => {
  keyword.value = ''
  page.value = 1
  refresh()
})
onMounted(refresh)
</script>

<template>
  <div class="business-page">
    <section class="page-intro">
      <div><p class="eyebrow">{{ config.eyebrow }}</p><h2>{{ route.meta.title }}</h2><p>{{ config.desc }}</p></div>
      <el-button type="primary" :icon="kind === 'order' ? Refresh : Plus" @click="primaryAction">{{ config.action }}</el-button>
    </section>
    <section class="card table-card">
      <div v-if="kind === 'order'" class="order-status-bar">
        <button :class="{ active: orderStatus === undefined }" @click="changeOrderStatus(undefined)"><b>全部订单</b><span>{{ total }}</span></button>
        <button :class="{ active: orderStatus === 2 }" @click="changeOrderStatus(2)"><b>待接单</b><span>{{ orderStats.toBeConfirmed || 0 }}</span></button>
        <button :class="{ active: orderStatus === 3 }" @click="changeOrderStatus(3)"><b>待派送</b><span>{{ orderStats.confirmed || 0 }}</span></button>
        <button :class="{ active: orderStatus === 4 }" @click="changeOrderStatus(4)"><b>派送中</b><span>{{ orderStats.deliveryInProgress || 0 }}</span></button>
        <button :class="{ active: orderStatus === 5 }" @click="changeOrderStatus(5)"><b>已完成</b></button>
        <button :class="{ active: orderStatus === 6 }" @click="changeOrderStatus(6)"><b>已取消</b></button>
      </div>
      <div class="table-tools"><el-input v-model="keyword" :prefix-icon="Search" :placeholder="kind === 'order' ? '输入订单号搜索' : kind === 'sensitive' ? '输入敏感词搜索' : '输入关键词搜索'" clearable @keyup.enter="refresh"/><el-button @click="refresh">查询</el-button></div>
      <el-alert v-if="kind === 'review'" title="评价管理后端接口暂未实现，当前页已移除写死假数据" type="warning" :closable="false" style="margin-bottom:14px"/>
      <el-alert v-else-if="!supported" title="该模块后端尚未实现" type="warning" :closable="false" style="margin-bottom:14px"/>
      <el-table v-loading="loading" :data="rows" height="430" :empty-text="supported ? '暂无数据' : '等待后端接口'">
        <el-table-column v-for="(c, i) in config.columns" :key="c" :label="c" :min-width="c === '操作' ? 210 : 120" :fixed="c === '操作' ? 'right' : false">
          <template #default="{ row }">
            <div v-if="c === '操作'" class="row-actions">
              <template v-if="kind === 'order'">
                <el-button link @click="showOrder(row)">详情</el-button>
                <el-button v-if="row.status === 2" link type="primary" @click="orderAction(row, 'confirm')">接单</el-button>
                <el-button v-if="row.status === 2" link type="danger" @click="orderAction(row, 'reject')">拒单</el-button>
                <el-button v-if="row.status === 3" link type="primary" @click="orderAction(row, 'delivery')">派送</el-button>
                <el-button v-if="row.status === 4" link type="success" @click="orderAction(row, 'complete')">完成</el-button>
                <el-button v-if="row.status < 5" link type="danger" @click="orderAction(row, 'cancel')">取消</el-button>
              </template>
              <template v-else-if="kind === 'review'">
                <el-button link disabled>等待接口</el-button>
              </template>
              <template v-else>
                <el-button link type="primary" @click="edit(row)">修改</el-button>
                <el-button link @click="toggle(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
                <el-button v-if="kind !== 'employee'" link type="danger" @click="remove(row)">删除</el-button>
              </template>
            </div>
            <span v-else>{{ cell(row, i) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-foot"><span>共 {{ total }} 条数据</span><el-pagination v-model:current-page="page" layout="prev, pager, next" :total="total" :page-size="10" @current-change="refresh"/></div>
    </section>

    <el-dialog v-model="dialog" :title="title" width="620px" destroy-on-close>
      <el-form label-position="top">
        <template v-if="kind === 'sensitive'">
          <el-form-item label="敏感词"><el-input v-model="form.word" maxlength="100"/></el-form-item>
          <div class="form-grid">
            <el-form-item label="等级"><el-input-number v-model="form.level" :min="1" :max="9"/></el-form-item>
            <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio :value="1">启用</el-radio><el-radio :value="0">停用</el-radio></el-radio-group></el-form-item>
          </div>
          <el-form-item label="替换词"><el-input v-model="form.replacement" maxlength="100" placeholder="默认 ***"/></el-form-item>
        </template>
        <template v-else>
          <el-form-item label="名称"><el-input v-model="form.name" maxlength="32"/></el-form-item>
          <template v-if="kind === 'category'">
            <el-form-item label="分类类型"><el-radio-group v-model="form.type"><el-radio :value="1">菜品分类</el-radio><el-radio :value="2">套餐分类</el-radio></el-radio-group></el-form-item>
            <el-form-item label="排序"><el-input-number v-model="form.sort" :min="1" :max="999"/></el-form-item>
          </template>
          <template v-if="kind === 'employee'">
            <el-form-item label="登录账号"><el-input v-model="form.username" maxlength="32" :disabled="editing"/></el-form-item>
            <el-form-item label="手机号"><el-input v-model="form.phone" maxlength="11"/></el-form-item>
            <el-form-item label="性别"><el-radio-group v-model="form.sex"><el-radio value="1">男</el-radio><el-radio value="2">女</el-radio></el-radio-group></el-form-item>
            <el-form-item label="身份证号"><el-input v-model="form.idNumber" maxlength="18"/></el-form-item>
            <el-alert v-if="!editing" title="新员工默认密码为 123456" type="info" :closable="false"/>
          </template>
          <template v-if="kind === 'dish'">
            <div class="form-grid"><el-form-item label="菜品分类"><el-select v-model="form.categoryId" placeholder="请选择"><el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id"/></el-select></el-form-item><el-form-item label="价格"><el-input-number v-model="form.price" :min="0.01" :precision="2"/></el-form-item></div>
            <el-form-item label="菜品图片"><div class="image-input"><el-input v-model="form.image" placeholder="图片 URL"/><el-upload action="/api/common/upload" name="file" :headers="uploadHeaders" :show-file-list="false" :on-success="uploadSuccess"><el-button>上传图片</el-button></el-upload></div><img v-if="form.image" :src="form.image" class="dish-preview"/></el-form-item>
            <el-form-item label="描述"><el-input v-model="form.description" type="textarea" maxlength="255" show-word-limit/></el-form-item>
            <div class="flavor-head"><b>口味配置</b><el-button link type="primary" @click="addFlavor">+ 添加口味</el-button></div>
            <div v-for="(flavor, index) in flavors" :key="index" class="flavor-row">
              <el-select v-model="flavor.name" filterable allow-create default-first-option placeholder="选择或新增口味" @change="flavorNameChanged(flavor)"><el-option v-for="name in availableFlavorNames(index)" :key="name" :label="name" :value="name"/></el-select>
              <el-select v-model="flavor.options" multiple filterable allow-create default-first-option placeholder="选择或新增口味选项"><el-option v-for="option in presetOptions(flavor.name)" :key="option" :label="option" :value="option"/></el-select>
              <el-button link type="danger" @click="flavors.splice(index, 1)">删除</el-button>
            </div>
          </template>
        </template>
      </el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="detailDialog" title="订单详情" width="680px">
      <el-descriptions :column="2" border><el-descriptions-item label="订单号">{{ orderDetail.number }}</el-descriptions-item><el-descriptions-item label="状态">{{ statusName(orderDetail.status) }}</el-descriptions-item><el-descriptions-item label="收货人">{{ orderDetail.consignee }}</el-descriptions-item><el-descriptions-item label="电话">{{ orderDetail.phone }}</el-descriptions-item><el-descriptions-item label="地址" :span="2">{{ orderDetail.address }}</el-descriptions-item><el-descriptions-item label="菜品" :span="2">{{ orderDetail.orderDishes }}</el-descriptions-item><el-descriptions-item label="金额">¥ {{ orderDetail.amount }}</el-descriptions-item><el-descriptions-item label="下单时间">{{ orderDetail.orderTime }}</el-descriptions-item><el-descriptions-item label="备注" :span="2">{{ orderDetail.remark || '无' }}</el-descriptions-item></el-descriptions>
      <template #footer><el-button @click="detailDialog=false">关闭</el-button><el-button v-if="orderDetail.status===2" type="danger" plain @click="orderAction(orderDetail,'reject')">拒单</el-button><el-button v-if="orderDetail.status<5" type="danger" plain @click="orderAction(orderDetail,'cancel')">取消订单</el-button><el-button v-if="orderDetail.status===2" type="primary" @click="orderAction(orderDetail,'confirm')">确认接单</el-button><el-button v-if="orderDetail.status===3" type="primary" @click="orderAction(orderDetail,'delivery')">开始派送</el-button><el-button v-if="orderDetail.status===4" type="success" @click="orderAction(orderDetail,'complete')">确认完成</el-button></template>
    </el-dialog>
  </div>
</template>
