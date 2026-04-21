<template>
  <div class="pagination-bar">
    <el-pagination
      background
      :current-page="page"
      :page-size="size"
      :page-sizes="pageSizes"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      @current-change="$emit('current-change', $event)"
      @size-change="$emit('size-change', $event)"
    />
  </div>
</template>

<script setup>
defineProps({
  page: {
    type: Number,
    required: true
  },
  size: {
    type: Number,
    required: true
  },
  total: {
    type: Number,
    required: true
  },
  pageSizes: {
    type: Array,
    default: () => [10, 20, 50]
  }
})

defineEmits(['current-change', 'size-change'])
</script>

<style scoped>
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding-top: 14px;
}

@media (max-width: 768px) {
  .pagination-bar {
    justify-content: center;
  }
}
</style>
