import React, { useState, useCallback, useEffect } from 'react'
import Bookshelf from './components/Bookshelf'
import BookDetail from './components/BookDetail'
import Reader from './components/Reader'
import Library from './components/Library'
import SearchPage from './components/SearchPage'
import { useLibrary } from './hooks/useLibrary'

export default function App() {
  const [view, setView]           = useState('shelf')
  const [selectedBook, setSelectedBook] = useState(null)
  const [returnView, setReturnView]     = useState('shelf')
  const [nightMode, setNightMode] = useState(() => localStorage.getItem('tome_night') === 'true')

  const { books: myLibrary, addBook, removeBook, hasBook, setProgress, getProgress } = useLibrary()

  useEffect(() => {
    localStorage.setItem('tome_night', nightMode)
    document.documentElement.setAttribute('data-theme', nightMode ? 'night' : 'day')
  }, [nightMode])

  const handleSelectBook = useCallback((book) => {
    setSelectedBook(book)
    setReturnView(view)
    setView('detail')
  }, [view])

  const handleRead = useCallback((book) => {
    setSelectedBook(book)
    setView('reader')
    addBook(book)
  }, [addBook])

  const handleBackFromDetail = useCallback(() => {
    setSelectedBook(null)
    setView(returnView || 'shelf')
  }, [returnView])

  const handleBackFromReader = useCallback((newBook) => {
    if (newBook && newBook.id) {
      setSelectedBook(newBook)
      setView('detail')
    } else {
      setView('shelf')
    }
  }, [])

  const handleOpenLibrary = useCallback(() => setView('library'), [])

  return (
    <div className={`app ${nightMode ? 'night' : 'day'}`}>
      {view !== 'reader' && (
        <header className="app-header">
          <div className="header-inner">
            <div className="logo" onClick={() => { setView('shelf'); setSelectedBook(null) }}>
              <span className="logo-icon">📖</span>
              <span className="logo-text">Tome</span>
            </div>

            <div className="header-actions">
              <button
                className={`library-tab ${view === 'shelf' ? 'active' : ''}`}
                onClick={() => setView('shelf')}
              >Discover</button>
              <button
                className={`library-tab ${view === 'browse' ? 'active' : ''}`}
                onClick={() => setView('browse')}
              >Browse</button>
              <button
                className={`library-tab ${view === 'library' ? 'active' : ''}`}
                onClick={handleOpenLibrary}
              >Library</button>
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
        {/* Keep Bookshelf mounted so fetched category data isn't lost */}
        <div style={{ display: view === 'shelf' ? 'block' : 'none' }}>
          <Bookshelf
            onSelectBook={handleSelectBook}
            getProgress={getProgress}
            myLibrary={myLibrary}
            onOpenLibrary={handleOpenLibrary}
            addBook={addBook}
            hasBook={hasBook}
          />
        </div>

        {view === 'browse' && (
          <SearchPage
            onSelectBook={handleSelectBook}
            getProgress={getProgress}
            addBook={addBook}
            hasBook={hasBook}
            onBack={() => setView('shelf')}
          />
        )}

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
        {view === 'library' && (
          <Library
            myLibrary={myLibrary}
            getProgress={getProgress}
            onBack={() => setView('shelf')}
            onRead={handleRead}
            nightMode={nightMode}
          />
        )}
      </main>
    </div>
  )
}
