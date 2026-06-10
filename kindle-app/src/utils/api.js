import { CapacitorHttp, Capacitor } from '@capacitor/core'
import { idbGet } from './idb'

const GUTENDEX        = 'https://gutendex.com'
const DICT_API        = 'https://api.dictionaryapi.dev/api/v2/entries/en'
const OPEN_LIBRARY    = 'https://openlibrary.org'
const INTERNET_ARCHIVE = 'https://archive.org'


function isHtmlResponse(text) {
  const t = (text || '').trimStart()
  return t.startsWith('<!') || /^<html/i.test(t)
}

// Force HTTPS and normalise Gutenberg URLs
function normaliseUrl(url) {
  return url.replace(/^http:\/\//i, 'https://')
}

// Build fallback Gutenberg text URLs from a book ID
function gutenbergFallbackUrls(bookId) {
  const id = String(bookId)
  return [
    `https://www.gutenberg.org/cache/epub/${id}/pg${id}.txt`,
    `https://gutenberg.org/cache/epub/${id}/pg${id}.txt`,
    `https://www.gutenberg.org/files/${id}/${id}-0.txt`,
    `https://www.gutenberg.org/files/${id}/${id}.txt`,
  ]
}

async function capGet(url) {
  const res = await CapacitorHttp.get({
    url,
    headers: { 'User-Agent': 'Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36' },
    connectTimeout: 8000,
    readTimeout: 10000,
  })
  if (res.status < 200 || res.status >= 300) throw new Error(`HTTP ${res.status}`)
  return res.data
}

async function capGetText(url) {
  const res = await CapacitorHttp.get({
    url,
    headers: { 'User-Agent': 'Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36' },
    responseType: 'text',
    connectTimeout: 15000,
    readTimeout: 60000,
  })
  if (res.status < 200 || res.status >= 300) throw new Error(`HTTP ${res.status}`)
  const data = typeof res.data === 'string' ? res.data : String(res.data ?? '')
  if (isHtmlResponse(data)) throw new Error('Received HTML page instead of text file')
  return data
}

// WebView fetch with timeout — uses Chromium stack, passes CDN/Cloudflare bot checks
function webFetch(url, timeoutMs) {
  const controller = new AbortController()
  const id = setTimeout(() => controller.abort(), timeoutMs)
  return fetch(url, { signal: controller.signal }).finally(() => clearTimeout(id))
}

async function getJson(url) {
  const u = normaliseUrl(url)
  if (Capacitor.isNativePlatform()) {
    // Race WebView fetch and CapacitorHttp in parallel — whichever succeeds first wins.
    // fetch() passes Cloudflare/CDN bot checks; capGet bypasses CORS.
    // Neither waits for the other to fail, so latency = fastest responder.
    return new Promise((resolve, reject) => {
      let failed = 0
      const onFail = () => { if (++failed === 2) reject(new Error('Could not connect — check your internet and try again')) }
      webFetch(u, 12000)
        .then(r => { if (!r.ok) throw new Error(`HTTP ${r.status}`); return r.json() })
        .then(resolve).catch(onFail)
      capGet(u)
        .then(d => typeof d === 'string' ? JSON.parse(d) : d)
        .then(resolve).catch(onFail)
    })
  }
  const res = await fetch(url)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}

async function getText(url) {
  const u = normaliseUrl(url)
  if (Capacitor.isNativePlatform()) {
    // WebView fetch first — better for large files, passes bot checks
    try {
      const res = await webFetch(u, 45000)
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const text = await res.text()
      if (isHtmlResponse(text)) throw new Error('Got HTML page instead of book text')
      return text
    } catch (fetchErr) {
      try { return await capGetText(u) } catch { /* fall through */ }
      throw fetchErr
    }
  }
  const res = await fetch(u)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.text()
}

// Hash any ID (string or number) to a stable non-negative integer
export function hashId(id) {
  const s = String(id)
  let h = 0
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) | 0
  return Math.abs(h)
}

function normalizeOpenLibraryBook(doc) {
  const iaId    = Array.isArray(doc.ia) ? doc.ia[0] : (doc.ia || null)
  const coverId = doc.cover_i || null
  const workKey = doc.key?.replace('/works/', '') || `olbook${Date.now()}`
  return {
    id:            `ol_${workKey}`,
    title:         doc.title || 'Unknown Title',
    authors:       (doc.author_name || []).map(name => ({ name })),
    formats: {
      ...(coverId && { 'image/jpeg':   `https://covers.openlibrary.org/b/id/${coverId}-M.jpg` }),
      ...(iaId    && { 'text/plain':   `${INTERNET_ARCHIVE}/download/${iaId}/${iaId}.txt` }),
    },
    subjects:      (doc.subject || []).slice(0, 5),
    languages:     doc.language || [],
    download_count: doc.readinglog_count || 0,
    source:        'openlibrary',
    ia:            iaId,
  }
}

export async function searchBooks(query, page = 1) {
  const url = query
    ? `${GUTENDEX}/books/?search=${encodeURIComponent(query)}&page=${page}`
    : `${GUTENDEX}/books/?sort=popular&page=${page}`
  return getJson(url)
}

