import JSZip from 'jszip'

function parseXml(str)  { return new DOMParser().parseFromString(str, 'application/xml') }
function parseHtml(str) { return new DOMParser().parseFromString(str, 'text/html') }

function resolvePath(base, relative) {
  if (/^https?:\/\//.test(relative)) return relative
  const parts = (base || '').split('/')
  parts.pop()
  for (const seg of relative.split('/')) {
    if (seg === '..') parts.pop()
    else if (seg !== '.') parts.push(seg)
  }
  return parts.join('/')
}

async function zipText(zip, path) {
  const f = zip.file(path) || zip.file(decodeURIComponent(path))
  return f ? f.async('string') : null
}

async function zipB64(zip, path) {
  const f = zip.file(path) || zip.file(decodeURIComponent(path))
  return f ? f.async('base64') : null
}

const EXT_MIME = {
  jpg: 'image/jpeg', jpeg: 'image/jpeg', png: 'image/png',
  gif: 'image/gif', svg: 'image/svg+xml', webp: 'image/webp',
}

function extMime(path) {
  return EXT_MIME[(path.split('.').pop() || '').toLowerCase()] || 'image/jpeg'
}

function countAncestors(el, tag) {
  let n = 0, p = el.parentElement
  while (p) { if (p.tagName === tag) n++; p = p.parentElement }
  return n
}

function parseTocNcx(xml) {
  const doc = parseXml(xml)
  const entries = []
  doc.querySelectorAll('navPoint').forEach(np => {
    const title = np.querySelector('navLabel > text')?.textContent?.trim()
    const src   = np.querySelector('content')?.getAttribute('src')
    if (title && src) entries.push({ title, src, level: countAncestors(np, 'navPoint') })
  })
  return entries
}

function parseTocNav(html) {
  const doc = parseHtml(html)
  const entries = []
  doc.querySelectorAll('nav[epub\\:type="toc"] a, nav.toc a, nav a').forEach(a => {
    const title = a.textContent.trim()
    const src   = a.getAttribute('href')
    if (title && src) entries.push({ title, src, level: 0 })
  })
  return entries
}

function processHtml(html, chapterHref, imageMap) {
  const doc = parseHtml(html)
  doc.querySelectorAll('script, style, link[rel="stylesheet"], meta').forEach(el => el.remove())

  doc.querySelectorAll('img, image').forEach(img => {
    const src = img.getAttribute('src') || img.getAttribute('href') || img.getAttribute('xlink:href') || ''
    if (!src) return
    const resolved = resolvePath(chapterHref, src)
    const b64 = imageMap[resolved] || imageMap[src] || imageMap[src.split('/').pop()]
    if (b64) {
      img.setAttribute('src', b64)
      img.style.maxWidth  = '100%'
      img.style.height    = 'auto'
      img.style.display   = 'block'
      img.style.margin    = '0.6em auto'
    } else {
      img.remove()
    }
  })

  const body = doc.querySelector('body')
  return body ? body.innerHTML : ''
}

export async function parseEpub(file) {
  const zip = await JSZip.loadAsync(file)

  // 1. Find OPF path from container.xml
  const containerXml = await zipText(zip, 'META-INF/container.xml')
  if (!containerXml) throw new Error('Not a valid ePub (missing container.xml)')
  const opfPath = parseXml(containerXml).querySelector('rootfile')?.getAttribute('full-path')
  if (!opfPath) throw new Error('Not a valid ePub (cannot find OPF)')
  const opfDir  = opfPath.includes('/') ? opfPath.split('/').slice(0, -1).join('/') : ''

  // 2. Parse OPF manifest + spine + metadata
  const opfXml = await zipText(zip, opfPath)
  if (!opfXml) throw new Error('Cannot read OPF file')
  const opfDoc = parseXml(opfXml)

  const title   = opfDoc.querySelector('metadata > title, metadata > *|title')?.textContent?.trim()
    || file.name.replace(/\.epub$/i, '')
  const creator = opfDoc.querySelector('metadata > creator, metadata > *|creator')?.textContent?.trim()
    || 'Unknown'

  const manifest = {}
  opfDoc.querySelectorAll('manifest > item').forEach(el => {
    manifest[el.getAttribute('id')] = {
      href:      el.getAttribute('href'),
      mediaType: el.getAttribute('media-type') || '',
      props:     el.getAttribute('properties') || '',
    }
  })

  const spineIds = []
  opfDoc.querySelectorAll('spine > itemref').forEach(el => spineIds.push(el.getAttribute('idref')))

  // 3. Embed all images as base64 data URLs
  const imageMap = {}
  await Promise.all(Object.values(manifest).map(async item => {
    if (!item.mediaType.startsWith('image/')) return
    const absPath = opfDir ? `${opfDir}/${item.href}` : item.href
    try {
      const b64 = await zipB64(zip, absPath)
      if (b64) {
        const mime = item.mediaType || extMime(absPath)
        const dataUrl = `data:${mime};base64,${b64}`
        imageMap[absPath]  = dataUrl
        imageMap[item.href] = dataUrl
        imageMap[item.href.split('/').pop()] = dataUrl
      }
    } catch {}
  }))

  // 4. Get cover
  const coverId = Array.from(opfDoc.querySelectorAll('meta[name="cover"]'))
    .map(m => m.getAttribute('content'))[0]
  const coverItem = (coverId && manifest[coverId])
    || Object.values(manifest).find(m => m.props.includes('cover-image'))
    || Object.values(manifest).find(m => m.mediaType.startsWith('image/') && /cover/i.test(m.href))
  let coverBase64 = null
  if (coverItem) {
    const covPath = opfDir ? `${opfDir}/${coverItem.href}` : coverItem.href
    coverBase64 = imageMap[covPath] || imageMap[coverItem.href] || null
  }

  // 5. Parse TOC
  const navItem = Object.values(manifest).find(m => m.props.includes('nav'))
  const ncxItem = Object.values(manifest).find(m => m.mediaType === 'application/x-dtbncx+xml')
  let rawToc = []
  if (navItem) {
    const navPath = opfDir ? `${opfDir}/${navItem.href}` : navItem.href
    const navHtml = await zipText(zip, navPath)
    if (navHtml) rawToc = parseTocNav(navHtml)
  } else if (ncxItem) {
    const ncxPath = opfDir ? `${opfDir}/${ncxItem.href}` : ncxItem.href
    const ncxXml  = await zipText(zip, ncxPath)
    if (ncxXml) rawToc = parseTocNcx(ncxXml)
  }

  // 6. Extract and process each chapter; build id-mapped HTML
  const chapterIds   = []  // 'ch-0', 'ch-1', ...
  const chapterHtmls = []
  for (let i = 0; i < spineIds.length; i++) {
    const item = manifest[spineIds[i]]
    if (!item || !item.mediaType.includes('html') && !item.mediaType.includes('xhtml')) continue
    const absPath = opfDir ? `${opfDir}/${item.href}` : item.href
    const raw = await zipText(zip, absPath)
    if (!raw) continue
    const inner = processHtml(raw, absPath, imageMap)
    const chId  = `ch-${i}`
    chapterIds.push(chId)
    chapterHtmls.push(`<div class="epub-chapter" id="${chId}">${inner}</div>`)
  }
  const combinedHtml = chapterHtmls.join('\n')

  // 7. Map TOC entries to chapter anchors
  const toc = rawToc.map((entry, idx) => {
    const srcFile = entry.src.split('#')[0].split('/').pop()
    // Find which chapter href matches
    const chIdx = spineIds.findIndex(id => {
      const h = (manifest[id]?.href || '').split('/').pop()
      return h === srcFile
    })
    const anchorId = chIdx >= 0 ? `ch-${chIdx}` : (chapterIds[0] || 'ch-0')
    return { title: entry.title, anchorId, level: entry.level || 0 }
  })

  return { title, authors: [{ name: creator }], toc, content: combinedHtml, contentFormat: 'html', coverBase64 }
}

// Auto-detect chapters in plain Gutenberg text
export function detectChapters(text) {
  const lines = text.split('\n')
  const toc   = []
  let offset  = 0
  const RE    = /^(CHAPTER\s+[\w]+|Chapter\s+[\w]+|PART\s+[\w]+|Book\s+[\w]+)\s*[.:—]?\s*(.{0,60})$/

  for (const line of lines) {
    const trimmed = line.trim()
    if (RE.test(trimmed)) {
      const match = RE.exec(trimmed)
      const label = (match[2]?.trim() ? `${match[1]} — ${match[2]}` : match[1]).slice(0, 60)
      // Encode offset as pseudo-anchor id using base36
      toc.push({ title: label, anchorId: null, charOffset: offset })
    }
    offset += line.length + 1
  }
  return toc
}
