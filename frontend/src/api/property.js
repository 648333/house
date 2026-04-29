import axios from 'axios'
import authHeader from './auth-header'

const API_URL = '/properties'

class PropertyService {
  getAllProperties() {
    return axios.get(API_URL)
  }

  getRecommendations(limit = 6, explain = false) {
    return axios.get(`${API_URL}/recommendations`, {
      headers: authHeader(),
      params: { limit, explain },
    })
  }

  getProperty(id) {
    return axios.get(API_URL + '/' + id)
  }

  createProperty(property) {
    return axios.post(API_URL, property, { headers: authHeader() })
  }

  updateProperty(id, property) {
    return axios.put(`${API_URL}/${id}`, property, { headers: authHeader() })
  }

  getMyProperties() {
      return axios.get(API_URL + '/my', { headers: authHeader() })
  }

  searchProperties(title) {
      return axios.get(API_URL + '/search', { params: { title } })
  }

  updateStatus(id, status) {
      return axios.put(`${API_URL}/${id}/status`, null, {
        headers: authHeader(),
        params: { status }
      })
  }

  getCommuteFilteredProperties(params) {
    return axios.get(`${API_URL}/commute-filter`, { params })
  }
}

export default new PropertyService()
