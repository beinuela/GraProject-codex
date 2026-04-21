import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ElMessage } from 'element-plus'
import { captureFrontendError } from '../monitoring'
import http from './http'

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn()
  }
}))

vi.mock('../monitoring', () => ({
  captureFrontendError: vi.fn()
}))

describe('http client', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('attaches bearer token and unwraps successful payloads', async () => {
    localStorage.setItem('token', 'unit-test-token')

    const result = await http.get('/inventory', {
      adapter: async (config) => {
        expect(config.headers.Authorization).toBe('Bearer unit-test-token')
        return {
          data: { code: 0, data: { ok: true } },
          status: 200,
          statusText: 'OK',
          headers: {},
          config
        }
      }
    })

    expect(result).toEqual({ ok: true })
  })

  it('refreshes once on 401 and retries the original request', async () => {
    localStorage.setItem('token', 'expired-token')
    localStorage.setItem('refreshToken', 'refresh-token-1')

    axios.post = vi.fn().mockResolvedValue({
      data: {
        code: 0,
        data: {
          accessToken: 'new-token',
          refreshToken: 'refresh-token-2'
        }
      }
    })

    const result = await http.get('/secure-resource', {
      adapter: async (config) => {
        if (!config.__retry) {
          const error = new Error('unauthorized')
          error.config = config
          error.response = { status: 401, data: { message: 'token expired' } }
          throw error
        }

        expect(config.headers.Authorization).toBe('Bearer new-token')
        return {
          data: { code: 0, data: { reloaded: true } },
          status: 200,
          statusText: 'OK',
          headers: {},
          config
        }
      }
    })

    expect(result).toEqual({ reloaded: true })
    expect(axios.post).toHaveBeenCalledWith(
      '/api/auth/refresh',
      { refreshToken: 'refresh-token-1' },
      { baseURL: '/', timeout: 15000 }
    )
    expect(localStorage.getItem('token')).toBe('new-token')
    expect(localStorage.getItem('refreshToken')).toBe('refresh-token-2')
  })

  it('reports server-side failures to monitoring', async () => {
    const failingRequest = http.get('/explode', {
      adapter: async (config) => {
        const error = new Error('server exploded')
        error.config = config
        error.response = {
          status: 500,
          data: { message: 'server exploded' }
        }
        throw error
      }
    })

    await expect(failingRequest).rejects.toThrow('server exploded')
    expect(captureFrontendError).toHaveBeenCalled()
    expect(ElMessage.error).toHaveBeenCalledWith('server exploded')
  })
})
