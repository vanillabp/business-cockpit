<script setup lang="ts">
interface ToggleOption {
  label: string
  value: string
  background: string
}

defineProps<{
  modelValue: string
  options: ToggleOption[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()
</script>

<template>
  <div class="task-toggle-container">
    <div
      v-for="option in options"
      :key="option.value"
      class="toggle-option"
      :class="{ active: modelValue === option.value }"
      :style="{ backgroundColor: option.background }"
      @click="emit('update:modelValue', option.value)"
    >
      {{ option.label }}
    </div>
  </div>
</template>

<style scoped>
.task-toggle-container {
  display: flex;
  flex-direction: row;
  gap: 4px;
  align-items: center;
  margin-left: 0.5rem;
}

.toggle-option {
  padding: 3px 10px;
  border-radius: 4px;
  border: 1px solid #ccc;
  color: #333;
  cursor: pointer;
  font-size: 0.75rem;
  transition: all 0.2s;
  background-color: white;
  white-space: nowrap;
}

.toggle-option:hover {
  background-color: #f0f0f0;
}

.toggle-option.active {
  border-color: #666;
  background-color: #e0e0e0;
  color: #333;
  font-weight: bold;
}
</style>
