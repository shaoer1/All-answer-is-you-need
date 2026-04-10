<template>
  <div class="chat-container">
    <aside class="sidebar">
      <div class="sidebar-header">
        <button class="new-chat-button" @click="createNewSession">
          <el-icon><Plus /></el-icon>
          新对话
        </button>
      </div>
      
      <div class="sidebar-content">
        <div class="section-title">会话列表</div>
        <div class="session-list">
          <div 
            v-for="session in sessions" 
            :key="session.id"
            :class="['session-item', { active: sessionId === session.id }]"
            @click="switchSession(session.id)"
          >
            <div class="session-name" v-if="!editingSessionId || editingSessionId !== session.id">
              {{ session.name }}
            </div>
            <div class="session-name-edit" v-else>
              <el-input 
                v-model="editingSessionName" 
                size="small" 
                @blur="saveSessionName(session.id)"
                @keyup.enter="saveSessionName(session.id)"
                @keyup.esc="cancelEditSessionName"
                ref="sessionNameInput"
              />
            </div>
            <div class="session-actions">
              <el-icon class="edit-icon" @click.stop="startEditSessionName(session.id, session.name)">
                <Edit />
              </el-icon>
              <el-icon class="delete-icon" @click.stop="removeSession(session.id)">
                <Delete />
              </el-icon>
            </div>
          </div>
        </div>
        
        <div class="section-title" style="margin-top: 24px;">知识库</div>
        <div class="kb-compact-card">
          <div class="kb-compact-row">
            <el-icon class="kb-icon"><Folder /></el-icon>
            <div class="kb-compact-name">{{ currentKbName || '未选择知识库' }}</div>
          </div>
          <button class="create-kb-button" @click="showKbManagerDialog = true">
            <el-icon><Setting /></el-icon>
            管理知识库
          </button>
        </div>
      </div>
      
      <div class="sidebar-footer">
        <div class="user-info">
          <el-avatar :size="32">{{ username.charAt(0).toUpperCase() }}</el-avatar>
          <span class="username">{{ username }}</span>
        </div>
        <button type="button" class="plain-link-btn" @click="handleLogout">退出登录</button>
      </div>
    </aside>
    
    <main class="main-content">
      <div class="chat-area">
        <div class="messages-container" ref="messagesContainer">
          <div v-if="messages.length === 0" class="empty-state">
            <el-icon class="empty-icon"><ChatLineRound /></el-icon>
            <h2>开始新对话</h2>
            <p>输入您的问题，我将为您提供专业的回答</p>
          </div>
          
          <div v-else class="messages-list">
            <div 
              v-for="message in messages" 
              :key="message.id"
              :class="['message-item', message.role]"
            >
              <div class="message-avatar">
                <el-avatar :size="32">
                  {{ message.role === 'user' ? username.charAt(0).toUpperCase() : 'AI' }}
                </el-avatar>
              </div>
              <div class="message-content">
                <div class="message-header">
                  <span class="message-role">{{ message.role === 'user' ? username : 'AI' }}</span>
                  <span class="message-time">{{ formatMessageTime(message.createdAt) }}</span>
                </div>
                <div class="message-text" v-html="message.raw"></div>
                
                <div v-if="message.role === 'assistant' && hasDisplayTrace(message.trace) && message.showTrace" class="trace-content">
                  <div class="trace-header">思考过程：</div>
                  <div class="trace-steps">
                    <div v-for="(step, i) in formatTraceSteps(message.trace)" :key="i" class="trace-step-card">
                      <div class="trace-step-title">{{ step.phase || '关键过程' }}</div>
                      <div class="trace-step-content">{{ step.content }}</div>
                    </div>
                  </div>
                </div>
                <button
                  type="button"
                  v-if="message.role === 'assistant' && hasDisplayTrace(message.trace)"
                  class="trace-toggle"
                  @click="message.showTrace = !message.showTrace"
                >
                  {{ message.showTrace ? '隐藏思考' : '显示思考' }}
                </button>
              </div>
            </div>
          </div>
        </div>
        
        <div class="input-area">
          <div class="upload-section">
            <el-upload
              action="#"
              :auto-upload="false"
              :on-change="onFileChange"
              :show-file-list="false"
              :disabled="!sessionId"
            >
              <button type="button" class="plain-link-btn" :disabled="!sessionId">
                <el-icon><Upload /></el-icon>
                上传文档
              </button>
            </el-upload>
            <div v-if="uploadStatus" class="upload-status">
              {{ uploadStatus }}
            </div>
          </div>
          
          <div class="input-wrapper">
            <el-input
              v-model="question"
              type="textarea"
              :rows="1"
              placeholder="输入您的问题..."
              :disabled="!sessionId"
              @keydown.enter.prevent="handleEnter"
              resize="none"
            />
            <el-button 
              type="primary" 
              :disabled="!question.trim() || !sessionId || asking"
              @click="ask"
            >
              <el-icon v-if="asking"><Loading /></el-icon>
              <el-icon v-else><Position /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </main>
    
    <KnowledgeBaseManagerDialog
      v-model="showKbManagerDialog"
      :user-id="userId"
      :current-kb-id="currentKbId"
      @selected="onKnowledgeBaseSelected"
      @updated="onKnowledgeBaseListUpdated"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick, watch, computed } from 'vue'
