import { Column } from '@vanillabp/bc-shared';

const taskDefinition = 'TestForm2';

const userTaskListColumns: Array<Column> = [
  {
    title: {
      'de': 'Fällig',
      'en': 'due'
    },
    path: 'dueDate',
    width: '10rem',
    priority: 99,
    show: true,
    sortable: true,
    filterable: true,
  }
];

export { userTaskListColumns, taskDefinition };
