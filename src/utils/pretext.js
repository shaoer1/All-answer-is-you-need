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
