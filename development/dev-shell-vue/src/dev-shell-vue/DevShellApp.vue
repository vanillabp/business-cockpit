<script setup lang="ts">
import { type Component, getCurrentInstance, provide } from 'vue'
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import PrimeVue from 'primevue/config'
import Aura from '@primevue/themes/aura'
import { createApis, tasklistApiKey, workflowlistApiKey } from './devShellAppContext'
import MainPage from './MainPage.vue'
import UserTaskPage from './UserTaskPage.vue'
import UserTaskWrapper from './UserTaskWrapper.vue'
import WorkflowPage from './WorkflowPage.vue'
import WorkflowWrapper from './WorkflowWrapper.vue'

const props = defineProps<{
  officialApiUri: string
  userTaskForm: Component
  workflowPage: Component
  additionalComponents?: Record<string, Component>
}>()

// Create API instances
const { tasklistApi, workflowlistApi } = createApis(props.officialApiUri)
provide(tasklistApiKey, tasklistApi)
provide(workflowlistApiKey, workflowlistApi)

// Register PrimeVue on the current app instance
const app = getCurrentInstance()?.appContext.app
if (app) {
  try {
    app.use(PrimeVue, {
      theme: {
        preset: Aura,
      },
    })
  } catch {
    // PrimeVue may already be installed
  }
}

// Build routes
const additionalRouteNames = Object.keys(props.additionalComponents ?? {})
const additionalRoutes: RouteRecordRaw[] = additionalRouteNames.map((name) => ({
  path: `/${name}`,
  name,
  component: props.additionalComponents![name],
}))

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'main',
    component: MainPage,
    props: { additionalRoutes: additionalRouteNames },
  },
  {
    path: '/task',
    component: UserTaskPage,
    children: [
      {
        path: ':userTaskId',
        component: UserTaskWrapper,
        props: (route) => ({
          userTaskId: route.params.userTaskId,
          userTaskForm: props.userTaskForm,
        }),
      },
    ],
  },
  {
    path: '/workflow',
    component: WorkflowPage,
    children: [
      {
        path: ':workflowId',
        component: WorkflowWrapper,
        props: (route) => ({
          workflowId: route.params.workflowId,
          workflowPage: props.workflowPage,
        }),
      },
    ],
  },
  ...additionalRoutes,
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

if (app) {
  try {
    app.use(router)
  } catch {
    // Router may already be installed
  }
}
</script>

<template>
  <RouterView />
</template>
