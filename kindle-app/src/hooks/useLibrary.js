import { useState, useEffect, useCallback } from 'react'

const STORAGE_KEY = 'tome_library'

function load() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
  } catch {
    return {}
  }
}

function save(data) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
}

export function useLibrary() {
  const [library, setLibrary] = useState(load)

  const updateLibrary = useCallback((updater) => {
    setLibrary(prev => {
      const next = updater(prev)
      save(next)
      return next
    })
  }, [])

  const addBook = useCallback((book) => {
    updateLibrary(prev => ({
      ...prev,
      [book.id]: { ...book, addedAt: Date.now(), progress: prev[book.id]?.progress || 0 }
    }))
  }, [updateLibrary])

  const removeBook = useCallback((bookId) => {
    updateLibrary(prev => {
      const next = { ...prev }
      delete next[bookId]
      return next
    })
  }, [updateLibrary])

  const hasBook = useCallback((bookId) => !!library[bookId], [library])

  const setProgress = useCallback((bookId, progress) => {
    updateLibrary(prev => ({
      ...prev,
      [bookId]: { ...prev[bookId], progress: Math.max(0, Math.min(100, progress)) }
    }))
  }, [updateLibrary])

  const getProgress = useCallback((bookId) => library[bookId]?.progress || 0, [library])

  const books = Object.values(library).sort((a, b) => (b.addedAt || 0) - (a.addedAt || 0))

  return { books, addBook, removeBook, hasBook, setProgress, getProgress }
}
