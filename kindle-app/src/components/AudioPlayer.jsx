import React, { useState, useRef, useEffect, useCallback } from 'react'
import { idbGet } from '../utils/idb'

const SPEEDS = [0.5, 0.75, 1, 1.25, 1.5, 1.75, 2, 2.5, 3]
const SLEEP_OPTIONS = [15, 30, 45, 60, 90]

function fmtTime(secs) {
  const h = Math.floor(secs / 3600)
  const m = Math.floor((secs % 3600) / 60)
  const s = Math.floor(secs % 60)
  return h > 0
    ? `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
    : `${m}:${String(s).padStart(2, '0')}`
}

export default function AudioPlayer({ book, onBack, setPosition, nightMode }) {
  const [src,        setSrc]        = useState(null)
  const [playing,    setPlaying]    = useState(false)
  const [duration,   setDuration]   = useState(0)
  const [currentTime,setCurrentTime]= useState(book.position || 0)
  const [speed,      setSpeed]      = useState(() => Number(localStorage.getItem('tome_audio_speed') || 1))
  const [sleepMin,   setSleepMin]   = useState(null)   // minutes remaining
  const [sleepLabel, setSleepLabel] = useState(null)   // display label
  const [chapter,    setChapter]    = useState(0)
  const [showSpeed,  setShowSpeed]  = useState(false)
  const [showSleep,  setShowSleep]  = useState(false)
  const [loading,    setLoading]    = useState(true)

  const audioRef   = useRef(null)
  const sleepTimer = useRef(null)
  const blobUrl    = useRef(null)

  // Load audio blob from IndexedDB
  useEffect(() => {
    setLoading(true)
    idbGet(book.id)
      .then(blob => {
        if (!blob) throw new Error('Audio file not found in storage')
        if (blobUrl.current) URL.revokeObjectURL(blobUrl.current)
        blobUrl.current = URL.createObjectURL(blob)
        setSrc(blobUrl.current)
      })
      .catch(e => { console.error(e) })
      .finally(() => setLoading(false))
    return () => { if (blobUrl.current) URL.revokeObjectURL(blobUrl.current) }
  }, [book.id])

  // Sync speed
  useEffect(() => {
    if (audioRef.current) audioRef.current.playbackRate = speed
    localStorage.setItem('tome_audio_speed', String(speed))
  }, [speed])

  // Resume position when src is ready
  useEffect(() => {
    if (!src || !audioRef.current) return
    audioRef.current.currentTime = book.position || 0
  }, [src])

  // Auto-detect chapters: one every 10 min as fallback
  const chapters = book.chapters?.length
    ? book.chapters
    : duration > 0
      ? Array.from({ length: Math.ceil(duration / 600) }, (_, i) => ({
          title: `Part ${i + 1}`,
          time: i * 600,
        }))
      : []

  const currentChapter = chapters.reduceRight((found, ch, i) =>
    found === chapter && ch.time <= currentTime ? i : found
  , chapter)

  // Save position periodically
  useEffect(() => {
    const id = setInterval(() => {
      if (audioRef.current) setPosition(book.id, audioRef.current.currentTime)
    }, 5000)
    return () => clearInterval(id)
  }, [book.id, setPosition])

  const startSleepTimer = (minutes) => {
    if (sleepTimer.current) clearTimeout(sleepTimer.current)
    setSleepLabel(`${minutes}m`)
    setSleepMin(minutes)
    sleepTimer.current = setTimeout(() => {
      audioRef.current?.pause()
      setPlaying(false)
      setSleepMin(null)
      setSleepLabel(null)
    }, minutes * 60 * 1000)
    setShowSleep(false)
  }

  const cancelSleep = () => {
    if (sleepTimer.current) clearTimeout(sleepTimer.current)
    setSleepMin(null)
    setSleepLabel(null)
  }

  const seekTo = (t) => {
    if (audioRef.current) audioRef.current.currentTime = t
    setCurrentTime(t)
  }

  const jumpChapter = (dir) => {
    const idx = currentChapter + dir
    if (idx < 0 || idx >= chapters.length) return
    seekTo(chapters[idx].time)
    setChapter(idx)
  }

  const skip = (secs) => seekTo(Math.max(0, Math.min(duration, currentTime + secs)))

  const togglePlay = () => {
    if (!audioRef.current) return
    if (playing) { audioRef.current.pause(); setPlaying(false) }
    else         { audioRef.current.play();  setPlaying(true) }
  }

  const progress = duration > 0 ? (currentTime / duration) * 100 : 0

  if (loading) return (
    <div className={`audio-player-page ${nightMode ? 'night' : ''}`}>
      <button className="back-btn" onClick={onBack}>← Audio</button>
      <div style={{ textAlign: 'center', padding: '80px 20px', color: 'var(--text-muted)' }}>
        <div className="search-spinner" style={{ margin: '0 auto 16px' }} />
        Loading audio…
      </div>
    </div>
  )

  return (
    <div className={`audio-player-page ${nightMode ? 'night' : ''}`}>
      <audio
        ref={audioRef}
        src={src}
        onTimeUpdate={e => setCurrentTime(e.target.currentTime)}
        onDurationChange={e => setDuration(e.target.duration)}
        onEnded={() => { setPlaying(false); setPosition(book.id, 0) }}
        onPlay={() => setPlaying(true)}
        onPause={() => setPlaying(false)}
      />

      <div className="ap-topbar">
        <button className="back-btn" onClick={onBack}>← Audio</button>
        {sleepLabel && (
          <button className="ap-sleep-chip" onClick={cancelSleep}>
            😴 {sleepLabel} ×
          </button>
        )}
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="ap-icon-btn" onClick={() => { setShowSleep(p => !p); setShowSpeed(false) }}>💤</button>
          <button className="ap-icon-btn" onClick={() => { setShowSpeed(p => !p); setShowSleep(false) }}>{speed}×</button>
        </div>
      </div>

      {showSpeed && (
        <div className="ap-speed-menu">
          {SPEEDS.map(s => (
            <button key={s} className={`ap-speed-btn ${speed === s ? 'active' : ''}`}
              onClick={() => { setSpeed(s); setShowSpeed(false) }}>{s}×</button>
          ))}
        </div>
      )}

      {showSleep && (
        <div className="ap-sleep-menu">
          {SLEEP_OPTIONS.map(m => (
            <button key={m} className="ap-sleep-btn" onClick={() => startSleepTimer(m)}>{m} min</button>
          ))}
          <button className="ap-sleep-btn" onClick={() => startSleepTimer(Math.ceil((duration - currentTime) / 60))}>
            End of chapter
          </button>
        </div>
      )}

      {/* Artwork / Album art placeholder */}
      <div className="ap-artwork">
        {book.coverBase64
          ? <img src={book.coverBase64} alt={book.title} />
          : <div className="ap-artwork-placeholder">🎧</div>
        }
      </div>

      <div className="ap-meta">
        <div className="ap-title">{book.title}</div>
        <div className="ap-author">{book.author || ''}</div>
        {chapters.length > 1 && (
          <div className="ap-chapter-label">{chapters[currentChapter]?.title}</div>
        )}
      </div>

      {/* Progress bar */}
      <div className="ap-progress-wrap">
        <span className="ap-time">{fmtTime(currentTime)}</span>
        <input type="range" className="ap-scrubber" min="0" max={duration || 1}
          step="1" value={currentTime}
          onChange={e => seekTo(Number(e.target.value))} />
        <span className="ap-time">{fmtTime(duration)}</span>
      </div>

      {/* Controls */}
      <div className="ap-controls">
        <button className="ap-ctrl-btn" onClick={() => jumpChapter(-1)} title="Previous chapter">⏮</button>
        <button className="ap-ctrl-btn" onClick={() => skip(-15)} title="Back 15s">↺15</button>
        <button className="ap-play-btn" onClick={togglePlay}>{playing ? '⏸' : '▶'}</button>
        <button className="ap-ctrl-btn" onClick={() => skip(30)} title="Forward 30s">30↻</button>
        <button className="ap-ctrl-btn" onClick={() => jumpChapter(1)} title="Next chapter">⏭</button>
      </div>

      {/* Chapter list */}
      {chapters.length > 1 && (
        <div className="ap-chapters">
          <div className="ap-chapters-title">Chapters</div>
          <div className="ap-chapters-list">
            {chapters.map((ch, i) => (
              <button key={i}
                className={`ap-chapter-item ${i === currentChapter ? 'active' : ''}`}
                onClick={() => { seekTo(ch.time); setChapter(i) }}>
                <span className="ap-ch-num">{i + 1}</span>
                <span className="ap-ch-title">{ch.title}</span>
                <span className="ap-ch-time">{fmtTime(ch.time)}</span>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
