import React, { useEffect, useState } from 'react'
import { fetchBooksBySubject, getBookAuthors, getBookCoverUrl, getBookColors } from '../utils/api'

export default function Recommendations({ book, onSelect, nightMode }) {
  const [recs, setRecs] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!book) return
    setLoading(true)
    // Find a relevant subject from the book
    const subjects = book.subjects || []
    const rawSubject = subjects.find(s => s.length < 30) || subjects[0] || ''
    // Extract a simple keyword
    const keyword = rawSubject.split(/[,;]/)[0].trim() ||
      (book.title || '').split(' ').slice(0, 2).join(' ')

    fetchBooksBySubject(keyword)
      .then(data => {
        const filtered = (data.results || []).filter(b => b.id !== book.id).slice(0, 8)
        setRecs(filtered)
      })
      .catch(() => setRecs([]))
      .finally(() => setLoading(false))
  }, [book?.id])

  if (!recs.length && !loading) return null

  return (
    <div className={`recommendations ${nightMode ? 'night' : ''}`}>
      <h3 className="rec-title">You might also like</h3>
      <div className="rec-grid">
        {loading
          ? Array.from({ length: 4 }).map((_, i) => <div key={i} className="rec-skeleton" />)
          : recs.map(rec => {
            const cover = getBookCoverUrl(rec)
            const colors = getBookColors(rec.id)
            return (
              <div key={rec.id} className="rec-card" onClick={() => onSelect(rec)}>
                <div className="rec-cover" style={{ background: colors.spine }}>
                  {cover
                    ? <img src={cover} alt={rec.title} onError={e => { e.target.style.display='none' }} />
                    : <span className="rec-initial">{(rec.title || '?')[0]}</span>
                  }
                </div>
                <div className="rec-info">
                  <div className="rec-book-title">{rec.title?.length > 30 ? rec.title.slice(0,28)+'…' : rec.title}</div>
                  <div className="rec-book-author">{getBookAuthors(rec)}</div>
                </div>
              </div>
            )
          })
        }
      </div>
    </div>
  )
}
