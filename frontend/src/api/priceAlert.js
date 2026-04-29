import axios from 'axios'
import authHeader from './auth-header'

const API_URL = '/price-alerts'

export function getMyPriceAlerts() {
  return axios.get(`${API_URL}/my`, { headers: authHeader() })
}

export function subscribePriceAlert(payload) {
  return axios.post(API_URL, payload, { headers: authHeader() })
}

export function unsubscribePriceAlert(propertyId) {
  return axios.delete(`${API_URL}/property/${propertyId}`, { headers: authHeader() })
}
