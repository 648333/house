import axios from 'axios'
import authHeader from './auth-header'

const API_URL = '/requirements'

export function getMyRequirements() {
  return axios.get(`${API_URL}/my`, { headers: authHeader() })
}

export function getAgentRequirements() {
  return axios.get(`${API_URL}/agent`, { headers: authHeader() })
}

export function createRequirement(data) {
  return axios.post(API_URL, data, { headers: authHeader() })
}

export function assignRequirement(id) {
  return axios.put(`${API_URL}/${id}/assign`, null, { headers: authHeader() })
}

export function updateRequirementStatus(id, status) {
  return axios.put(`${API_URL}/${id}/status`, null, {
    headers: authHeader(),
    params: { status },
  })
}
