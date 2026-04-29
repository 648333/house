import axios from 'axios'
import authHeader from './auth-header'

const API_URL = '/users'

export function getAllUsers() {
  return axios.get(API_URL, { headers: authHeader() })
}

export function getCurrentUserProfile() {
  return axios.get(`${API_URL}/me`, { headers: authHeader() })
}

export function updateCurrentUserProfile(data) {
  return axios.put(`${API_URL}/me`, data, { headers: authHeader() })
}

export function updateUserStatus(id, enabled) {
  return axios.put(`${API_URL}/${id}/status`, null, { 
    headers: authHeader(),
    params: { enabled } 
  })
}

export function deleteUser(id) {
  return axios.delete(`${API_URL}/${id}`, { headers: authHeader() })
}
