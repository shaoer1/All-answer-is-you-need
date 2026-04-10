<template>
  <section class="card">
    <div class="title">会话管理</div>
    <div class="new-row">
      <input :value="newSessionName" @input="$emit('update:newSessionName', $event.target.value)" placeholder="新会话名称" />
      <button @click="$emit('create')">新建</button>
    </div>

    <ul class="session-list">
      <li v-for="s in sessions" :key="s.id" :class="{ active: s.id === sessionId }">
        <span class="name" @click="$emit('switch', s.id)">{{ s.sessionName }}</span>
        <button class="del" @click="$emit('remove', s.id)">删</button>
      </li>
    </ul>
  </section>
</template>

<script setup>
defineProps({
  sessions: { type: Array, default: () => [] },
  sessionId: Number,
  newSessionName: String
})
defineEmits(['update:newSessionName', 'create', 'switch', 'remove'])
</script>