import { Plus, Delete, Upload, Folder, ChatLineRound, User, Position, Loading, Edit, Setting } from '@element-plus/icons-vue'
import {
  createSession,
  deleteSession,
  updateSessionName,
  initUser,
  listMessages,
  listSessions,
  uploadKnowledge,
  streamChat,
  getUserId,
  listKnowledgeBases
} from '../services/api'
import KnowledgeBaseManagerDialog from '../components/KnowledgeBaseManagerDialog.vue'

const props = defineProps({
  username: {
    type: String,
    required: true
  }
})

const emit = defineEmits(['logout'])

const sessions = ref([])
const sessionId = ref(null)
const knowledgeBases = ref([])
const currentKbId = ref(null)
const question = ref('')
const messages = ref([])
const asking = ref(false)
const uploading = ref(false)
const uploadStatus = ref('')
const messagesContainer = ref(null)
const file = ref(null)
const userId = ref(null)
const showKbManagerDialog = ref(false)

// 编辑会话名称相关状态
const editingSessionId = ref(null)
const editingSessionName = ref('')
const sessionNameInput = ref(null)

let rafId = 0
let pendingText = ''
let assistantIndex = -1
let messageAutoId = 1

const currentKbName = computed(() => knowledgeBases.value.find((kb) => kb.id === currentKbId.value)?.name || '')

// 格式化消息时间
const formatMessageTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleString()
}


const loadUserInfo = async () => {
  try {
    const user = await getUserId(props.username)
    userId.value = user?.userId ?? user?.id ?? null
    if (!userId.value) {
      throw new Error('获取 userId 失败')
    }
    await loadKnowledgeBases()
    // 在知识库加载完成后，加载会话列表
    await loadSessions()
  } catch (error) {
    console.error('加载用户信息失败:', error)
  }
}

const loadKnowledgeBases = async () => {
  try {
    if (!userId.value) {
      console.warn('跳过加载知识库：userId 为空')
      return
    }
    console.log('开始加载知识库，userId:', userId.value)
    const kbs = await listKnowledgeBases(userId.value)
    console.log('加载知识库成功:', kbs)
    if (kbs.length > 0) {
      knowledgeBases.value = kbs
      currentKbId.value = kbs[0].id
      // 在知识库加载完成后，加载会话列表
      await loadSessions()
    } else {
      knowledgeBases.value = []
      currentKbId.value = null
    }
  } catch (error) {
    console.error('加载知识库失败:', error)
    console.error('错误详情:', error.response)
    // 即使加载知识库失败，也尝试创建一个默认会话
    if (userId.value) {
      await createNewSession()
    }
  }
}

