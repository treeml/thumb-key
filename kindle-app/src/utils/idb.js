const DB_NAME = 'tome_content'
const STORE   = 'books'

// Single shared connection promise — avoids re-opening on every call
let dbPromise = null

function openDB() {
  if (!dbPromise) {
    dbPromise = new Promise((resolve, reject) => {
      const req = indexedDB.open(DB_NAME, 1)
      req.onupgradeneeded = e => e.target.result.createObjectStore(STORE)
      req.onsuccess = e => resolve(e.target.result)
      req.onerror   = e => { dbPromise = null; reject(e.target.error) }
    })
  }
  return dbPromise
}

export async function idbSet(id, value) {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, 'readwrite')
    tx.objectStore(STORE).put(value, id)
    tx.oncomplete = resolve
    tx.onerror    = e => reject(e.target.error)
  })
}

export async function idbGet(id) {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx  = db.transaction(STORE, 'readonly')
    const req = tx.objectStore(STORE).get(id)
    req.onsuccess = e => resolve(e.target.result)
    req.onerror   = e => reject(e.target.error)
  })
}

export async function idbDel(id) {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, 'readwrite')
    tx.objectStore(STORE).delete(id)
    tx.oncomplete = resolve
    tx.onerror    = e => reject(e.target.error)
  })
}
