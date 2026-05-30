import React, { useState, useCallback, useEffect } from 'react'
import Bookshelf from './components/Bookshelf'
import BookDetail from './components/BookDetail'
import Reader from './components/Reader'
import { useLibrary } from './hooks/useLibrary'

export default function App() {
  const [view, setView] = useState('shelf')
  const [selectedBook, setSelectedBook] = useState(null)
  const [nightMode, setNightMode] = useState(() => localStorage.getItem('tome_night') === 'true')
  const [searchQuery, setSearchQuery] = useState('')
  const [searchInput, setSearchInput] = useState('')

  const { books: myLibrary, addBook, removeBook, hasBook, setProgress, getProgress } = useLibrary()

  useEffect(() => {
    localStorage.setItem('tome_night', nightMode)
    document.documentElement.setAttribute('data-theme', nightMode ? 'night' : 'day')
  }, [nightMode])

  const handleSelectBook = useCallback((book) => {
    setSelectedBook(book)
    setView('detail')
  }, [])

  const handleRead = useCallback((book) => {
    setSelectedBook(book)
    setView('reader')
    addBook(book)
  }, [addBook])

  const handleBackFromDetail = useCallback(() => {
    setSelectedBook(null)
    setView('shelf')
  }, [])

  const handleBackFromReader = useCallback((newBook) => {
    if (newBook && newBook.id) {
      setSelectedBook(newBook)
      setView('detail')
    } else {
      setView('detail')
    }
  }, [])

  const handleSearch = (e) => {
    e.preventDefault()
    setSearchQuery(searchInput)
    setView('shelf')
  }

  return (
    <div className={`app ${nightMode ? 'night' : 'day'}`}>
      {view !== 'reader' && (
        <header className="app-header">
          <div className="header-inner">
            <div className="logo" onClick={() => { setView('shelf'); setSelectedBook(null); setSearchQuery(''); setSearchInput('') }}>
              <span className="logo-icon">📖</span>
              <span className="logo-text">Tome</span>
            </div>

            <form className="search-form" onSubmit={handleSearch}>
              <input
                className="search-input"
                type="text"
                placeholder="Search 70,000+ free books…"
                value={searchInput}
                onChange={e => setSearchInput(e.target.value)}
              />
              <button type="submit" className="search-btn">🔍</button>
            </form>

            <div className="header-actions">
              <button
                className={`library-tab ${view === 'shelf' ? 'active' : ''}`}
                onClick={() => { setView('shelf'); setSearchQuery(''); setSearchInput('') }}
              >Browse</button>
              <button
                className="night-toggle"
                onClick={() => setNightMode(n => !n)}
                title={nightMode ? 'Day mode' : 'Night mode'}
              >
                {nightMode ? '☀️' : '🌙'}
              </button>
            </div>
          </div>
        </header>
      )}

      <main className="app-main">
        {/* Keep mounted so fetched books aren't lost on navigation */}
        <div style={{ display: view === 'shelf' ? 'block' : 'none' }}>
          <Bookshelf
            onSelectBook={handleSelectBook}
            getProgress={getProgress}
            myLibrary={myLibrary}
            searchQuery={searchQuery}
          />
        </div>
        {view === 'detail' && selectedBook && (
          <BookDetail
            book={selectedBook}
            onRead={handleRead}
            onBack={handleBackFromDetail}
            onSelectBook={handleSelectBook}
            hasBook={hasBook}
            addBook={addBook}
            removeBook={removeBook}
            getProgress={getProgress}
            nightMode={nightMode}
          />
        )}
        {view === 'reader' && selectedBook && (
          <Reader
            book={selectedBook}
            nightMode={nightMode}
            setProgress={setProgress}
            initialProgress={getProgress(selectedBook.id)}
            onBack={handleBackFromReader}
          />
        )}
      </main>
    </div>
  )
}