const loadSessions = async () => {
  try {
    const rows = await listSessions(props.username)
    sessions.value = (rows || []).map((s) => ({
      ...s,
      // 兼容后端返回 sessionName，统一给前端模板使用 name
      name: s?.name ?? s?.sessionName ?? `对话 ${s?.id ?? ''}`,
      sessionName: s?.sessionName ?? s?.name ?? `对话 ${s?.id ?? ''}`
    }))
    if (sessions.value.length > 0 && !sessionId.value) {
      sessionId.value = sessions.value[0].id
      await loadSessionMessages()
    } else if (sessions.value.length === 0 && currentKbId.value) {
      // 如果没有会话，自动创建一个默认会话
      await createNewSession()
    }
  } catch (error) {
    console.error('加载会话失败:', error)
  }
}

const createNewSession = async () => {
  try {
    console.log('开始创建新会话，kbId:', currentKbId.value || 1)
    const created = await createSession({ 
      username: props.username, 
      kbId: currentKbId.value || 1, 
      sessionName: `对话 ${sessions.value.length + 1}` 
    })
    console.log('创建会话成功:', created)
    sessionId.value = created.sessionId
    await loadSessions()
    messages.value = []
  } catch (error) {
    console.error('创建会话失败:', error)
    console.error('错误详情:', error.response)
    // 即使创建会话失败，也设置一个默认的sessionId，以便输入框可以使用
    sessionId.value = 1
  }
}

const switchSession = async (id) => {
  sessionId.value = id
  await loadSessionMessages()
}

const removeSession = async (id) => {
  try {
    await deleteSession({ username: props.username, sessionId: id })
    if (sessionId.value === id) {
      sessionId.value = sessions.value.find(s => s.id !== id)?.id || null
      messages.value = []
    }
    await loadSessions()
  } catch (error) {
    console.error('删除会话失败:', error)
  }
}

const loadSessionMessages = async () => {
  if (!sessionId.value) {
    messages.value = []
    return
  }
  
  try {
    const rows = await listMessages({ 
      username: props.username, 
      sessionId: sessionId.value, 
      limit: 50 
    })
    messages.value = [...rows].reverse().map((m) => ({
      id: m.id ?? `msg-${messageAutoId++}`,
      role: m.role,
      raw: m.messageContent || '',
      createdAt: m.createdAt || null,
      trace: [],
      showTrace: false
    }))
    
    await nextTick()
    scrollToBottom()
  } catch (error) {
    console.error('加载消息失败:', error)
  }
}

const onKnowledgeBaseSelected = async (kbId) => {
  currentKbId.value = kbId
  await loadSessions()
}

const onKnowledgeBaseListUpdated = async (list) => {
  knowledgeBases.value = list || []
  if (knowledgeBases.value.length === 0) {
    currentKbId.value = null
    return
  }
  if (!knowledgeBases.value.some((kb) => kb.id === currentKbId.value)) {
    currentKbId.value = knowledgeBases.value[0].id
    await loadSessions()
  }
}

const onFileChange = (file) => {
  file.value = file.raw
  uploadFile()
}

const uploadFile = async () => {
  if (!file.value || !sessionId.value) return
  
  uploading.value = true
  try {
    const data = await uploadKnowledge({ 
      username: props.username, 
      kbId: currentKbId.value, 
      file: file.value 
    })
    uploadStatus.value = `文档已入库：chunk=${data.chunkCount}`
    setTimeout(() => {
      uploadStatus.value = ''
    }, 3000)
  } catch (error) {
    uploadStatus.value = '上传失败：' + error.message
    setTimeout(() => {
      uploadStatus.value = ''
    }, 3000)
  } finally {
    uploading.value = false
    file.value = null
  }
}

const handleEnter = (e) => {
  if (!e.shiftKey) {
    ask()
  }
}

