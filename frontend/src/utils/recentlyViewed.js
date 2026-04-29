const RECENTLY_VIEWED_KEY = 'recently_viewed_properties'
const MAX_RECENTLY_VIEWED = 8

function getUserKey() {
  const user = JSON.parse(localStorage.getItem('user') || 'null')
  return user?.id ? String(user.id) : 'guest'
}

function readStore() {
  return JSON.parse(localStorage.getItem(RECENTLY_VIEWED_KEY) || '{}')
}

function writeStore(store) {
  localStorage.setItem(RECENTLY_VIEWED_KEY, JSON.stringify(store))
}

export function addRecentlyViewed(propertyId) {
  const numericId = Number(propertyId)
  const store = readStore()
  const userKey = getUserKey()
  const current = (store[userKey] || []).filter((id) => id !== numericId)
  const next = [numericId, ...current].slice(0, MAX_RECENTLY_VIEWED)
  store[userKey] = next
  writeStore(store)
  return next
}

export function getRecentlyViewedIds() {
  const store = readStore()
  return store[getUserKey()] || []
}
