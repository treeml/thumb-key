import React, { useEffect, useState, useRef, useCallback } from 'react'
import { searchBooks, fetchBooksBySubject } from '../utils/api'
import BookCard from './BookCard'

const CATEGORIES = [
  { label: 'Popular',    key: 'popular',    query: null },
  { label: 'Adventure',  key: 'adventure',  query: 'adventure' },
  { label: 'Mystery',    key: 'mystery',    query: 'detective' },
  { label: 'Romance',    key: 'romance',    query: 'romance' },
  { label: 'Sci-Fi',     key: 'scifi',      query: 'science fiction' },
  { label: 'Philosophy', key: 'philosophy', query: 'philosophy' },
  { label: 'History',    key: 'history',    query: 'history' },
  { label: 'Poetry',     key: 'poetry',     query: 'poetry' },
]

// ─── Module-level cache (persisted to localStorage) ──────────────────────────
const cache     = {}
const errors    = {}
const pending   = {}
const listeners = new Set()
const CACHE_KEY = 'tome_shelf_v1'
const CACHE_TTL = 45 * 60 * 1000  // 45 min

function notify() { listeners.forEach(fn => fn()) }

// Warm cache from last session immediately
;(function warmCache() {
  try {
    const raw = localStorage.getItem(CACHE_KEY)
    if (!raw) return
    const { data, ts } = JSON.parse(raw)
    if (Date.now() - ts < CACHE_TTL) Object.assign(cache, data)
  } catch {}
})()

function persistCache() {
  try { localStorage.setItem(CACHE_KEY, JSON.stringify({ data: cache, ts: Date.now() })) } catch {}
}

function loadCat(cat) {
  if (cache[cat.key] || pending[cat.key]) return
  pending[cat.key] = true
  const p = cat.query ? fetchBooksBySubject(cat.query) : searchBooks(null, 1)
  p.then(data => {
    cache[cat.key]   = data.results || []
    errors[cat.key]  = null
    pending[cat.key] = false
    persistCache()
    notify()
  }).catch(e => {
    errors[cat.key]  = e?.message || 'Load failed'
    pending[cat.key] = false
    notify()
  })
}

// Load immediately for cached categories; stagger fresh fetches at 200ms intervals
CATEGORIES.forEach((cat, i) => setTimeout(() => loadCat(cat), cache[cat.key] ? 0 : i * 200))
// ─────────────────────────────────────────────────────────────────────────────

function ShelfRow({ title, books, onSelect, getProgress, loading, error, onRetry }) {
  const rowRef = useRef(null)
  const scroll = (dir) => rowRef.current?.scrollBy({ left: dir * 260, behavior: 'smooth' })

  return (
    <div className="shelf-row">
      <div className="shelf-row-header">
        <h2 className="shelf-row-title">{title}</h2>
        <div className="shelf-scroll-btns">
          <button className="shelf-scroll-btn" onClick={() => scroll(-1)}>‹</button>
          <button className="shelf-scroll-btn" onClick={() => scroll(1)}>›</button>
        </div>
      </div>
      <div className="shelf-track" ref={rowRef}>
        {loading
          ? Array.from({ length: 6 }).map((_, i) => <div key={i} className="book-card-skeleton" />)
          : error
          ? (
            <div className="shelf-error" style={{ flexDirection: 'column', alignItems: 'flex-start', gap: 8 }}>
              <div style={{ fontSize: 13, color: '#e07b39', maxWidth: 300 }}>Couldn't load — check your connection</div>
              <button className="retry-btn" onClick={onRetry}>Retry</button>
            </div>
          )
          : books.length === 0
          ? <div className="shelf-empty">No books found</div>
          : books.map(book => (
            <BookCard
              key={book.id}
              book={book}
              progress={getProgress(book.id)}
              onClick={onSelect}
            />
          ))
        }
      </div>
      <div className="shelf-plank" />
    </div>
  )
}

