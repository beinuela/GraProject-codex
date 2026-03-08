import http from './http'

export const apiGet = (url, params) => http.get(url, { params })
export const apiPost = (url, data, params) => http.post(url, data, { params })
export const apiDelete = (url) => http.delete(url)
