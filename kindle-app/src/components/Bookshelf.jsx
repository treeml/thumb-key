import React, { useEffect, useState, useRef, useCallback } from 'react'
import { Capacitor } from '@capacitor/core'
import { searchBooks, fetchBooksBySubject } from '../utils/api'
import BookCard from './BookCard'

const CATEGORIES = [
  { label: 'Popular',    key: 'popular',    query: null },
  { label: 'Adventure',  key: 'adventure',  query: 'adventure' },
  { label: 'Mystery',    key: 'mystery',    query: 'detective' },
  { label: 'Romance',    key: 'romance',    query: 'romance' },
  { label: 'Sci-Fi',     key: 'scifi',      query: 'science fiction' },
  { label: 'Philosophy', key: 'philosophy', query: 'philosophy' },
  { label: 'History',    key: 'history',    query: 'history' },
  { label: 'Poetry',     key: 'poetry',     query: 'poetry' },
]

// Module-level cache — survives component unmount/remount
const sectionCache = {}

function ShelfRow({ title, books, onSelect, getProgress, loading, error, onRetry }) {
  const rowRef = useRef(null)
  const scroll = (dir) => {
    if (rowRef.current) rowRef.current.scrollBy({ left: dir * 260, behavior: 'smooth' })
  }

  return (
    <div className="shelf-row">
      <div className="shelf-row-header">
        <h2 className="shelf-row-title">{title}</h2>
        <div className="shelf-scroll-btns">
          <button className="shelf-scroll-btn" onClick={() => scroll(-1)}>‹</button>
          <button className="shelf-scroll-btn" onClick={() => scroll(1)}>›</button>
        </div>
      </div>
      <div className="shelf-track" ref={rowRef}>
        {loading
          ? Array.from({ length: 6 }).map((_, i) => <div key={i} className="book-card-skeleton" />)
          : error
          ? (
            <div className="shelf-error">
              <div>
                <div style={{fontSize:11,color:'#e07b39',marginBottom:4}}>Error: {error}</div>
              </div>
              <button className="retry-btn" onClick={onRetry}>Retry</button>
            </div>
          )
          : books.length === 0
          ? <div className="shelf-empty">No books found</div>
          : books.map(book => (
            <BookCard
              key={book.id}
              book={book}
              progress={getProgress(book.id)}
              onClick={onSelect}
            />
          ))
        }
      </div>
      <div className="shelf-plank" />
    </div>
  )
}

export default function Bookshelf({ onSelectBook, getProgress, myLibrary, searchQuery }) {
  const [sections, setSections] = useState(() => ({ ...sectionCache }))
  const [loading, setLoading] = useState({})
  const [errors, setErrors] = useState({})

  const fetchCategory = useCallback((cat) => {
    delete sectionCache[cat.key]
    setSections(p => { const n = { ...p }; delete n[cat.key]; return n })
    setErrors(p => ({ ...p, [cat.key]: false }))
    setLoading(p => ({ ...p, [cat.key]: true }))

    const fetcher = cat.query ? fetchBooksBySubject(cat.query) : searchBooks(null, 1)
    fetcher
      .then(data => {
        const results = data.results || []
        sectionCache[cat.key] = results
        setSections(p => ({ ...p, [cat.key]: results }))
        setErrors(p => ({ ...p, [cat.key]: false }))
      })
      .catch((e) => {
        setErrors(p => ({ ...p, [cat.key]: (e && e.message) ? e.message : String(e) }))
      })
      .finally(() => setLoading(p => ({ ...p, [cat.key]: false })))
  }, [])

  useEffect(() => {
    const timers = []
    CATEGORIES.forEach((cat, i) => {
      if (sectionCache[cat.key]) return  // already cached

      const t = setTimeout(() => fetchCategory(cat), i * 400)
      timers.push(t)
    })
    return () => timers.forEach(clearTimeout)
  }, [fetchCategory])

  const [searchResults, setSearchResults] = useState(null)
  const [searching, setSearching] = useState(false)
  const [searchError, setSearchError] = useState(false)

  useEffect(() => {
    if (!searchQuery?.trim()) { setSearchResults(null); setSearchError(false); return }
    setSearching(true)
    setSearchError(false)
    searchBooks(searchQuery)
      .then(d => setSearchResults(d.results || []))
      .catch(() => { setSearchResults([]); setSearchError(true) })
      .finally(() => setSearching(false))
  }, [searchQuery])

  if (searchResults !== null) {
    return (
      <div className="bookshelf-container">
        <div className="search-results-header">
          <h2>Results for "{searchQuery}"</h2>
          <span className="result-count">{searchResults.length} books</span>
        </div>
        {searchError && <div className="search-error-msg">Connection error — check your internet and try again.</div>}
        <div className="search-results-grid">
          {searching
            ? Array.from({ length: 8 }).map((_, i) => <div key={i} className="book-card-skeleton" />)
            : searchResults.map(book => (
              <BookCard key={book.id} book={book} progress={getProgress(book.id)} onClick={onSelectBook} />
            ))
          }
          {!searching && !searchError && searchResults.length === 0 && (
            <div className="empty-state">No books found. Try another search.</div>
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="bookshelf-container">
      <div style={{fontSize:10,color:'#6a5030',padding:'4px 0 8px',fontFamily:'monospace'}}>
        v6 · platform: {Capacitor.getPlatform()} · native: {String(Capacitor.isNativePlatform())}
      </div>
      {myLibrary.length > 0 && (
        <div className="shelf-row my-library-row">
          <div className="shelf-row-header">
            <h2 className="shelf-row-title">My Library</h2>
          </div>
          <div className="my-library-grid">
            {myLibrary.slice(0, 6).map(book => (
              <BookCard key={book.id} book={book} progress={getProgress(book.id)} onClick={onSelectBook} compact />
            ))}
          </div>
        </div>
      )}
      {CATEGORIES.map(cat => (
        <ShelfRow
          key={cat.key}
          title={cat.label}
          books={sections[cat.key] || []}
          onSelect={onSelectBook}
          getProgress={getProgress}
          loading={!!loading[cat.key] || (!sectionCache[cat.key] && !errors[cat.key] && !sections[cat.key])}
          error={!!errors[cat.key]}
          onRetry={() => fetchCategory(cat)}
        />
      ))}
    </div>
  )
}
