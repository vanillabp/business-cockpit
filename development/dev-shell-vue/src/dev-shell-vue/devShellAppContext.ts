import { inject, type InjectionKey } from 'vue'
import { useRouter } from 'vue-router'
import {
  Configuration as GuiConfiguration,
  OfficialTasklistApi,
  OfficialWorkflowlistApi,
  type UserTask,
  type Workflow,
} from '@vanillabp/bc-official-gui-client'
import type { BcUserTask, BcWorkflow } from '@vanillabp/bc-types'

export const tasklistApiKey: InjectionKey<OfficialTasklistApi> = Symbol('tasklistApi')
export const workflowlistApiKey: InjectionKey<OfficialWorkflowlistApi> = Symbol('workflowlistApi')

export function createApis(basePath: string) {
  const config = new GuiConfiguration({ basePath })
  return {
    tasklistApi: new OfficialTasklistApi(config),
    workflowlistApi: new OfficialWorkflowlistApi(config),
  }
}

export function useDevShellAppContext() {
  const tasklistApi = inject(tasklistApiKey)!
  const workflowlistApi = inject(workflowlistApiKey)!
  const router = useRouter()

  function mapUserTask(userTask: UserTask): BcUserTask {
    return {
      ...userTask,
      open: () => router.push(`/task/${userTask.id}`),
      navigateToWorkflow: () => router.push(`/workflow/${userTask.workflowId}`),
      assign: (userId: string) => {
        tasklistApi.assignTask({ userTaskId: userTask.id, userId }).catch(console.warn)
      },
      unassign: (userId: string) => {
        tasklistApi.assignTask({ userTaskId: userTask.id, userId: '' }).catch(console.warn)
      },
      claim: () => {
        tasklistApi.claimTask({ userTaskId: userTask.id }).catch(console.warn)
      },
      unclaim: () => {
        tasklistApi.claimTask({ userTaskId: userTask.id, unclaim: true }).catch(console.warn)
      },
    } as BcUserTask
  }

  function mapWorkflow(workflow: Workflow): BcWorkflow {
    return {
      ...workflow,
      navigateToWorkflow: () => router.push(`/workflow/${workflow.id}`),
      getUserTasks: async (activeOnly: boolean, limitListAccordingToCurrentUsersPermissions: boolean) => {
        const userTasks = await workflowlistApi.getUserTasksOfWorkflow({
          workflowId: workflow.id,
          llatcup: limitListAccordingToCurrentUsersPermissions,
          userTasksRequest: {
            mode: activeOnly ? 'OpenTasks' : 'All',
          },
        })
        return userTasks.map(mapUserTask)
      },
    }
  }

  async function loadUserTask(userTaskId: string): Promise<BcUserTask | null> {
    try {
      const userTask = await tasklistApi.getUserTask({ userTaskId })
      return mapUserTask(userTask)
    } catch (err) {
      console.error(`Error loading task '${userTaskId}'`, err)
      return null
    }
  }

  async function loadWorkflow(workflowId: string): Promise<BcWorkflow | null> {
    try {
      const workflow = await workflowlistApi.getWorkflow({ workflowId })
      return mapWorkflow(workflow)
    } catch (err) {
      console.error(`Error loading workflow '${workflowId}'`, err)
      return null
    }
  }

  async function loadUserTasks(
    workflowId: string,
    activeOnly: boolean,
    llatcup: boolean,
  ): Promise<BcUserTask[]> {
    try {
      const userTasks = await workflowlistApi.getUserTasksOfWorkflow({
        workflowId,
        llatcup,
        userTasksRequest: {
          mode: activeOnly ? 'OpenTasks' : 'All',
        },
      })
      return userTasks.map(mapUserTask)
    } catch (err) {
      console.error(`Error loading tasks for workflow '${workflowId}'`, err)
      return []
    }
  }

  return {
    tasklistApi,
    workflowlistApi,
    mapUserTask,
    mapWorkflow,
    loadUserTask,
    loadWorkflow,
    loadUserTasks,
  }
}
