import React from 'react'
import { getBookAuthors, getBookCoverUrl, getBookColors } from '../utils/api'
import Recommendations from './Recommendations'

export default function BookDetail({ book, onRead, onBack, onSelectBook, hasBook, addBook, removeBook, getProgress, nightMode }) {
  const cover = getBookCoverUrl(book)
  const colors = getBookColors(book.id)
  const author = getBookAuthors(book)
  const progress = getProgress(book.id)
  const inLibrary = hasBook(book.id)

  const subjects = book.subjects?.slice(0, 5) || []
  const downloads = book.download_count || 0

  return (
    <div className={`book-detail ${nightMode ? 'night' : ''}`}>
      <div className="detail-topbar">
        <button className="back-btn" onClick={onBack}>← Back</button>
      </div>
      <div className="detail-hero">
        <div className="detail-cover-wrapper">
          <div className="detail-cover" style={{ background: colors.spine }}>
            {cover
              ? <img src={cover} alt={book.title} onError={e => { e.target.style.display='none' }} />
              : (
                <div className="detail-cover-text">
                  <div className="detail-cover-title">{book.title}</div>
                  <div className="detail-cover-author">{author}</div>
                </div>
              )
            }
          </div>
          <div className="detail-cover-spine" style={{ background: colors.spine }} />
        </div>
        <div className="detail-info">
          <h1 className="detail-title">{book.title}</h1>
          <div className="detail-author">by {author}</div>
          <div className="detail-stats">
            <span className="stat-badge">📥 {downloads.toLocaleString()} downloads</span>
            {book.languages?.map(l => (
              <span key={l} className="stat-badge lang-badge">{l.toUpperCase()}</span>
            ))}
          </div>
          {subjects.length > 0 && (
            <div className="detail-subjects">
              {subjects.map((s, i) => (
                <span key={i} className="subject-chip">{s.split(' -- ')[0].trim()}</span>
              ))}
            </div>
          )}
          {progress > 0 && (
            <div className="detail-progress">
              <div className="progress-bar-detail">
                <div className="progress-fill" style={{ width: `${progress}%` }} />
              </div>
              <span className="progress-label">{progress}% read</span>
            </div>
          )}
          <div className="detail-actions">
            <button className="btn-read" onClick={() => onRead(book)}>
              {progress > 0 ? `Continue Reading (${progress}%)` : 'Read Now'}
            </button>
            <button
              className={`btn-library ${inLibrary ? 'in-library' : ''}`}
              onClick={() => inLibrary ? removeBook(book.id) : addBook(book)}
            >
              {inLibrary ? '✓ In Library' : '+ Add to Library'}
            </button>
          </div>
        </div>
      </div>

      <div className="detail-section">
        <h2 className="section-title">About this Book</h2>
        <p className="detail-description">
          {subjects.length > 0
            ? `A work covering themes of ${subjects.slice(0,3).map(s=>s.split(' -- ')[0].trim().toLowerCase()).join(', ')}. Freely available through ${book.source === 'openlibrary' ? 'Open Library / Internet Archive' : 'Project Gutenberg'}.`
            : `Freely available through ${book.source === 'openlibrary' ? 'Open Library / Internet Archive' : 'Project Gutenberg, the world\'s oldest digital library'}.`
          }
        </p>
      </div>

      <div className="detail-section">
        <Recommendations
          book={book}
          onSelect={onSelectBook}
          nightMode={nightMode}
        />
      </div>
    </div>
  )
}
