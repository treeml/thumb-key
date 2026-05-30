import React, { useEffect, useState, useRef, useCallback } from 'react'
import { fetchBookText, getBookAuthors } from '../utils/api'
import { useHighlights } from '../hooks/useHighlights'
import Dictionary from './Dictionary'
import Recommendations from './Recommendations'

const CHARS_PER_PAGE = 1800

function paginateText(text) {
  const paragraphs = text.split(/\n\n+/).filter(p => p.trim().length > 0)
  const pages = []
  let current = ''

  for (const para of paragraphs) {
    if (current.length + para.length > CHARS_PER_PAGE && current.length > 0) {
      pages.push(current.trim())
      current = para
    } else {
      current += (current ? '\n\n' : '') + para
    }
  }
  if (current.trim()) pages.push(current.trim())
  return pages
}

function applyHighlights(text, pageHighlights) {
  if (!pageHighlights.length) return text
  let result = text
  // Sort by length descending to avoid nested replacement issues
  const sorted = [...pageHighlights].sort((a, b) => b.text.length - a.text.length)
  for (const hl of sorted) {
    const escaped = hl.text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    result = result.replace(
      new RegExp(escaped, 'g'),
      `<mark class="highlight" style="background:${hl.color}88" data-hl-id="${hl.id}">$&</mark>`
    )
  }
  return result
}

