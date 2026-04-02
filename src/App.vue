<template>
  <main class="app-shell">
    <aside class="sidebar">
      <h1>离线智能问答</h1>
      <p class="sub">按 README：用户隔离 + 会话管理 + 私有知识库</p>

      <label>用户名</label>
      <input v-model="username" />

      <label>知识库ID</label>
      <input v-model="kbId" />

      <button @click="initAndLoad" :disabled="loadingSessions">{{ loadingSessions ? '加载中...' : '初始化并加载会话' }}</button>

      <div class="session-block">
        <div class="session-title">会话</div>
        <div class="session-actions">
          <input v-model="newSessionName" placeholder="新会话名称" />
          <button @click="createNewSession">新建</button>
        </div>
        <ul class="session-list">
          <li v-for="s in sessions" :key="s.id" :class="{ active: s.id === sessionId }">
            <span class="name" @click="switchSession(s.id)">{{ s.sessionName }}</span>
            <button class="del" @click="removeSession(s.id)">删</button>
          </li>
        </ul>
      </div>

      <label>上传语料</label>
      <input type="file" @change="onFileChange" />
      <button @click="upload" :disabled="!file || uploading || !sessionId">{{ uploading ? '上传中...' : '上传并入库' }}</button>
      <p class="hint">{{ uploadStatus }}</p>
    </aside>

    <section class="chat-panel">
      <div class="chat-log" ref="chatLogRef">
        <article v-for="(msg, idx) in messages" :key="idx" :class="['msg', msg.role]">
          <div class="role">{{ msg.role === 'user' ? '你' : 'AI' }}</div>
          <div class="content" v-html="msg.html"></div>
        </article>
      </div>

      <div class="composer">
        <textarea v-model="question" placeholder="输入问题，按 Ctrl+Enter 发送" @keydown.ctrl.enter.prevent="ask" />
        <button @click="ask" :disabled="asking || !question.trim() || !sessionId">{{ asking ? '推理中...' : '发送' }}</button>
      </div>
    </section>
  </main>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import {
  createSession,
  deleteSession,
  initUser,
  listMessages,
  listSessions,
  streamChat,
  uploadKnowledge
} from './services/api'
import { renderPretext } from './utils/pretext'

const username = ref('demo')
const kbId = ref('kb-default')
const newSessionName = ref('默认会话')
const sessions = ref([])
const sessionId = ref(null)
const loadingSessions = ref(false)

const question = ref('')
const file = ref(null)
const uploading = ref(false)
const asking = ref(false)
const uploadStatus = ref('')
const messages = ref([])
const chatLogRef = ref(null)

let rafId = 0
let pendingText = ''
let assistantIndex = -1