export async function searchBooksAll(query, page = 1) {
  if (!query?.trim()) return searchBooks(null, page)

  const [gutResult, olResult] = await Promise.allSettled([
    searchBooks(query, page),
    getJson(
      `${OPEN_LIBRARY}/search.json?q=${encodeURIComponent(query)}` +
      `&fields=key,title,author_name,cover_i,ia,has_fulltext,public_scan_b,subject,language,readinglog_count` +
      `&limit=20&has_fulltext=true`
    ),
  ])

  const gutBooks = gutResult.status === 'fulfilled'
    ? (gutResult.value.results || []).map(b => ({ ...b, source: 'gutenberg' }))
    : []

  const seenTitles = new Set(gutBooks.map(b => b.title.toLowerCase()))
  const olBooks = olResult.status === 'fulfilled'
    ? (olResult.value.docs || [])
        // public_scan_b=true means freely downloadable; without it the book requires IA login (CDL)
        .filter(doc => doc.has_fulltext && doc.ia && doc.public_scan_b === true)
        .map(normalizeOpenLibraryBook)
        .filter(b => !seenTitles.has(b.title.toLowerCase()))
    : []

  return {
    results: [...gutBooks, ...olBooks],
    count:   gutBooks.length + olBooks.length,
    sources: { gutenberg: gutBooks.length, openlibrary: olBooks.length },
  }
}

export async function fetchBooksBySubject(subject, page = 1) {
  return getJson(`${GUTENDEX}/books/?topic=${encodeURIComponent(subject)}&sort=popular&page=${page}`)
}

const BOOK_CACHE_PREFIX = 'tome_bk_'
const MAX_BOOK_CACHE = 8

function readBookCache(bookId) {
  try {
    const raw = localStorage.getItem(BOOK_CACHE_PREFIX + bookId)
    if (!raw) return null
    return JSON.parse(raw).text || null
  } catch { return null }
}

function writeBookCache(bookId, text) {
  try {
    // Evict oldest entries if over limit
    const entries = []
    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i)
      if (k?.startsWith(BOOK_CACHE_PREFIX)) {
        try { entries.push({ k, ts: JSON.parse(localStorage.getItem(k)).ts }) } catch {}
      }
    }
    entries.sort((a, b) => a.ts - b.ts)
    while (entries.length >= MAX_BOOK_CACHE) {
      localStorage.removeItem(entries.shift().k)
    }
    localStorage.setItem(BOOK_CACHE_PREFIX + bookId, JSON.stringify({ text, ts: Date.now() }))
  } catch {}
}

export async function fetchBookText(book) {
  // Local imports are stored in IndexedDB
  if (book.source === 'local') {
    const stored = await idbGet(book.id)
    if (stored?.content) return stored.content
    throw new Error('Book content not found — it may have been deleted from device storage.')
  }

  const cached = readBookCache(book.id)
  if (cached) return cached

  const formats = book.formats || {}

  if (book.source === 'openlibrary') {
    const iaId = book.ia
    const urlsToTry = [
      iaId && `${INTERNET_ARCHIVE}/download/${iaId}/${iaId}.txt`,
      iaId && `${INTERNET_ARCHIVE}/download/${iaId}/${iaId}_rawtext.txt`,
      iaId && `${INTERNET_ARCHIVE}/download/${iaId}/${iaId}_djvu.txt`,
    ].filter(Boolean).map(normaliseUrl)

    let lastErr
    for (const url of urlsToTry) {
      try {
        const text = await getText(url)
        if (text && text.length > 500) {
          writeBookCache(book.id, text.trim())
          return text.trim()
        }
      } catch (e) {
        lastErr = e
        if (e.message?.includes('401')) break  // CDL — auth required, no point retrying
      }
    }
    if (lastErr?.message?.includes('401')) {
      throw new Error('This book requires an Internet Archive account. Try searching for it by title — a free Gutenberg version may be available.')
    }
    throw lastErr || new Error('Text not available for this book on Internet Archive')
  }

  // Gutenberg: try direct cache URL first (no redirect), then Gutendex format URL, then other fallbacks
  const gutId = String(book.id)
  const apiUrl =
    formats['text/plain; charset=utf-8'] ||
    formats['text/plain; charset=us-ascii'] ||
    formats['text/plain'] ||
    Object.entries(formats).find(([k]) => k.startsWith('text/plain'))?.[1]

  const seen = new Set()
  const urlsToTry = [
    `https://www.gutenberg.org/cache/epub/${gutId}/pg${gutId}.txt`,
    apiUrl,
    `https://gutenberg.org/cache/epub/${gutId}/pg${gutId}.txt`,
    `https://www.gutenberg.org/files/${gutId}/${gutId}-0.txt`,
    `https://www.gutenberg.org/files/${gutId}/${gutId}.txt`,
  ].filter(u => u && !seen.has(u) && seen.add(u))

  let lastErr
  for (const url of urlsToTry) {
    try {
      const text = await getText(url)
      if (text && text.length > 500) {
        const clean = cleanGutenbergText(text)
        writeBookCache(book.id, clean)
        return clean
      }
    } catch (e) {
      lastErr = e
    }
  }
  throw lastErr || new Error('No readable text found for this book')
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
  const url = normaliseUrl(`${DICT_API}/${encodeURIComponent(clean)}`)

  if (Capacitor.isNativePlatform()) {
    // Use CapacitorHttp directly so we can distinguish 404 (word unknown) from network errors.
    // getJson's race treats all non-2xx the same; dictionary callers need to know the difference.
    const res = await CapacitorHttp.get({
      url,
      headers: { 'User-Agent': 'Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36' },
      connectTimeout: 8000,
      readTimeout: 10000,
    })
    if (res.status === 404) throw new Error('not_found')
    if (res.status < 200 || res.status >= 300) throw new Error(`HTTP ${res.status}`)
    return typeof res.data === 'string' ? JSON.parse(res.data) : res.data
  }

  const res = await fetch(url)
  if (res.status === 404) throw new Error('not_found')
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
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
  return palettes[hashId(id) % palettes.length]
}
