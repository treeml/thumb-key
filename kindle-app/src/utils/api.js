const GUTENDEX = 'https://gutendex.com'
const DICT_API = 'https://api.dictionaryapi.dev/api/v2/entries/en'

export async function searchBooks(query, page = 1) {
  const url = query
    ? `${GUTENDEX}/books/?search=${encodeURIComponent(query)}&page=${page}`
    : `${GUTENDEX}/books/?sort=popular&page=${page}`
  const res = await fetch(url)
  if (!res.ok) throw new Error('Failed to fetch books')
  return res.json()
}

export async function getBook(id) {
  const res = await fetch(`${GUTENDEX}/books/${id}`)
  if (!res.ok) throw new Error('Failed to fetch book')
  return res.json()
}

export async function fetchBooksBySubject(subject, page = 1) {
  const res = await fetch(`${GUTENDEX}/books/?topic=${encodeURIComponent(subject)}&sort=popular&page=${page}`)
  if (!res.ok) throw new Error('Failed to fetch books by subject')
  return res.json()
}

export async function fetchBookText(book) {
  const formats = book.formats || {}
  const textUrl =
    formats['text/plain; charset=utf-8'] ||
    formats['text/plain; charset=us-ascii'] ||
    formats['text/plain'] ||
    Object.entries(formats).find(([k]) => k.startsWith('text/plain'))?.[1]

  if (!textUrl) throw new Error('No plain text available for this book')

  const proxyUrl = `https://corsproxy.io/?${encodeURIComponent(textUrl)}`
  const res = await fetch(proxyUrl)
  if (!res.ok) throw new Error('Failed to fetch book text')
  const text = await res.text()
  return cleanGutenbergText(text)
}

function cleanGutenbergText(text) {
  // Remove Gutenberg header/footer boilerplate
  const startMarkers = [
    /\*\*\* START OF (THE|THIS) PROJECT GUTENBERG/i,
    /\*\*\*START OF (THE|THIS) PROJECT GUTENBERG/i,
  ]
  const endMarkers = [
    /\*\*\* END OF (THE|THIS) PROJECT GUTENBERG/i,
    /\*\*\*END OF (THE|THIS) PROJECT GUTENBERG/i,
    /End of (the )?Project Gutenberg/i,
  ]

  let start = 0
  let end = text.length

  for (const marker of startMarkers) {
    const match = text.search(marker)
    if (match !== -1) {
      const lineEnd = text.indexOf('\n', match)
      start = lineEnd !== -1 ? lineEnd + 1 : match
      break
    }
  }

  for (const marker of endMarkers) {
    const match = text.search(marker)
    if (match !== -1 && match > start) {
      end = match
      break
    }
  }

  return text.slice(start, end).trim()
}

export async function lookupWord(word) {
  const clean = word.toLowerCase().replace(/[^a-z'-]/g, '')
  if (!clean) throw new Error('Invalid word')
  const res = await fetch(`${DICT_API}/${encodeURIComponent(clean)}`)
  if (!res.ok) throw new Error('Word not found')
  return res.json()
}

export function getBookCoverUrl(book) {
  return book.formats?.['image/jpeg'] || null
}

export function getBookAuthors(book) {
  if (!book.authors?.length) return 'Unknown Author'
  return book.authors.map(a => a.name).join(', ')
}

export function getBookGenres(book) {
  return book.subjects?.slice(0, 3) || []
}

// Generate a deterministic color palette for a book based on its ID
export function getBookColors(id) {
  const palettes = [
    { spine: '#8B2635', page: '#F5E6D3', text: '#2C1810' },
    { spine: '#1B4F72', page: '#EBF5FB', text: '#1B2631' },
    { spine: '#1E8449', page: '#EAFAF1', text: '#1B2631' },
    { spine: '#784212', page: '#FEF9E7', text: '#2C1810' },
    { spine: '#4A235A', page: '#F5EEF8', text: '#2C1810' },
    { spine: '#1A5276', page: '#EAF2F8', text: '#1B2631' },
    { spine: '#922B21', page: '#FDEDEC', text: '#2C1810' },
    { spine: '#1E6251', page: '#E8F8F5', text: '#1B2631' },
    { spine: '#5D4037', page: '#FBE9E7', text: '#2C1810' },
    { spine: '#283593', page: '#E8EAF6', text: '#1B2631' },
    { spine: '#BF360C', page: '#FBE9E7', text: '#2C1810' },
    { spine: '#006064', page: '#E0F7FA', text: '#1B2631' },
  ]
  return palettes[id % palettes.length]
}
