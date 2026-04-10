import { computed, ref } from 'vue'

export function useClosedBubbleRestore({ pairedMessagesAll, hiddenBubbles, bubbleSortTime, schedulePersistBubbleStates }) {
  const selectedClosedPairIds = ref([])

  const closedBubbleOptions = computed(() =>
    pairedMessagesAll.value
      .filter((p) => hiddenBubbles[p.id])
      .sort((a, b) => bubbleSortTime(b) - bubbleSortTime(a))
      .map((p) => {
        const text = (p.question?.raw || '').replace(/\s+/g, ' ').trim()
        const label = text.length > 46 ? `${text.slice(0, 46)}...` : (text || p.id)
        return { id: p.id, label, time: bubbleSortTime(p) }
      })
  )

  const toggleClosedBubbleSelection = (pairId) => {
    const idx = selectedClosedPairIds.value.indexOf(pairId)
    if (idx >= 0) {
      selectedClosedPairIds.value.splice(idx, 1)
    } else {
      selectedClosedPairIds.value.push(pairId)
    }
  }

  const clearClosedBubbleSelection = () => {
    selectedClosedPairIds.value = []
  }

  const restoreSelectedClosedBubbles = () => {
    if (selectedClosedPairIds.value.length === 0) return
    for (const pairId of selectedClosedPairIds.value) {
      hiddenBubbles[pairId] = false
    }
    clearClosedBubbleSelection()
    schedulePersistBubbleStates()
  }

  return {
    selectedClosedPairIds,
    closedBubbleOptions,
    toggleClosedBubbleSelection,
    clearClosedBubbleSelection,
    restoreSelectedClosedBubbles
  }
}
