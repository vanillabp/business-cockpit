import { userTaskListColumns as TestForm1_userTaskListColumns, taskDefinition as TestForm1_taskDefinition } from './TestForm1';
import { userTaskListColumns as TestForm2_userTaskListColumns, taskDefinition as TestForm2_taskDefinition } from './TestForm2';
import { userTaskListColumns as TestForm3_userTaskListColumns, taskDefinition as TestForm3_taskDefinition } from './TestForm3';

const bpmnProcessId = 'Test';

const userTaskListColumns = {
  [ TestForm1_taskDefinition ]: TestForm1_userTaskListColumns,
  [ TestForm2_taskDefinition ]: TestForm2_userTaskListColumns,
  [ TestForm3_taskDefinition ]: TestForm3_userTaskListColumns
};

const workflowListColumns = [
  {
    id: 'ID1',
    title: {
      'de': 'ID 1',
      'en': 'id 1'
    },
    path: 'details.test1.testId1',
    showAsColumn: true,
    sortable: true,
    filterable: true,
    width: '10rem',
    priority: 1
  }
];

export {
  bpmnProcessId,
  userTaskListColumns,
  workflowListColumns
};
