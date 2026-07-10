<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { createCoupon, deleteCoupon, getCouponPage, updateCoupon, type CouponPayload } from '@/api/admin'

interface CouponRow extends CouponPayload { id: number; remainingCount?: number; receivedCount?: number }
const loading = ref(false), saving = ref(false), dialogVisible = ref(false)
const editingId = ref<number | null>(null), rows = ref<CouponRow[]>([])
const total = ref(0), page = ref(1), pageSize = ref(10), name = ref('')
const status = ref<number | undefined>(), formRef = ref<FormInstance>()
const emptyForm = () => ({ name: '', type: 1, discountAmount: 1, minimumAmount: 0, totalCount: 100, perUserLimit: 1, validRange: [] as string[], status: 1, description: '' })
const form = reactive(emptyForm())
const rules: FormRules = {
  name: [{ required: true, message: '请输入优惠券名称', trigger: 'blur' }],
  discountAmount: [{ required: true, message: '请输入优惠金额', trigger: 'change' }],
  minimumAmount: [{ required: true, message: '请输入使用门槛', trigger: 'change' }],
  totalCount: [{ required: true, message: '请输入发行数量', trigger: 'change' }],
  perUserLimit: [{ required: true, message: '请输入每人限领数量', trigger: 'change' }],
  validRange: [{ type: 'array', required: true, len: 2, message: '请选择有效期', trigger: 'change' }]
}
const typeLabel = (type: number) => ({ 1: '满减券', 2: '折扣券', 3: '新人券' }[type] || '其他')
const formatMoney = (value: number) => `¥${Number(value || 0).toFixed(2)}`
const formatDate = (value?: string) => value ? value.replace('T', ' ').slice(0, 16) : '-'
const remaining = (row: CouponRow) => row.remainingCount ?? Math.max(0, Number(row.totalCount || 0) - Number(row.receivedCount || 0))

async function load() {
  loading.value = true
  try {
    const res: any = await getCouponPage({ page: page.value, pageSize: pageSize.value, name: name.value.trim() || undefined, status: status.value })
    rows.value = res.data?.records || []; total.value = Number(res.data?.total || 0)
  } finally { loading.value = false }
}
function search() { page.value = 1; load() }
function resetSearch() { name.value = ''; status.value = undefined; search() }
function openCreate() { editingId.value = null; Object.assign(form, emptyForm()); dialogVisible.value = true }
function openEdit(row: CouponRow) {
  editingId.value = row.id
  Object.assign(form, { name: row.name, type: row.type ?? 1, discountAmount: row.discountAmount, minimumAmount: row.minimumAmount, totalCount: row.totalCount, perUserLimit: row.perUserLimit, status: row.status ?? 1, validRange: [row.validFrom, row.validUntil], description: row.description || '' })
  dialogVisible.value = true
}
async function save() {
  if (!formRef.value || !await formRef.value.validate().catch(() => false)) return
  if (Number(form.discountAmount) <= 0) return ElMessage.warning('优惠金额必须大于 0')
  if (Number(form.minimumAmount) < 0) return ElMessage.warning('使用门槛不能小于 0')
  if (Number(form.discountAmount) > Number(form.minimumAmount) && Number(form.minimumAmount) > 0) return ElMessage.warning('优惠金额不能高于使用门槛')
  if (new Date(form.validRange[0]).getTime() >= new Date(form.validRange[1]).getTime()) return ElMessage.warning('结束时间必须晚于开始时间')
  const payload: CouponPayload = { name: form.name.trim(), type: Number(form.type), discountAmount: Number(form.discountAmount), minimumAmount: Number(form.minimumAmount), totalCount: Number(form.totalCount), perUserLimit: Number(form.perUserLimit), validFrom: form.validRange[0], validUntil: form.validRange[1], status: Number(form.status), description: form.description.trim() }
  saving.value = true
  try {
    editingId.value ? await updateCoupon(editingId.value, payload) : await createCoupon(payload)
    ElMessage.success(editingId.value ? '优惠券修改成功' : '优惠券创建成功'); dialogVisible.value = false; await load()
  } finally { saving.value = false }
}
async function remove(row: CouponRow) {
  await ElMessageBox.confirm(`确定删除“${row.name}”吗？删除后无法恢复。`, '删除优惠券', { type: 'warning' })
  await deleteCoupon(row.id); ElMessage.success('删除成功')
  if (rows.value.length === 1 && page.value > 1) page.value--
  await load()
}
onMounted(load)
</script>

