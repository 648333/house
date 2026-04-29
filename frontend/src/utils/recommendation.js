import { getPropertyTags } from './property'

function addTagsToScore(scoreMap, property, weight) {
  getPropertyTags(property).forEach((tag) => {
    scoreMap.set(tag, (scoreMap.get(tag) || 0) + weight)
  })
}

export function buildPreferenceProfile({ favorites = [], appointments = [], reviews = [] }) {
  const scoreMap = new Map()

  favorites.forEach((property) => addTagsToScore(scoreMap, property, 2))
  appointments.forEach((appointment) => addTagsToScore(scoreMap, appointment.property, 3))
  reviews.forEach((review) => addTagsToScore(scoreMap, review.property, Math.max(review.rating || 0, 1)))

  return scoreMap
}

export function explainRecommendation(property, preferenceProfile) {
  if (!preferenceProfile || preferenceProfile.size === 0) {
    return property?.area >= 100 ? '空间尺度舒适，适合改善型居住需求' : '户型清晰、通勤友好，适合日常稳定居住'
  }

  const matchingTags = getPropertyTags(property)
    .map((tag) => ({ tag, score: preferenceProfile.get(tag) || 0 }))
    .filter((item) => item.score > 0)
    .sort((left, right) => right.score - left.score)
    .slice(0, 2)
    .map((item) => item.tag)

  if (matchingTags.length > 0) {
    return `偏好标签与 ${matchingTags.join('、')} 更接近`
  }

  if (property?.price && Number(property.price) <= 450) {
    return '总价区间更友好，适合预算控制与首置选择'
  }

  return '综合热度和近期互动表现，进入优先展示列表'
}
