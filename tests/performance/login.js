import http from 'k6/http'
import { check, sleep } from 'k6'

export const options = {
  vus: Number(__ENV.K6_VUS || 1),
  iterations: Number(__ENV.K6_ITERATIONS || 4)
}

const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:8080'

export default function () {
  const response = http.post(`${baseUrl}/api/auth/login`, JSON.stringify({
    username: __ENV.LOGIN_USERNAME || 'admin',
    password: __ENV.LOGIN_PASSWORD || 'Abc@123456'
  }), {
    headers: {
      'Content-Type': 'application/json'
    }
  })

  check(response, {
    'login status is 200': (res) => res.status === 200,
    'login payload code is 0': (res) => res.json('code') === 0
  })

  sleep(1)
}
