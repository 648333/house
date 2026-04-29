const TAG_EMOJI = {
  '近地铁': '🚇',
  '采光好': '☀️',
  '学区房': '🎓',
  '带车位': '🅿️',
  '精装修': '✨',
  '宠物友好': '🐾',
  '拎包入住': '🧳',
  '安静社区': '🌿',
  '江景': '🏞️',
  '花园洋房': '🏡',
  '安防智能': '🔐',
  '儿童友好': '🧒',
  '海景': '🌊',
  '别墅': '🏰',
  '高端社区': '💎',
  '治愈风': '🍃',
  '改善型': '🌟',
}

export function formatTag(tag) {
  const trimmed = String(tag || '').trim()
  const emoji = TAG_EMOJI[trimmed]
  return emoji ? `${emoji} ${trimmed}` : trimmed
}

export default TAG_EMOJI

