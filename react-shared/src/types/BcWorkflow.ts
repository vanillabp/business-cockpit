import { Workflow } from "@vanillabp/bc-official-gui-client";
import { BcUserTask } from "./BcUserTask";

export type GetUserTasksFunction = (
  activeOnly: boolean,
  limitListAccordingToCurrentUsersPermissions: boolean,
) => Promise<Array<BcUserTask>>;

export interface BcWorkflow extends Workflow {
  
  getUserTasks: GetUserTasksFunction;
  
};
