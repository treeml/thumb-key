import React, { useEffect, useState, useRef, useCallback } from 'react'
import { fetchBookText } from '../utils/api'
import { useHighlights } from '../hooks/useHighlights'
import Dictionary from './Dictionary'
import Recommendations from './Recommendations'

function applyHighlights(text, hl) {
  if (!hl.length) return text
  let result = text
  for (const h of [...hl].sort((a, b) => b.text.length - a.text.length)) {
    const esc = h.text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    result = result.replace(
      new RegExp(esc, 'g'),
      `<mark class="highlight" style="background:${h.color}88">${h.text}</mark>`
    )
  }
  return result
}

export default function Reader({ book, nightMode, setProgress, initialProgress, onBack }) {
  const [fullText, setFullText]   = useState('')
  const [loading, setLoading]     = useState(true)
  const [error, setError]         = useState(null)
  const [offset, setOffset]       = useState(0)   // px from top of content
  const [pageH, setPageH]         = useState(0)   // visible page height px
  const [totalH, setTotalH]       = useState(0)   // full content height px
  const [flipping, setFlipping]   = useState(null) // 'fwd' | 'bck' | null
  const [flipDone, setFlipDone]   = useState(false)
  const [showMenu, setShowMenu]   = useState(false)
  const [dictWord, setDictWord]   = useState(null)
  const [dictPos, setDictPos]     = useState(null)
  const [showRecs, setShowRecs]   = useState(false)
  const [highlightColor, setHighlightColor] = useState('#FFD700')
  const [fontSize, setFontSize]   = useState(18)
  const [showHighlightPanel, setShowHighlightPanel] = useState(false)

  const windowRef = useRef(null) // overflow:hidden viewport
  const innerRef  = useRef(null) // full text content
  const touchStartX = useRef(null)
  const touchStartY = useRef(null)

  const { highlights, addHighlight, removeHighlight } = useHighlights(book.id)

  // --- Load text ---
  useEffect(() => {
    setLoading(true); setError(null); setOffset(0)
    fetchBookText(book)
      .then(text => { setFullText(text) })
      .catch(e => setError(e.message))
      .finally(() => setLoading(false))
  }, [book.id])

  // --- Measure heights after render ---
  const measure = useCallback(() => {
    if (!windowRef.current || !innerRef.current) return
    const ph = windowRef.current.clientHeight
    const th = innerRef.current.scrollHeight
    setPageH(ph)
    setTotalH(th)
    // Restore saved progress position
    if (initialProgress > 0 && offset === 0) {
      const target = Math.round((initialProgress / 100) * (th - ph))
      const snapped = Math.round(target / ph) * ph
      setOffset(Math.max(0, Math.min(snapped, th - ph)))
    }
  }, [initialProgress])

  useEffect(() => {
    if (!fullText) return
    // Wait one frame for DOM to paint, then measure
    const id = requestAnimationFrame(() => measure())
    return () => cancelAnimationFrame(id)
  }, [fullText, fontSize, measure])

  useEffect(() => {
    const ro = new ResizeObserver(measure)
    if (windowRef.current) ro.observe(windowRef.current)
    return () => ro.disconnect()
  }, [measure])

  // --- Track progress ---
  useEffect(() => {
    if (!pageH || !totalH) return
    const pct = Math.round((offset / Math.max(totalH - pageH, 1)) * 100)
    setProgress(book.id, Math.min(100, Math.max(0, pct)))
  }, [offset, pageH, totalH])

  // --- Navigation ---
  const maxOffset = Math.max(0, totalH - pageH)
  const atStart   = offset <= 0
  const atEnd     = offset >= maxOffset

  const goForward = useCallback(() => {
    if (atEnd || flipping) return
    const next = Math.min(offset + pageH, maxOffset)
    setFlipping('fwd')
    // Halfway through animation: update content position
    setTimeout(() => { setOffset(next); setFlipDone(true) }, 180)
    setTimeout(() => { setFlipping(null); setFlipDone(false) }, 380)
  }, [atEnd, flipping, offset, pageH, maxOffset])

  const goBack = useCallback(() => {
    if (atStart || flipping) return
    const next = Math.max(offset - pageH, 0)
    setFlipping('bck')
    setTimeout(() => { setOffset(next); setFlipDone(true) }, 180)
    setTimeout(() => { setFlipping(null); setFlipDone(false) }, 380)
  }, [atStart, flipping, offset, pageH])

  useEffect(() => {
    const handler = (e) => {
      if (e.key === 'ArrowRight') goForward()
      if (e.key === 'ArrowLeft') goBack()
      if (e.key === 'Escape') { setShowMenu(false); setDictWord(null) }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [goForward, goBack])

  const handleTouchStart = (e) => {
    touchStartX.current = e.touches[0].clientX
    touchStartY.current = e.touches[0].clientY
  }
  const handleTouchEnd = (e) => {
    if (touchStartX.current === null) return
    const dx = touchStartX.current - e.changedTouches[0].clientX
    const dy = Math.abs(e.changedTouches[0].clientY - touchStartY.current)
    if (Math.abs(dx) > 60 && Math.abs(dx) > dy) { dx > 0 ? goForward() : goBack() }
    touchStartX.current = null
  }

  const handleTextClick = (e) => {
    if (e.target.closest('.reader-menu, .highlights-panel, .recs-panel, .dictionary-popup')) return
    const sel = window.getSelection()?.toString().trim()
    if (sel && sel.length > 1 && sel.split(' ').length <= 5) {
      const rect = window.getSelection().getRangeAt(0).getBoundingClientRect()
      setDictWord(sel)
      setDictPos({ x: rect.left + rect.width / 2, y: rect.bottom })
    } else {
      setDictWord(null)
    }
  }

  const handleHighlight = () => {
    const sel = window.getSelection()?.toString().trim()
    if (!sel || sel.length < 2) return
    const pageIndex = pageH > 0 ? Math.floor(offset / pageH) : 0
    addHighlight(sel, pageIndex, highlightColor)
    window.getSelection().removeAllRanges()
    setDictWord(null)
  }

  // Progress info
  const currentPage  = pageH > 0 ? Math.floor(offset / pageH) + 1 : 1
  const totalPages   = pageH > 0 ? Math.ceil(totalH / pageH) : 1
  const progressPct  = Math.round((offset / Math.max(totalH - pageH, 1)) * 100)
  const displayHtml  = applyHighlights(fullText, highlights)
    .replace(/\n\n+/g, '</p><p>')
    .replace(/\n/g, '<br/>')

  // --- Loading / Error states ---
  if (loading) {
    return (
      <div className={`reader-loading ${nightMode ? 'night' : ''}`}>
        <div className="reader-loading-book">
          <div className="loading-page loading-page-1" />
          <div className="loading-page loading-page-2" />
          <div className="loading-page loading-page-3" />
        </div>
        <p>Loading book…</p>
      </div>
    )
  }
  if (error) {
    return (
      <div className={`reader-error ${nightMode ? 'night' : ''}`}>
        <div className="error-icon">📚</div>
        <h3>Couldn't load this book</h3>
        <p>{error}</p>
        <button className="btn-primary" onClick={onBack}>Go Back</button>
      </div>
    )
  }

  return (
    <div
      className={`reader-container ${nightMode ? 'night' : ''}`}
      onClick={handleTextClick}
      onTouchStart={handleTouchStart}
      onTouchEnd={handleTouchEnd}
    >
      {/* Top bar */}
      <div className="reader-topbar">
        <button className="reader-back-btn" onClick={onBack}>← Library</button>
        <div className="reader-book-title-bar">{book.title}</div>
        <button className="reader-menu-btn" onClick={e => { e.stopPropagation(); setShowMenu(p => !p) }}>⋮</button>
      </div>

      {/* Settings menu */}
      {showMenu && (
        <div className="reader-menu" onClick={e => e.stopPropagation()}>
          <div className="menu-section">
            <label className="menu-label">Font Size</label>
            <div className="font-size-controls">
              <button onClick={() => setFontSize(f => Math.max(13, f - 2))}>A-</button>
              <span>{fontSize}px</span>
              <button onClick={() => setFontSize(f => Math.min(26, f + 2))}>A+</button>
            </div>
          </div>
          <div className="menu-section">
            <label className="menu-label">Highlight Color</label>
            <div className="color-swatches">
              {['#FFD700', '#90EE90', '#87CEEB', '#FFB6C1', '#DDA0DD'].map(c => (
                <button key={c} className={`color-swatch ${highlightColor === c ? 'active' : ''}`}
                  style={{ background: c }} onClick={() => setHighlightColor(c)} />
              ))}
            </div>
          </div>
          <button className="menu-item" onClick={() => { setShowHighlightPanel(p => !p); setShowMenu(false) }}>
            📌 My Highlights ({highlights.length})
          </button>
          <button className="menu-item" onClick={() => { setShowRecs(p => !p); setShowMenu(false) }}>
            ✨ Recommendations
          </button>
          <div className="menu-section">
            <label className="menu-label">Jump to position</label>
            <input type="range" min="0" max="100" value={progressPct}
              onChange={e => {
                const pct = Number(e.target.value)
                const raw = (pct / 100) * (totalH - pageH)
                setOffset(Math.round(raw / pageH) * pageH)
              }}
            />
          </div>
        </div>
      )}

      {/* Page area */}
      <div className={`book-reader-wrap ${flipping || ''}`}>
        {/* Page curl overlay — animates on top during flip */}
        {flipping && (
          <div className={`page-curl page-curl-${flipping} ${flipDone ? 'curl-done' : ''}`} />
        )}

        {/* The visible window — overflow hidden, full text translated up */}
        <div className="page-window" ref={windowRef}>
          <div className="page-paper">
            {/* Binding shadow */}
            <div className="binding-shadow" />
            <div
              ref={innerRef}
              className="page-text-inner"
              style={{
                transform: `translateY(-${offset}px)`,
                fontSize,
                transition: flipping ? 'none' : undefined,
              }}
              dangerouslySetInnerHTML={{ __html: `<p>${displayHtml}</p>` }}
            />
          </div>
        </div>
      </div>

      {/* Navigation */}
      <div className="reader-nav">
        <button className={`nav-btn ${atStart ? 'disabled' : ''}`} onClick={goBack} disabled={atStart}>‹</button>
        <div className="reader-progress-info">
          <div className="progress-bar-reader">
            <div className="progress-fill-reader" style={{ width: `${Math.max(1, progressPct)}%` }} />
          </div>
          <span className="progress-text">{Math.min(100, progressPct)}% · Page {currentPage} of {totalPages}</span>
        </div>
        <button className={`nav-btn ${atEnd ? 'disabled' : ''}`} onClick={goForward} disabled={atEnd}>›</button>
      </div>

      {/* Highlight button — shown after text selection */}
      <button className="float-hl-btn" onClick={handleHighlight} onTouchEnd={e => { e.stopPropagation(); handleHighlight() }}>
        <span style={{ color: highlightColor }}>■</span> Highlight
      </button>

      {dictWord && (
        <Dictionary word={dictWord} position={dictPos} onClose={() => setDictWord(null)} nightMode={nightMode} />
      )}

      {showHighlightPanel && (
        <div className={`highlights-panel ${nightMode ? 'night' : ''}`} onClick={e => e.stopPropagation()}>
          <div className="panel-header">
            <h3>My Highlights</h3>
            <button onClick={() => setShowHighlightPanel(false)}>×</button>
          </div>
          {highlights.length === 0 && <p className="panel-empty">No highlights yet. Select text then tap Highlight.</p>}
          {highlights.map(hl => (
            <div key={hl.id} className="highlight-item" style={{ borderLeft: `4px solid ${hl.color}` }}>
              <p className="highlight-text">"{hl.text}"</p>
              <div className="highlight-meta">
                <button className="hl-remove" onClick={() => removeHighlight(hl.id)}>Remove</button>
              </div>
            </div>
          ))}
        </div>
      )}

      {showRecs && (
        <div className={`recs-panel ${nightMode ? 'night' : ''}`} onClick={e => e.stopPropagation()}>
          <div className="panel-header">
            <h3>Recommendations</h3>
            <button onClick={() => setShowRecs(false)}>×</button>
          </div>
          <Recommendations book={book} onSelect={b => { setShowRecs(false); onBack(b) }} nightMode={nightMode} />
        </div>
      )}
    </div>
  )
}
