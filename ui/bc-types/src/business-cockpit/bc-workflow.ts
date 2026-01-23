import { Workflow } from '@vanillabp/bc-official-gui-client';
import { BcUserTasksProvider } from './bc-user-tasks-provider';

export interface BcWorkflow extends Workflow {
  navigateToWorkflow: () => void;
  getUserTasks: BcUserTasksProvider;
}
