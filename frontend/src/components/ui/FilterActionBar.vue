<template>
  <section class="filter-action-bar">
    <div v-if="$slots.filters || $slots.default" class="filter-action-bar__filters">
      <slot name="filters">
        <slot />
      </slot>
    </div>
    <div v-if="$slots.actions" class="filter-action-bar__actions">
      <slot name="actions" />
    </div>
  </section>
</template>

<style scoped>
.filter-action-bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 16px;
  border: 1px solid rgba(20, 54, 107, 0.08);
  border-radius: var(--radius-xl);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(248, 250, 253, 0.94));
}

.filter-action-bar__filters,
.filter-action-bar__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-action-bar__filters {
  flex: 1;
  min-width: 0;
}

.filter-action-bar__actions {
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .filter-action-bar {
    flex-direction: column;
  }

  .filter-action-bar__actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
