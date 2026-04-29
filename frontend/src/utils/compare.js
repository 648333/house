const COMPARE_KEY = 'compare_properties'
const MAX_COMPARE_COUNT = 3

function getUserKey() {
  const user = JSON.parse(localStorage.getItem('user') || 'null')
  return user?.id ? String(user.id) : 'guest'
}

function readStore() {
  return JSON.parse(localStorage.getItem(COMPARE_KEY) || '{}')
}

function writeStore(store) {
  localStorage.setItem(COMPARE_KEY, JSON.stringify(store))
}

export function getComparedIds() {
  const store = readStore()
  return store[getUserKey()] || []
}

export function isCompared(propertyId) {
  return getComparedIds().includes(Number(propertyId))
}

export function addToCompare(propertyId) {
  const numericId = Number(propertyId)
  const store = readStore()
  const userKey = getUserKey()
  const current = store[userKey] || []

  if (current.includes(numericId)) {
    return { ids: current, changed: false, reason: 'already_exists' }
  }

  if (current.length >= MAX_COMPARE_COUNT) {
    return { ids: current, changed: false, reason: 'limit' }
  }

  const next = [...current, numericId]
  store[userKey] = next
  writeStore(store)
  return { ids: next, changed: true, reason: 'added' }
}

export function removeFromCompare(propertyId) {
  const numericId = Number(propertyId)
  const store = readStore()
  const userKey = getUserKey()
  const next = (store[userKey] || []).filter((id) => id !== numericId)
  store[userKey] = next
  writeStore(store)
  return next
}

export function toggleCompare(propertyId) {
  if (isCompared(propertyId)) {
    return { ids: removeFromCompare(propertyId), changed: true, reason: 'removed' }
  }

  return addToCompare(propertyId)
}

export function clearCompare() {
  const store = readStore()
  store[getUserKey()] = []
  writeStore(store)
}

export function getCompareLimit() {
  return MAX_COMPARE_COUNT
}
