<script setup lang="ts">
type TaskFilter = 'all' | 'open' | 'closed'

interface Option {
  label: string
  value: TaskFilter
  background: string
}

defineProps<{
  modelValue: TaskFilter
}>()

const emit = defineEmits<{
  'update:modelValue': [value: TaskFilter]
}>()

const options: Option[] = [
  { label: 'All Tasks', value: 'all', background: '#f2f2f2' },
  { label: 'Open Tasks', value: 'open', background: 'rgba(0, 200, 0, 0.2)' },
  { label: 'Closed Tasks', value: 'closed', background: 'rgba(200, 0, 0, 0.2)' },
]
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
