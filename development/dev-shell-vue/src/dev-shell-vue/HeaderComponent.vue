<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Select from 'primevue/select'
import TaskToggle from './TaskToggle.vue'

interface SelectOption {
  label: string
  value: string
}

interface UserTaskItem {
  id: string
  businessId: string
  taskDefinition: string
  bpmnProcessId: string
}

interface WorkflowItem {
  id: string
  businessId: string
  bpmnProcessId: string
}

interface Page {
  number: number
  totalPages: number
}

interface UserTasksResponse {
  userTasks: UserTaskItem[]
  page: Page
}

interface WorkflowsResponse {
  workflows: WorkflowItem[]
  page: Page
}

type TaskFilter = 'all' | 'open' | 'closed'
type ContentType = 'usertask' | 'workflow'

const route = useRoute()
const router = useRouter()

const contentType = ref<ContentType>('usertask')
const page = ref(0)
const hasMorePages = ref(true)
const isLoading = ref(false)
const taskFilter = ref<TaskFilter>('all')

// User task state
const userTasks = ref<UserTaskItem[]>([])
const userTaskOptions = ref<SelectOption[]>([])
const selectedTaskId = ref<string | undefined>(undefined)

// Workflow state
const workflows = ref<WorkflowItem[]>([])
const workflowOptions = ref<SelectOption[]>([])
const selectedWorkflowId = ref<string | undefined>(undefined)

const isUserTaskView = computed(() => contentType.value === 'usertask')
const isWorkflowView = computed(() => contentType.value === 'workflow')

const currentOptions = computed(() =>
  isUserTaskView.value ? userTaskOptions.value : workflowOptions.value,
)

const selectedValue = computed(() =>
  isUserTaskView.value ? selectedTaskId.value : selectedWorkflowId.value,
)

const placeholder = computed(() =>
  isUserTaskView.value ? 'Select a task' : 'Select a workflow',
)

