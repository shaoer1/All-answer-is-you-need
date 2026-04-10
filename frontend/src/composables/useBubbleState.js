import { reactive } from 'vue'

export function useBubbleState({ pairedMessagesAll, sessionId, username, listBubbleStates, saveBubbleStates, renderPretextWithLayout }) {
  const bubbleStates = reactive({})
  const hiddenBubbles = reactive({})
  const dragMeta = reactive({
    id: null,
    startX: 0,
    startY: 0,
    baseX: 0,
    baseY: 0,
    baseWidth: 560,
    mode: 'move'
  })
  const htmlLayoutCache = new Map()
  const COLLAPSE_WIDTH = 290
  let persistBubbleTimer = 0
  let zCounter = 2

  const ensureBubbleState = (pairId) => {
    if (!bubbleStates[pairId]) {
      const idx = pairedMessagesAll.value.findIndex((p) => p.id === pairId)
      const defaultY = idx >= 0 ? idx * 280 : 0
      bubbleStates[pairId] = { x: 0, y: defaultY, width: 560, lastLayoutWidth: 560, z: 1 }
    }
    return bubbleStates[pairId]
  }

  const isBubbleCollapsed = (pairId) => {
    const st = bubbleStates[pairId]
    if (!st) return false
    return st.width <= COLLAPSE_WIDTH
  }

  const closeBubble = (pairId) => {
    hiddenBubbles[pairId] = true
    schedulePersistBubbleStates()
  }

  const getBubbleStyle = (pairId) => {
    const st = bubbleStates[pairId] || { x: 0, y: 0, width: 560, z: 1 }
    return {
      position: 'absolute',
      left: '0px',
      top: '0px',
      width: `${st.width}px`,
      transform: `translate3d(${st.x}px, ${st.y}px, 0)`,
      zIndex: st.z || 1
    }
  }

  const renderBubbleHtml = (raw, pairId) => {
    const st = ensureBubbleState(pairId)
    const roundedWidth = Math.round(Math.max(220, st.width - 48))
    const lastWidth = st.lastLayoutWidth || roundedWidth
    const shouldRelayout = Math.abs(roundedWidth - lastWidth) >= 14 || !dragMeta.id
    const layoutWidth = shouldRelayout ? roundedWidth : lastWidth
    st.lastLayoutWidth = layoutWidth

    const key = `${pairId}:${layoutWidth}:${raw || ''}`
    if (htmlLayoutCache.has(key)) return htmlLayoutCache.get(key)
    const html = renderPretextWithLayout(raw || '', layoutWidth)
    htmlLayoutCache.set(key, html)
    if (htmlLayoutCache.size > 1200) htmlLayoutCache.clear()
    return html
  }

  const bubbleSortTime = (pair) => {
    const t = pair.answer?.createdAt || pair.question?.createdAt
    if (!t) return 0
    const ms = new Date(t).getTime()
    return Number.isFinite(ms) ? ms : 0
  }

  const persistBubbleStates = async () => {
    if (!sessionId.value) return
    const states = pairedMessagesAll.value.map((p) => {
      const st = bubbleStates[p.id] || { x: 0, y: 0, width: 560 }
      return {
        pairId: p.id,
        x: st.x || 0,
        y: st.y || 0,
        width: st.width || 560,
        hidden: !!hiddenBubbles[p.id]
      }
    })
    try {
      await saveBubbleStates({ username: username.value, sessionId: sessionId.value, states })
    } catch (e) {
      console.error('保存气泡状态失败:', e)
    }
  }

  const schedulePersistBubbleStates = () => {
    if (!sessionId.value) return
    if (persistBubbleTimer) clearTimeout(persistBubbleTimer)
    persistBubbleTimer = setTimeout(() => {
      persistBubbleStates()
    }, 280)
  }

  const applySavedBubbleStates = async () => {
    if (!sessionId.value) return
    try {
      const states = await listBubbleStates({ username: username.value, sessionId: sessionId.value })
      for (const st of states || []) {
        if (!st.pairId) continue
        ensureBubbleState(st.pairId)
        bubbleStates[st.pairId].x = Number(st.x || 0)
        bubbleStates[st.pairId].y = Number(st.y || 0)
        bubbleStates[st.pairId].width = Math.max(190, Number(st.width || 560))
        hiddenBubbles[st.pairId] = !!st.hidden
      }
    } catch (e) {
      console.error('加载气泡状态失败:', e)
    }
  }

  const onBubblePointerDown = (e, pairId, mode = 'move') => {
    if (mode === 'move') {
      const noDrag = e.target.closest('.trace-toggle, .el-button, .qa-bubble-resizer, .qa-close-btn')
      if (noDrag) return
    }
    const st = bubbleStates[pairId]
    if (!st) return
    dragMeta.id = pairId
    dragMeta.mode = mode
    dragMeta.startX = e.clientX
    dragMeta.startY = e.clientY
    dragMeta.baseX = st.x
    dragMeta.baseY = st.y
    dragMeta.baseWidth = st.width
    st.z = ++zCounter
    e.target.setPointerCapture?.(e.pointerId)
  }

  const onBubblePointerMove = (e) => {
    if (!dragMeta.id) return
    const st = bubbleStates[dragMeta.id]
    if (!st) return
    const dx = e.clientX - dragMeta.startX
    const dy = e.clientY - dragMeta.startY
    if (dragMeta.mode === 'move') {
      st.x = dragMeta.baseX + dx
      st.y = dragMeta.baseY + dy
    } else {
      st.width = Math.max(190, dragMeta.baseWidth + dx)
      if (isBubbleCollapsed(dragMeta.id)) {
        hiddenBubbles[dragMeta.id] = false
      }
    }
    schedulePersistBubbleStates()
  }

  const onBubblePointerUp = () => {
    dragMeta.id = null
    schedulePersistBubbleStates()
  }

  const resetBubbleRuntimeState = () => {
    for (const key of Object.keys(hiddenBubbles)) delete hiddenBubbles[key]
    for (const key of Object.keys(bubbleStates)) delete bubbleStates[key]
    htmlLayoutCache.clear()
    zCounter = 2
  }

  const clearPersistTimer = () => {
    if (persistBubbleTimer) clearTimeout(persistBubbleTimer)
  }

  return {
    bubbleStates,
    hiddenBubbles,
    dragMeta,
    ensureBubbleState,
    isBubbleCollapsed,
    closeBubble,
    getBubbleStyle,
    renderBubbleHtml,
    bubbleSortTime,
    schedulePersistBubbleStates,
    applySavedBubbleStates,
    onBubblePointerDown,
    onBubblePointerMove,
    onBubblePointerUp,
    resetBubbleRuntimeState,
    clearPersistTimer
  }
}
