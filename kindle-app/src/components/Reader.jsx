import React, { useEffect, useState, useRef, useCallback } from 'react'
import { App as CapApp } from '@capacitor/app'
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
  const [offset, setOffset]       = useState(0)
  const [pageH, setPageH]         = useState(0)
  const [totalH, setTotalH]       = useState(0)
  const [showMenu, setShowMenu]   = useState(false)
  const [dictWord, setDictWord]   = useState(null)
  const [dictPos, setDictPos]     = useState(null)
  const [showRecs, setShowRecs]   = useState(false)
  const [highlightColor, setHighlightColor] = useState('#FFD700')
  const [fontSize, setFontSize]   = useState(18)
  const [showHighlightPanel, setShowHighlightPanel] = useState(false)
  const [hasSelection, setHasSelection] = useState(false)

  const windowRef   = useRef(null)
  const innerRef    = useRef(null)
  const frontRef    = useRef(null)  // front page layer element
  const backRef     = useRef(null)  // back page layer element
  const foldRef     = useRef(null)  // fold shadow strip
  const dragState   = useRef(null)  // { dir, startX, lastX, backOffset }
  const offsetRef   = useRef(0)     // mirrors offset for use in event handlers
  const pageHRef    = useRef(0)
  const totalHRef   = useRef(0)

  const { highlights, addHighlight, removeHighlight } = useHighlights(book.id)

  // Keep refs in sync
  useEffect(() => { offsetRef.current = offset }, [offset])
  useEffect(() => { pageHRef.current = pageH }, [pageH])
  useEffect(() => { totalHRef.current = totalH }, [totalH])

  // Load text
  useEffect(() => {
    setLoading(true); setError(null); setOffset(0)
    fetchBookText(book)
      .then(text => setFullText(text))
      .catch(e => setError(e.message))
      .finally(() => setLoading(false))
  }, [book.id])

  // Measure heights
  const measure = useCallback(() => {
    if (!windowRef.current || !innerRef.current) return
    const ph = windowRef.current.clientHeight
    const th = innerRef.current.scrollHeight
    setPageH(ph)
    setTotalH(th)
    if (initialProgress > 0 && offsetRef.current === 0) {
      const target = Math.round((initialProgress / 100) * (th - ph))
      const snapped = Math.round(target / ph) * ph
      setOffset(Math.max(0, Math.min(snapped, th - ph)))
    }
  }, [initialProgress])

  useEffect(() => {
    if (!fullText) return
    const id = requestAnimationFrame(() => measure())
    return () => cancelAnimationFrame(id)
  }, [fullText, fontSize, measure])

  useEffect(() => {
    const ro = new ResizeObserver(measure)
    if (windowRef.current) ro.observe(windowRef.current)
    return () => ro.disconnect()
  }, [measure])

  // Track progress
  useEffect(() => {
    if (!pageH || !totalH) return
    const pct = Math.round((offset / Math.max(totalH - pageH, 1)) * 100)
    setProgress(book.id, Math.min(100, Math.max(0, pct)))
  }, [offset, pageH, totalH])

  // Android back button
  useEffect(() => {
    let handle
    try {
      CapApp.addListener('backButton', () => {
        if (showMenu)            { setShowMenu(false); return }
        if (showHighlightPanel)  { setShowHighlightPanel(false); return }
        if (showRecs)            { setShowRecs(false); return }
        if (dictWord)            { setDictWord(null); return }
        onBack()
      }).then(h => { handle = h })
    } catch {}
    return () => { try { handle?.remove() } catch {} }
  }, [showMenu, showHighlightPanel, showRecs, dictWord, onBack])

  // Track text selection for highlight button
  useEffect(() => {
    const update = () => {
      const sel = window.getSelection()?.toString().trim()
      setHasSelection(!!(sel && sel.length > 1))
    }
    document.addEventListener('selectionchange', update)
    return () => document.removeEventListener('selectionchange', update)
  }, [])

  const maxOffset = Math.max(0, totalH - pageH)
  const atStart   = offset <= 0
  const atEnd     = offset >= maxOffset

  // ───── Drag page-turn helpers ─────

  // Directly update clip-paths on DOM refs (no React setState during drag)
  const applyDragVisuals = useCallback((dir, progress) => {
    const front = frontRef.current
    const back  = backRef.current
    const fold  = foldRef.current
    if (!front || !back) return
    const w = windowRef.current.offsetWidth
    const foldX = dir === 'fwd' ? w * (1 - progress) : w * progress

    if (dir === 'fwd') {
      front.style.clipPath = `polygon(0 0,${foldX}px 0,${foldX}px 100%,0 100%)`
      back.style.clipPath  = `polygon(${foldX}px 0,100% 0,100% 100%,${foldX}px 100%)`
    } else {
      front.style.clipPath = `polygon(${foldX}px 0,100% 0,100% 100%,${foldX}px 100%)`
      back.style.clipPath  = `polygon(0 0,${foldX}px 0,${foldX}px 100%,0 100%)`
    }
    if (fold) {
      fold.style.left    = `${foldX - 18}px`
      fold.style.opacity = String(Math.min(1, progress * 4))
    }
  }, [])

  const resetDragVisuals = useCallback(() => {
    if (frontRef.current) frontRef.current.style.clipPath = ''
    if (backRef.current)  backRef.current.style.clipPath  = ''
    if (foldRef.current)  foldRef.current.style.opacity   = '0'
  }, [])

  const animateToProgress = useCallback((dir, fromP, toP, onDone) => {
    const duration = Math.max(80, Math.abs(toP - fromP) * 280)
    const start = performance.now()
    const tick = (now) => {
      const t = Math.min(1, (now - start) / duration)
      // ease-out cubic
      const e = 1 - Math.pow(1 - t, 3)
      applyDragVisuals(dir, fromP + (toP - fromP) * e)
      if (t < 1) requestAnimationFrame(tick)
      else onDone()
    }
    requestAnimationFrame(tick)
  }, [applyDragVisuals])

  // Commit or abort after finger-up
  const finishDrag = useCallback((dir, progress) => {
    const THRESHOLD = 0.3
    const curOff = offsetRef.current
    const ph     = pageHRef.current
    const maxOff = Math.max(0, totalHRef.current - ph)

    if (progress >= THRESHOLD) {
      animateToProgress(dir, progress, 1.0, () => {
        const next = dir === 'fwd'
          ? Math.min(curOff + ph, maxOff)
          : Math.max(curOff - ph, 0)
        resetDragVisuals()
        dragState.current = null
        setOffset(next)
      })
    } else {
      animateToProgress(dir, progress, 0, () => {
        resetDragVisuals()
        dragState.current = null
      })
    }
  }, [animateToProgress, resetDragVisuals])

  // Attach non-passive touchmove to the window element
  useEffect(() => {
    const el = windowRef.current
    if (!el) return
    const onMove = (e) => {
      const ds = dragState.current
      if (!ds) return
      e.preventDefault()
      const x = e.touches[0].clientX
      ds.lastX = x
      const w = el.offsetWidth
      const progress = ds.dir === 'fwd'
        ? Math.max(0, Math.min(1, (ds.startX - x) / w))
        : Math.max(0, Math.min(1, (x - ds.startX) / w))
      applyDragVisuals(ds.dir, progress)
    }
    el.addEventListener('touchmove', onMove, { passive: false })
    return () => el.removeEventListener('touchmove', onMove)
  }, [applyDragVisuals])

  const handleTouchStart = useCallback((e) => {
    if (dragState.current) return
    // Ignore touches inside menus/panels
    if (e.target.closest?.('.reader-menu,.highlights-panel,.recs-panel,.dictionary-popup,.reader-topbar,.reader-nav')) return

    const x = e.touches[0].clientX
    const w = windowRef.current?.offsetWidth || window.innerWidth
    const curOff = offsetRef.current
    const ph     = pageHRef.current
    const maxOff = Math.max(0, totalHRef.current - ph)

    let dir = null
    if (x > w * 0.55 && curOff < maxOff) dir = 'fwd'
    else if (x < w * 0.45 && curOff > 0) dir = 'bck'
    if (!dir) return

    const backOffset = dir === 'fwd'
      ? Math.min(curOff + ph, maxOff)
      : Math.max(curOff - ph, 0)

    dragState.current = { dir, startX: x, lastX: x, backOffset }
  }, [])

  const handleTouchEnd = useCallback((e) => {
    const ds = dragState.current
    if (!ds) return
    const x = e.changedTouches[0].clientX
    const w = windowRef.current?.offsetWidth || window.innerWidth

    // Quick swipe detection (no drag layer yet) — treat as instant flip
    const rawProgress = ds.dir === 'fwd'
      ? (ds.startX - x) / w
      : (x - ds.startX) / w

    const progress = Math.max(0, Math.min(1, rawProgress))
    finishDrag(ds.dir, Math.max(progress, Math.abs(ds.startX - x) > 50 ? 0.4 : 0))
  }, [finishDrag])

  // Keyboard nav
  useEffect(() => {
    const handler = (e) => {
      const maxOff = Math.max(0, totalHRef.current - pageHRef.current)
      if (e.key === 'ArrowRight') {
        const next = Math.min(offsetRef.current + pageHRef.current, maxOff)
        animateToProgress('fwd', 0, 1, () => { resetDragVisuals(); dragState.current = null; setOffset(next) })
        dragState.current = { dir: 'fwd', startX: 0, lastX: 0, backOffset: next }
      }
      if (e.key === 'ArrowLeft') {
        const next = Math.max(offsetRef.current - pageHRef.current, 0)
        animateToProgress('bck', 0, 1, () => { resetDragVisuals(); dragState.current = null; setOffset(next) })
        dragState.current = { dir: 'bck', startX: 0, lastX: 0, backOffset: next }
      }
      if (e.key === 'Escape') { setShowMenu(false); setDictWord(null) }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [animateToProgress, resetDragVisuals])

  const handleTextClick = (e) => {
    if (e.target.closest('.reader-menu,.highlights-panel,.recs-panel,.dictionary-popup')) return
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
    setHasSelection(false)
    setDictWord(null)
  }

  const currentPage  = pageH > 0 ? Math.floor(offset / pageH) + 1 : 1
  const totalPages   = pageH > 0 ? Math.ceil(totalH / pageH) : 1
  const progressPct  = Math.round((offset / Math.max(totalH - pageH, 1)) * 100)

  const displayHtml = applyHighlights(fullText, highlights)
    .replace(/\n\n+/g, '</p><p>')
    .replace(/\n/g, '<br/>')

  const backOffset = dragState.current
    ? dragState.current.backOffset
    : offset < maxOffset ? offset + pageH : offset - pageH

  const pageStyle = (off) => ({
    transform: `translateY(-${off}px)`,
    fontSize,
  })

  const pageContent = `<p>${displayHtml}</p>`

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

      {/* Page area — two stacked layers for drag flip */}
      <div className="book-reader-wrap" ref={windowRef}>
        {/* Back layer: page being revealed */}
        <div className="page-layer page-layer-back" ref={backRef}>
          <div className="page-paper">
            <div className="binding-shadow" />
            <div
              className="page-text-inner"
              style={pageStyle(backOffset)}
              dangerouslySetInnerHTML={{ __html: pageContent }}
            />
          </div>
        </div>

        {/* Front layer: current page, gets clipped during drag */}
        <div className="page-layer page-layer-front" ref={frontRef}>
          <div className="page-paper">
            <div className="binding-shadow" />
            <div
              ref={innerRef}
              className="page-text-inner"
              style={pageStyle(offset)}
              dangerouslySetInnerHTML={{ __html: pageContent }}
            />
          </div>
        </div>

        {/* Fold shadow strip */}
        <div className="fold-shadow" ref={foldRef} />
      </div>

      {/* Navigation */}
      <div className="reader-nav">
        <button className={`nav-btn ${atStart ? 'disabled' : ''}`}
          onClick={() => {
            if (atStart || dragState.current) return
            dragState.current = { dir: 'bck', startX: 0, lastX: 0, backOffset: Math.max(offset - pageH, 0) }
            finishDrag('bck', 1)
          }} disabled={atStart}>‹</button>
        <div className="reader-progress-info">
          <div className="progress-bar-reader">
            <div className="progress-fill-reader" style={{ width: `${Math.max(1, progressPct)}%` }} />
          </div>
          <span className="progress-text">{Math.min(100, progressPct)}% · Page {currentPage} of {totalPages}</span>
        </div>
        <button className={`nav-btn ${atEnd ? 'disabled' : ''}`}
          onClick={() => {
            if (atEnd || dragState.current) return
            dragState.current = { dir: 'fwd', startX: 0, lastX: 0, backOffset: Math.min(offset + pageH, maxOffset) }
            finishDrag('fwd', 1)
          }} disabled={atEnd}>›</button>
      </div>

      {/* Highlight button — only visible when text is selected */}
      {hasSelection && (
        <button className="float-hl-btn" onClick={handleHighlight}
          onTouchEnd={e => { e.stopPropagation(); handleHighlight() }}>
          <span style={{ color: highlightColor }}>■</span> Highlight
        </button>
      )}

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