function fetchUserTasks(pageToFetch: number) {
  if (isLoading.value) return
  isLoading.value = true

  let mode: string
  if (taskFilter.value === 'open') mode = 'OpenTasks'
  else if (taskFilter.value === 'closed') mode = 'ClosedTasksOnly'
  else mode = 'All'

  const request = {
    pageNumber: pageToFetch,
    pageSize: 20,
    sort: 'createdAt',
    sortAscending: false,
    mode,
  }

  fetch('/official-api/v1/usertask', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
    .then((res) => res.json())
    .then((data: UserTasksResponse) => {
      userTasks.value =
        pageToFetch === 0 ? data.userTasks : [...userTasks.value, ...data.userTasks]

      userTaskOptions.value = userTasks.value.map((task) => ({
        label: `${task.businessId || ''} | ${task.taskDefinition}/${task.bpmnProcessId} | (${task.id})`,
        value: task.id,
      }))

      page.value = pageToFetch
      hasMorePages.value = data.page.number + 1 < data.page.totalPages
      isLoading.value = false
    })
    .catch((error) => {
      console.error('Error fetching tasks:', error)
      if (pageToFetch === 0) {
        userTasks.value = []
        userTaskOptions.value = []
      }
      isLoading.value = false
      hasMorePages.value = false
    })
}

function fetchWorkflows(pageToFetch: number) {
  if (isLoading.value) return
  isLoading.value = true

  const request = {
    pageNumber: pageToFetch,
    pageSize: 20,
    sort: 'createdAt',
    sortAscending: false,
  }

  fetch('/official-api/v1/workflow', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
    .then((res) => res.json())
    .then((data: WorkflowsResponse) => {
      workflows.value =
        pageToFetch === 0 ? data.workflows : [...workflows.value, ...data.workflows]

      workflowOptions.value = workflows.value.map((wf) => ({
        label: `${wf.businessId || ''} | ${wf.bpmnProcessId} (${wf.id})`,
        value: wf.id,
      }))

      page.value = pageToFetch
      hasMorePages.value = data.page.number + 1 < data.page.totalPages
      isLoading.value = false
    })
    .catch((error) => {
      console.error('Error fetching workflows:', error)
      if (pageToFetch === 0) {
        workflows.value = []
        workflowOptions.value = []
      }
      isLoading.value = false
      hasMorePages.value = false
    })
}

function loadMore() {
  if (!isLoading.value && hasMorePages.value) {
    const nextPage = page.value + 1
    if (isUserTaskView.value) {
      fetchUserTasks(nextPage)
    } else {
      fetchWorkflows(nextPage)
    }
  }
}

function onItemSelect(itemId: string | undefined) {
  if (isUserTaskView.value && itemId) {
    selectedTaskId.value = itemId
    router.push(`/task/${itemId}`)
  } else if (isWorkflowView.value && itemId) {
    selectedWorkflowId.value = itemId
    router.push(`/workflow/${itemId}`)
  } else if (!itemId) {
    if (isUserTaskView.value) router.push('/task')
    else router.push('/workflow')
  }
}

function onFilterChange(filter: TaskFilter) {
  taskFilter.value = filter
  page.value = 0
  hasMorePages.value = true
  isLoading.value = false

  if (isUserTaskView.value) {
    userTasks.value = []
    userTaskOptions.value = []
    fetchUserTasks(0)
  }
}

function navigateToView(view: string) {
  const currentBase = isUserTaskView.value
    ? `/task/${selectedTaskId.value}`
    : `/workflow/${selectedWorkflowId.value}`
  const targetRoute = view === 'form' || view === 'page' ? currentBase : `${currentBase}/${view}`

  if (route.path !== targetRoute) {
    router.push(targetRoute)
  }
}

onMounted(() => {
  const url = route.path
  if (url.includes('/workflow')) {
    contentType.value = 'workflow'
    selectedWorkflowId.value = (route.params.workflowId as string) || undefined
    fetchWorkflows(0)
  } else {
    contentType.value = 'usertask'
    selectedTaskId.value = (route.params.userTaskId as string) || undefined
    fetchUserTasks(0)
  }
})

watch(
  () => route.params,
  (params) => {
    const newTaskId = params.userTaskId as string | undefined
    const newWorkflowId = params.workflowId as string | undefined
    if (isUserTaskView.value && newTaskId !== selectedTaskId.value) {
      selectedTaskId.value = newTaskId
    }
    if (isWorkflowView.value && newWorkflowId !== selectedWorkflowId.value) {
      selectedWorkflowId.value = newWorkflowId
    }
  },
)
</script>

<template>
  <div class="header-container">
    <div class="task-selection-container">
      <div class="select-container">
        <Select
          :modelValue="selectedValue"
          :options="currentOptions"
          optionLabel="label"
          optionValue="value"
          :placeholder="placeholder"
          :virtualScrollerOptions="{ itemSize: 38 }"
          filter
          showClear
          :loading="isLoading"
          :style="{ width: '100%' }"
          scrollHeight="250px"
          @update:modelValue="onItemSelect"
          @scroll.passive="loadMore"
        >
          <template #option="{ option }">
            <div class="flex items-center gap-2">
              <template v-if="option.label.includes('|')">
                <span class="font-bold">{{ option.label.split('|')[0].trim() }}</span>
                <span class="font-normal"> | {{ option.label.split('|').slice(1).join('|').trim() }}</span>
              </template>
              <template v-else>
                <span>{{ option.label }}</span>
              </template>
            </div>
          </template>
          <template #value="{ value, placeholder: ph }">
            <template v-if="value">
              <template v-for="opt in currentOptions" :key="opt.value">
                <div v-if="opt.value === value" class="flex items-center gap-2">
                  <template v-if="opt.label.includes('|')">
                    <span class="font-bold">{{ opt.label.split('|')[0].trim() }}</span>
                    <span class="font-normal"> | {{ opt.label.split('|').slice(1).join('|').trim() }}</span>
                  </template>
                  <template v-else>
                    <span>{{ opt.label }}</span>
                  </template>
                </div>
              </template>
            </template>
            <span v-else>{{ ph }}</span>
          </template>
          <template #footer>
            <div v-if="hasMorePages" class="more-button" @click="loadMore">
              Load more...
            </div>
          </template>
        </Select>
      </div>

      <TaskToggle v-if="isUserTaskView" :modelValue="taskFilter" @update:modelValue="onFilterChange" />
    </div>

    <div class="view-menu">
      <div class="dropdown">
        <template v-if="isUserTaskView">
          <button class="dropdown-button" :disabled="!selectedTaskId">Views</button>
          <div v-if="selectedTaskId" class="dropdown-content">
            <a @click="navigateToView('form')">form</a>
            <a @click="navigateToView('list')">list</a>
            <a @click="navigateToView('icon')">icon</a>
          </div>
        </template>
        <template v-if="isWorkflowView">
          <button class="dropdown-button" :disabled="!selectedWorkflowId">Views</button>
          <div v-if="selectedWorkflowId" class="dropdown-content">
            <a @click="navigateToView('page')">page</a>
            <a @click="navigateToView('list')">list</a>
            <a @click="navigateToView('icon')">icon</a>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.header-container {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem;
  gap: 0.5rem;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  min-height: 3.5rem;
  font-family: 'Arial', sans-serif;
  width: 100%;
}

.task-selection-container {
  display: flex;
  flex-direction: row;
  gap: 0.5rem;
  align-items: center;
  flex: 1;
}

.select-container {
  flex: 1;
  min-width: 0;
  position: relative;
}

.more-button {
  margin-top: 0.25rem;
  padding: 0.5rem;
  background-color: transparent;
  border: none;
  color: #444;
  cursor: pointer;
  text-align: center;
  font-size: 0.8rem;
}

.more-button:hover {
  color: #000;
  text-decoration: underline;
}

.view-menu {
  position: relative;
  flex-shrink: 0;
}

.dropdown {
  position: relative;
  display: inline-block;
}

.dropdown-button {
  padding: 0.5rem 1rem;
  background-color: white;
  border: 1px solid #ccc;
  color: #333;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.2s;
}

.dropdown-button:hover:not(:disabled) {
  background-color: #f0f0f0;
}

.dropdown-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.dropdown-content {
  display: none;
  position: absolute;
  right: 0;
  background-color: #ffffff;
  min-width: 120px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
  z-index: 1;
  border-radius: 4px;
  border: 1px solid #ccc;
}

.dropdown:hover .dropdown-content {
  display: block;
}

.dropdown-content a {
  padding: 0.5rem 1rem;
  display: block;
  color: #333;
  text-decoration: none;
  cursor: pointer;
}

.dropdown-content a:hover {
  background-color: #f0f0f0;
}
</style>
