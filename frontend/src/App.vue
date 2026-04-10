<template>
  <div id="app">
    <LoginPage v-if="!isLoggedIn" @login-success="handleLogin" />
    <ChatPage v-else :username="username" @logout="handleLogout" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import LoginPage from './views/LoginPage.vue'
import ChatPage from './views/ChatPage.vue'

const isLoggedIn = ref(false)
const username = ref('')

const handleLogin = (user) => {
  username.value = user
  isLoggedIn.value = true
}

const handleLogout = () => {
  isLoggedIn.value = false
  username.value = ''
}

onMounted(() => {
  const savedUsername = localStorage.getItem('username')
  if (savedUsername) {
    username.value = savedUsername
    isLoggedIn.value = true
  }
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#app {
  width: 100%;
  height: 100vh;
}
</style>