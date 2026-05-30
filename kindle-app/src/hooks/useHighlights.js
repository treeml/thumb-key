import { useState, useCallback } from 'react'

const KEY = (bookId) => `tome_highlights_${bookId}`

function load(bookId) {
  try {
    return JSON.parse(localStorage.getItem(KEY(bookId)) || '[]')
  } catch {
    return []
  }
}

export function useHighlights(bookId) {
  const [highlights, setHighlights] = useState(() => load(bookId))

  const addHighlight = useCallback((text, pageIndex, color = '#FFD700') => {
    setHighlights(prev => {
      const next = [...prev, { id: Date.now(), text, pageIndex, color, createdAt: Date.now() }]
      localStorage.setItem(KEY(bookId), JSON.stringify(next))
      return next
    })
  }, [bookId])

  const removeHighlight = useCallback((id) => {
    setHighlights(prev => {
      const next = prev.filter(h => h.id !== id)
      localStorage.setItem(KEY(bookId), JSON.stringify(next))
      return next
    })
  }, [bookId])

  const getPageHighlights = useCallback((pageIndex) =>
    highlights.filter(h => h.pageIndex === pageIndex),
  [highlights])

  return { highlights, addHighlight, removeHighlight, getPageHighlights }
}
