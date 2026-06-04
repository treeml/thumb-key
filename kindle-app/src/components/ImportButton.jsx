import React, { useRef, useState } from 'react'
import { parseEpub } from '../utils/epubParser'
import { parseFb2 } from '../utils/fb2Parser'
import { idbSet } from '../utils/idb'
import { exportBackup, importBackup } from '../utils/backup'

const ACCEPT = '.epub,.fb2,.txt,.json,application/x-fictionbook+xml,application/x-fictionbook'

export default function ImportButton({ onBookImported }) {
  const inputRef = useRef(null)
  const [status, setStatus] = useState(null) // null | 'reading' | 'done' | 'error'
  const [msg,    setMsg]    = useState('')

  const handleExport = () => {
    try {
      const count = exportBackup()
      setStatus('done')
      setMsg(`✓ Backup exported (${count} entries)`)
      setTimeout(() => { setStatus(null); setMsg('') }, 3000)
    } catch (e) {
      setStatus('error')
      setMsg(`Export failed: ${e.message}`)
      setTimeout(() => { setStatus(null); setMsg('') }, 3000)
    }
  }

  const handleFiles = async (files) => {
    for (const file of Array.from(files)) {
      const ext = file.name.split('.').pop()?.toLowerCase() || ''
      setStatus('reading')
      setMsg(`Importing ${file.name}…`)

      try {
        if (ext === 'json') {
          setMsg('Restoring backup…')
          const count = await importBackup(file)
          setStatus('done')
          setMsg(`✓ Restored ${count} items — reloading…`)
          await new Promise(r => setTimeout(r, 1800))
          window.location.reload()
          return

        } else if (ext === 'epub') {
          setMsg('Parsing ePub…')
          const parsed = await parseEpub(file)

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
          if (parsed.coverBase64) book.formats['image/jpeg'] = parsed.coverBase64

          onBookImported(book)
          setMsg(`✓ Added "${parsed.title}"`)

        } else if (ext === 'fb2') {
          setMsg('Parsing FB2…')
          const parsed = await parseFb2(file)

          const bookId = `local_${Date.now()}_${Math.random().toString(36).slice(2)}`
          await idbSet(bookId, { content: parsed.content, contentFormat: parsed.contentFormat })

          const book = {
            id:            bookId,
            title:         parsed.title,
            authors:       parsed.authors,
            toc:           parsed.toc,
            contentFormat: parsed.contentFormat,
            source:        'local',
            format:        'fb2',
            formats:       {},
            subjects:      [],
            coverBase64:   parsed.coverBase64,
          }
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

        } else {
          setMsg(`Unsupported format: .${ext}`)
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
        title="Import ePub, FB2, TXT, or backup JSON"
        disabled={status === 'reading'}
      >
        {status === 'reading' ? '…' : '⤵ Import'}
      </button>
      <button
        className="export-btn"
        onClick={handleExport}
        title="Export library backup"
        disabled={status === 'reading'}
      >
        ⤴ Backup
      </button>
      {status && (
        <div className={`import-toast ${status}`}>{msg}</div>
      )}
    </>
  )
}
