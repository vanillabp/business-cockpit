<script setup lang="ts">
import { ref, watch, type Component } from 'vue'
import { useDevShellAppContext } from './devShellAppContext'
import type { BcWorkflow } from '@vanillabp/bc-types'

const props = defineProps<{
  workflowId: string
  workflowPage: Component
}>()

const { loadWorkflow } = useDevShellAppContext()
const workflow = ref<BcWorkflow | null>(null)
const loading = ref(true)
const error = ref(false)

async function load(id: string) {
  loading.value = true
  error.value = false
  const result = await loadWorkflow(id)
  if (result) {
    workflow.value = result
  } else {
    error.value = true
  }
  loading.value = false
}

load(props.workflowId)

watch(
  () => props.workflowId,
  (newId) => load(newId),
)
</script>

<template>
  <div v-if="loading" class="loading">Loading workflow...</div>
  <div v-else-if="error" class="error">Unknown workflow</div>
  <component v-else-if="workflow" :is="workflowPage" :workflow="workflow" />
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
