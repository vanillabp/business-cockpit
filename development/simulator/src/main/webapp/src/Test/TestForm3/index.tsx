const taskDefinition = 'TestForm3';

const taskListColumns = [
  {
    id: 'dueDate',
    title: {
      'de': 'Fällig',
      'en': 'due'
    },
    path: 'dueDate',
    showAsColumn: true,
    sortable: true,
    filterable: true,
    width: '10rem',
    priority: 99
  },
  {
    id: 'ID2',
    title: {
      'de': 'ID 2',
      'en': 'id 2'
    },
    path: 'details.test1.testId2',
    showAsColumn: true,
    sortable: true,
    filterable: true,
    width: '10rem',
    priority: 2
  }
];

export { taskListColumns, taskDefinition };
