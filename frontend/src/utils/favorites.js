const FAVORITES_KEY = 'favorite_properties'

function getUserKey() {
  const user = JSON.parse(localStorage.getItem('user') || 'null')
  return user?.id ? String(user.id) : 'guest'
}

function readStore() {
  return JSON.parse(localStorage.getItem(FAVORITES_KEY) || '{}')
}

function writeStore(store) {
  localStorage.setItem(FAVORITES_KEY, JSON.stringify(store))
}

export function getFavorites() {
  const store = readStore()
  return store[getUserKey()] || []
}

export function isFavorite(propertyId) {
  return getFavorites().includes(Number(propertyId))
}

export function toggleFavorite(propertyId) {
  const store = readStore()
  const userKey = getUserKey()
  const favorites = new Set(store[userKey] || [])
  const numericId = Number(propertyId)

  if (favorites.has(numericId)) {
    favorites.delete(numericId)
  } else {
    favorites.add(numericId)
  }

  store[userKey] = Array.from(favorites)
  writeStore(store)
  return store[userKey]
}
