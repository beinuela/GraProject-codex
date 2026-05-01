<template>
  <el-dialog
    :model-value="modelValue"
    :width="width"
    :top="top"
    :destroy-on-close="destroyOnClose"
    append-to-body
    class="shell-dialog"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <template #header>
      <div class="shell-dialog__header">
        <div class="shell-dialog__eyebrow">{{ eyebrow }}</div>
        <h3 class="shell-dialog__title">{{ title }}</h3>
        <p v-if="subtitle" class="shell-dialog__subtitle">{{ subtitle }}</p>
      </div>
    </template>

    <div class="shell-dialog__body">
      <slot />
    </div>

    <template #footer>
      <div class="shell-dialog__footer">
        <slot name="footer" />
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
const props = defineProps({
  modelValue: {
    type: Boolean,
    required: true
  },
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    default: ''
  },
  eyebrow: {
    type: String,
    default: '编辑内容'
  },
  width: {
    type: [String, Number],
    default: 620
  },
  top: {
    type: String,
    default: '7vh'
  },
  destroyOnClose: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])
</script>

<style scoped>
.shell-dialog__header {
  padding-right: 40px;
}

.shell-dialog__eyebrow {
  margin-bottom: 8px;
  color: var(--accent-primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.shell-dialog__title {
  margin: 0;
  font-size: 24px;
  font-family: var(--font-display);
  line-height: 1.1;
  color: var(--text-primary);
}

.shell-dialog__subtitle {
  margin: 10px 0 0;
  color: var(--text-secondary);
  line-height: 1.6;
}

.shell-dialog__body,
.shell-dialog__footer {
  padding-inline: 6px;
}

.shell-dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