<template>
  <div class="coupon-page">
    <section class="page-intro">
      <div><p class="eyebrow">GROWTH TOOLKIT</p><h2>优惠券管理</h2><p>创建营销活动，统一管理发放数量、领取规则和有效期</p></div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建优惠券</el-button>
    </section>
    <section class="card table-card">
      <div class="coupon-tools">
        <el-input v-model="name" :prefix-icon="Search" placeholder="搜索优惠券名称" clearable @keyup.enter="search"/>
        <el-select v-model="status" placeholder="全部状态" clearable @change="search"><el-option label="启用" :value="1"/><el-option label="停用" :value="0"/></el-select>
        <el-button type="primary" @click="search">查询</el-button><el-button @click="resetSearch">重置</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" height="500" empty-text="暂无优惠券">
        <el-table-column prop="name" label="优惠券名称" min-width="160" show-overflow-tooltip/>
        <el-table-column label="类型" width="95"><template #default="{ row }"><el-tag effect="plain">{{ typeLabel(row.type) }}</el-tag></template></el-table-column>
        <el-table-column label="优惠/门槛" min-width="145"><template #default="{ row }"><b class="discount">减 {{ formatMoney(row.discountAmount) }}</b><small class="cell-sub">满 {{ formatMoney(row.minimumAmount) }} 可用</small></template></el-table-column>
        <el-table-column label="库存" width="120"><template #default="{ row }">{{ remaining(row) }} / {{ row.totalCount }}</template></el-table-column>
        <el-table-column label="每人限领" width="100"><template #default="{ row }">{{ row.perUserLimit || 0 }} 张</template></el-table-column>
        <el-table-column label="有效期" min-width="230"><template #default="{ row }"><span>{{ formatDate(row.validFrom) }}</span><small class="cell-sub">至 {{ formatDate(row.validUntil) }}</small></template></el-table-column>
        <el-table-column label="状态" width="85"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" fixed="right" width="130"><template #default="{ row }"><el-button link type="primary" @click="openEdit(row)">编辑</el-button><el-button link type="danger" @click="remove(row)">删除</el-button></template></el-table-column>
      </el-table>
      <div class="table-foot"><span>共 {{ total }} 条数据</span><el-pagination v-model:current-page="page" layout="prev, pager, next" :total="total" :page-size="pageSize" @current-change="load"/></div>
    </section>
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑优惠券' : '新建优惠券'" width="650px" destroy-on-close @closed="formRef?.clearValidate()">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="优惠券名称" prop="name"><el-input v-model="form.name" maxlength="32" show-word-limit placeholder="例如：夏日满 50 减 10"/></el-form-item>
        <div class="form-grid">
          <el-form-item label="优惠券类型"><el-select v-model="form.type"><el-option label="满减券" :value="1"/><el-option label="折扣券" :value="2"/><el-option label="新人券" :value="3"/></el-select></el-form-item>
          <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio :value="1">启用</el-radio><el-radio :value="0">停用</el-radio></el-radio-group></el-form-item>
          <el-form-item label="优惠金额（元）" prop="discountAmount"><el-input-number v-model="form.discountAmount" :min="0.01" :precision="2" :step="1" controls-position="right"/></el-form-item>
          <el-form-item label="使用门槛（元）" prop="minimumAmount"><el-input-number v-model="form.minimumAmount" :min="0" :precision="2" :step="10" controls-position="right"/></el-form-item>
          <el-form-item label="发行总量" prop="totalCount"><el-input-number v-model="form.totalCount" :min="1" :max="999999" controls-position="right"/></el-form-item>
          <el-form-item label="每人限领" prop="perUserLimit"><el-input-number v-model="form.perUserLimit" :min="1" :max="999" controls-position="right"/></el-form-item>
        </div>
        <el-form-item label="有效期" prop="validRange"><el-date-picker v-model="form.validRange" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss" start-placeholder="开始时间" end-placeholder="结束时间" range-separator="至"/></el-form-item>
        <el-form-item label="使用说明"><el-input v-model="form.description" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="填写适用范围或使用规则"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.coupon-tools{display:flex;gap:10px;margin-bottom:18px}.coupon-tools .el-input{width:300px}.coupon-tools .el-select{width:140px}.discount{display:block;color:#e84613}.cell-sub{display:block;margin-top:4px;color:#8a938d;font-size:11px}.form-grid .el-input-number,.form-grid .el-select{width:100%}.el-date-editor{width:100%}
</style>
