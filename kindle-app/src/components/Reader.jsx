import React, { useEffect, useState, useRef, useCallback, useLayoutEffect, useMemo } from 'react'
import { App as CapApp } from '@capacitor/app'
import { fetchBookText } from '../utils/api'
import { useHighlights } from '../hooks/useHighlights'
import { detectChapters } from '../utils/epubParser'
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
  const [showToc,  setShowToc]    = useState(false)
  const [highlightColor, setHighlightColor] = useState('#FFD700')
  const [fontSize, setFontSize]   = useState(() => Number(localStorage.getItem('tome_font_size') || 18))
  const [showHighlightPanel, setShowHighlightPanel] = useState(false)
  const [hasSelection, setHasSelection] = useState(false)
  const [autoToc, setAutoToc]     = useState([])
  const [showChrome, setShowChrome] = useState(false)
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
  const animCanvasRef  = useRef(null)  // canvas overlay for page turn edge shadow

  // Value refs — let event handlers read current values without stale closures
  const offsetRef      = useRef(0)
  const pageHRef       = useRef(0)
  const totalHRef      = useRef(0)
  const fontSizeRef    = useRef(18)
  const pageColorRef   = useRef(pageColor)
  const dragRef        = useRef(null)  // { dir, startX, lastX } | null
  const pendingReset   = useRef(false) // set true when we need resetDragVisuals after React commit

  const hasRestored    = useRef(false)
  const paddingTopRef  = useRef(20)   // measured from DOM; updated in measure()
  const lineHRef       = useRef(33)   // fontSize * 1.85, rounded
  const showChromeRef  = useRef(false)
  const chromeTimerRef = useRef(null)
  const pendingTurnRef = useRef(null)  // { dir, startX, startY } — intent not yet confirmed
  const lastSelRef     = useRef(null)  // last valid selection text (survives tap-to-dismiss)
  const lastSelPosRef  = useRef(null)  // bounding rect centre of last selection

  const { highlights, addHighlight, removeHighlight } = useHighlights(book.id)

  useEffect(() => { offsetRef.current = offset }, [offset])
  useEffect(() => { pageHRef.current = pageH }, [pageH])
  useEffect(() => { totalHRef.current = totalH }, [totalH])
  useEffect(() => { fontSizeRef.current = fontSize; lineHRef.current = Math.round(fontSize * 1.85) }, [fontSize])
  useEffect(() => { pageColorRef.current = pageColor }, [pageColor])
  useEffect(() => { localStorage.setItem('tome_page_color', pageColorId) }, [pageColorId])
  useEffect(() => { localStorage.setItem('tome_font_size', String(fontSize)) }, [fontSize])

  const setChrome = useCallback((val) => {
    showChromeRef.current = val
    setShowChrome(val)
  }, [])

  const showAndAutoHideChrome = useCallback(() => {
    showChromeRef.current = true
    setShowChrome(true)
    if (chromeTimerRef.current) clearTimeout(chromeTimerRef.current)
    chromeTimerRef.current = setTimeout(() => {
      showChromeRef.current = false
      setShowChrome(false)
    }, 4000)
  }, [])

  // Keep chrome visible while any panel is open; restart timer when panels close
  useEffect(() => {
    if (showMenu || showToc || showHighlightPanel || showRecs || dictWord) {
      if (chromeTimerRef.current) clearTimeout(chromeTimerRef.current)
      showChromeRef.current = true
      setShowChrome(true)
    } else if (showChromeRef.current) {
      chromeTimerRef.current = setTimeout(() => {
        showChromeRef.current = false
        setShowChrome(false)
      }, 4000)
    }
  }, [showMenu, showToc, showHighlightPanel, showRecs, dictWord])

  useEffect(() => () => { if (chromeTimerRef.current) clearTimeout(chromeTimerRef.current) }, [])

  // The active TOC is from the book object (epub/local) or auto-detected for Gutenberg
  const activeToc = (book.toc?.length > 0) ? book.toc : autoToc

  // Load text — cancelled flag prevents stale async updates when book.id changes mid-flight
  useEffect(() => {
    let cancelled = false
    setLoading(true); setError(null); setOffset(0); setAutoToc([])
    hasRestored.current = false
    fetchBookText(book)
      .then(text => {
        if (cancelled) return
        setFullText(text)
        if (book.contentFormat !== 'html' && book.source !== 'local') {
          const chapters = detectChapters(text)
          if (chapters.length > 1) setAutoToc(chapters)
        }
      })
      .catch(e => { if (!cancelled) setError(e.message) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [book.id])

  // Measure using the page LAYER (not wrap) so padding is excluded
  const measure = useCallback(() => {
    if (!frontLayerRef.current || !frontInnerRef.current) return
    const rawPh = frontLayerRef.current.clientHeight   // ← page layer, not wrap
    const th    = frontInnerRef.current.scrollHeight
    // Read actual padding-top from DOM so we can snap to real line boundaries
    const pt   = parseFloat(getComputedStyle(frontInnerRef.current).paddingTop) || 20
    const lineH = Math.round(fontSizeRef.current * 1.85)
    paddingTopRef.current = pt
    lineHRef.current      = lineH
    // Snap page height to an integer number of lines so turns never land mid-line
    const ph = Math.max(lineH, Math.floor(rawPh / lineH) * lineH)
    setPageH(ph)
    setTotalH(th)
    if (!hasRestored.current && ph > 0) {
      hasRestored.current = true
      const snapLine = (raw) => raw <= 0 ? 0 : pt + Math.floor(Math.max(0, raw - pt) / lineH) * lineH
      const savedPage = parseInt(localStorage.getItem(`tome_pg_${book.id}`) || '', 10)
      if (!isNaN(savedPage) && savedPage > 0) {
        setOffset(snapLine(Math.min(savedPage * ph, th - ph)))
      } else if (initialProgress > 0) {
        const target = Math.round((initialProgress / 100) * (th - ph))
        setOffset(snapLine(Math.max(0, Math.min(target, th - ph))))
      }
    }
  }, [book.id, initialProgress])

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

  // Save progress — store both percentage (for API/display) and exact page index (for accurate restore)
  useEffect(() => {
    if (!pageH || !totalH) return
    const pct = Math.round((offset / Math.max(totalH - pageH, 1)) * 100)
    setProgress(book.id, Math.min(100, Math.max(0, pct)))
    localStorage.setItem(`tome_pg_${book.id}`, String(Math.floor(offset / pageH)))
  }, [offset, pageH, totalH])

  // Android back button
  useEffect(() => {
    let handle
    try {
      CapApp.addListener('backButton', () => {
        if (showToc)            { setShowToc(false); return }
        if (showMenu)           { setShowMenu(false); return }
        if (showHighlightPanel) { setShowHighlightPanel(false); return }
        if (showRecs)           { setShowRecs(false); return }
        if (dictWord)           { setDictWord(null); return }
        onBack()
      }).then(h => { handle = h })
    } catch {}
    return () => { try { handle?.remove() } catch {} }
  }, [showToc, showMenu, showHighlightPanel, showRecs, dictWord, onBack])

  // Selection tracking: drives highlight button AND auto-dictionary on single-word select.
  // On Android WebView, onClick does NOT fire after a long-press selection, so we must
  // detect dictionary intent here rather than in handleTextClick.
  useEffect(() => {
    let timer = null
    const onSelChange = () => {
      clearTimeout(timer)
      const sel  = window.getSelection()
      const text = sel?.toString().trim()
      const valid = !!(text && text.length > 1)
      setHasSelection(valid)
      if (valid) {
        try {
          const rect = sel.getRangeAt(0).getBoundingClientRect()
          lastSelRef.current    = text
          lastSelPosRef.current = { x: rect.left + rect.width / 2, y: rect.bottom }
        } catch {}
        // Auto-show dictionary after a brief pause for single-word selections
        if (text.split(/\s+/).length <= 2) {
          timer = setTimeout(() => {
            if (window.getSelection()?.toString().trim() === text) {
              setDictWord(text)
              setDictPos(lastSelPosRef.current)
            }
          }, 350)
        }
      }
    }
    document.addEventListener('selectionchange', onSelChange)
    return () => { document.removeEventListener('selectionchange', onSelChange); clearTimeout(timer) }
  }, [])

  const maxOffset = Math.max(0, totalH - pageH)
  const atStart   = offset <= 0
  const atEnd     = offset >= maxOffset

  // ─── Page-turn engine ───────────────────────────────────────────────────────

  const applyDragVisuals = useCallback((dir, progress) => {
    const front  = frontLayerRef.current
    const canvas = animCanvasRef.current
    const wrap   = wrapRef.current
    if (!front || !canvas || !wrap) return

    const dpr = window.devicePixelRatio || 1
    const w   = wrap.offsetWidth
    const h   = wrap.offsetHeight
    const bw  = Math.round(w * dpr)
    const bh  = Math.round(h * dpr)
    if (canvas.width !== bw || canvas.height !== bh) {
      canvas.width  = bw
      canvas.height = bh
    }
    const ctx = canvas.getContext('2d')
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    ctx.clearRect(0, 0, w, h)

    if (progress <= 0.005) {
      front.style.transform = ''
      canvas.style.display = 'none'
      return
    }

    canvas.style.display = 'block'

    // Bow: paper edge lifts slightly at mid-height, peaks at 50% through the turn
    const bowAmp = Math.min(w * 0.035, 14) * Math.sin(progress * Math.PI)
    // Shadow alpha ramps up quickly then stays steady
    const sinP = Math.sin(progress * Math.PI / 2)

    if (dir === 'fwd') {
      // Entire front page slides leftward — the sheet slides off the pile
      front.style.transform = `translateX(${(-progress * w).toFixed(1)}px)`

      // Trailing edge = right side of the sliding layer
      const edgeX = w * (1 - progress)

      if (sinP > 0.02 && edgeX < w) {
        // Shadow falls on back layer, just right of the trailing edge
        const shadowW = Math.min(52, w - edgeX)
        if (shadowW > 1) {
          ctx.save()
          ctx.beginPath()
          ctx.moveTo(edgeX, 0)
          ctx.quadraticCurveTo(edgeX + bowAmp, h / 2, edgeX, h)
          ctx.lineTo(edgeX + shadowW, h)
          ctx.lineTo(edgeX + shadowW, 0)
          ctx.closePath()
          ctx.clip()
          const sg = ctx.createLinearGradient(edgeX, 0, edgeX + shadowW, 0)
          sg.addColorStop(0,   `rgba(0,0,0,${(sinP * 0.38).toFixed(3)})`)
          sg.addColorStop(0.5, `rgba(0,0,0,${(sinP * 0.10).toFixed(3)})`)
          sg.addColorStop(1,   'rgba(0,0,0,0)')
          ctx.fillStyle = sg
          ctx.fillRect(0, 0, w, h)
          ctx.restore()
        }
        // Bright edge highlight — paper edge catching light as it lifts
        ctx.save()
        ctx.beginPath()
        ctx.moveTo(edgeX, 0)
        ctx.quadraticCurveTo(edgeX + bowAmp, h / 2, edgeX, h)
        ctx.lineWidth = 2.5
        ctx.strokeStyle = `rgba(255,255,255,${(sinP * 0.55).toFixed(3)})`
        ctx.stroke()
        ctx.restore()
      }

    } else {
      // Entire front page slides rightward — sheet returns to pile from left
      front.style.transform = `translateX(${(progress * w).toFixed(1)}px)`

      // Trailing edge = left side of the sliding layer
      const edgeX = progress * w

      if (sinP > 0.02 && edgeX > 0) {
        // Shadow falls on back layer, just left of the trailing edge
        const shadowW = Math.min(52, edgeX)
        if (shadowW > 1) {
          ctx.save()
          ctx.beginPath()
          ctx.moveTo(edgeX, 0)
          ctx.quadraticCurveTo(edgeX - bowAmp, h / 2, edgeX, h)
          ctx.lineTo(edgeX - shadowW, h)
          ctx.lineTo(edgeX - shadowW, 0)
          ctx.closePath()
          ctx.clip()
          const sg = ctx.createLinearGradient(edgeX, 0, edgeX - shadowW, 0)
          sg.addColorStop(0,   `rgba(0,0,0,${(sinP * 0.38).toFixed(3)})`)
          sg.addColorStop(0.5, `rgba(0,0,0,${(sinP * 0.10).toFixed(3)})`)
          sg.addColorStop(1,   'rgba(0,0,0,0)')
          ctx.fillStyle = sg
          ctx.fillRect(0, 0, w, h)
          ctx.restore()
        }
        ctx.save()
        ctx.beginPath()
        ctx.moveTo(edgeX, 0)
        ctx.quadraticCurveTo(edgeX - bowAmp, h / 2, edgeX, h)
        ctx.lineWidth = 2.5
        ctx.strokeStyle = `rgba(255,255,255,${(sinP * 0.55).toFixed(3)})`
        ctx.stroke()
        ctx.restore()
      }
    }
  }, [])

  const resetDragVisuals = useCallback(() => {
    if (frontLayerRef.current) frontLayerRef.current.style.transform = ''
    const canvas = animCanvasRef.current
    if (canvas) {
      canvas.getContext('2d')?.clearRect(0, 0, canvas.width, canvas.height)
      canvas.style.display = 'none'
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

  // Snap a raw pixel offset to the nearest line boundary at or below it,
  // ensuring the first visible line is never partially clipped.
  const snapLine = useCallback((raw) => {
    const pt  = paddingTopRef.current
    const lh  = lineHRef.current
    if (raw <= 0) return 0
    if (raw <= pt) return 0
    return pt + Math.floor((raw - pt) / lh) * lh
  }, [])

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
      const e = 1 - Math.pow(1 - t, 3)  // ease-out cubic
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

    const rawNext = dir === 'fwd' ? Math.min(curOff + ph, maxOff) : Math.max(curOff - ph, 0)
    const next = snapLine(rawNext)
    const w = wrapRef.current?.offsetWidth || window.innerWidth
    setBackLayerOffset(next)
    dragRef.current = { dir, startX: dir === 'fwd' ? w : 0, lastX: 0 }

    animateFold(dir, 0, 1, 380, () => {
      dragRef.current    = null
      pendingReset.current = true
      setOffset(next)   // triggers re-render → useLayoutEffect resets clips
    })
  }, [animateFold, setBackLayerOffset, snapLine])

  // Finish a touch drag — snap to complete or abort
  const finishDrag = useCallback((dir, progress) => {
    const THRESHOLD = 0.20
    const curOff = offsetRef.current
    const ph     = pageHRef.current
    const maxOff = Math.max(0, totalHRef.current - ph)

    if (progress >= THRESHOLD) {
      const rawNext = dir === 'fwd' ? Math.min(curOff + ph, maxOff) : Math.max(curOff - ph, 0)
      const next = snapLine(rawNext)
      const dur  = Math.max(160, (1 - progress) * 420)
      animateFold(dir, progress, 1, dur, () => {
        dragRef.current      = null
        pendingReset.current = true
        setOffset(next)
      })
    } else {
      const dur = Math.max(80, progress * 220)
      animateFold(dir, progress, 0, dur, () => {
        dragRef.current = null
        resetDragVisuals()
      })
    }
  }, [animateFold, resetDragVisuals, snapLine])

  // Non-passive touchmove listener attached imperatively (must preventDefault)
  useEffect(() => {
    const el = wrapRef.current
    if (!el) return
    const onMove = (e) => {
      // ── Active drag: update fold visuals ──
      const dr = dragRef.current
      if (dr) {
        e.preventDefault()
        const x = e.touches[0].clientX
        dr.lastX = x
        const w = el.offsetWidth
        const progress = dr.dir === 'fwd'
          ? Math.max(0, Math.min(1, (dr.startX - x) / Math.max(dr.startX, 1)))
          : Math.max(0, Math.min(1, (x - dr.startX) / Math.max(w - dr.startX, 1)))
        applyDragVisuals(dr.dir, progress)
        return
      }

      // ── Pending turn: wait for horizontal swipe intent, then derive direction ──
      const pt = pendingTurnRef.current
      if (!pt) return
      const touch = e.touches[0]
      const adx = Math.abs(touch.clientX - pt.startX)
      const ady = Math.abs(touch.clientY - pt.startY)

      if (ady > 36) { pendingTurnRef.current = null; return }

      if (adx > 8) {
        e.preventDefault()
        const curOff = offsetRef.current
        const ph     = pageHRef.current
        const maxOff = Math.max(0, totalHRef.current - ph)
        // Direction from the gesture itself — swipe left = forward, swipe right = backward
        const dir = touch.clientX < pt.startX ? 'fwd' : 'bck'
        if ((dir === 'fwd' && curOff >= maxOff) || (dir === 'bck' && curOff <= 0)) {
          pendingTurnRef.current = null; return
        }
        const backOff = dir === 'fwd'
          ? Math.min(curOff + ph, maxOff)
          : Math.max(curOff - ph, 0)
        setBackLayerOffset(backOff)
        dragRef.current        = { dir, startX: pt.startX, lastX: touch.clientX }
        pendingTurnRef.current = null
      }
    }
    el.addEventListener('touchmove', onMove, { passive: false })
    return () => el.removeEventListener('touchmove', onMove)
  }, [applyDragVisuals, setBackLayerOffset])

  const handleTouchStart = useCallback((e) => {
    if (dragRef.current) return
    if (e.target.closest?.('.reader-menu,.highlights-panel,.recs-panel,.dictionary-popup,.reader-topbar,.reader-nav')) return

    const y = e.touches[0].clientY
    const h = window.innerHeight

    // Top/bottom 60 px zones → reveal chrome
    if (y < 60 || y > h - 60) {
      showAndAutoHideChrome()
      return
    }

    // Middle tap while chrome visible → dismiss it, don't turn page
    if (showChromeRef.current) {
      setChrome(false)
      if (chromeTimerRef.current) clearTimeout(chromeTimerRef.current)
      return
    }

    const x = e.touches[0].clientX
    // Any swipe anywhere triggers a turn — direction is determined in touchmove from gesture
    pendingTurnRef.current = { dir: null, startX: x, startY: y }
  }, [showAndAutoHideChrome, setChrome])

  const handleTouchEnd = useCallback((e) => {
    const pt = pendingTurnRef.current
    pendingTurnRef.current = null

    const x = e.changedTouches[0].clientX
    const w = wrapRef.current?.offsetWidth || window.innerWidth

    // Normal case: drag was already confirmed in touchmove
    const dr = dragRef.current
    if (dr) {
      const progress = dr.dir === 'fwd'
        ? Math.max(0, Math.min(1, (dr.startX - x) / Math.max(dr.startX, 1)))
        : Math.max(0, Math.min(1, (x - dr.startX) / Math.max(w - dr.startX, 1)))
      const flick = Math.abs(dr.startX - x) >= 50
      finishDrag(dr.dir, flick ? Math.max(progress, 0.35) : progress)
      return
    }

    // Quick-flick: finger lifted before touchmove confirmed the drag.
    // Derive direction from the touch path and commit as a full turn.
    if (pt) {
      const adx = Math.abs(x - pt.startX)
      const ady = Math.abs(e.changedTouches[0].clientY - pt.startY)
      if (adx > 12 && adx > ady) {
        const dir = x < pt.startX ? 'fwd' : 'bck'
        const curOff = offsetRef.current
        const ph     = pageHRef.current
        const maxOff = Math.max(0, totalHRef.current - ph)
        if ((dir === 'fwd' && curOff >= maxOff) || (dir === 'bck' && curOff <= 0)) return
        const backOff = dir === 'fwd'
          ? Math.min(curOff + ph, maxOff)
          : Math.max(curOff - ph, 0)
        setBackLayerOffset(backOff)
        dragRef.current = { dir, startX: pt.startX, lastX: x }
        finishDrag(dir, 0.45)
      }
    }
  }, [finishDrag, setBackLayerOffset])

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
    // Live selection takes priority; fall back to the last stored selection (survives tap-dismiss)
    const live = window.getSelection()?.toString().trim()
    const word = (live && live.length > 1 && live.split(/\s+/).length <= 5) ? live
      : (lastSelRef.current && lastSelRef.current.split(/\s+/).length <= 5 ? lastSelRef.current : null)
    if (word) {
      let pos = lastSelPosRef.current
      if (live && live.length > 1) {
        try {
          const r = window.getSelection().getRangeAt(0).getBoundingClientRect()
          pos = { x: r.left + r.width / 2, y: r.bottom }
        } catch {}
      }
      setDictWord(word)
      setDictPos(pos || null)
    } else {
      setDictWord(null)
      lastSelRef.current = null
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

  // Jump to a chapter by anchor ID (epub) or char offset (gutenberg auto-toc)
  const jumpToChapter = useCallback((entry) => {
    if (entry.anchorId && frontInnerRef.current) {
      const el = frontInnerRef.current.querySelector(`#${entry.anchorId}`)
      if (el) {
        const raw   = el.offsetTop
        const snapped = Math.round(raw / Math.max(pageHRef.current, 1)) * Math.max(pageHRef.current, 1)
        setOffset(Math.max(0, Math.min(snapped, Math.max(0, totalHRef.current - pageHRef.current))))
        setShowToc(false)
        return
      }
    }
    if (entry.charOffset != null && fullText) {
      const frac    = entry.charOffset / Math.max(fullText.length, 1)
      const raw     = frac * totalHRef.current
      const snapped = Math.round(raw / Math.max(pageHRef.current, 1)) * Math.max(pageHRef.current, 1)
      setOffset(Math.max(0, Math.min(snapped, Math.max(0, totalHRef.current - pageHRef.current))))
      setShowToc(false)
    }
  }, [fullText])

  // Jump to highlight page
  const jumpToHighlight = useCallback((hl) => {
    const ph = pageHRef.current
    if (!ph) return
    setOffset(Math.min(hl.pageIndex * ph, Math.max(0, totalHRef.current - ph)))
    setShowHighlightPanel(false)
  }, [])

  // Memoized: reprocessing the entire book text through regex on every render is expensive
  const pageContent = useMemo(() => {
    const raw = applyHighlights(fullText, highlights)
    if (book.contentFormat === 'html') return raw
    return raw
      .replace(/\r\n/g, '\n').replace(/\r/g, '\n')
      .split(/\n{2,}/)
      .map(block => {
        const text = block.replace(/\n/g, ' ').replace(/\s+/g, ' ').trim()
        return text ? `<p>${text}</p>` : ''
      })
      .filter(Boolean)
      .join('')
  }, [fullText, highlights, book.contentFormat])

  const paperStyle = useMemo(
    () => pageColor ? { background: pageColor.bg, color: pageColor.text } : undefined,
    [pageColor]
  )
  const frontStyle = useMemo(
    () => ({ transform: `translateY(-${offset}px)`, fontSize }),
    [offset, fontSize]
  )

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
      {/* Top bar — hidden until top-zone tap */}
      <div className={`reader-topbar${!showChrome ? ' chrome-hidden' : ''}`}>
        <button className="reader-back-btn" onClick={onBack}>← Library</button>
        <div className="reader-book-title-bar">{book.title}</div>
        <div style={{ display: 'flex', gap: 4 }}>
          {activeToc.length > 0 && (
            <button className="reader-menu-btn"
              onClick={e => { e.stopPropagation(); setShowToc(p => !p) }} title="Table of Contents">☰</button>
          )}
          <button className="reader-menu-btn"
            onClick={e => { e.stopPropagation(); setShowMenu(p => !p) }}>⋮</button>
        </div>
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

        {/* Layer 2: Current page — slides laterally during page turn */}
        <div className="page-layer page-layer-front" ref={frontLayerRef}>
          <div className="page-paper" style={paperStyle}>
            <div className="binding-shadow" />
            <div ref={frontInnerRef} className="page-text-inner"
              style={frontStyle}
              dangerouslySetInnerHTML={{ __html: pageContent }} />
          </div>
        </div>

        {/* Canvas overlay: cylinder page turn animation */}
        <canvas ref={animCanvasRef} className="page-turn-canvas" />

      </div>

      {/* Navigation — hidden until bottom-zone tap */}
      <div className={`reader-nav${!showChrome ? ' chrome-hidden' : ''}`}>
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
              style={{ borderLeft: `4px solid ${hl.color}`, cursor: 'pointer' }}
              onClick={() => jumpToHighlight(hl)}>
              <p className="highlight-text">"{hl.text}"</p>
              <div className="highlight-meta">
                <span className="hl-page-hint">Tap to jump · Page {hl.pageIndex + 1}</span>
                <button className="hl-remove" onClick={e => { e.stopPropagation(); removeHighlight(hl.id) }}>Remove</button>
              </div>
            </div>
          ))}
        </div>
      )}

      {showToc && (
        <div className={`toc-panel ${nightMode ? 'night' : ''}`}
          onClick={e => e.stopPropagation()}>
          <div className="panel-header">
            <h3>Contents</h3>
            <button onClick={() => setShowToc(false)}>×</button>
          </div>
          <div className="toc-list">
            {activeToc.map((entry, i) => (
              <button key={i}
                className="toc-item"
                style={{ paddingLeft: `${16 + (entry.level || 0) * 12}px` }}
                onClick={() => jumpToChapter(entry)}>
                {entry.title}
              </button>
            ))}
          </div>
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
