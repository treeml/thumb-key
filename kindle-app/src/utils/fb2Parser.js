/**
 * Parse a FictionBook 2 (.fb2) file into the same shape as parseEpub().
 * FB2 is a well-formed XML format, so we use DOMParser directly.
 */
export async function parseFb2(file) {
  const text = await file.text()
  const doc  = new DOMParser().parseFromString(text, 'application/xml')

  if (doc.querySelector('parsererror')) {
    // Some FB2 files declare a non-UTF-8 encoding in the XML header;
    // fall back to latin-1 read.
    const buf  = await file.arrayBuffer()
    const dec  = new TextDecoder('windows-1251')
    const doc2 = new DOMParser().parseFromString(dec.decode(buf), 'application/xml')
    if (doc2.querySelector('parsererror')) throw new Error('Invalid or unreadable FB2 file')
    return _parseFb2Doc(doc2, file.name)
  }
  return _parseFb2Doc(doc, file.name)
}

function _parseFb2Doc(doc, fileName) {
  const qs  = (parent, sel) => parent?.querySelector(sel)
  const txt = (parent, sel) => qs(parent, sel)?.textContent?.trim() || ''

  // ── Metadata ───────────────────────────────────────────────────────────────
  const titleInfo = qs(doc, 'title-info')
  const bookTitle = txt(titleInfo, 'book-title') || fileName.replace(/\.fb2$/i, '')

  const authorEl  = qs(titleInfo, 'author')
  const authorName = [
    txt(authorEl, 'first-name'),
    txt(authorEl, 'middle-name'),
    txt(authorEl, 'last-name'),
  ].filter(Boolean).join(' ') || 'Unknown'

  // ── Binary images → data URLs ──────────────────────────────────────────────
  const binMap = {}   // '#id' and 'id' → data:... URL
  doc.querySelectorAll('binary').forEach(bin => {
    const id   = bin.getAttribute('id') || ''
    const mime = bin.getAttribute('content-type') || 'image/jpeg'
    const b64  = bin.textContent.replace(/\s+/g, '')
    if (id && b64) {
      const url      = `data:${mime};base64,${b64}`
      binMap[id]     = url
      binMap['#' + id] = url
    }
  })

  // ── Cover ──────────────────────────────────────────────────────────────────
  let coverBase64 = null
  const coverImg = qs(titleInfo, 'coverpage image')
  if (coverImg) {
    const href = coverImg.getAttribute('l:href') || coverImg.getAttribute('href') || ''
    coverBase64 = binMap[href] || binMap[href.replace(/^#/, '')] || null
  }

  // ── Assign stable IDs to top-level sections (for TOC anchors) ─────────────
  const body = qs(doc, 'body')
  if (!body) throw new Error('FB2 file has no <body>')

  let secIdx = 0
  body.querySelectorAll('section').forEach(sec => {
    if (!sec.getAttribute('id')) sec.setAttribute('id', `s${secIdx}`)
    secIdx++
  })

  // ── Build TOC from top-two-level section titles ────────────────────────────
  const toc = []
  body.querySelectorAll('body > section, body > section > section').forEach(sec => {
    const titleEl = sec.querySelector(':scope > title')
    if (!titleEl) return
    const level  = sec.parentElement.localName === 'body' ? 0 : 1
    const label  = titleEl.textContent.replace(/\s+/g, ' ').trim().slice(0, 80)
    toc.push({ title: label, anchorId: sec.getAttribute('id'), level })
  })

  // ── Convert FB2 XML → HTML ─────────────────────────────────────────────────
  function node2html(n) {
    if (n.nodeType === 3) return n.textContent          // text node
    if (n.nodeType !== 1) return ''                     // skip comments etc.
    const tag      = n.localName
    const children = () => Array.from(n.childNodes).map(node2html).join('')

    switch (tag) {
      case 'body':
        return children()

      case 'section': {
        const id = n.getAttribute('id') || ''
        return `<div class="fb2-section"${id ? ` id="${id}"` : ''}>${children()}</div>`
      }
      case 'title':
        // Depth-aware heading: body>section = h2, deeper = h3
        return n.parentElement?.parentElement?.localName === 'body'
          ? `<h2 class="fb2-title">${children()}</h2>`
          : `<h3 class="fb2-title">${children()}</h3>`

      case 'subtitle':   return `<h4 class="fb2-subtitle">${children()}</h4>`
      case 'p':          return `<p>${children()}</p>`
      case 'empty-line': return '<p class="fb2-empty"> </p>'
      case 'strong':     return `<strong>${children()}</strong>`
      case 'emphasis':   return `<em>${children()}</em>`
      case 'strikethrough': return `<s>${children()}</s>`
      case 'code':       return `<code>${children()}</code>`
      case 'a':          return children()              // strip links, keep text
      case 'sup':        return `<sup>${children()}</sup>`
      case 'sub':        return `<sub>${children()}</sub>`

      case 'image': {
        const href = n.getAttribute('l:href') || n.getAttribute('href') || ''
        const src  = binMap[href] || ''
        return src
          ? `<img src="${src}" style="max-width:100%;height:auto;display:block;margin:0.8em auto"/>`
          : ''
      }
      case 'epigraph':
        return `<blockquote class="fb2-epigraph">${children()}</blockquote>`
      case 'cite':
        return `<blockquote>${children()}</blockquote>`
      case 'poem':
        return `<div class="fb2-poem">${children()}</div>`
      case 'stanza':
        return `<div class="fb2-stanza">${children()}</div>`
      case 'v':
        return `<div class="fb2-v">${children()}</div>`
      case 'text-author':
        return `<div class="fb2-text-author">${children()}</div>`
      case 'annotation':
        return `<aside class="fb2-annotation">${children()}</aside>`
      case 'table':      return `<table>${children()}</table>`
      case 'tr':         return `<tr>${children()}</tr>`
      case 'th':         return `<th>${children()}</th>`
      case 'td':         return `<td>${children()}</td>`

      default:           return children()
    }
  }

  const content = Array.from(body.childNodes).map(node2html).join('\n')

  return {
    title:         bookTitle,
    authors:       [{ name: authorName }],
    toc,
    content,
    contentFormat: 'html',
    coverBase64,
  }
}
