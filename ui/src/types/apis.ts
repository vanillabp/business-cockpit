import { WakeupSseCallback } from "@vanillabp/bc-shared";
import { UserTask, Workflow, Workflows } from "@vanillabp/bc-official-gui-client";
import { UserTasks } from "@vanillabp/bc-official-gui-client/dist/models/UserTasks";

/**
 * This helps to initialize the given API hook late by
 * first passing the empty reference and setting
 * 'current' property later.
 */
export interface WakeupSseCallbackReference {
  current: WakeupSseCallback | undefined;
}

type TasklistApiCall = (listId: string, pageNumber: number, pageSize: number, initialTimestamp?: Date) => Promise<UserTasks>;

type TasklistUpdateCall = (listId: string, size: number, knownUserTasksIds: Array<string>, initialTimestamp?: Date) => Promise<UserTasks>;

type TasklistMarkAsReadCall = (userTaskId: string, unread?: boolean) => void;

type GetUserTaskCall = (userTaskId: string, markAsRead?: boolean) => Promise<UserTask>;

export interface TasklistApi {
  getUserTasks: TasklistApiCall,
  getUserTasksUpdate: TasklistUpdateCall,
  markUserTaskAsRead: TasklistMarkAsReadCall,
  getUserTask: GetUserTaskCall,
}

type GetWorkflowsApiCall = (listId: string, pageNumber: number, pageSize: number, initialTimestamp?: Date) => Promise<Workflows>;

type GetWorkflowsUpdateApiCall = (listId: string, size: number, knownWorkflowsIds: Array<string>, initialTimestamp?: Date) => Promise<Workflows>;

type GetUserTasksOfWorkflowApiCall = (workflowId: string, activeOnlyRequested?: boolean, limitListAccordingToCurrentUsersPermissions?: boolean) => Promise<Array<UserTask>>;

type GetWorkflowApiCall = (workflowId: string) => Promise<Workflow>;

export interface WorkflowlistApi {
  getUserTasksOfWorkflow: GetUserTasksOfWorkflowApiCall,
  getWorkflows: GetWorkflowsApiCall,
  getWorkflowsUpdate: GetWorkflowsUpdateApiCall,
  getWorkflow: GetWorkflowApiCall,
}

export type WorkflowlistApiHook = (wakeupSseCallback?: WakeupSseCallbackReference) => WorkflowlistApi;

export type TasklistApiHook = (wakeupSseCallback?: WakeupSseCallbackReference) => TasklistApi;
