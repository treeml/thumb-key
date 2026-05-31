import React, { useEffect, useState, useRef, useCallback, useLayoutEffect } from 'react'
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
  const [fontSize, setFontSize]   = useState(() => Number(localStorage.getItem('tome_font_size') || 18))
  const [showHighlightPanel, setShowHighlightPanel] = useState(false)
  const [hasSelection, setHasSelection] = useState(false)
  const [pageColorId, setPageColorId] = useState(
    () => localStorage.getItem('tome_page_color') || 'cream'
  )

  const pageColor = nightMode
    ? null
    : (PAGE_PALETTES.find(p => p.id === pageColorId) || PAGE_PALETTES[0])

  // DOM refs
  const wrapRef       = useRef(null)  // .book-reader-wrap
  const frontLayerRef = useRef(null)  // current page layer (gets clip-path)
  const frontInnerRef = useRef(null)  // current page text div (height measurement)
  const backLayerRef  = useRef(null)  // revealed page layer (always full-width, behind)
  const backInnerRef  = useRef(null)  // revealed page text div (transform set via DOM)
  const foldRef       = useRef(null)  // 3-D fold element (the turning portion)
  const foldInnerRef  = useRef(null)  // text div inside the fold element

  // Value refs — let event handlers read current values without stale closures
  const offsetRef      = useRef(0)
  const pageHRef       = useRef(0)
  const totalHRef      = useRef(0)
  const fontSizeRef    = useRef(18)
  const pageColorRef   = useRef(pageColor)
  const dragRef        = useRef(null)  // { dir, startX, lastX } | null
  const pendingReset   = useRef(false) // set true when we need resetDragVisuals after React commit

  const { highlights, addHighlight, removeHighlight } = useHighlights(book.id)

  useEffect(() => { offsetRef.current = offset }, [offset])
  useEffect(() => { pageHRef.current = pageH }, [pageH])
  useEffect(() => { totalHRef.current = totalH }, [totalH])
  useEffect(() => { fontSizeRef.current = fontSize }, [fontSize])
  useEffect(() => { pageColorRef.current = pageColor }, [pageColor])
  useEffect(() => { localStorage.setItem('tome_page_color', pageColorId) }, [pageColorId])
  useEffect(() => { localStorage.setItem('tome_font_size', String(fontSize)) }, [fontSize])

  // Load text
  useEffect(() => {
    setLoading(true); setError(null); setOffset(0)
    fetchBookText(book)
      .then(text => setFullText(text))
      .catch(e => setError(e.message))
      .finally(() => setLoading(false))
  }, [book.id])

  // Measure using the page LAYER (not wrap) so padding is excluded
  const measure = useCallback(() => {
    if (!frontLayerRef.current || !frontInnerRef.current) return
    const ph = frontLayerRef.current.clientHeight   // ← page layer, not wrap
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
    if (frontLayerRef.current) ro.observe(frontLayerRef.current)
    return () => ro.disconnect()
  }, [measure])

  // Save progress
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

  // Selection tracking for highlight button
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

  // ─── Page-turn engine ───────────────────────────────────────────────────────

  // Update all three layers directly on the DOM (no React setState during drag)
  const applyDragVisuals = useCallback((dir, progress) => {
    const front     = frontLayerRef.current
    const back      = backLayerRef.current
    const fold      = foldRef.current
    const foldInner = foldInnerRef.current
    const wrap      = wrapRef.current
    if (!front || !back || !fold || !wrap) return

    const w = wrap.offsetWidth
    const curOff = offsetRef.current
    const fs     = fontSizeRef.current
    const col    = pageColorRef.current?.text || ''

    // Anchor fold crease to where the touch started so it follows the finger.
    // For programmatic turns (buttons) startX defaults to the screen edge.
    const dr = dragRef.current
    const sx = dr?.startX ?? (dir === 'fwd' ? w : 0)

    if (dir === 'fwd') {
      // Crease sweeps from sx → 0 as progress goes 0 → 1
      // Result: foldX === currentX (crease is exactly at the finger)
      const foldX = sx * (1 - progress)
      const foldW = Math.max(0, w - foldX)

      front.style.clipPath = foldX > 0
        ? `polygon(0 0,${foldX}px 0,${foldX}px 100%,0 100%)`
        : 'polygon(0 0,0 0,0 100%,0 100%)'

      fold.style.left            = `${foldX}px`
      fold.style.width           = `${foldW}px`
      fold.style.transformOrigin = '0% 50%'
      fold.style.transform       = `perspective(600px) rotateY(${-progress * 90}deg)`
      fold.style.filter          = `brightness(${1 - progress * 0.5})`
      fold.style.opacity         = progress > 0.005 ? '1' : '0'

      if (foldInner) {
        foldInner.style.transform = `translateY(-${curOff}px)`
        foldInner.style.left      = `-${foldX}px`
        foldInner.style.width     = `${w}px`
        foldInner.style.fontSize  = `${fs}px`
        if (col) foldInner.style.color = col
      }

    } else {
      // Crease sweeps from sx → w as progress goes 0 → 1
      const foldW = Math.max(0, sx + progress * (w - sx))

      front.style.clipPath = foldW < w
        ? `polygon(${foldW}px 0,100% 0,100% 100%,${foldW}px 100%)`
        : 'polygon(100% 0,100% 0,100% 100%,100% 100%)'

      fold.style.left            = '0'
      fold.style.width           = `${foldW}px`
      fold.style.transformOrigin = '100% 50%'
      fold.style.transform       = `perspective(600px) rotateY(${progress * 90}deg)`
      fold.style.filter          = `brightness(${1 - progress * 0.5})`
      fold.style.opacity         = progress > 0.005 ? '1' : '0'

      if (foldInner) {
        foldInner.style.transform = `translateY(-${curOff}px)`
        foldInner.style.left      = '0'
        foldInner.style.width     = `${w}px`
        foldInner.style.fontSize  = `${fs}px`
        if (col) foldInner.style.color = col
      }
    }

    back.style.clipPath = ''
  }, [])

  const resetDragVisuals = useCallback(() => {
    if (frontLayerRef.current) frontLayerRef.current.style.clipPath = ''
    if (foldRef.current) {
      foldRef.current.style.opacity   = '0'
      foldRef.current.style.transform = ''
      foldRef.current.style.filter    = ''
      foldRef.current.style.width     = '0'
    }
  }, [])

  // useLayoutEffect runs synchronously after React commits DOM changes.
  // This is how we reset clip-paths AFTER the new offset is painted, preventing the
  // one-frame flicker where the front layer shows old content with no clip.
  useLayoutEffect(() => {
    if (pendingReset.current) {
      pendingReset.current = false
      resetDragVisuals()
    }
  }, [offset, resetDragVisuals])

  // Set the back layer's scroll offset directly on the DOM
  const setBackLayerOffset = useCallback((off) => {
    if (backInnerRef.current) {
      backInnerRef.current.style.transform = `translateY(-${off}px)`
      backInnerRef.current.style.fontSize  = `${fontSizeRef.current}px`
      const col = pageColorRef.current?.text || ''
      if (col) backInnerRef.current.style.color = col
    }
  }, [])

  // Animate fold from one progress value to another, then call onDone
  const animateFold = useCallback((dir, fromP, toP, duration, onDone) => {
    const start = performance.now()
    const tick  = (now) => {
      const t = Math.min(1, (now - start) / duration)
      const e = 1 - Math.pow(1 - t, 4)  // ease-out quart
      applyDragVisuals(dir, fromP + (toP - fromP) * e)
      if (t < 1) requestAnimationFrame(tick)
      else onDone?.()
    }
    requestAnimationFrame(tick)
  }, [applyDragVisuals])

  // Programmatic page turn (buttons, keyboard)
  const turnPage = useCallback((dir) => {
    if (dragRef.current) return
    const curOff = offsetRef.current
    const ph     = pageHRef.current
    const maxOff = Math.max(0, totalHRef.current - ph)
    if (dir === 'fwd' && curOff >= maxOff) return
    if (dir === 'bck' && curOff <= 0) return

    const next = dir === 'fwd' ? Math.min(curOff + ph, maxOff) : Math.max(curOff - ph, 0)
    const w = wrapRef.current?.offsetWidth || window.innerWidth
    setBackLayerOffset(next)
    dragRef.current = { dir, startX: dir === 'fwd' ? w : 0, lastX: 0 }

    animateFold(dir, 0, 1, 960, () => {
      dragRef.current    = null
      pendingReset.current = true
      setOffset(next)   // triggers re-render → useLayoutEffect resets clips
    })
  }, [animateFold, setBackLayerOffset])

  // Finish a touch drag — snap to complete or abort
  const finishDrag = useCallback((dir, progress) => {
    const THRESHOLD = 0.27
    const curOff = offsetRef.current
    const ph     = pageHRef.current
    const maxOff = Math.max(0, totalHRef.current - ph)

    if (progress >= THRESHOLD) {
      const next = dir === 'fwd' ? Math.min(curOff + ph, maxOff) : Math.max(curOff - ph, 0)
      const dur  = Math.max(250, (1 - progress) * 960)
      animateFold(dir, progress, 1, dur, () => {
        dragRef.current      = null
        pendingReset.current = true
        setOffset(next)
      })
    } else {
      const dur = Math.max(180, progress * 600)
      animateFold(dir, progress, 0, dur, () => {
        dragRef.current = null
        resetDragVisuals()
      })
    }
  }, [animateFold, resetDragVisuals])

  // Non-passive touchmove listener attached imperatively (must preventDefault)
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
      // Normalise by distance from startX to the target edge so foldX === x
      const progress = dr.dir === 'fwd'
        ? Math.max(0, Math.min(1, (dr.startX - x) / Math.max(dr.startX, 1)))
        : Math.max(0, Math.min(1, (x - dr.startX) / Math.max(w - dr.startX, 1)))
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

    let dir = null
    if (x > w * 0.5 && curOff < maxOff) dir = 'fwd'
    else if (x <= w * 0.5 && curOff > 0) dir = 'bck'
    if (!dir) return

    const backOff = dir === 'fwd'
      ? Math.min(curOff + ph, maxOff)
      : Math.max(curOff - ph, 0)

    setBackLayerOffset(backOff)
    dragRef.current = { dir, startX: x, lastX: x }
    applyDragVisuals(dir, 0)
  }, [applyDragVisuals, setBackLayerOffset])

  const handleTouchEnd = useCallback((e) => {
    const dr = dragRef.current
    if (!dr) return
    const x = e.changedTouches[0].clientX
    const w = wrapRef.current?.offsetWidth || window.innerWidth
    const progress = dr.dir === 'fwd'
      ? Math.max(0, Math.min(1, (dr.startX - x) / Math.max(dr.startX, 1)))
      : Math.max(0, Math.min(1, (x - dr.startX) / Math.max(w - dr.startX, 1)))

    const flick = Math.abs(dr.startX - x) >= 50
    finishDrag(dr.dir, flick ? Math.max(progress, 0.35) : progress)
  }, [finishDrag])

  // Keyboard navigation
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
    addHighlight(sel, pageH > 0 ? Math.floor(offset / pageH) : 0, highlightColor)
    window.getSelection().removeAllRanges()
    setHasSelection(false)
    setDictWord(null)
  }

  const currentPage = pageH > 0 ? Math.floor(offset / pageH) + 1 : 1
  const totalPages  = pageH > 0 ? Math.ceil(totalH / pageH) : 1
  const progressPct = Math.round((offset / Math.max(totalH - pageH, 1)) * 100)

  // Reflow Gutenberg text: paragraphs split on blank lines, single newlines → space
  // (Gutenberg wraps at ~70 chars; those hard breaks look terrible on a phone)
  const pageContent = (() => {
    const raw = applyHighlights(fullText, highlights)
      .replace(/\r\n/g, '\n').replace(/\r/g, '\n')
    return raw
      .split(/\n{2,}/)
      .map(block => {
        const text = block.replace(/\n/g, ' ').replace(/\s+/g, ' ').trim()
        return text ? `<p>${text}</p>` : ''
      })
      .filter(Boolean)
      .join('')
  })()

  const paperStyle = pageColor ? { background: pageColor.bg, color: pageColor.text } : undefined
  const frontStyle = { transform: `translateY(-${offset}px)`, fontSize }

  if (loading) return (
    <div className={`reader-loading ${nightMode ? 'night' : ''}`}>
      <div className="reader-loading-book">
        <div className="loading-page loading-page-1" />
        <div className="loading-page loading-page-2" />
        <div className="loading-page loading-page-3" />
      </div>
      <p>Loading book…</p>
    </div>
  )
  if (error) return (
    <div className={`reader-error ${nightMode ? 'night' : ''}`}>
      <div className="error-icon">📚</div>
      <h3>Couldn't load this book</h3>
      <p>{error}</p>
      <button className="btn-primary" onClick={onBack}>Go Back</button>
    </div>
  )

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
        <button className="reader-menu-btn"
          onClick={e => { e.stopPropagation(); setShowMenu(p => !p) }}>⋮</button>
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
                  <button key={p.id}
                    className={`page-color-swatch ${pageColorId === p.id ? 'active' : ''}`}
                    style={{ background: p.bg, color: p.text }}
                    title={p.label}
                    onClick={() => setPageColorId(p.id)}>
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
          <button className="menu-item"
            onClick={() => { setShowHighlightPanel(p => !p); setShowMenu(false) }}>
            📌 My Highlights ({highlights.length})
          </button>
          <button className="menu-item"
            onClick={() => { setShowRecs(p => !p); setShowMenu(false) }}>
            ✨ Recommendations
          </button>
          <div className="menu-section">
            <label className="menu-label">Jump to position</label>
            <input type="range" min="0" max="100" value={progressPct}
              onChange={e => {
                const pct = Number(e.target.value)
                const raw = (pct / 100) * (totalH - pageH)
                setOffset(Math.round(raw / pageH) * pageH)
              }} />
          </div>
        </div>
      )}

      {/* ── Three-layer page area ── */}
      <div className="book-reader-wrap" ref={wrapRef}>

        {/* Layer 1 (bottom): The page being revealed — always full-width behind everything */}
        <div className="page-layer page-layer-back" ref={backLayerRef}>
          <div className="page-paper" style={paperStyle}>
            <div className="binding-shadow" />
            {/* No style prop — transform set entirely via backInnerRef DOM ref */}
            <div ref={backInnerRef} className="page-text-inner"
              dangerouslySetInnerHTML={{ __html: pageContent }} />
          </div>
        </div>

        {/* Layer 2: Current page — clipped to unturned portion */}
        <div className="page-layer page-layer-front" ref={frontLayerRef}>
          <div className="page-paper" style={paperStyle}>
            <div className="binding-shadow" />
            <div ref={frontInnerRef} className="page-text-inner"
              style={frontStyle}
              dangerouslySetInnerHTML={{ __html: pageContent }} />
          </div>
        </div>

        {/* Layer 3: The turning portion — 3D perspective rotateY so text curls with the fold */}
        <div className="page-fold" ref={foldRef}>
          <div className="page-paper page-paper-fold" style={paperStyle}>
            <div className="binding-shadow" />
            {/* No style prop — all managed via foldInnerRef DOM ref */}
            <div ref={foldInnerRef} className="page-text-inner page-text-fold"
              dangerouslySetInnerHTML={{ __html: pageContent }} />
          </div>
          {/* Edge shadow that darkens the fold crease */}
          <div className="fold-edge-shadow" />
        </div>

      </div>

      {/* Navigation */}
      <div className="reader-nav">
        <button className={`nav-btn ${atStart ? 'disabled' : ''}`}
          onClick={() => turnPage('bck')} disabled={atStart}>‹</button>
        <div className="reader-progress-info">
          <div className="progress-bar-reader">
            <div className="progress-fill-reader" style={{ width: `${Math.max(1, progressPct)}%` }} />
          </div>
          <span className="progress-text">
            {Math.min(100, progressPct)}% · Page {currentPage} of {totalPages}
          </span>
        </div>
        <button className={`nav-btn ${atEnd ? 'disabled' : ''}`}
          onClick={() => turnPage('fwd')} disabled={atEnd}>›</button>
      </div>

      {hasSelection && (
        <button className="float-hl-btn" onClick={handleHighlight}
          onTouchEnd={e => { e.stopPropagation(); handleHighlight() }}>
          <span style={{ color: highlightColor }}>■</span> Highlight
        </button>
      )}

      {dictWord && (
        <Dictionary word={dictWord} position={dictPos}
          onClose={() => setDictWord(null)} nightMode={nightMode} />
      )}

      {showHighlightPanel && (
        <div className={`highlights-panel ${nightMode ? 'night' : ''}`}
          onClick={e => e.stopPropagation()}>
          <div className="panel-header">
            <h3>My Highlights</h3>
            <button onClick={() => setShowHighlightPanel(false)}>×</button>
          </div>
          {highlights.length === 0 &&
            <p className="panel-empty">No highlights yet. Select text then tap Highlight.</p>}
          {highlights.map(hl => (
            <div key={hl.id} className="highlight-item"
              style={{ borderLeft: `4px solid ${hl.color}` }}>
              <p className="highlight-text">"{hl.text}"</p>
              <div className="highlight-meta">
                <button className="hl-remove" onClick={() => removeHighlight(hl.id)}>Remove</button>
              </div>
            </div>
          ))}
        </div>
      )}

      {showRecs && (
        <div className={`recs-panel ${nightMode ? 'night' : ''}`}
          onClick={e => e.stopPropagation()}>
          <div className="panel-header">
            <h3>Recommendations</h3>
            <button onClick={() => setShowRecs(false)}>×</button>
          </div>
          <Recommendations book={book}
            onSelect={b => { setShowRecs(false); onBack(b) }}
            nightMode={nightMode} />
        </div>
      )}
    </div>
  )
}
