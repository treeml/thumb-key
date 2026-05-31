import React from 'react'

function fmtDuration(secs) {
  if (!secs) return ''
  const h = Math.floor(secs / 3600)
  const m = Math.floor((secs % 3600) / 60)
  return h > 0 ? `${h}h ${m}m` : `${m}m`
}

export default function AudioLibrary({ books, onOpen, onDelete, nightMode }) {
  return (
    <div className={`audio-library ${nightMode ? 'night' : ''}`}>
      <div className="audio-library-header">
        <h1 className="audio-library-title">Audiobooks</h1>
      </div>

      {books.length === 0 ? (
        <div className="audio-empty">
          <div className="audio-empty-icon">🎧</div>
          <p>No audiobooks yet.</p>
          <p className="audio-empty-sub">
            Tap the <strong>Import</strong> button and choose an MP3, M4A, or M4B file.
          </p>
        </div>
      ) : (
        <div className="audio-book-grid">
          {books.map(book => (
            <div key={book.id} className="audio-book-card" onClick={() => onOpen(book)}>
              <div className="audio-book-art">
                {book.coverBase64
                  ? <img src={book.coverBase64} alt={book.title} />
                  : <div className="audio-art-placeholder">🎧</div>
                }
                <div className="audio-book-play-icon">▶</div>
              </div>
              <div className="audio-book-meta">
                <div className="audio-book-title">{book.title}</div>
                {book.author && <div className="audio-book-author">{book.author}</div>}
                {book.duration > 0 && (
                  <div className="audio-book-duration">{fmtDuration(book.duration)}</div>
                )}
                {book.position > 0 && book.duration > 0 && (
                  <div className="audio-progress-bar">
                    <div className="audio-progress-fill"
                      style={{ width: `${Math.round((book.position / book.duration) * 100)}%` }} />
                  </div>
                )}
              </div>
              <button className="audio-delete-btn"
                onClick={e => { e.stopPropagation(); onDelete(book.id) }}
                title="Remove">×</button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
