<template>
  <div class="page-scaffold" :class="`page-scaffold--${resolvedPageType}`">
    <PageHero
      :eyebrow="resolvedEyebrow"
      :title="resolvedTitle"
      :description="resolvedDescription"
      :page-type="resolvedPageType"
    >
      <template #meta>
        <slot name="hero-meta" />
      </template>
      <template #actions>
        <slot name="hero-actions" />
      </template>
    </PageHero>

    <MetricStrip v-if="metrics.length" :metrics="metrics" />

    <div class="page-scaffold__content">
      <slot />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import MetricStrip from './MetricStrip.vue'
import PageHero from './PageHero.vue'

const props = defineProps({
  title: {
    type: String,
    default: ''
  },
  description: {
    type: String,
    default: ''
  },
  eyebrow: {
    type: String,
    default: ''
  },
  pageType: {
    type: String,
    default: ''
  },
  metrics: {
    type: Array,
    default: () => []
  }
})

const route = useRoute()

const resolvedTitle = computed(() => props.title || route.meta.title || '未命名页面')
const resolvedDescription = computed(() => props.description || route.meta.description || '')
const resolvedEyebrow = computed(() => props.eyebrow || route.meta.section || '业务视图')
const resolvedPageType = computed(() => props.pageType || route.meta.pageType || 'standard')
</script>

<style scoped>
.page-scaffold {
  display: grid;
  gap: 18px;
}

.page-scaffold__content {
  display: grid;
  gap: 18px;
}
</style>
