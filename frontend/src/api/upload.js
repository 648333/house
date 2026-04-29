import axios from 'axios'

export function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)

  return axios.post('/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}
