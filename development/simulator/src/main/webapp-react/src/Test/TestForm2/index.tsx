import { Column } from '@vanillabp/bc-shared';

const taskDefinition = 'TestForm2';

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
  }
];

export { userTaskListColumns, taskDefinition };
