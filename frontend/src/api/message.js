import axios from 'axios'
import authHeader from './auth-header'

const API_URL = '/messages'

export function getMyMessages() {
    return axios.get(API_URL + '/my', { headers: authHeader() })
}

export function getChatHistory(propertyId, otherUserId) {
  return axios.get(`${API_URL}/history`, { 
      params: { propertyId, otherUserId },
      headers: authHeader() 
  })
}

export function sendMessage(data) {
  return axios.post(API_URL, data, { headers: authHeader() })
}

export function markMessageAsRead(id) {
  return axios.patch(`${API_URL}/${id}/read`, {}, { headers: authHeader() })
}

export function markAllMyMessagesAsRead() {
  return axios.patch(`${API_URL}/read-all`, {}, { headers: authHeader() })
}

export function markChatHistoryAsRead(propertyId, otherUserId) {
  return axios.patch(`${API_URL}/history/read`, {}, {
    params: { propertyId, otherUserId },
    headers: authHeader(),
  })
}
