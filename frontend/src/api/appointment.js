import axios from 'axios'
import authHeader from './auth-header'

const API_URL = '/appointments'

export function createAppointment(data) {
  return axios.post(API_URL, data, { headers: authHeader() })
}

export function getMyAppointments() {
    return axios.get(API_URL + '/my', { headers: authHeader() })
}

export function getAgentAppointments() {
  return axios.get(API_URL + '/agent', { headers: authHeader() })
}

export function updateAppointmentStatus(id, status) {
  return axios.put(`${API_URL}/${id}/status`, null, {
    headers: authHeader(),
    params: { status }
  })
}

export function getAppointmentTimeline(id) {
  return axios.get(`${API_URL}/${id}/timeline`, { headers: authHeader() })
}
