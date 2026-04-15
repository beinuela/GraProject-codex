<template>
  <div class="metric-strip" :class="[`metric-strip--${columns}`]">
    <article
      v-for="metric in metrics"
      :key="metric.label"
      class="metric-card"
      :class="metric.tone ? `metric-card--${metric.tone}` : 'metric-card--default'"
    >
      <div class="metric-card__icon" :class="metric.icon ? '' : 'metric-card__icon--ghost'">
        <component :is="metric.icon" v-if="metric.icon" />
        <span v-else>{{ metric.abbr || metric.label.slice(0, 1) }}</span>
      </div>
      <div class="metric-card__body">
        <span class="metric-card__label">{{ metric.label }}</span>
        <strong class="metric-card__value">
          {{ metric.value }}
          <small v-if="metric.suffix">{{ metric.suffix }}</small>
        </strong>
        <span v-if="metric.helper" class="metric-card__helper">{{ metric.helper }}</span>
      </div>
    </article>
  </div>
</template>

<script setup>
const props = defineProps({
  metrics: {
    type: Array,
    default: () => []
  },
  columns: {
    type: String,
    default: 'auto'
  }
})
</script>

<style scoped>
.metric-strip {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.metric-card {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  padding: 18px;
  min-height: 108px;
  border-radius: var(--radius-xl);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(247, 250, 255, 0.92));
  border: 1px solid var(--border-subtle);
  box-shadow: var(--shadow-soft);
  transition: transform var(--motion-base), box-shadow var(--motion-base), border-color var(--motion-base);
}

.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card);
}

.metric-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 16px;
  background: rgba(38, 112, 233, 0.1);
  color: var(--accent-primary-strong);
  font-size: 20px;
}

.metric-card__icon--ghost {
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 700;
}

.metric-card__body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.metric-card__label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--text-tertiary);
}

.metric-card__value {
  display: flex;
  align-items: baseline;
  gap: 6px;
  font-family: var(--font-display);
  font-size: clamp(24px, 2.4vw, 32px);
  line-height: 1;
  letter-spacing: -0.04em;
  color: var(--text-primary);
}

.metric-card__value small {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-tertiary);
  letter-spacing: 0;
}

.metric-card__helper {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.4;
}

.metric-card--accent {
  border-color: rgba(38, 112, 233, 0.18);
}

.metric-card--accent .metric-card__icon {
  background: rgba(38, 112, 233, 0.14);
  color: var(--accent-primary);
}

.metric-card--teal {
  border-color: rgba(53, 212, 198, 0.18);
}

.metric-card--teal .metric-card__icon {
  background: rgba(53, 212, 198, 0.16);
  color: var(--accent-teal-strong);
}

.metric-card--warning {
  border-color: rgba(255, 159, 67, 0.2);
}

.metric-card--warning .metric-card__icon {
  background: rgba(255, 159, 67, 0.16);
  color: var(--warning-strong);
}

.metric-card--danger {
  border-color: rgba(250, 82, 82, 0.2);
}

.metric-card--danger .metric-card__icon {
  background: rgba(250, 82, 82, 0.16);
  color: var(--danger-strong);
}

.metric-card--neutral .metric-card__icon {
  background: rgba(15, 23, 42, 0.08);
  color: var(--text-primary);
}
</style>
