<template>
  <el-dialog
    :model-value="modelValue"
    title="知识库管理"
    width="860px"
    top="7vh"
    class="kb-manager-dialog"
    @close="emit('update:modelValue', false)"
    @open="loadKnowledgeBases"
  >
    <div class="kb-layout">
      <section class="kb-list-panel">
        <div class="panel-title">知识库列表</div>
        <div v-if="loading" class="panel-empty">加载中...</div>
        <div v-else-if="list.length === 0" class="panel-empty">还没有知识库，先创建一个</div>
        <div v-else class="kb-list">
          <button
            v-for="kb in list"
            :key="kb.id"
            class="kb-item"
            :class="{ active: kb.id === localCurrentKbId }"
            @click="selectKb(kb.id)"
          >
            <span class="kb-name">{{ kb.name }}</span>
            <el-button type="danger" text size="small" @click.stop="removeKb(kb.id)">删除</el-button>
          </button>
        </div>
      </section>

      <section class="kb-form-panel">
        <div class="panel-title">新建知识库</div>
        <el-form label-position="top">
          <el-form-item label="名称">
            <el-input v-model="form.name" placeholder="例如：产品手册 / FAQ / 规范" maxlength="50" show-word-limit />
          </el-form-item>
          <el-form-item label="描述">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="5"
              placeholder="可选：记录知识库用途和内容范围"
            />
          </el-form-item>
          <el-button type="primary" :loading="creating" :disabled="!form.name.trim()" @click="createKb">创建知识库</el-button>
        </el-form>
      </section>
    </div>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listKnowledgeBases, createKnowledgeBase, deleteKnowledgeBase } from '../services/api'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  userId: { type: [Number, String], default: null },
  currentKbId: { type: [Number, String], default: null }
})

const emit = defineEmits(['update:modelValue', 'selected', 'updated'])

const loading = ref(false)
const creating = ref(false)
const list = ref([])
const localCurrentKbId = ref(props.currentKbId)
const form = reactive({
  name: '',
  description: ''
})

watch(
  () => props.currentKbId,
  (v) => {
    localCurrentKbId.value = v
  },
  { immediate: true }
)

async function loadKnowledgeBases() {
  if (!props.userId) return
  loading.value = true
  try {
    const rows = await listKnowledgeBases(props.userId)
    list.value = rows || []
    if (!localCurrentKbId.value && list.value.length > 0) {
      localCurrentKbId.value = list.value[0].id
    }
    emit('updated', list.value)
  } catch (e) {
    ElMessage.error('加载知识库失败')
  } finally {
    loading.value = false
  }
}

function selectKb(id) {
  localCurrentKbId.value = id
  emit('selected', id)
}

async function createKb() {
  if (!props.userId || !form.name.trim()) return
  creating.value = true
  try {
    await createKnowledgeBase({
      userId: props.userId,
      name: form.name.trim(),
      description: form.description.trim()
    })
    form.name = ''
    form.description = ''
    ElMessage.success('创建成功')
    await loadKnowledgeBases()
  } catch (e) {
    ElMessage.error('创建知识库失败')
  } finally {
    creating.value = false
  }
}

async function removeKb(id) {
  try {
    await ElMessageBox.confirm('确认删除该知识库？删除后不可恢复。', '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteKnowledgeBase(id)
    ElMessage.success('删除成功')
    if (localCurrentKbId.value === id) {
      localCurrentKbId.value = null
    }
    await loadKnowledgeBases()
    if (!localCurrentKbId.value && list.value.length > 0) {
      selectKb(list.value[0].id)
    }
  } catch (e) {
    // canceled or failed
  }
}
</script>

<style scoped>
.kb-layout {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 16px;
}

.kb-list-panel,
.kb-form-panel {
  background: #323544;
  border: 1px solid #4a4f66;
  border-radius: 12px;
  padding: 14px;
}

.panel-title {
  font-size: 14px;
  color: #dfe3f6;
  margin-bottom: 10px;
  font-weight: 600;
}

.panel-empty {
  color: #9da3bf;
  font-size: 13px;
  padding: 20px 0;
}

.kb-list {
  max-height: 420px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.kb-item {
  width: 100%;
  border: 1px solid #58607a;
  border-radius: 8px;
  background: #2a2d39;
  color: #e8ebfb;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
}

.kb-item.active {
  border-color: #10a37f;
  background: #27453f;
}

.kb-name {
  text-align: left;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 320px;
}

:deep(.kb-manager-dialog .el-dialog) {
  background: #2b2e3a;
  border: 1px solid #4f5670;
  border-radius: 14px;
}

:deep(.kb-manager-dialog .el-dialog__title) {
  color: #f2f4ff;
}
</style>
