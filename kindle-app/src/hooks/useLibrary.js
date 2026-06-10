import { useState, useCallback, useMemo } from 'react'

const STORAGE_KEY = 'tome_library'

function load() {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}') }
  catch { return {} }
}

function save(data) { localStorage.setItem(STORAGE_KEY, JSON.stringify(data)) }

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
    updateLibrary(prev => { const n = { ...prev }; delete n[bookId]; return n })
  }, [updateLibrary])

  const hasBook = useCallback((bookId) => !!library[bookId], [library])

  const setProgress = useCallback((bookId, progress) => {
    updateLibrary(prev => ({
      ...prev,
      [bookId]: {
        ...prev[bookId],
        progress: Math.max(0, Math.min(100, progress)),
        lastRead: Date.now(),
        shelved:  false,  // un-shelve on next read
      }
    }))
  }, [updateLibrary])

  const getProgress = useCallback((bookId) => library[bookId]?.progress || 0, [library])

  // Hides a book from the "Reading Now" shelf without removing it from library
  const shelveBook = useCallback((bookId) => {
    updateLibrary(prev => ({
      ...prev,
      [bookId]: { ...prev[bookId], shelved: true }
    }))
  }, [updateLibrary])

  const books = useMemo(
    () => Object.values(library).sort((a, b) => (b.addedAt || 0) - (a.addedAt || 0)),
    [library]
  )

  const readingNow = useMemo(
    () => Object.values(library)
      .filter(b => (b.progress || 0) > 0 && (b.progress || 0) < 100 && !b.shelved)
      .sort((a, b) => (b.lastRead || b.addedAt || 0) - (a.lastRead || a.addedAt || 0))
      .slice(0, 4),
    [library]
  )

  return { books, readingNow, addBook, removeBook, hasBook, setProgress, getProgress, shelveBook }
}
