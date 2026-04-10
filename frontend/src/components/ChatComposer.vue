<template>
  <div class="composer">
    <textarea :value="question" placeholder="输入问题，按 Ctrl+Enter 发送" @input="$emit('update:question', $event.target.value)" @keydown.ctrl.enter.prevent="$emit('ask')" />
    <button @click="$emit('ask')" :disabled="asking || !question.trim() || !sessionId">{{ asking ? '推理中...' : '发送' }}</button>
  </div>
</template>

<script setup>
defineProps({
  question: String,
  asking: Boolean,
  sessionId: Number
})
defineEmits(['update:question', 'ask'])
</script>
