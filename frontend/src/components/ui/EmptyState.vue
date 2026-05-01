<template>
  <div class="empty-state" :class="compact ? 'empty-state--compact' : ''">
    <div class="empty-state__glyph">{{ glyph }}</div>
    <strong class="empty-state__title">{{ title }}</strong>
    <p v-if="description" class="empty-state__description">{{ description }}</p>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: '暂无数据'
  },
  description: {
    type: String,
    default: '当前条件下没有可展示的内容。'
  },
  glyph: {
    type: String,
    default: '00'
  },
  compact: {
    type: Boolean,
    default: false
  }
})
</script>

<style scoped>
.empty-state {
  display: grid;
  place-items: center;
  gap: 10px;
  min-height: 180px;
  padding: 20px;
  color: var(--text-secondary);
  text-align: center;
}

.empty-state--compact {
  min-height: 120px;
}

.empty-state__glyph {
  display: grid;
  place-items: center;
  width: 60px;
  height: 60px;
  border-radius: 20px;
  background: rgba(20, 54, 107, 0.06);
  color: var(--accent-primary-strong);
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 700;
}

.empty-state__title {
  color: var(--text-primary);
  font-size: 16px;
}

.empty-state__description {
  margin: 0;
  max-width: 34ch;
  line-height: 1.6;
}
</style>
