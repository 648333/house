import axios from 'axios'
import authHeader from './auth-header'

const API_URL = '/reviews'

export function getPropertyReviews(propertyId) {
    return axios.get(`${API_URL}/property/${propertyId}`)
}

export function createReview(data) {
  return axios.post(API_URL, data, { headers: authHeader() })
}

export function getAllReviews() {
  return axios.get(API_URL, { headers: authHeader() })
}

export function deleteReview(id) {
  return axios.delete(`${API_URL}/${id}`, { headers: authHeader() })
}