export default function Bookshelf({ onSelectBook, getProgress, myLibrary, onOpenLibrary, addBook, hasBook }) {
  // A single counter forces a re-render whenever the module cache updates
  const [, rerender] = useState(0)

  useEffect(() => {
    const trigger = () => rerender(n => n + 1)
    listeners.add(trigger)
    return () => listeners.delete(trigger)
  }, [])

  const retryCategory = useCallback((cat) => {
    delete cache[cat.key]
    delete errors[cat.key]
    loadCat(cat)
    rerender(n => n + 1)
  }, [])

  return (
    <div className="bookshelf-container">
      <div className="shelf-row my-library-row">
        <div className="shelf-row-header">
          <h2 className="shelf-row-title">My Library</h2>
          {myLibrary.length > 0 && (
            <span className="lib-book-count">{myLibrary.length} book{myLibrary.length !== 1 ? 's' : ''}</span>
          )}
        </div>
        <div className="lib-building-entry" onClick={onOpenLibrary} role="button">
          <svg viewBox="0 0 560 200" className="lib-building-svg" xmlns="http://www.w3.org/2000/svg">
            <defs>
              <radialGradient id="moonGl" cx="82%" cy="18%" r="28%">
                <stop offset="0%" stopColor="#ffeaa0" stopOpacity="0.28"/>
                <stop offset="100%" stopColor="#0d0820" stopOpacity="0"/>
              </radialGradient>
              <radialGradient id="lg1" cx="20%" cy="70%" r="18%">
                <stop offset="0%" stopColor="#ff9020" stopOpacity="0.22"/>
                <stop offset="100%" stopColor="#ff9020" stopOpacity="0"/>
              </radialGradient>
              <radialGradient id="lg2" cx="80%" cy="70%" r="18%">
                <stop offset="0%" stopColor="#ff9020" stopOpacity="0.22"/>
                <stop offset="100%" stopColor="#ff9020" stopOpacity="0"/>
              </radialGradient>
            </defs>
            <rect width="560" height="200" fill="#0d0820"/>
            <rect width="560" height="200" fill="url(#moonGl)"/>
            <rect width="560" height="200" fill="url(#lg1)"/>
            <rect width="560" height="200" fill="url(#lg2)"/>
            {[[42,14,1.1],[110,8,0.9],[190,18,1.3],[290,6,1.0],[390,12,1.2],[490,17,0.9],[68,40,0.8],[240,28,1.1],[440,32,1.0],[18,22,0.8],[530,10,1.1],[340,38,0.9]].map(([x,y,r],i)=>(
              <circle key={i} cx={x} cy={y} r={r} fill="white" opacity={0.55+i%4*0.1}/>
            ))}
            <circle cx="468" cy="26" r="19" fill="#ffecc0"/>
            <circle cx="477" cy="21" r="16" fill="#0d0820"/>
            <rect x="0" y="183" width="560" height="17" fill="#1a0d05"/>
            <rect x="0" y="181" width="560" height="3" fill="#261508"/>
            <rect x="55" y="174" width="450" height="9" fill="#3a2210" rx="1"/>
            <rect x="70" y="165" width="420" height="10" fill="#472a15" rx="1"/>
            <rect x="85" y="157" width="390" height="9" fill="#543218" rx="1"/>
            <rect x="85" y="68" width="390" height="90" fill="#2e1c0c"/>
            <polygon points="70,70 280,18 490,70" fill="#2a1908"/>
            <polygon points="85,70 280,23 475,70" fill="#3a2410"/>
            <text x="280" y="46" textAnchor="middle" fill="#c9a84c" fontSize="9" fontFamily="Georgia,serif" letterSpacing="3" opacity="0.85">BIBLIOTHECA</text>
            <rect x="78" y="66" width="404" height="8" fill="#4a3018"/>
            <rect x="78" y="72" width="404" height="3" fill="#3a2410"/>
            {[100,155,210,265,320,375,430,469].map((x,i)=>(
              <g key={i}>
                <rect x={x} y="75" width="13" height="83" fill="#4a3018" rx="1"/>
                <rect x={x+3} y="75" width="1" height="83" fill="#3a2408" opacity="0.45"/>
                <rect x={x+7} y="75" width="1" height="83" fill="#3a2408" opacity="0.45"/>
                <rect x={x-2} y="73" width="17" height="4" fill="#5a3a22" rx="1"/>
                <rect x={x-2} y="156" width="17" height="4" fill="#5a3a22" rx="1"/>
              </g>
            ))}
            {[120,213,303,393].map((x,i)=>(
              <g key={i}>
                <rect x={x} y="88" width="55" height="50" fill="#160e06" rx="1"/>
                <path d={`M${x} 88 Q${x+27.5} 76 ${x+55} 88`} fill="#160e06"/>
                <rect x={x+2} y="90" width="51" height="46" fill="#c87020" opacity="0.14" rx="1"/>
                <path d={`M${x+2} 90 Q${x+27.5} 79 ${x+53} 90`} fill="#c87020" opacity="0.14"/>
                <rect x={x+5} y="93" width="45" height="40" fill="#e8a040" opacity="0.09" rx="1"/>
                <line x1={x+27} y1="80" x2={x+27} y2="138" stroke="#160e06" strokeWidth="1.5"/>
                <line x1={x} y1="113" x2={x+55} y2="113" stroke="#160e06" strokeWidth="1.5"/>
              </g>
            ))}
            <rect x="257" y="126" width="46" height="34" fill="#0e0808" rx="2"/>
            <path d="M257 126 Q280 113 303 126" fill="#0e0808"/>
            <rect x="259" y="128" width="19" height="14" fill="#1a1008" opacity="0.5" rx="1"/>
            <rect x="281" y="128" width="20" height="14" fill="#1a1008" opacity="0.5" rx="1"/>
            <rect x="259" y="145" width="19" height="13" fill="#1a1008" opacity="0.5" rx="1"/>
            <rect x="281" y="145" width="20" height="13" fill="#1a1008" opacity="0.5" rx="1"/>
            <circle cx="298" cy="144" r="2.5" fill="#c9a84c"/>
            {[[112,155],[447,155]].map(([x,y],i)=>(
              <g key={i}>
                <rect x={x-2} y={y} width="4" height="30" fill="#3a2010"/>
                <rect x={x-8} y={y-3} width="16" height="2.5" fill="#3a2010" rx="1"/>
                <polygon points={`${x-6},${y-6} ${x+6},${y-6} ${x+4},${y} ${x-4},${y}`} fill="#2a1808"/>
                <rect x={x-5} y={y-9} width="10" height="4" fill="#2a1808" rx="1"/>
                <ellipse cx={x} cy={y-5} rx="5" ry="3" fill="#ffd060" opacity="0.95"/>
                <rect x={x-4} y={y+29} width="8" height="4" fill="#2a1808" rx="1"/>
              </g>
            ))}
          </svg>
          <div className="lib-building-cta">
            {myLibrary.length === 0
              ? 'Your library awaits — start reading to fill it'
              : `Enter your library — ${myLibrary.length} book${myLibrary.length !== 1 ? 's' : ''} inside`}
          </div>
        </div>
      </div>

      {CATEGORIES.map(cat => (
        <ShelfRow
          key={cat.key}
          title={cat.label}
          books={cache[cat.key] || []}
          onSelect={onSelectBook}
          getProgress={getProgress}
          loading={!!pending[cat.key] || (!cache[cat.key] && !errors[cat.key])}
          error={errors[cat.key] || null}
          onRetry={() => retryCategory(cat)}
        />
      ))}
    </div>
  )
}
