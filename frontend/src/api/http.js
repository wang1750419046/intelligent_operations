import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 60000,
})

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const response = error.response?.data
    const message = response?.message || error.message || 'Request failed'
    return Promise.reject(new Error(message))
  },
)

export default http
