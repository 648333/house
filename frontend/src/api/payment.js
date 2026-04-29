import axios from 'axios'
import authHeader from './auth-header'

const API_URL = '/payments'

export function createPaymentOrder(payload) {
  return axios.post(`${API_URL}/orders`, payload, { headers: authHeader() })
}

export function getPaymentOrder(id) {
  return axios.get(`${API_URL}/orders/${id}`, { headers: authHeader() })
}

export function payPaymentOrder(id, payload = { success: true }) {
  return axios.post(`${API_URL}/orders/${id}/pay`, payload, { headers: authHeader() })
}

export function getMyPaymentOrders() {
  return axios.get(`${API_URL}/my`, { headers: authHeader() })
}

export function getPaymentVoucher(id) {
  return axios.get(`${API_URL}/orders/${id}/voucher`, { headers: authHeader() })
}
