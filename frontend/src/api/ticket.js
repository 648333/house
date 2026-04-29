import axios from 'axios'
import authHeader from './auth-header'

const API_URL = '/support-tickets'

export function createSupportTicket(payload) {
  return axios.post(API_URL, payload, { headers: authHeader() })
}

export function getMySupportTickets() {
  return axios.get(`${API_URL}/my`, { headers: authHeader() })
}

export function getAllSupportTickets() {
  return axios.get(API_URL, { headers: authHeader() })
}

export function updateSupportTicketStatus(id, status, handlerNote = '') {
  return axios.put(
    `${API_URL}/${id}/status`,
    { handlerNote },
    { headers: authHeader(), params: { status } }
  )
}

export function getSupportCategories() {
  return axios.get(`${API_URL}/categories`, { headers: authHeader() })
}
