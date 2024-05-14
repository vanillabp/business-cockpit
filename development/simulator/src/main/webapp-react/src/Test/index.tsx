import { Column, ColumnsOfUserTaskFunction } from '@vanillabp/bc-shared';
import {
  taskDefinition as TestForm1_taskDefinition,
  userTaskListColumns as TestForm1_userTaskListColumns
} from './TestForm1';
import {
  taskDefinition as TestForm2_taskDefinition,
  userTaskListColumns as TestForm2_userTaskListColumns
} from './TestForm2';
import {
  taskDefinition as TestForm3_taskDefinition,
  userTaskListColumns as TestForm3_userTaskListColumns
} from './TestForm3';

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

const workflowListColumns: Array<Column> = [
  {
    title: {
      'de': 'ID 1',
      'en': 'id 1'
    },
    path: 'details.test1.testId1',
    type: 'i18n',
    width: '10rem',
    priority: 1,
    show: true,
    sortable: true,
    resizeable: false,
    filterable: true,
  }
];

export {
  bpmnProcessId,
  userTaskListColumns,
  workflowListColumns
};
