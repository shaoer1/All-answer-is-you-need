<template>
  <section class="card trace">
    <div class="title">工具过程可视化</div>
    <ul>
      <li v-for="(item, idx) in traces" :key="idx">{{ item }}</li>
    </ul>
  </section>
</template>

<script setup>
defineProps({
  traces: { type: Array, default: () => [] }
})
</script>
