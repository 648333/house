const DEFAULT_PROPERTY_IMAGES = [
  'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80',
  'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80',
  'https://images.unsplash.com/photo-1484154218962-a197022b5858?auto=format&fit=crop&w=1200&q=80',
  'https://images.unsplash.com/photo-1460317442991-0ec209397118?auto=format&fit=crop&w=1200&q=80',
  'https://images.unsplash.com/photo-1449844908441-8829872d2607?auto=format&fit=crop&w=1200&q=80',
]

const DEFAULT_FLOORPLANS = {
  family: '/floorplans/family-a.svg',
  compact: '/floorplans/compact-b.svg',
  loft: '/floorplans/loft-c.svg',
}

const DEFAULT_MODELS = {
  family: '/models/house-interiors.glb',
  compact: '/models/apartment-2.glb',
  loft: '/models/apartment-2.glb',
}

export function getPropertyImage(property, seed = 0) {
  if (property?.imageUrl && String(property.imageUrl).trim()) {
    return property.imageUrl
  }

  const fallbackSeed = Number(property?.id || seed || 0)
  return DEFAULT_PROPERTY_IMAGES[fallbackSeed % DEFAULT_PROPERTY_IMAGES.length]
}

function detectLayoutCategory(property, seed = 0) {
  const titleText = String(property?.title || '').toLowerCase()
  const layoutText = String(property?.layout || '').toLowerCase()
  const combined = `${titleText} ${layoutText}`

  if (
    combined.includes('loft')
    || combined.includes('复式')
    || combined.includes('挑空')
    || combined.includes('跃层')
  ) {
    return 'loft'
  }

  if (
    combined.includes('一室')
    || combined.includes('两室')
    || combined.includes('1室')
    || combined.includes('2室')
    || combined.includes('小户型')
    || combined.includes('公寓')
  ) {
    return 'compact'
  }

  const fallbackSeed = Number(property?.id || seed || 0)
  return fallbackSeed % 3 === 1 ? 'compact' : fallbackSeed % 3 === 2 ? 'loft' : 'family'
}

function getFallbackFloorPlan(property, seed = 0) {
  return DEFAULT_FLOORPLANS[detectLayoutCategory(property, seed)]
}

function getFallbackModel(property, seed = 0) {
  return DEFAULT_MODELS[detectLayoutCategory(property, seed)]
}

function looksLikeRealModel(url) {
  const normalized = String(url || '').trim().toLowerCase()
  if (!normalized) return false

  return normalized.includes('/models/')
    || normalized.endsWith('.glb')
    || normalized.endsWith('.gltf')
}

function looksLikeRealFloorPlan(url) {
  const normalized = String(url || '').trim().toLowerCase()
  if (!normalized) return false

  return normalized.includes('/floorplans/')
    || normalized.endsWith('.svg')
    || normalized.includes('floorplan')
    || normalized.includes('huxing')
    || normalized.includes('户型')
}

export function normalizeProperty(property, seed = 0) {
  let panoramaImages = []
  let furnishingPlan = null

  if (Array.isArray(property?.panoramaImages)) {
    panoramaImages = property.panoramaImages
  } else if (typeof property?.panoramaImages === 'string' && property.panoramaImages.trim()) {
    try {
      panoramaImages = JSON.parse(property.panoramaImages)
    } catch (error) {
      panoramaImages = property.panoramaImages
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean)
    }
  }

  if (property?.furnishingPlan && typeof property.furnishingPlan === 'string') {
    try {
      furnishingPlan = JSON.parse(property.furnishingPlan)
    } catch (error) {
      furnishingPlan = null
    }
  } else if (property?.furnishingPlan && typeof property.furnishingPlan === 'object') {
    furnishingPlan = property.furnishingPlan
  }

  return {
    ...property,
    imageUrl: getPropertyImage(property, seed),
    floorPlanUrl: looksLikeRealFloorPlan(property?.floorPlanUrl)
      ? property.floorPlanUrl
      : getFallbackFloorPlan(property, seed),
    model3dUrl: looksLikeRealModel(property?.model3dUrl)
      ? property.model3dUrl
      : getFallbackModel(property, seed),
    panoramaImages,
    furnishingPlan,
  }
}

export function getPropertyTags(property) {
  if (!property?.tags) return []
  return String(property.tags)
    .split(',')
    .map((tag) => tag.trim())
    .filter(Boolean)
}
