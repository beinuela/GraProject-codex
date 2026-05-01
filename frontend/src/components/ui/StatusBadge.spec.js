import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import StatusBadge from './StatusBadge.vue'

describe('StatusBadge', () => {
  it('renders the label, dot, and tone modifier', () => {
    const wrapper = mount(StatusBadge, {
      props: {
        label: '已完成',
        tone: 'success'
      }
    })

    expect(wrapper.text()).toContain('已完成')
    expect(wrapper.classes()).toContain('status-badge--success')
    expect(wrapper.find('.status-badge__dot').exists()).toBe(true)
  })

  it('supports subtle badges without a dot', () => {
    const wrapper = mount(StatusBadge, {
      props: {
        label: '待处理',
        tone: 'warning',
        dot: false,
        subtle: true
      }
    })

    expect(wrapper.classes()).toContain('status-badge--warning')
    expect(wrapper.classes()).toContain('status-badge--subtle')
    expect(wrapper.find('.status-badge__dot').exists()).toBe(false)
  })
})
