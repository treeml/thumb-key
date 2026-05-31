import React, { useRef, useState } from 'react'
import { parseEpub } from '../utils/epubParser'
import { idbSet } from '../utils/idb'

const ACCEPT = '.epub,.txt,.mp3,.m4a,.m4b,.ogg,.aac,.flac'

async function readMetaFromAudio(file) {
  return new Promise(resolve => {
    const audio = new Audio()
    const url   = URL.createObjectURL(file)
    audio.src   = url
    audio.onloadedmetadata = () => {
      URL.revokeObjectURL(url)
      resolve({ duration: audio.duration })
    }
    audio.onerror = () => { URL.revokeObjectURL(url); resolve({ duration: 0 }) }
    setTimeout(() => { URL.revokeObjectURL(url); resolve({ duration: 0 }) }, 5000)
  })
}

const AUDIO_EXTS = ['mp3', 'm4a', 'm4b', 'ogg', 'aac', 'flac', 'wav']

export default function ImportButton({ onBookImported, onAudioImported }) {
  const inputRef = useRef(null)
  const [status, setStatus] = useState(null) // null | 'reading' | 'done' | 'error'
  const [msg,    setMsg]    = useState('')

  const handleFiles = async (files) => {
    for (const file of Array.from(files)) {
      const ext = file.name.split('.').pop()?.toLowerCase() || ''
      setStatus('reading')
      setMsg(`Importing ${file.name}…`)

      try {
        if (ext === 'epub') {
          setMsg('Parsing ePub…')
          const parsed = await parseEpub(file)

          // Store content in IndexedDB
          const bookId = `local_${Date.now()}_${Math.random().toString(36).slice(2)}`
          await idbSet(bookId, { content: parsed.content, contentFormat: parsed.contentFormat })

          const book = {
            id:            bookId,
            title:         parsed.title,
            authors:       parsed.authors,
            toc:           parsed.toc,
            contentFormat: parsed.contentFormat,
            source:        'local',
            format:        'epub',
            formats:       {},
            subjects:      [],
            coverBase64:   parsed.coverBase64,
          }
          // Cover URL for BookCard
          if (parsed.coverBase64) book.formats['image/jpeg'] = parsed.coverBase64

          onBookImported(book)
          setMsg(`✓ Added "${parsed.title}"`)

        } else if (ext === 'txt') {
          const text = await file.text()
          const bookId = `local_${Date.now()}_${Math.random().toString(36).slice(2)}`
          await idbSet(bookId, { content: text, contentFormat: 'text' })

          const book = {
            id:            bookId,
            title:         file.name.replace(/\.txt$/i, ''),
            authors:       [{ name: 'Unknown' }],
            toc:           [],
            contentFormat: 'text',
            source:        'local',
            format:        'txt',
            formats:       {},
            subjects:      [],
          }
          onBookImported(book)
          setMsg(`✓ Added "${book.title}"`)

        } else if (ext === 'pdf') {
          setMsg('PDF import coming soon — ePub is recommended for best results.')
          setStatus('error')
          await new Promise(r => setTimeout(r, 2500))

        } else if (AUDIO_EXTS.includes(ext)) {
          setMsg('Reading audio file…')
          const { duration } = await readMetaFromAudio(file)

          const meta = {
            title:    file.name.replace(/\.[^.]+$/, ''),
            author:   '',
            duration,
            chapters: [],
          }
          onAudioImported(meta, file)
          setMsg(`✓ Added audio "${meta.title}"`)

        } else {
          setMsg(`Unsupported format: .${ext}. Supported: ePub, TXT, MP3, M4A, M4B`)
          setStatus('error')
          await new Promise(r => setTimeout(r, 2500))
          setStatus(null)
          continue
        }

        setStatus('done')
        await new Promise(r => setTimeout(r, 1800))
        setStatus(null)
        setMsg('')

      } catch (e) {
        setStatus('error')
        setMsg(`Error: ${e.message}`)
        await new Promise(r => setTimeout(r, 3000))
        setStatus(null)
        setMsg('')
      }
    }
  }

  return (
    <>
      <input
        ref={inputRef}
        type="file"
        accept={ACCEPT}
        multiple
        style={{ display: 'none' }}
        onChange={e => { handleFiles(e.target.files); e.target.value = '' }}
      />
      <button
        className="import-btn"
        onClick={() => inputRef.current?.click()}
        title="Import ePub, TXT, or audio file"
        disabled={status === 'reading'}
      >
        {status === 'reading' ? '…' : '⤵ Import'}
      </button>
      {status && (
        <div className={`import-toast ${status}`}>{msg}</div>
      )}
    </>
  )
}
