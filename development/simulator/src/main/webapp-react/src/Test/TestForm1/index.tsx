import { Column } from "@vanillabp/bc-shared";

const taskDefinition = 'TestForm1';

const userTaskListColumns: Array<Column> = [
  {
    title: {
      'de': 'Fällig',
      'en': 'due'
    },
    path: 'dueDate',
    type: 'date',
    width: '10rem',
    priority: 99,
    show: true,
    sortable: true,
    filterable: true,
    resizeable: true,
  },
  {
    title: {
      'de': 'ID 1',
      'en': 'id 1'
    },
    path: 'details.test1.testId1',
    width: '10rem',
    priority: 1,
    show: true,
    sortable: true,
    filterable: true,
    resizeable: false,
  }
];

export { userTaskListColumns, taskDefinition };
