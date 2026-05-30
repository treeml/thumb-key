import { CapacitorHttp, Capacitor } from '@capacitor/core'

const GUTENDEX = 'https://gutendex.com'
const DICT_API = 'https://api.dictionaryapi.dev/api/v2/entries/en'

function xhrGet(url) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('GET', url, true)
    xhr.timeout = 15000
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) resolve(xhr.responseText)
      else reject(new Error(`XHR ${xhr.status}`))
    }
    xhr.onerror = () => reject(new Error('XHR network error'))
    xhr.ontimeout = () => reject(new Error('XHR timeout'))
    xhr.send()
  })
}

async function getJson(url) {
  if (Capacitor.isNativePlatform()) {
    // Try CapacitorHttp first (native Android HTTP, no CORS)
    try {
      const res = await CapacitorHttp.get({ url })
      if (res.status < 200 || res.status >= 300) throw new Error(`HTTP ${res.status}`)
      return typeof res.data === 'string' ? JSON.parse(res.data) : res.data
    } catch (capErr) {
      // Fall back to XHR
      try {
        const text = await xhrGet(url)
        return JSON.parse(text)
      } catch (xhrErr) {
        // Throw combined error for diagnostics
        const capMsg = capErr?.message || JSON.stringify(capErr) || 'no-msg'
        const xhrMsg = xhrErr?.message || 'no-msg'
        throw new Error(`cap: ${capMsg} | xhr: ${xhrMsg}`)
      }
    }
  }
  // Web fallback
  const res = await fetch(url)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}

async function getText(url) {
  if (Capacitor.isNativePlatform()) {
    try {
      const res = await CapacitorHttp.get({ url })
      if (res.status < 200 || res.status >= 300) throw new Error(`HTTP ${res.status}`)
      return typeof res.data === 'string' ? res.data : JSON.stringify(res.data)
    } catch {
      return xhrGet(url)
    }
  }
  const res = await fetch(url)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.text()
}

export async function searchBooks(query, page = 1) {
  const url = query
    ? `${GUTENDEX}/books/?search=${encodeURIComponent(query)}&page=${page}`
    : `${GUTENDEX}/books/?sort=popular&page=${page}`
  return getJson(url)
}

export async function fetchBooksBySubject(subject, page = 1) {
  return getJson(`${GUTENDEX}/books/?topic=${encodeURIComponent(subject)}&sort=popular&page=${page}`)
}

export async function fetchBookText(book) {
  const formats = book.formats || {}
  const textUrl =
    formats['text/plain; charset=utf-8'] ||
    formats['text/plain; charset=us-ascii'] ||
    formats['text/plain'] ||
    Object.entries(formats).find(([k]) => k.startsWith('text/plain'))?.[1]
  if (!textUrl) throw new Error('No plain text available')
  const text = await getText(textUrl)
  return cleanGutenbergText(text)
}

function cleanGutenbergText(text) {
  let start = 0, end = text.length
  const sm = text.search(/\*\*\* START OF (THE|THIS) PROJECT GUTENBERG/i)
  if (sm !== -1) start = text.indexOf('\n', sm) + 1
  const em = text.search(/\*\*\* END OF (THE|THIS) PROJECT GUTENBERG/i)
  if (em !== -1 && em > start) end = em
  return text.slice(start, end).trim()
}

export async function lookupWord(word) {
  const clean = word.toLowerCase().replace(/[^a-z'-]/g, '')
  if (!clean) throw new Error('Invalid word')
  return getJson(`${DICT_API}/${encodeURIComponent(clean)}`)
}

export function getBookCoverUrl(book) { return book.formats?.['image/jpeg'] || null }
export function getBookAuthors(book) {
  if (!book.authors?.length) return 'Unknown Author'
  return book.authors.map(a => a.name).join(', ')
}
export function getBookGenres(book) { return book.subjects?.slice(0, 3) || [] }
export function getBookColors(id) {
  const palettes = [
    { spine: '#8B2635' }, { spine: '#1B4F72' }, { spine: '#1E8449' },
    { spine: '#784212' }, { spine: '#4A235A' }, { spine: '#1A5276' },
    { spine: '#922B21' }, { spine: '#1E6251' }, { spine: '#5D4037' },
    { spine: '#283593' }, { spine: '#BF360C' }, { spine: '#006064' },
  ]
  return palettes[id % palettes.length]
}
