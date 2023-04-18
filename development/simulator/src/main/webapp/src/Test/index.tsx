import { taskListColumns as TestForm1_taskListColumns, taskDefinition as TestForm1_taskDefinition } from './TestForm1';
import { taskListColumns as TestForm2_taskListColumns, taskDefinition as TestForm2_taskDefinition } from './TestForm2';
import { taskListColumns as TestForm3_taskListColumns, taskDefinition as TestForm3_taskDefinition } from './TestForm3';

const bpmnProcessId = 'Test';

const taskListColumns = {
  [ TestForm1_taskDefinition ]: TestForm1_taskListColumns,
  [ TestForm2_taskDefinition ]: TestForm2_taskListColumns,
  [ TestForm3_taskDefinition ]: TestForm3_taskListColumns
};

export {
  bpmnProcessId,
  taskListColumns,
};
