import { bootstrapDevShell } from '@vanillabp/bc-dev-shell';
import { UserTaskForm } from '../src/UserTaskForm';
import { UserTaskListCell, userTaskListColumns } from '../src/UserTaskList';
import { WorkflowListCell, workflowListColumns } from '../src/WorkflowList';
import { WorkflowPage } from '../src/WorkflowPage';

bootstrapDevShell(
    'root',
    '/official-api/v1',
    UserTaskForm,
    userTaskListColumns,
    UserTaskListCell,
    workflowListColumns,
    WorkflowListCell,
    WorkflowPage);