const flushStream = () => {
  if (assistantIndex < 0) return
  messages.value[assistantIndex].html = renderPretext(pendingText)
  nextTick(() => {
    const el = chatLogRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
  rafId = 0
}

const scheduleFlush = () => {
  if (rafId) return
  rafId = requestAnimationFrame(flushStream)
}

const initAndLoad = async () => {
  loadingSessions.value = true
  try {
    await initUser(username.value)
    sessions.value = await listSessions(username.value)
    if (!sessions.value.length) {
      const created = await createSession({ username: username.value, kbId: kbId.value, sessionName: newSessionName.value || '默认会话' })
      sessionId.value = created.sessionId
      sessions.value = await listSessions(username.value)
    } else {
      sessionId.value = sessions.value[0].id
    }
    await loadSessionMessages()
  } finally {
    loadingSessions.value = false
  }
}

const createNewSession = async () => {
  const name = newSessionName.value.trim()
  if (!name) return
  const created = await createSession({ username: username.value, kbId: kbId.value, sessionName: name })
  sessions.value = await listSessions(username.value)
  sessionId.value = created.sessionId
  await loadSessionMessages()
}

const switchSession = async (id) => {
  sessionId.value = id
  await loadSessionMessages()
}

const removeSession = async (id) => {
  await deleteSession({ username: username.value, sessionId: id })
  sessions.value = await listSessions(username.value)
  if (sessionId.value === id) {
    sessionId.value = sessions.value[0]?.id || null
    await loadSessionMessages()
  }
}

const loadSessionMessages = async () => {
  if (!sessionId.value) {
    messages.value = []
    return
  }
  const rows = await listMessages({ username: username.value, sessionId: sessionId.value, limit: 50 })
  const asc = [...rows].reverse()
  messages.value = asc.map((m) => ({ role: m.role, html: renderPretext(m.messageContent) }))
}

const onFileChange = (e) => {
  file.value = e.target.files?.[0] || null
}

const upload = async () => {
  if (!file.value) return
  uploading.value = true
  try {
    const data = await uploadKnowledge({ username: username.value, kbId: kbId.value, file: file.value })
    uploadStatus.value = `文档已入库：chunk=${data.chunkCount}，增量忽略=${data.ignoredHashes.length}`
  } catch (e) {
    uploadStatus.value = `上传失败：${e.message}`
  } finally {
    uploading.value = false
  }
}

const ask = async () => {
  const q = question.value.trim()
  if (!q || asking.value || !sessionId.value) return

  asking.value = true
  messages.value.push({ role: 'user', html: renderPretext(q) })
  messages.value.push({ role: 'assistant', html: '' })
  assistantIndex = messages.value.length - 1
  pendingText = ''
  question.value = ''

  try {
    await streamChat({
      username: username.value,
      kbId: kbId.value,
      sessionId: sessionId.value,
      question: q,
      onToken: (token) => {
        pendingText += token
        scheduleFlush()
      },
      onDone: () => {
        if (rafId) cancelAnimationFrame(rafId)
        flushStream()
      }
    })
  } catch (e) {
    messages.value[assistantIndex].html = renderPretext('推理失败：' + e.message)
  } finally {
    asking.value = false
  }
}

onMounted(initAndLoad)
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=ZCOOL+XiaoWei&family=JetBrains+Mono:wght@400;700&display=swap');

:root {
  --bg: #0f1115;
  --card: #171a21;
  --line: #2b3242;
  --accent: #2ed3a8;
  --accent-2: #f8b84e;
  --text: #f3f5f8;
  --muted: #95a0b3;
}

.app-shell {
  min-height: 100vh;
  color: var(--text);
  background: radial-gradient(circle at 15% 20%, #17344a 0%, transparent 35%),
    radial-gradient(circle at 82% 12%, #422f52 0%, transparent 38%),
    linear-gradient(135deg, #0b0f14, #131823 55%, #0f1115);
  display: grid;
  grid-template-columns: 350px 1fr;
  gap: 16px;
  padding: 16px;
  box-sizing: border-box;
  font-family: 'ZCOOL XiaoWei', serif;
}

.sidebar, .chat-panel {
  background: color-mix(in srgb, var(--card) 92%, #000 8%);
  border: 1px solid var(--line);
  border-radius: 18px;
  padding: 18px;
  backdrop-filter: blur(4px);
}

.sub { color: var(--muted); margin: 8px 0 18px; }
label { display: block; margin: 12px 0 6px; color: var(--muted); }
input, textarea, button {
  width: 100%;
  border-radius: 12px;
  border: 1px solid var(--line);
  background: #11151d;
  color: var(--text);
  padding: 10px;
  box-sizing: border-box;
  font-family: 'JetBrains Mono', monospace;
}
button {
  margin-top: 10px;
  cursor: pointer;
  background: linear-gradient(120deg, var(--accent), #4de1ff);
  color: #06231c;
  border: none;
  font-weight: 700;
}
.hint { color: var(--accent-2); font-family: 'JetBrains Mono', monospace; font-size: 12px; }

.session-block { margin-top: 12px; }
.session-title { color: var(--muted); margin-bottom: 8px; }
.session-actions { display: grid; grid-template-columns: 1fr 72px; gap: 8px; }
.session-list { list-style: none; padding: 0; margin: 10px 0 0; max-height: 220px; overflow: auto; }
.session-list li { display: grid; grid-template-columns: 1fr 34px; gap: 8px; padding: 8px; border: 1px solid #293247; border-radius: 10px; margin-bottom: 6px; }
.session-list li.active { border-color: #2ed3a8; background: #12261f; }
.session-list .name { cursor: pointer; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.session-list .del { margin: 0; padding: 0; font-size: 12px; background: #5a2130; color: #ffd7de; }

.chat-panel { display: grid; grid-template-rows: 1fr auto; min-height: 0; }
.chat-log { overflow: auto; padding-right: 8px; }
.msg { margin-bottom: 14px; }
.role { color: var(--muted); margin-bottom: 6px; font-size: 12px; }
.content {
  border: 1px solid #273041;
  border-radius: 12px;
  padding: 12px;
  line-height: 1.7;
  word-break: break-word;
  background: #111620;
}
.msg.user .content { border-color: #24524a; background: #10241f; }
.composer { display: grid; grid-template-columns: 1fr 120px; gap: 8px; margin-top: 12px; }
textarea { min-height: 80px; resize: vertical; }

:deep(.pt-code) {
  background: #0c1219;
  border: 1px solid #2b3752;
  padding: 12px;
  border-radius: 10px;
  overflow: auto;
}
</style>