const ask = async () => {
  const q = question.value.trim()
  if (!q || asking.value || !sessionId.value) return

  asking.value = true
  messages.value.push({ id: messageAutoId++, role: 'user', raw: q, createdAt: new Date().toISOString(), trace: [], showTrace: false })
  messages.value.push({ id: messageAutoId++, role: 'assistant', raw: '', createdAt: new Date().toISOString(), trace: [], showTrace: false })
  assistantIndex = messages.value.length - 1
  pendingText = ''
  question.value = ''

  try {
    await streamChat({
      username: props.username,
      kbId: currentKbId.value || 'default',
      sessionId: sessionId.value,
      question: q,
      onToken: (token) => {
        pendingText += token
        if (messages.value[assistantIndex]) {
          messages.value[assistantIndex].raw = pendingText
        }
        scheduleFlush()
      },
      onTrace: (trace) => {
        if (messages.value[assistantIndex]) {
          const cur = messages.value[assistantIndex]
          if (!cur.trace.includes(trace)) {
            cur.trace.push(trace)
          }
          cur.showTrace = true
        }
      },
      onError: (error) => {
        console.error('Error:', error)
      },
      onDone: () => {
        if (rafId) cancelAnimationFrame(rafId)
        flushStream()
      }
    })
  } catch (error) {
    messages.value[assistantIndex].raw = '推理失败：' + error.message
  } finally {
    asking.value = false
  }
}

const flushStream = () => {
  if (assistantIndex < 0) return
  rafId = 0
  scrollToBottom()
}

const scheduleFlush = () => {
  if (rafId) return
  rafId = requestAnimationFrame(flushStream)
}

const TRACE_NOISE_RE = /(开始执行|进入问答链路|通过安全检查|开始向量化|向量生成成功|拼接会话记忆|开启流式输出|token 分片发送|持久化完成|本轮执行完成|读取会话摘要|准备逐段发送答案|调用大模型生成答案|进入答案校验|判定通过)/

const pickTraceContent = (step) => {
  const candidates = [step.action, step.observe, step.judge]
    .map((s) => (s || '').trim())
    .filter(Boolean)
  for (const c of candidates) {
    if (!TRACE_NOISE_RE.test(c)) return c
  }
  return candidates[0] || ''
}

const hasDisplayTrace = (traceLines = []) => formatTraceSteps(traceLines).length > 0

