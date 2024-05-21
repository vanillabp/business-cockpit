import { Column } from '@vanillabp/bc-shared';

const taskDefinition = 'TestForm3';

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
    resizeable: false,
  },
  {
    title: {
      'de': 'ID 2',
      'en': 'id 2'
    },
    path: 'details.test1.testId2',
    width: '10rem',
    priority: 2,
    show: true,
    sortable: false,
    filterable: false,
    resizeable: false,
  }
];

export { userTaskListColumns, taskDefinition };
