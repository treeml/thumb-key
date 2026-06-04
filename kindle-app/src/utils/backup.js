// Exports all tome_ localStorage keys into a downloadable JSON file.
export function exportBackup() {
  const data = {}
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i)
    if (key?.startsWith('tome_')) data[key] = localStorage.getItem(key)
  }
  const payload = JSON.stringify({ version: 1, exportedAt: new Date().toISOString(), data }, null, 2)
  const blob = new Blob([payload], { type: 'application/json' })
  const url  = URL.createObjectURL(blob)
  const a    = document.createElement('a')
  a.href     = url
  a.download = `tome-backup-${new Date().toISOString().slice(0, 10)}.json`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  return Object.keys(data).length
}

// Reads a backup JSON file and restores all tome_ keys to localStorage.
// Returns a promise that resolves with a count of restored keys.
export function importBackup(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      try {
        const parsed = JSON.parse(e.target.result)
        const { data } = parsed
        if (!data || typeof data !== 'object') throw new Error('Invalid backup file format')
        let count = 0
        Object.entries(data).forEach(([key, value]) => {
          if (key.startsWith('tome_') && typeof value === 'string') {
            localStorage.setItem(key, value)
            count++
          }
        })
        resolve(count)
      } catch (err) {
        reject(new Error(`Could not read backup: ${err.message}`))
      }
    }
    reader.onerror = () => reject(new Error('Failed to read file'))
    reader.readAsText(file)
  })
}
