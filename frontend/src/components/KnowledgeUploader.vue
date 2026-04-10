<template>
  <section class="card">
    <div class="title">知识上传</div>
    <input type="file" @change="$emit('file-change', $event)" />
    <button @click="$emit('upload')" :disabled="uploading || !canUpload">{{ uploading ? '上传中...' : '上传并入库' }}</button>
    <p class="hint">{{ status }}</p>
  </section>
</template>

<script setup>
defineProps({
  uploading: Boolean,
  canUpload: Boolean,
  status: String
})
defineEmits(['file-change', 'upload'])
</script>
