import type { Component } from 'vue'

export interface DevShellConfig {
  officialApiUri: string
  userTaskForm: Component
  workflowPage: Component
  additionalComponents?: Record<string, Component>
}
