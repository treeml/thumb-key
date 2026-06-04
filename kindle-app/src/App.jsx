import React, { useState, useCallback, useEffect, useRef } from 'react'
import Bookshelf    from './components/Bookshelf'
import BookDetail   from './components/BookDetail'
import Reader       from './components/Reader'
import Library      from './components/Library'
import SearchPage   from './components/SearchPage'
import AudioLibrary from './components/AudioLibrary'
import AudioPlayer  from './components/AudioPlayer'
import ImportButton from './components/ImportButton'
import { useLibrary }    from './hooks/useLibrary'
import { useAudiobooks } from './hooks/useAudiobooks'

const NAV_TABS = [
  { id: 'shelf',   icon: '🏠', label: 'Discover' },
  { id: 'browse',  icon: '🔍', label: 'Browse'   },
  { id: 'library', icon: '📚', label: 'Library'  },
  { id: 'audio',   icon: '🎧', label: 'Audio'    },
]

export default function App() {
  const [view,         setView]         = useState('shelf')
  const [selectedBook, setSelectedBook] = useState(null)
  const [activeAudio,  setActiveAudio]  = useState(null)
  const [returnView,   setReturnView]   = useState('shelf')
  const [nightMode,    setNightMode]    = useState(() => localStorage.getItem('tome_night') === 'true')

  const shelfScrollY = useRef(0)

  const { books: myLibrary, readingNow, addBook, removeBook, hasBook, setProgress, getProgress, shelveBook } = useLibrary()
  const { books: audioBooks, addAudiobook, removeAudiobook, setPosition, library: audioLib } = useAudiobooks()

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

  const handleAudioImported = useCallback(async (meta, file) => {
    await addAudiobook(meta, file)
    setView('audio')
  }, [addAudiobook])

  const handleOpenAudio = useCallback((book) => {
    setActiveAudio(book)
    setView('audioplayer')
  }, [])

  const isReader      = view === 'reader'
  const isAudioPlayer = view === 'audioplayer'
  const hideChrome    = isReader || isAudioPlayer

  return (
    <div className={`app ${nightMode ? 'night' : 'day'}`}>
      {!hideChrome && (
        <header className="app-header">
          <div className="header-inner">
            <div className="logo" onClick={() => { setView('shelf'); setSelectedBook(null) }}>
              <span className="logo-icon">📖</span>
              <span className="logo-text">Tome</span>
            </div>
            <div className="header-right">
              <ImportButton
                onBookImported={handleBookImported}
                onAudioImported={handleAudioImported}
              />
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

        {view === 'audio' && (
          <AudioLibrary
            books={audioBooks}
            onOpen={handleOpenAudio}
            onDelete={removeAudiobook}
            nightMode={nightMode}
          />
        )}

        {view === 'audioplayer' && activeAudio && (
          <AudioPlayer
            book={{ ...activeAudio, ...audioLib[activeAudio.id] }}
            nightMode={nightMode}
            setPosition={setPosition}
            onBack={() => setView('audio')}
          />
        )}
      </main>

      {!hideChrome && (
        <nav className="bottom-nav">
          {NAV_TABS.map(tab => (
            <button
              key={tab.id}
              className={`bnav-btn ${(view === tab.id || (tab.id === 'library' && view === 'library')) ? 'active' : ''}`}
              onClick={() => setView(tab.id)}
            >
              <span className="bnav-icon">{tab.icon}</span>
              <span className="bnav-label">{tab.label}</span>
            </button>
          ))}
        </nav>
      )}
    </div>
  )
}
