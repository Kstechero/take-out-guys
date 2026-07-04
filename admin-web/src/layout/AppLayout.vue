<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { DataLine, MagicStick, List, Dish, Ticket, ChatDotRound, Warning, User, Fold, Expand, Bell, SwitchButton, Menu, Box, TrendCharts } from '@element-plus/icons-vue'
import { getAiHealth, getShopStatus, setShopStatus } from '@/api/admin'

const route = useRoute(); const router = useRouter(); const auth = useAuthStore(); const collapsed = ref(false); const shopOpen = ref(false)
const aiStatus = ref<'pending' | 'up' | 'down'>('pending')
const aiStatusTitle = computed(() => aiStatus.value === 'up' ? 'AI 后端已连接' : aiStatus.value === 'down' ? 'AI 后端异常' : 'AI 后端待接入')
const aiStatusDetail = ref('GX10 · ornith 状态未知')
const title = computed(() => route.meta.title || '运营中心')
const menus = [
  ['/dashboard', '经营总览', DataLine], ['/reports', '数据统计', TrendCharts], ['/agent', 'AI Agent', MagicStick], ['/orders', '订单管理', List],
  ['/dishes', '菜品管理', Dish], ['/categories', '分类管理', Menu], ['/setmeals', '套餐管理', Box], ['/coupons', '优惠券', Ticket], ['/reviews', '评价管理', ChatDotRound],
  ['/service', '人工客服', ChatDotRound], ['/sensitive', '敏感词库', Warning], ['/employees', '员工管理', User]
]
async function logout() { await auth.logout(); router.push('/login') }
async function changeShop(value: string | number | boolean) { await setShopStatus(value ? 1 : 0) }
async function refreshAiHealth() {
  try {
    const res: any = await getAiHealth()
    const health = res.data || {}
    aiStatus.value = health.status === 'UP' ? 'up' : 'down'
    aiStatusDetail.value = `${health.provider || 'AI'} · ${health.model || 'unknown'} · ${health.status || 'UNKNOWN'}`
  } catch {
    aiStatus.value = 'down'
    aiStatusDetail.value = 'GX10 · ornith · 连接失败'
  }
}
onMounted(async () => {
  const res: any = await getShopStatus()
  shopOpen.value = Number(res.data) === 1
  await refreshAiHealth()
})
</script>

<template>
  <div class="shell">
    <aside :class="['sidebar', { collapsed }]">
      <div class="brand"><img class="brand-bot" src="/takeout-guys-bot.png" alt="Takeout Guys" /><div v-if="!collapsed"><strong>Takeout <em>Guys</em></strong><small>AI OPERATIONS</small></div></div>
      <nav>
        <router-link v-for="m in menus" :key="m[0] as string" :to="m[0] as string" class="nav-item">
          <el-icon><component :is="m[2]" /></el-icon><span v-if="!collapsed">{{ m[1] }}</span>
          <i v-if="m[0] === '/service' && !collapsed" class="dot">3</i>
        </router-link>
      </nav>
      <div :class="['ai-status', aiStatus]" v-if="!collapsed"><i></i><div><b>{{ aiStatusTitle }}</b><small>{{ aiStatusDetail }}</small></div></div>
    </aside>
    <section class="main">
      <header>
        <button class="icon-btn" @click="collapsed = !collapsed"><el-icon><component :is="collapsed ? Expand : Fold" /></el-icon></button>
        <div><small class="breadcrumb">运营中心 / {{ title }}</small><h1>{{ title }}</h1></div>
        <div class="header-actions"><span class="shop-state">店铺营业</span><el-switch v-model="shopOpen" @change="changeShop"/><button class="icon-btn"><el-icon><Bell /></el-icon><i class="notice"></i></button><div class="avatar">{{ auth.name.slice(0, 1) }}</div><div class="account"><b>{{ auth.name }}</b><small>超级管理员</small></div><button class="icon-btn" @click="logout"><el-icon><SwitchButton /></el-icon></button></div>
      </header>
      <main><router-view /></main>
    </section>
  </div>
</template>
