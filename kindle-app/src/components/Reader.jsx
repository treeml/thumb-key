import React, { useEffect, useState, useRef, useCallback } from 'react'
import { App as CapApp } from '@capacitor/app'
import { fetchBookText } from '../utils/api'
import { useHighlights } from '../hooks/useHighlights'
import Dictionary from './Dictionary'
import Recommendations from './Recommendations'

const PAGE_PALETTES = [
  { id: 'cream',  bg: '#fdf6e3', text: '#2c1a06', label: 'Cream'  },
  { id: 'white',  bg: '#f5f5f0', text: '#111111', label: 'White'  },
  { id: 'sepia',  bg: '#f2dfc5', text: '#3a1a05', label: 'Sepia'  },
  { id: 'moss',   bg: '#e2ede0', text: '#162014', label: 'Moss'   },
  { id: 'dusk',   bg: '#dfe3f0', text: '#101830', label: 'Dusk'   },
]

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
  const [pageColorId, setPageColorId] = useState(
    () => localStorage.getItem('tome_page_color') || 'cream'
  )

  const pageColor = nightMode ? null : (PAGE_PALETTES.find(p => p.id === pageColorId) || PAGE_PALETTES[0])

  // DOM refs
  const wrapRef        = useRef(null) // .book-reader-wrap — used for height measurement
  const frontInnerRef  = useRef(null) // front layer text div — height measurement
  const backInnerRef   = useRef(null) // back layer text div — transform set directly
  const frontLayerRef  = useRef(null)
  const backLayerRef   = useRef(null)
  const foldStripRef   = useRef(null)

  // Value refs (avoid stale closure captures)
  const offsetRef   = useRef(0)
  const pageHRef    = useRef(0)
  const totalHRef   = useRef(0)
  const fontSizeRef = useRef(18)
  const dragRef     = useRef(null) // { dir, startX, lastX } or null

  const { highlights, addHighlight, removeHighlight } = useHighlights(book.id)

  useEffect(() => { offsetRef.current = offset }, [offset])
  useEffect(() => { pageHRef.current = pageH }, [pageH])
  useEffect(() => { totalHRef.current = totalH }, [totalH])
  useEffect(() => { fontSizeRef.current = fontSize }, [fontSize])

  // Save page color preference
  useEffect(() => {
    localStorage.setItem('tome_page_color', pageColorId)
  }, [pageColorId])

  // Load text
  useEffect(() => {
    setLoading(true); setError(null); setOffset(0)
    fetchBookText(book)
      .then(text => setFullText(text))
      .catch(e => setError(e.message))
      .finally(() => setLoading(false))
  }, [book.id])

  // Measure heights after render
  const measure = useCallback(() => {
    if (!wrapRef.current || !frontInnerRef.current) return
    const ph = wrapRef.current.clientHeight
    const th = frontInnerRef.current.scrollHeight
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
    if (wrapRef.current) ro.observe(wrapRef.current)
    return () => ro.disconnect()
  }, [measure])

  // Track reading progress
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
        if (showMenu)           { setShowMenu(false); return }
        if (showHighlightPanel) { setShowHighlightPanel(false); return }
        if (showRecs)           { setShowRecs(false); return }
        if (dictWord)           { setDictWord(null); return }
        onBack()
      }).then(h => { handle = h })
    } catch {}
    return () => { try { handle?.remove() } catch {} }
  }, [showMenu, showHighlightPanel, showRecs, dictWord, onBack])

  // Track text selection for the highlight button
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

  // ─── Page-turn engine — all DOM manipulation, no React setState during drag ───

  const applyDragVisuals = useCallback((dir, progress) => {
    const front = frontLayerRef.current
    const back  = backLayerRef.current
    const strip = foldStripRef.current
    const wrap  = wrapRef.current
    if (!front || !back || !wrap) return

    const w = wrap.offsetWidth
    // Center of the fold line (moves from edge toward opposite edge)
    const foldCenter = dir === 'fwd' ? w * (1 - progress) : w * progress
    // Fold strip width: sine curve — 0 at start/end, peaks ~70px at midpoint
    const stripW = Math.round(Math.sin(Math.min(1, progress) * Math.PI) * 70)
    const stripLeft  = Math.max(0, foldCenter - stripW / 2)
    const stripRight = Math.min(w, foldCenter + stripW / 2)

    if (dir === 'fwd') {
      // Back (next page): revealed on the left of the fold
      back.style.clipPath  = `polygon(0 0,${stripLeft}px 0,${stripLeft}px 100%,0 100%)`
      // Front (current page): remaining on the right of the fold
      front.style.clipPath = `polygon(${stripRight}px 0,100% 0,100% 100%,${stripRight}px 100%)`
    } else {
      // Backward: back page revealed on the right
      back.style.clipPath  = `polygon(${stripRight}px 0,100% 0,100% 100%,${stripRight}px 100%)`
      // Front: remaining on the left
      front.style.clipPath = `polygon(0 0,${stripLeft}px 0,${stripLeft}px 100%,0 100%)`
    }

    if (strip) {
      strip.style.left    = `${stripLeft}px`
      strip.style.width   = `${Math.max(1, stripW)}px`
      strip.style.opacity = progress > 0.005 ? '1' : '0'
    }
  }, [])

  const resetDragVisuals = useCallback(() => {
    if (frontLayerRef.current) frontLayerRef.current.style.clipPath = ''
    if (backLayerRef.current)  backLayerRef.current.style.clipPath  = ''
    if (foldStripRef.current) {
      foldStripRef.current.style.opacity = '0'
      foldStripRef.current.style.width   = '0'
    }
  }, [])

  // duration = ms for the animation; easing = ease-out quart
  const animateFold = useCallback((dir, fromP, toP, duration, onDone) => {
    const start = performance.now()
    const tick  = (now) => {
      const t = Math.min(1, (now - start) / duration)
      const e = 1 - Math.pow(1 - t, 4) // ease-out quart
      applyDragVisuals(dir, fromP + (toP - fromP) * e)
      if (t < 1) requestAnimationFrame(tick)
      else onDone?.()
    }
    requestAnimationFrame(tick)
  }, [applyDragVisuals])

  // Set the back layer's offset directly on the DOM (bypasses React render cycle)
  const setBackLayerOffset = useCallback((off) => {
    if (backInnerRef.current) {
      backInnerRef.current.style.transform = `translateY(-${off}px)`
      backInnerRef.current.style.fontSize  = `${fontSizeRef.current}px`
    }
  }, [])

  // Programmatic page turn (nav buttons, keyboard)
  const turnPage = useCallback((dir) => {
    if (dragRef.current) return
    const curOff = offsetRef.current
    const ph     = pageHRef.current
    const maxOff = Math.max(0, totalHRef.current - ph)
    if (dir === 'fwd' && curOff >= maxOff) return
    if (dir === 'bck' && curOff <= 0) return

    const next = dir === 'fwd' ? Math.min(curOff + ph, maxOff) : Math.max(curOff - ph, 0)
    setBackLayerOffset(next)
    dragRef.current = { dir, startX: 0, lastX: 0 }
    animateFold(dir, 0, 1, 560, () => {
      resetDragVisuals()
      dragRef.current = null
      setOffset(next)
    })
  }, [animateFold, resetDragVisuals, setBackLayerOffset])

  // Finish a touch drag — snap to complete or abort
  const finishDrag = useCallback((dir, progress) => {
    const THRESHOLD = 0.28
    const curOff = offsetRef.current
    const ph     = pageHRef.current
    const maxOff = Math.max(0, totalHRef.current - ph)

    if (progress >= THRESHOLD) {
      const next = dir === 'fwd' ? Math.min(curOff + ph, maxOff) : Math.max(curOff - ph, 0)
      const dur  = Math.max(160, (1 - progress) * 520) // shorter if already dragged far
      animateFold(dir, progress, 1, dur, () => {
        resetDragVisuals()
        dragRef.current = null
        setOffset(next)
      })
    } else {
      const dur = Math.max(120, progress * 380)
      animateFold(dir, progress, 0, dur, () => {
        resetDragVisuals()
        dragRef.current = null
      })
    }
  }, [animateFold, resetDragVisuals])

  // Non-passive touchmove (must preventDefault to stop scroll interference)
  useEffect(() => {
    const el = wrapRef.current
    if (!el) return
    const onMove = (e) => {
      const dr = dragRef.current
      if (!dr) return
      e.preventDefault()
      const x = e.touches[0].clientX
      dr.lastX = x
      const w = el.offsetWidth
      const progress = dr.dir === 'fwd'
        ? Math.max(0, Math.min(1, (dr.startX - x) / w))
        : Math.max(0, Math.min(1, (x  - dr.startX) / w))
      applyDragVisuals(dr.dir, progress)
    }
    el.addEventListener('touchmove', onMove, { passive: false })
    return () => el.removeEventListener('touchmove', onMove)
  }, [applyDragVisuals])

  const handleTouchStart = useCallback((e) => {
    if (dragRef.current) return
    if (e.target.closest?.('.reader-menu,.highlights-panel,.recs-panel,.dictionary-popup,.reader-topbar,.reader-nav')) return

    const x    = e.touches[0].clientX
    const w    = wrapRef.current?.offsetWidth || window.innerWidth
    const curOff = offsetRef.current
    const ph     = pageHRef.current
    const maxOff = Math.max(0, totalHRef.current - ph)

    // Allow drag from anywhere on screen (not just edges) for "grab the page" feel
    let dir = null
    if (x > w * 0.5 && curOff < maxOff) dir = 'fwd'
    else if (x <= w * 0.5 && curOff > 0) dir = 'bck'
    if (!dir) return

    const backOff = dir === 'fwd' ? Math.min(curOff + ph, maxOff) : Math.max(curOff - ph, 0)
    setBackLayerOffset(backOff)
    dragRef.current = { dir, startX: x, lastX: x }
    // Show fold strip at 0 progress (invisible) so clip-paths are initialised
    applyDragVisuals(dir, 0)
  }, [applyDragVisuals, setBackLayerOffset])

  const handleTouchEnd = useCallback((e) => {
    const dr = dragRef.current
    if (!dr) return
    const x = e.changedTouches[0].clientX
    const w = wrapRef.current?.offsetWidth || window.innerWidth
    const progress = dr.dir === 'fwd'
      ? Math.max(0, Math.min(1, (dr.startX - x) / w))
      : Math.max(0, Math.min(1, (x  - dr.startX) / w))

    // Quick flick (≥50px travel): treat as intent to turn
    const flick = Math.abs(dr.startX - x) >= 50
    finishDrag(dr.dir, flick ? Math.max(progress, 0.35) : progress)
  }, [finishDrag])

  // Keyboard nav
  useEffect(() => {
    const handler = (e) => {
      if (e.key === 'ArrowRight') turnPage('fwd')
      if (e.key === 'ArrowLeft')  turnPage('bck')
      if (e.key === 'Escape') { setShowMenu(false); setDictWord(null) }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [turnPage])

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

  const currentPage = pageH > 0 ? Math.floor(offset / pageH) + 1 : 1
  const totalPages  = pageH > 0 ? Math.ceil(totalH / pageH) : 1
  const progressPct = Math.round((offset / Math.max(totalH - pageH, 1)) * 100)

  const displayHtml = applyHighlights(fullText, highlights)
    .replace(/\n\n+/g, '</p><p>')
    .replace(/\n/g, '<br/>')
  const pageContent = `<p>${displayHtml}</p>`

  const paperStyle = pageColor ? { background: pageColor.bg, color: pageColor.text } : undefined
  const textStyle  = (off) => ({ transform: `translateY(-${off}px)`, fontSize })

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
          {!nightMode && (
            <div className="menu-section">
              <label className="menu-label">Page Color</label>
              <div className="page-color-swatches">
                {PAGE_PALETTES.map(p => (
                  <button
                    key={p.id}
                    className={`page-color-swatch ${pageColorId === p.id ? 'active' : ''}`}
                    style={{ background: p.bg, color: p.text }}
                    title={p.label}
                    onClick={() => setPageColorId(p.id)}
                  >
                    {pageColorId === p.id ? '✓' : ''}
                  </button>
                ))}
              </div>
            </div>
          )}
          <div className="menu-section">
            <label className="menu-label">Highlight Color</label>
            <div className="color-swatches">
              {['#FFD700', '#90EE90', '#87CEEB', '#FFB6C1', '#DDA0DD'].map(c => (
                <button key={c}
                  className={`color-swatch ${highlightColor === c ? 'active' : ''}`}
                  style={{ background: c }}
                  onClick={() => setHighlightColor(c)} />
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

      {/* ── Two-layer page area ── */}
      <div className="book-reader-wrap" ref={wrapRef}>

        {/* Back layer — page being revealed (offset set via DOM ref) */}
        <div className="page-layer page-layer-back" ref={backLayerRef}>
          <div className="page-paper" style={paperStyle}>
            <div className="binding-shadow" />
            <div
              ref={backInnerRef}
              className="page-text-inner"
              dangerouslySetInnerHTML={{ __html: pageContent }}
            />
          </div>
        </div>

        {/* Front layer — current page, clipped away as fold progresses */}
        <div className="page-layer page-layer-front" ref={frontLayerRef}>
          <div className="page-paper" style={paperStyle}>
            <div className="binding-shadow" />
            <div
              ref={frontInnerRef}
              className="page-text-inner"
              style={textStyle(offset)}
              dangerouslySetInnerHTML={{ __html: pageContent }}
            />
          </div>
        </div>

        {/* Fold strip — the physical crease that travels across the page */}
        <div className="fold-strip" ref={foldStripRef} />

      </div>

      {/* Navigation */}
      <div className="reader-nav">
        <button className={`nav-btn ${atStart ? 'disabled' : ''}`}
          onClick={() => turnPage('bck')} disabled={atStart}>‹</button>
        <div className="reader-progress-info">
          <div className="progress-bar-reader">
            <div className="progress-fill-reader" style={{ width: `${Math.max(1, progressPct)}%` }} />
          </div>
          <span className="progress-text">{Math.min(100, progressPct)}% · Page {currentPage} of {totalPages}</span>
        </div>
        <button className={`nav-btn ${atEnd ? 'disabled' : ''}`}
          onClick={() => turnPage('fwd')} disabled={atEnd}>›</button>
      </div>

      {/* Highlight button — only when text is selected */}
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
