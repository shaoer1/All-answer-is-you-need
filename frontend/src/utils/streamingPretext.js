const escapeHtml = (s) =>
  s
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')

// 流式Pretext渲染器
class StreamingPretextRenderer {
  constructor() {
    this.buffer = ''
    this.inCode = false
    this.inList = false
    this.currentLine = ''
  }

  // 处理流式输入的文本片段
  process(chunk) {
    this.buffer += chunk
    const lines = this.buffer.split('\n')
    this.buffer = lines.pop() || ''

    let html = ''
    
    for (const line of lines) {
      html += this.processLine(line + '\n')
    }

    // 处理当前行的部分内容
    if (this.buffer) {
      html += this.processPartialLine(this.buffer)
    }

    return html
  }

  // 处理完整的一行
  processLine(line) {
    let html = ''

    if (line.startsWith('```')) {
      this.inCode = !this.inCode
      html += this.inCode ? '<pre class="pt-code"><code>' : '</code></pre>'
      return html
    }

    if (this.inCode) {
      html += escapeHtml(line)
      return html
    }

    if (line.startsWith('### ')) {
      html += `<h3>${escapeHtml(line.slice(4))}</h3>`
    } else if (line.startsWith('## ')) {
      html += `<h2>${escapeHtml(line.slice(3))}</h2>`
    } else if (line.startsWith('# ')) {
      html += `<h1>${escapeHtml(line.slice(2))}</h1>`
    } else if (line.startsWith('- ')) {
      if (!this.inList) {
        html += '<ul>'
        this.inList = true
      }
      html += `<li>${escapeHtml(line.slice(2))}</li>`
    } else if (line.trim() === '') {
      if (this.inList) {
        html += '</ul>'
        this.inList = false
      }
      html += '<br/>'
    } else {
      if (this.inList) {
        html += '</ul>'
        this.inList = false
      }
      html += `<p>${escapeHtml(line)}</p>`
    }

    return html
  }

  // 处理部分行（用于流式渲染）
  processPartialLine(partialLine) {
    let html = ''

    if (this.inCode) {
      html += escapeHtml(partialLine)
    } else if (partialLine.startsWith('```')) {
      // 处理代码块开始
      this.inCode = true
      html += '<pre class="pt-code"><code>'
    } else if (partialLine.startsWith('### ')) {
      // 处理三级标题
      html += `<h3>${escapeHtml(partialLine.slice(4))}</h3>`
    } else if (partialLine.startsWith('## ')) {
      // 处理二级标题
      html += `<h2>${escapeHtml(partialLine.slice(3))}</h2>`
    } else if (partialLine.startsWith('# ')) {
      // 处理一级标题
      html += `<h1>${escapeHtml(partialLine.slice(2))}</h1>`
    } else if (partialLine.startsWith('- ')) {
      // 处理列表项
      if (!this.inList) {
        html += '<ul>'
        this.inList = true
      }
      html += `<li>${escapeHtml(partialLine.slice(2))}</li>`
    } else {
      // 处理普通文本
      if (this.inList) {
        html += '</ul>'
        this.inList = false
      }
      html += `<p>${escapeHtml(partialLine)}</p>`
    }

    return html
  }

  // 完成流式处理，返回剩余内容
  finalize() {
    let html = ''

    // 处理剩余的缓冲区内容
    if (this.buffer) {
      html += this.processLine(this.buffer + '\n')
      this.buffer = ''
    }

    // 关闭未完成的标签
    if (this.inCode) {
      html += '</code></pre>'
      this.inCode = false
    }

    if (this.inList) {
      html += '</ul>'
      this.inList = false
    }

    return html
  }
}

export function createStreamingPretextRenderer() {
  return new StreamingPretextRenderer()
}
