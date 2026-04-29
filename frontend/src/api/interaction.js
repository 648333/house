import axios from 'axios'
import authHeader from './auth-header'

const API_URL = '/interactions'

export function trackInteraction(payload) {
  return axios.post(API_URL, payload, { headers: authHeader() })
}

export function getMyInteractions() {
  return axios.get(`${API_URL}/my`, { headers: authHeader() })
}
