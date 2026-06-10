import React, { useState, useCallback, useEffect, useRef } from 'react'
import Bookshelf    from './components/Bookshelf'
import BookDetail   from './components/BookDetail'
import Reader       from './components/Reader'
import Library      from './components/Library'
import SearchPage   from './components/SearchPage'
import ImportButton from './components/ImportButton'
import { useLibrary } from './hooks/useLibrary'

const DiscoverIcon = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
    <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/>
    <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>
  </svg>
)

const BrowseIcon = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="11" cy="11" r="7"/>
    <path d="m21 21-4.35-4.35"/>
  </svg>
)

const LibraryIcon = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
    <rect x="3" y="4" width="3.5" height="16" rx="0.8"/>
    <rect x="9" y="7" width="3.5" height="13" rx="0.8"/>
    <rect x="15" y="5" width="3.5" height="15" rx="0.8"/>
    <line x1="2" y1="21" x2="21" y2="21"/>
  </svg>
)

const SunIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
    <circle cx="12" cy="12" r="4"/>
    <line x1="12" y1="2" x2="12" y2="5"/><line x1="12" y1="19" x2="12" y2="22"/>
    <line x1="2" y1="12" x2="5" y2="12"/><line x1="19" y1="12" x2="22" y2="12"/>
    <line x1="4.93" y1="4.93" x2="7.05" y2="7.05"/><line x1="16.95" y1="16.95" x2="19.07" y2="19.07"/>
    <line x1="19.07" y1="4.93" x2="16.95" y2="7.05"/><line x1="7.05" y1="16.95" x2="4.93" y2="19.07"/>
  </svg>
)

const MoonIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
  </svg>
)

const NAV_TABS = [
  { id: 'shelf',   Icon: DiscoverIcon, label: 'Discover' },
  { id: 'browse',  Icon: BrowseIcon,   label: 'Browse'   },
  { id: 'library', Icon: LibraryIcon,  label: 'Library'  },
]

export default function App() {
  const [view,         setView]         = useState('shelf')
  const [selectedBook, setSelectedBook] = useState(null)
  const [returnView,   setReturnView]   = useState('shelf')
  const [nightMode,    setNightMode]    = useState(() => localStorage.getItem('tome_night') === 'true')

  const shelfScrollY = useRef(0)

  const { books: myLibrary, readingNow, addBook, removeBook, hasBook, setProgress, getProgress, shelveBook } = useLibrary()

  useEffect(() => {
    localStorage.setItem('tome_night', nightMode)
    document.documentElement.setAttribute('data-theme', nightMode ? 'night' : 'day')
  }, [nightMode])

  const handleSelectBook = useCallback((book) => {
    if (view === 'shelf') shelfScrollY.current = window.scrollY
    setReturnView(view)
    setSelectedBook(book)
    setView('detail')
  }, [view])

  const handleRead = useCallback((book) => {
    setSelectedBook(book)
    setView('reader')
    addBook(book)
  }, [addBook])

  const handleBackFromDetail = useCallback(() => {
    const dest = returnView || 'shelf'
    setSelectedBook(null)
    setView(dest)
    if (dest === 'shelf') {
      requestAnimationFrame(() => window.scrollTo(0, shelfScrollY.current))
    }
  }, [returnView])

  const handleBackFromReader = useCallback((newBook) => {
    if (newBook && newBook.id) { setSelectedBook(newBook); setView('detail') }
    else setView('shelf')
  }, [])

  const handleBookImported = useCallback((book) => {
    addBook(book)
    setSelectedBook(book)
    setReturnView('library')
    setView('detail')
  }, [addBook])

  const isReader   = view === 'reader'
  const hideChrome = isReader

  return (
    <div className={`app ${nightMode ? 'night' : 'day'}`}>
      {!hideChrome && (
        <header className="app-header">
          <div className="header-inner">
            <div className="logo" onClick={() => { setView('shelf'); setSelectedBook(null) }}>
              <svg className="logo-mark" width="28" height="28" viewBox="0 0 28 28" fill="none">
                <rect x="1" y="1" width="26" height="26" rx="6" fill="rgba(201,168,76,0.1)" stroke="var(--gold)" strokeWidth="1.5"/>
                <path d="M8 8h12M14 8v12" stroke="var(--gold)" strokeWidth="2.2" strokeLinecap="round"/>
              </svg>
              <span className="logo-text">Tome</span>
            </div>
            <div className="header-right">
              <ImportButton onBookImported={handleBookImported} />
              <button
                className="night-toggle"
                onClick={() => setNightMode(n => !n)}
                title={nightMode ? 'Day mode' : 'Night mode'}
              >
                {nightMode ? <SunIcon /> : <MoonIcon />}
              </button>
            </div>
          </div>
        </header>
      )}

      <main className={`app-main${hideChrome ? '' : ' with-bottom-nav'}`}>
        <div style={{ display: view === 'shelf' ? 'block' : 'none' }}>
          <Bookshelf
            onSelectBook={handleSelectBook}
            getProgress={getProgress}
            myLibrary={myLibrary}
            onOpenLibrary={() => setView('library')}
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
            readingNow={readingNow}
            getProgress={getProgress}
            onBack={() => setView('shelf')}
            onRead={handleRead}
            removeBook={removeBook}
            shelveBook={shelveBook}
            nightMode={nightMode}
          />
        )}
      </main>

      {!hideChrome && (
        <nav className="bottom-nav">
          {NAV_TABS.map(({ id, Icon, label }) => (
            <button
              key={id}
              className={`bnav-btn${(view === id || (id === 'library' && view === 'library')) ? ' active' : ''}`}
              onClick={() => setView(id)}
            >
              <span className="bnav-icon"><Icon /></span>
              <span className="bnav-label">{label}</span>
            </button>
          ))}
        </nav>
      )}
    </div>
  )
}
