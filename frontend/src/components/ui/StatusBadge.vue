<template>
  <span class="status-badge" :class="classes">
    <span v-if="dot" class="status-badge__dot" />
    <span>{{ label }}</span>
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: {
    type: String,
    required: true
  },
  tone: {
    type: String,
    default: 'neutral'
  },
  dot: {
    type: Boolean,
    default: true
  },
  subtle: {
    type: Boolean,
    default: false
  }
})

const classes = computed(() => [
  `status-badge--${props.tone}`,
  props.subtle ? 'status-badge--subtle' : ''
])
</script>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
  white-space: nowrap;
}

.status-badge__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: currentColor;
}

.status-badge--neutral {
  color: var(--text-secondary);
  background: rgba(100, 116, 139, 0.1);
  border-color: rgba(100, 116, 139, 0.16);
}

.status-badge--accent {
  color: var(--accent-primary-strong);
  background: rgba(38, 112, 233, 0.1);
  border-color: rgba(38, 112, 233, 0.16);
}

.status-badge--teal {
  color: var(--accent-teal-strong);
  background: rgba(53, 212, 198, 0.12);
  border-color: rgba(53, 212, 198, 0.18);
}

.status-badge--success {
  color: var(--success-strong);
  background: rgba(18, 184, 134, 0.12);
  border-color: rgba(18, 184, 134, 0.18);
}

.status-badge--warning {
  color: var(--warning-strong);
  background: rgba(255, 159, 67, 0.14);
  border-color: rgba(255, 159, 67, 0.18);
}

.status-badge--danger {
  color: var(--danger-strong);
  background: rgba(250, 82, 82, 0.12);
  border-color: rgba(250, 82, 82, 0.18);
}

.status-badge--subtle {
  background: transparent;
}
</style>
