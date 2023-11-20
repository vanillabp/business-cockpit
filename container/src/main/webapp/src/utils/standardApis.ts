import { MutableRefObject } from "react";
import { WakeupSseCallback } from "@vanillabp/bc-shared";
import { TasklistApi, WorkflowlistApi } from "@vanillabp/bc-ui";
import { useTasklistApi, useWorkflowlistApi } from "./apis";

const useStandardTasklistApi = (wakeupSseCallback?: MutableRefObject<WakeupSseCallback>): TasklistApi => {
  const tasklistApi = useTasklistApi(wakeupSseCallback);
  return {
    getUserTasks: (listId, pageNumber, pageSize, initialTimestamp) => tasklistApi
        .getUserTasks({ pageNumber, pageSize, initialTimestamp }),
    getUserTasksUpdate: (listId, size, knownUserTasksIds, initialTimestamp) => tasklistApi
        .getUserTasksUpdate({ userTasksUpdate: { size, knownUserTasksIds, initialTimestamp } }),
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
    getWorkflows: (listId, pageNumber, pageSize, initialTimestamp) => workflowlistApi
        .getWorkflows({ pageNumber, pageSize, initialTimestamp }),
    getWorkflowsUpdate: (listId, size, knownWorkflowsIds, initialTimestamp) => workflowlistApi
        .getWorkflowsUpdate({ workflowsUpdate: { size, knownWorkflowsIds, initialTimestamp } }),
    getWorkflow: workflowId => workflowlistApi
        .getWorkflow({ workflowId }),
    getUserTasksOfWorkflow: (workflowId, activeOnlyRequested, limitListAccordingToCurrentUsersPermissions) => workflowlistApi
        .getUserTasksOfWorkflow({
            workflowId,
            activeOnly: activeOnlyRequested === undefined ? true : activeOnlyRequested,
            llatcup: limitListAccordingToCurrentUsersPermissions === undefined ? true : limitListAccordingToCurrentUsersPermissions})
  };
}

export {
  useStandardTasklistApi,
  useStandardWorkflowlistApi
};
