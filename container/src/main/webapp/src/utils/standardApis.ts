import { MutableRefObject } from "react";
import { WakeupSseCallback } from "@vanillabp/bc-shared";
import { TasklistApi, WorkflowlistApi } from "@vanillabp/bc-ui";
import { useSpecializedTasklistApi, useTasklistApi, useWorkflowlistApi } from "./apis";
import { OfficialTasklistApi, SearchQuery } from "@vanillabp/bc-official-gui-client";

const wrapOfficialTasklistApi = (tasklistApi: OfficialTasklistApi): TasklistApi => {
  return {
    getUserTasks: (listId, pageNumber, pageSize, sort, sortAscending, initialTimestamp) => tasklistApi
        .getUserTasks({ initialTimestamp, userTasksRequest: { pageNumber, pageSize, sort, sortAscending } }),
    getUserTasksUpdate: (listId, size, knownUserTasksIds, sort, sortAscending, initialTimestamp) => tasklistApi
        .getUserTasksUpdate({ initialTimestamp, userTasksUpdateRequest: { size, knownUserTasksIds, sort, sortAscending } }),
    getUserTask: (userTaskId, markAsRead) => tasklistApi
        .getUserTask({ userTaskId, markAsRead }),
    markUserTaskAsRead: (userTaskId, unread) => tasklistApi
        .markTaskAsRead({ userTaskId, unread }),
    markUserTasksAsRead: (userTaskIds, unread) => tasklistApi
        .markTasksAsRead({ userTaskIds: { userTaskIds }, unread }),
    claimTask: (userTaskId, unclaim) => tasklistApi
        .claimTask({ userTaskId, unclaim }),
    claimTasks: (userTaskIds, unclaim) => tasklistApi
        .claimTasks({ userTaskIds: { userTaskIds }, unclaim }),
    assignTask: (userTaskId, userId, unassign) => tasklistApi
        .assignTask({ userTaskId, userId, unassign }),
    assignTasks: (userTaskIds, userId, unassign) => tasklistApi
        .assignTasks({ userTaskIds: { userTaskIds}, userId, unassign }),
    findUsers: (query, limit) => tasklistApi
        .findUsers({ query, limit }),
  };
}

const useStandardTasklistApi = (wakeupSseCallback?: MutableRefObject<WakeupSseCallback>): TasklistApi => {
  const tasklistApi = useTasklistApi(wakeupSseCallback);
  return {
    getUserTasks: (listId, pageNumber, pageSize, sort, sortAscending, initialTimestamp) => tasklistApi
        .getUserTasks({ initialTimestamp, userTasksRequest: { pageNumber, pageSize, sort, sortAscending } }),
    getUserTasksUpdate: (listId, size, knownUserTasksIds, sort, sortAscending, initialTimestamp) => tasklistApi
        .getUserTasksUpdate({ initialTimestamp, userTasksUpdateRequest: { size, knownUserTasksIds, sort, sortAscending } }),
    getUserTask: (userTaskId, markAsRead) => tasklistApi
        .getUserTask({ userTaskId, markAsRead }),
    markUserTaskAsRead: (userTaskId, unread) => tasklistApi
        .markTaskAsRead({ userTaskId, unread }),
    markUserTasksAsRead: (userTaskIds, unread) => tasklistApi
        .markTasksAsRead({ userTaskIds: { userTaskIds }, unread }),
    claimTask: (userTaskId, unclaim) => tasklistApi
        .claimTask({ userTaskId, unclaim }),
    claimTasks: (userTaskIds, unclaim) => tasklistApi
        .claimTasks({ userTaskIds: { userTaskIds }, unclaim }),
    assignTask: (userTaskId, userId, unassign) => tasklistApi
        .assignTask({ userTaskId, userId, unassign }),
    assignTasks: (userTaskIds, userId, unassign) => tasklistApi
        .assignTasks({ userTaskIds: { userTaskIds}, userId, unassign }),
    findUsers: (query, limit) => tasklistApi
        .findUsers({ query, limit }),
  };
}

const useStandardWorkflowlistApi = (wakeupSseCallback?: MutableRefObject<WakeupSseCallback>): WorkflowlistApi => {
  const workflowlistApi = useWorkflowlistApi(wakeupSseCallback);
  return {
    getWorkflows: (requestId, pageNumber, pageSize, sort, sortAscending, searchQueries, initialTimestamp) => workflowlistApi
        .getWorkflows({ requestId, initialTimestamp, workflowsRequest: { sort, sortAscending, searchQueries, pageNumber, pageSize } }),
    getWorkflowsUpdate: (requestId, size, knownWorkflowsIds, sort, sortAscending, searchQueries, initialTimestamp) => workflowlistApi
        .getWorkflowsUpdate({ requestId, initialTimestamp, workflowsUpdateRequest: { size, knownWorkflowsIds, sort, sortAscending, searchQueries } }),
    getWorkflow: workflowId => workflowlistApi
        .getWorkflow({ workflowId }),
    getUserTasksOfWorkflow: (workflowId, activeOnlyRequested, limitListAccordingToCurrentUsersPermissions) => workflowlistApi
        .getUserTasksOfWorkflow({
            workflowId,
            llatcup: limitListAccordingToCurrentUsersPermissions === undefined ? true : limitListAccordingToCurrentUsersPermissions,
            userTasksRequest: {
              mode: Boolean(activeOnlyRequested) ? "OpenTasks" : "All"
            }
        }),
    kwicWorkflows: async (query: string, path?: string, searchQueries?: Array<SearchQuery>, initialTimestamp?: Date) => {
        const result = await workflowlistApi.getKwicResults({ initialTimestamp, path, query, kwicRequest: { searchQueries } })
        return result.result;
      },
  };
}

const useCurrentUsersTasksTasklistApi = (wakeupSseCallback?: MutableRefObject<WakeupSseCallback>): TasklistApi => {
  const tasklistApi = useSpecializedTasklistApi('current-user', wakeupSseCallback);
  return wrapOfficialTasklistApi(tasklistApi);
}

const useUsersRoleTasksTasksTasklistApi = (wakeupSseCallback?: MutableRefObject<WakeupSseCallback>): TasklistApi => {
  const tasklistApi = useSpecializedTasklistApi('user-roles', wakeupSseCallback);
  return wrapOfficialTasklistApi(tasklistApi);
}

export {
  useStandardTasklistApi,
  useStandardWorkflowlistApi,
  useCurrentUsersTasksTasklistApi,
  useUsersRoleTasksTasksTasklistApi,
};
