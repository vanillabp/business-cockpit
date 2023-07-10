import { userTaskListColumns as TestForm1_userTaskListColumns, taskDefinition as TestForm1_taskDefinition } from './TestForm1';
import { userTaskListColumns as TestForm2_userTaskListColumns, taskDefinition as TestForm2_taskDefinition } from './TestForm2';
import { userTaskListColumns as TestForm3_userTaskListColumns, taskDefinition as TestForm3_taskDefinition } from './TestForm3';

const bpmnProcessId = 'Test';

const userTaskListColumns = {
  [ TestForm1_taskDefinition ]: TestForm1_userTaskListColumns,
  [ TestForm2_taskDefinition ]: TestForm2_userTaskListColumns,
  [ TestForm3_taskDefinition ]: TestForm3_userTaskListColumns
};

export {
  bpmnProcessId,
  userTaskListColumns,
};
