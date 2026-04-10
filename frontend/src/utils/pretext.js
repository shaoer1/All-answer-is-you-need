const escapeHtml = (s) =>
  s
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')

export function renderPretext(raw) {
  const lines = raw.split('\n')
  const out = []
  let inCode = false

  for (const line of lines) {
    if (line.startsWith('```')) {
      inCode = !inCode
      out.push(inCode ? '<pre class="pt-code"><code>' : '</code></pre>')
      continue
    }
    if (inCode) {
      out.push(escapeHtml(line) + '\n')
      continue
    }

    if (line.startsWith('### ')) {
      out.push(`<h3>${escapeHtml(line.slice(4))}</h3>`)
    } else if (line.startsWith('## ')) {
      out.push(`<h2>${escapeHtml(line.slice(3))}</h2>`)
    } else if (line.startsWith('# ')) {
      out.push(`<h1>${escapeHtml(line.slice(2))}</h1>`)
    } else if (line.startsWith('- ')) {
      out.push(`<li>${escapeHtml(line.slice(2))}</li>`)
    } else if (line.trim() === '') {
      out.push('<br/>')
    } else {
      out.push(`<p>${escapeHtml(line)}</p>`)
    }
  }

  return out.join('')
}

function wrapPlainLine(line, maxChars) {
  if (!line || line.length <= maxChars) return [line]
  const chunks = []
  let rest = line
  while (rest.length > maxChars) {
    let cut = rest.lastIndexOf(' ', maxChars)
    if (cut <= 0) cut = maxChars
    chunks.push(rest.slice(0, cut))
    rest = rest.slice(cut).trimStart()
  }
  if (rest.length) chunks.push(rest)
  return chunks
}

export function renderPretextWithLayout(raw, widthPx = 420) {
  if (!raw) return ''
  const safeWidth = Math.max(220, Number(widthPx) || 420)
  const maxChars = Math.max(12, Math.floor(safeWidth / 9))
  const lines = raw.split('\n')
  const wrapped = []
  let inCode = false

  for (const line of lines) {
    if (line.startsWith('```')) {
      inCode = !inCode
      wrapped.push(line)
      continue
    }
    if (inCode || line.startsWith('#') || line.startsWith('- ')) {
      wrapped.push(line)
      continue
    }
    if (line.trim() === '') {
      wrapped.push('')
      continue
    }
    wrapped.push(...wrapPlainLine(line, maxChars))
  }

  return renderPretext(wrapped.join('\n'))
}
