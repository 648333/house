import axios from 'axios'

const API_URL = '/auth/'

class AuthService {
  login(user) {
    return axios
      .post(API_URL + 'login', {
        username: user.username,
        password: user.password
      })
      .then(response => {
        if (response.data.accessToken) {
          localStorage.setItem('user', JSON.stringify(response.data))
        }
        return response.data
      })
  }

  logout() {
    localStorage.removeItem('user')
  }

  register(user) {
    return axios.post(API_URL + 'register', {
      username: user.username,
      email: user.email,
      password: user.password,
      role: user.role // Ensure role is passed
    })
  }

  forgotPassword(email) {
    return axios.post(API_URL + 'forgot-password', { email })
  }

  resetPassword(payload) {
    return axios.post(API_URL + 'reset-password', payload)
  }
}

export default new AuthService()
