<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
const form = reactive({ username: 'admin', password: '123456' }); const loading = ref(false)
const auth = useAuthStore(); const router = useRouter()
async function submit() { loading.value = true; try { await auth.login(form.username, form.password); router.push('/') } finally { loading.value = false } }
</script>
<template>
  <div class="login-page">
    <div class="login-visual"><img class="login-logo" src="/takeout-guys-logo.png" alt="Takeout Guys AI"/><div class="speed-lines"><i></i><i></i><i></i></div><p class="eyebrow">TAKEOUT GUYS INTELLIGENCE</p><h1>外卖够快，<br>决策更聪明。</h1><p>从经营洞察到顾客服务，Takeout Guys AI Agent 正在重新组织餐厅的每一次决策。</p><div class="visual-stats"><span><b>7×24</b>智能值守</span><span><b>98.6%</b>服务可用率</span></div></div>
    <div class="login-panel"><div class="login-box"><img class="login-bot" src="/takeout-guys-bot.png" alt="Takeout Guys"/><p class="eyebrow">WELCOME BACK</p><h2>登录运营中心</h2><p class="muted">使用管理员账号继续</p><el-form @submit.prevent="submit"><label>账号</label><el-input v-model="form.username" size="large" placeholder="请输入账号" /><label>密码</label><el-input v-model="form.password" size="large" type="password" show-password placeholder="请输入密码" /><el-button type="primary" size="large" :loading="loading" @click="submit">进入控制台</el-button></el-form><small class="login-foot">Takeout Guys AI · 安全连接</small></div></div>
  </div>
</template>
