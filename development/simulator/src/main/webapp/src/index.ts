import { UserTaskForm } from './Form';
import { TaskListCell } from './List';
import { bpmnProcessId as Test_bpmnProcessId, taskListColumns as Test_taskListColumns } from './Test';

const buildVersion = process.env.BUILD_VERSION;
const buildTimestamp = process.env.BUILD_TIMESTAMP;

const taskListColumns = {
  [ Test_bpmnProcessId ]: Test_taskListColumns
};

export {
  buildVersion,
  buildTimestamp,
  taskListColumns,
  UserTaskForm,
  TaskListCell
};
