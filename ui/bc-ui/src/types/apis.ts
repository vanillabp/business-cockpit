import { WakeupSseCallback } from "@vanillabp/bc-shared";
import {
  KwicResult,
  SearchQuery,
  UserSearchResult,
  UserTask,
  Workflow,
  Workflows
} from "@vanillabp/bc-official-gui-client";
import { UserTasks } from "@vanillabp/bc-official-gui-client/dist/models/UserTasks";

/**
 * This helps to initialize the given API hook late by
 * first passing the empty reference and setting
 * 'current' property later.
 */
export interface WakeupSseCallbackReference {
  current: WakeupSseCallback | undefined;
}

type TasklistApiCall = (listId: string, pageNumber: number, pageSize: number, sort: string | undefined, sortAscending: boolean, initialTimestamp?: Date) => Promise<UserTasks>;

type TasklistUpdateCall = (listId: string, size: number, knownUserTasksIds: Array<string>, sort: string | undefined, sortAscending: boolean, initialTimestamp?: Date) => Promise<UserTasks>;

type TasklistMarkAsReadCall = (userTaskId: string, unread?: boolean) => void;

type TasklistMarkMultipleAsReadCall = (userTaskIds: Array<string>, unread?: boolean) => void;

type TasklistClaimTaskCall = (userTaskId: string, unclaim?: boolean) => void;

type TasklistClaimMultipleTasksCall = (userTaskIds: Array<string>, unclaim?: boolean) => void;

type TasklistAssignTaskCall = (userTaskId: string, userId: string, unassign?: boolean) => void;

type TasklistAssignMultipleTasksCall = (userTaskIds: Array<string>, userId: string, unassign?: boolean) => void;

type GetUserTaskCall = (userTaskId: string, markAsRead?: boolean) => Promise<UserTask>;

type FindUsersCall = (query?: string, limit?: number) => Promise<UserSearchResult>;

export interface TasklistApi {
  getUserTasks: TasklistApiCall,
  getUserTasksUpdate: TasklistUpdateCall,
  markUserTaskAsRead: TasklistMarkAsReadCall,
  markUserTasksAsRead: TasklistMarkMultipleAsReadCall,
  claimTask: TasklistClaimTaskCall,
  claimTasks: TasklistClaimMultipleTasksCall,
  assignTask: TasklistAssignTaskCall,
  assignTasks: TasklistAssignMultipleTasksCall,
  getUserTask: GetUserTaskCall,
  findUsers: FindUsersCall,
}

type GetWorkflowsApiCall = (requestId: string, pageNumber: number, pageSize: number, sort: string | undefined, sortAscending: boolean, searchQueries?: Array<SearchQuery>, initialTimestamp?: Date) => Promise<Workflows>;

type GetWorkflowsUpdateApiCall = (requestId: string, size: number, knownWorkflowsIds: Array<string>, sort: string | undefined, sortAscending: boolean, searchQueries?: Array<SearchQuery>, initialTimestamp?: Date) => Promise<Workflows>;

type GetUserTasksOfWorkflowApiCall = (workflowId: string, activeOnlyRequested?: boolean, limitListAccordingToCurrentUsersPermissions?: boolean) => Promise<Array<UserTask>>;

type GetWorkflowApiCall = (workflowId: string) => Promise<Workflow>;

type KwicWorkflowsApiCall = (query: string, path?: string /* undefined => fulltext */, searchQueries?: Array<SearchQuery>) => Promise<Array<KwicResult>>;

export interface WorkflowlistApi {
  getUserTasksOfWorkflow: GetUserTasksOfWorkflowApiCall,
  getWorkflows: GetWorkflowsApiCall,
  getWorkflowsUpdate: GetWorkflowsUpdateApiCall,
  getWorkflow: GetWorkflowApiCall,
  kwicWorkflows: KwicWorkflowsApiCall,
}

export type WorkflowlistApiHook = (wakeupSseCallback?: WakeupSseCallbackReference) => WorkflowlistApi;

export type TasklistApiHook = (wakeupSseCallback?: WakeupSseCallbackReference) => TasklistApi;
