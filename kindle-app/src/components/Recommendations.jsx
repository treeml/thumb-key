import React, { useEffect, useState } from 'react'
import { fetchBooksBySubject, searchBooks, getBookAuthors, getBookCoverUrl, getBookColors } from '../utils/api'

export default function Recommendations({ book, onSelect, nightMode }) {
  const [recs,    setRecs]    = useState([])
  const [loading, setLoading] = useState(true)
  const [tried,   setTried]   = useState(false)

  useEffect(() => {
    if (!book) return
    setLoading(true)
    setTried(false)
    let cancelled = false

    // Build search candidates in priority order
    const subjects = book.subjects || []
    const subjectKw = subjects
      .map(s => s.split(/[,;]/)[0].trim())
      .find(s => s.length >= 3 && s.length <= 28) || ''
    const authorSurname = (book.authors?.[0]?.name || '')
      .replace(/,.*/, '').split(' ').pop() || ''
    const titleKw = (book.title || '').split(/\s+/).slice(0, 2).join(' ').trim()

    // Unique non-empty candidates
    const candidates = [...new Set([subjectKw, authorSurname, titleKw].filter(s => s.length >= 2))]

    const tryCandidate = async (idx) => {
      if (idx >= candidates.length) {
        // Last resort: popular books
        const data = await searchBooks(null, 1)
        return (data.results || []).filter(b => b.id !== book.id).slice(0, 8)
      }
      try {
        const data = await fetchBooksBySubject(candidates[idx])
        const results = (data.results || []).filter(b => b.id !== book.id).slice(0, 8)
        return results.length >= 2 ? results : tryCandidate(idx + 1)
      } catch {
        return tryCandidate(idx + 1)
      }
    }

    tryCandidate(0)
      .then(results  => { if (!cancelled) { setRecs(results); setTried(true) } })
      .catch(()      => { if (!cancelled) { setRecs([]);      setTried(true) } })
      .finally(()    => { if (!cancelled)   setLoading(false) })

    return () => { cancelled = true }
  }, [book?.id])

  return (
    <div className={`recommendations ${nightMode ? 'night' : ''}`}>
      {loading ? (
        <div className="rec-grid">
          {Array.from({ length: 4 }).map((_, i) => <div key={i} className="rec-skeleton" />)}
        </div>
      ) : tried && recs.length === 0 ? (
        <div className="rec-empty">Couldn't find similar books right now.</div>
      ) : (
        <>
          <h3 className="rec-title">You might also like</h3>
          <div className="rec-grid">
            {recs.map(rec => {
              const cover  = getBookCoverUrl(rec)
              const colors = getBookColors(rec.id)
              return (
                <div key={rec.id} className="rec-card" onClick={() => onSelect(rec)}>
                  <div className="rec-cover" style={{ background: colors.spine }}>
                    {cover
                      ? <img src={cover} alt={rec.title} onError={e => { e.target.style.display = 'none' }} />
                      : <span className="rec-initial">{(rec.title || '?')[0]}</span>
                    }
                  </div>
                  <div className="rec-info">
                    <div className="rec-book-title">{rec.title?.length > 30 ? rec.title.slice(0, 28) + '…' : rec.title}</div>
                    <div className="rec-book-author">{getBookAuthors(rec)}</div>
                  </div>
                </div>
              )
            })}
          </div>
        </>
      )}
    </div>
  )
}
