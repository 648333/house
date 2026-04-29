import axios from 'axios'
import authHeader from './auth-header'

const API_URL = '/stats'

export function getDashboardStats() {
    return axios.get(API_URL + '/dashboard', { headers: authHeader() })
}

export function getAgentStats() {
    return axios.get(API_URL + '/agent', { headers: authHeader() })
}

export function getAdminStats() {
    return axios.get(API_URL + '/admin', { headers: authHeader() })
}
