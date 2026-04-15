<template>
  <DataPanel :title="title" :description="description" variant="subtle">
    <div class="detail-section">
      <slot />
    </div>
  </DataPanel>
</template>

<script setup>
import DataPanel from './DataPanel.vue'

defineProps({
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ''
  }
})
</script>

<style scoped>
.detail-section {
  display: grid;
  gap: 16px;
}
</style>
