<script setup lang="ts">
import { ref, watch, type Component } from 'vue'
import { useDevShellAppContext } from './devShellAppContext'
import type { BcUserTask } from '@vanillabp/bc-types'

const props = defineProps<{
  userTaskId: string
  userTaskForm: Component
}>()

const { loadUserTask } = useDevShellAppContext()
const userTask = ref<BcUserTask | null>(null)
const loading = ref(true)
const error = ref(false)

async function load(id: string) {
  loading.value = true
  error.value = false
  const result = await loadUserTask(id)
  if (result) {
    userTask.value = result
  } else {
    error.value = true
  }
  loading.value = false
}

load(props.userTaskId)

watch(
  () => props.userTaskId,
  (newId) => load(newId),
)
</script>

<template>
  <div v-if="loading" class="loading">Loading task...</div>
  <div v-else-if="error" class="error">Unknown task</div>
  <component v-else-if="userTask" :is="userTaskForm" :userTask="userTask" />
</template>

<style scoped>
.loading,
.error {
  padding: 1rem;
  color: #666;
}

.error {
  color: #c00;
}
</style>
