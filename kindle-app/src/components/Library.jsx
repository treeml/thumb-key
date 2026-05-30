import React, { useState, useCallback } from 'react'

const SPINE_PALETTES = [
  { base: '#8B2635', read: '#d4736b' },
  { base: '#1B4F72', read: '#5a9ec9' },
  { base: '#1E8449', read: '#5ec98a' },
  { base: '#784212', read: '#c4895a' },
  { base: '#4A235A', read: '#9a5db0' },
  { base: '#1A5276', read: '#4ea4c9' },
  { base: '#922B21', read: '#d47a70' },
  { base: '#1E6251', read: '#5ebda0' },
  { base: '#5D4037', read: '#a07865' },
  { base: '#283593', read: '#6878d4' },
  { base: '#BF360C', read: '#e8855a' },
  { base: '#006064', read: '#30b0b8' },
]

function bookPalette(id) { return SPINE_PALETTES[Number(id) % SPINE_PALETTES.length] }
function bookH(id)  { return 130 + (Number(id) * 17 + 13) % 65 }
function bookW(id)  { return 24  + (Number(id) * 7  +  5) % 16 }

function SpineBook({ book, progress, onTap, isPulled }) {
  const pal = bookPalette(book.id)
  const h   = bookH(book.id)
  const w   = bookW(book.id)
  const pct = Math.round((progress || 0) * 100)

  const bg = pct > 0
    ? `linear-gradient(to top, ${pal.read} 0%, ${pal.read} ${pct}%, ${pal.base} ${pct}%, ${pal.base} 100%)`
    : pal.base

  return (
    <div
      className={`lib-spine-book${isPulled ? ' lib-spine-pulled' : ''}`}
      style={{ height: h, width: w }}
      onClick={() => !isPulled && onTap(book)}
      role="button"
      aria-label={book.title}
    >
      <div className="lib-spine-face" style={{ background: bg }}>
        {pct > 0 && pct < 100 && (
          <div className="lib-spine-divider" style={{ bottom: `${pct}%` }} />
        )}
        <div className="lib-spine-title-wrap">
          <span className="lib-spine-title">{book.title}</span>
        </div>
        {pct === 100 && <div className="lib-spine-check">✓</div>}
      </div>
      <div className="lib-spine-top"    style={{ background: `${pal.base}dd` }} />
      <div className="lib-spine-right"  style={{ background: `${pal.base}88` }} />
    </div>
  )
}

function BookOpenOverlay({ book, progress }) {
  const pal = bookPalette(book.id)
  const pct = Math.round((progress || 0) * 100)
  const authors = book.authors?.map(a => a.name).join(', ') || ''

  return (
    <div className="lib-open-overlay">
      <div className="lib-open-stage">
        <div className="lib-open-page">
          <div className="lib-open-page-lines">
            {Array.from({ length: 12 }).map((_, i) => (
              <div key={i} className="lib-open-line" style={{ width: `${60 + (i % 3) * 15}%`, opacity: 0.18 + (i % 4) * 0.05 }} />
            ))}
          </div>
          <div className="lib-open-page-caption">
            {pct > 0 ? `Resuming at ${pct}%…` : 'Starting from the beginning…'}
          </div>
        </div>
        <div className="lib-open-cover" style={{ background: pal.base }}>
          <div className="lib-open-cover-title">{book.title}</div>
          {authors && <div className="lib-open-cover-author">{authors}</div>}
        </div>
      </div>
    </div>
  )
}

export default function Library({ myLibrary, getProgress, onBack, onRead, nightMode }) {
  const [phase, setPhase]       = useState('shelf')
  const [activeBook, setActiveBook] = useState(null)

  const handleTapBook = useCallback((book) => {
    setActiveBook(book)
    setPhase('pulling')
    setTimeout(() => {
      setPhase('opening')
      setTimeout(() => { onRead(book) }, 1100)
    }, 650)
  }, [onRead])

  const ROW_SIZE = 9
  const rows = []
  for (let i = 0; i < myLibrary.length; i += ROW_SIZE) {
    rows.push(myLibrary.slice(i, i + ROW_SIZE))
  }

  return (
    <div className={`library-view${nightMode ? ' night' : ''}`}>
      {(phase === 'pulling' || phase === 'opening') && activeBook && (
        <BookOpenOverlay book={activeBook} progress={getProgress(activeBook.id)} />
      )}

      <div className="library-topbar">
        <button className="back-btn" onClick={onBack}>← Browse</button>
        <h1 className="library-heading">My Library</h1>
        <div style={{ width: 60 }} />
      </div>

      {myLibrary.length === 0 ? (
        <div className="library-empty">
          <div className="library-empty-icon">📚</div>
          <p>Your library is empty.</p>
          <p style={{ color: 'var(--text-muted)', fontSize: 13, marginTop: 4 }}>
            Start reading any book to add it here.
          </p>
        </div>
      ) : (
        <div className="library-shelves-wrap">
          <div className="library-shelves">
            <p className="library-hint">Tap a book to open it</p>
            {rows.map((rowBooks, ri) => (
              <div key={ri} className="lib-shelf-row">
                <div className="lib-shelf-books">
                  {rowBooks.map(book => (
                    <SpineBook
                      key={book.id}
                      book={book}
                      progress={getProgress(book.id)}
                      onTap={handleTapBook}
                      isPulled={activeBook?.id === book.id && phase !== 'shelf'}
                    />
                  ))}
                </div>
                <div className="lib-shelf-plank" />
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
