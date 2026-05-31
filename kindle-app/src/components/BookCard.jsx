import React from 'react'
import { getBookAuthors, getBookCoverUrl, getBookColors } from '../utils/api'

export default function BookCard({ book, progress, onClick, compact, onAddToLibrary, inLibrary }) {
  const cover  = getBookCoverUrl(book)
  const colors = getBookColors(book.id)
  const author = getBookAuthors(book)
  const title  = book.title || 'Untitled'
  const isOL   = book.source === 'openlibrary'

  if (compact) {
    return (
      <div className="book-card-compact" onClick={() => onClick?.(book)}>
        <div className="book-cover-sm" style={{ background: colors.spine }}>
          {cover
            ? <img src={cover} alt={title} onError={e => { e.target.style.display = 'none' }} />
            : <span className="book-initial">{title[0]}</span>
          }
        </div>
        <div className="book-card-info">
          <div className="book-title-sm">{title}</div>
          <div className="book-author-sm">{author}</div>
          {progress > 0 && (
            <div className="progress-bar-sm">
              <div className="progress-fill-sm" style={{ width: `${progress}%` }} />
            </div>
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="book-card" onClick={() => onClick?.(book)}>
      <div className="book-cover-wrapper">
        <div className="book-cover" style={{ background: colors.spine }}>
          {cover
            ? <img src={cover} alt={title} onError={e => { e.target.style.display = 'none' }} />
            : (
              <div className="book-cover-placeholder">
                <span className="book-cover-title">{title}</span>
                <span className="book-cover-author">{author}</span>
              </div>
            )
          }
          <div className="book-spine" style={{ background: colors.spine }} />
        </div>
        {progress > 0 && (
          <div className="book-progress-badge">{Math.round(progress)}%</div>
        )}
        {onAddToLibrary && (
          <button
            className={`book-quick-add${inLibrary ? ' in-lib' : ''}`}
            onClick={e => { e.stopPropagation(); if (!inLibrary) onAddToLibrary(book) }}
            title={inLibrary ? 'In Library' : 'Add to Library'}
          >
            {inLibrary ? '✓' : '+'}
          </button>
        )}
      </div>
      <div className="book-card-meta">
        <div className="book-card-title">{title.length > 40 ? title.slice(0, 38) + '…' : title}</div>
        <div className="book-card-author">{author}</div>
        {isOL && <div className="book-source-badge">Open Library</div>}
        {progress > 0 && (
          <div className="progress-bar">
            <div className="progress-fill" style={{ width: `${progress}%` }} />
          </div>
        )}
      </div>
    </div>
  )
}
