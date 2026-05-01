import http from 'k6/http'
import { check, sleep } from 'k6'

export const options = {
  vus: Number(__ENV.K6_VUS || 5),
  iterations: Number(__ENV.K6_ITERATIONS || 20)
}

const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:8080'
const token = __ENV.ACCESS_TOKEN || ''

export default function () {
  const response = http.get(`${baseUrl}/api/inventory/list?page=1&size=10`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })

  check(response, {
    'inventory list status is 200': (res) => res.status === 200,
    'inventory list returns paged payload': (res) => res.json('data.records') !== undefined
  })

  sleep(1)
}
