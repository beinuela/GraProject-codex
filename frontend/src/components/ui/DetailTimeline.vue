<template>
  <div class="detail-timeline">
    <div v-for="(item, index) in items" :key="index" class="detail-timeline__item">
      <div class="detail-timeline__rail">
        <span class="detail-timeline__dot" :class="resolveTone(item)" />
        <span v-if="index < items.length - 1" class="detail-timeline__line" />
      </div>
      <div class="detail-timeline__content">
        <div class="detail-timeline__header">
          <StatusBadge :label="resolveBadge(item)" :tone="resolveTone(item)" />
          <span class="detail-timeline__time">{{ resolveTime(item) }}</span>
        </div>
        <strong class="detail-timeline__title">{{ resolveTitle(item) }}</strong>
        <div class="detail-timeline__meta">
          <span v-if="resolveMeta(item)">{{ resolveMeta(item) }}</span>
        </div>
      </div>
    </div>
    <EmptyState
      v-if="!items.length"
      compact
      glyph="TL"
      title="暂无业务轨迹"
      description="当前记录尚未产生可追踪的业务节点。"
    />
  </div>
</template>

<script setup>
import EmptyState from './EmptyState.vue'
import StatusBadge from './StatusBadge.vue'

const props = defineProps({
  items: {
    type: Array,
    default: () => []
  },
  titleKey: {
    type: String,
    default: 'title'
  },
  timeKey: {
    type: String,
    default: 'time'
  },
  metaKey: {
    type: String,
    default: 'meta'
  },
  badgeKey: {
    type: String,
    default: 'badge'
  },
  toneResolver: {
    type: Function,
    default: null
  }
})

const resolveTitle = (item) => item?.[props.titleKey] || item?.detail || '未命名节点'
const resolveTime = (item) => item?.[props.timeKey] || item?.createdAt || ''
const resolveMeta = (item) => item?.[props.metaKey] || ''
const resolveBadge = (item) => item?.[props.badgeKey] || item?.operation || '节点'
const resolveTone = (item) => props.toneResolver?.(item) || item?.tone || 'neutral'
</script>

<style scoped>
.detail-timeline {
  display: grid;
  gap: 16px;
}

.detail-timeline__item {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: 14px;
}

.detail-timeline__rail {
  position: relative;
  display: flex;
  justify-content: center;
}

.detail-timeline__dot {
  position: relative;
  z-index: 1;
  width: 14px;
  height: 14px;
  border-radius: 999px;
  background: rgba(100, 116, 139, 0.3);
  margin-top: 6px;
}

.detail-timeline__line {
  position: absolute;
  top: 26px;
  bottom: -18px;
  width: 2px;
  background: linear-gradient(180deg, rgba(38, 112, 233, 0.18), rgba(20, 54, 107, 0.06));
}

.success {
  background: var(--success);
}

.accent {
  background: var(--accent-primary);
}

.warning {
  background: var(--warning);
}

.danger {
  background: var(--danger);
}

.teal {
  background: var(--accent-teal);
}

.detail-timeline__content {
  display: grid;
  gap: 8px;
  padding: 14px 16px;
  border: 1px solid rgba(20, 54, 107, 0.08);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.72);
}

.detail-timeline__header {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.detail-timeline__time {
  color: var(--text-tertiary);
  font-size: 12px;
  font-family: var(--font-mono);
}

.detail-timeline__title {
  color: var(--text-primary);
  font-size: 15px;
}

.detail-timeline__meta {
  color: var(--text-secondary);
  font-size: 13px;
}
</style>
