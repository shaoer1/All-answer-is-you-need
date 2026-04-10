<template>
  <div class="chat-log" ref="chatLogRef">
    <article v-for="(msg, idx) in messages" :key="idx" :class="['msg', msg.role]">
      <div class="role">{{ msg.role === 'user' ? '你' : 'AI' }}</div>
      <div class="content" v-html="msg.html"></div>
    </article>
  </div>
</template>

<script setup>
import { nextTick, onUpdated, ref } from 'vue'

const props = defineProps({
  messages: { type: Array, default: () => [] }
})

const chatLogRef = ref(null)

onUpdated(() => {
  nextTick(() => {
    const el = chatLogRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
})
</script>
