import { shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import LoginView from './LoginView.vue'

const push = vi.fn()
const login = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push
  })
}))

vi.mock('../store/auth', () => ({
  useAuthStore: () => ({
    login
  })
}))

const stubs = {
  'el-form': { template: '<form><slot /></form>' },
  'el-form-item': { template: '<div><slot /></div>' },
  'el-input': { template: '<input />' },
  'el-button': { template: '<button><slot /></button>' },
  'el-icon': { template: '<i><slot /></i>' },
  Monitor: true,
  User: true,
  Lock: true,
  Warning: true
}

describe('LoginView', () => {
  beforeEach(() => {
    push.mockReset()
    login.mockReset()
  })

  it('shows a validation error when credentials are missing', async () => {
    const wrapper = shallowMount(LoginView, { global: { stubs } })

    await wrapper.vm.handleLogin()

    expect(login).not.toHaveBeenCalled()
    expect(wrapper.vm.errorMsg).toBe('请输入用户名和密码')
  })

  it('navigates to dashboard after a successful login', async () => {
    login.mockResolvedValue(undefined)
    const wrapper = shallowMount(LoginView, { global: { stubs } })

    wrapper.vm.form.username = 'admin'
    wrapper.vm.form.password = 'Abc@123456'
    await wrapper.vm.handleLogin()

    expect(login).toHaveBeenCalledWith('admin', 'Abc@123456')
    expect(push).toHaveBeenCalledWith('/dashboard')
    expect(wrapper.vm.errorMsg).toBe('')
  })

  it('renders backend login errors to the form', async () => {
    login.mockRejectedValue(new Error('用户名或密码错误'))
    const wrapper = shallowMount(LoginView, { global: { stubs } })

    wrapper.vm.form.username = 'admin'
    wrapper.vm.form.password = 'bad-password'
    await wrapper.vm.handleLogin()

    expect(wrapper.vm.errorMsg).toBe('用户名或密码错误')
  })
})
