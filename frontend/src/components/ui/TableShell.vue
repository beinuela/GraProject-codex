<template>
  <DataPanel :title="title" :description="description" :badge="badge" :variant="variant" :padded="false">
    <template #actions>
      <slot name="actions" />
    </template>
    <div class="table-shell">
      <slot />
    </div>
  </DataPanel>
</template>

<script setup>
import DataPanel from './DataPanel.vue'

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
  }
})
</script>

<style scoped>
.table-shell {
  padding: 0 6px 6px;
}
</style>
