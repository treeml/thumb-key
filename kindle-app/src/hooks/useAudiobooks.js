import { useState, useCallback } from 'react'
import { idbSet, idbDel } from '../utils/idb'

const KEY = 'tome_audiobooks'

function load() {
  try { return JSON.parse(localStorage.getItem(KEY) || '{}') }
  catch { return {} }
}

function save(data) { localStorage.setItem(KEY, JSON.stringify(data)) }

export function useAudiobooks() {
  const [library, setLibrary] = useState(load)

  const update = useCallback((fn) => {
    setLibrary(prev => { const next = fn(prev); save(next); return next })
  }, [])

  const addAudiobook = useCallback(async (meta, blob) => {
    const id = `audio_${Date.now()}`
    await idbSet(id, blob)
    update(prev => ({
      ...prev,
      [id]: { ...meta, id, addedAt: Date.now(), position: 0 },
    }))
    return id
  }, [update])

  const removeAudiobook = useCallback(async (id) => {
    await idbDel(id)
    update(prev => { const next = { ...prev }; delete next[id]; return next })
  }, [update])

  const setPosition = useCallback((id, position) => {
    update(prev => ({ ...prev, [id]: { ...prev[id], position } }))
  }, [update])

  const books = Object.values(library).sort((a, b) => (b.addedAt || 0) - (a.addedAt || 0))

  return { books, addAudiobook, removeAudiobook, setPosition, library }
}