const formatTraceSteps = (traceLines = []) => {
  const steps = []
  let current = null
  for (const raw of traceLines) {
    const line = String(raw || '').trim()
    if (!line) continue
    if (line.startsWith('[阶段]')) {
      if (current) steps.push(current)
      current = { phase: line.replace('[阶段]', '').trim(), observe: '', judge: '', action: '' }
      continue
    }
    if (!current) {
      current = { phase: '过程', observe: '', judge: '', action: '' }
    }
    const parseField = (tag) => {
      const body = line.slice(tag.length).trim()
      if (!body) return ''
      const normalized = body.replace(/^(?:未提供|无|空|暂无|\-|—|null|undefined)$/i, '').trim()
      return normalized
    }
    if (line.startsWith('[观察]')) {
      current.observe = parseField('[观察]')
    } else if (line.startsWith('[判断]')) {
      current.judge = parseField('[判断]')
    } else if (line.startsWith('[动作]')) {
      current.action = parseField('[动作]')
    } else {
      current.action = current.action ? `${current.action}；${line}` : line
    }
  }
  if (current) steps.push(current)

  const cleaned = steps
    .map((step) => {
      const content = pickTraceContent(step)
      return {
        phase: step.phase || '',
        content
      }
    })
    .filter((step) => step.content && step.content.length >= 6)
    .filter((step) => !/^耗时=\d+ms$/i.test(step.content))

  const dedup = []
  const seen = new Set()
  for (const s of cleaned) {
    const k = `${s.phase}|${s.content}`
    if (seen.has(k)) continue
    seen.add(k)
    dedup.push(s)
    if (dedup.length >= 6) break
  }
  return dedup
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const handleLogout = () => {
  localStorage.removeItem('username')
  emit('logout')
}

// 编辑会话名称相关方法
const startEditSessionName = (id, name) => {
  editingSessionId.value = id
  editingSessionName.value = name
  nextTick(() => {
    const refValue = sessionNameInput.value
    const inputComp = Array.isArray(refValue) ? refValue[0] : refValue
    if (inputComp && typeof inputComp.focus === 'function') {
      inputComp.focus()
      return
    }
    if (inputComp?.input && typeof inputComp.input.focus === 'function') {
      inputComp.input.focus()
    }
  })
}

const saveSessionName = async (id) => {
  if (!editingSessionName.value.trim()) {
    cancelEditSessionName()
    return
  }
  
  try {
    // 调用后端API更新会话名称
    await updateSessionName({
      username: props.username,
      sessionId: id,
      name: editingSessionName.value.trim()
    })
    // 更新前端数据
    const session = sessions.value.find(s => s.id === id)
    if (session) {
      const newName = editingSessionName.value.trim()
      session.name = newName
      session.sessionName = newName
    }
    editingSessionId.value = null
    editingSessionName.value = ''
  } catch (error) {
    console.error('更新会话名称失败:', error)
    cancelEditSessionName()
  }
}

const cancelEditSessionName = () => {
  editingSessionId.value = null
  editingSessionName.value = ''
}

onMounted(() => {
  loadUserInfo()
})

onBeforeUnmount(() => {
  // 清理资源
})

watch(() => sessionId.value, () => {
  if (sessionId.value) {
    loadSessionMessages()
  }
})
</script>

<style scoped>
.chat-container {
  display: flex;
  height: 100vh;
  background: #343541;
  color: #ececf1;
}

.sidebar {
  width: 260px;
  background: #202123;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #4d4d4f;
}

.sidebar-header {
  padding: 16px;
}

.new-chat-button {
  width: 100%;
  padding: 12px;
  background: #343541;
  border: 1px solid #565869;
  border-radius: 6px;
  color: #ececf1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  transition: all 0.2s;
}

.new-chat-button:hover {
  background: #40414f;
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px;
}

.section-title {
  font-size: 12px;
  color: #8e8ea0;
  margin: 16px 0 8px;
  font-weight: 500;
}

.session-list,
.knowledge-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.session-item,
.knowledge-item {
  padding: 12px;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.2s;
  font-size: 14px;
}

.session-item:hover,
.knowledge-item:hover {
  background: #2a2b32;
}

.session-item.active,
.knowledge-item.active {
  background: #343541;
}

.session-name,
.kb-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-actions,
.kb-actions {
  opacity: 0;
  transition: opacity 0.2s;
}

.session-item:hover .session-actions,
.knowledge-item:hover .kb-actions {
  opacity: 1;
}

.delete-icon, .edit-icon {
  color: #8e8ea0;
  cursor: pointer;
  margin-left: 8px;
}

.delete-icon:hover {
  color: #ef4444;
}

.edit-icon:hover {
  color: #10a37f;
}

.session-name-edit {
  flex: 1;
  min-width: 0;
}

.session-name-edit :deep(.el-input__inner) {
  background: #40414f;
  border: 1px solid #565869;
  color: #ececf1;
  border-radius: 4px;
  font-size: 14px;
  padding: 4px 8px;
  height: 28px;
}

.session-name-edit :deep(.el-input__inner):focus {
  border-color: #10a37f;
  outline: none;
}

.kb-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.kb-icon {
  color: #8e8ea0;
}

.create-kb-button {
  width: 100%;
  padding: 8px;
  background: transparent;
  border: 1px dashed #565869;
  border-radius: 6px;
  color: #8e8ea0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 13px;
  margin-top: 8px;
  transition: all 0.2s;
}

.kb-compact-card {
  border: 1px solid #4a4d61;
  border-radius: 10px;
  padding: 10px;
  background: #2a2c36;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.kb-compact-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.kb-compact-name {
  font-size: 13px;
  color: #d8daea;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.create-kb-button:hover {
  background: #2a2b32;
  border-color: #8e8ea0;
  color: #ececf1;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid #4d4d4f;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.username {
  font-size: 14px;
  color: #ececf1;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px 0;
  max-height: calc(100vh - 200px);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #8e8ea0;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  color: #6b7280;
}

.empty-state h2 {
  font-size: 24px;
  font-weight: 500;
  margin: 0 0 8px 0;
  color: #ececf1;
}

.empty-state p {
  font-size: 14px;
  margin: 0;
}

.messages-list {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 20px;
}

.message-item {
  display: flex;
  margin-bottom: 20px;
  gap: 12px;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  flex: 1;
  max-width: 70%;
}

.message-item.user .message-content {
  text-align: right;
}

.message-header {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
  font-size: 12px;
  color: #8e8ea0;
}

.message-item.user .message-header {
  justify-content: flex-end;
}

.message-role {
  font-weight: 500;
  margin-right: 8px;
}

.message-item.user .message-role {
  margin-right: 0;
  margin-left: 8px;
}

.message-time {
  opacity: 0.7;
}

.message-text {
  padding: 12px 16px;
  border-radius: 8px;
  line-height: 1.6;
  font-size: 15px;
  word-break: break-word;
  overflow-wrap: anywhere;
  text-align: left;
}

.message-item.user .message-text {
  background: #343541;
  border: 1px solid #565869;
  border-bottom-right-radius: 2px;
}

.message-item.assistant .message-text {
  background: #444654;
  border-bottom-left-radius: 2px;
}

.trace-toggle {
  margin-top: 4px;
  border: none;
  background: transparent;
  color: #8e8ea0;
  cursor: pointer;
  padding: 0;
}

.trace-content {
  margin-top: 8px;
  padding: 12px;
  background: #343541;
  border: 1px solid #565869;
  border-radius: 6px;
  font-size: 13px;
  color: #8e8ea0;
}

.trace-header {
  font-weight: 500;
  margin-bottom: 8px;
  color: #10a37f;
}

.trace-steps {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.trace-step-card {
  border: 1px solid #535874;
  border-radius: 8px;
  padding: 10px;
  background: #2f3241;
}

.trace-step-title {
  color: #e9ecff;
  font-size: 13px;
  margin-bottom: 6px;
  font-weight: 600;
}

.trace-step-content {
  line-height: 1.5;
  color: #c7cbe0;
}

.input-area {
  padding: 20px;
  border-top: 1px solid #4d4d4f;
  background: #343541;
}

.upload-section {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.upload-status {
  font-size: 12px;
  color: #10b981;
}

.trace-toggle {
  margin-top: 8px;
  font-size: 12px;
}

.plain-link-btn {
  border: none;
  background: transparent;
  color: #ececf1;
  cursor: pointer;
  padding: 0;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.plain-link-btn:disabled {
  color: #565869;
  cursor: not-allowed;
}

.input-wrapper {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.input-wrapper :deep(.el-textarea__inner) {
  background: #40414f;
  border: 1px solid #565869;
  color: #ececf1;
  border-radius: 8px;
  resize: none;
  min-height: 52px;
}

.input-wrapper :deep(.el-textarea__inner):focus {
  border-color: #10a37f;
  outline: none;
}

.input-wrapper :deep(.el-textarea__inner)::placeholder {
  color: #8e8ea0;
}

.input-wrapper .el-button {
  height: 52px;
  min-width: 52px;
  padding: 0;
}

:deep(.el-button--primary) {
  background: #10a37f;
  border-color: #10a37f;
}

:deep(.el-button--primary:hover) {
  background: #1a7f64;
  border-color: #1a7f64;
}

:deep(.el-button--primary.is-disabled) {
  background: #40414f;
  border-color: #565869;
  color: #8e8ea0;
}

:deep(.el-button--text) {
  color: #ececf1;
}

:deep(.el-button--text:hover) {
  background: #40414f;
}

:deep(.el-button--text.is-disabled) {
  color: #565869;
}
</style>