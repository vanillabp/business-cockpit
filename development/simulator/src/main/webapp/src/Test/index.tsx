import { taskListColumns as TestForm1_taskListColumns, formKey as TestForm1_formKey } from './TestForm1';
import { taskListColumns as TestForm2_taskListColumns, formKey as TestForm2_formKey } from './TestForm2';
import { taskListColumns as TestForm3_taskListColumns, formKey as TestForm3_formKey } from './TestForm3';

const bpmnProcessId = 'Test';

const taskListColumns = {
  [ TestForm1_formKey ]: TestForm1_taskListColumns,
  [ TestForm2_formKey ]: TestForm2_taskListColumns,
  [ TestForm3_formKey ]: TestForm3_taskListColumns
};

export {
  bpmnProcessId,
  taskListColumns,
};
