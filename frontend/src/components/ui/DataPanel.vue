<template>
  <section class="data-panel" :class="[`data-panel--${variant}`, padded ? 'data-panel--padded' : '']">
    <header v-if="title || description || $slots.actions || $slots.header" class="data-panel__header">
      <slot name="header">
        <div class="data-panel__heading">
          <div>
            <h3 v-if="title" class="data-panel__title">{{ title }}</h3>
            <p v-if="description" class="data-panel__description">{{ description }}</p>
          </div>
          <span v-if="badge" class="data-panel__badge">{{ badge }}</span>
        </div>
        <div v-if="$slots.actions" class="data-panel__actions">
          <slot name="actions" />
        </div>
      </slot>
    </header>
    <div class="data-panel__body">
      <slot />
    </div>
    <footer v-if="$slots.footer" class="data-panel__footer">
      <slot name="footer" />
    </footer>
  </section>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: ''
  },
  description: {
    type: String,
    default: ''
  },
  badge: {
    type: String,
    default: ''
  },
  variant: {
    type: String,
    default: 'default'
  },
  padded: {
    type: Boolean,
    default: true
  }
})
</script>

<style scoped>
.data-panel {
  border-radius: var(--radius-2xl);
  border: 1px solid var(--border-subtle);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(249, 251, 255, 0.96));
  box-shadow: var(--shadow-panel);
}

.data-panel--subtle {
  background: linear-gradient(180deg, rgba(250, 252, 255, 0.98), rgba(245, 248, 252, 0.98));
}

.data-panel--elevated {
  border-color: rgba(20, 54, 107, 0.12);
  box-shadow: var(--shadow-card);
}

.data-panel--dark {
  color: rgba(240, 246, 255, 0.94);
  background: linear-gradient(180deg, rgba(10, 18, 36, 0.98), rgba(6, 12, 24, 0.96));
  border-color: rgba(87, 165, 255, 0.14);
}

.data-panel--padded .data-panel__body {
  padding: 0 20px 20px;
}

.data-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding: 20px 20px 14px;
}

.data-panel__heading {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.data-panel__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: inherit;
}

.data-panel__description {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--text-secondary);
}

.data-panel--dark .data-panel__description {
  color: rgba(195, 212, 235, 0.72);
}

.data-panel__badge {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(38, 112, 233, 0.09);
  color: var(--accent-primary-strong);
  font-size: 12px;
  font-weight: 700;
}

.data-panel__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.data-panel__footer {
  padding: 0 20px 20px;
}
</style>
