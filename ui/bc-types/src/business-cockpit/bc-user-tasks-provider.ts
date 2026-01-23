import { BcUserTask } from './bc-user-task';

export type BcUserTasksProvider = (
  activeOnly: boolean,
  limitListAccordingToCurrentUsersPermissions: boolean,
) => Promise<Array<BcUserTask>>;
