<template>
  <section class="card login">
    <div class="title-row">
      <h2>用户与知识库</h2>
      <button class="ghost" @click="$emit('toggle-theme')">{{ theme === 'dark' ? '浅色' : '深色' }}</button>
    </div>

    <div class="tabs">
      <button :class="{ active: mode === 'login' }" @click="$emit('update:mode', 'login')">登录</button>
      <button :class="{ active: mode === 'register' }" @click="$emit('update:mode', 'register')">注册</button>
    </div>

    <label>用户名</label>
    <input :value="username" @input="$emit('update:username', $event.target.value)" />

    <label>密码</label>
    <input type="password" :value="password" @input="$emit('update:password', $event.target.value)" />

    <label v-if="mode === 'register'">昵称</label>
    <input v-if="mode === 'register'" :value="nickname" @input="$emit('update:nickname', $event.target.value)" />

    <label>知识库ID</label>
    <input :value="kbId" @input="$emit('update:kbId', $event.target.value)" />

    <button @click="$emit('auth')" :disabled="loading">{{ loading ? '处理中...' : (mode === 'login' ? '登录并初始化' : '注册并初始化') }}</button>
  </section>
</template>

<script setup>
defineProps({
  username: String,
  password: String,
  nickname: String,
  kbId: String,
  loading: Boolean,
  theme: String,
  mode: String
})
defineEmits([
  'update:username',
  'update:password',
  'update:nickname',
  'update:kbId',
  'update:mode',
  'auth',
  'toggle-theme'
])
</script>
