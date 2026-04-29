import axios from 'axios'
import authHeader from './auth-header'

const API_URL = '/schedules'

export function getMyScheduleSlots() {
  return axios.get(`${API_URL}/mine`, { headers: authHeader() })
}

export function getAgentScheduleSlots(agentId) {
  return axios.get(`${API_URL}/agent/${agentId}`, { headers: authHeader() })
}

export function createScheduleSlot(data) {
  return axios.post(API_URL, data, { headers: authHeader() })
}

export function updateScheduleAvailability(id, available) {
  return axios.put(`${API_URL}/${id}/availability`, null, {
    headers: authHeader(),
    params: { available },
  })
}
