import axios from 'axios'
import authHeader from './auth-header'

const API_URL = '/ml/recommendations'

export function exportMlDataset() {
  return axios.get(`${API_URL}/dataset`, { headers: authHeader() })
}

export function importMlPredictions(payload) {
  return axios.post(`${API_URL}/predictions/import`, payload, { headers: authHeader() })
}
