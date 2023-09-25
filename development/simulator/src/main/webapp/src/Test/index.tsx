import { ColumnsOfUserTaskFunction } from '@vanillabp/bc-shared';
import { userTaskListColumns as TestForm1_userTaskListColumns, taskDefinition as TestForm1_taskDefinition } from './TestForm1';
import { userTaskListColumns as TestForm2_userTaskListColumns, taskDefinition as TestForm2_taskDefinition } from './TestForm2';
import { userTaskListColumns as TestForm3_userTaskListColumns, taskDefinition as TestForm3_taskDefinition } from './TestForm3';

const bpmnProcessId = 'Test';

const userTaskListColumns: ColumnsOfUserTaskFunction = userTask => {
  if (userTask.taskDefinition === TestForm1_taskDefinition) {
    return TestForm1_userTaskListColumns;
  } else if (userTask.taskDefinition === TestForm2_taskDefinition) {
    return TestForm2_userTaskListColumns;
  } else if (userTask.taskDefinition === TestForm3_taskDefinition) {
    return TestForm3_userTaskListColumns;
  }
  return undefined;
}

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