export default function Reader({ book, nightMode, setProgress, initialProgress, onBack }) {
  const [pages, setPages] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [pageIndex, setPageIndex] = useState(0)
  const [flipping, setFlipping] = useState(null) // 'forward' | 'back' | null
  const [showMenu, setShowMenu] = useState(false)
  const [dictWord, setDictWord] = useState(null)
  const [dictPos, setDictPos] = useState(null)
  const [showRecs, setShowRecs] = useState(false)
  const [highlightColor, setHighlightColor] = useState('#FFD700')
  const [fontSize, setFontSize] = useState(18)
  const [showHighlightPanel, setShowHighlightPanel] = useState(false)

  const { highlights, addHighlight, removeHighlight, getPageHighlights } = useHighlights(book.id)
  const pageRef = useRef(null)
  const touchStartX = useRef(null)

  useEffect(() => {
    setLoading(true)
    setError(null)
    fetchBookText(book)
      .then(text => {
        const pgs = paginateText(text)
        setPages(pgs)
        if (initialProgress > 0) {
          const startPage = Math.floor((initialProgress / 100) * pgs.length)
          setPageIndex(Math.min(startPage, pgs.length - 1))
        }
      })
      .catch(e => setError(e.message))
      .finally(() => setLoading(false))
  }, [book.id])

  useEffect(() => {
    if (pages.length > 0) {
      const pct = Math.round(((pageIndex + 1) / pages.length) * 100)
      setProgress(book.id, pct)
    }
  }, [pageIndex, pages.length])

  const goForward = useCallback(() => {
    if (pageIndex >= pages.length - 1 || flipping) return
    setFlipping('forward')
    setTimeout(() => {
      setPageIndex(p => Math.min(p + 1, pages.length - 1))
      setFlipping(null)
    }, 400)
  }, [pageIndex, pages.length, flipping])

  const goBack = useCallback(() => {
    if (pageIndex <= 0 || flipping) return
    setFlipping('back')
    setTimeout(() => {
      setPageIndex(p => Math.max(p - 1, 0))
      setFlipping(null)
    }, 400)
  }, [pageIndex, flipping])

  useEffect(() => {
    const handler = (e) => {
      if (e.key === 'ArrowRight') goForward()
      if (e.key === 'ArrowLeft') goBack()
      if (e.key === 'Escape') setShowMenu(false)
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [goForward, goBack])

  const handleTouchStart = (e) => { touchStartX.current = e.touches[0].clientX }
  const handleTouchEnd = (e) => {
    if (touchStartX.current === null) return
    const diff = touchStartX.current - e.changedTouches[0].clientX
    if (Math.abs(diff) > 50) { diff > 0 ? goForward() : goBack() }
    touchStartX.current = null
  }

  const handleWordSelect = (e) => {
    const selection = window.getSelection()
    const selected = selection?.toString().trim()
    if (!selected || selected.split(' ').length > 4) return
    if (selected.length < 2) return
    const range = selection.getRangeAt(0)
    const rect = range.getBoundingClientRect()
    setDictWord(selected)
    setDictPos({ x: rect.left + rect.width / 2, y: rect.bottom })
  }

  const handleTextClick = (e) => {
    if (e.target.closest('.dict-popup, .reader-menu, .reader-toolbar')) return
    const selection = window.getSelection()
    const selected = selection?.toString().trim()
    if (selected && selected.length > 1) {
      handleWordSelect(e)
    } else {
      setDictWord(null)
    }
  }

  const handleHighlight = () => {
    const selection = window.getSelection()
    const selected = selection?.toString().trim()
    if (!selected || selected.length < 2) return
    addHighlight(selected, pageIndex, highlightColor)
    selection.removeAllRanges()
    setDictWord(null)
  }

  const progress = pages.length > 0 ? Math.round(((pageIndex + 1) / pages.length) * 100) : 0
  const currentPageHighlights = getPageHighlights(pageIndex)
  const rawText = pages[pageIndex] || ''
  const displayHtml = applyHighlights(rawText, currentPageHighlights)

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
    <div className={`reader-container ${nightMode ? 'night' : ''}`}
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
              <button onClick={() => setFontSize(f => Math.max(12, f - 2))}>A-</button>
              <span>{fontSize}px</span>
              <button onClick={() => setFontSize(f => Math.min(28, f + 2))}>A+</button>
            </div>
          </div>
          <div className="menu-section">
            <label className="menu-label">Highlight Color</label>
            <div className="color-swatches">
              {['#FFD700', '#90EE90', '#87CEEB', '#FFB6C1', '#DDA0DD'].map(c => (
                <button
                  key={c}
                  className={`color-swatch ${highlightColor === c ? 'active' : ''}`}
                  style={{ background: c }}
                  onClick={() => setHighlightColor(c)}
                />
              ))}
            </div>
          </div>
          <button className="menu-item" onClick={() => { setShowHighlightPanel(p=>!p); setShowMenu(false) }}>
            📌 My Highlights ({highlights.length})
          </button>
          <button className="menu-item" onClick={() => { setShowRecs(p=>!p); setShowMenu(false) }}>
            ✨ Recommendations
          </button>
          <div className="menu-section">
            <label className="menu-label">Jump to Page</label>
            <div className="page-jump">
              <input
                type="range"
                min="0" max={pages.length - 1}
                value={pageIndex}
                onChange={e => setPageIndex(Number(e.target.value))}
              />
            </div>
          </div>
        </div>
      )}

      {/* Page book */}
      <div className={`book-reader ${flipping ? `flip-${flipping}` : ''}`}>
        <div className="book-page-container">
          {/* Back of previous page (visible during forward flip) */}
          <div className="page-back" />
          {/* Main page */}
          <div className="page-content" style={{ fontSize }}>
            <div
              className="page-text"
              dangerouslySetInnerHTML={{ __html: displayHtml.replace(/\n/g, '<br/>') }}
            />
          </div>
          {/* Page turn shadow */}
          <div className="page-shadow" />
        </div>
      </div>

      {/* Navigation */}
      <div className="reader-nav">
        <button
          className={`nav-btn nav-prev ${pageIndex === 0 ? 'disabled' : ''}`}
          onClick={goBack}
          disabled={pageIndex === 0}
        >‹</button>

        <div className="reader-progress-info">
          <div className="progress-bar-reader">
            <div className="progress-fill-reader" style={{ width: `${progress}%` }} />
          </div>
          <span className="progress-text">{progress}% · Page {pageIndex + 1} of {pages.length}</span>
        </div>

        <button
          className={`nav-btn nav-next ${pageIndex >= pages.length - 1 ? 'disabled' : ''}`}
          onClick={goForward}
          disabled={pageIndex >= pages.length - 1}
        >›</button>
      </div>

      {/* Highlight toolbar - shows on text selection */}
      <div className="highlight-toolbar" id="highlight-toolbar">
        <button className="hl-btn" onClick={handleHighlight} title="Highlight">
          <span style={{ color: highlightColor }}>■</span> Highlight
        </button>
        {dictWord && (
          <button className="hl-btn" onClick={() => setDictWord(dictWord)}>
            📖 Define
          </button>
        )}
      </div>

      {/* Dictionary popup */}
      {dictWord && (
        <Dictionary
          word={dictWord}
          position={dictPos}
          onClose={() => setDictWord(null)}
          nightMode={nightMode}
        />
      )}

      {/* Highlights panel */}
      {showHighlightPanel && (
        <div className={`highlights-panel ${nightMode ? 'night' : ''}`} onClick={e => e.stopPropagation()}>
          <div className="panel-header">
            <h3>My Highlights</h3>
            <button onClick={() => setShowHighlightPanel(false)}>×</button>
          </div>
          {highlights.length === 0 && (
            <p className="panel-empty">No highlights yet. Select text and press Highlight.</p>
          )}
          {highlights.map(hl => (
            <div key={hl.id} className="highlight-item" style={{ borderLeft: `4px solid ${hl.color}` }}>
              <p className="highlight-text">"{hl.text}"</p>
              <div className="highlight-meta">
                <span>Page {hl.pageIndex + 1}</span>
                <button className="hl-remove" onClick={() => removeHighlight(hl.id)}>Remove</button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Recommendations panel */}
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
