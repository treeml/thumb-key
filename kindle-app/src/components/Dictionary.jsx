import React, { useEffect, useState } from 'react'
import { lookupWord } from '../utils/api'

export default function Dictionary({ word, position, onClose, nightMode }) {
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!word) return
    setLoading(true)
    setError(null)
    setResult(null)
    lookupWord(word)
      .then(data => setResult(data[0]))
      .catch(e => setError(e.message))
      .finally(() => setLoading(false))
  }, [word])

  const style = position
    ? {
        position: 'fixed',
        top: Math.min(position.y + 12, window.innerHeight - 280),
        left: Math.max(8, Math.min(position.x - 140, window.innerWidth - 308)),
        zIndex: 1000,
      }
    : { position: 'fixed', bottom: 80, left: '50%', transform: 'translateX(-50%)', zIndex: 1000 }

  return (
    <div className={`dictionary-popup ${nightMode ? 'night' : ''}`} style={style} onClick={e => e.stopPropagation()}>
      <div className="dict-header">
        <span className="dict-word">{word}</span>
        <button className="dict-close" onClick={onClose}>×</button>
      </div>
      {loading && <div className="dict-loading">Looking up…</div>}
      {error && (
        <div className="dict-error">
          {error === 'not_found' ? 'No definition found.' : 'Could not reach dictionary — check your connection.'}
        </div>
      )}
      {result && (
        <div className="dict-body">
          {result.phonetics?.find(p => p.text) && (
            <div className="dict-phonetic">{result.phonetics.find(p => p.text).text}</div>
          )}
          {result.meanings?.slice(0, 2).map((meaning, i) => (
            <div key={i} className="dict-meaning">
              <div className="dict-pos">{meaning.partOfSpeech}</div>
              {meaning.definitions?.slice(0, 2).map((def, j) => (
                <div key={j} className="dict-def">
                  <span className="dict-def-num">{j + 1}.</span> {def.definition}
                  {def.example && <div className="dict-example">"{def.example}"</div>}
                </div>
              ))}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
