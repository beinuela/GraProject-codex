<template>
  <section class="page-hero" :class="`page-hero--${pageType}`">
    <div class="page-hero__content">
      <div class="page-hero__eyebrow">
        <span class="page-hero__section">{{ eyebrow }}</span>
        <span class="page-hero__type">{{ typeLabel }}</span>
      </div>
      <div class="page-hero__title-row">
        <h1 class="page-hero__title">{{ title }}</h1>
        <slot name="meta" />
      </div>
      <p v-if="description" class="page-hero__description">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="page-hero__actions">
      <slot name="actions" />
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  eyebrow: {
    type: String,
    default: '页面'
  },
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ''
  },
  pageType: {
    type: String,
    default: 'standard'
  }
})

const typeMap = {
  auth: '系统入口',
  screen: '指挥大屏',
  dashboard: '数据看板',
  analytics: '分析视图',
  workflow: '流程视图',
  monitor: '监控视图',
  standard: '业务页面'
}

const typeLabel = computed(() => typeMap[props.pageType] || typeMap.standard)
</script>

<style scoped>
.page-hero {
  position: relative;
  display: grid;
  gap: 20px;
  grid-template-columns: minmax(0, 1fr) auto;
  padding: 28px;
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-2xl);
  background:
    radial-gradient(circle at 0% 0%, rgba(38, 112, 233, 0.12), transparent 38%),
    radial-gradient(circle at 100% 100%, rgba(53, 212, 198, 0.1), transparent 28%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(245, 249, 255, 0.9));
  box-shadow: var(--shadow-panel);
  overflow: hidden;
}

.page-hero::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(120deg, transparent 10%, rgba(255, 255, 255, 0.46) 48%, transparent 85%);
  opacity: 0.38;
  pointer-events: none;
}

.page-hero--workflow {
  background:
    radial-gradient(circle at 0% 0%, rgba(255, 159, 67, 0.16), transparent 36%),
    radial-gradient(circle at 100% 100%, rgba(250, 82, 82, 0.08), transparent 28%),
    linear-gradient(135deg, rgba(255, 252, 248, 0.96), rgba(255, 246, 239, 0.92));
}

.page-hero--analytics,
.page-hero--dashboard {
  background:
    radial-gradient(circle at 0% 0%, rgba(38, 112, 233, 0.18), transparent 34%),
    radial-gradient(circle at 100% 100%, rgba(120, 86, 255, 0.08), transparent 28%),
    linear-gradient(135deg, rgba(246, 250, 255, 0.96), rgba(239, 247, 255, 0.92));
}

.page-hero__content {
  position: relative;
  z-index: 1;
  min-width: 0;
}

.page-hero__eyebrow {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}

.page-hero__section,
.page-hero__type {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.page-hero__section {
  background: rgba(20, 54, 107, 0.08);
  color: var(--accent-primary-strong);
}

.page-hero__type {
  background: rgba(53, 212, 198, 0.12);
  color: var(--accent-teal-strong);
}

.page-hero__title-row {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.page-hero__title {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(28px, 3vw, 40px);
  line-height: 1.02;
  letter-spacing: -0.04em;
  color: var(--text-primary);
}

.page-hero__description {
  margin: 14px 0 0;
  max-width: 68ch;
  color: var(--text-secondary);
  line-height: 1.7;
}

.page-hero__actions {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .page-hero {
    grid-template-columns: 1fr;
    padding: 22px;
  }

  .page-hero__actions {
    justify-content: flex-start;
  }
}
</style>
