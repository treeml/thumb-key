import React, { useState, useRef, useEffect, useCallback } from 'react'
import { searchBooksAll } from '../utils/api'
import BookCard from './BookCard'

const SUGGESTIONS = [
  'Sherlock Holmes', 'Pride and Prejudice', 'Dracula',
  'Moby Dick', 'War and Peace', 'Alice in Wonderland',
  'The Great Gatsby', 'Frankenstein', 'Jane Eyre',
]

export default function SearchPage({ onSelectBook, getProgress, addBook, hasBook, onBack }) {
  const [query,     setQuery]     = useState('')
  const [submitted, setSubmitted] = useState('')
  const [results,   setResults]   = useState(null)
  const [searching, setSearching] = useState(false)
  const [error,     setError]     = useState(false)
  const [sources,   setSources]   = useState(null)
  const inputRef = useRef(null)

  useEffect(() => { inputRef.current?.focus() }, [])

  const doSearch = useCallback((q) => {
    const trimmed = q.trim()
    if (!trimmed) return
    setSubmitted(trimmed)
    setSearching(true)
    setError(false)
    setResults(null)
    setSources(null)
    searchBooksAll(trimmed)
      .then(d => { setResults(d.results || []); setSources(d.sources || null) })
      .catch(() => { setResults([]); setError(true) })
      .finally(() => setSearching(false))
  }, [])

  const handleSubmit = (e) => { e.preventDefault(); doSearch(query) }

  return (
    <div className="search-page">
      <div className="search-page-hero">
        <h1 className="search-page-title">Find Free Books</h1>
        <p className="search-page-sub">
          Search 5 million+ titles from Project Gutenberg &amp; Open Library
        </p>
        <form className="search-page-form" onSubmit={handleSubmit}>
          <input
            ref={inputRef}
            className="search-page-input"
            type="text"
            value={query}
            onChange={e => setQuery(e.target.value)}
            placeholder="Title, author, or topic…"
          />
          <button type="submit" className="search-page-btn">Search</button>
        </form>
      </div>

      {!searching && results === null && (
        <div className="search-page-tips">
          <div className="tip-label">Try searching for…</div>
          <div className="tip-chips">
            {SUGGESTIONS.map(t => (
              <button key={t} className="tip-chip"
                onClick={() => { setQuery(t); doSearch(t) }}>
                {t}
              </button>
            ))}
          </div>
        </div>
      )}

      {searching && (
        <div className="search-page-loading">
          <div className="search-spinner" />
          <p>Searching Gutenberg &amp; Open Library…</p>
        </div>
      )}

      {results !== null && !searching && (
        <div className="search-page-results">
          <div className="search-results-header">
            <h2>"{submitted}"</h2>
            <span className="result-count">{results.length} books</span>
          </div>
          {sources && (sources.gutenberg > 0 || sources.openlibrary > 0) && (
            <div className="search-sources-row">
              {sources.gutenberg  > 0 && <span className="src-chip src-gut">📕 {sources.gutenberg} Gutenberg</span>}
              {sources.openlibrary > 0 && <span className="src-chip src-ol">📚 {sources.openlibrary} Open Library</span>}
            </div>
          )}
          {error && (
            <div className="search-error-msg">Connection error — check your internet and try again.</div>
          )}
          <div className="search-results-grid">
            {results.map(book => (
              <BookCard
                key={book.id}
                book={book}
                progress={getProgress(book.id)}
                onClick={onSelectBook}
                onAddToLibrary={addBook}
                inLibrary={hasBook?.(book.id)}
              />
            ))}
            {results.length === 0 && !error && (
              <div className="empty-state">No free copies found. Try a different title or author.</div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
